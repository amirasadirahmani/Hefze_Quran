package ir.hefze.quran;

import android.content.Context;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
import org.json.*;

/** Small bundled index; only the Yasin learning pack is included in this MVP. */
public final class SurahCatalog {
    public static final class Surah {
        public final int number,ayahs,firstJuz,lastJuz;
        public final String name;
        public final boolean available;
        Surah(JSONObject o) throws JSONException {
            number=o.getInt("number");ayahs=o.getInt("ayahs");firstJuz=o.getInt("firstJuz");lastJuz=o.getInt("lastJuz");
            name=o.getString("name");available=o.getBoolean("available");
        }
    }
    private final List<Surah> all=new ArrayList<>();
    public SurahCatalog(Context context) throws Exception {
        try(InputStream in=context.getAssets().open("content/surahs.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            JSONArray a=new JSONObject(out.toString("UTF-8")).getJSONArray("surahs");
            for(int i=0;i<a.length();i++)all.add(new Surah(a.getJSONObject(i)));
        }
    }
    public List<Surah> filter(String term,int juz){
        String q=normalize(term);List<Surah> result=new ArrayList<>();
        for(Surah s:all)if((juz==0||(s.firstJuz<=juz&&s.lastJuz>=juz))
            &&(q.isEmpty()||normalize(s.name).contains(q)||(s.number==36&&normalize("یاسین").contains(q))||String.valueOf(s.number).equals(q)))result.add(s);
        return result;
    }
    private static String normalize(String value){
        String s=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace("ـ","").replace('ی','ي').replace('ک','ك').replace('ٱ','ا').replace('ة','ه').replace('ى','ي');
        for(int i=0;i<10;i++)s=s.replace((char)('۰'+i),(char)('0'+i)).replace((char)('٠'+i),(char)('0'+i));
        return s.trim();
    }
}
