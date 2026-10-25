package ir.hefze.quran;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.text.style.*;
import android.view.*;
import java.util.*;

/** Fixed authored lines; only the canvas scale changes between screen sizes. */
public final class QuranPageView extends View {
    public interface Selection { void select(int ayah); }
    private static final int WIDTH=760, HEIGHT=1072, LINE_HEIGHT=96, TEXT_WIDTH=700;
    private final QuranRepository repository;
    private final Selection selection;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final List<StaticLayout> layouts=new ArrayList<>();
    private final List<SpannableString> strings=new ArrayList<>();
    private final List<List<int[]>> offsets=new ArrayList<>();
    private int page,ayah=1,active=-1,read=-1;
    private boolean hidden;

    public QuranPageView(Context context,QuranRepository repository,Selection selection){
        super(context);this.repository=repository;this.selection=selection;
        text.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/ScheherazadeNew-Regular.ttf"));
        text.setTextSize(44);text.setColor(0xff203022);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);setClickable(true);setPage(0);
    }
    public int page(){return page;}
    public void setPage(int value){
        page=Math.max(0,Math.min(repository.pages.size()-1,value));layouts.clear();strings.clear();offsets.clear();
        List<List<QuranRepository.Token>> lines=repository.pages.get(page);
        for(int lineIndex=0;lineIndex<lines.size();lineIndex++){
            List<QuranRepository.Token> line=lines.get(lineIndex);
            StringBuilder s=new StringBuilder();List<int[]> ranges=new ArrayList<>();
            for(QuranRepository.Token t:line){if(s.length()>0)s.append(' ');int start=s.length();s.append(t.text);ranges.add(new int[]{start,s.length(),t.ayah,t.word});}
            SpannableString span=new SpannableString(s);
            for(int[] r:ranges)if(r[3]<0)span.setSpan(new AyahMarker(r[2]),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Stretch only inter-word spaces, never Arabic glyphs or diacritics.
            float measured=Layout.getDesiredWidth(span,text);
            int spaces=Math.max(0,line.size()-1);
            boolean finalLine=page==repository.pages.size()-1&&lineIndex==lines.size()-1;
            if(spaces>0&&!finalLine&&measured<660){
                int space=(int)Math.floor(text.measureText(" ")+(660-measured)/spaces);
                for(int j=0;j<s.length();j++)if(s.charAt(j)==' ')span.setSpan(new WordSpace(space),j,j+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            strings.add(span);offsets.add(ranges);
            layouts.add(StaticLayout.Builder.obtain(span,0,span.length(),text,TEXT_WIDTH)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build());
        }
        highlight(ayah,active,read,hidden);requestLayout();invalidate();
    }
    public void highlight(int verse,int word,int readThrough,boolean hide){
        ayah=verse;active=word;read=readThrough;hidden=hide;
        for(int i=0;i<strings.size();i++){
            SpannableString s=strings.get(i);
            for(BackgroundColorSpan span:s.getSpans(0,s.length(),BackgroundColorSpan.class))s.removeSpan(span);
            for(ForegroundColorSpan span:s.getSpans(0,s.length(),ForegroundColorSpan.class))s.removeSpan(span);
            int first=-1,last=-1;
            for(int[] r:offsets.get(i))if(r[2]==ayah){if(first<0)first=r[0];last=r[1];}
            if(first>=0)s.setSpan(new BackgroundColorSpan(0xffedf2dc),first,last,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for(int[] r:offsets.get(i))if(r[2]==ayah&&r[3]>=0){
                if(r[3]<=read)s.setSpan(new BackgroundColorSpan(0xffdce9bb),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(r[3]==active&&active>=0){
                    s.setSpan(new ForegroundColorSpan(0xff355a2a),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    s.setSpan(new BackgroundColorSpan(0xffc5da9e),r[0],r[1],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        StringBuilder description=new StringBuilder("صفحهٔ "+MainActivity.fa(page+1));
        if(!hidden)for(SpannableString s:strings)description.append('\n').append(s);
        else description.append("؛ متن برای آزمون پنهان است");
        setContentDescription(description);invalidate();
    }
    @Override protected void onMeasure(int ws,int hs){int width=MeasureSpec.getSize(ws);setMeasuredDimension(width,Math.round(width*(float)HEIGHT/WIDTH));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float scale=getWidth()/(float)WIDTH;canvas.save();canvas.scale(scale,scale);
        for(int i=0;i<layouts.size();i++){
            if(hidden){paint.setColor(0xffe0e9cd);canvas.drawRect(50,36+i*LINE_HEIGHT,710,56+i*LINE_HEIGHT,paint);}
            else {canvas.save();canvas.translate(30,8+i*LINE_HEIGHT+Math.max(0,(LINE_HEIGHT-layouts.get(i).getHeight())/2f));layouts.get(i).draw(canvas);canvas.restore();}
        }
        canvas.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN)return true;
        if(event.getAction()==MotionEvent.ACTION_UP){
            performClick();float scale=getWidth()/(float)WIDTH;float y=event.getY()/scale-8;int row=(int)(y/LINE_HEIGHT);
            if(y>=0&&row>=0&&row<layouts.size()){
                int offset=layouts.get(row).getOffsetForHorizontal(0,event.getX()/scale-30);
                for(int[] r:offsets.get(row))if(offset>=r[0]&&offset<=r[1]){selection.select(r[2]);break;}
            }return true;
        }return super.onTouchEvent(event);
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private static final class WordSpace extends ReplacementSpan {
        final int width;WordSpace(int width){this.width=width;}
        @Override public int getSize(Paint p,CharSequence t,int start,int end,Paint.FontMetricsInt fm){return width;}
        @Override public void draw(Canvas c,CharSequence t,int start,int end,float x,int top,int y,int bottom,Paint p){}
    }
    private static final class AyahMarker extends ReplacementSpan {
        final int number;AyahMarker(int number){this.number=number;}
        @Override public int getSize(Paint p,CharSequence t,int start,int end,Paint.FontMetricsInt fm){return 46;}
        @Override public void draw(Canvas c,CharSequence t,int start,int end,float x,int top,int y,int bottom,Paint original){
            Paint p=new Paint(original);p.setColor(0xff70884d);p.setStrokeWidth(1.2f);p.setStyle(Paint.Style.STROKE);
            float cx=x+23,cy=y-15;
            c.drawOval(cx-18,cy-25,cx+18,cy+25,p);c.drawOval(cx-15,cy-22,cx+15,cy+22,p);
            c.drawCircle(cx,cy-27,2,p);c.drawCircle(cx,cy+27,2,p);
            p.setStyle(Paint.Style.FILL);p.setTextSize(24);p.setTextAlign(Paint.Align.CENTER);
            String digits=String.valueOf(number).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤').replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');
            c.drawText(digits,cx,cy-(p.ascent()+p.descent())/2,p);
        }
    }
}
