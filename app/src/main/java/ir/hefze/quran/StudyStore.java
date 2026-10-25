package ir.hefze.quran;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.LocalDate;
import java.util.*;

/** Profile/preferences and progress stay exclusively in app-private SQLite. */
public final class StudyStore extends SQLiteOpenHelper {
    private static final int SCHEMA_VERSION = 1;
    private long session=-1;
    public StudyStore(Context context) { super(context,"study.db",null,SCHEMA_VERSION); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        db.execSQL("CREATE TABLE verses (ayah INTEGER PRIMARY KEY, mastered INTEGER NOT NULL DEFAULT 0, due TEXT)");
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, started INTEGER NOT NULL, day TEXT NOT NULL, listened_ms INTEGER NOT NULL DEFAULT 0, plays INTEGER NOT NULL DEFAULT 0, manual INTEGER NOT NULL DEFAULT 0)");
    }
    // TODO: Define and test a v1-to-v2 migration before bumping SCHEMA_VERSION.
    // No v2 schema exists yet. When bumping SCHEMA_VERSION, add a transactional,
    // data-preserving migration here and test upgrading a populated v1 database.
    // Never drop user progress or silently accept an unknown schema version.
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        throw new IllegalStateException("Missing study.db migration from " + oldVersion + " to " + newVersion);
    }
    public String get(String key,String fallback) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT value FROM settings WHERE key=?",new String[]{key})) {return c.moveToFirst()?c.getString(0):fallback;}
    }
    public int number(String key,int fallback) {try{return Integer.parseInt(get(key,""));}catch(NumberFormatException e){return fallback;}}
    public boolean flag(String key) {return "true".equals(get(key,"false"));}
    public void put(String key,Object value) {ContentValues v=new ContentValues();v.put("key",key);v.put("value",String.valueOf(value));getWritableDatabase().insertWithOnConflict("settings",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private long session() {
        String day=LocalDate.now().toString();
        if(session!=-1) {
            try(Cursor c=getReadableDatabase().rawQuery("SELECT day FROM sessions WHERE id=?",new String[]{String.valueOf(session)})) {
                if(!c.moveToFirst() || !day.equals(c.getString(0)))session=-1;
            }
        }
        if(session==-1){ContentValues v=new ContentValues();v.put("started",System.currentTimeMillis());v.put("day",day);session=getWritableDatabase().insertOrThrow("sessions",null,v);}
        return session;
    }
    public void endSession(){session=-1;}
    public void listened(long ms){if(ms>0)getWritableDatabase().execSQL("UPDATE sessions SET listened_ms=listened_ms+? WHERE id=?",new Object[]{ms,session()});}
    public void completed(){getWritableDatabase().execSQL("UPDATE sessions SET plays=plays+1 WHERE id=?",new Object[]{session()});}
    public void manual(){getWritableDatabase().execSQL("UPDATE sessions SET manual=manual+1 WHERE id=?",new Object[]{session()});}
    public void mark(int ayah,boolean mastered,boolean tomorrow) {
        ContentValues v=new ContentValues();v.put("ayah",ayah);v.put("mastered",mastered?1:0);
        if(tomorrow)v.put("due",LocalDate.now().plusDays(1).toString());else v.putNull("due");
        getWritableDatabase().insertWithOnConflict("verses",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public boolean mastered(int ayah){try(Cursor c=getReadableDatabase().rawQuery("SELECT mastered FROM verses WHERE ayah=?",new String[]{""+ayah})){return c.moveToFirst()&&c.getInt(0)==1;}}
    public List<Integer> due(){List<Integer> list=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT ayah FROM verses WHERE due<=? ORDER BY ayah",new String[]{LocalDate.now().toString()})){while(c.moveToNext())list.add(c.getInt(0));}return list;}
    public long scalar(String sql){try(Cursor c=getReadableDatabase().rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    public long[] dailyMinutes(){long[] days=new long[7];for(int i=0;i<7;i++){try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(listened_ms),0) FROM sessions WHERE day=?",new String[]{LocalDate.now().minusDays(6-i).toString()})){if(c.moveToFirst())days[i]=c.getLong(0);}}return days;}
}
