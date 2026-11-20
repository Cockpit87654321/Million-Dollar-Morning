package com.morning.mdm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Home screen: wake-up time, settings, permission checks, manual start. */
public class MainActivity extends Activity {

    private TextView timeView, nextView, streakView, cafView, warnView;
    private Switch alarmSwitch, voiceSwitch;
    private EditText affirmInput, moveInput;
    private Button fixButton;
    private Runnable fixAction;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Core.ensureChannels(this);
        buildLayout();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Core.scheduleAlarm(this);
        refresh();
    }

    private void buildLayout() {
        Context c = this;
        int pad = Ui.dp(c, 22);

        ScrollView scroll = new ScrollView(c);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, Ui.dp(c, 28), pad, Ui.dp(c, 28));
        scroll.addView(root);

        root.addView(Ui.label(c, "Mel Robbins · after the alarm", Ui.MUTED), lp(c, 6));
        TextView title = Ui.text(c, 32, Ui.TEXT, Ui.SERIF);
        title.setText("Million-Dollar Morning");
        root.addView(title, lp(c, 8));
        TextView sub = Ui.text(c, 15, Ui.MUTED, Ui.SANS);
        sub.setText("This app is your alarm. When it rings, the routine opens on its own, even on the lock screen, and talks you through every step.");
        sub.setLineSpacing(0, 1.3f);
        root.addView(sub, lp(c, 22));

        // alarm card
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.panel(c));
        card.setPadding(Ui.dp(c, 18), Ui.dp(c, 16), Ui.dp(c, 18), Ui.dp(c, 16));
        root.addView(card, lp(c, 12));

        LinearLayout timeRow = new LinearLayout(c);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        timeView = Ui.text(c, 44, Ui.TEXT, Ui.MONO);
        timeView.setOnClickListener(v -> pickTime());
        timeRow.addView(timeView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        alarmSwitch = new Switch(c);
        alarmSwitch.setOnCheckedChangeListener((b, on) -> {
            Core.prefs(this).edit().putBoolean(Core.K_ALARM_ON, on).apply();
            if (!Core.scheduleAlarm(this)) {
                Toast.makeText(this, "Allow alarms first (see below)", Toast.LENGTH_LONG).show();
            }
            refresh();
        });
        timeRow.addView(alarmSwitch);
        card.addView(timeRow);

        nextView = Ui.text(c, 13, Ui.MUTED, Ui.SANS);
        card.addView(nextView, lp(c, 0));
        TextView tapHint = Ui.text(c, 12, Ui.MUTED, Ui.SANS);
        tapHint.setText("Tap the time to change it. Rings every day.");
        card.addView(tapHint, lp(c, 0));

        // permission warning
        warnView = Ui.text(c, 14, Ui.DAWN2, Ui.SANS);
        warnView.setLineSpacing(0, 1.25f);
        root.addView(warnView, lp(c, 8));
        fixButton = Ui.ghost(c, "Fix it");
        fixButton.setOnClickListener(v -> {
            if (fixAction != null) fixAction.run();
        });
        root.addView(fixButton, lp(c, 16));

        streakView = Ui.label(c, "", Ui.SAGE);
        root.addView(streakView, lp(c, 4));
        cafView = Ui.label(c, "", Ui.DAWN);
        root.addView(cafView, lp(c, 20));

        // settings
        root.addView(Ui.label(c, "Your affirmation (step 3)", Ui.MUTED), lp(c, 8));
        affirmInput = Ui.input(c, "I follow through on what I promise myself.");
        affirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(affirmInput, lp(c, 16));

        root.addView(Ui.label(c, "Move your body (minutes)", Ui.MUTED), lp(c, 8));
        moveInput = Ui.input(c, "10");
        moveInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(moveInput, lp(c, 16));

        LinearLayout voiceRow = new LinearLayout(c);
        voiceRow.setOrientation(LinearLayout.HORIZONTAL);
        voiceRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView voiceLabel = Ui.text(c, 15, Ui.TEXT, Ui.SANS);
        voiceLabel.setText("Voice guidance");
        voiceRow.addView(voiceLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        voiceSwitch = new Switch(c);
        voiceRow.addView(voiceSwitch);
        root.addView(voiceRow, lp(c, 18));

        Button save = Ui.primary(c, "Save settings");
        save.setOnClickListener(v -> saveSettings());
        root.addView(save, lp(c, 10));

        Button now = Ui.ghost(c, "Start the routine now");
        now.setOnClickListener(v -> startActivity(new Intent(this, RoutineActivity.class)));
        root.addView(now, lp(c, 10));

        Button test = Ui.ghost(c, "Test the alarm in 1 minute");
        test.setOnClickListener(v -> {
            if (Core.scheduleTestAlarm(this)) {
                Toast.makeText(this, "Test alarm in 1 minute. Lock your phone to see it.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Allow alarms first (see above)", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(test, lp(c, 0));

        setContentView(scroll);

        SharedPreferences sp = Core.prefs(this);
        affirmInput.setText(sp.getString(Core.K_AFFIRM, ""));
        moveInput.setText(String.valueOf(sp.getInt(Core.K_MOVE, 10)));
        voiceSwitch.setChecked(sp.getBoolean(Core.K_VOICE, true));
    }

    private static LinearLayout.LayoutParams lp(Context c, int bottomDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = Ui.dp(c, bottomDp);
        return p;
    }

    private void pickTime() {
        SharedPreferences sp = Core.prefs(this);
        new TimePickerDialog(this, android.R.style.Theme_DeviceDefault_Dialog, (v, hour, minute) -> {
            sp.edit().putInt(Core.K_HOUR, hour).putInt(Core.K_MINUTE, minute)
                    .putBoolean(Core.K_ALARM_ON, true).apply();
            Core.scheduleAlarm(this);
            refresh();
        }, sp.getInt(Core.K_HOUR, 6), sp.getInt(Core.K_MINUTE, 30),
                android.text.format.DateFormat.is24HourFormat(this)).show();
    }

    private void saveSettings() {
        int move;
        try {
            move = Integer.parseInt(moveInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            move = 10;
        }
        move = Math.max(1, Math.min(30, move));
        moveInput.setText(String.valueOf(move));
        Core.prefs(this).edit()
                .putString(Core.K_AFFIRM, affirmInput.getText().toString().trim())
                .putInt(Core.K_MOVE, move)
                .putBoolean(Core.K_VOICE, voiceSwitch.isChecked())
                .apply();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        SharedPreferences sp = Core.prefs(this);
        timeView.setText(Core.formatTime(this, sp.getInt(Core.K_HOUR, 6), sp.getInt(Core.K_MINUTE, 30)));
        boolean on = sp.getBoolean(Core.K_ALARM_ON, false);
        alarmSwitch.setOnCheckedChangeListener(null);
        alarmSwitch.setChecked(on);
        alarmSwitch.setOnCheckedChangeListener((b, isOn) -> {
            Core.prefs(this).edit().putBoolean(Core.K_ALARM_ON, isOn).apply();
            if (!Core.scheduleAlarm(this)) {
                Toast.makeText(this, "Allow alarms first (see below)", Toast.LENGTH_LONG).show();
            }
            refresh();
        });
        long next = Core.nextTrigger(this);
        nextView.setText(next > 0 ? "Alarm on. Next: " + nextLabel(next) : "Alarm off");
        timeView.setTextColor(on ? Ui.TEXT : Ui.MUTED);

        int streak = Core.currentStreak(this);
        streakView.setText(streak > 0 ? streak + "-day streak" : "No streak yet. Tomorrow starts it");
        long caf = Core.caffeineUntil(this);
        cafView.setText(caf > 0 ? "No caffeine until " + Core.formatTime(this, caf) : "");
        cafView.setVisibility(caf > 0 ? View.VISIBLE : View.GONE);

        updateWarning();
    }

    private String nextLabel(long at) {
        java.util.Calendar a = java.util.Calendar.getInstance();
        a.setTimeInMillis(at);
        java.util.Calendar now = java.util.Calendar.getInstance();
        String day = a.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
                ? "today" : "tomorrow";
        return day + " at " + Core.formatTime(this, at);
    }

    private void updateWarning() {
        String msg = null;
        fixAction = null;
        NotificationManager nm = getSystemService(NotificationManager.class);

        if (!Core.canScheduleExact(this)) {
            msg = "Alarms are not allowed yet. Without this the app cannot wake you.";
            fixAction = () -> startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())));
        } else if (nm != null && !nm.areNotificationsEnabled()) {
            msg = "Notifications are off. The alarm needs them to ring and open the routine.";
            fixAction = () -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        } else if (Build.VERSION.SDK_INT >= 34 && nm != null && !nm.canUseFullScreenIntent()) {
            msg = "Full-screen alarms are off. Turn them on so the routine opens over the lock screen.";
            fixAction = () -> startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + getPackageName())));
        }

        boolean show = msg != null;
        warnView.setText(show ? msg : "");
        warnView.setVisibility(show ? View.VISIBLE : View.GONE);
        fixButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refresh();
    }
}
