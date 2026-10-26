package ir.hefze.quran;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Reinstates the user's alarm after a reboot or an app update. */
public final class ReminderBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        StudyStore store = new StudyStore(context);
        try {
            ReminderReceiver.schedule(context, store.flag("reminder"));
        } finally {
            store.close();
        }
    }
}
