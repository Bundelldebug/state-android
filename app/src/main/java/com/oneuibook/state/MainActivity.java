package com.oneuibook.state;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

/**
 * State ("스테이트", v9.5.4) — Good Lock-inspired unified Android suite hub & update manager.
 * Manages all 7 published apps (Prism, Play, Viewer, Workspace, Aurora, Checker, State).
 */
public final class MainActivity extends Activity implements Dock.Listener, Header.Listener, AppCatalog.Listener {
    static final String VERSION = "9.5.4";

    FrameLayout root;
    Glass.Backdrop backdrop;
    private FrameLayout pageHost;
    Header header;
    Dock dock;

    final Pages.BasePage[] tabs = new Pages.BasePage[3];
    private Pages.SearchPage searchPage;
    int tab = 0;
    private boolean searching = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this, AppCatalog.prefs(this));
        AppCatalog.init(this);

        Window w = getWindow();
        w.setDecorFitsSystemWindows(false);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        w.setNavigationBarContrastEnforced(false);
        w.getDecorView().setBackgroundColor(Ui.bg());

        root = new FrameLayout(this);
        backdrop = new Glass.Backdrop(this);
        backdrop.setBackgroundColor(Ui.bg());
        root.addView(backdrop, match());

        pageHost = new FrameLayout(this);
        backdrop.addView(pageHost, match());

        header = new Header(this, backdrop);
        header.setListener(this);
        root.addView(header, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.statusInset + Ui.dp(74)));

        dock = new Dock(this, backdrop, new String[]{
                getString(R.string.tab_apps),
                getString(R.string.tab_updates),
                getString(R.string.tab_settings)
        });
        dock.setListener(this);
        root.addView(dock, match());
        setContentView(root);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sys = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
            Ui.statusInset = sys.top;
            Ui.navInset = sys.bottom;
            Ui.cutoutLeft = sys.left;
            Ui.cutoutRight = sys.right;
            Ui.imeInset = ime;
            dock.setImeOffset(ime);
            header.getLayoutParams().height = Ui.statusInset + Ui.dp(74);
            header.requestLayout();
            for (Pages.BasePage p : tabs) if (p != null) p.applyInsets();
            if (searchPage != null) searchPage.applyInsets();
            return WindowInsets.CONSUMED;
        });

        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                boolean dirty = backdrop.isDirty();
                if (dirty || header.animating()) header.invalidate();
                if (dirty || dock.animating()) dock.invalidate();
                return true;
            }
        });

        tabs[0] = new Pages.AppsPage(this);
        tabs[1] = new Pages.UpdatesPage(this);
        tabs[2] = new Pages.SettingsPage(this);
        for (Pages.BasePage p : tabs) {
            p.setVisibility(View.GONE);
            pageHost.addView(p, match());
            p.rebuild();
        }

        tab = savedInstanceState == null ? 0 : savedInstanceState.getInt("tab", 0);
        switchTab(tab);
        applySystemBars(!Ui.dark);

        AppCatalog.listeners.add(this);
        if (AppCatalog.prefs(this).getBoolean("auto_check", true)) {
            AppCatalog.checkAllUpdates(this);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        AppCatalog.refreshInstalled(this);
        AppCatalog.continuePendingQueue(this);
        refreshAllPages();
    }

    @Override public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Ui.init(this, AppCatalog.prefs(this));
        getWindow().getDecorView().setBackgroundColor(Ui.bg());
        backdrop.setBackgroundColor(Ui.bg());
        applySystemBars(!Ui.dark);
        root.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            header.getLayoutParams().height = Ui.statusInset + Ui.dp(74);
            header.requestLayout();
            for (Pages.BasePage p : tabs) if (p != null) p.applyInsets();
            if (searchPage != null) searchPage.applyInsets();
            dock.select(tab, false);
            dock.requestLayout();
            refreshAllPages();
        });
    }

    @Override protected void onDestroy() {
        AppCatalog.listeners.remove(this);
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", tab);
    }

    void switchTab(int idx) {
        int nextIdx = Math.max(0, Math.min(tabs.length - 1, idx));
        boolean changed = nextIdx != tab;
        tab = nextIdx;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].animate().cancel();
            tabs[i].setVisibility(i == tab && !searching ? View.VISIBLE : View.GONE);
        }
        if (changed && !searching) {
            Pages.BasePage cur = tabs[tab];
            cur.setAlpha(0.45f);
            cur.setScaleX(0.985f);
            cur.setScaleY(0.985f);
            cur.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(Ui.EMPHASIZED).start();
        }
        dock.select(tab, changed);
        header.setTitle(tabs[tab].title);
        onPageScroll(tabs[tab], tabs[tab].scroll.getScrollY());
    }

    @Override public void onBackPressed() {
        if (searching) {
            dock.setSearchMode(false);
            onExitSearch();
            return;
        }
        if (tab != 0) {
            switchTab(0);
            return;
        }
        super.onBackPressed();
    }

    void refreshAllPages() {
        dock.setUpdateBadge(AppCatalog.pendingUpdates().size());
        for (Pages.BasePage p : tabs) if (p != null) p.rebuild();
        if (searchPage != null) searchPage.rebuild();
        header.invalidate();
    }

    void onPageScroll(Pages.BasePage p, int scrollY) {
        if (searching && p != searchPage) return;
        if (!searching && p != tabs[tab]) return;
        header.setScroll(p.titleProgress(), p.blurProgress());
    }

    @Override public void onTab(int index) {
        searching = false;
        if (searchPage != null) searchPage.setVisibility(View.GONE);
        switchTab(index);
    }

    @Override public void onSearch() {
        searching = true;
        for (Pages.BasePage p : tabs) p.setVisibility(View.GONE);
        if (searchPage == null) {
            searchPage = new Pages.SearchPage(this);
            pageHost.addView(searchPage, match());
        }
        searchPage.setQuery("");
        searchPage.setVisibility(View.VISIBLE);
        header.setTitle(getString(R.string.app_name));
    }

    @Override public void onExitSearch() {
        searching = false;
        if (searchPage != null) searchPage.setVisibility(View.GONE);
        switchTab(tab);
    }

    @Override public void onQuery(String q) {
        if (searchPage != null) searchPage.setQuery(q);
    }

    @Override public void onRefresh() {
        AppCatalog.refreshInstalled(this);
        AppCatalog.checkAllUpdates(this);
        refreshAllPages();
    }

    @Override public void onCatalogChanged() {
        refreshAllPages();
    }

    private void applySystemBars(boolean light) {
        WindowInsetsController ic = getWindow().getInsetsController();
        if (ic == null) return;
        int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        ic.setSystemBarsAppearance(light ? mask : 0, mask);
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
