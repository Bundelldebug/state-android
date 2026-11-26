package com.oneuibook.state;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared State UI components: rounded cards, category chips, Kyant0 LiquidToggle, glass slider, sheets. */
final class Widgets {
    private Widgets() {}

    static final LinearLayout.LayoutParams MW = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    static RippleDrawable ripple() {
        return new RippleDrawable(ColorStateList.valueOf(Ui.pressed()), null, Ui.round(0xFFFFFFFF, 16));
    }

    static TextView pillBadge(Context c, String label, int bg, int fg) {
        TextView tv = Ui.text(c, label, 11.5f, Ui.semibold, fg);
        tv.setPadding(Ui.dp(9), Ui.dp(4), Ui.dp(9), Ui.dp(4));
        tv.setBackground(Ui.round(bg, 100));
        return tv;
    }

    static final class Group extends LinearLayout {
        private final LinearLayout card;

        Group(Context c, String title) {
            super(c);
            setOrientation(VERTICAL);
            setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(8));
            if (title != null && !title.isEmpty()) {
                TextView t = Ui.text(c, title, 13f, Ui.semibold, Ui.text2());
                t.setPadding(Ui.dp(8), Ui.dp(2), Ui.dp(8), Ui.dp(8));
                addView(t, MW);
            }
            card = new LinearLayout(c);
            card.setOrientation(VERTICAL);
            GradientDrawable bg = Ui.round(Ui.card(), Ui.cardRadius());
            bg.setStroke(Math.max(1, Ui.dp(0.6f)), Ui.separator());
            card.setBackground(bg);
            addView(card, MW);
        }

        void addCustom(View v) {
            if (card.getChildCount() > 0) addDivider();
            card.addView(v, MW);
        }

