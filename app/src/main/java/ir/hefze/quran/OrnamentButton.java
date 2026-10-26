package ir.hefze.quran;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.Button;

/* JADX INFO: loaded from: classes.dex */
public final class OrnamentButton extends Button {
    private static Bitmap art;
    private final Paint p;

    public OrnamentButton(Context context) {
        super(context);
        this.p = new Paint(3);
        if (art == null) {
            art = BitmapFactory.decodeResource(getResources(), R.drawable.ornament_button);
        }
        setBackgroundColor(0);
        setBackgroundTintList(null);
        setAllCaps(false);
        setTextColor(-329495);
        setTextSize(13.0f);
        setTypeface(getResources().getFont(R.font.vazirmatn_regular));
        setPadding(8, 0, 8, 0);
        setMinWidth(0);
        setMinimumWidth(0);
        setMinHeight(0);
        setMinimumHeight(0);
        setSingleLine();
    }

    @Override // android.widget.TextView, android.view.View
    protected void onDraw(Canvas canvas) {
        float fMin = Math.min(getHeight(), Math.min(getResources().getDisplayMetrics().density * 36.0f, getWidth() * 0.2f));
        this.p.setAlpha(isEnabled() ? 255 : 120);
        canvas.drawBitmap(art, (Rect) null, new RectF(0.0f, (getHeight() - fMin) / 2.0f, getWidth(), (getHeight() + fMin) / 2.0f), this.p);
        super.onDraw(canvas);
    }
}
