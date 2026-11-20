package com.morning.mdm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Full-screen routine: rings, then guides through every step with visuals and voice. */
public class RoutineActivity extends Activity {
    public static final String EXTRA_FROM_ALARM = "from_alarm";

    private static final long RING_LIMIT_MS = 10 * 60 * 1000;
    private static final long WAKE_REPEAT_MS = 25 * 1000;

    private final Handler h = new Handler(Looper.getMainLooper());
    private List<Steps.Step> steps;
    private int index = 0;
    private boolean ringing = false;
    private boolean finished = false;
    private String task = "";

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String pendingSpeech = null;
    private MediaPlayer player;
    private Vibrator vibrator;

    private Ui.StepBar bar;
    private Ui.RingView ring;
    private TextView ruleView, titleView, lineView, phoneView, cafView, voiceToggle;
    private EditText taskInput;
    private Button action;

    private Runnable tick;
    private long stepEndsAt;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        vibrator = getSystemService(Vibrator.class);
        steps = Steps.build(this);
        buildLayout();
        initTts();

        boolean fromAlarm = getIntent().getBooleanExtra(EXTRA_FROM_ALARM, false);
        cancelAlarmNotification();
        if (fromAlarm) startRinging();
        showStep(0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        cancelAlarmNotification();
        if (intent.getBooleanExtra(EXTRA_FROM_ALARM, false) && !ringing) {
            finished = false;
            task = "";
            steps = Steps.build(this);
            startRinging();
            showStep(0);
        }
    }

    // ---------- layout ----------

