package ir.hefze.quran;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.widget.FrameLayout;

/* JADX INFO: loaded from: classes.dex */
public final class FramePanel extends FrameLayout {
    private final Bitmap art;
    private View body;
    private int bottom;
    private View footer;
    private View header;
    private final Paint paint;
    private int top;

    public FramePanel(Context context) {
        super(context);
        this.paint = new Paint(3);
        this.art = BitmapFactory.decodeResource(getResources(), R.drawable.mushaf_pistachio);
        setWillNotDraw(false);
    }

    public void content(View view, View view2, View view3) {
        this.header = view;
        this.body = view2;
        this.footer = view3;
        addView(view);
        addView(view2);
        addView(view3);
    }

    private int exact(int i) {
        return View.MeasureSpec.makeMeasureSpec(Math.max(1, i), 1073741824);
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        setMeasuredDimension(size, size2);
        float f = size;
        int i3 = size2 / 4;
        this.top = Math.min(Math.round((f * 140.0f) / 592.0f), i3);
        this.bottom = Math.min(Math.round((f * 159.0f) / 592.0f), i3);
        double d = size;
        this.header.measure(exact((int) (0.52d * d)), exact((int) ((this.top * 48.0f) / 140.0f)));
        this.body.measure(exact((int) (0.9d * d)), exact((size2 - this.top) - this.bottom));
        this.footer.measure(exact((int) (d * 0.48d)), exact((int) ((this.bottom * 56.0f) / 159.0f)));
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        place(this.header, i5, Math.round((this.top * 51.0f) / 140.0f));
        place(this.body, i5, this.top);
        place(this.footer, i5, ((i4 - i2) - this.bottom) + Math.round((this.bottom * 44.0f) / 159.0f));
    }

    private void place(View view, int i, int i2) {
        int measuredWidth = (i - view.getMeasuredWidth()) / 2;
        view.layout(measuredWidth, i2, view.getMeasuredWidth() + measuredWidth, view.getMeasuredHeight() + i2);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        part(canvas, 0, 140, 0, this.top);
        part(canvas, 140, 865, this.top, getHeight() - this.bottom);
        part(canvas, 865, 1024, getHeight() - this.bottom, getHeight());
    }

    private void part(Canvas canvas, int i, int i2, int i3, int i4) {
        canvas.drawBitmap(this.art, new Rect(0, i, 592, i2), new RectF(0.0f, i3, getWidth(), i4), this.paint);
    }
}
