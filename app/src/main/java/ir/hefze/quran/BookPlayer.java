package ir.hefze.quran;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.File;
import java.util.function.Consumer;

/* JADX INFO: loaded from: classes.dex */
public final class BookPlayer implements AutoCloseable {
    private final Runnable changed;
    private final Runnable ended;
    private final Consumer<String> error;
    private final AudioFocusRequest focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() { // from class: ir.hefze.quran.BookPlayer$$ExternalSyntheticLambda0
        @Override // android.media.AudioManager.OnAudioFocusChangeListener
        public final void onAudioFocusChange(int i) {
            BookPlayer.this.lambda$new$0(i);
        }
    }).build();
    private long generation;
    private final AudioManager manager;
    private MediaPlayer player;
    private boolean ready;
    private boolean wantsPlay;

    public BookPlayer(Context context, Runnable runnable, Runnable runnable2, Consumer<String> consumer) {
        this.changed = runnable;
        this.ended = runnable2;
        this.error = consumer;
        this.manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(int i) {
        if (i < 0) {
            pause();
        }
    }

    public boolean loading() {
        return (this.player == null || this.ready) ? false : true;
    }

    public boolean playing() {
        return this.wantsPlay;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$start$1(long j, MediaPlayer mediaPlayer) {
        if (j != this.generation) {
            return;
        }
        this.ready = true;
        if (this.wantsPlay) {
            mediaPlayer.start();
        }
        this.changed.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$start$2(long j, MediaPlayer mediaPlayer) {
        if (j == this.generation && this.wantsPlay) {
            stop();
            this.ended.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$start$3(long j, MediaPlayer mediaPlayer, int i, int i2) {
        if (j != this.generation) {
            return true;
        }
        stop();
        this.error.accept("پخش فایل صوتی انجام نشد.");
        return true;
    }

    public void start(File file) {
        stop();
        if (this.manager.requestAudioFocus(this.focus) != 1) {
            this.error.accept("خروجی صدا در دسترس نیست.");
            return;
        }
        final long j = this.generation + 1;
        this.generation = j;
        this.player = new MediaPlayer();
        MediaPlayer mediaPlayer = this.player;
        this.wantsPlay = true;
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { // from class: ir.hefze.quran.BookPlayer$$ExternalSyntheticLambda1
            @Override // android.media.MediaPlayer.OnPreparedListener
            public final void onPrepared(MediaPlayer mediaPlayer2) {
                BookPlayer.this.lambda$start$1(j, mediaPlayer2);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: ir.hefze.quran.BookPlayer$$ExternalSyntheticLambda2
            @Override // android.media.MediaPlayer.OnCompletionListener
            public final void onCompletion(MediaPlayer mediaPlayer2) {
                BookPlayer.this.lambda$start$2(j, mediaPlayer2);
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() { // from class: ir.hefze.quran.BookPlayer$$ExternalSyntheticLambda3
            @Override // android.media.MediaPlayer.OnErrorListener
            public final boolean onError(MediaPlayer mediaPlayer2, int i, int i2) {
                return BookPlayer.this.lambda$start$3(j, mediaPlayer2, i, i2);
            }
        });
        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepareAsync();
            this.changed.run();
        } catch (Exception e) {
            stop();
            this.error.accept("فایل صوتی قابل پخش نیست.");
        }
    }

    public void toggle(File file) {
        if (this.player == null) {
            start(file);
            return;
        }
        if (this.wantsPlay) {
            pause();
        } else {
            if (this.manager.requestAudioFocus(this.focus) != 1) {
                return;
            }
            this.wantsPlay = true;
            if (this.ready) {
                this.player.start();
            }
            this.changed.run();
        }
    }

    public void pause() {
        this.wantsPlay = false;
        if (this.player != null && this.ready && this.player.isPlaying()) {
            this.player.pause();
        }
        this.manager.abandonAudioFocusRequest(this.focus);
        this.changed.run();
    }

    public void stop() {
        this.generation++;
        this.wantsPlay = false;
        this.ready = false;
        if (this.player != null) {
            this.player.release();
            this.player = null;
        }
        this.manager.abandonAudioFocusRequest(this.focus);
        this.changed.run();
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        stop();
    }
}
