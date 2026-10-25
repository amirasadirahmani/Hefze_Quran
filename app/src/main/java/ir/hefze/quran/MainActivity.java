package ir.hefze.quran;

import android.Manifest;
import android.app.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import java.util.*;
import ir.hefze.quran.core.*;

public final class MainActivity extends Activity {
    private static final int GREEN=0xff607b43,INK=0xff263521,CREAM=0xfffaf8eb;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private StudyStore store;
    private QuranRepository repository;
    private AudioController audio;
    private AudioDownloads downloads;
    private TextView downloadStatus;
    private Button downloadAll,downloadOne,cancelDownload,fullscreenExit;
    private SurahCatalog catalog;
    private Typeface quranFont;
    private LinearLayout root,readerTools,topBar;
    private QuranPageView page;
    private TextView status,positionLabel,pageLabel;
    private Button playButton,hideButton,masterButton;
    private ProgressBar playbackProgress;
    private boolean hidden,fullscreen;
    private String screen="splash";
    private int lastVerse=-1,lastWord=-2,lastRead=-2;
    public static String fa(int value){return fa(String.valueOf(value));}
    public static String fa(String value){StringBuilder s=new StringBuilder();for(char c:value.toCharArray())s.append(c>='0'&&c<='9'?(char)('۰'+c-'0'):c);return s.toString();}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        store=new StudyStore(this);
        try {repository=new QuranRepository(this);}catch(Exception e){base();label(root,"بستهٔ آیات قابل خواندن نیست. نصب برنامه را بررسی کنید.",20);return;}
        audio=new AudioController(this,repository,store,this::updatePlayback);
        downloads=new AudioDownloads(repository,this::updateDownloads);
        quranFont=Typeface.createFromAsset(getAssets(),"fonts/ScheherazadeNew-Regular.ttf");
        StudyMethod method=StudyMethod.safe(store.get("method","AUDIO"));
        audio.queue.defaults(method.repeats,method.gap);
        for(int i=1;i<=83;i++)audio.queue.configure(i,clamp(store.number("repeat."+i,method.repeats),1,10),clamp(store.number("gap."+i,method.gap),0,30));
        int first=clamp(store.number("range.first",1),1,83),last=clamp(store.number("range.last",83),first,83);
        audio.queue.range(first,last);audio.queue.select(clamp(store.number("ayah",first),first,last),false);
        audio.restorePosition(store.number("position",0));hidden=store.flag("hidden");
        base();root.setGravity(Gravity.CENTER);label(root,"بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",22);label(root,"حفظ یاسین",38);label(root,"هر روز، فرصتی برای حفظ و مرور",16);
        handler.postDelayed(()->{if(!isFinishing()&&!isDestroyed()){if(store.get("name","").isEmpty())profile(false);else surahs();}},1800);
        ReminderReceiver.schedule(this,store.flag("reminder"));
    }
    private static int clamp(int n,int min,int max){return Math.max(min,Math.min(max,n));}
    private void base(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(CREAM);root.setPadding(dp(5),dp(4),dp(5),dp(4));
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int top=insets.getSystemWindowInsetTop(),bottom=insets.getSystemWindowInsetBottom();
            v.setPadding(dp(5)+insets.getSystemWindowInsetLeft(),dp(4)+top,dp(5)+insets.getSystemWindowInsetRight(),dp(4)+bottom);return insets;
        });
        setContentView(root);root.requestApplyInsets();
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    private LinearLayout row(LinearLayout parent){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);parent.addView(l,new LinearLayout.LayoutParams(-1,-2));return l;}
    private LinearLayout scroll(){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout content=column();content.setPadding(0,dp(8),0,dp(16));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return content;}
    private TextView label(LinearLayout parent,String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextColor(INK);text.setTextSize(size);text.setGravity(Gravity.CENTER);text.setPadding(dp(4),dp(6),dp(4),dp(6));text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);parent.addView(text,new LinearLayout.LayoutParams(-1,-2));return text;}
    private Button button(LinearLayout parent,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(GREEN);b.setMinHeight(dp(48));b.setPadding(dp(4),0,dp(4),0);b.setBackgroundTintList(ColorStateList.valueOf(0xffe8efd6));b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=parent.getOrientation()==LinearLayout.HORIZONTAL?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(2),dp(2),dp(2),dp(2));parent.addView(b,lp);return b;}
    private void primary(Button b){b.setBackgroundTintList(null);b.setTextColor(Color.WHITE);GradientDrawable d=new GradientDrawable();d.setColor(GREEN);d.setCornerRadius(dp(4));b.setBackground(d);}
    private EditText input(LinearLayout parent,String hint,String value,boolean number){EditText field=new EditText(this);field.setHint(hint);field.setText(value);field.setTextSize(18);field.setSingleLine();field.setInputType(number?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);parent.addView(field,new LinearLayout.LayoutParams(-1,dp(58)));return field;}
    private void profile(boolean editing){
        screen="profile";if(audio!=null)audio.pauseNow();base();label(root,editing?"روش و مشخصات من":"به مسیر حفظ خوش آمدید",25);LinearLayout content=scroll();
        label(content,"مشخصات فقط روی همین دستگاه ذخیره می‌شود.",15);
        EditText name=input(content,"نام",store.get("name",""),false),age=input(content,"سن",store.get("age",""),true);
        label(content,"روش حفظ",20);Spinner method=new Spinner(this);String[] options=Arrays.stream(StudyMethod.values()).map(m->m.title).toArray(String[]::new);
        method.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));method.setSelection(StudyMethod.safe(store.get("method","AUDIO")).ordinal());content.addView(method,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView description=label(content,"",16);method.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){description.setText(StudyMethod.values()[i].description);}});
        if(editing)label(content,"ذخیرهٔ روش، تکرار و مکث همهٔ آیات را به پیش‌فرض آن روش برمی‌گرداند.",14);
        primary(button(content,"ذخیره و شروع",()->{
            String n=name.getText().toString().trim();int a;try{a=Integer.parseInt(age.getText().toString());}catch(Exception e){a=-1;}
            if(n.isEmpty()||n.length()>60){name.setError("نام را بین ۱ تا ۶۰ نویسه وارد کنید.");return;}
            if(a<3||a>120){age.setError("سن را بین ۳ تا ۱۲۰ وارد کنید.");return;}
            StudyMethod selected=StudyMethod.values()[method.getSelectedItemPosition()];store.put("name",n);store.put("age",a);store.put("method",selected.name());
            audio.queue.defaults(selected.repeats,selected.gap);
            for(int i=1;i<=83;i++){store.put("repeat."+i,selected.repeats);store.put("gap."+i,selected.gap);}
            hidden=selected.hidden;store.put("hidden",hidden);reader();
        }));
        if(editing)button(content,"بازگشت",this::reader);
    }
    private void frameRoot(){
        base();LinearLayout outer=root;root=column();outer.addView(root,new LinearLayout.LayoutParams(-1,0,1));
    }
    private TextView compact(LinearLayout parent,String text,int size){TextView v=label(parent,text,size);v.setPadding(dp(2),dp(1),dp(2),dp(1));return v;}
    private void reader(){
        screen="reader";frameRoot();lastVerse=-1;lastWord=-2;lastRead=-2;masterButton=null;
        topBar=column();topBar.setPadding(dp(28),dp(18),dp(28),dp(18));root.addView(topBar,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout title=row(topBar);button(title,"فهرست",this::surahs);
        TextView name=compact(title,"سورة يس",28);name.setTypeface(quranFont);name.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1.4f));
        button(title,"بیشتر",this::readerMenu);
        LinearLayout heading=row(topBar);TextView basmala=compact(heading,repository.basmala,24);basmala.setTypeface(quranFont);basmala.setLayoutParams(new LinearLayout.LayoutParams(0,-2,4));
        pageLabel=compact(heading,"",12);pageLabel.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        ScrollView paper=new ScrollView(this);paper.setFillViewport(false);paper.setPadding(dp(10),0,dp(10),0);paper.setClipToPadding(true);
        page=new QuranPageView(this,repository,this::selectVerse);paper.addView(page,new ScrollView.LayoutParams(-1,-2));root.addView(paper,new LinearLayout.LayoutParams(-1,0,1));
        fullscreenExit=button(root,"بازگشت از تمام‌صفحه",()->setFullscreen(false));fullscreenExit.setVisibility(View.GONE);
        readerTools=column();readerTools.setPadding(dp(28),dp(18),dp(28),dp(26));root.addView(readerTools,new LinearLayout.LayoutParams(-1,-2));
        positionLabel=compact(readerTools,"",12);status=compact(readerTools,"",12);status.setMinHeight(dp(32));status.setMaxLines(2);
        playbackProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);playbackProgress.setProgressTintList(ColorStateList.valueOf(GREEN));readerTools.addView(playbackProgress,new LinearLayout.LayoutParams(-1,dp(3)));
        LinearLayout transport=row(readerTools);button(transport,"آیهٔ قبل",()->move(-1));playButton=button(transport,"پخش",()->{if(audio.queue.running()||repository.hasAudio(audio.queue.ayah()))audio.toggle();else downloadPrompt();});primary(playButton);button(transport,"آیهٔ بعد",()->move(1));
        LinearLayout navigation=row(readerTools);button(navigation,"صفحهٔ قبل",()->browsePage(-1));hideButton=button(navigation,"آزمون",()->{hidden=!hidden;store.put("hidden",hidden);lastWord=-2;updatePlayback();});button(navigation,"صفحهٔ بعد",()->browsePage(1));
        root.setBackground(new MushafFrameDrawable(this,topBar,readerTools));setFullscreen(false);updatePlayback();
    }
    private void readerMenu(){
        String[] items={"دریافت صوت","انتخاب آیه","تکرار و مکث","تکرار همین آیه","خودم تکرار کردم",store.mastered(audio.queue.ayah())?"برداشتن علامت حفظ":"حفظ کردم","مرور فردا","تمام‌صفحه","برنامهٔ تمرین","پیشرفت","تنظیمات"};
        new AlertDialog.Builder(this).setTitle("تمرین یاسین").setItems(items,(d,w)->{
            switch(w){
                case 0:downloadScreen();break;
                case 1:chooseVerse();break;
                case 2:playSettings();break;
                case 3:if(repository.hasAudio(audio.queue.ayah())){audio.clearSeek();audio.queue.replay();}else downloadPrompt();break;
                case 4:store.manual();toast("یک تکرار شخصی ثبت شد.");break;
                case 5:store.mark(audio.queue.ayah(),!store.mastered(audio.queue.ayah()),false);updatePlayback();break;
                case 6:store.mark(audio.queue.ayah(),false,true);toast("برای مرور فردا ثبت شد.");break;
                case 7:setFullscreen(true);break;
                case 8:plan();break;
                case 9:progress();break;
                case 10:settings();break;
            }
        }).show();
    }
    private void downloadPrompt(){
        new AlertDialog.Builder(this).setTitle("دریافت صوت مشاری العفاسی")
            .setMessage("صوت این آیه هنوز دانلود نشده است. پس از دریافت، بدون اینترنت هم پخش می‌شود.")
            .setPositiveButton("دریافت این آیه",(d,w)->{downloadScreen();downloads.download(audio.queue.ayah(),audio.queue.ayah());})
            .setNeutralButton("دریافت کل سوره",(d,w)->{downloadScreen();downloads.download(1,83);})
            .setNegativeButton("بازگشت",null).show();
    }
    private void downloadScreen(){
        screen="downloads";audio.pauseNow();base();label(root,"صوت سورهٔ یاسین",26);LinearLayout c=scroll();
        label(c,"مشاری العفاسی • ۶۴ کیلوبیت",20);
        label(c,"منبع: EveryAyah\nدریافت صوت اختیاری است؛ مطالعهٔ متن به اینترنت نیاز ندارد. هنگام دانلود برنامه را باز نگه دارید. فایل‌های کامل برای استفادهٔ آفلاین ذخیره می‌شوند.",16);
        downloadStatus=label(c,"",17);
        downloadOne=button(c,"دریافت آیهٔ "+fa(audio.queue.ayah()),()->downloads.download(audio.queue.ayah(),audio.queue.ayah()));
        downloadAll=button(c,"دریافت کل یاسین",()->downloads.download(1,83));primary(downloadAll);
        cancelDownload=button(c,"توقف دانلود",downloads::cancel);
        button(c,"بازگشت به مطالعه",this::reader);updateDownloads();
    }
    private void updateDownloads(){
        if(downloads==null)return;
        if("downloads".equals(screen)&&downloadStatus!=null){
            int count=repository.downloadedCount();downloadStatus.setText(fa(count)+" از ۸۳ آیه آمادهٔ پخش\n"+downloads.message());
            downloadOne.setEnabled(!downloads.running()&&!repository.hasAudio(audio.queue.ayah()));downloadAll.setEnabled(!downloads.running()&&count<83);cancelDownload.setEnabled(downloads.running());
        }
        updatePlayback();
    }
    private void surahs(){
        screen="surahs";audio.pauseNow();frameRoot();
        try{if(catalog==null)catalog=new SurahCatalog(this);}catch(Exception e){label(root,"فهرست سوره‌ها خوانده نشد.",18);button(root,"مطالعهٔ یاسین",this::reader);return;}
        LinearLayout header=column();header.setPadding(dp(28),dp(18),dp(28),dp(20));root.addView(header,new LinearLayout.LayoutParams(-1,-2));
        TextView title=compact(header,"قرآن کریم",30);title.setTypeface(quranFont);
        LinearLayout actions=row(header);button(actions,"درباره",()->new AlertDialog.Builder(this).setTitle("حفظ قرآن").setMessage("نسخهٔ فعلی برای تمرین سورهٔ یاسین آماده است. فهرست کامل قرآن برای آشنایی با سوره‌ها و جزءها نمایش داده می‌شود.").setPositiveButton("بستن",null).show());button(actions,"پیشرفت",this::progress);button(actions,"تنظیمات",this::settings);
        LinearLayout body=row(root);body.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));body.setPadding(dp(22),0,dp(22),0);body.setGravity(Gravity.TOP);
        LinearLayout surahColumn=column();body.addView(surahColumn,new LinearLayout.LayoutParams(0,-1,1));
        EditText search=input(surahColumn,"جستجوی نام یا شمارهٔ سوره","",false);search.setTextSize(15);
        ListView list=new ListView(this);list.setDivider(new android.graphics.drawable.ColorDrawable(0xffd1ddba));list.setDividerHeight(dp(1));list.setCacheColorHint(Color.TRANSPARENT);surahColumn.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=compact(surahColumn,"سوره‌ای با این نام پیدا نشد.",14);empty.setVisibility(View.GONE);list.setEmptyView(empty);
        final int[] selectedJuz={0};final List<SurahCatalog.Surah> items=new ArrayList<>(catalog.filter("",0));
        BaseAdapter adapter=new BaseAdapter(){
            @Override public int getCount(){return items.size();}
            @Override public Object getItem(int position){return items.get(position);}
            @Override public long getItemId(int position){return items.get(position).number;}
            @Override public View getView(int position,View reusable,ViewGroup parent){
                SurahCatalog.Surah entry=items.get(position);LinearLayout cell=column();cell.setPadding(dp(8),dp(8),dp(8),dp(8));
                cell.setBackgroundColor(entry.available?0xffe5edce:Color.TRANSPARENT);
                TextView name=compact(cell,entry.name,25);name.setTypeface(quranFont);name.setGravity(Gravity.RIGHT);name.setTextColor(entry.available?GREEN:INK);
                TextView info=compact(cell,fa(entry.number)+"  •  "+fa(entry.ayahs)+" آیه  •  "+(entry.available?"آمادهٔ مطالعه":"هنوز افزوده نشده"),11);info.setGravity(Gravity.RIGHT);
                cell.setContentDescription(entry.name+"، "+(entry.available?"مطالعه":"هنوز افزوده نشده"));return cell;
            }
        };list.setAdapter(adapter);
        Runnable filter=()->{items.clear();items.addAll(catalog.filter(search.getText().toString(),selectedJuz[0]));adapter.notifyDataSetChanged();};
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence t,int st,int count,int after){}public void onTextChanged(CharSequence t,int st,int before,int count){filter.run();}public void afterTextChanged(Editable e){}});
        list.setOnItemClickListener((p,v,pos,id)->{if(items.get(pos).available)reader();else toast("بستهٔ مطالعهٔ این سوره هنوز به برنامه اضافه نشده است.");});
        ScrollView juzScroll=new ScrollView(this);LinearLayout juz=column();juz.setPadding(dp(6),0,0,0);juzScroll.addView(juz);body.addView(juzScroll,new LinearLayout.LayoutParams(dp(74),-1));
        List<Button> juzButtons=new ArrayList<>();
        for(int i=0;i<=30;i++){final int number=i;Button b=button(juz,i==0?"همه":"جزء "+fa(i),()->{selectedJuz[0]=number;for(int j=0;j<juzButtons.size();j++)juzButtons.get(j).setBackgroundTintList(ColorStateList.valueOf(j==number?0xffcbdba8:0xffeef1df));filter.run();});b.setTextSize(12);juzButtons.add(b);}
        juzButtons.get(0).setBackgroundTintList(ColorStateList.valueOf(0xffcbdba8));
        LinearLayout footer=column();footer.setPadding(dp(28),dp(18),dp(28),dp(26));root.addView(footer,new LinearLayout.LayoutParams(-1,-2));
        compact(footer,"بستهٔ فعال: یاسین • ۸۳ آیه",13);primary(button(footer,"ادامهٔ مطالعهٔ یاسین",this::reader));
        root.setBackground(new MushafFrameDrawable(this,header,footer));
    }
    private void updatePlayback(){
        if(audio==null||page==null||!"reader".equals(screen))return;
        PlaybackQueue q=audio.queue;int verse=q.ayah();
        if(lastVerse!=verse){page.setPage(repository.verse(verse).page-1);lastVerse=verse;store.put("ayah",verse);lastWord=-2;}
        int active=audio.timeline==null?-1:audio.timeline.active(audio.position());int read=audio.timeline==null?-1:audio.timeline.readThrough(audio.position());
        if(active>=0){for(int p=0;p<repository.pages.size();p++)for(List<QuranRepository.Token> l:repository.pages.get(p))for(QuranRepository.Token t:l)if(t.ayah==verse&&t.word==active&&page.page()!=p)page.setPage(p);}
        if(lastWord!=active||lastRead!=read){page.highlight(verse,active,read,hidden);lastWord=active;lastRead=read;}
        set(pageLabel,fa(page.page()+1)+" از "+fa(repository.pages.size()));set(hideButton,hidden?"نمایش متن":"آزمون حفظ");
        set(playButton,q.running()?"مکث":repository.hasAudio(verse)?"پخش":"دریافت صوت");set(masterButton,store.mastered(verse)?"حفظ شد ✓":"حفظ کردم");
        set(positionLabel,"آیهٔ "+fa(verse)+"  •  تکرار "+fa(q.iteration())+" از "+fa(q.repeatCount())+"  •  محدودهٔ "+fa(q.first())+" تا "+fa(q.last()));
        String message;
        switch(q.state()){
            case ERROR:message=audio.error;break;
            case LOADING:message="آماده‌سازی صوت…";break;
            case GAP:message="فرصت تکرار شما: "+fa((int)Math.ceil(q.gapRemaining(SystemClock.elapsedRealtime())/1000.0))+" ثانیه";break;
            case FINISHED:message="پخش این محدوده تمام شد؛ وضعیت حفظ را خودتان ثبت کنید.";break;
            case PAUSED:message="پخش متوقف است؛ برای ادامه دکمهٔ پخش را بزنید.";break;
            case PLAYING:message=audio.timeline!=null?"هایلایت کلمات هم‌زمان با صوت":"در حال پخش • مشاری العفاسی";break;
            default:message=repository.hasAudio(verse)?"آمادهٔ تمرین":"برای شنیدن این آیه، صوت آن را دریافت کنید.";
        }
        if(downloads!=null&&downloads.running())message=downloads.message();
        set(status,message);playbackProgress.setMax(Math.max(1,audio.duration()));playbackProgress.setProgress(audio.duration()>0?audio.position():0);
    }
    private static void set(TextView view,String text){if(view!=null&&!view.getText().toString().equals(text))view.setText(text);}
    private void move(int delta){audio.clearSeek();audio.queue.move(delta);}
    private void selectVerse(int verse){
        audio.clearSeek();if(verse<audio.queue.first()||verse>audio.queue.last())audio.queue.range(1,83);audio.queue.select(verse,false);lastWord=-2;updatePlayback();saveRange();
    }
    private void browsePage(int delta){int p=clamp(page.page()+delta,0,repository.pages.size()-1);if(p==page.page())return;
        selectVerse(repository.pages.get(p).get(0).get(0).ayah);page.setPage(p);lastWord=-2;updatePlayback();
    }
    private NumberPicker picker(LinearLayout parent,int min,int max,int value){NumberPicker p=new NumberPicker(this);p.setMinValue(min);p.setMaxValue(max);p.setValue(clamp(value,min,max));p.setWrapSelectorWheel(false);p.setFormatter(MainActivity::fa);parent.addView(p,new LinearLayout.LayoutParams(-1,dp(140)));return p;}
    private void chooseVerse(){LinearLayout c=column();NumberPicker p=picker(c,1,83,audio.queue.ayah());new AlertDialog.Builder(this).setTitle("انتخاب آیه").setView(c).setPositiveButton("انتخاب",(d,w)->selectVerse(p.getValue())).setNegativeButton("بازگشت",null).show();}
    private void playSettings(){
        audio.pauseNow();LinearLayout c=column();c.setPadding(dp(16),0,dp(16),0);label(c,"تعداد تکرار صوت",16);NumberPicker repeat=picker(c,1,10,audio.queue.repeatCount());label(c,"سکوت پس از هر بار پخش (ثانیه)",16);NumberPicker gap=picker(c,0,30,audio.queue.gapSeconds());CheckBox all=new CheckBox(this);all.setText("برای همهٔ آیات اعمال شود");c.addView(all);
        new AlertDialog.Builder(this).setTitle("تکرار و مکث").setView(c).setPositiveButton("ذخیره",(d,w)->{
            int start=all.isChecked()?1:audio.queue.ayah(),end=all.isChecked()?83:start;
            for(int i=start;i<=end;i++){audio.queue.configure(i,repeat.getValue(),gap.getValue());store.put("repeat."+i,repeat.getValue());store.put("gap."+i,gap.getValue());}
            audio.clearSeek();audio.queue.select(audio.queue.ayah(),false);updatePlayback();
        }).setNegativeButton("بازگشت",null).show();
    }
    private void plan(){
        screen="plan";audio.pauseNow();base();label(root,"برنامهٔ تمرین",26);LinearLayout c=scroll();
        label(c,"هر جلسه یک محدودهٔ مشخص دارد. تعداد آیات روزانه را متناسب با توان خود انتخاب کنید.",16);
        EditText daily=input(c,"آیات هر جلسه (۱ تا ۲۰)",""+store.number("daily",5),true);
        primary(button(c,"شروع بخش بعدی",()->{
            int count;try{count=Integer.parseInt(daily.getText().toString());}catch(Exception e){count=0;}if(count<1||count>20){daily.setError("عددی بین ۱ تا ۲۰ وارد کنید.");return;}
            store.put("daily",count);int start=1;while(start<=83&&store.mastered(start))start++;
            if(start>83){toast("همهٔ آیات علامت حفظ دارند؛ مرور آزاد را انتخاب کنید.");return;}startRange(start,Math.min(83,start+count-1));
        }));
        button(c,"مرور آزاد کل سوره",()->startRange(1,83));
        label(c,"محدودهٔ دلخواه",20);EditText first=input(c,"از آیه",""+audio.queue.first(),true),last=input(c,"تا آیه",""+audio.queue.last(),true);
        button(c,"شروع محدوده",()->{try{int a=Integer.parseInt(first.getText().toString()),b=Integer.parseInt(last.getText().toString());if(a<1||b>83||a>b)throw new IllegalArgumentException();startRange(a,b);}catch(Exception e){toast("محدوده باید بین ۱ و ۸۳ باشد و آغاز از پایان بیشتر نباشد.");}});
        List<Integer> due=store.due();label(c,"مرورهای موعدرسیده: "+fa(due.size()),20);for(int verse:due)button(c,"مرور آیهٔ "+fa(verse),()->startRange(verse,verse));
        button(c,"بازگشت",this::reader);
    }
    private void startRange(int first,int last){audio.clearSeek();audio.queue.range(first,last);saveRange();reader();}
    private void saveRange(){store.put("range.first",audio.queue.first());store.put("range.last",audio.queue.last());store.put("ayah",audio.queue.ayah());store.put("position",0);}
    private void progress(){
        screen="progress";audio.pauseNow();audio.flush();base();label(root,"مسیر پیشرفت",26);LinearLayout c=scroll();
        long mastered=store.scalar("SELECT COUNT(*) FROM verses WHERE mastered=1"),sessions=store.scalar("SELECT COUNT(*) FROM sessions"),ms=store.scalar("SELECT COALESCE(SUM(listened_ms),0) FROM sessions"),plays=store.scalar("SELECT COALESCE(SUM(plays),0) FROM sessions"),manual=store.scalar("SELECT COALESCE(SUM(manual),0) FROM sessions");
        label(c,store.get("name","")+"، "+fa((int)mastered)+" آیه از ۸۳ آیه",23);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(83);bar.setProgress((int)mastered);c.addView(bar,new LinearLayout.LayoutParams(-1,dp(12)));
        label(c,"حفظ‌شده بر اساس علامت‌گذاری خود شماست، نه ارزیابی خودکار تلاوت.",14);
        label(c,"جلسه‌ها: "+fa((int)sessions)+"\nگوش‌دادن: "+fa(String.format(Locale.ROOT,"%.1f",ms/60000.0))+" دقیقه\nپخش‌های کامل: "+fa((int)plays)+"\nتکرارهای شخصی: "+fa((int)manual),19);
        label(c,"زمان گوش‌دادن در هفت روز گذشته (دقیقه)",17);c.addView(new ProgressChartView(this,store.dailyMinutes()),new LinearLayout.LayoutParams(-1,dp(210)));label(c,"تاریخ‌های محور نمودار میلادی هستند.",13);
        button(c,"بازگشت",this::reader);
    }
    private void settings(){
        screen="settings";audio.pauseNow();base();label(root,"تنظیمات",26);LinearLayout c=scroll();
        button(c,"ویرایش نام و روش حفظ",()->profile(true));button(c,"دریافت صوت",this::downloadScreen);label(c,"قاری • مشاری العفاسی",20);
        label(c,"متن و پیشرفت آفلاین هستند. اینترنت فقط برای دریافت اختیاری صوت لازم است؛ پس از دانلود، پخش آفلاین خواهد بود.",16);
        Switch reminder=new Switch(this);reminder.setText("یادآوری روزانه حدود ساعت ۲۰");reminder.setChecked(store.flag("reminder"));reminder.setMinHeight(dp(56));c.addView(reminder);
        reminder.setOnCheckedChangeListener((v,enabled)->{
            if(enabled&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},36);return;}
            store.put("reminder",enabled);ReminderReceiver.schedule(this,enabled);
        });
        label(c,"زمان یادآوری ممکن است با مدیریت باتری دستگاه جابه‌جا شود. بعد از راه‌اندازی دوبارهٔ گوشی، برنامه را یک‌بار باز کنید.",14);
        label(c,"صفحه‌بندی ثابت اختصاصی برنامه است؛ متن ارسالی باید پیش از انتشار با نسخهٔ مرجع تطبیق داده شود.",14);
        button(c,"مجوز فونت Scheherazade New",()->{try(java.io.InputStream in=getAssets().open("fonts/Scheherazade-OFL.txt")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);new AlertDialog.Builder(this).setTitle("Scheherazade New • SIL OFL 1.1").setMessage(out.toString("UTF-8")).setPositiveButton("بستن",null).show();}catch(Exception e){toast("مجوز فونت همراه بستهٔ پروژه است.");}});
        button(c,"بازگشت",this::reader);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==36){boolean allowed=results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED;store.put("reminder",allowed);ReminderReceiver.schedule(this,allowed);if(!allowed)toast("یادآوری فعال نشد؛ مطالعه همچنان در دسترس است.");settings();}}
    private void setFullscreen(boolean enabled){
        fullscreen=enabled;fullscreenExit.setVisibility(enabled?View.VISIBLE:View.GONE);readerTools.setVisibility(enabled?View.GONE:View.VISIBLE);topBar.setVisibility(enabled?View.GONE:View.VISIBLE);root.invalidate();
        if(Build.VERSION.SDK_INT>=30){
            WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null){if(enabled){controller.hide(WindowInsets.Type.systemBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}else controller.show(WindowInsets.Type.systemBars());}
        }else getWindow().getDecorView().setSystemUiVisibility(enabled?View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    @Override protected void onResume(){super.onResume();if(audio!=null)audio.visible(true);}
    @Override protected void onPause(){
        if(downloads!=null)downloads.cancel();
        if(audio!=null){int position=audio.position();audio.pauseNow();store.put("ayah",audio.queue.ayah());store.put("position",position);store.put("range.first",audio.queue.first());store.put("range.last",audio.queue.last());store.endSession();audio.visible(false);}
        super.onPause();
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(downloads!=null)downloads.close();if(audio!=null)audio.close();if(store!=null)store.close();super.onDestroy();}
    @Override public void onBackPressed(){if("surahs".equals(screen)){super.onBackPressed();return;}if(!"reader".equals(screen)&&!"splash".equals(screen)&&!store.get("name","").isEmpty()){reader();return;}if(fullscreen){setFullscreen(false);return;}if("reader".equals(screen)){surahs();return;}super.onBackPressed();}
}
