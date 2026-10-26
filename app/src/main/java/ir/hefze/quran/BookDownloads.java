package ir.hefze.quran;
import android.content.Context;import android.media.MediaMetadataRetriever;import android.os.*;
import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
/** User initiated foreground download; publish only fully decoded recordings. */
public final class BookDownloads implements AutoCloseable {
 private final Context context;private final QuranBook book;private final Runnable changed;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();private final AtomicBoolean cancel=new AtomicBoolean();private volatile HttpURLConnection connection;private volatile boolean closed;private long lastUpdate;
 public volatile boolean running;public volatile int progress;public volatile String message="";
 public BookDownloads(Context c,QuranBook b,Runnable r){context=c.getApplicationContext();book=b;changed=r;}
 private void notifyUi(){main.post(()->{if(!closed)changed.run();});}
 public void start(QuranBook.Reciter reciter,int surah){if(running||closed)return;List<QuranBook.Ayah> verses=new ArrayList<>(book.surahs.get(surah));if(!reciter.byAyah())verses=Collections.singletonList(verses.get(0));final List<QuranBook.Ayah> work=verses;running=true;progress=0;cancel.set(false);message="آماده‌سازی…";notifyUi();worker.execute(()->{
 try{for(int i=0;i<work.size();i++){if(cancel.get())throw new InterruptedIOException();QuranBook.Ayah a=work.get(i);File target=book.audio(context,reciter,a);if(target.length()>0){progress=(i+1)*100/work.size();notifyUi();continue;}
  int key=reciter.byAyah()?a.id:a.surah;if(!reciter.available.contains(key))throw new IOException("Unavailable edition item");
  HttpURLConnection con=open(book.url(reciter,a));File part=new File(target.getPath()+".part");
  try{if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Storage");long expected=con.getContentLengthLong(),limit=512L*1024*1024,received=0;if(expected>limit)throw new IOException("Too large");
   try(InputStream in=con.getInputStream();FileOutputStream out=new FileOutputStream(part)){byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1){if(cancel.get())throw new InterruptedIOException();received+=n;if(received>limit)throw new IOException("Too large");out.write(buf,0,n);long now=SystemClock.elapsedRealtime();if(now-lastUpdate>200){lastUpdate=now;progress=(int)((i+(expected>0?Math.min(1,received/(double)expected):0))*100/work.size());message="دریافت "+MainActivity.fa(progress)+"٪";notifyUi();}}out.getFD().sync();}
   if(received==0||(expected>=0&&received!=expected)||cancel.get())throw new IOException("Incomplete");MediaMetadataRetriever m=new MediaMetadataRetriever();try{m.setDataSource(part.getAbsolutePath());String duration=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(duration==null||Long.parseLong(duration)<=0)throw new IOException("Invalid audio");}finally{m.release();}if(cancel.get())throw new InterruptedIOException();try{Files.move(part.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(part.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);}
  }finally{con.disconnect();connection=null;Files.deleteIfExists(part.toPath());}
 }progress=100;message="دریافت سوره کامل شد";
 }catch(Exception e){message=cancel.get()?"دریافت متوقف شد؛ فایل‌های کامل حفظ شدند.":"دریافت کامل نشد؛ اتصال، فضای خالی یا موجودبودن صوت این قاری را بررسی و دوباره تلاش کنید.";}finally{running=false;notifyUi();}
 });}
 private HttpURLConnection open(String address)throws IOException{URL u=new URL(address);for(int i=0;i<4;i++){if(cancel.get())throw new InterruptedIOException();if(!u.getProtocol().equals("https")||!u.getHost().equals("cdn.islamic.network"))throw new IOException("Unexpected host");HttpURLConnection c=(HttpURLConnection)u.openConnection();connection=c;c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept-Encoding","identity");int code;try{code=c.getResponseCode();}catch(IOException e){c.disconnect();throw e;}if(code==200)return c;String location=c.getHeaderField("Location");c.disconnect();if(code>=300&&code<400&&location!=null)u=new URL(u,location);else throw new IOException("HTTP "+code);}throw new IOException("Redirects");}
 public void cancel(){cancel.set(true);HttpURLConnection c=connection;if(c!=null)c.disconnect();}
 public void close(){closed=true;cancel();worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
