package com.oneuibook.state;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central suite catalog & GitHub Releases updater for State ("스테이트").
 * Tracks all 7 published Android apps, detects installed status/versions via PackageManager,
 * checks GitHub Releases in parallel, formats patch notes cleanly, and installs/updates APKs.
 */
final class AppCatalog {
    private AppCatalog() {}

    static final int CAT_ALL = 0, CAT_MEDIA = 1, CAT_PRODUCTIVITY = 2, CAT_SYSTEM = 3;

    static final class ModuleApp {
        final String id;
        final String name;
        final String tagline;
        final int category;
        final String pkg;
        final String repo;
        final String apkName;
        final int accent;
        final int icon;

        boolean installed;
        String installedVersion = "";
        String latestVersion = "";
        String releaseNotes = "";
        String apkUrl = "";
        String pageUrl = "";
        boolean downloading;
        long downloadId = -1;

        ModuleApp(String id, String name, String tagline, int category, String pkg, String repo,
                  String apkName, int accent, int icon) {
            this.id = id;
            this.name = name;
            this.tagline = tagline;
            this.category = category;
            this.pkg = pkg;
            this.repo = repo;
            this.apkName = apkName;
            this.accent = accent;
            this.icon = icon;
            this.pageUrl = "https://github.com/" + repo + "/releases";
        }

        boolean hasUpdate() {
            return installed && !latestVersion.isEmpty() && newer(latestVersion, installedVersion);
        }
    }

    interface Listener {
        void onCatalogChanged();
    }

