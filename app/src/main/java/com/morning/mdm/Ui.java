package com.morning.mdm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/** Small UI kit so both screens share one look. */
public final class Ui {
    private Ui() {}

    public static final int BG = Color.parseColor("#12172B");
    public static final int PANEL = Color.parseColor("#1F2748");
    public static final int DAWN = Color.parseColor("#F2A65A");
    public static final int DAWN2 = Color.parseColor("#E8735B");
    public static final int TEXT = Color.parseColor("#F3EFE6");
    public static final int MUTED = Color.parseColor("#8891B5");
    public static final int SAGE = Color.parseColor("#92B48C");
    public static final int LINE = Color.parseColor("#2A3152");
    public static final int ON_DAWN = Color.parseColor("#20140A");

    public static final Typeface SERIF = Typeface.create("serif", Typeface.BOLD);
    public static final Typeface MONO = Typeface.create("monospace", Typeface.BOLD);
    public static final Typeface SANS = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface SANS_MED = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static TextView text(Context c, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        return t;
    }

    /** Small uppercase label in the mono face. */
    public static TextView label(Context c, String s, int color) {
        TextView t = text(c, 11, color, MONO);
        t.setAllCaps(true);
        t.setLetterSpacing(0.14f);
        t.setText(s);
        return t;
    }

    public static Button primary(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(ON_DAWN);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        b.setTypeface(SANS_MED);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{DAWN, DAWN2});
        g.setCornerRadius(dp(c, 16));
        b.setBackground(g);
        b.setPadding(dp(c, 20), dp(c, 18), dp(c, 20), dp(c, 18));
        b.setStateListAnimator(null);
        return b;
    }

    public static Button ghost(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(MUTED);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTypeface(SANS);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setStroke(dp(c, 1), LINE);
        g.setCornerRadius(dp(c, 14));
        b.setBackground(g);
        b.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        b.setStateListAnimator(null);
        return b;
    }

    public static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        GradientDrawable g = new GradientDrawable();
        g.setColor(PANEL);
        g.setStroke(dp(c, 1), LINE);
        g.setCornerRadius(dp(c, 12));
        e.setBackground(g);
        e.setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 14));
        return e;
    }

    public static GradientDrawable panel(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(PANEL);
        g.setCornerRadius(dp(c, 18));
        return g;
    }

    /** Circular countdown ring with a number in the middle. */
    public static class RingView extends View {
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private float fraction = 0f;
        private String label = "";

        public RingView(Context c) {
            super(c);
            float stroke = dp(c, 7);
            bg.setStyle(Paint.Style.STROKE);
            bg.setStrokeWidth(stroke);
            bg.setColor(LINE);
            fg.setStyle(Paint.Style.STROKE);
            fg.setStrokeWidth(stroke);
            fg.setStrokeCap(Paint.Cap.ROUND);
            txt.setColor(TEXT);
            txt.setTypeface(MONO);
            txt.setTextAlign(Paint.Align.CENTER);
        }

        /** fraction elapsed, 0..1 */
        public void set(float fraction, String label) {
            this.fraction = Math.max(0f, Math.min(1f, fraction));
            this.label = label;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            float size = Math.min(w, h);
            float pad = bg.getStrokeWidth();
            oval.set((w - size) / 2 + pad, (h - size) / 2 + pad,
                    (w + size) / 2 - pad, (h + size) / 2 - pad);
            canvas.drawArc(oval, 0, 360, false, bg);
            fg.setShader(new LinearGradient(oval.left, oval.top, oval.right, oval.bottom,
                    DAWN, DAWN2, Shader.TileMode.CLAMP));
            float sweep = 360f * (1f - fraction);
            if (sweep > 0.5f) canvas.drawArc(oval, -90, sweep, false, fg);
            txt.setTextSize(size * (label.length() > 4 ? 0.2f : 0.26f));
            float y = h / 2 - (txt.descent() + txt.ascent()) / 2;
            canvas.drawText(label, w / 2, y, txt);
        }
    }

    /** Row of short bars showing which step you are on. */
    public static class StepBar extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private int count = 1, current = 0;

        public StepBar(Context c) {
            super(c);
        }

        public void set(int count, int current) {
            this.count = Math.max(1, count);
            this.current = current;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float gap = dp(getContext(), 5);
            float w = (getWidth() - gap * (count - 1)) / count;
            float h = getHeight();
            for (int i = 0; i < count; i++) {
                float x = i * (w + gap);
                r.set(x, 0, x + w, h);
                if (i < current) {
                    p.setShader(null);
                    p.setColor(SAGE);
                } else if (i == current) {
                    p.setShader(new LinearGradient(x, 0, x + w, 0, DAWN, DAWN2, Shader.TileMode.CLAMP));
                } else {
                    p.setShader(null);
                    p.setColor(LINE);
                }
                canvas.drawRoundRect(r, h / 2, h / 2, p);
            }
        }
    }

    public static void center(TextView t) {
        t.setGravity(Gravity.CENTER_HORIZONTAL);
    }
}
