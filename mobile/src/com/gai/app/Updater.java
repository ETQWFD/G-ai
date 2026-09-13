package com.gai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * G-ai 检查更新：从 GitHub Releases API 获取最新版本并提示下载。
 * 仓库：github.com/ETQWFD/G-ai（与官网/Release 同源）。
 */
public class Updater {

    public static final String REPO = "ETQWFD/G-ai";
    public static final String API = "https://api.github.com/repos/ETQWFD/G-ai/releases/latest";
    public static final String RELEASES = "https://github.com/ETQWFD/G-ai/releases";
    public static final String WEBSITE = "https://etqwfd.github.io/G-ai/";

    public interface Callback {
        void done(String latest, String notes, String error);
    }

    /** 在后台线程检查，结果回调到 UI 线程（latest=null 表示失败）。 */
    public static void check(final Activity act, final Callback cb) {
        new Thread(new Runnable() {
            public void run() {
                final String[] out = new String[3];
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
                    c.setRequestMethod("GET");
                    c.setRequestProperty("User-Agent", "G-ai/" + Prefs.VERSION);
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    int code = c.getResponseCode();
                    if (code == 200) {
                        String body = readAll(c.getInputStream());
                        String tag = Json.gs(Json.obj(body), "tag_name");
                        String notes = Json.gs(Json.obj(body), "body");
                        out[0] = tag == null ? null : tag.replace("v", "").trim();
                        out[1] = notes == null ? "" : notes.trim();
                    }
                    c.disconnect();
                } catch (Exception e) {
                    out[2] = e.getMessage();
                }
                act.runOnUiThread(new Runnable() {
                    public void run() { cb.done(out[0], out[1], out[2]); }
                });
            }
        }).start();
    }

    /** 标准流程：检查 → 有新版弹提示，无新版提示已最新，失败提示网络错误。 */
    public static void checkAndPrompt(final Activity act, final String buttonText) {
        act.runOnUiThread(new Runnable() {
            public void run() { toast(act, "正在检查更新…"); }
        });
        check(act, new Callback() {
            public void done(String latest, String notes, String error) {
                if (latest == null) {
                    toast(act, "检查更新失败：" + (error == null ? "无法连接 GitHub" : error));
                    return;
                }
                if (latest.equals(Prefs.VERSION)) {
                    toast(act, "当前已是最新版本 v" + Prefs.VERSION);
                    return;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(act);
                b.setTitle("发现新版本 v" + latest);
                b.setMessage("当前版本 v" + Prefs.VERSION + "\n\n" +
                        (notes == null || notes.isEmpty() ? "修复了若干问题并新增功能" : notes) +
                        "\n\n点击「前往下载」打开 GitHub Releases 页面。");
                b.setPositiveButton("前往下载", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try {
                            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES)));
                        } catch (Exception e) {
                            toast(act, "无法打开浏览器，请访问 " + RELEASES);
                        }
                    }
                });
                b.setNegativeButton("稍后再说", null);
                b.show();
            }
        });
    }

    private static String readAll(InputStream in) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line).append('\n');
        r.close();
        return sb.toString();
    }

    private static void toast(final Activity act, final String s) {
        act.runOnUiThread(new Runnable() {
            public void run() { android.widget.Toast.makeText(act, s, android.widget.Toast.LENGTH_LONG).show(); }
        });
    }
}
