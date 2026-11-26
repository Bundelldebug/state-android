package com.oneuibook.state;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All screens of State ("스테이트"): Good Lock-inspired Apps Hub, Updates Manager, Settings, and Search. */
final class Pages {
    private Pages() {}

    abstract static class BasePage extends FrameLayout {
        final MainActivity main;
        final ScrollView scroll;
        final LinearLayout content;
        final String title;
        TextView bigTitle;

        BasePage(MainActivity m, String title) {
            super(m);
            this.main = m;
            this.title = title;
            setBackgroundColor(Ui.bg());
            scroll = new ScrollView(m) {
                @Override protected void onScrollChanged(int l, int t, int ol, int ot) {
                    super.onScrollChanged(l, t, ol, ot);
                    main.onPageScroll(BasePage.this, t);
                }
            };
            scroll.setVerticalScrollBarEnabled(false);
            scroll.setClipToPadding(false);
            content = new LinearLayout(m);
            content.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            applyInsets();
        }

        void applyInsets() {
            int top = Ui.statusInset + Ui.dp(62);
            int bot = Math.max(Ui.navInset, Ui.dp(12)) + Ui.dp(92);
            content.setPadding(0, top, 0, bot);
        }

        void rebuild() {
            content.removeAllViews();
            bigTitle = Ui.text(main, title, 34f, Ui.bold, Ui.text());
            bigTitle.setLetterSpacing(-0.02f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Ui.dp(20), Ui.dp(8), Ui.dp(20), Ui.dp(10));
            content.addView(bigTitle, lp);
            build();
        }

        float titleProgress() {
            int y = scroll.getScrollY();
            if (y <= Ui.dp(8)) return 0f;
            return Ui.clamp((y - Ui.dp(12)) / (float) Ui.dp(28), 0f, 1f);
        }

        float blurProgress() { return Ui.clamp(scroll.getScrollY() / (float) Ui.dp(20), 0f, 1f); }

        abstract void build();
    }

    /** Good Lock-style Module Hub showing all suite apps with installed state, versions, and 1-tap actions. */
    static final class AppsPage extends BasePage {
        private int category = AppCatalog.CAT_ALL;

        AppsPage(MainActivity m) {
            super(m, m.getString(R.string.app_name));
        }

        @Override void build() {
            int installed = AppCatalog.installedCount();
            int total = AppCatalog.APPS.size();
            int updates = AppCatalog.pendingUpdates().size();

            // Compact summary status bar (Good Lock style clean pill strip)
            LinearLayout strip = new LinearLayout(main);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            strip.setGravity(Gravity.CENTER_VERTICAL);
            strip.setPadding(Ui.dp(20), Ui.dp(2), Ui.dp(20), Ui.dp(8));

            strip.addView(Widgets.pillBadge(main,
                    main.getString(R.string.status_installed_summary, installed, total),
                    Ui.chip(), Ui.text()));

            if (updates > 0) {
                TextView uBadge = Widgets.pillBadge(main,
                        main.getString(R.string.status_updates_avail, updates),
                        Ui.alpha(Ui.ACCENT, 0.16f), Ui.ACCENT);
                uBadge.setOnClickListener(v -> main.switchTab(1));
                LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ulp.leftMargin = Ui.dp(8);
                strip.addView(uBadge, ulp);
            } else if (AppCatalog.checking) {
                TextView chk = Ui.text(main, main.getString(R.string.status_checking), 12f, Ui.medium, Ui.text2());
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                clp.leftMargin = Ui.dp(10);
                strip.addView(chk, clp);
            }
            content.addView(strip, Widgets.MW);

            // Category filter chips
            String[] cats = {
                    main.getString(R.string.cat_all),
                    main.getString(R.string.cat_media),
                    main.getString(R.string.cat_productivity),
                    main.getString(R.string.cat_system)
            };
            content.addView(new Widgets.Chips(main, cats, category, idx -> {
                category = idx;
                rebuild();
            }), Widgets.MW);

            boolean showUninstalled = AppCatalog.prefs(main).getBoolean("show_uninstalled", true);
            List<AppCatalog.ModuleApp> installedList = new ArrayList<>();
            List<AppCatalog.ModuleApp> availableList = new ArrayList<>();
            for (AppCatalog.ModuleApp a : AppCatalog.APPS) {
                if (category != AppCatalog.CAT_ALL && a.category != category) continue;
                if (a.installed) installedList.add(a);
                else if (showUninstalled) availableList.add(a);
            }

            if (!installedList.isEmpty()) {
                Widgets.Group g = new Widgets.Group(main, "설치된 모듈 (" + installedList.size() + ")");
                for (AppCatalog.ModuleApp a : installedList) g.addCustom(buildModuleRow(main, a, false));
                content.addView(g, Widgets.MW);
            }

            if (!availableList.isEmpty()) {
                Widgets.Group g = new Widgets.Group(main, "설치 가능한 모듈 (" + availableList.size() + ")");
                for (AppCatalog.ModuleApp a : availableList) g.addCustom(buildModuleRow(main, a, false));
                content.addView(g, Widgets.MW);
            }
        }
    }

