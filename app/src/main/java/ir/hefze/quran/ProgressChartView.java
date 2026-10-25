package ir.hefze.quran;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ProgressChartView extends View {
    private final long[] values;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public ProgressChartView(Context c,long[] values){super(c);this.values=values;setContentDescription("نمودار زمان گوش‌دادن در هفت روز گذشته");}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth()/7f,h=getHeight()-64;long max=60000;for(long n:values)max=Math.max(max,n);
        paint.setTextAlign(Paint.Align.CENTER);
        for(int i=0;i<7;i++){float x=(6-i)*w;float bar=(float)values[i]/max*(h-28);paint.setColor(0xffa9c877);c.drawRoundRect(x+w*.22f,h-bar,x+w*.78f,h,8,8,paint);
            paint.setColor(0xff304830);paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);
            c.drawText(MainActivity.fa(String.format(java.util.Locale.ROOT,"%.1f",values[i]/60000.0)),x+w/2,h-bar-10,paint);
            c.drawText(MainActivity.fa(LocalDate.now().minusDays(6-i).format(DateTimeFormatter.ofPattern("MM/dd"))),x+w/2,h+30,paint);
        }
    }
}