        void addRow(int icon, int iconTint, String title, String value, boolean chevron, OnClickListener click) {
            if (card.getChildCount() > 0) addDivider();
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
            if (click != null) {
                row.setBackground(ripple());
                row.setOnClickListener(click);
            }
            if (icon != 0) {
                Icons.IconView iv = new Icons.IconView(getContext(), icon, 20, iconTint);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24));
                ilp.rightMargin = Ui.dp(12);
                row.addView(iv, ilp);
            }
            TextView tv = Ui.text(getContext(), title, 15f, Ui.medium, Ui.text());
            row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (value != null && !value.isEmpty()) {
                TextView vv = Ui.text(getContext(), value, 13.5f, Ui.regular, Ui.text2());
                LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                vlp.leftMargin = Ui.dp(8);
                row.addView(vv, vlp);
            }
            if (chevron) {
                Icons.IconView ch = new Icons.IconView(getContext(), Icons.CHEVRON, 16, Ui.text3());
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18));
                clp.leftMargin = Ui.dp(6);
                row.addView(ch, clp);
            }
            card.addView(row, MW);
        }

        void addToggle(String title, String sub, boolean initial, Toggle.OnChange cb) {
            if (card.getChildCount() > 0) addDivider();
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(12));
            LinearLayout col = new LinearLayout(getContext());
            col.setOrientation(VERTICAL);
            col.addView(Ui.text(getContext(), title, 15f, Ui.medium, Ui.text()));
            if (sub != null && !sub.isEmpty()) {
                TextView st = Ui.text(getContext(), sub, 12.5f, Ui.regular, Ui.text2());
                st.setSingleLine(false);
                col.addView(st);
            }
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Toggle tg = new Toggle(getContext(), initial);
            tg.change = cb;
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(Ui.dp(64), Ui.dp(28));
            tlp.leftMargin = Ui.dp(12);
            row.addView(tg, tlp);
            card.addView(row, MW);
        }

        private void addDivider() {
            View d = new View(getContext());
            d.setBackgroundColor(Ui.separator());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(0.6f)));
            lp.setMargins(Ui.dp(16), 0, Ui.dp(16), 0);
            card.addView(d, lp);
        }
    }

    /** Horizontally scrollable filter chips. */
    static final class Chips extends HorizontalScrollView {
        interface OnPick { void onPick(int index); }

        Chips(Context c, String[] items, int selected, OnPick cb) {
            super(c);
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(OVER_SCROLL_NEVER);
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(6));
            for (int i = 0; i < items.length; i++) {
                final int idx = i;
                boolean act = i == selected;
                TextView chip = Ui.text(c, items[i], 13.5f, act ? Ui.semibold : Ui.medium, act ? 0xFFFFFFFF : Ui.text());
                chip.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(8));
                chip.setBackground(Ui.round(act ? Ui.ACCENT : Ui.chip(), 100));
                chip.setOnClickListener(v -> cb.onPick(idx));
                Ui.press(chip, 0.94f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i < items.length - 1) lp.rightMargin = Ui.dp(8);
                row.addView(chip, lp);
            }
            addView(row);
        }
    }

    /** Kyant0 AndroidLiquidGlass-style LiquidToggle (64x28dp track, 40x24dp refractive capsule thumb, critically damped). */
    static final class Toggle extends View {
        interface OnChange { void onChange(boolean on); }
        boolean on;
        OnChange change;
        private final Ui.Spring p = new Ui.Spring(480f, 1.0f, 0.002f);
        private final Ui.Spring press = new Ui.Spring(640f, 1.0f, 0.002f);
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private float downX;
        private boolean dragging;

        Toggle(Context c, boolean initial) {
            super(c);
            on = initial;
            p.snap(initial ? 1f : 0f);
            p.onUpdate = this::invalidate;
            press.onUpdate = this::invalidate;
            stroke.setStyle(Paint.Style.STROKE);
            setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(64), Ui.dp(28)));
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float w = getWidth(), thumbW = Ui.dpf(40), pad = Ui.dpf(2f);
            float travel = Math.max(1f, w - pad * 2f - thumbW);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    dragging = false;
                    press.to(1f);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getX() - downX) > Ui.dpf(5)) dragging = true;
                    if (dragging) p.snap(Ui.clamp((e.getX() - pad - thumbW / 2f) / travel, 0f, 1f));
                    return true;
                case MotionEvent.ACTION_UP:
                    press.to(0f);
                    boolean next = dragging ? (p.value >= 0.5f) : !on;
                    boolean changed = next != on;
                    on = next;
                    p.to(on ? 1f : 0f);
                    if (changed && change != null) change.onChange(on);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    press.to(0f);
                    p.to(on ? 1f : 0f);
                    return true;
            }
            return super.onTouchEvent(e);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float k = Ui.clamp(p.value, 0f, 1f);
            float pr = Ui.clamp(press.value, 0f, 1f);
            int offCol = Ui.dark ? 0xFF343844 : 0xFFD4D7E2;
            paint.setColor(Ui.mix(offCol, 0xFF34C759, k));
            r.set(0, 0, w, h);
            c.drawRoundRect(r, h / 2f, h / 2f, paint);

            float pad = Ui.dpf(2f);
            float baseW = Math.min(w * 0.64f, Ui.dpf(40));
            float tw = Math.min(w - pad * 2f, baseW + pr * Ui.dpf(3.5f));
            float travel = Math.max(0f, w - pad * 2f - tw);
            float left = pad + travel * k;
            r.set(left, Math.max(0.5f, pad - pr * Ui.dpf(1f)), left + tw, Math.min(h - 0.5f, h - pad + pr * Ui.dpf(1f)));
            float rr = r.height() / 2f;

            paint.setColor(pr > 0.15f ? (Ui.dark ? 0xD8F2F6FF : 0xE6FFFFFF) : 0xFFFFFFFF);
            paint.setShadowLayer(Ui.dpf(3f + pr * 2.5f), 0, Ui.dpf(1.2f), 0x3A000000);
            c.drawRoundRect(r, rr, rr, paint);
            paint.clearShadowLayer();

            stroke.setStrokeWidth(Ui.dpf(1f));
            stroke.setColor(pr > 0.15f ? 0xCCFFFFFF : 0x44FFFFFF);
            c.drawRoundRect(r, rr, rr, stroke);
        }
    }

    /** Smooth slider for glass transparency. */
    static final class GlassSlider extends View {
        interface OnChange { void onChange(float value); }
        private float v;
        private final OnChange cb;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();

        GlassSlider(Context c, float initial, OnChange cb) {
            super(c);
            this.v = Ui.clamp(initial, 0f, 1f);
            this.cb = cb;
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(38)));
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), pad = Ui.dpf(16), th = Ui.dpf(6), cy = h / 2f;
            float usable = Math.max(1f, w - pad * 2);
            p.setColor(Ui.separator());
            rf.set(pad, cy - th / 2f, w - pad, cy + th / 2f);
            c.drawRoundRect(rf, th / 2f, th / 2f, p);
            float x = pad + usable * v;
            p.setColor(Ui.ACCENT);
            rf.set(pad, cy - th / 2f, x, cy + th / 2f);
            c.drawRoundRect(rf, th / 2f, th / 2f, p);
            p.setColor(0xFFFFFFFF);
            p.setShadowLayer(Ui.dpf(4), 0, Ui.dpf(1.5f), 0x38000000);
            c.drawCircle(x, cy, Ui.dpf(11), p);
            p.clearShadowLayer();
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN || e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                getParent().requestDisallowInterceptTouchEvent(true);
                float pad = Ui.dpf(16);
                float nv = Ui.clamp((e.getX() - pad) / Math.max(1f, getWidth() - pad * 2), 0f, 1f);
                if (Math.abs(nv - v) > 0.004f) {
                    v = nv;
                    invalidate();
                    if (cb != null) cb.onChange(v);
                }
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    static void pickDialog(Context c, String title, String[] items, int selected, Chips.OnPick onPick) {
        new AlertDialog.Builder(c, Ui.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(title)
                .setSingleChoiceItems(items, selected, (d, which) -> { d.dismiss(); onPick.onPick(which); })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    static void showNotesDialog(Context c, AppCatalog.ModuleApp app, Runnable onAction) {
        ScrollView sv = new ScrollView(c);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(22), Ui.dp(12), Ui.dp(22), Ui.dp(12));

        String verLine = app.installed
                ? ("설치됨 v" + app.installedVersion + (app.latestVersion.isEmpty() ? "" : "  →  최신 v" + app.latestVersion))
                : (app.latestVersion.isEmpty() ? "미설치" : "최신 릴리스 v" + app.latestVersion);
        TextView meta = Ui.text(c, verLine, 13f, Ui.semibold, Ui.ACCENT);
        box.addView(meta, MW);

        TextView pkgTv = Ui.text(c, app.pkg + " · " + app.repo, 11.5f, Ui.regular, Ui.text2());
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = Ui.dp(3);
        plp.bottomMargin = Ui.dp(12);
        box.addView(pkgTv, plp);

        CharSequence notes = app.releaseNotes != null && !app.releaseNotes.trim().isEmpty()
                ? AppCatalog.formatReleaseNotes(app.releaseNotes)
                : c.getString(R.string.status_no_notes);
        TextView notesTv = new TextView(c);
        notesTv.setText(notes);
        notesTv.setTextSize(14f);
        notesTv.setLineSpacing(0f, 1.32f);
        notesTv.setTextColor(Ui.text());
        box.addView(notesTv, MW);
        sv.addView(box);

        String posLabel = app.hasUpdate() ? c.getString(R.string.btn_update)
                : (app.installed ? c.getString(R.string.btn_open) : c.getString(R.string.btn_install));
        AlertDialog.Builder b = new AlertDialog.Builder(c,
                Ui.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(app.name)
                .setView(sv)
                .setPositiveButton(posLabel, (d, w) -> { if (onAction != null) onAction.run(); })
                .setNegativeButton(R.string.btn_close, null);
        if (app.installed && !app.pkg.equals(c.getPackageName())) {
            b.setNeutralButton(R.string.btn_app_info, (d, w) -> AppCatalog.openAppInfo(c, app));
        }
        b.show();
    }
}
