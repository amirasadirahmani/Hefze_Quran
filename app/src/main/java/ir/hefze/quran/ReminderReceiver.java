package ir.hefze.quran;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Calendar;

public final class ReminderReceiver extends BroadcastReceiver {
    public static void schedule(Context context,boolean enabled){
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi=PendingIntent.getBroadcast(context,36,new Intent(context,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pi);if(!enabled)return;
        Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,20);next.set(Calendar.MINUTE,0);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
        if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);
    }
    @Override public void onReceive(Context context,Intent intent){
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        StudyStore store=new StudyStore(context);
        if(!store.flag("reminder")){store.close();return;}
        int due=store.due().size();store.close();
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("daily","تمرین روزانهٔ حفظ",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        manager.notify(36,new Notification.Builder(context,"daily").setSmallIcon(ir.hefze.quran.R.drawable.ic_book).setContentTitle("وقت تمرین یاسین")
            .setContentText(due>0?MainActivity.fa(due)+" آیه برای مرور دارید.":"چند دقیقه برای حفظ و مرور امروز.").setContentIntent(open).setAutoCancel(true).build());
    }
}
