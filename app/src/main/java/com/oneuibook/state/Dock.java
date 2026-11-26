package com.oneuibook.state;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Editable;
import android.text.InputType;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * Prism-unified Liquid Glass floating bottom dock for State:
 * 3-tab glass pill (앱 / 업데이트 / 설정) with draggable refracting lens (critically damped, zero bounce)
 * + detached glass search bubble that morphs smoothly into a search bar.
 */
final class Dock extends FrameLayout {
    interface Listener {
        void onTab(int index);
        void onSearch();
        void onExitSearch();
        void onQuery(String q);
    }

    static final int[] TAB_ICONS = {Icons.APPS, Icons.UPDATES, Icons.SETTINGS};

    private final Glass.Backdrop backdrop;
    private final String[] labels;
    private Listener listener;
    int selected = 0;
    int updateBadgeCount = 0;
    float search = 0f;
    private int imeOffset = 0;

    final RectF pill = new RectF(), circle = new RectF(), lens = new RectF();
    private final Glass.Surface barGlass = new Glass.Surface();
    private final Glass.Surface lensGlass = new Glass.Surface();

    // Critically damped springs matching Prism 9.5.17 (smooth fluid glide, zero bounce)
    private final Ui.Spring lensX = new Ui.Spring(440f, 1.0f, 0.4f);
    private final Ui.Spring lift = new Ui.Spring(560f, 1.0f, 0.002f);
    private final Ui.Spring searchPress = new Ui.Spring(640f, 1.0f, 0.002f);

    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint badgePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator searchAnim;

    final EditText field;
    private int touch = 0; // 0=none, 1=tab, 2=search
    private float downX;
    private boolean dragging;

