package ir.hefze.quran;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.graphics.text.LineBreaker;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/* JADX INFO: loaded from: classes.dex */
public final class FullQuranActivity extends Activity {
    private Typeface arabic;
    private QuranBook book;
    private SpannableStringBuilder content;
    private ProgressBar downloadBar;
    private Dialog downloadDialog;
    private TextView downloadLabel;
    private Button downloadStart;
    private Button downloadStop;
    private BookDownloads downloads;
    private TextView passage;
    private BookPlayer player;
    private QuranBook.Reciter reciter;
    private boolean registered;
    private ScrollView scroll;
    private QuranBook.Ayah selected;
    private LinearLayout transport;
    private int page = 1;
    private boolean hasReading, settingsOpen, continueLocked;
    private OpeningRecitation opening;
    private String fontName = "ScheherazadeNew-Regular.ttf";
    private int repeatCount = 3, gapSeconds = 3;
    private ReaderSettingsScreen settingsScreen;
    private ir.hefze.quran.core.RecitationCycle cycle;
    private final android.os.Handler gapHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable gapTask = this::finishGap;
    private int fontSize = 28;
    private boolean home = true;
    private boolean repeat = false;
    private String audioReadyKey;
    private boolean audioReady;
    private final Map<QuranBook.Ayah, int[]> offsets = new LinkedHashMap();
    private final BroadcastReceiver readingChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) {
            if(book==null||home||settingsOpen)return;
            int id=i.getIntExtra("ayah",selected.id);
            if(id<1||id>book.all.size())return;
            selected=book.all.get(id-1);
            if(selected.page!=page)showPage();else{highlight(true);updateTransport();}
        }
    };
    private final BroadcastReceiver noisy = new BroadcastReceiver() { // from class: ir.hefze.quran.FullQuranActivity.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (FullQuranActivity.this.player != null) {
                FullQuranActivity.this.pausePlayback();
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(float f) {
        return Math.round(f * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String fa(int i) {
        return MainActivity.fa(i);
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        try {
            StudyStore studyStore = new StudyStore(this);
            try {
                ReminderReceiver.schedule(this, studyStore.flag("reminder"));
            } finally {
                studyStore.close();
            }
            this.fontName = getPreferences(0).getString("quran_font", "ScheherazadeNew-Regular.ttf");
            if (!fontName.equals("Amiri-Regular.ttf")) fontName = "ScheherazadeNew-Regular.ttf";
            this.arabic = Typeface.createFromAsset(getAssets(), "fonts/" + fontName);
            this.repeatCount = Math.max(1, Math.min(10, getPreferences(0).getInt("repeat_count", 3)));
            this.gapSeconds = Math.max(0, Math.min(30, getPreferences(0).getInt("gap_seconds", 3)));
            this.cycle = new ir.hefze.quran.core.RecitationCycle(repeatCount, gapSeconds);
            this.continueLocked = getPreferences(0).getBoolean("continue_locked", false);
            this.opening = new OpeningRecitation(this);
            this.book = new QuranBook(this);
            String string = getPreferences(0).getString("reciter", "ar.alafasy");
            this.reciter = this.book.reciters.get(0);
            for (QuranBook.Reciter reciter : this.book.reciters) {
                if (reciter.id.equals(string)) {
                    this.reciter = reciter;
                }
            }
            this.fontSize = Math.max(18, Math.min(42, getPreferences(0).getInt("font", 28)));
            this.hasReading = getPreferences(0).getBoolean("has_reading", getPreferences(0).contains("ayah"));
            this.selected = this.book.all.get(this.book.pageIndex.restoreAyah(hasReading, getPreferences(0).getInt("ayah", 1)) - 1);
            this.page = this.selected.page;
            this.repeat = getPreferences(0).getBoolean("repeat", false);
            this.downloads = new BookDownloads(this, this.book, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.downloadChanged();
                }
            });
            this.player = new BookPlayer(this, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.playbackChanged();
                }
            }, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.completed();
                }
            }, new Consumer() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda11
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    FullQuranActivity.this.toast((String) obj);
                }
            });
            if (bundle != null && bundle.getBoolean("settings", false)) {
                this.home = bundle.getBoolean("home", true);
                openSettings(ReaderSettingsScreen.Values.from(bundle.getBundle("settings_values")));
            } else if (bundle == null || bundle.getBoolean("home", true)) {
                showHome();
            } else {
                showPage();
            }
        } catch (Exception e) {
            this.home = true;
            TextView textView = new TextView(this);
            textView.setText("متن قرآن قابل خواندن نیست.");
            setContentView(textView);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinearLayout col() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return linearLayout;
    }

    private LinearLayout row() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(16);
        linearLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TextView text(String str, int i, boolean z) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTypeface(z ? this.arabic : getResources().getFont(R.font.vazirmatn_regular));
        textView.setTextColor(-14273247);
        textView.setGravity(17);
        return textView;
    }

    private Button icon(String str, String str2, int i, final Runnable runnable) {
        Button button = new Button(this);
        button.setText(str);
        button.setTextColor(i);
        button.setTextSize(21.0f);
        button.setIncludeFontPadding(false);
        button.setAutoSizeTextTypeUniformWithConfiguration(9, 21, 1, 2);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(0);
        button.setContentDescription(str2);
        button.setOnClickListener(new View.OnClickListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                runnable.run();
            }
        });
        return button;
    }

    private Button ornament(String str, final Runnable runnable) {
        OrnamentButton ornamentButton = new OrnamentButton(this);
        ornamentButton.setText(str);
        ornamentButton.setOnClickListener(new View.OnClickListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                runnable.run();
            }
        });
        return ornamentButton;
    }

    private GradientDrawable surface(int i, int i2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(12.0f));
        gradientDrawable.setStroke(dp(1.0f), i2);
        return gradientDrawable;
    }

    static /* synthetic */ WindowInsets lambda$mount$2(View view, WindowInsets windowInsets) {
        view.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
        return windowInsets;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$mount$3() {
        turn(-1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$mount$4() {
        turn(1);
    }

    private void mount(View view, View view2, View view3, boolean z) {
        LinearLayout linearLayoutCol = col();
        linearLayoutCol.setBackgroundColor(-329493);
        linearLayoutCol.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnApplyWindowInsetsListener
            public final WindowInsets onApplyWindowInsets(View view4, WindowInsets windowInsets) {
                return FullQuranActivity.lambda$mount$2(view4, windowInsets);
            }
        });
        FramePanel framePanel = new FramePanel(this);
        framePanel.content(view, view2, view3);
        linearLayoutCol.addView(framePanel, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayoutRow = row();
        if (z) {
            Button next = pageButton("صفحهٔ بعد  ‹‹", "صفحهٔ بعد", "page_next", () -> turn(1));
            next.setEnabled(page < book.pageIndex.pageCount());
            linearLayoutRow.addView(next, new LinearLayout.LayoutParams(0, dp(48), 1.6f));
            Button homeButton = icon("⌂", "بازگشت به صفحهٔ اصلی", -11376071, this::showHome);
            homeButton.setTag("home_button");
            linearLayoutRow.addView(homeButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        Button settingsButton = icon("⚙", "تنظیمات", -11376071, this::settings);
        settingsButton.setTag("settings_button");
        linearLayoutRow.addView(settingsButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        if (z) {
            Button previous = pageButton("››  صفحهٔ قبل", "صفحهٔ قبل", "page_previous", () -> turn(-1));
            previous.setEnabled(page > 1);
            linearLayoutRow.addView(previous, new LinearLayout.LayoutParams(0, dp(48), 1.6f));
        }
        linearLayoutCol.addView(linearLayoutRow);
        setContentView(linearLayoutCol);
        linearLayoutCol.requestApplyInsets();
    }

    private Button pageButton(String title, String description, String tag, Runnable action) {
        Button b = icon(title, description, 0xff425d38, action);
        b.setTag(tag); b.setTypeface(getResources().getFont(R.font.vazirmatn_regular));
        b.setTextSize(13); b.setAutoSizeTextTypeUniformWithConfiguration(10, 13, 1, 2);
        b.setBackground(surface(0xffeef2df, 0xffbdcba6));
        return b;
    }

    private View header(String str, String str2) {
        LinearLayout linearLayoutCol = col();
        linearLayoutCol.setGravity(17);
        TextView textViewText = text(str, 20, true);
        textViewText.setIncludeFontPadding(false);
        textViewText.setSingleLine();
        textViewText.setAutoSizeTextTypeUniformWithConfiguration(8, 23, 1, 2);
        linearLayoutCol.addView(textViewText, new LinearLayout.LayoutParams(-1, 0, 2.0f));
        TextView textViewText2 = text(str2, 11, false);
        textViewText2.setTag("page_label");
        textViewText2.setIncludeFontPadding(false);
        textViewText2.setSingleLine();
        textViewText2.setAutoSizeTextTypeUniformWithConfiguration(7, 12, 1, 2);
        linearLayoutCol.addView(textViewText2, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        return linearLayoutCol;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showHome() {
        hideKeyboard();
        if (this.player != null) {
            pausePlayback();
        }
        this.home = true;
        this.transport = null;
        this.passage = null;
        LinearLayout linearLayoutCol = col();
        linearLayoutCol.setFocusableInTouchMode(true);
        linearLayoutCol.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        linearLayoutCol.requestFocus();
        final EditText editText = new EditText(this);
        editText.setTextSize(15.0f);
        editText.setSingleLine();
        editText.setHint("جستجوی نام یا شمارهٔ سوره…");
        editText.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        editText.setCompoundDrawablePadding(dp(10.0f));
        editText.setPadding(dp(14.0f), dp(8.0f), dp(14.0f), dp(8.0f));
        editText.setBackground(surface(-65543, -6641023));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(52.0f));
        layoutParams.setMargins(dp(2.0f), dp(8.0f), dp(2.0f), dp(8.0f));
        linearLayoutCol.addView(editText, layoutParams);
        final Spinner spinner = new Spinner(this);
        String[] strArr = new String[31];
        strArr[0] = "همهٔ جزءها";
        for (int i = 1; i <= 30; i++) {
            strArr[i] = "جزء " + fa(i);
        }
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, strArr));
        linearLayoutCol.addView(spinner, new LinearLayout.LayoutParams(-1, dp(48.0f)));
        final ListView listView = new ListView(this);
        listView.setDividerHeight(dp(1.0f));
        linearLayoutCol.addView(listView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        final ArrayList arrayList = new ArrayList();
        final Runnable runnable = new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                FullQuranActivity.this.lambda$showHome$5(arrayList, spinner, editText, listView);
            }
        };
        editText.addTextChangedListener(new TextWatcher() { // from class: ir.hefze.quran.FullQuranActivity.3
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
                runnable.run();
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: ir.hefze.quran.FullQuranActivity.4
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i2, long j) {
                runnable.run();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda16
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i2, long j) {
                FullQuranActivity.this.lambda$showHome$6(arrayList, spinner, adapterView, view, i2, j);
            }
        });
        TextView textViewText = text(hasReading ? "ادامهٔ مطالعه" : "شروع مطالعه", 20, false);
        textViewText.setTypeface(getResources().getFont(R.font.vazirmatn_regular), Typeface.BOLD);
        textViewText.setContentDescription(hasReading ? "ادامهٔ مطالعهٔ قبلی" : "شروع مطالعه");
        textViewText.setTag("reading_entry");
        textViewText.setOnClickListener(new View.OnClickListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                FullQuranActivity.this.lambda$showHome$7(view);
            }
        });
        textViewText.setFocusable(true);
        mount(header("قرآن کریم", "۱۱۴ سوره · ۳۰ جزء"), linearLayoutCol, textViewText, false);
        runnable.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showHome$5(final List list, Spinner spinner, EditText editText, ListView listView) {
        list.clear();
        int selectedItemPosition = spinner.getSelectedItemPosition();
        String strNormalize = SurahSearch.normalize(editText.getText().toString());
        for (int i = 1; i <= 114; i++) {
            boolean z = selectedItemPosition == 0;
            Iterator<QuranBook.Ayah> it = this.book.surahs.get(i).iterator();
            while (it.hasNext()) {
                if (it.next().juz == selectedItemPosition) {
                    z = true;
                    break;
                }
            }
            if (z && (SurahSearch.normalize(this.book.names[i]).contains(strNormalize) || String.valueOf(i).equals(strNormalize) || (i == 36 && SurahSearch.normalize("یاسین").contains(strNormalize)))) {
                list.add(Integer.valueOf(i));
            }
        }
        listView.setAdapter((ListAdapter) new BaseAdapter() { // from class: ir.hefze.quran.FullQuranActivity.2
            @Override // android.widget.Adapter
            public int getCount() {
                return list.size();
            }

            @Override // android.widget.Adapter
            public Object getItem(int i2) {
                return list.get(i2);
            }

            @Override // android.widget.Adapter
            public long getItemId(int i2) {
                return ((Integer) list.get(i2)).intValue();
            }

            @Override // android.widget.Adapter
            public View getView(int i2, View view, ViewGroup viewGroup) {
                int iIntValue = ((Integer) list.get(i2)).intValue();
                LinearLayout linearLayoutCol = FullQuranActivity.this.col();
                linearLayoutCol.setPadding(FullQuranActivity.this.dp(12.0f), FullQuranActivity.this.dp(10.0f), FullQuranActivity.this.dp(12.0f), FullQuranActivity.this.dp(10.0f));
                TextView textViewText = FullQuranActivity.this.text(FullQuranActivity.this.fa(iIntValue) + "  " + FullQuranActivity.this.book.names[iIntValue], 18, false);
                textViewText.setTypeface(FullQuranActivity.this.getResources().getFont(R.font.vazirmatn_regular), Typeface.BOLD);
                textViewText.setGravity(5);
                linearLayoutCol.addView(textViewText);
                TextView textViewText2 = FullQuranActivity.this.text(FullQuranActivity.this.fa(FullQuranActivity.this.book.surahs.get(iIntValue).size()) + " آیه · صفحهٔ " + FullQuranActivity.this.fa(FullQuranActivity.this.book.surahs.get(iIntValue).get(0).page), 13, false);
                textViewText2.setGravity(5);
                linearLayoutCol.addView(textViewText2);
                return linearLayoutCol;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showHome$6(List list, Spinner spinner, AdapterView adapterView, View view, int i, long j) {
        int iIntValue = ((Integer) list.get(i)).intValue();
        int selectedItemPosition = spinner.getSelectedItemPosition();
        stopPlayback();
        this.selected = this.book.surahs.get(iIntValue).get(0);
        if (selectedItemPosition > 0) {
            for (QuranBook.Ayah ayah : this.book.surahs.get(iIntValue)) {
                if (ayah.juz == selectedItemPosition) {
                    this.selected = ayah;
                    break;
                }
            }
        }
        this.page = this.selected.page;
        showPage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showHome$7(View view) {
        showPage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    private void saveSettings() {
        getPreferences(0).edit().putInt("font", fontSize).putString("quran_font", fontName)
            .putString("reciter", reciter.id).putBoolean("repeat", repeat)
            .putInt("repeat_count", repeatCount).putInt("gap_seconds", gapSeconds)
            .putBoolean("continue_locked",continueLocked).putString("method", "AUDIO").putString("theme", "default").apply();
    }

    public void save() {
        hasReading = true;
        getPreferences(0).edit().putBoolean("has_reading", true).putInt("page", page).putInt("ayah", selected.id).apply();
        saveSettings();
    }

    private void showPage() {
        hideKeyboard();
        this.home = false;
        this.page = this.selected.page;
        save();
        this.scroll = new ScrollView(this);
        this.scroll.setSmoothScrollingEnabled(true);
        this.scroll.setFillViewport(false);
        this.passage = text("", this.fontSize, true);
        this.passage.setTag("quran_passage");
        this.passage.setGravity(5);
        this.passage.setTextDirection(4);
        // Justify every complete line; the final line of each surah section stays natural.
        this.passage.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        this.passage.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        this.passage.setPadding(dp(10.0f), dp(8.0f), dp(10.0f), dp(12.0f));
        this.passage.setLineSpacing(dp(4.0f), 1.12f);
        this.passage.setMovementMethod(LinkMovementMethod.getInstance());
        this.passage.setHighlightColor(0);
        this.content = new SpannableStringBuilder();
        this.offsets.clear();
        List<QuranBook.Ayah> list = this.book.pages.get(this.page);
        QuranBook.Ayah ayah = list.get(0);
        for (final QuranBook.Ayah ayah2 : list) {
            if (ayah2.number == 1) {
                if (this.content.length() > 0) {
                    this.content.append((CharSequence) "\n\n");
                }
                if (ayah2 != ayah) {
                    this.content.append((CharSequence) this.book.names[ayah2.surah]).append((CharSequence) "\n");
                    if (ayah2.surah != 9) {
                        this.content.append((CharSequence) this.book.all.get(0).text.replace("\ufeff", "")).append((CharSequence) "\n");
                    }
                }
            }
            int length = this.content.length();
            this.content.append((CharSequence) this.book.display(ayah2)).append((CharSequence) " ﴿").append((CharSequence) fa(ayah2.number)).append((CharSequence) "﴾ ");
            int length2 = this.content.length();
            this.offsets.put(ayah2, new int[]{length, length2});
            this.content.setSpan(new ClickableSpan() { // from class: ir.hefze.quran.FullQuranActivity.5
                @Override // android.text.style.ClickableSpan
                public void onClick(View view) {
                    FullQuranActivity.this.stopPlayback();
                    FullQuranActivity.this.selected = ayah2;
                    FullQuranActivity.this.save();
                    FullQuranActivity.this.highlight(true);
                    FullQuranActivity.this.updateTransport();
                }

                @Override // android.text.style.ClickableSpan, android.text.style.CharacterStyle
                public void updateDrawState(TextPaint textPaint) {
                    textPaint.setColor(-14273247);
                    textPaint.setUnderlineText(false);
                }
            }, length, length2, 33);
        }
        this.passage.setText(this.content);
        this.scroll.addView(this.passage, new FrameLayout.LayoutParams(-1, -2));
        this.transport = row();
        mount(header((ayah.number != 1 || ayah.surah == 9) ? this.book.names[ayah.surah] : this.book.all.get(0).text.replace("\ufeff", ""), "صفحهٔ " + fa(this.page) + " · جزء " + fa(ayah.juz)), this.scroll, this.transport, true);
        updateTransport();
        highlight(false);
        this.scroll.post(new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                FullQuranActivity.this.lambda$showPage$8();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showPage$8() {
        follow(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void highlight(boolean z) {
        if (this.passage == null || this.home) {
            return;
        }
        Spannable spannable = (Spannable) this.passage.getText();
        for (BackgroundColorSpan backgroundColorSpan : (BackgroundColorSpan[]) spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class)) {
            spannable.removeSpan(backgroundColorSpan);
        }
        int[] iArr = this.offsets.get(this.selected);
        if (iArr != null) {
            spannable.setSpan(new BackgroundColorSpan(863803473), iArr[0], iArr[1], 33);
        }
        if (z) {
            this.passage.post(new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$highlight$9();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$highlight$9() {
        follow(true);
    }

    private void follow(boolean z) {
        int[] iArr;
        if (this.home || this.passage == null || this.scroll == null || this.passage.getLayout() == null || (iArr = this.offsets.get(this.selected)) == null) {
            return;
        }
        Layout layout = this.passage.getLayout();
        int lineTop = layout.getLineTop(layout.getLineForOffset(iArr[0])) + this.passage.getPaddingTop();
        int lineBottom = layout.getLineBottom(layout.getLineForOffset(Math.max(iArr[0], iArr[1] - 1)));
        int iMax = Math.max(0, lineTop - dp(24.0f));
        if (lineTop < this.scroll.getScrollY() + dp(12.0f) || lineBottom > (this.scroll.getScrollY() + this.scroll.getHeight()) - dp(24.0f)) {
            if (z) {
                this.scroll.smoothScrollTo(0, iMax);
            } else {
                this.scroll.scrollTo(0, iMax);
            }
        }
    }

    private void turn(int i) {
        stopPlayback();
        this.page = this.book.pageIndex.adjacent(this.page, i);
        this.selected = this.book.all.get(this.book.pageIndex.firstAyah(this.page) - 1);
        showPage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateTransport$10() {
        move(-1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateTransport$11() {
        this.repeat = !this.repeat;
        save();
        updateTransport();
        toast(this.repeat ? "تکرار آیه روشن شد" : "تکرار آیه خاموش شد");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateTransport$12() {
        move(1);
    }

    private boolean currentSurahAudioReady() {
        String key = this.reciter.id + ":" + this.selected.surah;
        if (!key.equals(this.audioReadyKey)) {
            this.audioReady = this.book.complete(this, this.reciter, this.selected.surah);
            this.audioReadyKey = key;
        }
        return this.audioReady;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTransport() {
        if (home || settingsOpen || transport == null) return;
        transport.removeAllViews();
        if (!currentSurahAudioReady()) {
            Button download = ornament("دریافت صوت", this::downloadPopup); download.setTag("download_audio");
            transport.addView(download, new LinearLayout.LayoutParams(-1, -1)); return;
        }
        // Physical left is NEXT, physical right is PREVIOUS in Quran reading order.
        Button next = icon("‹", "آیهٔ بعد", -12559574, () -> move(1)); next.setTag("ayah_next");
        next.setEnabled(reciter.byAyah() && selected.number < book.surahs.get(selected.surah).size());
        Button play = icon(sequenceRunning() ? "Ⅱ" : "▶", sequenceRunning() ? "مکث" : "پخش", -12559574, this::toggle); play.setTag("play_pause");
        play.setEnabled(player == null || !player.loading());
        Button loop = icon("↻", repeat ? "خاموش‌کردن تکرار آیه" : "تکرار آیه", repeat ? -5076968 : -12559574, this::lambda$updateTransport$11); loop.setTag("repeat_one");
        loop.setEnabled(reciter.byAyah());
        Button previous = icon("›", "آیهٔ قبل", -12559574, () -> move(-1)); previous.setTag("ayah_previous");
        previous.setEnabled(reciter.byAyah() && selected.number > 1);
        for (Button button : new Button[]{next, play, loop, previous}) transport.addView(button, new LinearLayout.LayoutParams(0, -1, 1f));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggle() {
        if (!currentSurahAudioReady()) {
            downloadPopup();
            return;
        }
        if (ReadingPlaybackService.activeSession()) {
            startService(new Intent(this,ReadingPlaybackService.class).setAction(ReadingPlaybackService.playing()?"pause":"resume"));
            updateTransport(); return;
        }
        if (opening.pending()) { stopPlayback(); updateTransport(); return; }
        if (cycle.pending()) {
            if (cycle.paused()) { cycle.resume(android.os.SystemClock.uptimeMillis()); scheduleGap(); }
            else pausePlayback();
            updateTransport(); return;
        }
        if (continueLocked) {
            ReadingPlaybackService.start(this,reciter,selected,repeatCount,gapSeconds,repeat);
        } else if (player.hasSession()) player.toggle(book.audio(this,reciter,selected));
        else {
            opening.speak(selected.surah,selected.number, () -> {
                player.start(book.audio(this,reciter,selected)); highlight(true);
            }, () -> toast("صدای عربی برای مقدمه روی دستگاه موجود نیست. زبان عربی موتور گفتار را نصب کنید."));
        }
        if (this.reciter.byAyah()) {
            highlight(true);
        }
    }

    private void move(int i) {
        boolean zPlaying = sequenceRunning();
        stopPlayback();
        List<QuranBook.Ayah> list = this.book.surahs.get(this.selected.surah);
        this.selected = list.get(Math.max(0, Math.min(list.size() - 1, (this.selected.number - 1) + i)));
        if (this.selected.page != this.page) {
            showPage();
        } else {
            save();
            highlight(true);
            updateTransport();
        }
        if (zPlaying) {
            if (continueLocked) ReadingPlaybackService.start(this,reciter,selected,repeatCount,gapSeconds,repeat);
            else this.player.start(this.book.audio(this, this.reciter, this.selected));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void completed() {
        if (!reciter.byAyah()) return;
        cycle.completed(repeat, selected.number < book.surahs.get(selected.surah).size(), android.os.SystemClock.uptimeMillis());
        if (!cycle.pending()) cycle.reset();
        scheduleGap(); updateTransport();
    }
    private boolean sequenceRunning() {
        return ReadingPlaybackService.playing() || (opening != null && opening.pending()) || (player != null && player.playing()) || (cycle != null && cycle.pending() && !cycle.paused());
    }
    private void pausePlayback() {
        gapHandler.removeCallbacks(gapTask);
        if (opening != null) opening.stop();
        if (ReadingPlaybackService.activeSession()) startService(new Intent(this,ReadingPlaybackService.class).setAction("pause"));
        if (cycle != null) cycle.pause(android.os.SystemClock.uptimeMillis());
        if (player != null) player.pause();
    }
    private void stopPlayback() {
        gapHandler.removeCallbacks(gapTask);
        if (opening != null) opening.stop();
        ReadingPlaybackService.stopActive();
        if (cycle != null) cycle.reset();
        if (player != null) player.stop();
    }
    private void scheduleGap() {
        gapHandler.removeCallbacks(gapTask);
        if (cycle.pending() && !cycle.paused()) gapHandler.postDelayed(gapTask, cycle.remaining(android.os.SystemClock.uptimeMillis()));
    }
    private void finishGap() {
        ir.hefze.quran.core.RecitationCycle.Action action = cycle.takeDue(android.os.SystemClock.uptimeMillis());
        if (action == ir.hefze.quran.core.RecitationCycle.Action.NONE) return;
        if (action == ir.hefze.quran.core.RecitationCycle.Action.NEXT) move(1);
        player.start(book.audio(this, reciter, selected));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void playbackChanged() {
        if (this.book == null) {
            return;
        }
        updateTransport();
        if (this.player != null && this.player.playing() && this.reciter.byAyah()) {
            highlight(true);
        }
    }

    public void settings() {
        hideKeyboard();
        pausePlayback();
        openSettings(new ReaderSettingsScreen.Values(book.reciters.indexOf(reciter), fontSize, repeatCount, gapSeconds, fontName, continueLocked));
    }
    private void openSettings(ReaderSettingsScreen.Values initial) {
        settingsOpen = true;
        settingsScreen = new ReaderSettingsScreen(this, book.reciters, initial, values -> {
            QuranBook.Reciter chosen = book.reciters.get(values.reciter);
            if (downloads.running && !chosen.id.equals(reciter.id)) { toast("ابتدا دانلود جاری را متوقف کنید."); return; }
            stopPlayback(); reciter = chosen; fontSize = values.size; fontName = values.font;
            repeatCount = values.repeats; gapSeconds = values.gap; continueLocked = values.continueLocked;
            cycle = new ir.hefze.quran.core.RecitationCycle(repeatCount, gapSeconds);
            arabic = Typeface.createFromAsset(getAssets(), "fonts/" + fontName);
            audioReadyKey = null; saveSettings(); closeSettings();
        }, this::closeSettings);
        settingsScreen.setOnApplyWindowInsetsListener((view, insets) -> lambda$mount$2(view, insets));
        setContentView(settingsScreen); settingsScreen.requestApplyInsets();
    }
    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }
    private void closeSettings() {
        settingsOpen = false; settingsScreen = null;
        if (home) showHome(); else showPage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void downloadPopup() {
        pausePlayback();
        if (this.downloadDialog == null || !this.downloadDialog.isShowing()) {
            final QuranBook.Reciter reciter = this.reciter;
            final int i = this.selected.surah;
            this.downloadDialog = new Dialog(this);
            LinearLayout linearLayoutCol = col();
            linearLayoutCol.setPadding(dp(18.0f), dp(12.0f), dp(18.0f), dp(18.0f));
            linearLayoutCol.setBackground(surface(-329493, -3747661));
            LinearLayout linearLayoutRow = row();
            linearLayoutRow.addView(icon("×", "بستن پنجره", -3916491, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda24
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$downloadPopup$16();
                }
            }), new LinearLayout.LayoutParams(dp(48.0f), dp(48.0f)));
            linearLayoutRow.addView(text("دریافت صوت سوره", 20, false), new LinearLayout.LayoutParams(0, dp(48.0f), 1.0f));
            linearLayoutCol.addView(linearLayoutRow);
            linearLayoutCol.addView(text(this.book.names[i] + "\n" + this.reciter.name, 16, false));
            this.downloadLabel = text("", 13, false);
            this.downloadLabel.setPadding(0, dp(12.0f), 0, dp(8.0f));
            linearLayoutCol.addView(this.downloadLabel);
            LinearLayout linearLayoutRow2 = row();
            this.downloadBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            this.downloadBar.setMax(100);
            this.downloadBar.setProgressTintList(ColorStateList.valueOf(-9338031));
            linearLayoutRow2.addView(this.downloadBar, new LinearLayout.LayoutParams(0, dp(8.0f), 1.0f));
            final BookDownloads bookDownloads = this.downloads;
            Objects.requireNonNull(bookDownloads);
            this.downloadStop = icon("■", "توقف دانلود", -3759843, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda25
                @Override // java.lang.Runnable
                public final void run() {
                    bookDownloads.cancel();
                }
            });
            this.downloadStop.setTag("download_stop");
            linearLayoutRow2.addView(this.downloadStop, new LinearLayout.LayoutParams(dp(48.0f), dp(48.0f)));
            linearLayoutCol.addView(linearLayoutRow2);
            this.downloadStart = ornament("دریافت سوره", new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda26
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$downloadPopup$17(reciter, i);
                }
            });
            this.downloadStart.setTag("download_start");
            linearLayoutCol.addView(this.downloadStart, new LinearLayout.LayoutParams(-1, dp(48.0f)));
            this.downloadDialog.setContentView(linearLayoutCol);
            this.downloadDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            this.downloadDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda27
                @Override // android.content.DialogInterface.OnDismissListener
                public final void onDismiss(DialogInterface dialogInterface) {
                    FullQuranActivity.this.lambda$downloadPopup$18(dialogInterface);
                }
            });
            this.downloadDialog.show();
            this.downloadDialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(32.0f), dp(430.0f)), -2);
            downloadChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadPopup$16() {
        this.downloadDialog.dismiss();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadPopup$17(QuranBook.Reciter reciter, int i) {
        this.downloads.start(reciter, i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadPopup$18(DialogInterface dialogInterface) {
        this.downloadLabel = null;
        this.downloadBar = null;
        this.downloadStart = null;
        this.downloadStop = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void downloadChanged() {
        if (!this.downloads.running) this.audioReadyKey = null;
        if (this.downloadLabel != null) {
            boolean available = this.book.supportsSurah(this.reciter, this.selected.surah);
            this.downloadLabel.setText(!available ? "صوت کامل این سوره برای قاری انتخاب‌شده موجود نیست. قاری دیگری انتخاب کنید."
                    : this.downloads.message.isEmpty() ? "پس از دریافت، بدون اینترنت بشنوید." : this.downloads.message);
            this.downloadBar.setProgress(this.downloads.progress);
            this.downloadStart.setEnabled(available && !this.downloads.running && !currentSurahAudioReady());
            this.downloadStop.setVisibility(this.downloads.running ? View.VISIBLE : View.GONE);
        }
        updateTransport();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter("android.media.AUDIO_BECOMING_NOISY");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(this.noisy, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(this.noisy, intentFilter);
        }
        if(Build.VERSION.SDK_INT>=33)registerReceiver(readingChanged,new IntentFilter(ReadingPlaybackService.SYNC),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(readingChanged,new IntentFilter(ReadingPlaybackService.SYNC));
        this.registered = true;
        if(book!=null && !home && !settingsOpen){
            int id=getPreferences(0).getInt("ayah",selected.id);
            if(id>=1 && id<=book.all.size() && selected.id!=id){
                selected=book.all.get(id-1);
                if(selected.page!=page)showPage();else{highlight(true);updateTransport();}
            }
        }
    }

    @Override // android.app.Activity
    protected void onStop() {
        if (this.player != null && !continueLocked) {
            pausePlayback();
        }
        if (this.downloads != null) {
            this.downloads.cancel();
        }
        if (this.registered) {
            unregisterReceiver(this.noisy);
            unregisterReceiver(readingChanged);
            this.registered = false;
        }
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean("home", this.home);
        bundle.putBoolean("settings", settingsOpen);
        if (settingsOpen) bundle.putBundle("settings_values", settingsScreen.values().bundle());
        super.onSaveInstanceState(bundle);
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        gapHandler.removeCallbacks(gapTask);
        if (this.downloads != null) {
            this.downloads.close();
        }
        if (this.opening != null) this.opening.close();
        if (this.player != null) {
            this.player.close();
        }
        if (this.downloadDialog != null) {
            this.downloadDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (settingsOpen) { closeSettings(); return; }
        if (this.home || this.book == null) {
            super.onBackPressed();
        } else {
            showHome();
        }
    }
}
