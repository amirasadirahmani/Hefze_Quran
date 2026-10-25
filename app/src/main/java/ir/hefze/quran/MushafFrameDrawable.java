package ir.hefze.quran;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Reuse the frame in three sections so the header and player follow their actual content height. */
public final class MushafFrameDrawable extends Drawable {
    private static Bitmap artwork;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final View header, footer;
    private final float density;
    public MushafFrameDrawable(Context context, View header, View footer) {
        this.header = header; this.footer = footer;
        density=context.getResources().getDisplayMetrics().density;
        if (artwork == null) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            artwork = BitmapFactory.decodeResource(context.getResources(), R.drawable.mushaf_pistachio, options);
        }
    }
    @Override public void draw(Canvas canvas) {
        Rect b = getBounds(); float top = header.getVisibility() == View.GONE ? b.width() * .035f : header.getHeight();
        float bottom = footer.getVisibility() == View.GONE ? b.width() * .035f : footer.getHeight();
        int h = artwork.getHeight();
        int a = Math.round(h * .16f), z = Math.round(h * .815f);
        float topCap=Math.min(12*density,top*.15f),topTail=Math.min(14*density,top*.15f);
        int headerStart=Math.round(h*.036f),headerEnd=Math.round(h*.135f);
        part(canvas,0,headerStart,b.top,b.top+topCap);
        part(canvas,headerStart,headerEnd,b.top+topCap,b.top+top-topTail);
        part(canvas,headerEnd,a,b.top+top-topTail,b.top+top);
        part(canvas,a,z,b.top+top,b.bottom-bottom);
        float footCap=Math.min(12*density,bottom*.15f),footTail=Math.min(18*density,bottom*.15f);
        int footerStart=Math.round(h*.839f),footerEnd=Math.round(h*.962f);
        part(canvas,z,footerStart,b.bottom-bottom,b.bottom-bottom+footCap);
        part(canvas,footerStart,footerEnd,b.bottom-bottom+footCap,b.bottom-footTail);
        part(canvas,footerEnd,h,b.bottom-footTail,b.bottom);
    }
    private void part(Canvas canvas,int from,int to,float top,float bottom){
        Rect b=getBounds();canvas.drawBitmap(artwork,new Rect(0,from,artwork.getWidth(),to),new RectF(b.left,top,b.right,bottom),paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