    /** Dedicated Updates Manager tab with "모두 업데이트" batch banner and clean patch-note previews. */
    static final class UpdatesPage extends BasePage {
        UpdatesPage(MainActivity m) {
            super(m, m.getString(R.string.tab_updates));
        }

        @Override void build() {
            List<AppCatalog.ModuleApp> pending = AppCatalog.pendingUpdates();

            // Top summary / Update All card
            Widgets.Group top = new Widgets.Group(main, null);
            LinearLayout banner = new LinearLayout(main);
            banner.setOrientation(LinearLayout.HORIZONTAL);
            banner.setGravity(Gravity.CENTER_VERTICAL);
            banner.setPadding(Ui.dp(18), Ui.dp(16), Ui.dp(16), Ui.dp(16));

            LinearLayout txtCol = new LinearLayout(main);
            txtCol.setOrientation(LinearLayout.VERTICAL);
            String headline = AppCatalog.checking
                    ? main.getString(R.string.status_checking)
                    : (pending.isEmpty() ? main.getString(R.string.status_all_up_to_date)
                    : main.getString(R.string.status_updates_avail, pending.size()));
            txtCol.addView(Ui.text(main, headline, 15.5f, Ui.bold, Ui.text()));
            String sub = pending.isEmpty()
                    ? "GitHub 릴리스에서 최신 버전을 확인합니다."
                    : "한 번의 탭으로 대기 중인 모듈 APK를 순차 다운로드합니다.";
            TextView subTv = Ui.text(main, sub, 12.5f, Ui.regular, Ui.text2());
            subTv.setSingleLine(false);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = Ui.dp(3);
            txtCol.addView(subTv, slp);
            banner.addView(txtCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView actBtn = Ui.text(main,
                    pending.isEmpty() ? main.getString(R.string.btn_check_now) : main.getString(R.string.btn_update_all),
                    13f, Ui.semibold, 0xFFFFFFFF);
            actBtn.setPadding(Ui.dp(14), Ui.dp(9), Ui.dp(14), Ui.dp(9));
            actBtn.setBackground(Ui.round(Ui.ACCENT, 100));
            actBtn.setOnClickListener(v -> {
                if (pending.isEmpty()) AppCatalog.checkAllUpdates(main);
                else AppCatalog.updateAll(main);
            });
            Ui.press(actBtn, 0.94f);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.leftMargin = Ui.dp(10);
            banner.addView(actBtn, blp);
            top.addCustom(banner);
            content.addView(top, Widgets.MW);

            if (!pending.isEmpty()) {
                Widgets.Group pg = new Widgets.Group(main, "업데이트 대기 (" + pending.size() + ")");
                for (AppCatalog.ModuleApp a : pending) pg.addCustom(buildModuleRow(main, a, true));
                content.addView(pg, Widgets.MW);
            }

            List<AppCatalog.ModuleApp> upToDate = new ArrayList<>();
            for (AppCatalog.ModuleApp a : AppCatalog.APPS) if (a.installed && !a.hasUpdate()) upToDate.add(a);
            if (!upToDate.isEmpty()) {
                Widgets.Group ug = new Widgets.Group(main, "최신 상태 (" + upToDate.size() + ")");
                for (AppCatalog.ModuleApp a : upToDate) ug.addCustom(buildModuleRow(main, a, false));
                content.addView(ug, Widgets.MW);
            }
        }
    }

    /** Unified State Settings screen using Kyant0 LiquidToggle switches. */
    static final class SettingsPage extends BasePage {
        SettingsPage(MainActivity m) {
            super(m, m.getString(R.string.tab_settings));
        }

