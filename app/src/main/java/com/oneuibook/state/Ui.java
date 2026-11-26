package com.oneuibook.state;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

/** Theme, metrics, typography, and critically-damped springs for State. */
final class Ui {
    private Ui() {}

    static float density = 3f;
    static float fontScale = 1f;
    static boolean dark;
    static boolean glass = true;
    static boolean gStyle = false;
    static float glassT = 0.5f;
    static int screenW, screenH;
    static int statusInset, navInset, imeInset, cutoutLeft, cutoutRight;

    static final int ACCENT_DEFAULT = 0xFF4F46E5;
    static int ACCENT = ACCENT_DEFAULT;
    static final String[] ACCENT_KEYS = {"indigo", "blue", "green", "red", "violet", "teal", "orange", "pink", "dynamic"};
    static final int[] ACCENT_COLORS = {0xFF4F46E5, 0xFF387AFF, 0xFF19B36B, 0xFFE92D44, 0xFF7B61FF, 0xFF0FB59D, 0xFFF26D21, 0xFFE84593, 0};
    static final int[] ACCENT_LABELS = {
            R.string.accent_indigo, R.string.accent_blue, R.string.accent_green, R.string.accent_red,
            R.string.accent_violet, R.string.accent_teal, R.string.accent_orange, R.string.accent_pink, R.string.accent_dynamic
    };

    static final TimeInterpolator EMPHASIZED = new PathInterpolator(0.2f, 0f, 0f, 1f);
    static final TimeInterpolator STANDARD = new PathInterpolator(0.2f, 0f, 0f, 1f);
    static final TimeInterpolator RECOIL_RELEASE = new PathInterpolator(0.22f, 0.25f, 0f, 1f);

    static Typeface regular, medium, semibold, bold, heavy;

    static String accentKey(SharedPreferences p) { return p.getString("accent_color", "indigo"); }

    static int resolveAccent(Context c, String key) {
        if ("dynamic".equals(key)) {
            try {
                int col = c.getColor(dark ? android.R.color.system_accent1_300 : android.R.color.system_accent1_600);
                return (col & 0xFFFFFF) | 0xFF000000;
            } catch (Throwable ignored) { return ACCENT_DEFAULT; }
        }
        for (int i = 0; i < ACCENT_KEYS.length; i++)
            if (ACCENT_KEYS[i].equals(key) && ACCENT_COLORS[i] != 0) return ACCENT_COLORS[i];
        return ACCENT_DEFAULT;
    }

    static void init(Context c, SharedPreferences p) {
        density = c.getResources().getDisplayMetrics().density;
        fontScale = Math.min(1.3f, c.getResources().getConfiguration().fontScale);
        screenW = c.getResources().getDisplayMetrics().widthPixels;
        screenH = c.getResources().getDisplayMetrics().heightPixels;
        String theme = p.getString("theme", "system");
        if ("dark".equals(theme)) dark = true;
        else if ("light".equals(theme)) dark = false;
        else dark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        glass = p.getBoolean("glass", true);
        glassT = clamp(p.getFloat("glass_t", 0.5f), 0f, 1f);
        gStyle = "g".equals(p.getString("ui_style", "a"));
        ACCENT = resolveAccent(c, accentKey(p));
        Typeface base = Typeface.SANS_SERIF;
        regular = Typeface.create(base, 400, false);
        medium = Typeface.create(base, 500, false);
        semibold = Typeface.create(base, 600, false);
        bold = Typeface.create(base, 700, false);
        heavy = Typeface.create(base, 800, false);
    }

    static int dp(float v) { return Math.round(v * density); }
    static float dpf(float v) { return v * density; }
    static float sp(float v) { return v * density * fontScale; }

