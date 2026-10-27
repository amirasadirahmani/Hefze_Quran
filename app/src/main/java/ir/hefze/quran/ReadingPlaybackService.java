package ir.hefze.quran;

import android.app.*;
import android.content.*;
import android.os.*;
import java.util.List;

/** Foreground owner of reading playback while the lock-screen option is enabled. */
public final class ReadingPlaybackService extends Service {
    static final String SYNC="ir.hefze.quran.READING_CHANGED";
    private static final String CHANNEL="reading_playback";
    private static ReadingPlaybackService active;
    private BookPlayer player;
    private OpeningRecitation opening;
    private QuranBook book;
    private QuranBook.Reciter reciter;
    private QuranBook.Ayah ayah;
    private ir.hefze.quran.core.RecitationCycle cycle;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable due=this::finishGap;
    private boolean repeat, closing;
    private boolean prefacePending;
    private int repeatCount,gapSeconds;
    public static boolean activeSession() { return active!=null && !active.closing && active.ayah!=null; }
    public static boolean playing() { return active!=null && active.running(); }
    public static void stopActive() { if(active!=null)active.finish(); }
    static void start(Context c,QuranBook.Reciter reciter,QuranBook.Ayah ayah,int count,int gap,boolean repeat) {
        Intent i=new Intent(c,ReadingPlaybackService.class).setAction("start")
            .putExtra("reciter",reciter.id).putExtra("ayah",ayah.id).putExtra("count",count).putExtra("gap",gap).putExtra("repeat",repeat);
        c.startForegroundService(i);
    }
    @Override public void onCreate() {
        super.onCreate();active=this;
        android.app.NotificationChannel channel=new android.app.NotificationChannel(CHANNEL,"قرائت قرآن",android.app.NotificationManager.IMPORTANCE_LOW);
        ((android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        try { book=new QuranBook(this); } catch(Exception e) { stopSelf(); return; }
        opening=new OpeningRecitation(this);
        player=new BookPlayer(this,this::notifyState,this::completed,s -> { android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_LONG).show();finish(); });
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if (book==null) {stopSelf();return START_NOT_STICKY;}
        String command=intent==null?"stop":intent.getAction();
        if ("start".equals(command)) {
            closing=false;cancelPlayback();
            reciter=book.reciters.get(0);
            for(QuranBook.Reciter r:book.reciters)if(r.id.equals(intent.getStringExtra("reciter")))reciter=r;
            ayah=book.all.get(Math.max(0,Math.min(book.all.size()-1,intent.getIntExtra("ayah",1)-1)));
            repeatCount=Math.max(1,Math.min(10,intent.getIntExtra("count",3)));
            gapSeconds=Math.max(0,Math.min(30,intent.getIntExtra("gap",3)));
            repeat=intent.getBooleanExtra("repeat",false);
            cycle=new ir.hefze.quran.core.RecitationCycle(repeatCount,gapSeconds);
            startForeground(41,notification());
            opening.speak(ayah.surah,ayah.number,()->{ player.start(book.audio(this,reciter,ayah));notifyState(); },()->{
                android.widget.Toast.makeText(this,"صدای عربی برای مقدمه موجود نیست؛ زبان عربی موتور گفتار را نصب کنید.",android.widget.Toast.LENGTH_LONG).show();finish();
            });
        } else if ("pause".equals(command)) pause();
        else if ("resume".equals(command)) resume();
        else if ("stop".equals(command)) finish();
        return START_NOT_STICKY;
    }
    private boolean running() { return opening!=null&&opening.pending() || player!=null&&player.playing() || cycle!=null&&cycle.pending()&&!cycle.paused(); }
    private Notification notification() {
        Intent launch=new Intent(this,FullQuranActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content=PendingIntent.getActivity(this,0,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_book).setContentTitle("قرائت قرآن")
            .setContentText(ayah==null?"آمادهٔ پخش":book.names[ayah.surah]+" · آیهٔ "+MainActivity.fa(ayah.number))
            .setContentIntent(content).setOngoing(running()).setVisibility(Notification.VISIBILITY_PUBLIC);
        String command=running()?"pause":"resume";
        n.addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause,running()?"مکث":"ادامه",PendingIntent.getService(this,1,new Intent(this,ReadingPlaybackService.class).setAction(command),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build());
        n.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"توقف",PendingIntent.getService(this,2,new Intent(this,ReadingPlaybackService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build());
        return n.build();
    }
    private void notifyState() {
        if(closing||ayah==null)return;
        getSharedPreferences(FullQuranActivity.class.getName(),0).edit().putInt("ayah",ayah.id).putInt("page",ayah.page).putBoolean("has_reading",true).apply();
        sendBroadcast(new Intent(SYNC).setPackage(getPackageName()).putExtra("ayah",ayah.id));
        ((android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(41,notification());
    }
    private void completed() {
        if(!reciter.byAyah()){finish();return;}
        cycle.completed(repeat,ayah.number<book.surahs.get(ayah.surah).size(),SystemClock.uptimeMillis());
        if(!cycle.pending()){finish();return;}
        handler.postDelayed(due,cycle.remaining(SystemClock.uptimeMillis()));notifyState();
    }
    private void finishGap() {
        ir.hefze.quran.core.RecitationCycle.Action action=cycle.takeDue(SystemClock.uptimeMillis());
        if(action==ir.hefze.quran.core.RecitationCycle.Action.NONE)return;
        if(action==ir.hefze.quran.core.RecitationCycle.Action.NEXT) {
            List<QuranBook.Ayah> verses=book.surahs.get(ayah.surah);
            ayah=verses.get(ayah.number);notifyState();
        }
        player.start(book.audio(this,reciter,ayah));
    }
    private void pause(){
        if(opening.pending()) { opening.stop(); prefacePending=true; }
        handler.removeCallbacks(due);if(cycle!=null)cycle.pause(SystemClock.uptimeMillis());player.pause();notifyState();
    }
    private void resume(){
        if(cycle!=null&&cycle.pending()){
            cycle.resume(SystemClock.uptimeMillis());handler.postDelayed(due,cycle.remaining(SystemClock.uptimeMillis()));
        }else if(player.hasSession())player.toggle(book.audio(this,reciter,ayah));
        else opening.speak(ayah.surah,ayah.number,()->player.start(book.audio(this,reciter,ayah)),this::finish);
        notifyState();
    }
    private void cancelPlayback(){prefacePending=false;handler.removeCallbacks(due);if(opening!=null)opening.stop();if(player!=null)player.stop();}
    private void finish(){
        if(closing)return;closing=true;cancelPlayback();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    @Override public void onDestroy(){cancelPlayback();if(opening!=null)opening.close();if(player!=null)player.close();active=null;super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
