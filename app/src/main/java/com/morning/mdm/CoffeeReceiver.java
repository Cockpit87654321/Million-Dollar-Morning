package com.morning.mdm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Tells the user the two-hour caffeine window is over. */
public class CoffeeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Core.ensureChannels(c);
        PendingIntent open = PendingIntent.getActivity(c, 4, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, Core.CH_INFO)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Coffee is fine now")
                .setContentText("Two hours since you got up. Enjoy it.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(Core.NOTIF_COFFEE, n);
    }
}