    static int bg() { return dark ? 0xFF06070B : 0xFFF2F3F8; }
    static int groupedBg() { return dark ? 0xFF06070B : 0xFFF2F3F8; }
    static int card() { return dark ? 0xFF151720 : 0xFFFFFFFF; }
    static int text() { return dark ? 0xFFF8F9FC : 0xFF0E1117; }
    static int text2() { return dark ? 0xA6E2E6F2 : 0x993C404B; }
    static int text3() { return dark ? 0x66E2E6F2 : 0x553C404B; }
    static int chip() { return dark ? 0xFF1D202C : 0xFFE6E8F0; }
    static int separator() { return dark ? 0x1EFFFFFF : 0x12000000; }
    static int pressed() { return dark ? 0x26FFFFFF : 0x12000000; }
    static int glassTint() { return dark ? 0x5C191B24 : 0x5CFAFAFC; }
    static int glassStroke() { return dark ? 0x30FFFFFF : 0x20000000; }
    static int gStroke() { return dark ? 0x1FFFFFFF : 0x12000000; }
    static int gOutlineTop() { return dark ? 0x24FFFFFF : 0xB2FFFFFF; }
    static float cardRadius() { return gStyle ? 26f : 20f; }

    static int gTint(int t) {
        int a = t >>> 24;
        if (a >= 242) return t;
        int rgb = (dark ? 0xFF2B2D36 : 0xFFFCFCFF) & 0xFFFFFF;
        int minA = Math.round(lerp(dark ? 236f : 244f, dark ? 182f : 188f, glassT));
        return (Math.max(a, minA) << 24) | rgb;
    }

    static int glassBlend(int opaque, int medium, int high) {
        return glassT >= 0.5f ? mix(medium, high, (glassT - 0.5f) * 2f) : mix(opaque, medium, glassT * 2f);
    }

    static int barTint() {
        return dark ? glassBlend(0xFF191B24, 0xA6191B24, 0x52191B24) : glassBlend(0xFFFAFAFC, 0xA8FAFAFC, 0x42FFFFFF);
    }

    static float barBlur() { return glassT >= 0.5f ? lerp(6f, 2f, (glassT - 0.5f) * 2f) : 6f; }

    static int menuTint() {
        return dark ? glassBlend(0xF5191B24, 0xB8191B24, 0x8C191B24) : glassBlend(0xF5FAFAFC, 0xBDFAFAFC, 0x8CFAFAFC);
    }

    static int alpha(int color, float a) {
        return (Math.round(Color.alpha(color) * clamp(a, 0f, 1f)) << 24) | (color & 0xFFFFFF);
    }

    static int mix(int a, int b, float t) {
        t = clamp(t, 0f, 1f);
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, aa = a >>> 24;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255, ba = b >>> 24;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

    static TextView text(Context c, CharSequence s, float sizeSp, Typeface tf, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    static GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dpf(radiusDp));
        return g;
    }

    static void press(View v, float scale) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(scale).scaleY(scale).setDuration(110).setInterpolator(STANDARD).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(RECOIL_RELEASE).start();
                    break;
            }
            return false;
        });
    }

    static final class Spring implements Choreographer.FrameCallback {
        float value, target, velocity;
        float stiffness, damping, precision;
        boolean running;
        private long last;
        Runnable onUpdate, onEnd;

        Spring(float stiffness, float dampingRatio, float precision) {
            this.stiffness = stiffness;
            this.damping = dampingRatio;
            this.precision = precision;
        }

        void to(float t) {
            target = t;
            if (!running) {
                running = true;
                last = 0;
                Choreographer.getInstance().postFrameCallback(this);
            }
        }

        void snap(float v) {
            if (running) Choreographer.getInstance().removeFrameCallback(this);
            running = false;
            value = target = v;
            velocity = 0;
            if (onUpdate != null) onUpdate.run();
        }

        @Override public void doFrame(long now) {
            float dt = last == 0 ? 1f / 120f : Math.min((now - last) / 1e9f, 1f / 30f);
            last = now;
            int steps = 6;
            float h = dt / steps;
            float w0 = (float) Math.sqrt(stiffness);
            float c = 2f * damping * w0;
            for (int i = 0; i < steps; i++) {
                float a = -stiffness * (value - target) - c * velocity;
                velocity += a * h;
                value += velocity * h;
            }
            if (Math.abs(velocity) < precision * 6 && Math.abs(value - target) < precision) {
                value = target;
                velocity = 0;
                running = false;
                if (onUpdate != null) onUpdate.run();
                if (onEnd != null) onEnd.run();
                return;
            }
            if (onUpdate != null) onUpdate.run();
            Choreographer.getInstance().postFrameCallback(this);
        }
    }
}
