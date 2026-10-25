package ir.hefze.quran;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Keep the supplied frame's ornate caps intact while the plain center fits the screen. */
public final class MushafFrameDrawable extends Drawable {
    private static Bitmap artwork;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final View header, footer;
    public MushafFrameDrawable(Context context, View header, View footer) {
        this.header = header; this.footer = footer;
        if (artwork == null) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            artwork = BitmapFactory.decodeResource(context.getResources(), R.drawable.mushaf_pistachio, options);
        }
    }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds();float scale=b.width()/(float)artwork.getWidth();
        float top=header.getVisibility()==View.GONE?b.width()*.035f:header.getHeight();
        float bottom=footer.getVisibility()==View.GONE?b.width()*.035f:footer.getHeight();
        // Source coordinates are from the user's 592x1024 portrait frame.
        int h=artwork.getHeight();
        float topCap=Math.min(60*scale,top*.4f),topTail=Math.min(50*scale,top*.35f);
        part(canvas,0,60,b.top,b.top+topCap);
        part(canvas,60,90,b.top+topCap,b.top+top-topTail);
        part(canvas,90,140,b.top+top-topTail,b.top+top);
        // Put extra player height in the straight side rails, above the footer ornament.
        float ornamentHeight=Math.min(bottom,(h-865)*scale);
        part(canvas,140,865,b.top+top,b.bottom-ornamentHeight);
        part(canvas,865,h,b.bottom-ornamentHeight,b.bottom);
    }
    private void part(Canvas canvas,int from,int to,float top,float bottom){
        Rect b=getBounds();canvas.drawBitmap(artwork,new Rect(0,from,artwork.getWidth(),to),new RectF(b.left,top,b.right,bottom),paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