    static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4);
    static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    static final List<ModuleApp> APPS = new ArrayList<>();
    static boolean checking = false;
    static long lastCheckedMs = 0;

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("state_hub", Context.MODE_PRIVATE);
    }

    static void init(Context c) {
        if (APPS.isEmpty()) {
            APPS.add(new ModuleApp("prism", "프리즘", "리퀴드 글래스 음악 · Spotify · YouTube Music · 비트퍼펙트",
                    CAT_MEDIA, "com.oneuibook.prism", "Bundelldebug/prism-music-android", "PrismMusic_Android.apk",
                    0xFFFA2D48, Icons.APP_PRISM));
            APPS.add(new ModuleApp("play", "플레이", "시네마 비디오 · 자막 · NPU 프로스케일러 · YouTube",
                    CAT_MEDIA, "com.oneuibook.play", "Bundelldebug/play-video-android", "PlayVideo_Android.apk",
                    0xFF387AFF, Icons.APP_PLAY));
            APPS.add(new ModuleApp("viewer", "뷰어", "HWP · HWPX · PDF · 오피스 문서 리치 뷰어",
                    CAT_PRODUCTIVITY, "com.samsung.oneui.viewer", "Bundelldebug/viewer-android", "SamsungViewer_Android.apk",
                    0xFF5E5CE6, Icons.APP_VIEWER));
            APPS.add(new ModuleApp("workspace", "워크스페이스", "문서 · 시트 · 프레젠테이션 · HWP 통합 편집",
                    CAT_PRODUCTIVITY, "com.oneuibook.workspace", "Bundelldebug/workspace-android", "Workspace_Android.apk",
                    0xFF0FB59D, Icons.APP_WORKSPACE));
            APPS.add(new ModuleApp("aurora", "오로라", "크로미움 기반 리퀴드 글래스 브라우저 · 웹 스토어 확장",
                    CAT_PRODUCTIVITY, "com.oneuibook.aurora", "Bundelldebug/aurora-android", "Aurora_Android.apk",
                    0xFF06B6D4, Icons.APP_AURORA));
            APPS.add(new ModuleApp("checker", "체커", "구독 · 고정지출 캘린더 · 실시간 환율 · 결제일 알림",
                    CAT_SYSTEM, "com.oneuibook.checker", "Bundelldebug/checker-android", "Checker_Android.apk",
                    0xFF7B61FF, Icons.APP_CHECKER));
            APPS.add(new ModuleApp("state", "스테이트", "통합 앱 모듈 허브 및 실시간 업데이트 관리자",
                    CAT_SYSTEM, "com.oneuibook.state", "Bundelldebug/state-android", "State_Android.apk",
                    0xFF4F46E5, Icons.APP_STATE));
        }
        loadCache(c);
        refreshInstalled(c);
    }

    private static void loadCache(Context c) {
        SharedPreferences p = prefs(c);
        lastCheckedMs = p.getLong("last_checked_ms", 0);
        for (ModuleApp a : APPS) {
            a.latestVersion = p.getString("ver_" + a.id, a.latestVersion);
            a.releaseNotes = p.getString("notes_" + a.id, a.releaseNotes);
            String cachedApk = p.getString("apk_" + a.id, "");
            a.apkUrl = isTrustedReleaseUrl(cachedApk, a.repo) ? cachedApk : "";
            String pg = p.getString("page_" + a.id, "");
            if (isTrustedReleaseUrl(pg, a.repo)) a.pageUrl = pg;
        }
    }

    private static void saveCache(Context c, ModuleApp a) {
        prefs(c).edit()
                .putString("ver_" + a.id, a.latestVersion)
                .putString("notes_" + a.id, a.releaseNotes)
                .putString("apk_" + a.id, a.apkUrl)
                .putString("page_" + a.id, a.pageUrl)
                .putLong("last_checked_ms", System.currentTimeMillis())
                .apply();
    }

    static void refreshInstalled(Context c) {
        PackageManager pm = c.getPackageManager();
        for (ModuleApp a : APPS) {
            if (a.pkg.equals(c.getPackageName())) {
                a.installed = true;
                a.installedVersion = MainActivity.VERSION;
                continue;
            }
            try {
                PackageInfo pi = pm.getPackageInfo(a.pkg, 0);
                a.installed = true;
                a.installedVersion = pi.versionName == null ? "" : pi.versionName.replaceFirst("^[vV]", "");
            } catch (Throwable t) {
                a.installed = false;
                a.installedVersion = "";
            }
        }
    }

    static int installedCount() {
        int n = 0;
        for (ModuleApp a : APPS) if (a.installed) n++;
        return n;
    }

    static List<ModuleApp> pendingUpdates() {
        List<ModuleApp> out = new ArrayList<>();
        for (ModuleApp a : APPS) if (a.hasUpdate()) out.add(a);
        return out;
    }

    static void notifyListeners() {
        MAIN.post(() -> {
            for (Listener l : listeners) l.onCatalogChanged();
        });
    }

    static void checkAllUpdates(Context c) {
        if (checking || "robolectric".equals(android.os.Build.FINGERPRINT)) return;
        checking = true;
        refreshInstalled(c);
        notifyListeners();

        AtomicInteger remaining = new AtomicInteger(APPS.size());
        Context appCtx = c.getApplicationContext();
        for (ModuleApp a : APPS) {
            EXEC.execute(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(
                            "https://api.github.com/repos/" + a.repo + "/releases/latest").openConnection();
                    conn.setConnectTimeout(7000);
                    conn.setReadTimeout(7000);
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    conn.setRequestProperty("User-Agent", "State-Android-Hub/" + MainActivity.VERSION);
                    if (conn.getResponseCode() == 200) {
                        String body = readStream(conn.getInputStream());
                        JSONObject o = new JSONObject(body);
                        String tag = o.optString("tag_name").replaceFirst("^[vV]", "");
                        if (!tag.isEmpty()) {
                            a.latestVersion = tag;
                            a.releaseNotes = o.optString("body");
                            String pg = o.optString("html_url", a.pageUrl);
                            if (isTrustedReleaseUrl(pg, a.repo)) a.pageUrl = pg;
                            a.apkUrl = "";
                            JSONArray assets = o.optJSONArray("assets");
                            if (assets != null) {
                                for (int i = 0; i < assets.length(); i++) {
                                    JSONObject as = assets.optJSONObject(i);
                                    if (as != null && as.optString("name").toLowerCase(Locale.US).endsWith(".apk")) {
                                        String cand = as.optString("browser_download_url");
                                        if (isTrustedReleaseUrl(cand, a.repo)) {
                                            a.apkUrl = cand;
                                            break;
                                        }
                                    }
                                }
                            }
                            saveCache(appCtx, a);
                        }
                    }
                    conn.disconnect();
                } catch (Throwable ignored) {}
                if (remaining.decrementAndGet() <= 0) {
                    checking = false;
                    lastCheckedMs = System.currentTimeMillis();
                    notifyListeners();
                }
            });
        }
    }

    private static String readStream(InputStream in) throws Exception {
        try (InputStream s = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n, total = 0;
            while ((n = s.read(buf)) > 0) {
                total += n;
                if (total > 1024 * 1024) throw new IllegalStateException("Response too large");
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** Validates that release/APK download URLs strictly belong to the given GitHub repo over HTTPS. */
    static boolean isTrustedReleaseUrl(String url, String repo) {
        if (url == null || repo == null || !url.startsWith("https://")) return false;
        try {
            Uri u = Uri.parse(url);
            String host = u.getHost();
            if (host == null) return false;
            String h = host.toLowerCase(Locale.ROOT);
            if ("github.com".equals(h)) {
                String p = u.getPath();
                return p != null && p.toLowerCase(Locale.ROOT).startsWith("/" + repo.toLowerCase(Locale.ROOT) + "/");
            }
            return "objects.githubusercontent.com".equals(h) || "release-assets.githubusercontent.com".equals(h);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Verifies downloaded APK package name matches {@code a.pkg}, carries valid signing certificates,
     * and (when updating an already-installed app) matches the installed app's signing certificate SHA-256.
     */
    static boolean verifyDownloadedApk(Context appCtx, Uri apkUri, File fallbackFile, ModuleApp a) {
        try {
            PackageManager pm = appCtx.getPackageManager();
            String path = null;
            if (fallbackFile != null && fallbackFile.isFile()) {
                path = fallbackFile.getAbsolutePath();
            } else if (apkUri != null && "file".equals(apkUri.getScheme())) {
                path = apkUri.getPath();
            }
            if (path == null) return false;
            int flags = PackageManager.GET_SIGNING_CERTIFICATES;
            PackageInfo arch = pm.getPackageArchiveInfo(path, flags);
            if (arch == null || !a.pkg.equals(arch.packageName)) return false;
            java.util.Set<String> actual = certDigests(arch);
            if (actual.isEmpty()) return false;
            if (a.installed) {
                PackageInfo cur = pm.getPackageInfo(a.pkg, flags);
                java.util.Set<String> expected = certDigests(cur);
                if (expected.isEmpty()) return false;
                for (String d : actual) {
                    if (expected.contains(d)) return true;
                }
                return false;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static java.util.Set<String> certDigests(PackageInfo pi) {
        java.util.Set<String> out = new java.util.HashSet<>();
        if (pi == null || pi.signingInfo == null) return out;
        android.content.pm.Signature[] sigs = pi.signingInfo.hasMultipleSigners()
                ? pi.signingInfo.getApkContentsSigners()
                : pi.signingInfo.getSigningCertificateHistory();
        if (sigs == null) return out;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            for (android.content.pm.Signature s : sigs) {
                byte[] d = md.digest(s.toByteArray());
                StringBuilder sb = new StringBuilder();
                for (byte b : d) sb.append(String.format(Locale.US, "%02x", b & 0xff));
                out.add(sb.toString());
            }
        } catch (Throwable ignored) {}
        return out;
    }

    static boolean newer(String latest, String installed) {
        if (latest == null || latest.isEmpty()) return false;
        if (installed == null || installed.isEmpty()) return true;
        String[] x = latest.replaceFirst("^[vV]", "").split("[.\\-+]");
        String[] y = installed.replaceFirst("^[vV]", "").split("[.\\-+]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? parseNum(x[i]) : 0;
            int q = i < y.length ? parseNum(y[i]) : 0;
            if (p != q) return p > q;
        }
        return false;
    }

    private static int parseNum(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D", "")); } catch (Throwable t) { return 0; }
    }

    static void launchApp(Context c, ModuleApp a) {
        try {
            if (a.pkg.equals(c.getPackageName())) return;
            Intent launch = c.getPackageManager().getLaunchIntentForPackage(a.pkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(launch);
            } else {
                Toast.makeText(c, a.name + " 앱을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Toast.makeText(c, a.name + " 앱을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    static void openAppInfo(Context c, ModuleApp a) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Throwable ignored) {}
    }

    static void uninstallApp(Context c, ModuleApp a) {
        try {
            Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Throwable ignored) {}
    }

    private static final java.util.ArrayDeque<ModuleApp> UPDATE_QUEUE = new java.util.ArrayDeque<>();

    static void installOrUpdate(Activity act, ModuleApp a) {
        if (a == null || a.downloading) return;
        if (!isTrustedReleaseUrl(a.apkUrl, a.repo)) {
            String safePage = isTrustedReleaseUrl(a.pageUrl, a.repo) ? a.pageUrl : ("https://github.com/" + a.repo + "/releases");
            try { act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(safePage))); } catch (Throwable ignored) {}
            return;
        }
        if (!act.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(act, R.string.status_allow_install, Toast.LENGTH_LONG).show();
            try {
                act.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + act.getPackageName())));
            } catch (Throwable ignored) {}
            return;
        }
        DownloadManager dm = act.getSystemService(DownloadManager.class);
        if (dm == null) return;
        String safeVer = a.latestVersion == null || a.latestVersion.isEmpty() ? "latest" : a.latestVersion.replaceAll("[^A-Za-z0-9._-]", "");
        String fname = a.id + "-" + safeVer + ".apk";
        final File destFile = new File(act.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fname);
        if (destFile.exists()) destFile.delete();
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(a.apkUrl))
                .setTitle(a.name + (a.latestVersion.isEmpty() ? "" : " " + safeVer))
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(act, Environment.DIRECTORY_DOWNLOADS, fname);
        try {
            a.downloadId = dm.enqueue(req);
            a.downloading = true;
        } catch (Throwable t) {
            a.downloading = false;
            Toast.makeText(act, R.string.status_install_failed, Toast.LENGTH_LONG).show();
            return;
        }
        notifyListeners();
        Toast.makeText(act, a.name + " " + act.getString(R.string.status_downloading), Toast.LENGTH_SHORT).show();

        Context appCtx = act.getApplicationContext();
        final long expectedId = a.downloadId;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != expectedId) return;
                try { appCtx.unregisterReceiver(this); } catch (Throwable ignored) {}
                a.downloading = false;
                notifyListeners();
                Uri apkUri = dm.getUriForDownloadedFile(id);
                if (apkUri == null) {
                    Toast.makeText(appCtx, R.string.status_install_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                if (!verifyDownloadedApk(appCtx, apkUri, destFile, a)) {
                    try { destFile.delete(); } catch (Throwable ignored) {}
                    Toast.makeText(appCtx, a.name + " APK 패키지·서명 검증에 실패해 설치를 중단했습니다.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent install = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(apkUri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    appCtx.startActivity(install);
                } catch (Throwable t) {
                    Toast.makeText(appCtx, R.string.status_install_failed, Toast.LENGTH_LONG).show();
                }
            }
        };
        appCtx.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
    }

    static void updateAll(Activity act) {
        List<ModuleApp> list = pendingUpdates();
        if (list.isEmpty()) {
            Toast.makeText(act, R.string.status_all_up_to_date, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!act.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(act, R.string.status_allow_install, Toast.LENGTH_LONG).show();
            try {
                act.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + act.getPackageName())));
            } catch (Throwable ignored) {}
            return;
        }
        synchronized (UPDATE_QUEUE) {
            UPDATE_QUEUE.clear();
            UPDATE_QUEUE.addAll(list);
        }
        continuePendingQueue(act);
    }

    static void continuePendingQueue(Activity act) {
        ModuleApp next = null;
        synchronized (UPDATE_QUEUE) {
            for (ModuleApp a : APPS) if (a.downloading) return;
            while (!UPDATE_QUEUE.isEmpty()) {
                ModuleApp cand = UPDATE_QUEUE.poll();
                if (cand != null && cand.hasUpdate() && !cand.downloading) { next = cand; break; }
            }
        }
        if (next != null) installOrUpdate(act, next);
    }

    /** Clean GitHub Markdown release note renderer (strips raw #, **, `, [label](url)). */
    static CharSequence formatReleaseNotes(String raw) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (raw == null || raw.trim().isEmpty()) return sb;
        String text = raw.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (text.length() > 4000) text = text.substring(0, 4000) + "…";
        boolean lastBlank = true;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                if (!lastBlank && sb.length() > 0) { sb.append('\n'); lastBlank = true; }
                continue;
            }
            if (t.matches("^[-*_]{3,}$")) continue;
            if (sb.length() > 0) sb.append('\n');
            lastBlank = false;

            int h = 0;
            while (h < t.length() && t.charAt(h) == '#') h++;
            if (h > 0 && h < t.length() && t.charAt(h) == ' ') {
                int st = sb.length();
                appendInline(sb, t.substring(h + 1).trim());
                int en = sb.length();
                if (en > st) {
                    sb.setSpan(new StyleSpan(Typeface.BOLD), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new RelativeSizeSpan(h <= 2 ? 1.12f : 1.05f), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                continue;
            }
            int indent = 0;
            while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) indent++;
            String after = line.substring(indent);
            if (after.length() >= 2 && (after.charAt(0) == '-' || after.charAt(0) == '*' || after.charAt(0) == '+') && after.charAt(1) == ' ') {
                boolean nested = indent >= 2;
                int st = sb.length();
                sb.append(nested ? "◦  " : "•  ");
                appendInline(sb, after.substring(2).trim());
                sb.setSpan(new LeadingMarginSpan.Standard(Ui.dp(nested ? 16 : 4), Ui.dp(nested ? 26 : 14)),
                        st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            Matcher nm = Pattern.compile("^(\\d+)[.)]\\s+(.*)$").matcher(after);
            if (nm.matches()) {
                int st = sb.length();
                sb.append(nm.group(1)).append(". ");
                appendInline(sb, nm.group(2).trim());
                sb.setSpan(new LeadingMarginSpan.Standard(Ui.dp(4), Ui.dp(16)), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            appendInline(sb, t);
        }
        return sb;
    }

    private static void appendInline(SpannableStringBuilder sb, String s) {
        int i = 0, n = s.length();
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < n) { sb.append(s.charAt(i + 1)); i += 2; continue; }
            if (ch == '[') {
                int cb = s.indexOf(']', i + 1);
                if (cb > i + 1 && cb + 1 < n && s.charAt(cb + 1) == '(') {
                    int cp = s.indexOf(')', cb + 2);
                    if (cp > cb + 2) {
                        int st = sb.length();
                        appendInline(sb, s.substring(i + 1, cb));
                        if (sb.length() > st) sb.setSpan(new UnderlineSpan(), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        i = cp + 1;
                        continue;
                    }
                }
            }
            if (ch == '`') {
                int en = s.indexOf('`', i + 1);
                if (en > i + 1) {
                    int st = sb.length();
                    sb.append(s, i + 1, en);
                    sb.setSpan(new TypefaceSpan("monospace"), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new RelativeSizeSpan(0.92f), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = en + 1;
                    continue;
                }
            }
            if ((ch == '*' || ch == '_') && i + 1 < n && s.charAt(i + 1) == ch) {
                String d = ch == '*' ? "**" : "__";
                int en = s.indexOf(d, i + 2);
                if (en > i + 2) {
                    int st = sb.length();
                    appendInline(sb, s.substring(i + 2, en));
                    if (sb.length() > st) sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = en + 2;
                    continue;
                }
            }
            if (ch == '~' && i + 1 < n && s.charAt(i + 1) == '~') {
                int en = s.indexOf("~~", i + 2);
                if (en > i + 2) {
                    int st = sb.length();
                    appendInline(sb, s.substring(i + 2, en));
                    if (sb.length() > st) sb.setSpan(new StrikethroughSpan(), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = en + 2;
                    continue;
                }
            }
            sb.append(ch);
            i++;
        }
    }
}