        @Override void build() {
            SharedPreferences p = AppCatalog.prefs(main);

            // 1. Appearance
            Widgets.Group appear = new Widgets.Group(main, main.getString(R.string.settings_appearance));
            String theme = p.getString("theme", "system");
            String[] themeLabels = {main.getString(R.string.theme_system), main.getString(R.string.theme_light), main.getString(R.string.theme_dark)};
            String[] themeVals = {"system", "light", "dark"};
            int tIdx = "light".equals(theme) ? 1 : ("dark".equals(theme) ? 2 : 0);
            appear.addRow(Icons.PALETTE, Ui.ACCENT, main.getString(R.string.theme_label), themeLabels[tIdx], true, v ->
                    Widgets.pickDialog(main, main.getString(R.string.theme_label), themeLabels, tIdx, pick -> {
                        p.edit().putString("theme", themeVals[pick]).apply();
                        main.recreate();
                    }));

            String style = p.getString("ui_style", "a");
            String[] styleLabels = {main.getString(R.string.style_a), main.getString(R.string.style_g)};
            int sIdx = "g".equals(style) ? 1 : 0;
            appear.addRow(Icons.APPS, Ui.ACCENT, main.getString(R.string.style_label), styleLabels[sIdx], true, v ->
                    Widgets.pickDialog(main, main.getString(R.string.style_label), styleLabels, sIdx, pick -> {
                        p.edit().putString("ui_style", pick == 1 ? "g" : "a").apply();
                        main.recreate();
                    }));

            String accKey = Ui.accentKey(p);
            String[] accLabels = new String[Ui.ACCENT_LABELS.length];
            int aIdx = 0;
            for (int i = 0; i < Ui.ACCENT_LABELS.length; i++) {
                accLabels[i] = main.getString(Ui.ACCENT_LABELS[i]);
                if (Ui.ACCENT_KEYS[i].equals(accKey)) aIdx = i;
            }
            final int faIdx = aIdx;
            appear.addRow(Icons.PALETTE, Ui.ACCENT, main.getString(R.string.accent_label), accLabels[faIdx], true, v ->
                    Widgets.pickDialog(main, main.getString(R.string.accent_label), accLabels, faIdx, pick -> {
                        p.edit().putString("accent_color", Ui.ACCENT_KEYS[pick]).apply();
                        main.recreate();
                    }));

            LinearLayout sliderBox = new LinearLayout(main);
            sliderBox.setOrientation(LinearLayout.VERTICAL);
            sliderBox.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(8));
            sliderBox.addView(Ui.text(main, main.getString(R.string.glass_transparency), 14.5f, Ui.medium, Ui.text()));
            sliderBox.addView(new Widgets.GlassSlider(main, Ui.glassT, val -> {
                Ui.glassT = val;
                p.edit().putFloat("glass_t", val).apply();
                main.header.invalidate();
                main.dock.invalidate();
            }));
            appear.addCustom(sliderBox);
            content.addView(appear, Widgets.MW);

            // 2. Update Management (Kyant0 LiquidToggles)
            Widgets.Group upd = new Widgets.Group(main, main.getString(R.string.settings_updates));
            upd.addToggle(main.getString(R.string.pref_auto_check), "실행 시 GitHub 최신 릴리스 자동 조회",
                    p.getBoolean("auto_check", true), on -> p.edit().putBoolean("auto_check", on).apply());
            upd.addToggle(main.getString(R.string.pref_show_uninstalled), "아직 설치하지 않은 모듈도 앱 탭에 표시",
                    p.getBoolean("show_uninstalled", true), on -> {
                        p.edit().putBoolean("show_uninstalled", on).apply();
                        main.refreshAllPages();
                    });
            upd.addRow(Icons.REFRESH, Ui.ACCENT, main.getString(R.string.btn_check_now), "", true, v -> AppCatalog.checkAllUpdates(main));
            content.addView(upd, Widgets.MW);

            // 3. About
            Widgets.Group about = new Widgets.Group(main, main.getString(R.string.settings_about));
            about.addRow(Icons.INFO, Ui.ACCENT, main.getString(R.string.about_version), MainActivity.VERSION, false, null);
            about.addRow(Icons.EXTERNAL, Ui.ACCENT, main.getString(R.string.about_repo), "Bundelldebug", true, v -> {
                try { main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Bundelldebug"))); } catch (Throwable ignored) {}
            });
            content.addView(about, Widgets.MW);
        }
    }

    /** Instant Search overlay filtering all 9 suite modules. */
    static final class SearchPage extends BasePage {
        private String query = "";

        SearchPage(MainActivity m) {
            super(m, m.getString(R.string.app_name));
        }

        void setQuery(String q) {
            query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
            rebuild();
        }

