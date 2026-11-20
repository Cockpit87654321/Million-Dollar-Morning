package com.morning.mdm;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import java.util.Calendar;
import java.util.Date;

/** Shared helpers: settings, alarm scheduling, notification channels. */
public final class Core {
    private Core() {}

    public static final String CH_ALARM = "mdm_alarm_v1";
    public static final String CH_INFO = "mdm_info_v1";
    public static final int NOTIF_ALARM = 1;
    public static final int NOTIF_COFFEE = 2;

    // preference keys
    public static final String K_ALARM_ON = "alarm_on";
    public static final String K_HOUR = "hour";
    public static final String K_MINUTE = "minute";
    public static final String K_MOVE = "move_min";
    public static final String K_VOICE = "voice";
    public static final String K_AFFIRM = "affirm";
    public static final String K_STREAK = "streak";
    public static final String K_LAST_DONE = "last_done";
    public static final String K_CAFFEINE = "caffeine_until";

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("mdm", Context.MODE_PRIVATE);
    }

    public static String today() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
    }

    public static String yesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
    }

    /** Current streak, zero if the last completed day was before yesterday. */
    public static int currentStreak(Context c) {
        SharedPreferences sp = prefs(c);
        String last = sp.getString(K_LAST_DONE, "");
        if (last.equals(today()) || last.equals(yesterday())) return sp.getInt(K_STREAK, 0);
        return 0;
    }

    public static void markDone(Context c) {
        SharedPreferences sp = prefs(c);
        String last = sp.getString(K_LAST_DONE, "");
        int streak = sp.getInt(K_STREAK, 0);
        if (!last.equals(today())) {
            streak = last.equals(yesterday()) ? streak + 1 : 1;
        }
        sp.edit().putInt(K_STREAK, streak).putString(K_LAST_DONE, today()).apply();
    }

    public static String formatTime(Context c, long millis) {
        return android.text.format.DateFormat.getTimeFormat(c).format(new Date(millis));
    }

    public static String formatTime(Context c, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return android.text.format.DateFormat.getTimeFormat(c).format(cal.getTime());
    }

    public static void ensureChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(CH_ALARM) == null) {
            NotificationChannel ch = new NotificationChannel(CH_ALARM, "Wake-up alarm",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Rings and opens the morning routine");
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            ch.setSound(sound, aa);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 600, 400, 600});
            ch.setBypassDnd(true);
            ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
        if (nm.getNotificationChannel(CH_INFO) == null) {
            NotificationChannel ch = new NotificationChannel(CH_INFO, "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Tells you when coffee is fine again");
            nm.createNotificationChannel(ch);
        }
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class);
        return PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static boolean canScheduleExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Next trigger time for the saved alarm, or -1 if off. */
    public static long nextTrigger(Context c) {
        SharedPreferences sp = prefs(c);
        if (!sp.getBoolean(K_ALARM_ON, false)) return -1;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, sp.getInt(K_HOUR, 6));
        cal.set(Calendar.MINUTE, sp.getInt(K_MINUTE, 30));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis();
    }

    /** (Re)schedules the daily alarm. Returns false if exact alarms are not allowed. */
    public static boolean scheduleAlarm(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return false;
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        long at = nextTrigger(c);
        if (at < 0) return true;
        if (!canScheduleExact(c)) return false;
        Intent show = new Intent(c, MainActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(c, 1, show,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, showPi), pi);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /** One-off alarm a minute from now, for testing. */
    public static boolean scheduleTestAlarm(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null || !canScheduleExact(c)) return false;
        Intent i = new Intent(c, AlarmReceiver.class).putExtra("test", true);
        PendingIntent pi = PendingIntent.getBroadcast(c, 9, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent show = new Intent(c, MainActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(c, 1, show,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 60_000, showPi), pi);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /** Starts the two-hour caffeine window and a reminder when it ends. */
    public static long startCaffeineWindow(Context c) {
        long until = System.currentTimeMillis() + 2L * 60 * 60 * 1000;
        prefs(c).edit().putLong(K_CAFFEINE, until).apply();
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am != null) {
            Intent i = new Intent(c, CoffeeReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, 3, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pi);
        }
        return until;
    }

    public static long caffeineUntil(Context c) {
        long until = prefs(c).getLong(K_CAFFEINE, 0);
        return until > System.currentTimeMillis() ? until : 0;
    }
}
