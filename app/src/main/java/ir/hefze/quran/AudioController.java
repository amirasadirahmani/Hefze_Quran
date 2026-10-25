package ir.hefze.quran;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.os.*;
import java.util.concurrent.*;
import ir.hefze.quran.core.*;

/** Android adapter for the tested queue. Foreground-only: backgrounding pauses both sound and silence. */
public final class AudioController implements PlaybackQueue.Port {
    public interface Listener {void update();}
    public final PlaybackQueue queue;
    public WordTimeline timeline;
    public String error="";
    private final Context context;
    private final QuranRepository repository;
    private final StudyStore store;
    private final Listener listener;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService loader=Executors.newSingleThreadExecutor();
    private final AudioManager manager;
    private final AudioFocusRequest focus;
    private MediaPlayer player;
    private boolean prepared,closed;
    private int seekOnLoad;
    private long lastTick,unflushed;
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){pauseNow();}};
    private final Runnable ticker=new Runnable(){@Override public void run(){
        if(closed)return;
        account(); queue.tick(SystemClock.elapsedRealtime()); listener.update(); handler.postDelayed(this,100);
    }};
    public AudioController(Context context,QuranRepository repository,StudyStore store,Listener listener){
        this.context=context;this.repository=repository;this.store=store;this.listener=listener;
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(change->{if(change<0)pauseNow();},handler).build();
        queue=new PlaybackQueue(this);
        IntentFilter filter=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if(Build.VERSION.SDK_INT>=33)context.registerReceiver(noisy,filter,Context.RECEIVER_NOT_EXPORTED);else context.registerReceiver(noisy,filter);
        handler.post(ticker);
    }
    public void visible(boolean visible){handler.removeCallbacks(ticker);if(visible&&!closed){lastTick=SystemClock.elapsedRealtime();handler.post(ticker);}}
    public void restorePosition(int position){seekOnLoad=Math.max(0,position);}
    public void clearSeek(){seekOnLoad=0;}
    public void toggle(){if(queue.running())pauseNow();else queue.play(SystemClock.elapsedRealtime());}
    public void pauseNow(){queue.pause(SystemClock.elapsedRealtime());flush();manager.abandonAudioFocusRequest(focus);}
    public int position(){if(player!=null&&prepared){try{return player.getCurrentPosition();}catch(IllegalStateException ignored){}}return seekOnLoad;}
    public int duration(){if(player!=null&&prepared){try{return player.getDuration();}catch(IllegalStateException ignored){}}return 0;}
    private void account(){
        long now=SystemClock.elapsedRealtime();
        if(lastTick>0&&player!=null&&prepared&&queue.state()==PlaybackQueue.State.PLAYING)unflushed+=Math.max(0,now-lastTick);
        lastTick=now;if(unflushed>=5000)flush();
    }
    public void flush(){if(unflushed>0){store.listened(unflushed);unflushed=0;}}
    @Override public void load(int ayah,long generation){
        release();timeline=null;error="";
        // Asset hashing and timing parsing can be expensive; never do them on the UI thread.
        loader.execute(()->{
            WordTimeline candidate=repository.timeline(ayah);
            handler.post(()->{
                if(closed||queue.generation()!=generation)return;
                timeline=candidate;
                try {
                    player=new MediaPlayer();
                    player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                    java.io.File local=repository.audioFile(ayah);
                    if(local.isFile())player.setDataSource(local.getAbsolutePath());
                    else try(AssetFileDescriptor fd=context.getAssets().openFd(repository.verse(ayah).audio)) {player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}
                    player.setOnPreparedListener(mp->{
                        if(closed||queue.generation()!=generation)return;
                        prepared=true;
                        if(timeline!=null&&timeline.ends[timeline.ends.length-1]>mp.getDuration())timeline=null;
                        int seek=Math.min(seekOnLoad,Math.max(0,mp.getDuration()-1));seekOnLoad=0;
                        if(seek>0){mp.setOnSeekCompleteListener(p->{p.setOnSeekCompleteListener(null);queue.ready(generation);});mp.seekTo(seek);}
                        else queue.ready(generation);
                    });
                    player.setOnCompletionListener(mp->{account();queue.ended(generation,SystemClock.elapsedRealtime());});
                    player.setOnErrorListener((mp,what,extra)->{error="فایل صوتی قابل پخش نیست.";queue.failed(generation);return true;});
                    player.prepareAsync();
                } catch(Exception e){error="صوت آیهٔ "+MainActivity.fa(ayah)+" دانلود نشده یا قابل خواندن نیست؛ از بخش دریافت صوت استفاده کنید.";queue.failed(generation);}
            });
        });
    }
    @Override public void start(){
        if(player==null||!prepared)return;
        if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){error="در حال حاضر امکان دریافت صدای دستگاه نیست.";queue.pause(SystemClock.elapsedRealtime());return;}
        try{lastTick=SystemClock.elapsedRealtime();player.start();}catch(IllegalStateException e){error="پخش متوقف شد؛ دوباره تلاش کنید.";queue.failed(queue.generation());}
    }
    @Override public void pause(){account();if(player!=null&&prepared)player.pause();flush();}
    @Override public void release(){
        account();flush();prepared=false;
        if(player!=null){player.setOnCompletionListener(null);player.setOnPreparedListener(null);player.setOnErrorListener(null);player.release();player=null;}
    }
    @Override public void changed(){listener.update();}
    @Override public void completed(int ayah){store.completed();}
    public void close(){closed=true;queue.stop();handler.removeCallbacksAndMessages(null);loader.shutdownNow();manager.abandonAudioFocusRequest(focus);context.unregisterReceiver(noisy);flush();}
}