    Dock(Context c, Glass.Backdrop backdrop, String[] labels) {
        super(c);
        this.backdrop = backdrop;
        this.labels = labels;
        setWillNotDraw(false);
        setClipChildren(false);

        labelPaint.setTypeface(Ui.semibold);
        labelPaint.setTextSize(Ui.sp(10.5f));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        badgePaint.setTypeface(Ui.bold);
        badgePaint.setTextSize(Ui.sp(9.5f));
        badgePaint.setTextAlign(Paint.Align.CENTER);

        barGlass.blurDp = 3f;
        barGlass.refractDp = 17f;
        barGlass.edgeDp = 24f;
        barGlass.outerDp = 7f;
        barGlass.outerRefractDp = 11f;
        barGlass.vibrance = 1.35f;
        barGlass.rim = 0.34f;

        lensGlass.blur = false;
        lensGlass.refractDp = 15f;
        lensGlass.edgeDp = 22f;
        lensGlass.outerDp = 7f;
        lensGlass.outerRefractDp = 12f;
        lensGlass.depthEffect = 0.6f;
        lensGlass.chroma = 0.9f;
        lensGlass.rim = 0.54f;
        lensGlass.shadowAlpha = 0.16f;

        Runnable inv = this::invalidate;
        lensX.onUpdate = inv;
        lift.onUpdate = inv;
        searchPress.onUpdate = inv;

        field = new EditText(c);
        field.setBackground(null);
        field.setSingleLine(true);
        field.setHint(c.getString(R.string.search_hint));
        field.setHintTextColor(Ui.text3());
        field.setTextColor(Ui.text());
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        field.setTypeface(Ui.medium);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
                field.clearFocus();
                return true;
            }
            return false;
        });
        field.setVisibility(GONE);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (listener != null) listener.onQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        addView(field);
    }

    void setListener(Listener l) { listener = l; }
    void setImeOffset(int ime) { imeOffset = ime; requestLayout(); invalidate(); }
    void setUpdateBadge(int count) { updateBadgeCount = Math.max(0, count); invalidate(); }
    boolean animating() { return lensX.running || lift.running || searchPress.running || (searchAnim != null && searchAnim.isRunning()); }

    void select(int index, boolean animate) {
        selected = Math.max(0, Math.min(labels.length - 1, index));
        layoutRects();
        float tx = tabCenterX(selected);
        if (animate && pill.width() > 1) lensX.to(tx);
        else lensX.snap(tx);
        invalidate();
    }

    void setSearchMode(boolean open) {
        if (searchAnim != null) searchAnim.cancel();
        searchAnim = ValueAnimator.ofFloat(search, open ? 1f : 0f);
        searchAnim.setDuration(260);
        searchAnim.setInterpolator(Ui.EMPHASIZED);
        searchAnim.addUpdateListener(a -> {
            search = (float) a.getAnimatedValue();
            field.setVisibility(search > 0.35f ? VISIBLE : GONE);
            field.setAlpha(Ui.clamp((search - 0.35f) / 0.65f, 0f, 1f));
            requestLayout();
            invalidate();
        });
        searchAnim.start();
        if (open) {
            field.setVisibility(VISIBLE);
            field.requestFocus();
            InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
            if (imm != null) imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
        } else {
            field.setText("");
            field.clearFocus();
            InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
            if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
    }

    private float tabCenterX(int idx) {
        if (pill.width() <= 0) return 0f;
        float pad = Ui.dpf(5);
        float seg = (pill.width() - pad * 2f) / labels.length;
        return pill.left + pad + seg * (idx + 0.5f);
    }

    private void layoutRects() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float barH = Ui.dpf(60);
        float margin = Ui.dpf(18);
        float gap = Ui.dpf(12);
        float bottom = h - Math.max(Ui.navInset, Ui.dp(12)) - Ui.dpf(10) - imeOffset;
        float top = bottom - barH;
        float maxW = Math.min(w - margin * 2f, Ui.dpf(440));
        float left = (w - maxW) / 2f;
        float right = left + maxW;

        if (search > 0.01f) {
            float pillRight = Ui.lerp(right - barH - gap, right, search);
            pill.set(left, top, pillRight, bottom);
            circle.set(Ui.lerp(right - barH, right - barH, search), top, right, bottom);
        } else {
            pill.set(left, top, right - barH - gap, bottom);
            circle.set(right - barH, top, right, bottom);
        }
        if (lensX.value < pill.left || lensX.value > pill.right) {
            lensX.snap(tabCenterX(selected));
        }
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutRects();
        if (pill.width() > Ui.dp(60)) {
            int padL = (int) (pill.left + Ui.dpf(44));
            int padR = (int) (pill.right - Ui.dpf(44));
            field.measure(MeasureSpec.makeMeasureSpec(Math.max(0, padR - padL), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((int) pill.height(), MeasureSpec.EXACTLY));
            field.layout(padL, (int) pill.top, padR, (int) pill.bottom);
        }
    }

    @Override protected void onDraw(Canvas c) {
        layoutRects();
        if (pill.width() <= 1) return;

        barGlass.tint = Ui.barTint();
        barGlass.blurDp = Ui.barBlur();

        if (search < 0.99f) {
            barGlass.shapes(pill, pill.height() / 2f, circle, circle.height() / 2f, null, 0);
        } else {
            barGlass.shapes(pill, pill.height() / 2f, null, 0, null, 0);
        }
        barGlass.draw(c, this, backdrop);

        if (search > 0.45f) {
            float a = Ui.clamp((search - 0.45f) / 0.55f, 0f, 1f);
            Icons.draw(c, Icons.SEARCH, pill.left + Ui.dpf(24), pill.centerY(), Ui.dpf(20), Ui.alpha(Ui.text2(), a));
            Icons.draw(c, Icons.CLOSE, pill.right - Ui.dpf(24), pill.centerY(), Ui.dpf(18), Ui.alpha(Ui.text2(), a));
            return;
        }

        float tabAlpha = 1f - Ui.clamp(search * 2.2f, 0f, 1f);
        float pad = Ui.dpf(5);
        float seg = (pill.width() - pad * 2f) / labels.length;
        float lf = Ui.clamp(lift.value, 0f, 1f);
        float lw = seg + lf * Ui.dpf(8);
        float lh = pill.height() - pad * 2f + lf * Ui.dpf(4);
        float lx = Ui.clamp(lensX.value, pill.left + pad + lw / 2f, pill.right - pad - lw / 2f);
        lens.set(lx - lw / 2f, pill.centerY() - lh / 2f, lx + lw / 2f, pill.centerY() + lh / 2f);

        // Active indicator capsule (subtle tinted fill at rest, full refractive lens on drag/lift)
        if (Ui.gStyle || lf < 0.08f) {
            fill.setColor(Ui.alpha(Ui.dark ? 0x26FFFFFF : 0x16000000, tabAlpha));
            c.drawRoundRect(lens, lens.height() / 2f, lens.height() / 2f, fill);
        }
        if (!Ui.gStyle && lf > 0.04f) {
            lensGlass.tint = Ui.dark ? 0x18FFFFFF : 0x12FFFFFF;
            lensGlass.strokeColor = Ui.dark ? 0x42FFFFFF : 0x1E000000;
            lensGlass.alpha = lf * tabAlpha;
            lensGlass.shapes(lens, lens.height() / 2f, null, 0, null, 0);
            lensGlass.draw(c, this, backdrop);
        }

        // Draw tabs (dual-pass: inactive outside lens, accent color inside lens)
        drawTabs(c, Ui.alpha(Ui.text2(), tabAlpha));
        c.save();
        android.graphics.Path lp = new android.graphics.Path();
        lp.addRoundRect(lens, lens.height() / 2f, lens.height() / 2f, android.graphics.Path.Direction.CW);
        c.clipPath(lp);
        drawTabs(c, Ui.alpha(Ui.ACCENT, tabAlpha));
        c.restore();

        // Detached search icon
        Icons.draw(c, Icons.SEARCH, circle.centerX(), circle.centerY(), Ui.dpf(22), Ui.alpha(Ui.text(), tabAlpha));
    }

    private void drawTabs(Canvas c, int color) {
        float pad = Ui.dpf(5);
        float seg = (pill.width() - pad * 2f) / labels.length;
        labelPaint.setColor(color);
        for (int i = 0; i < labels.length; i++) {
            float cx = pill.left + pad + seg * (i + 0.5f);
            float iy = pill.centerY() - Ui.dpf(7.5f);
            Icons.draw(c, TAB_ICONS[i], cx, iy, Ui.dpf(21), color);
            c.drawText(labels[i], cx, pill.centerY() + Ui.dpf(17.5f), labelPaint);

            // Red update badge dot on Updates tab (index 1)
            if (i == 1 && updateBadgeCount > 0) {
                fill.setColor(0xFFFF3B30);
                float bx = cx + Ui.dpf(12), by = iy - Ui.dpf(7);
                c.drawCircle(bx, by, Ui.dpf(7.5f), fill);
                badgePaint.setColor(0xFFFFFFFF);
                c.drawText(String.valueOf(Math.min(9, updateBadgeCount)), bx, by + Ui.dpf(3.3f), badgePaint);
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        if (search > 0.45f) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP && x >= pill.right - Ui.dpf(48) && y >= pill.top && y <= pill.bottom) {
                setSearchMode(false);
                if (listener != null) listener.onExitSearch();
                return true;
            }
            return y >= pill.top - Ui.dpf(6) && y <= pill.bottom + Ui.dpf(6);
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (pill.contains(x, y)) {
                    touch = 1;
                    downX = x;
                    dragging = false;
                    lift.to(1f);
                    return true;
                }
                if (circle.contains(x, y)) {
                    touch = 2;
                    searchPress.to(1f);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (touch == 1) {
                    if (Math.abs(x - downX) > Ui.dpf(6)) dragging = true;
                    if (dragging) lensX.snap(Ui.clamp(x, pill.left + Ui.dpf(28), pill.right - Ui.dpf(28)));
                    return true;
                }
                return touch != 0;
            case MotionEvent.ACTION_UP:
                if (touch == 1) {
                    lift.to(0f);
                    float pad = Ui.dpf(5);
                    float seg = Math.max(1f, (pill.width() - pad * 2f) / labels.length);
                    float targetX = dragging ? lensX.value : x;
                    int idx = Math.max(0, Math.min(labels.length - 1, (int) ((targetX - pill.left - pad) / seg)));
                    select(idx, true);
                    if (listener != null) listener.onTab(idx);
                    touch = 0;
                    return true;
                }
                if (touch == 2) {
                    searchPress.to(0f);
                    touch = 0;
                    if (circle.contains(x, y)) {
                        setSearchMode(true);
                        if (listener != null) listener.onSearch();
                    }
                    return true;
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                lift.to(0f);
                searchPress.to(0f);
                if (touch == 1) select(selected, true);
                touch = 0;
                return true;
        }
        return false;
    }
}
