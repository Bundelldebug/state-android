package com.oneuibook.state;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

/** Fixed top progressive-blur header with collapsing title and Liquid Glass refresh pill. */
final class Header extends View {
    interface Listener { void onRefresh(); }

    private final Glass.Backdrop backdrop;
    private Listener listener;
    private final Glass.Surface pillGlass = new Glass.Surface();
    private final RenderNode blurNode = new RenderNode("state-hdr-blur");
    private final Paint mask = new Paint();
    private final Paint wash = new Paint();
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private RenderEffect blurFx;

    private String title = "";
    float titleAlpha, blurAlpha;
    final RectF pill = new RectF();
    private final Ui.Spring press = new Ui.Spring(640f, 1.0f, 0.002f);
    private boolean down;

    Header(Context c, Glass.Backdrop backdrop) {
        super(c);
        this.backdrop = backdrop;
        titlePaint.setTypeface(Ui.bold);
        titlePaint.setTextSize(Ui.sp(18));
        mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        pillGlass.blurDp = 10;
        pillGlass.refractDp = 14;
        pillGlass.edgeDp = 18;
        pillGlass.outerRefractDp = 10;
        pillGlass.vibrance = 1.3f;
        pillGlass.rim = 0.32f;
        pillGlass.shadowAlpha = 0.08f;
        press.onUpdate = this::invalidate;
    }

    void setListener(Listener l) { listener = l; }
    void setTitle(String t) { title = t == null ? "" : t; invalidate(); }
    void setScroll(float ta, float ba) {
        if (Math.abs(ta - titleAlpha) < 0.003f && Math.abs(ba - blurAlpha) < 0.003f) return;
        titleAlpha = ta;
        blurAlpha = ba;
        invalidate();
    }
    boolean animating() { return press.running; }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        float top = Ui.statusInset + Ui.dpf(12);
        float sz = Ui.dpf(44);
        float rightEdge = w - Ui.cutoutRight - Ui.dpf(16);
        pill.set(rightEdge - sz, top, rightEdge, top + sz);

        if (blurAlpha > 0.01f && c.isHardwareAccelerated() && backdrop.recorded && Ui.glass) {
            int sc = c.saveLayer(0, 0, w, h, null);
            blurNode.setPosition(0, 0, w, h);
            if (blurFx == null) blurFx = RenderEffect.createBlurEffect(Ui.dpf(20), Ui.dpf(20), Shader.TileMode.CLAMP);
            blurNode.setRenderEffect(blurFx);
            RecordingCanvas rc = blurNode.beginRecording(w, h);
            try {
                float[] off = Glass.offsetIn(this, backdrop);
                rc.translate(-off[0], -off[1]);
                rc.drawRenderNode(backdrop.node);
            } finally { blurNode.endRecording(); }
            c.drawRenderNode(blurNode);
            int bg = Ui.bg();
            wash.setShader(new LinearGradient(0, 0, 0, h, new int[]{Ui.alpha(bg, 0.72f), Ui.alpha(bg, 0.35f), 0},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, wash);
            mask.setShader(new LinearGradient(0, 0, 0, h, new int[]{Ui.alpha(0xFF000000, blurAlpha), Ui.alpha(0xFF000000, blurAlpha), 0},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mask);
            c.restoreToCount(sc);
        } else if (blurAlpha > 0.01f) {
            wash.setShader(new LinearGradient(0, 0, 0, h, new int[]{Ui.alpha(Ui.bg(), 0.94f * blurAlpha), 0}, null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, wash);
        }

        if (titleAlpha > 0.01f && !title.isEmpty()) {
            titlePaint.setColor(Ui.alpha(Ui.text(), titleAlpha));
            float leftX = Ui.cutoutLeft + Ui.dpf(20);
            float avail = Math.max(Ui.dpf(80), pill.left - leftX - Ui.dpf(12));
            CharSequence el = TextUtils.ellipsize(title, titlePaint, avail, TextUtils.TruncateAt.END);
            c.drawText(el, 0, el.length(), leftX, pill.centerY() + titlePaint.getTextSize() * 0.36f, titlePaint);
        }

        pillGlass.shapes(pill, sz / 2f, null, 0, null, 0);
        pillGlass.tint = Ui.barTint();
        pillGlass.blurDp = Ui.barBlur();
        pillGlass.pressX = pill.centerX();
        pillGlass.pressY = pill.centerY();
        pillGlass.pressR = Ui.dpf(34);
        pillGlass.pressAmt = press.value;
        pillGlass.draw(c, this, backdrop);
        Icons.draw(c, Icons.REFRESH, pill.centerX(), pill.centerY(), Ui.dpf(21), AppCatalog.checking ? Ui.ACCENT : Ui.text());
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (e.getX() >= pill.left - Ui.dpf(8) && e.getX() <= pill.right + Ui.dpf(8)
                        && e.getY() >= pill.top - Ui.dpf(8) && e.getY() <= pill.bottom + Ui.dpf(8)) {
                    down = true;
                    press.to(1f);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                press.to(0f);
                if (down && listener != null) listener.onRefresh();
                down = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                down = false;
                press.to(0f);
                return true;
        }
        return down;
    }
}
