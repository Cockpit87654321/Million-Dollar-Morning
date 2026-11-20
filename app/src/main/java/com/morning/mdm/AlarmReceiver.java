package com.morning.mdm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires at wake-up time: rings and opens the routine over the lock screen. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Core.ensureChannels(c);

        Intent open = new Intent(c, RoutineActivity.class)
                .putExtra(RoutineActivity.EXTRA_FROM_ALARM, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent full = PendingIntent.getActivity(c, 2, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(c, Core.CH_ALARM)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Million-Dollar Morning")
                .setContentText("The alarm is a promise. 5-4-3-2-1, get up.")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .setOngoing(true)
                .setAutoCancel(true)
                .build();
        n.flags |= Notification.FLAG_INSISTENT;

        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(Core.NOTIF_ALARM, n);

        // Try to open the routine directly as well; Android may block this, the
        // full-screen notification above is the reliable path.
        try {
            c.startActivity(open);
        } catch (Exception ignored) {
        }

        if (!intent.getBooleanExtra("test", false)) {
            Core.scheduleAlarm(c); // tomorrow
        }
    }
}
