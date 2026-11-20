package com.morning.mdm;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** The nine steps of the morning, in the order the user chose. */
public final class Steps {
    private Steps() {}

    public static final int COUNTDOWN = 0, TIMER = 1, HOLD = 2, INPUT = 3;

    public static final class Step {
        public final String rule, title, line, noPhone, voice, action;
        public final int type, seconds;

        Step(String rule, String title, String line, String noPhone, String voice,
             int type, int seconds, String action) {
            this.rule = rule;
            this.title = title;
            this.line = line;
            this.noPhone = noPhone;
            this.voice = voice;
            this.type = type;
            this.seconds = seconds;
            this.action = action;
        }
    }

    public static List<Step> build(Context c) {
        SharedPreferences sp = Core.prefs(c);
        String affirm = sp.getString(Core.K_AFFIRM, "").trim();
        if (affirm.isEmpty()) affirm = "I follow through on what I promise myself.";
        int move = Math.max(1, Math.min(30, sp.getInt(Core.K_MOVE, 10)));
        String noPhone = "No phone, email or TV";

        List<Step> s = new ArrayList<>();
        s.add(new Step("Rule 1 · No snooze", "5-4-3-2-1, get up",
                "That alarm is a promise you made last night. Snoozing traps your brain in grogginess for hours. Count down and move.",
                noPhone,
                "Good morning. That alarm is a promise you made to yourself last night. No snooze. Five, four, three, two, one. Get up.",
                COUNTDOWN, 5, "I'm up"));
        s.add(new Step("Rule 2 · Tackle it first", "Make your bed",
                "One small finished thing sets off the rest of the day. It is also a gift to the you who climbs back in tonight.",
                noPhone,
                "Good. Now make your bed. It is the first thing you finish today, and a gift to the you who comes back to this room tonight.",
                TIMER, 60, "Done"));
        s.add(new Step("Rule 3 · High five habit", "Say it. High-five the mirror.",
                "Decide who you want to be today. Say it out loud: \"" + affirm + "\" Then look yourself in the eye and high-five the mirror.",
                noPhone,
                "Go to the mirror. Decide who you want to be today. Say it out loud: " + affirm
                        + " Then look yourself in the eye and give yourself a high five. You've got this.",
                TIMER, 60, "High-fived"));
        s.add(new Step("Fuel it right", "Loaded water. Protein breakfast. No caffeine.",
                "Drink a full glass of water first, then eat a protein-rich breakfast. No coffee or tea for two hours.",
                noPhone,
                "Drink a full glass of water first. Then eat a protein-rich breakfast. And no caffeine for the next two hours.",
                HOLD, 0, "Done"));
        s.add(new Step("Rule 4 · Move your body", "Move your body",
                move + " minutes at a brisk pace. Walk like you're late. Music or a podcast is fine. Texts and email are not.",
                noPhone,
                "Move your body for " + move + " minutes at a brisk pace, like you're late. Music or a podcast is fine. No texts, no email.",
                TIMER, move * 60, "Back"));
        s.add(new Step("Rule 4 · Move your mind", "Move your mind",
                "One quiet minute. Journal, meditate, pray, or play a calming recording.",
                noPhone,
                "Now move your mind. One quiet minute. Journal, meditate, pray, or just breathe.",
                TIMER, 60, "Done"));
        s.add(new Step("Rule 5 · Progress principle", "Name one thing that matters",
                "Not busywork. The one thing that would move your life forward if you touched it today.",
                noPhone,
                "Before you look at your phone, name the one thing that actually matters today. Type it in.",
                INPUT, 0, "Lock it in"));
        s.add(new Step("Rule 5 · Make progress", "5 minutes of real progress",
                "Small progress every day is what builds big things. Give it five real minutes now.",
                noPhone,
                "Give it five real minutes right now, before anything else gets your attention.",
                TIMER, 300, "Done"));
        s.add(new Step("Daylight", "Get into the sunlight",
                "Step outside, or at least to a window. A few minutes of daylight resets your mood and tonight's sleep.",
                "Still no phone, email or TV",
                "Last step. Step outside, or get to a window. Let daylight hit your face for a few minutes.",
                TIMER, 180, "Done. Phone can come out now"));
        return s;
    }
}
