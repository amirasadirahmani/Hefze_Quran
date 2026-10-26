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
import java.text.Normalizer;
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
    private int page = 440;
    private int fontSize = 28;
    private boolean home = true;
    private boolean repeat = false;
    private final Map<QuranBook.Ayah, int[]> offsets = new LinkedHashMap();
    private final BroadcastReceiver noisy = new BroadcastReceiver() { // from class: ir.hefze.quran.FullQuranActivity.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (FullQuranActivity.this.player != null) {
                FullQuranActivity.this.player.pause();
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
        this.arabic = Typeface.createFromAsset(getAssets(), "fonts/ScheherazadeNew-Regular.ttf");
        try {
            this.book = new QuranBook(this);
            String string = getPreferences(0).getString("reciter", "ar.alafasy");
            this.reciter = this.book.reciters.get(0);
            for (QuranBook.Reciter reciter : this.book.reciters) {
                if (reciter.id.equals(string)) {
                    this.reciter = reciter;
                }
            }
            this.fontSize = Math.max(18, Math.min(42, getPreferences(0).getInt("font", 28)));
            this.page = Math.max(1, Math.min(604, getPreferences(0).getInt("page", 440)));
            this.selected = this.book.all.get(Math.max(0, Math.min(6235, getPreferences(0).getInt("ayah", this.book.pages.get(this.page).get(0).id) - 1)));
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
            if (bundle == null || bundle.getBoolean("home", true)) {
                showHome();
            } else {
                showPage();
            }
        } catch (Exception e) {
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
            linearLayoutRow.addView(icon("⌂", "بازگشت به صفحهٔ اصلی", -11376071, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.showHome();
                }
            }), new LinearLayout.LayoutParams(0, dp(48.0f), 1.0f));
            Button buttonIcon = icon("‹", "صفحهٔ قبل", -11376071, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$mount$3();
                }
            });
            buttonIcon.setEnabled(this.page > 1);
            linearLayoutRow.addView(buttonIcon, new LinearLayout.LayoutParams(0, dp(48.0f), 1.0f));
            Button buttonIcon2 = icon("›", "صفحهٔ بعد", -11376071, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$mount$4();
                }
            });
            buttonIcon2.setEnabled(this.page < 604);
            linearLayoutRow.addView(buttonIcon2, new LinearLayout.LayoutParams(0, dp(48.0f), 1.0f));
        }
        linearLayoutRow.addView(icon("⚙", "تنظیمات", -11376071, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda22
            @Override // java.lang.Runnable
            public final void run() {
                FullQuranActivity.this.settings();
            }
        }), new LinearLayout.LayoutParams(0, dp(48.0f), 1.0f));
        linearLayoutCol.addView(linearLayoutRow);
        setContentView(linearLayoutCol);
        linearLayoutCol.requestApplyInsets();
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
        textViewText2.setIncludeFontPadding(false);
        textViewText2.setSingleLine();
        textViewText2.setAutoSizeTextTypeUniformWithConfiguration(7, 12, 1, 2);
        linearLayoutCol.addView(textViewText2, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        return linearLayoutCol;
    }

    private String normalize(String str) {
        String strReplace = Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace((char) 1740, (char) 1610).replace((char) 1705, (char) 1603).replace((char) 1649, (char) 1575).replace("ـ", "");
        for (int i = 0; i < 10; i++) {
            char c = (char) (i + 48);
            strReplace = strReplace.replace((char) (i + 1776), c).replace((char) (i + 1632), c);
        }
        return strReplace.trim();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showHome() {
        if (this.player != null) {
            this.player.pause();
        }
        this.home = true;
        this.transport = null;
        this.passage = null;
        LinearLayout linearLayoutCol = col();
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
        TextView textViewText = text("ادامهٔ مطالعه", 20, false);
        textViewText.setTypeface(getResources().getFont(R.font.vazirmatn_regular), Typeface.BOLD);
        textViewText.setContentDescription("ادامهٔ مطالعهٔ قبلی");
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
        String strNormalize = normalize(editText.getText().toString());
        for (int i = 1; i <= 114; i++) {
            boolean z = selectedItemPosition == 0;
            Iterator<QuranBook.Ayah> it = this.book.surahs.get(i).iterator();
            while (it.hasNext()) {
                if (it.next().juz == selectedItemPosition) {
                    z = true;
                    break;
                }
            }
            if (z && (normalize(this.book.names[i]).contains(strNormalize) || String.valueOf(i).equals(strNormalize) || (i == 36 && normalize("یاسین").contains(strNormalize)))) {
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
        this.player.stop();
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
    public void save() {
        getPreferences(0).edit().putInt("page", this.page).putInt("ayah", this.selected.id).putInt("font", this.fontSize).putString("reciter", this.reciter.id).putBoolean("repeat", this.repeat).apply();
    }

    private void showPage() {
        this.home = false;
        this.page = this.selected.page;
        save();
        this.scroll = new ScrollView(this);
        this.scroll.setSmoothScrollingEnabled(true);
        this.scroll.setFillViewport(false);
        this.passage = text("", this.fontSize, true);
        this.passage.setGravity(5);
        this.passage.setTextDirection(4);
        // Justify every complete line; the final line of each surah section stays natural.
        this.passage.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        this.passage.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
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
                    FullQuranActivity.this.player.stop();
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
        this.player.stop();
        this.page = Math.max(1, Math.min(604, this.page + i));
        this.selected = this.book.pages.get(this.page).get(0);
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

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTransport() {
        Button buttonIcon;
        LinearLayout linearLayout;
        LinearLayout.LayoutParams layoutParams;
        if (this.home || this.transport == null) {
            return;
        }
        this.transport.removeAllViews();
        if (this.book.complete(this, this.reciter, this.selected.surah)) {
            Button buttonIcon2 = icon("‹", "آیهٔ قبل", -12559574, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$updateTransport$10();
                }
            });
            buttonIcon2.setEnabled(this.reciter.byAyah() && this.selected.number > 1);
            this.transport.addView(buttonIcon2, new LinearLayout.LayoutParams(0, -1, 1.0f));
            Button buttonIcon3 = icon((this.player == null || !this.player.playing()) ? "▶" : "Ⅱ", (this.player == null || !this.player.playing()) ? "پخش" : "مکث", -12559574, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.toggle();
                }
            });
            buttonIcon3.setEnabled(this.player == null || !this.player.loading());
            this.transport.addView(buttonIcon3, new LinearLayout.LayoutParams(0, -1, 1.0f));
            Button buttonIcon4 = icon("↻", this.repeat ? "خاموش\u200cکردن تکرار آیه" : "تکرار آیه", this.repeat ? -5076968 : -12559574, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$updateTransport$11();
                }
            });
            buttonIcon4.setEnabled(this.reciter.byAyah());
            this.transport.addView(buttonIcon4, new LinearLayout.LayoutParams(0, -1, 1.0f));
            buttonIcon = icon("›", "آیهٔ بعد", -12559574, new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$updateTransport$12();
                }
            });
            buttonIcon.setEnabled(this.reciter.byAyah() && this.selected.number < this.book.surahs.get(this.selected.surah).size());
            linearLayout = this.transport;
            layoutParams = new LinearLayout.LayoutParams(0, -1, 1.0f);
        } else {
            buttonIcon = ornament("دریافت صوت", new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.downloadPopup();
                }
            });
            linearLayout = this.transport;
            layoutParams = new LinearLayout.LayoutParams(-1, -1);
        }
        linearLayout.addView(buttonIcon, layoutParams);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggle() {
        if (!this.book.complete(this, this.reciter, this.selected.surah)) {
            downloadPopup();
            return;
        }
        this.player.toggle(this.book.audio(this, this.reciter, this.selected));
        if (this.reciter.byAyah()) {
            highlight(true);
        }
    }

    private void move(int i) {
        boolean zPlaying = this.player.playing();
        this.player.stop();
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
            this.player.start(this.book.audio(this, this.reciter, this.selected));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void completed() {
        if (this.reciter.byAyah()) {
            if (this.repeat) {
                this.player.start(this.book.audio(this, this.reciter, this.selected));
            } else if (this.selected.number < this.book.surahs.get(this.selected.surah).size()) {
                move(1);
                this.player.start(this.book.audio(this, this.reciter, this.selected));
            }
        }
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

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$settings$13(DialogInterface dialogInterface, int i) {
        startActivity(new Intent(this, (Class<?>) MainActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$settings$14(Spinner spinner, SeekBar seekBar, AlertDialog alertDialog, View view) {
        QuranBook.Reciter reciter = (QuranBook.Reciter) spinner.getSelectedItem();
        if (this.downloads.running && !reciter.id.equals(this.reciter.id)) {
            toast("ابتدا دانلود جاری را متوقف کنید.");
            return;
        }
        this.player.stop();
        this.reciter = reciter;
        this.fontSize = seekBar.getProgress() + 18;
        save();
        alertDialog.dismiss();
        if (this.home) {
            showHome();
        } else {
            showPage();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$settings$15(final AlertDialog alertDialog, final Spinner spinner, final SeekBar seekBar, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                FullQuranActivity.this.lambda$settings$14(spinner, seekBar, alertDialog, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void settings() {
        this.player.pause();
        LinearLayout linearLayoutCol = col();
        linearLayoutCol.setPadding(dp(20.0f), dp(12.0f), dp(20.0f), dp(12.0f));
        linearLayoutCol.addView(text("اندازهٔ متن قرآن", 17, false));
        final TextView textViewText = text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", this.fontSize, true);
        final SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(24);
        seekBar.setProgress(this.fontSize - 18);
        linearLayoutCol.addView(seekBar);
        linearLayoutCol.addView(textViewText);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: ir.hefze.quran.FullQuranActivity.6
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar2, int i, boolean z) {
                textViewText.setTextSize(i + 18);
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar2) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar2) {
            }
        });
        linearLayoutCol.addView(text("انتخاب قاری و نسخهٔ صوت", 17, false));
        final Spinner spinner = new Spinner(this);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.book.reciters);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
        spinner.setSelection(this.book.reciters.indexOf(this.reciter));
        linearLayoutCol.addView(spinner, new LinearLayout.LayoutParams(-1, dp(56.0f)));
        final TextView textViewText2 = text("", 13, false);
        linearLayoutCol.addView(textViewText2);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: ir.hefze.quran.FullQuranActivity.7
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                textViewText2.setText(FullQuranActivity.this.book.reciters.get(i).byAyah() ? "پخش پیوسته، تکرار آیه و دنبال\u200cکردن آیه فعال است." : "فایل یکپارچهٔ سوره؛ زمان\u200cبندی آیات در منبع موجود نیست. دنبال\u200cکردن و تکرار آیه برای این نسخه فعال نمی\u200cشود.");
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(linearLayoutCol);
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("تنظیمات").setView(scrollView).setPositiveButton("ذخیره", (DialogInterface.OnClickListener) null).setNegativeButton("بازگشت", (DialogInterface.OnClickListener) null).setNeutralButton("تمرین حفظ یاسین", new DialogInterface.OnClickListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda12
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                FullQuranActivity.this.lambda$settings$13(dialogInterface, i);
            }
        }).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda13
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                FullQuranActivity.this.lambda$settings$15(alertDialogCreate, spinner, seekBar, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void downloadPopup() {
        this.player.pause();
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
            linearLayoutRow2.addView(this.downloadStop, new LinearLayout.LayoutParams(dp(48.0f), dp(48.0f)));
            linearLayoutCol.addView(linearLayoutRow2);
            this.downloadStart = ornament("دریافت سوره", new Runnable() { // from class: ir.hefze.quran.FullQuranActivity$$ExternalSyntheticLambda26
                @Override // java.lang.Runnable
                public final void run() {
                    FullQuranActivity.this.lambda$downloadPopup$17(reciter, i);
                }
            });
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
        if (this.downloadLabel != null) {
            this.downloadLabel.setText(this.downloads.message.isEmpty() ? "پس از دریافت، بدون اینترنت بشنوید." : this.downloads.message);
            this.downloadBar.setProgress(this.downloads.progress);
            this.downloadStart.setEnabled((this.downloads.running || this.book.complete(this, this.reciter, this.selected.surah)) ? false : true);
            this.downloadStop.setEnabled(this.downloads.running);
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
        this.registered = true;
    }

    @Override // android.app.Activity
    protected void onStop() {
        if (this.player != null) {
            this.player.pause();
        }
        if (this.downloads != null) {
            this.downloads.cancel();
        }
        if (this.registered) {
            unregisterReceiver(this.noisy);
            this.registered = false;
        }
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean("home", this.home);
        super.onSaveInstanceState(bundle);
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (this.downloads != null) {
            this.downloads.close();
        }
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
        if (this.home) {
            super.onBackPressed();
        } else {
            showHome();
        }
    }
}
