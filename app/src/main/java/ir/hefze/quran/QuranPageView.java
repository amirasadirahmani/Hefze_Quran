package ir.hefze.quran;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.text.style.*;
import android.view.*;
import java.util.*;

/** Immutable line breaks at reference width. Scale the canvas, never reflow on device width. */
public final class QuranPageView extends View {
    public interface Selection {void select(int ayah);}
    private final QuranRepository repository;
    private final Selection selection;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final List<StaticLayout> layouts=new ArrayList<>();
    private final List<SpannableString> strings=new ArrayList<>();
    private final List<List<int[]>> offsets=new ArrayList<>();
    private int page=0,ayah=1,active=-1,read=-1;
    private boolean hidden;
    public QuranPageView(Context context,QuranRepository repository,Selection selection){
        super(context);this.repository=repository;this.selection=selection;
        text.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Amiri-Regular.ttf"));text.setTextSize(34);text.setColor(0xff273c2d);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);setClickable(true);setPage(0);
    }
    public int page(){return page;}
    public void setPage(int value){
        page=Math.max(0,Math.min(repository.pages.size()-1,value));layouts.clear();strings.clear();offsets.clear();
        for(List<QuranRepository.Token> line:repository.pages.get(page)){
            StringBuilder s=new StringBuilder();List<int[]> ranges=new ArrayList<>();
            for(QuranRepository.Token t:line){if(s.length()>0)s.append(' ');int start=s.length();s.append(t.text);ranges.add(new int[]{start,s.length(),t.ayah,t.word});}
            SpannableString span=new SpannableString(s);strings.add(span);offsets.add(ranges);
            layouts.add(StaticLayout.Builder.obtain(span,0,span.length(),text,680).setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(true).build());
        }
        highlight(ayah,active,read,hidden);requestLayout();invalidate();
    }
    public void highlight(int verse,int word,int readThrough,boolean hide){
        ayah=verse;active=word;read=readThrough;hidden=hide;
        for(int i=0;i<strings.size();i++){
            SpannableString s=strings.get(i);
            for(Object span:s.getSpans(0,s.length(),Object.class))s.removeSpan(span);
            for(int[] r:offsets.get(i))if(r[2]==ayah){
                s.setSpan(new BackgroundColorSpan(0xffedf2de),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(r[3]>=0&&r[3]<=read)s.setSpan(new BackgroundColorSpan(0xffdce9bb),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(r[3]==active&&active>=0){s.setSpan(new ForegroundColorSpan(0xff185e39),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);s.setSpan(new BackgroundColorSpan(0xffbad389),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
            }
        }
        StringBuilder description=new StringBuilder("صفحهٔ "+MainActivity.fa(page+1));
        if(!hidden)for(SpannableString s:strings)description.append('\n').append(s);
        else description.append("؛ متن برای آزمون پنهان است");
        setContentDescription(description);invalidate();
    }
    @Override protected void onMeasure(int ws,int hs){int width=MeasureSpec.getSize(ws);setMeasuredDimension(width,Math.round(width*1120f/760f));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float scale=getWidth()/760f;canvas.save();canvas.scale(scale,scale);
        paint.setColor(0xfffffdf5);canvas.drawRoundRect(4,4,756,1116,28,28,paint);
        paint.setColor(0xffb7c493);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRoundRect(20,20,740,1100,20,20,paint);canvas.drawRoundRect(28,28,732,1092,16,16,paint);paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(text.getTypeface());paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(40);paint.setColor(0xff416c45);canvas.drawText("سورة يس",380,77,paint);
        paint.setTextSize(27);canvas.drawText(page==0?repository.basmala:"حفظ یاسین",380,125,paint);
        for(int i=0;i<layouts.size();i++){
            if(hidden){paint.setColor(0xffe8eddb);canvas.drawRoundRect(60,168+i*62,700,193+i*62,10,10,paint);}
            else {canvas.save();canvas.translate(40,145+i*62);layouts.get(i).draw(canvas);canvas.restore();}
        }
        paint.setTextSize(26);paint.setColor(0xff65794d);canvas.drawText(MainActivity.fa(page+1),380,1082,paint);canvas.restore();
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN)return true;
        if(event.getAction()==MotionEvent.ACTION_UP){
            performClick();float scale=getWidth()/760f;int row=(int)((event.getY()/scale-145)/62);
            if(event.getY()/scale>=145&&row>=0&&row<layouts.size()){
                int offset=layouts.get(row).getOffsetForHorizontal(0,event.getX()/scale-40);
                for(int[] r:offsets.get(row))if(offset>=r[0]&&offset<=r[1]){selection.select(r[2]);break;}
            } return true;
        }return super.onTouchEvent(event);
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
