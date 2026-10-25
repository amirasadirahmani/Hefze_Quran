package ir.hefze.quran;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import ir.hefze.quran.core.WordTimeline;

public final class QuranRepository {
    public static final class Verse {
        public int number,page;
        public String text,audio,timing;
        public final List<String> words=new ArrayList<>();
    }
    public static final class Token {
        public int ayah,word; public String text;
    }
    public final List<Verse> verses=new ArrayList<>();
    public final List<List<List<Token>>> pages=new ArrayList<>();
    private final Context context;
    private final Map<Integer,WordTimeline> timelines=new HashMap<>();
    private final Set<Integer> checked=new HashSet<>();
    private final Map<Integer,Boolean> audioAvailability=new HashMap<>();
    public final String basmala;
    public QuranRepository(Context context) throws Exception {
        this.context=context;
        JSONObject root=read("content/yasin.json");
        if(root.getInt("surah")!=36) throw new IOException("Wrong surah");
        basmala=root.getString("basmala");
        JSONArray data=root.getJSONArray("verses");
        if(data.length()!=83) throw new IOException("Expected 83 verses");
        for(int i=0;i<data.length();i++) {
            JSONObject o=data.getJSONObject(i); Verse v=new Verse();
            v.number=o.getInt("number"); if(v.number!=i+1) throw new IOException("Verse sequence");
            v.text=o.getString("text");v.page=o.getInt("page");v.audio=o.getString("audio");v.timing=o.getString("timing");
            JSONArray w=o.getJSONArray("words");for(int j=0;j<w.length();j++)v.words.add(w.getString(j));
            verses.add(v);
        }
        JSONArray ps=root.getJSONArray("pages");
        for(int p=0;p<ps.length();p++) {
            List<List<Token>> page=new ArrayList<>(); JSONArray ls=ps.getJSONObject(p).getJSONArray("lines");
            for(int l=0;l<ls.length();l++) {
                List<Token> line=new ArrayList<>();JSONArray ts=ls.getJSONArray(l);
                for(int t=0;t<ts.length();t++) {
                    JSONObject o=ts.getJSONObject(t);Token x=new Token();x.ayah=o.getInt("ayah");x.word=o.getInt("word");x.text=o.getString("text");line.add(x);
                } page.add(line);
            } pages.add(page);
        }
    }
    public Verse verse(int ayah) { return verses.get(ayah-1); }
    public synchronized boolean hasAudio(int ayah) {
        if(audioFile(ayah).isFile() && audioFile(ayah).length()>0)return true;
        if(audioAvailability.containsKey(ayah))return audioAvailability.get(ayah);
        boolean available;
        try(InputStream in=context.getAssets().open(verse(ayah).audio)) { available=in.read()!=-1; }
        catch(IOException e) { available=false; }
        audioAvailability.put(ayah,available);return available;
    }
    public File audioFile(int ayah) { return new File(context.getFilesDir(), "recitations/alafasy64/" + String.format(Locale.ROOT,"036-%03d.mp3",ayah)); }
    public synchronized void audioChanged(int ayah) { audioAvailability.remove(ayah); checked.remove(ayah); timelines.remove(ayah); }
    public InputStream openAudio(int ayah) throws IOException { File f=audioFile(ayah); return f.isFile()?new FileInputStream(f):context.getAssets().open(verse(ayah).audio); }
    public int downloadedCount() { int count=0;for(int i=1;i<=83;i++)if(hasAudio(i))count++;return count; }
    private JSONObject read(String path) throws Exception {
        try(InputStream in=context.getAssets().open(path); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        }
    }
    /** Reject missing, unreviewed, stale, incomplete or malformed timing data. Never approximate. */
    public synchronized WordTimeline timeline(int ayah) {
        if(checked.contains(ayah))return timelines.get(ayah);
        checked.add(ayah);
        try {
            Verse v=verse(ayah);JSONObject o=read(v.timing);
            if(o.getInt("schemaVersion")!=1 || !o.getBoolean("reviewed") || o.getInt("surah")!=36 || o.getInt("ayah")!=ayah) return null;
            JSONArray words=o.getJSONArray("words");if(words.length()!=v.words.size())return null;
            int[] starts=new int[words.length()],ends=new int[words.length()];
            for(int i=0;i<words.length();i++) {
                JSONObject w=words.getJSONObject(i);
                if(w.getInt("index")!=i || !w.getString("text").equals(v.words.get(i)))return null;
                starts[i]=w.getInt("startMs");ends[i]=w.getInt("endMs");
            }
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            try(InputStream in=openAudio(ayah)) {byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}
            StringBuilder hash=new StringBuilder();for(byte b:md.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));
            if(!hash.toString().equals(o.getString("audioSha256")))return null;
            WordTimeline result=new WordTimeline(starts,ends,o.getInt("durationMs"));timelines.put(ayah,result);return result;
        } catch(Exception e) { return null; }
    }
}
