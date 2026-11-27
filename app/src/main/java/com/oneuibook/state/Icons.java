package com.oneuibook.state;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Crisp custom vector icons for State chrome and all 7 managed suite apps. */
final class Icons {
    private Icons() {}

    static final int APPS = 1, UPDATES = 2, SETTINGS = 3, SEARCH = 4, REFRESH = 5,
            BACK = 6, CHEVRON = 7, CLOSE = 8, CHECK = 9, DOWNLOAD = 10,
            INFO = 11, EXTERNAL = 12, TRASH = 13, PALETTE = 14,
            // Suite app emblems
            APP_PRISM = 101, APP_PLAY = 102, APP_VIEWER = 103, APP_WORKSPACE = 104,
            APP_AURORA = 105, APP_LINUX = 106, APP_CHECKER = 107, APP_STOCK = 108, APP_STATE = 109;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF RF = new RectF();

    static void draw(Canvas c, int id, float cx, float cy, float size, int color) {
        P.reset();
        P.setAntiAlias(true);
        P.setColor(color);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        P.setStrokeWidth(Math.max(1.8f, size * 0.09f));
        float r = size * 0.40f;

        switch (id) {
            case APPS:
            case APP_STATE: {
                float b = r * 0.68f, gap = r * 0.14f;
                P.setStyle(Paint.Style.FILL);
                RF.set(cx - b, cy - b, cx - gap, cy - gap); c.drawRoundRect(RF, r * 0.18f, r * 0.18f, P);
                RF.set(cx - b, cy + gap, cx - gap, cy + b); c.drawRoundRect(RF, r * 0.18f, r * 0.18f, P);
                RF.set(cx + gap, cy + gap, cx + b, cy + b); c.drawRoundRect(RF, r * 0.18f, r * 0.18f, P);
                c.save();
                c.rotate(10f, cx + (gap + b) * 0.5f, cy - (gap + b) * 0.5f);
                RF.set(cx + gap, cy - b, cx + b, cy - gap); c.drawRoundRect(RF, r * 0.18f, r * 0.18f, P);
                c.restore();
                break;
            }
            case UPDATES:
            case DOWNLOAD:
                c.drawLine(cx, cy - r * 0.68f, cx, cy + r * 0.30f, P);
                PATH.reset();
                PATH.moveTo(cx - r * 0.38f, cy - r * 0.04f);
                PATH.lineTo(cx, cy + r * 0.36f);
                PATH.lineTo(cx + r * 0.38f, cy - r * 0.04f);
                c.drawPath(PATH, P);
                c.drawLine(cx - r * 0.64f, cy + r * 0.68f, cx + r * 0.64f, cy + r * 0.68f, P);
                break;
            case SETTINGS:
                c.drawCircle(cx, cy, r * 0.32f, P);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0;
                    c.drawLine(cx + (float) Math.cos(a) * r * 0.55f, cy + (float) Math.sin(a) * r * 0.55f,
                            cx + (float) Math.cos(a) * r * 0.78f, cy + (float) Math.sin(a) * r * 0.78f, P);
                }
                break;
            case SEARCH:
                c.drawCircle(cx - r * 0.14f, cy - r * 0.14f, r * 0.52f, P);
                c.drawLine(cx + r * 0.24f, cy + r * 0.24f, cx + r * 0.72f, cy + r * 0.72f, P);
                break;
            case REFRESH:
                RF.set(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f);
                c.drawArc(RF, -45, 290, false, P);
                PATH.reset();
                PATH.moveTo(cx + r * 0.22f, cy - r * 0.62f);
                PATH.lineTo(cx + r * 0.62f, cy - r * 0.54f);
                PATH.lineTo(cx + r * 0.54f, cy - r * 0.94f);
                c.drawPath(PATH, P);
                break;
            case BACK:
                PATH.reset();
                PATH.moveTo(cx + r * 0.26f, cy - r * 0.68f);
                PATH.lineTo(cx - r * 0.40f, cy);
                PATH.lineTo(cx + r * 0.26f, cy + r * 0.68f);
                c.drawPath(PATH, P);
                break;
            case CHEVRON:
                PATH.reset();
                PATH.moveTo(cx - r * 0.24f, cy - r * 0.56f);
                PATH.lineTo(cx + r * 0.34f, cy);
                PATH.lineTo(cx - r * 0.24f, cy + r * 0.56f);
                c.drawPath(PATH, P);
                break;
            case CLOSE:
                c.drawLine(cx - r * 0.54f, cy - r * 0.54f, cx + r * 0.54f, cy + r * 0.54f, P);
                c.drawLine(cx + r * 0.54f, cy - r * 0.54f, cx - r * 0.54f, cy + r * 0.54f, P);
                break;
            case CHECK:
                PATH.reset();
                PATH.moveTo(cx - r * 0.56f, cy + r * 0.04f);
                PATH.lineTo(cx - r * 0.14f, cy + r * 0.48f);
                PATH.lineTo(cx + r * 0.62f, cy - r * 0.44f);
                c.drawPath(PATH, P);
                break;
            case INFO:
                c.drawCircle(cx, cy, r * 0.76f, P);
                c.drawLine(cx, cy - r * 0.08f, cx, cy + r * 0.38f, P);
                P.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy - r * 0.34f, r * 0.11f, P);
                break;
            case EXTERNAL:
                RF.set(cx - r * 0.68f, cy - r * 0.42f, cx + r * 0.42f, cy + r * 0.68f);
                c.drawRoundRect(RF, r * 0.18f, r * 0.18f, P);
                c.drawLine(cx - r * 0.08f, cy + r * 0.08f, cx + r * 0.68f, cy - r * 0.68f, P);
                PATH.reset();
                PATH.moveTo(cx + r * 0.20f, cy - r * 0.68f);
                PATH.lineTo(cx + r * 0.68f, cy - r * 0.68f);
                PATH.lineTo(cx + r * 0.68f, cy - r * 0.20f);
                c.drawPath(PATH, P);
                break;
            case TRASH:
                c.drawLine(cx - r * 0.68f, cy - r * 0.46f, cx + r * 0.68f, cy - r * 0.46f, P);
                RF.set(cx - r * 0.50f, cy - r * 0.46f, cx + r * 0.50f, cy + r * 0.72f);
                c.drawRoundRect(RF, r * 0.16f, r * 0.16f, P);
                c.drawLine(cx - r * 0.24f, cy - r * 0.72f, cx + r * 0.24f, cy - r * 0.72f, P);
                break;
            case PALETTE:
                c.drawCircle(cx, cy, r * 0.74f, P);
                P.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - r * 0.26f, cy - r * 0.18f, r * 0.13f, P);
                c.drawCircle(cx + r * 0.22f, cy - r * 0.24f, r * 0.13f, P);
                c.drawCircle(cx + r * 0.26f, cy + r * 0.18f, r * 0.13f, P);
                break;

            // ---- Suite App Emblems ----
            case APP_PRISM:
                P.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - r * 0.34f, cy + r * 0.46f, r * 0.26f, P);
                c.drawCircle(cx + r * 0.38f, cy + r * 0.30f, r * 0.26f, P);
                P.setStyle(Paint.Style.STROKE);
                P.setStrokeWidth(Math.max(2f, size * 0.10f));
                c.drawLine(cx - r * 0.10f, cy + r * 0.42f, cx - r * 0.10f, cy - r * 0.52f, P);
                c.drawLine(cx + r * 0.62f, cy + r * 0.26f, cx + r * 0.62f, cy - r * 0.66f, P);
                c.drawLine(cx - r * 0.10f, cy - r * 0.52f, cx + r * 0.62f, cy - r * 0.66f, P);
                break;
            case APP_PLAY:
                P.setStyle(Paint.Style.FILL);
                PATH.reset();
                PATH.moveTo(cx - r * 0.42f, cy - r * 0.60f);
                PATH.lineTo(cx + r * 0.64f, cy);
                PATH.lineTo(cx - r * 0.42f, cy + r * 0.60f);
                PATH.close();
                c.drawPath(PATH, P);
                break;
            case APP_VIEWER:
                RF.set(cx - r * 0.58f, cy - r * 0.74f, cx + r * 0.58f, cy + r * 0.74f);
                c.drawRoundRect(RF, r * 0.20f, r * 0.20f, P);
                c.drawLine(cx - r * 0.28f, cy - r * 0.26f, cx + r * 0.28f, cy - r * 0.26f, P);
                c.drawLine(cx - r * 0.28f, cy + r * 0.06f, cx + r * 0.28f, cy + r * 0.06f, P);
                c.drawLine(cx - r * 0.28f, cy + r * 0.38f, cx + r * 0.10f, cy + r * 0.38f, P);
                break;
            case APP_WORKSPACE:
                RF.set(cx - r * 0.70f, cy - r * 0.62f, cx + r * 0.70f, cy + r * 0.62f);
                c.drawRoundRect(RF, r * 0.20f, r * 0.20f, P);
                c.drawLine(cx - r * 0.70f, cy - r * 0.16f, cx + r * 0.70f, cy - r * 0.16f, P);
                c.drawLine(cx - r * 0.12f, cy - r * 0.16f, cx - r * 0.12f, cy + r * 0.62f, P);
                break;
            case APP_AURORA:
                P.setStrokeWidth(Math.max(2.2f, size * 0.11f));
                PATH.reset();
                PATH.moveTo(cx - r * 0.74f, cy + r * 0.30f);
                PATH.cubicTo(cx - r * 0.36f, cy - r * 0.62f, cx + r * 0.26f, cy + r * 0.18f, cx + r * 0.74f, cy - r * 0.46f);
                c.drawPath(PATH, P);
                PATH.reset();
                PATH.moveTo(cx - r * 0.52f, cy + r * 0.62f);
                PATH.cubicTo(cx - r * 0.12f, cy + r * 0.04f, cx + r * 0.36f, cy + r * 0.46f, cx + r * 0.76f, cy + r * 0.02f);
                c.drawPath(PATH, P);
                break;
            case APP_LINUX:
                RF.set(cx - r * 0.76f, cy - r * 0.60f, cx + r * 0.76f, cy + r * 0.60f);
                c.drawRoundRect(RF, r * 0.22f, r * 0.22f, P);
                PATH.reset();
                PATH.moveTo(cx - r * 0.42f, cy - r * 0.22f);
                PATH.lineTo(cx - r * 0.14f, cy + r * 0.04f);
                PATH.lineTo(cx - r * 0.42f, cy + r * 0.30f);
                c.drawPath(PATH, P);
                c.drawLine(cx + r * 0.04f, cy + r * 0.28f, cx + r * 0.44f, cy + r * 0.28f, P);
                break;
            case APP_CHECKER:
                c.drawCircle(cx, cy, r * 0.74f, P);
                PATH.reset();
                PATH.moveTo(cx - r * 0.36f, cy + r * 0.02f);
                PATH.lineTo(cx - r * 0.08f, cy + r * 0.32f);
                PATH.lineTo(cx + r * 0.42f, cy - r * 0.28f);
                c.drawPath(PATH, P);
                break;
            case APP_STOCK:
                PATH.reset();
                PATH.moveTo(cx - r * 0.72f, cy + r * 0.44f);
                PATH.lineTo(cx - r * 0.24f, cy - r * 0.04f);
                PATH.lineTo(cx + r * 0.14f, cy + r * 0.22f);
                PATH.lineTo(cx + r * 0.72f, cy - r * 0.46f);
                c.drawPath(PATH, P);
                PATH.reset();
                PATH.moveTo(cx + r * 0.34f, cy - r * 0.46f);
                PATH.lineTo(cx + r * 0.72f, cy - r * 0.46f);
                PATH.lineTo(cx + r * 0.72f, cy - r * 0.08f);
                c.drawPath(PATH, P);
                break;
        }
    }

    static final class IconView extends View {
        int icon;
        int color;
        float sizeDp;

        IconView(Context c, int icon, float sizeDp, int color) {
            super(c);
            this.icon = icon;
            this.sizeDp = sizeDp;
            this.color = color;
        }

        void set(int icon, int color) {
            this.icon = icon;
            this.color = color;
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            Icons.draw(c, icon, getWidth() / 2f, getHeight() / 2f, Ui.dpf(sizeDp), color);
        }
    }

    /** Renders each suite app's REAL launcher icon (installed PackageManager icon or exact bundled adaptive icon) clipped to a One UI squircle. */
    static final class AppBadgeView extends View {
        private final android.graphics.drawable.Drawable iconDrawable;
        private final int fallbackIcon;
        private final int accent;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path squircle = new Path();
        private final RectF rf = new RectF();
        private float lastW = -1f, lastH = -1f;

        AppBadgeView(Context c, AppCatalog.ModuleApp app) {
            super(c);
            this.fallbackIcon = app.icon;
            this.accent = app.accent;
            this.iconDrawable = resolveAppIcon(c, app);
        }

        private static android.graphics.drawable.Drawable resolveAppIcon(Context c, AppCatalog.ModuleApp app) {
            // 1) If installed on device (not Robolectric mock), use the live launcher icon directly from PackageManager
            if (app.installed && !"robolectric".equals(android.os.Build.FINGERPRINT)) {
                try {
                    android.graphics.drawable.Drawable pmIcon = c.getPackageManager().getApplicationIcon(app.pkg);
                    if (pmIcon != null) return pmIcon;
                } catch (Throwable ignored) {}
            }
            // 2) Authentic bundled AdaptiveIconDrawable / Drawable matching every published app's exact launcher icon!
            try {
                switch (app.id) {
                    case "prism": return adaptive(c, R.drawable.icon_prism_bg, R.drawable.icon_prism_fg);
                    case "play": return adaptive(c, R.drawable.icon_play_bg, R.drawable.icon_play_fg);
                    case "viewer": return c.getDrawable(R.drawable.icon_viewer);
                    case "workspace": return adaptive(c, R.drawable.icon_workspace_bg, R.drawable.icon_workspace_fg);
                    case "aurora": return adaptive(c, R.drawable.icon_aurora_bg, R.drawable.icon_aurora_fg);
                    case "checker": return adaptive(c, R.drawable.icon_checker_bg, R.drawable.icon_checker_fg);
                    case "state": return adaptive(c, R.drawable.ic_launcher_bg, R.drawable.ic_launcher_fg);
                }
            } catch (Throwable ignored) {}
            return null;
        }

        private static android.graphics.drawable.Drawable adaptive(Context c, int bgRes, int fgRes) {
            android.graphics.drawable.Drawable bg = c.getDrawable(bgRes);
            android.graphics.drawable.Drawable fg = c.getDrawable(fgRes);
            if (bg != null && fg != null) {
                return new android.graphics.drawable.AdaptiveIconDrawable(bg, fg);
            }
            return fg != null ? fg : bg;
        }

        private void buildSquircle(float w, float h) {
            squircle.reset();
            float cx = w * 0.5f, cy = h * 0.5f;
            float rx = w * 0.495f, ry = h * 0.495f;
            float n = 2.76f;
            for (int i = 0; i <= 72; i++) {
                double a = i * Math.PI * 2.0 / 72.0;
                double ca = Math.cos(a), sa = Math.sin(a);
                float x = cx + (float) (Math.signum(ca) * Math.pow(Math.abs(ca), 2.0 / n) * rx);
                float y = cy + (float) (Math.signum(sa) * Math.pow(Math.abs(sa), 2.0 / n) * ry);
                if (i == 0) squircle.moveTo(x, y); else squircle.lineTo(x, y);
            }
            squircle.close();
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            if (w != lastW || h != lastH) {
                lastW = w; lastH = h;
                buildSquircle(w, h);
            }
            c.save();
            c.clipPath(squircle);
            if (iconDrawable != null) {
                iconDrawable.setBounds(0, 0, Math.round(w), Math.round(h));
                iconDrawable.draw(c);
            } else {
                rf.set(0, 0, w, h);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(accent);
                c.drawRect(rf, paint);
                Icons.draw(c, fallbackIcon, w / 2f, h / 2f, w * 0.56f, 0xFFFFFFFF);
            }
            c.restore();

            // Subtle One UI squircle edge hairline for crisp contrast on both light & dark cards
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, Ui.dpf(0.65f)));
            paint.setColor(Ui.dark ? 0x26FFFFFF : 0x16000000);
            c.drawPath(squircle, paint);
        }
    }
}
