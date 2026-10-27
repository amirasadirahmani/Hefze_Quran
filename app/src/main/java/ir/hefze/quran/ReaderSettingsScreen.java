package ir.hefze.quran;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.List;
import java.util.function.Consumer;

/** Full-screen settings. Disabled roadmap options cannot be selected. */
final class ReaderSettingsScreen extends LinearLayout {
    static final class Values {
        int reciter, size, repeats, gap;
        String font;
        Values(int r, int s, int n, int g, String f) { reciter=r; size=s; repeats=n; gap=g; font=f; }
        Bundle bundle() {
            Bundle b=new Bundle(); b.putInt("reciter",reciter); b.putInt("size",size);
            b.putInt("repeats",repeats); b.putInt("gap",gap); b.putString("font",font); return b;
        }
        static Values from(Bundle b) { return new Values(b.getInt("reciter"),b.getInt("size",28),b.getInt("repeats",3),b.getInt("gap",3),b.getString("font","ScheherazadeNew-Regular.ttf")); }
    }
    private final Spinner reciters, fonts;
    private final SeekBar size, repeats, gap;
    private final Typeface ui;
    ReaderSettingsScreen(Context c, List<QuranBook.Reciter> editions, Values initial, Consumer<Values> save, Runnable back) {
        super(c); ui=c.getResources().getFont(R.font.vazirmatn_regular);
        setTag("settings_screen"); setOrientation(VERTICAL); setLayoutDirection(LAYOUT_DIRECTION_RTL);
        setBackgroundColor(0xfff9f7ed);
        LinearLayout heading=new LinearLayout(c); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("تنظیمات",24); title.setTag("settings_title"); title.setTypeface(ui,Typeface.BOLD);
        heading.addView(title,new LayoutParams(0,dp(64),1));
        Button close=new Button(c); close.setText("بازگشت"); close.setTypeface(ui); close.setTextColor(0xff415c3b); close.setBackgroundColor(0);
        close.setTag("settings_back"); close.setOnClickListener(v->back.run()); heading.addView(close,new LayoutParams(dp(88),dp(56)));
        addView(heading,new LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(c); scroll.setFillViewport(true);
        LinearLayout body=column(); body.setPadding(dp(16),0,dp(16),dp(16)); scroll.addView(body);
        addView(scroll,new LayoutParams(-1,0,1));
        LinearLayout appearance=card(body,"تنظیمات ظاهری");
        appearance.addView(label("فونت متن قرآن",15));
        fonts=choice(new String[]{"شهرزاد","امیری"},-1,"font_choice");
        fonts.setSelection(initial.font.startsWith("Amiri")?1:0); appearance.addView(fonts);
        TextView preview=label("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",initial.size); preview.setGravity(Gravity.CENTER); preview.setPadding(0,dp(12),0,dp(12));
        size=slider(appearance,"اندازهٔ متن",18,42,initial.size,"font_size",v->preview.setTextSize(v));
        appearance.addView(preview);
        fonts.setOnItemSelectedListener(selection(()->preview.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/"+fontName()))));
        appearance.addView(label("تم برنامه",15));
        appearance.addView(choice(new String[]{"پیش‌فرض · سبز پسته‌ای","کودک پسرانه · به‌زودی","کودک دخترانه · به‌زودی"},0,"theme_choice"));
        appearance.addView(label("فونت رابط برنامه: وزیرمتن",13));
        LinearLayout practice=card(body,"قرائت و تمرین حفظ");
        practice.addView(label("انتخاب قاری",15));
        String[] names=new String[editions.size()]; for(int i=0;i<names.length;i++) names[i]=editions.get(i).toString();
        reciters=choice(names,-1,"reciter_choice"); reciters.setSelection(initial.reciter); practice.addView(reciters);
        TextView mode=label("",13); practice.addView(mode);
        repeats=slider(practice,"تعداد قرائت هر آیه",1,10,initial.repeats,"repeat_count",v->{});
        gap=slider(practice,"مکث بین قرائت‌ها (ثانیه)",0,30,initial.gap,"gap_seconds",v->{});
        reciters.setOnItemSelectedListener(selection(()->{
            boolean ayah=editions.get(reciters.getSelectedItemPosition()).byAyah();
            repeats.setEnabled(ayah); gap.setEnabled(ayah);
            mode.setText(ayah ? "تعداد شامل قرائت اول است؛ مکث برای تکرار شماست." : "صوت یکپارچهٔ سوره؛ تکرار و مکث آیه‌به‌آیه برای این قاری در دسترس نیست.");
        }));
        practice.addView(label("ترجمه",15));
        Spinner translation=choice(new String[]{"انتخاب ترجمه · به‌زودی"},0,"translation_choice"); translation.setEnabled(false); translation.setAlpha(.5f); practice.addView(translation);
        LinearLayout methods=card(body,"روش حفظ قرآن");
        Spinner method=choice(new String[]{"حفظ موضوعی · به‌زودی","حفظ به روش کلام‌محور · به‌زودی","حفظ معنامحور · به‌زودی","حفظ به روش صوتی","حفظ سطرمحور · به‌زودی","حفظ سوره‌محور · به‌زودی","حفظ به روش تصویری · به‌زودی"},3,"method_choice");
        methods.addView(method); methods.addView(label("فعلاً روش صوتی فعال است.",13));
        OrnamentButton apply=new OrnamentButton(c); apply.setText("ذخیرهٔ تنظیمات"); apply.setTag("settings_save");
        apply.setOnClickListener(v->save.accept(values()));
        LayoutParams saveParams=new LayoutParams(-1,dp(56)); saveParams.setMargins(dp(24),dp(4),dp(24),dp(12)); addView(apply,saveParams);
    }
    Values values() { return new Values(reciters.getSelectedItemPosition(),size.getProgress()+18,repeats.getProgress()+1,gap.getProgress(),fontName()); }
    private String fontName() { return fonts.getSelectedItemPosition()==1?"Amiri-Regular.ttf":"ScheherazadeNew-Regular.ttf"; }
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v=new LinearLayout(getContext()); v.setOrientation(VERTICAL); return v; }
    private TextView label(String value,int sp) {
        TextView t=new TextView(getContext()); t.setText(value); t.setTextSize(sp); t.setTypeface(ui); t.setTextColor(0xff33492e);
        t.setTextDirection(TEXT_DIRECTION_RTL); t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); t.setPadding(dp(12),dp(8),dp(12),dp(8)); return t;
    }
    private GradientDrawable background(int color) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(14)); d.setStroke(dp(1),0xffd9dfc8); return d;
    }
    private LinearLayout card(LinearLayout parent,String title) {
        LinearLayout v=column(); v.setPadding(dp(10),dp(8),dp(10),dp(12)); v.setBackground(background(0xfffffef8));
        TextView h=label(title,19); h.setTypeface(ui,Typeface.BOLD); v.addView(h);
        LayoutParams p=new LayoutParams(-1,-2); p.bottomMargin=dp(16); parent.addView(v,p); return v;
    }
    private SeekBar slider(LinearLayout parent,String name,int min,int max,int value,String tag,Consumer<Integer> change) {
        TextView caption=label(name+": "+MainActivity.fa(value),14); parent.addView(caption);
        SeekBar bar=new SeekBar(getContext()); bar.setTag(tag); bar.setMax(max-min); bar.setProgress(value-min);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff6e8754)); bar.setThumbTintList(android.content.res.ColorStateList.valueOf(0xff6e8754));
        parent.addView(bar,new LayoutParams(-1,dp(48)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){caption.setText(name+": "+MainActivity.fa(p+min));change.accept(p+min);}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        }); return bar;
    }
    private AdapterView.OnItemSelectedListener selection(Runnable action) {
        return new AdapterView.OnItemSelectedListener(){ public void onItemSelected(AdapterView<?> a,View v,int p,long id){action.run();} public void onNothingSelected(AdapterView<?> a){} };
    }
    private Spinner choice(String[] names,int onlyEnabled,String tag) {
        Spinner s=new Spinner(getContext(),Spinner.MODE_DROPDOWN); s.setTag(tag); s.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        s.setTextDirection(TEXT_DIRECTION_RTL); s.setGravity(Gravity.RIGHT); s.setBackground(background(0xfff4f6eb));
        s.setAdapter(new BaseAdapter(){
            public int getCount(){return names.length;} public Object getItem(int p){return names[p];} public long getItemId(int p){return p;}
            public boolean areAllItemsEnabled(){return onlyEnabled<0;} public boolean isEnabled(int p){return onlyEnabled<0||p==onlyEnabled;}
            private View item(int p, boolean dropdown){TextView t=label(names[p],15);t.setMinHeight(dp(56));t.setAlpha(isEnabled(p)?1f:.4f);if(!dropdown)t.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_down_float,0,0,0);return t;}
            public View getView(int p,View old,ViewGroup parent){return item(p,false);}
            public View getDropDownView(int p,View old,ViewGroup parent){return item(p,true);}
        });
        s.setLayoutParams(new LayoutParams(-1,-2));
        if(onlyEnabled>=0){s.setSelection(onlyEnabled);s.setOnItemSelectedListener(selection(()->{if(s.getSelectedItemPosition()!=onlyEnabled)s.setSelection(onlyEnabled);}));}
        return s;
    }
}
