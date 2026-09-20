package com.codingroadmap.live;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import org.json.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Stable shell: only verified HTML from the owner's pinned GitHub commit is loaded. */
public final class LiveActivity extends Activity {
    private static final String CHANNEL = "https://raw.githubusercontent.com/irewon1-lgtm/Myapp/coding-roadmap-live/live/channel.json";
    private static final String RAW = "https://raw.githubusercontent.com/irewon1-lgtm/Myapp/";
    private static final int MAX_HTML = 3 * 1024 * 1024;
    private WebView web;
    private SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile String status = "업데이트 확인 중";
    private volatile boolean checking = false;
    private boolean displayed = false;
    private String pendingExport;
    private String activeHtml;
    private String visibleHtml;
    
    private boolean healthy = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (android.os.Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, () -> onBackPressed());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(19,21,24));
        getWindow().setNavigationBarColor(Color.rgb(19,21,24));
        prefs = getSharedPreferences("codingroadmap_live", MODE_PRIVATE);
        TextView splash = new TextView(this);
        splash.setText("코딩 로드맵\n\n학습 내용을 준비하고 있어요");
        splash.setTextSize(20); splash.setTextColor(Color.rgb(239,235,226));
        splash.setBackgroundColor(Color.rgb(19,21,24)); splash.setGravity(17);
        setContentView(splash);
        // Cache is independent of reading progress; an interrupted update cannot erase notes.
        activeHtml = readCached();
        checkUpdate(true);
        main.postDelayed(() -> { if (!displayed) showReader(activeHtml); }, 2200);
    }

    private String readCached() {
        File f = new File(getFilesDir(), "codingroadmap-live.html");
        try {
            byte[] bytes = read(new FileInputStream(f), MAX_HTML);
            if (sha(bytes).equals(prefs.getString("html_sha", ""))) return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        try { return new String(read(getAssets().open("live/reader.html"), MAX_HTML), StandardCharsets.UTF_8); }
        catch (Exception e) { return "<html><body style='background:#131518;color:white'>교재를 열지 못했습니다.</body></html>"; }
    }

    private synchronized void checkUpdate(boolean launch) {
        if (checking) return;
        checking = true;
        new Thread(() -> {
            String updated = null;
            try {
                JSONObject m = new JSONObject(new String(download(CHANNEL + "?t=" + System.currentTimeMillis(), 16384), StandardCharsets.UTF_8));
                if (!"codingroadmap-live-v1".equals(m.getString("schema"))) throw new IOException("schema");
                if (m.getInt("minShell") > 1) { status = "앱 본체 업데이트가 필요합니다"; return; }
                long version = m.getLong("version");
                long current = prefs.getLong("version", 2026092001L);
                if (version <= current) { status = "최신 교재입니다"; return; }
                String commit = m.getString("commit");
                String digest = m.getString("sha256");
                if (!commit.matches("[0-9a-f]{40}") || !digest.matches("[0-9a-f]{64}")) throw new IOException("identity");
                byte[] bytes = download(RAW + commit + "/live/reader.html", MAX_HTML);
                if (bytes.length != m.getInt("size") || !sha(bytes).equals(digest)) throw new IOException("integrity");
                String html = new String(bytes, StandardCharsets.UTF_8);
                if (!html.contains("codingroadmap-live-v1") || !html.contains("</html>")) throw new IOException("format");
                File target = new File(getFilesDir(), "codingroadmap-live.html");
                File temp = new File(getFilesDir(), "codingroadmap-live.tmp");
                try (FileOutputStream out = new FileOutputStream(temp)) { out.write(bytes); out.getFD().sync(); }
                if (!temp.renameTo(target)) throw new IOException("atomic rename");
                prefs.edit().putString("html_sha", digest).putLong("version", version).putString("release", m.optString("label", "")).commit();
                updated = html;
                status = "새 교재를 받았습니다 · 다음 실행에 반영";
            } catch (Exception e) { status = "오프라인 또는 연결 지연 · 저장된 교재 사용"; }
            finally {
                checking = false;
                final String result = updated;
                main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (result != null) activeHtml = result;
                    if (launch && !displayed) showReader(activeHtml);
                    else if (web != null) web.evaluateJavascript("window.onUpdateStatus && window.onUpdateStatus(" + JSONObject.quote(status) + ")", null);
                });
            }
        }, "roadmap-update").start();
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void showReader(String html) {
        if (isFinishing() || isDestroyed()) return;
        visibleHtml = html;
        displayed = true;
        if (web != null) web.destroy();
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(19,21,24));
        web.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(), "RoadmapNative");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("https".equals(u.getScheme()) && !"codingroadmap.local".equals(u.getHost())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                }
                return true;
            }
        });
        setContentView(web);
        healthy = false;
        web.loadDataWithBaseURL("https://codingroadmap.local/", html, "text/html", "UTF-8", null);
        main.postDelayed(() -> {
            if (!healthy && !isFinishing()) {
                try {
                    String bundled = new String(read(getAssets().open("live/reader.html"), MAX_HTML), StandardCharsets.UTF_8);
                    if (!bundled.equals(html)) {
                        status = "새 화면 오류 · 기본 교재로 복구";
                        web.loadDataWithBaseURL("https://codingroadmap.local/", bundled, "text/html", "UTF-8", null);
                    }
                } catch (Exception ignored) {}
            }
        }, 6000);
    }

    public final class Bridge {
        @JavascriptInterface public String getState() { return prefs.getString("reading_state", "{}"); }
        @JavascriptInterface public void saveState(String json) {
            if (json.length() > 2 * 1024 * 1024) return;
            try { new JSONObject(json); prefs.edit().putString("reading_state", json).apply(); } catch (Exception ignored) {}
        }
        @JavascriptInterface public String getLegacy() { return legacyState().toString(); }
        @JavascriptInterface public String getStatus() { return status; }
        @JavascriptInterface public void ready() { healthy = true; }
        @JavascriptInterface public void refresh() { main.post(() -> checkUpdate(false)); }
        @JavascriptInterface public void apply() { main.post(() -> showReader(readCached())); }
        @JavascriptInterface public void openLegacy() { main.post(() -> {
            try { startActivity(new Intent().setClassName(LiveActivity.this, "com.codingroadmap.app.MainActivity")); }
            catch (Exception e) { Toast.makeText(LiveActivity.this, "기존 화면을 열지 못했습니다", Toast.LENGTH_LONG).show(); }
        }); }
        @JavascriptInterface public void exportState(String json) {
            if (json.length() > 2 * 1024 * 1024) return;
            main.post(() -> {
                pendingExport = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "CodingRoadmap-progress.json");
                startActivityForResult(intent, 41);
            });
        }
        @JavascriptInterface public void importState() { main.post(() -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE), 42)); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (request == 41 && pendingExport != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) { out.write(pendingExport.getBytes(StandardCharsets.UTF_8)); }
                pendingExport = null;
                Toast.makeText(this, "학습 기록을 저장했습니다", Toast.LENGTH_SHORT).show();
            } else if (request == 42) {
                String json = new String(read(getContentResolver().openInputStream(data.getData()), 2 * 1024 * 1024), StandardCharsets.UTF_8);
                web.evaluateJavascript("window.importProgress(" + JSONObject.quote(json) + ")", null);
            }
        } catch (Exception e) { Toast.makeText(this, "파일을 처리하지 못했습니다", Toast.LENGTH_LONG).show(); }
    }
    @Override public void onBackPressed() {
        if (web == null) { super.onBackPressed(); return; }
        web.evaluateJavascript("window.goBack ? window.goBack() : false", result -> { if (!"true".equals(result)) finish(); });
    }
    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            String cached = readCached();
            if (!cached.equals(visibleHtml)) showReader(cached);
            checkUpdate(false);
        }
    }
    @Override protected void onDestroy() { main.removeCallbacksAndMessages(null); if (web != null) { web.removeJavascriptInterface("RoadmapNative"); web.destroy(); } super.onDestroy(); }

    static byte[] download(String address, int limit) throws Exception {
        HttpsURLConnection c = (HttpsURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(3500); c.setReadTimeout(5000); c.setUseCaches(false);
        c.setInstanceFollowRedirects(false); c.setRequestProperty("Cache-Control", "no-cache");
        try { if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode()); return read(c.getInputStream(), limit); }
        finally { c.disconnect(); }
    }
    static byte[] read(InputStream in, int limit) throws IOException {
        if (in == null) throw new IOException("missing stream");
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n;
            while ((n = input.read(buf)) != -1) { if (out.size() + n > limit) throw new IOException("size limit"); out.write(buf, 0, n); }
            return out.toByteArray();
        }
    }
    static String sha(byte[] b) throws Exception { StringBuilder s = new StringBuilder(); for (byte v : MessageDigest.getInstance("SHA-256").digest(b)) s.append(String.format(Locale.ROOT, "%02x", v & 255)); return s.toString(); }

    // Read-only migration of AndroidX Preferences protobuf. Original file stays untouched.
    private JSONObject legacyState() {
        JSONObject result = new JSONObject();
        try {
            File path = new File(getFilesDir(), "datastore/roadmap_reader.preferences_pb");
            P root = new P(read(new FileInputStream(path), 2 * 1024 * 1024));
            while (root.more()) {
                int tag = (int)root.var();
                if (tag != 10) { root.skip(tag); continue; }
                P entry = new P(root.bytes()); String key = null; byte[] val = null;
                while (entry.more()) { int t = (int)entry.var(); if (t == 10) key = new String(entry.bytes(), StandardCharsets.UTF_8); else if (t == 18) val = entry.bytes(); else entry.skip(t); }
                if (key != null && val != null) {
                    P value = new P(val); if (!value.more()) continue;
                    int t = (int)value.var(); Object obj = null;
                    if (t == 24 || t == 32) obj = value.var();
                    else if (t == 21) obj = Float.intBitsToFloat(value.fixed32());
                    else if (t == 42) obj = new String(value.bytes(), StandardCharsets.UTF_8);
                    else if (t == 50) { P set = new P(value.bytes()); JSONArray a = new JSONArray(); while(set.more()) { int st = (int)set.var(); if(st == 10) a.put(new String(set.bytes(), StandardCharsets.UTF_8)); else set.skip(st); } obj = a; }
                    if (obj != null) result.put(key, obj);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }
    static final class P {
        final byte[] b; int p;
        P(byte[] b) { this.b = b; }
        boolean more() { return p < b.length; }
        long var() throws IOException { long v=0; for(int s=0;s<64;s+=7) { if(p>=b.length)throw new IOException(); int x=b[p++]&255; v|=(long)(x&127)<<s; if((x&128)==0)return v; }throw new IOException(); }
        byte[] bytes() throws IOException { long l=var(); if(l<0||l>b.length-p)throw new IOException(); byte[] r=Arrays.copyOfRange(b,p,p+(int)l);p+=(int)l;return r; }
        int fixed32() throws IOException { if(p+4>b.length)throw new IOException(); int r=(b[p]&255)|((b[p+1]&255)<<8)|((b[p+2]&255)<<16)|((b[p+3]&255)<<24);p+=4;return r; }
        void skip(int t) throws IOException { switch(t&7) { case 0:var();break; case 1:p+=8;break;case 2:bytes();break;case 5:p+=4;break;default:throw new IOException(); }if(p>b.length)throw new IOException(); }
    }
}