    private void buildLayout() {
        Context c = this;
        int pad = Ui.dp(c, 22);

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(pad, Ui.dp(c, 24), pad, Ui.dp(c, 20));

        bar = new Ui.StepBar(c);
        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 5)));

        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        LinearLayout mid = new LinearLayout(c);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        mid.setPadding(0, Ui.dp(c, 16), 0, Ui.dp(c, 16));
        scroll.addView(mid, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        ring = new Ui.RingView(c);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(Ui.dp(c, 210), Ui.dp(c, 210));
        ringLp.bottomMargin = Ui.dp(c, 18);
        mid.addView(ring, ringLp);

        ruleView = Ui.label(c, "", Ui.DAWN);
        Ui.center(ruleView);
        mid.addView(ruleView, wrap(c, 10));

        titleView = Ui.text(c, 27, Ui.TEXT, Ui.SERIF);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        mid.addView(titleView, wrap(c, 12));

        lineView = Ui.text(c, 15, Ui.MUTED, Ui.SANS);
        lineView.setGravity(Gravity.CENTER_HORIZONTAL);
        lineView.setLineSpacing(0, 1.3f);
        mid.addView(lineView, wrap(c, 14));

        phoneView = Ui.label(c, "", Ui.DAWN2);
        Ui.center(phoneView);
        mid.addView(phoneView, wrap(c, 6));

        cafView = Ui.label(c, "", Ui.DAWN);
        Ui.center(cafView);
        mid.addView(cafView, wrap(c, 12));

        taskInput = Ui.input(c, "e.g. Send the proposal");
        taskInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        taskInput.setVisibility(View.GONE);
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mid.addView(taskInput, inLp);

        action = Ui.primary(c, "");
        action.setOnClickListener(v -> onAction());
        root.addView(action, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, Ui.dp(c, 12), 0, 0);
        voiceToggle = Ui.text(c, 13, Ui.MUTED, Ui.SANS);
        voiceToggle.setPadding(Ui.dp(c, 12), Ui.dp(c, 8), Ui.dp(c, 12), Ui.dp(c, 8));
        voiceToggle.setOnClickListener(v -> toggleVoice());
        row.addView(voiceToggle);
        TextView stop = Ui.text(c, 13, Ui.MUTED, Ui.SANS);
        stop.setText("Stop routine");
        stop.setPadding(Ui.dp(c, 12), Ui.dp(c, 8), Ui.dp(c, 12), Ui.dp(c, 8));
        stop.setOnLongClickListener(v -> {
            stopRinging();
            finish();
            return true;
        });
        stop.setOnClickListener(v -> lineView.setText("Hold \"Stop routine\" to end it."));
        row.addView(stop);
        root.addView(row);

        setContentView(root);
        updateVoiceLabel();
    }

    private static LinearLayout.LayoutParams wrap(Context c, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(c, bottomDp);
        return lp;
    }

    // ---------- steps ----------

    private void showStep(int i) {
        index = i;
        h.removeCallbacks(tick);
        Steps.Step s = steps.get(i);
        bar.set(steps.size(), i);
        ruleView.setText(s.rule);
        titleView.setText(s.title);
        lineView.setText(s.line);
        phoneView.setText(s.noPhone);
        action.setText(s.action);
        updateCaffeineLabel();

        taskInput.setVisibility(s.type == Steps.INPUT ? View.VISIBLE : View.GONE);
        if (s.type == Steps.INPUT) {
            taskInput.setText(task);
            taskInput.requestFocus();
        } else {
            hideKeyboard();
        }

        if (s.type == Steps.COUNTDOWN || s.type == Steps.TIMER) {
            ring.setVisibility(View.VISIBLE);
            startTimer(s);
        } else {
            ring.setVisibility(View.GONE);
        }

        if (ringing && i == 0) {
            speakOverAlarm(s.voice);
        } else {
            speak(s.voice);
        }
    }

    private void startTimer(Steps.Step s) {
        final long total = s.seconds * 1000L;
        stepEndsAt = System.currentTimeMillis() + total;
        tick = new Runnable() {
            @Override
            public void run() {
                long left = stepEndsAt - System.currentTimeMillis();
                if (left <= 0) {
                    ring.set(1f, s.type == Steps.COUNTDOWN ? "Up." : "Done");
                    if (s.type == Steps.COUNTDOWN && ringing) {
                        // keep counting while the alarm rings
                        h.postDelayed(() -> {
                            if (ringing && index == 0) startTimer(s);
                        }, 3000);
                    } else if (s.type == Steps.TIMER) {
                        buzz(new long[]{0, 120, 80, 120, 80, 240});
                        speak("Time's up.");
                    }
                    return;
                }
                int secs = (int) Math.ceil(left / 1000.0);
                String label = s.type == Steps.COUNTDOWN ? String.valueOf(secs)
                        : (secs / 60) + ":" + String.format(Locale.US, "%02d", secs % 60);
                ring.set(1f - (float) left / total, label);
                h.postDelayed(this, 200);
            }
        };
        h.post(tick);
    }

    private void onAction() {
        if (finished) {
            finish();
            return;
        }
        Steps.Step s = steps.get(index);
        if (index == 0) {
            stopRinging();
            Core.startCaffeineWindow(this);
        }
        if (s.type == Steps.INPUT) {
            task = taskInput.getText().toString().trim();
        }
        buzz(new long[]{0, 30});
        if (index + 1 < steps.size()) {
            showStep(index + 1);
            if (s.type == Steps.INPUT && !task.isEmpty()) {
                speak("Good. Five minutes on: " + task + ". Starting now.");
            }
        } else {
            showDone();
        }
    }

    private void showDone() {
        finished = true;
        h.removeCallbacks(tick);
        hideKeyboard();
        Core.markDone(this);
        int streak = Core.currentStreak(this);
        bar.set(steps.size(), steps.size());
        ring.setVisibility(View.GONE);
        taskInput.setVisibility(View.GONE);
        ruleView.setText("Done");
        titleView.setText("Good morning. You kept the promise.");
        lineView.setText(task.isEmpty()
                ? "All " + steps.size() + " steps done, before the phone even got a turn."
                : "All " + steps.size() + " steps done, and you already put five minutes into: \"" + task + "\".");
        phoneView.setText(streak + "-day streak");
        phoneView.setTextColor(Ui.SAGE);
        long caf = Core.caffeineUntil(this);
        cafView.setText(caf > 0 ? "Coffee is fine from " + Core.formatTime(this, caf) : "");
        action.setText("Finish");
        speak("That's the whole million dollar morning. " + streak + " day streak.");
        buzz(new long[]{0, 60, 50, 60, 50, 200});
    }

    private void updateCaffeineLabel() {
        long caf = Core.caffeineUntil(this);
        cafView.setText(index >= 3 && caf > 0 ? "No caffeine until " + Core.formatTime(this, caf) : "");
    }

    // ---------- alarm sound ----------

    private void startRinging() {
        ringing = true;
        try {
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            player = null;
        }
        buzzRepeat();
        h.postDelayed(ringLimit, RING_LIMIT_MS);
        h.postDelayed(wakeRepeat, WAKE_REPEAT_MS);
    }

    private final Runnable ringLimit = this::stopRinging;

    private final Runnable wakeRepeat = new Runnable() {
        @Override
        public void run() {
            if (!ringing) return;
            speakOverAlarm(steps.get(0).voice);
            h.postDelayed(this, WAKE_REPEAT_MS);
        }
    };

    private void stopRinging() {
        ringing = false;
        h.removeCallbacks(ringLimit);
        h.removeCallbacks(wakeRepeat);
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
        if (vibrator != null) vibrator.cancel();
        cancelAlarmNotification();
    }

    private void cancelAlarmNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(Core.NOTIF_ALARM);
    }

    private void duck(boolean down) {
        if (player != null) {
            float v = down ? 0.15f : 1f;
            try {
                player.setVolume(v, v);
            } catch (Exception ignored) {
            }
        }
    }

    // ---------- voice ----------

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            int r = tts.setLanguage(Locale.US);
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.ENGLISH);
            }
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            tts.setSpeechRate(0.95f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                }

                @Override
                public void onDone(String id) {
                    if ("wake".equals(id)) h.post(() -> duck(false));
                }

                @Override
                public void onError(String id) {
                    if ("wake".equals(id)) h.post(() -> duck(false));
                }
            });
            h.post(() -> {
                ttsReady = true;
                if (pendingSpeech != null) {
                    String p = pendingSpeech;
                    pendingSpeech = null;
                    if (ringing && index == 0) speakOverAlarm(p);
                    else speak(p);
                }
            });
        });
    }

    private boolean voiceOn() {
        return Core.prefs(this).getBoolean(Core.K_VOICE, true);
    }

    private void speak(String text) {
        if (!voiceOn()) return;
        if (!ttsReady) {
            pendingSpeech = text;
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step");
    }

    private void speakOverAlarm(String text) {
        if (!voiceOn()) return;
        if (!ttsReady) {
            pendingSpeech = text;
            return;
        }
        duck(true);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wake");
    }

    private void toggleVoice() {
        SharedPreferences sp = Core.prefs(this);
        boolean on = !sp.getBoolean(Core.K_VOICE, true);
        sp.edit().putBoolean(Core.K_VOICE, on).apply();
        if (!on && tts != null) tts.stop();
        if (!on) duck(false);
        updateVoiceLabel();
    }

    private void updateVoiceLabel() {
        voiceToggle.setText(voiceOn() ? "Voice on · Mute" : "Voice off · Unmute");
    }

    // ---------- misc ----------

    private void buzz(long[] pattern) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } catch (Exception ignored) {
        }
    }

    private void buzzRepeat() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 700, 700}, 0));
        } catch (Exception ignored) {
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null && taskInput != null) imm.hideSoftInputFromWindow(taskInput.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (finished) {
            super.onBackPressed();
        } else {
            lineView.setText("Hold \"Stop routine\" to end it.");
        }
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        stopRinging();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
