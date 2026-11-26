package com.oneuibook.state;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class StateUiTest {
    private File outDir;

    @Before public void setup() {
        outDir = new File(System.getProperty("state.output", "."));
        outDir.mkdirs();
        AppCatalog.APPS.clear();
    }

    private void shot(View v, int w, int h, String name) throws Exception {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        v.draw(c);
        try (FileOutputStream fos = new FileOutputStream(new File(outDir, name))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bmp.recycle();
    }

    @Test public void catalogVersionAndMarkdownFormatting() {
        Context ctx = RuntimeEnvironment.getApplication();
        AppCatalog.init(ctx);
        assertEquals(9, AppCatalog.APPS.size());
        assertTrue(AppCatalog.newer("9.5.17", "9.5.16"));
        assertFalse(AppCatalog.newer("9.5.16", "9.5.16"));
        assertFalse(AppCatalog.newer("9.5.15", "9.5.16"));
        CharSequence notes = AppCatalog.formatReleaseNotes("## 프리즘 9.5.17\n- **YouTube Music** 통합\n- `Kyant0` 토글 적용\n- [릴리스](https://github.com)");
        String s = notes.toString();
        assertFalse(s.contains("##"));
        assertFalse(s.contains("**"));
        assertFalse(s.contains("]("));
        assertTrue(s.contains("YouTube Music"));
    }

    @Test public void launcherIconRenders() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        Drawable icon = ctx.getDrawable(R.mipmap.ic_launcher);
        assertNotNull(icon);
        Bitmap bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        android.graphics.Path sq = new android.graphics.Path();
        float r = 122f, cx = 128f, cy = 128f, n = 2.76f;
        for (int i = 0; i <= 120; i++) {
            double a = i * Math.PI * 2.0 / 120.0;
            double ca = Math.cos(a), sa = Math.sin(a);
            float x = cx + (float) (Math.signum(ca) * Math.pow(Math.abs(ca), 2.0 / n) * r);
            float y = cy + (float) (Math.signum(sa) * Math.pow(Math.abs(sa), 2.0 / n) * r);
            if (i == 0) sq.moveTo(x, y); else sq.lineTo(x, y);
        }
        sq.close();
        c.clipPath(sq);
        icon.setBounds(0, 0, 256, 256);
        icon.draw(c);
        try (FileOutputStream fos = new FileOutputStream(new File(outDir, "icon.png"))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bmp.recycle();
    }

    @Test @Config(qualifiers = "ko-rKR-w412dp-h915dp-xxhdpi")
    public void phoneScreensLightAndDark() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        AppCatalog.init(ctx);
        // Seed realistic suite state for visual verification (installed + pending updates + available)
        seedSampleSuiteState();

        ActivityController<MainActivity> ctrl = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity act = ctrl.get();
        seedSampleSuiteState();
        act.refreshAllPages();
        assertEquals("스테이트", act.getString(R.string.app_name));
        assertEquals("9.5.2", MainActivity.VERSION);
        assertEquals(2, AppCatalog.pendingUpdates().size());

        shot(act.root, 1236, 2745, "phone-apps-light.png");

        act.switchTab(1);
        org.robolectric.shadows.ShadowLooper.idleMainLooper(350, java.util.concurrent.TimeUnit.MILLISECONDS);
        shot(act.root, 1236, 2745, "phone-updates-light.png");

        // Back button from Updates tab returns cleanly to Apps tab (0) without exiting
        act.onBackPressed();
        assertEquals(0, act.tab);
        assertFalse(act.isFinishing());

        act.switchTab(2);
        org.robolectric.shadows.ShadowLooper.idleMainLooper(350, java.util.concurrent.TimeUnit.MILLISECONDS);
        shot(act.root, 1236, 2745, "phone-settings-light.png");

        // Dark mode Apps Hub
        AppCatalog.prefs(ctx).edit().putString("theme", "dark").apply();
        ActivityController<MainActivity> darkCtrl = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity darkAct = darkCtrl.get();
        seedSampleSuiteState();
        darkAct.refreshAllPages();
        shot(darkAct.root, 1236, 2745, "phone-apps-dark.png");
    }

    private void seedSampleSuiteState() {
        for (AppCatalog.ModuleApp a : AppCatalog.APPS) {
            if ("prism".equals(a.id)) {
                a.installed = true; a.installedVersion = "9.5.16"; a.latestVersion = "9.5.17";
                a.releaseNotes = "## 프리즘 9.5.17\n- **YouTube Music** 탭 및 뮤직비디오 전환 추가\n- Kyant0 LiquidToggle 및 비트퍼펙트 어시스트 고도화";
            } else if ("play".equals(a.id)) {
                a.installed = true; a.installedVersion = "9.5.6"; a.latestVersion = "9.5.7";
                a.releaseNotes = "## 플레이 비디오 9.5.7\n- YouTube 네이티브 InnerTube 플레이어 및 Google · microG 로그인 추가";
            } else if ("aurora".equals(a.id)) {
                a.installed = true; a.installedVersion = "9.5.1"; a.latestVersion = "9.5.1";
            } else if ("viewer".equals(a.id)) {
                a.installed = true; a.installedVersion = "9.5.6"; a.latestVersion = "9.5.6";
            } else if ("state".equals(a.id)) {
                a.installed = true; a.installedVersion = "9.5.0"; a.latestVersion = "9.5.0";
            } else {
                a.installed = false; a.latestVersion = "9.5.2";
            }
        }
    }
}