        @Override void build() {
            List<AppCatalog.ModuleApp> hits = new ArrayList<>();
            for (AppCatalog.ModuleApp a : AppCatalog.APPS) {
                if (query.isEmpty()
                        || a.name.toLowerCase(Locale.ROOT).contains(query)
                        || a.tagline.toLowerCase(Locale.ROOT).contains(query)
                        || a.pkg.toLowerCase(Locale.ROOT).contains(query)) {
                    hits.add(a);
                }
            }
            Widgets.Group g = new Widgets.Group(main, query.isEmpty() ? "전체 모듈 (" + hits.size() + ")" : "검색 결과 (" + hits.size() + ")");
            if (hits.isEmpty()) {
                TextView empty = Ui.text(main, "일치하는 앱 모듈이 없습니다.", 14f, Ui.regular, Ui.text2());
                empty.setPadding(Ui.dp(18), Ui.dp(22), Ui.dp(18), Ui.dp(22));
                g.addCustom(empty);
            } else {
                for (AppCatalog.ModuleApp a : hits) g.addCustom(buildModuleRow(main, a, false));
            }
            content.addView(g, Widgets.MW);
        }
    }

    /** Builds a crisp Good Lock-style module row with squircle badge, version status, and action pill. */
    private static View buildModuleRow(MainActivity m, AppCatalog.ModuleApp a, boolean showNotesPreview) {
        LinearLayout outer = new LinearLayout(m);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(Widgets.ripple());
        outer.setPadding(Ui.dp(16), Ui.dp(13), Ui.dp(14), Ui.dp(13));

        LinearLayout row = new LinearLayout(m);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Icons.AppBadgeView badge = new Icons.AppBadgeView(m, a);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(46), Ui.dp(46));
        blp.rightMargin = Ui.dp(13);
        row.addView(badge, blp);

        LinearLayout info = new LinearLayout(m);
        info.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(m);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(Ui.text(m, a.name, 15.5f, Ui.semibold, Ui.text()));

        String verText;
        int verColor;
        if (a.hasUpdate()) {
            verText = a.installedVersion + " → " + a.latestVersion;
            verColor = Ui.ACCENT;
        } else if (a.installed) {
            verText = "v" + a.installedVersion;
            verColor = Ui.text2();
        } else if (!a.latestVersion.isEmpty()) {
            verText = "v" + a.latestVersion;
            verColor = Ui.text2();
        } else {
            verText = m.getString(R.string.status_not_installed);
            verColor = Ui.text3();
        }
        TextView verTv = Ui.text(m, verText, 11.5f, a.hasUpdate() ? Ui.bold : Ui.medium, verColor);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.leftMargin = Ui.dp(7);
        titleRow.addView(verTv, vlp);
        info.addView(titleRow, Widgets.MW);

        TextView tag = Ui.text(m, a.tagline, 12.5f, Ui.regular, Ui.text2());
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(2);
        info.addView(tag, tlp);

        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Right-hand action pill: 업데이트 / 열기 / 설치
        boolean isUpdate = a.hasUpdate();
        String btnLabel = a.downloading ? "…"
                : (isUpdate ? m.getString(R.string.btn_update)
                : (a.installed ? m.getString(R.string.btn_open) : m.getString(R.string.btn_install)));
        int btnBg = isUpdate ? Ui.ACCENT : (a.installed ? Ui.chip() : Ui.alpha(Ui.ACCENT, 0.14f));
        int btnFg = isUpdate ? 0xFFFFFFFF : (a.installed ? Ui.text() : Ui.ACCENT);

        TextView actionBtn = Ui.text(m, btnLabel, 12.5f, Ui.semibold, btnFg);
        actionBtn.setPadding(Ui.dp(13), Ui.dp(7), Ui.dp(13), Ui.dp(7));
        actionBtn.setBackground(Ui.round(btnBg, 100));
        Runnable primaryAction = () -> {
            if (isUpdate || !a.installed) AppCatalog.installOrUpdate(m, a);
            else AppCatalog.launchApp(m, a);
        };
        actionBtn.setOnClickListener(v -> primaryAction.run());
        Ui.press(actionBtn, 0.94f);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.leftMargin = Ui.dp(10);
        row.addView(actionBtn, alp);

        outer.addView(row, Widgets.MW);

        if (showNotesPreview && a.releaseNotes != null && !a.releaseNotes.trim().isEmpty()) {
            TextView notes = new TextView(m);
            notes.setText(AppCatalog.formatReleaseNotes(a.releaseNotes));
            notes.setTextSize(12.5f);
            notes.setLineSpacing(0f, 1.25f);
            notes.setTextColor(Ui.text2());
            notes.setMaxLines(3);
            notes.setEllipsize(TextUtils.TruncateAt.END);
            notes.setPadding(Ui.dp(12), Ui.dp(9), Ui.dp(12), Ui.dp(9));
            GradientDrawable nb = Ui.round(Ui.dark ? 0x16FFFFFF : 0x0A000000, 12);
            notes.setBackground(nb);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.topMargin = Ui.dp(10);
            outer.addView(notes, nlp);
        }

        outer.setOnClickListener(v -> Widgets.showNotesDialog(m, a, primaryAction));
        return outer;
    }
}
