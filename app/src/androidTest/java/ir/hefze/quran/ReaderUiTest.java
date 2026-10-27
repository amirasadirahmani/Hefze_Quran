package ir.hefze.quran;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Spinner;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.is;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public final class ReaderUiTest {
    private Context context;
    private SharedPreferences prefs;
    @Before public void fresh() {
        context=ApplicationProvider.getApplicationContext();
        prefs=context.getSharedPreferences(FullQuranActivity.class.getName(),0);
        assertTrue(prefs.edit().clear().commit());
        remove(new File(context.getFilesDir(),"recitations"));
    }
    private static void remove(File f) { if(f.isDirectory()){File[] files=f.listFiles();if(files!=null)for(File child:files)remove(child);} if(f.exists())assertTrue(f.delete()); }
    private static void tap(String tag) { onView(withTagValue(is((Object)tag))).perform(click()); }
    private static <T extends View> T view(FullQuranActivity a,String tag) { T v=a.getWindow().getDecorView().findViewWithTag(tag); assertNotNull(tag,v); return v; }
    private static float x(View v) { int[] xy=new int[2];v.getLocationOnScreen(xy);return xy[0]; }
    private void screenshot(String name) throws Exception {
        try (android.os.ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("screencap -p /sdcard/Download/hefze-ui-"+name+".png");
             java.io.FileInputStream in = new java.io.FileInputStream(fd.getFileDescriptor())) {
            byte[] buffer=new byte[1024]; while(in.read(buffer)!=-1) { }
        }
    }
    @Test public void freshStartSettingsAndRealPageClicks() throws Exception {
        try(ActivityScenario<FullQuranActivity> a=ActivityScenario.launch(FullQuranActivity.class)) {
            onView(withTagValue(is((Object)"reading_entry"))).check(matches(withText("شروع مطالعه")));
            tap("settings_button"); tap("settings_save"); assertFalse(prefs.contains("ayah"));
            tap("reading_entry"); assertEquals(1,prefs.getInt("page",0)); assertEquals(1,prefs.getInt("ayah",0));
            a.onActivity(v->{assertFalse(view(v,"page_previous").isEnabled());assertTrue(x(view(v,"page_next"))<x(view(v,"page_previous")));});
            tap("page_next"); assertEquals(2,prefs.getInt("page",0)); assertEquals(8,prefs.getInt("ayah",0));
            tap("page_previous"); assertEquals(1,prefs.getInt("page",0)); screenshot("fresh-reader");
        }
    }
    @Test public void pageBoundary439440441AndLastPage() {
        prefs.edit().putBoolean("has_reading",true).putInt("ayah",3699).commit();
        try(ActivityScenario<FullQuranActivity> a=ActivityScenario.launch(FullQuranActivity.class)) {
            tap("reading_entry"); assertEquals(439,prefs.getInt("page",0));
            tap("page_next"); assertEquals(440,prefs.getInt("page",0)); assertEquals(3705,prefs.getInt("ayah",0));
            tap("page_next"); assertEquals(441,prefs.getInt("page",0)); assertEquals(3718,prefs.getInt("ayah",0));
            tap("page_previous"); assertEquals(440,prefs.getInt("page",0));
        }
        prefs.edit().putInt("ayah",6236).commit();
        try(ActivityScenario<FullQuranActivity> a=ActivityScenario.launch(FullQuranActivity.class)) {
            tap("reading_entry"); a.onActivity(v->assertFalse(view(v,"page_next").isEnabled()));
        }
    }
    @Test public void settingsRtlDisabledOptionsPersistenceAndRecreation() throws Exception {
        try(ActivityScenario<FullQuranActivity> a=ActivityScenario.launch(FullQuranActivity.class)) {
            tap("settings_button");
            a.onActivity(v->{
                TextView title=view(v,"settings_title");assertEquals(Gravity.RIGHT,title.getGravity()&Gravity.HORIZONTAL_GRAVITY_MASK);
                Spinner reciter=view(v,"reciter_choice");assertEquals(View.LAYOUT_DIRECTION_RTL,reciter.getLayoutDirection());
                assertFalse(reciter.getSelectedItem().toString().contains("ar."));
                Spinner theme=view(v,"theme_choice");assertFalse(((android.widget.ListAdapter)theme.getAdapter()).isEnabled(1));
                Spinner method=view(v,"method_choice");assertEquals(3,method.getSelectedItemPosition());
                assertFalse(((android.widget.ListAdapter)method.getAdapter()).isEnabled(0));
                assertFalse(view(v,"translation_choice").isEnabled());
                ((SeekBar)view(v,"repeat_count")).setProgress(3); ((SeekBar)view(v,"gap_seconds")).setProgress(7);
                ((Spinner)view(v,"font_choice")).setSelection(1);
                assertFalse(((android.widget.Switch)view(v,"continue_locked")).isChecked());
                ((android.widget.Switch)view(v,"continue_locked")).setChecked(true);
            });
            a.recreate();
            a.onActivity(v->{assertEquals(3,((SeekBar)view(v,"repeat_count")).getProgress());assertEquals(7,((SeekBar)view(v,"gap_seconds")).getProgress());});
            screenshot("settings");
            onView(withTagValue(is((Object)"reciter_choice"))).perform(scrollTo()); screenshot("settings-reciter");
            onView(withTagValue(is((Object)"method_choice"))).perform(scrollTo()); screenshot("settings-methods");
            tap("settings_save");
            assertEquals(4,prefs.getInt("repeat_count",0)); assertEquals(7,prefs.getInt("gap_seconds",0));
            assertEquals("Amiri-Regular.ttf",prefs.getString("quran_font",""));
            assertTrue(prefs.getBoolean("continue_locked",false)); assertFalse(prefs.contains("ayah"));
        }
    }
    private static void assertArtwork(View v) {
        assertNotNull(v.getBackground()); assertTrue(v.getWidth()>0&&v.getHeight()>0);
        // Inspect the actual composited screen. View.draw() into a software canvas can
        // omit an already hardware-rendered TextView/background on these emulator builds.
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(b);
        int[] xy=new int[2];v.getLocationOnScreen(xy);
        int green=0,area=0;
        for(int y=Math.max(0,xy[1]);y<Math.min(b.getHeight(),xy[1]+v.getHeight());y++)
            for(int x=Math.max(0,xy[0]);x<Math.min(b.getWidth(),xy[0]+v.getWidth());x++){
                area++;int c=b.getPixel(x,y);
                if(Color.alpha(c)>100&&Color.green(c)>Color.blue(c)+8&&Color.green(c)<190)green++;
            }
        b.recycle();assertTrue("Button artwork must be visible in screenshot",area>0&&green>area/8);
    }
    @Test public void downloadArtworkAndIdleStopHidden() throws Exception {
        try(ActivityScenario<FullQuranActivity> a=ActivityScenario.launch(FullQuranActivity.class)) {
            tap("reading_entry");screenshot("download-button");a.onActivity(v->assertArtwork(view(v,"download_audio")));
            tap("download_audio");screenshot("download-dialog");
            onView(withTagValue(is((Object)"download_start"))).check((v,e)->{if(e!=null)throw e;assertArtwork(v);});
            onView(withTagValue(is((Object)"download_stop"))).check(matches(withEffectiveVisibility(Visibility.GONE)));
            screenshot("download-dialog");pressBack();
        }
    }
    @Test public void ayahControlsPhysicalOrder() throws Exception {
        // Presence fixtures test navigation only; these are not playable MP3s.
        QuranBook book=new QuranBook(context);QuranBook.Reciter reciter=null;
        for(QuranBook.Reciter r:book.reciters)if(r.id.equals("ar.alafasy"))reciter=r;
        assertNotNull(reciter);
        for(QuranBook.Ayah verse:book.surahs.get(1)){
            File f=book.audio(context,reciter,verse);f.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(f)){out.write(1);}
        }
        try(ActivityScenario<FullQuranActivity> a=ActivityScenario.launch(FullQuranActivity.class)) {
            tap("reading_entry");a.onActivity(v->assertTrue(x(view(v,"ayah_next"))<x(view(v,"ayah_previous"))));
            tap("ayah_next");assertEquals(2,prefs.getInt("ayah",0));tap("ayah_previous");assertEquals(1,prefs.getInt("ayah",0));
        }
    }
    @Test public void openingIsBeforeManualAyahAndTawbahHasNoBasmala() {
        assertEquals(2,OpeningRecitation.phrases(36,1).length);
        assertEquals(1,OpeningRecitation.phrases(36,12).length);
        assertEquals(1,OpeningRecitation.phrases(9,1).length);
        assertTrue(OpeningRecitation.phrases(36,1)[0].contains("أَعُوذُ"));
    }
    @Test public void all604ReferencePages() throws Exception {
        QuranBook book=new QuranBook(context);
        for(int page=1;page<=604;page++) assertEquals(book.pages.get(page).get(0).id,book.pageIndex.firstAyah(page));
    }
}
