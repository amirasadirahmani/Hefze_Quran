package ir.hefze.quran;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class QuranBook {
    public final List<Ayah> all = new ArrayList();
    public final List<List<Ayah>> pages = new ArrayList();
    public final List<List<Ayah>> surahs = new ArrayList();
    public final String[] names = new String[115];
    public final List<Reciter> reciters = new ArrayList();

    public static final class Ayah {
        public int id;
        public int juz;
        public int number;
        public int page;
        public int surah;
        public String text;
    }

    public static final class Reciter {
        public final Set<Integer> available = new HashSet();
        public int bitrate;
        public String cdnId;
        public String id;
        public String mode;
        public String name;

        public boolean byAyah() {
            return this.mode.equals("ayah");
        }

        public String toString() {
            return this.name + " — " + (byAyah() ? "آیه\u200cبه\u200cآیه" : "سورهٔ کامل") + " (" + this.id + ")";
        }
    }

    /* JADX WARN: Code duplicated, block: B:32:0x0039 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    public static String read(Context c,String path)throws Exception {try(InputStream in=c.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
    public QuranBook(Context context) throws Exception {
        for (int i = 0; i <= 604; i++) {
            this.pages.add(new ArrayList());
        }
        for (int i2 = 0; i2 <= 114; i2++) {
            this.surahs.add(new ArrayList());
        }
        JSONArray jSONArray = new JSONObject(read(context, "content/quran-uthmani.json")).getJSONObject("data").getJSONArray("surahs");
        for (int i3 = 0; i3 < jSONArray.length(); i3++) {
            JSONObject jSONObject = jSONArray.getJSONObject(i3);
            int i4 = jSONObject.getInt("number");
            this.names[i4] = jSONObject.getString("name");
            JSONArray jSONArray2 = jSONObject.getJSONArray("ayahs");
            for (int i5 = 0; i5 < jSONArray2.length(); i5++) {
                JSONObject jSONObject2 = jSONArray2.getJSONObject(i5);
                Ayah ayah = new Ayah();
                ayah.id = jSONObject2.getInt("number");
                ayah.surah = i4;
                ayah.number = jSONObject2.getInt("numberInSurah");
                ayah.page = jSONObject2.getInt("page");
                ayah.juz = jSONObject2.getInt("juz");
                ayah.text = jSONObject2.getString("text");
                this.all.add(ayah);
                this.pages.get(ayah.page).add(ayah);
                this.surahs.get(i4).add(ayah);
            }
        }
        JSONArray jSONArray3 = new JSONArray(read(context, "content/reciters.json"));
        for (int i6 = 0; i6 < jSONArray3.length(); i6++) {
            JSONObject jSONObject3 = jSONArray3.getJSONObject(i6);
            Reciter reciter = new Reciter();
            reciter.id = jSONObject3.getString("id");
            reciter.cdnId = jSONObject3.getString("cdnId");
            reciter.name = jSONObject3.getString("name");
            reciter.mode = jSONObject3.getString("mode");
            reciter.bitrate = jSONObject3.getInt("bitrate");
            JSONArray jSONArray4 = jSONObject3.getJSONArray("available");
            for (int i7 = 0; i7 < jSONArray4.length(); i7++) {
                reciter.available.add(Integer.valueOf(jSONArray4.getInt(i7)));
            }
            this.reciters.add(reciter);
        }
        if (this.all.size() != 6236) {
            throw new IOException("Incomplete Quran");
        }
    }

    public File audio(Context context, Reciter reciter, Ayah ayah) {
        return new File(context.getFilesDir(), String.format(Locale.ROOT, "recitations/%s/%03d/%s.mp3", reciter.id, Integer.valueOf(ayah.surah), reciter.byAyah() ? String.format(Locale.ROOT, "%03d", Integer.valueOf(ayah.number)) : "surah"));
    }

    public String url(Reciter reciter, Ayah ayah) {
        return "https://cdn.islamic.network/quran/" + (reciter.byAyah() ? "audio/" : "audio-surah/") + reciter.bitrate + "/" + reciter.cdnId + "/" + (reciter.byAyah() ? ayah.id : ayah.surah) + ".mp3";
    }

    public boolean complete(Context context, Reciter reciter, int i) {
        Iterator<Ayah> it = this.surahs.get(i).iterator();
        while (it.hasNext()) {
            if (audio(context, reciter, it.next()).length() == 0) {
                return false;
            }
            if (!reciter.byAyah()) {
                return true;
            }
        }
        return true;
    }

    /** The CDN catalog may omit individual ayahs from an otherwise listed edition. */
    public boolean supportsSurah(Reciter reciter, int surah) {
        List<Ayah> verses = this.surahs.get(surah);
        if (verses.isEmpty()) return false;
        if (!reciter.byAyah()) return reciter.available.contains(surah);
        for (Ayah verse : verses) {
            if (!reciter.available.contains(verse.id)) return false;
        }
        return true;
    }

    public String display(Ayah ayah) {
        String strReplace = ayah.text.replace("\ufeff", "");
        String strReplace2 = this.all.get(0).text.replace("\ufeff", "");
        return (ayah.number != 1 || ayah.surah == 1 || ayah.surah == 9 || !strReplace.startsWith(new StringBuilder().append(strReplace2).append(" ").toString())) ? strReplace : strReplace.substring(strReplace2.length() + 1);
    }
}
