package ir.hefze.quran;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

/** Speaks the invocation before a manually started recitation; never silently skips it. */
final class OpeningRecitation implements AutoCloseable {
    private static final String ISTIADHA = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ";
    private static final String BASMALA = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ";
    private TextToSpeech speech;
    private long generation;
    private boolean pending;
    private final Context context;
    OpeningRecitation(Context context) { this.context = context.getApplicationContext(); }
    static String[] phrases(int surah, int verse) {
        // At-Tawbah has no opening basmala.
        return verse == 1 && surah != 9 ? new String[]{ISTIADHA, BASMALA} : new String[]{ISTIADHA};
    }
    boolean pending() { return pending; }
    void speak(int surah, int verse, Runnable done, Runnable unavailable) {
        stop(); pending = true;
        final long token = generation;
        if (speech != null) { enqueue(token, phrases(surah,verse), done,unavailable); return; }
        speech = new TextToSpeech(context, status -> {
            if (token != generation) return;
            if (status != TextToSpeech.SUCCESS) { stop(); unavailable.run(); return; }
            enqueue(token, phrases(surah,verse), done,unavailable);
        });
    }
    private void enqueue(long token, String[] phrases, Runnable done, Runnable unavailable) {
        if (speech.isLanguageAvailable(new Locale("ar")) < TextToSpeech.LANG_AVAILABLE || speech.setLanguage(new Locale("ar")) < TextToSpeech.LANG_AVAILABLE) {
            stop(); unavailable.run(); return;
        }
        speech.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) {
                if (!id.equals(token + ":" + (phrases.length-1))) return;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (token == generation) { pending = false; done.run(); }
                });
            }
            @Override public void onError(String id) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (token == generation) { stop(); unavailable.run(); }
                });
            }
        });
        for (int i=0;i<phrases.length;i++) {
            int result=speech.speak(phrases[i], i==0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, new Bundle(),token+":"+i);
            if (result == TextToSpeech.ERROR) { stop(); unavailable.run(); return; }
        }
    }
    void stop() { generation++; pending=false; if (speech!=null) speech.stop(); }
    @Override public void close() { stop(); if(speech!=null){speech.shutdown();speech=null;} }
}
