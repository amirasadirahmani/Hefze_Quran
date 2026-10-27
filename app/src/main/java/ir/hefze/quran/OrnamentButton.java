package ir.hefze.quran;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.widget.Button;

/** The supplied artwork is a real background, independent of OEM button drawing. */
public final class OrnamentButton extends Button {
    private static Bitmap art;
    public OrnamentButton(Context context) {
        super(context, null, 0);
        if (art == null) art = BitmapFactory.decodeResource(getResources(), R.drawable.ornament_button);
        setBackgroundTintList(null);
        setStateListAnimator(null);
        setAllCaps(false);
        setTextColor(0xfffaf7ed);
        setTypeface(getResources().getFont(R.font.vazirmatn_regular));
        setGravity(Gravity.CENTER);
        setIncludeFontPadding(false);
        setPadding(dp(14), 0, dp(14), 0);
        setMinWidth(0); setMinimumWidth(0); setMinHeight(0); setMinimumHeight(0);
        setSingleLine();
        setAutoSizeTextTypeUniformWithConfiguration(10, 15, 1, 2);
        updateBackground(0);
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void updateBackground(int height) {
        BitmapDrawable background = new BitmapDrawable(getResources(), art);
        background.setGravity(Gravity.FILL);
        int inset = Math.max(0, (height - dp(36)) / 2);
        setBackground(new InsetDrawable(background, 0, inset, 0, inset));
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh); updateBackground(h);
    }
    @Override public void setEnabled(boolean enabled) { super.setEnabled(enabled); setAlpha(enabled ? 1f : .55f); }
}
