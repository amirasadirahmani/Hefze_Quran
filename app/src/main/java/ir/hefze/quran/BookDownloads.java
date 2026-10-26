package ir.hefze.quran;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import ir.hefze.quran.core.AudioTransfer;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Foreground-only downloads; AudioTransfer validates and atomically publishes each file. */
public final class BookDownloads implements AutoCloseable {
    private static final long MAX_SURAH_BYTES = 512L * 1024 * 1024;
    private final Context context;
    private final QuranBook book;
    private final Runnable changed;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile HttpURLConnection connection;
    private volatile boolean closed;
    private long lastUpdate;
    public volatile boolean running;
    public volatile int progress;
    public volatile String message = "";

    public BookDownloads(Context context, QuranBook book, Runnable changed) {
        this.context = context.getApplicationContext();
        this.book = book;
        this.changed = changed;
    }

    private void notifyUi() { main.post(() -> { if (!closed) changed.run(); }); }

    public void start(QuranBook.Reciter reciter, int surah) {
        if (running || closed) return;
        if (!book.supportsSurah(reciter, surah)) {
            message = "صوت این سوره برای قاری انتخاب‌شده موجود نیست. قاری دیگری انتخاب کنید.";
            notifyUi();
            return;
        }
        List<QuranBook.Ayah> verses = new ArrayList<>(book.surahs.get(surah));
        if (!reciter.byAyah()) verses = Collections.singletonList(verses.get(0));
        final List<QuranBook.Ayah> work = verses;
        running = true;
        progress = 0;
        cancelled.set(false);
        message = "آماده‌سازی…";
        notifyUi();
        worker.execute(() -> download(reciter, work));
    }

    private void download(QuranBook.Reciter reciter, List<QuranBook.Ayah> work) {
        try {
            for (int index = 0; index < work.size(); index++) {
                if (cancelled.get()) throw new InterruptedIOException("Cancelled");
                QuranBook.Ayah ayah = work.get(index);
                File target = book.audio(context, reciter, ayah);
                if (target.length() == 0) downloadOne(reciter, ayah, target, index, work.size());
                progress = (index + 1) * 100 / work.size();
                notifyUi();
            }
            progress = 100;
            message = "دریافت سوره کامل شد";
        } catch (Exception e) {
            message = cancelled.get() ? "دریافت متوقف شد؛ فایل‌های کامل حفظ شدند."
                    : "دریافت کامل نشد؛ اتصال، فضای خالی یا موجودبودن صوت این قاری را بررسی و دوباره تلاش کنید.";
        } finally {
            running = false;
            notifyUi();
        }
    }

    private void downloadOne(QuranBook.Reciter reciter, QuranBook.Ayah ayah,
                             File target, int index, int total) throws IOException {
        HttpURLConnection request = open(book.url(reciter, ayah));
        try (InputStream input = request.getInputStream()) {
            long maxBytes = reciter.byAyah() ? AudioTransfer.MAX_BYTES : MAX_SURAH_BYTES;
            AudioTransfer.save(input, request.getContentLengthLong(), target, cancelled::get,
                    (received, expected) -> {
                        long now = SystemClock.elapsedRealtime();
                        if (now - lastUpdate >= 200) {
                            lastUpdate = now;
                            double fraction = expected > 0 ? Math.min(1.0, (double) received / expected) : 0;
                            progress = (int) ((index + fraction) * 100 / total);
                            message = "دریافت " + MainActivity.fa(progress) + "٪";
                            notifyUi();
                        }
                    }, BookDownloads::validateAudio, maxBytes);
        } finally {
            request.disconnect();
            connection = null;
        }
    }

    private static void validateAudio(File file) throws IOException {
        MediaMetadataRetriever metadata = new MediaMetadataRetriever();
        try {
            metadata.setDataSource(file.getAbsolutePath());
            String duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null || Long.parseLong(duration) <= 0) throw new IOException("Invalid audio duration");
        } catch (RuntimeException e) {
            throw new IOException("Cannot decode audio", e);
        } finally {
            metadata.release();
        }
    }

    private HttpURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        for (int redirect = 0; redirect < 4; redirect++) {
            if (cancelled.get()) throw new InterruptedIOException("Cancelled");
            if (!"https".equals(url.getProtocol()) || !"cdn.islamic.network".equals(url.getHost())) {
                throw new IOException("Unexpected audio host");
            }
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            connection = request;
            request.setConnectTimeout(15000);
            request.setReadTimeout(20000);
            request.setInstanceFollowRedirects(false);
            request.setRequestProperty("Accept-Encoding", "identity");
            int code;
            try { code = request.getResponseCode(); }
            catch (IOException e) { request.disconnect(); throw e; }
            if (code == 200) return request;
            String location = request.getHeaderField("Location");
            request.disconnect();
            if (code >= 300 && code < 400 && location != null) url = new URL(url, location);
            else throw new IOException("Audio HTTP " + code);
        }
        throw new IOException("Too many redirects");
    }

    public void cancel() {
        cancelled.set(true);
        HttpURLConnection current = connection;
        if (current != null) current.disconnect();
    }

    @Override public void close() {
        closed = true;
        cancel();
        worker.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
