package ir.hefze.quran;

import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import ir.hefze.quran.core.AudioTransfer;

/** User-initiated foreground downloads; completed verses survive cancellation and app restarts. */
public final class AudioDownloads implements AutoCloseable {
    public interface Listener { void update(); }
    public static final String BASE_URL = "https://everyayah.com/data/Alafasy_64kbps/";
    private final QuranRepository repository;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile HttpURLConnection connection;
    private volatile boolean running, closed;
    private volatile String message = "";
    private long lastProgress;

    public AudioDownloads(QuranRepository repository, Listener listener) { this.repository = repository; this.listener = listener; }
    public boolean running() { return running; }
    public String message() { return message; }
    public static String url(int ayah) {
        if (ayah < 1 || ayah > 83) throw new IllegalArgumentException("Yasin ayah");
        return BASE_URL + String.format(Locale.ROOT, "036%03d.mp3", ayah);
    }
    private void notifyUi() { main.post(() -> { if (!closed) listener.update(); }); }
    public void download(int first, int last) {
        if (running || closed) return;
        if (first < 1 || last > 83 || first > last) throw new IllegalArgumentException("Yasin range");
        cancelled.set(false); running = true; message = "آغاز دانلود…"; notifyUi();
        worker.execute(() -> {
            int current = first;
            try {
                for (; current <= last; current++) {
                    if (cancelled.get()) throw new InterruptedIOException("Cancelled");
                    if (repository.hasAudio(current)) continue;
                    final int verse = current;
                    message = "دانلود آیهٔ " + MainActivity.fa(verse); notifyUi();
                    HttpURLConnection request = open(url(verse));
                    try (InputStream input = request.getInputStream()) {
                        long expected = request.getContentLengthLong();
                        AudioTransfer.save(input, expected, repository.audioFile(verse), cancelled::get, (bytes, total) -> {
                            long now = android.os.SystemClock.elapsedRealtime();
                            if (now - lastProgress > 200) {
                                lastProgress = now;
                                message = "دانلود آیهٔ " + MainActivity.fa(verse) + (total > 0 ? " • " + MainActivity.fa((int)(100 * bytes / total)) + "٪" : "");
                                notifyUi();
                            }
                        }, AudioDownloads::validateAudio);
                        repository.audioChanged(verse);
                    } finally { request.disconnect(); connection = null; }
                }
                message = "دانلود کامل شد؛ صوت‌های دریافت‌شده آفلاین پخش می‌شوند.";
            } catch (Exception e) {
                message = cancelled.get() ? "دانلود متوقف شد؛ فایل‌های کامل نگه داشته شدند."
                    : "دانلود آیهٔ " + MainActivity.fa(current) + " انجام نشد. اینترنت و فضای خالی را بررسی و دوباره تلاش کنید.";
            } finally { running = false; notifyUi(); }
        });
    }
    private HttpURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        for (int i = 0; i < 4; i++) {
            if (cancelled.get()) throw new InterruptedIOException("Cancelled");
            if (!"https".equals(url.getProtocol()) || !("everyayah.com".equals(url.getHost()) || "www.everyayah.com".equals(url.getHost()))) throw new IOException("Unexpected audio host");
            HttpURLConnection request = (HttpURLConnection) url.openConnection(); connection = request;
            request.setConnectTimeout(15000); request.setReadTimeout(20000); request.setInstanceFollowRedirects(false);
            request.setRequestProperty("Accept", "audio/mpeg"); request.setRequestProperty("Accept-Encoding", "identity");
            int code;
            try { code = request.getResponseCode(); } catch (IOException e) { request.disconnect(); throw e; }
            if (code == 200) return request;
            String location = request.getHeaderField("Location"); request.disconnect();
            if ((code == 301 || code == 302 || code == 303 || code == 307 || code == 308) && location != null) url = new URL(url, location);
            else throw new IOException("Audio HTTP " + code);
        }
        throw new IOException("Too many redirects");
    }
    private static void validateAudio(File file) throws IOException {
        MediaMetadataRetriever metadata = new MediaMetadataRetriever();
        try {
            metadata.setDataSource(file.getAbsolutePath());
            String duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null || Long.parseLong(duration) <= 0) throw new IOException("Invalid audio duration");
        } catch (RuntimeException e) { throw new IOException("Not a decodable audio file", e); }
        finally { metadata.release(); }
    }
    public void cancel() {
        if (!running) return;
        cancelled.set(true);
        HttpURLConnection current = connection;
        if (current != null) current.disconnect();
    }
    @Override public void close() { closed = true; cancel(); worker.shutdownNow(); main.removeCallbacksAndMessages(null); }
}
