package com.gai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

/** 主界面：动漫随机背景 · 许可协议 → 权限授权 → 启动/停止悬浮窗 */
public class MainActivity extends Activity {

    static final String MIT =
            "MIT License\n\n" +
            "Copyright (c) 2026 G-ai\n\n" +
            "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
            "of this software and associated documentation files (the \"Software\"), to deal\n" +
            "in the Software without restriction, including without limitation the rights\n" +
            "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
            "copies of the Software, and to permit persons to whom the Software is\n" +
            "furnished to do so, subject to the following conditions:\n\n" +
            "The above copyright notice and this permission notice shall be included in all\n" +
            "copies or substantial portions of the Software.\n\n" +
            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
            "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
            "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
            "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
            "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
            "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
            "SOFTWARE.";

    private static final int[] BG = {
            R.drawable.bg1, R.drawable.bg2, R.drawable.bg3,
            R.drawable.bg4, R.drawable.bg5, R.drawable.bg6
    };

    private TextView storageStatus, overlayStatus, accessStatus, aiStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        // 随机动漫背景
        ImageView bg = (ImageView) findViewById(R.id.bg);
        bg.setImageResource(BG[new Random().nextInt(BG.length)]);

        storageStatus = (TextView) findViewById(R.id.storageStatus);
        overlayStatus = (TextView) findViewById(R.id.overlayStatus);
        accessStatus = (TextView) findViewById(R.id.accessStatus);
        aiStatus = (TextView) findViewById(R.id.aiStatus);
        ((TextView) findViewById(R.id.versionText)).setText("v" + Prefs.VERSION + " · MIT License");
        Notify.ensureChannels(this);

        findViewById(R.id.btnStorage).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { requestStorage(); }
        });
        findViewById(R.id.btnOverlay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { requestOverlay(); }
        });
        findViewById(R.id.btnAccess).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { requestAccessibility(); }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        });
        findViewById(R.id.btnUpdate).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Updater.checkAndPrompt(MainActivity.this, null); }
        });
        findViewById(R.id.btnWebsite).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Updater.WEBSITE)));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "无法打开浏览器", Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doStart(); }
        });
        findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, FloatingWindowService.class));
                Toast.makeText(MainActivity.this, "已停止 G-ai 后台运行", Toast.LENGTH_SHORT).show();
            }
        });

        if (!Prefs.agreed(this)) {
            showLicense();
        } else {
            maybeRequestNotifPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        boolean sg = storageGranted();
        storageStatus.setText(sg ? "已授权" : "未授权");
        storageStatus.setTextColor(sg ? 0xFF6EE7B7 : 0xFFFCA5A5);
        boolean og = overlayGranted();
        overlayStatus.setText(og ? "已授权" : "未授权");
        overlayStatus.setTextColor(og ? 0xFF6EE7B7 : 0xFFFCA5A5);
        boolean ag = accessibilityGranted();
        accessStatus.setText(ag ? "已开启" : "未开启");
        accessStatus.setTextColor(ag ? 0xFF6EE7B7 : 0xFFFCA5A5);
        boolean cfg = Prefs.url(this).length() > 0 && Prefs.key(this).length() > 0 && Prefs.model(this).length() > 0;
        aiStatus.setText(cfg ? ("已配置 AI：" + Prefs.model(this)) : "未配置 AI（请先进入设置完成配置）");
    }

    private boolean storageGranted() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean overlayGranted() {
        return Settings.canDrawOverlays(this);
    }

    private boolean accessibilityGranted() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String comp = new ComponentName(this, GaiAccessibilityService.class).flattenToString();
        return enabled.contains(comp);
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    } catch (Exception e2) {
                        Toast.makeText(this, "请在系统设置中手动开启「所有文件访问」权限", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else {
            requestPermissions(new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    private void requestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                } catch (Exception e2) {
                    Toast.makeText(this, "请在系统设置中手动开启「显示在其他应用上层」权限", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void requestAccessibility() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "请在系统设置-无障碍中开启 G-ai 无障碍服务", Toast.LENGTH_LONG).show();
        }
    }

    private void maybeRequestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void doStart() {
        if (!overlayGranted()) {
            requestOverlay();
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }
        if (Prefs.url(this).length() == 0 || Prefs.key(this).length() == 0 || Prefs.model(this).length() == 0) {
            Toast.makeText(this, "请先配置 AI 模型（接口 / Key / 模型名称）", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        maybeRequestNotifPermission();
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(new Intent(this, FloatingWindowService.class));
        } else {
            startService(new Intent(this, FloatingWindowService.class));
        }
        Toast.makeText(this, "G-ai 已启动，悬浮窗已显示", Toast.LENGTH_SHORT).show();
    }

    private void showLicense() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("G-ai 许可协议（MIT License）");
        b.setMessage(MIT + "\n\n版本：v" + Prefs.VERSION + "\n\n同意后即可使用 G-ai：配置自定义 AI、悬浮窗、连接我的世界、AI 分身建造与操控、苍天旗与五星红旗。");
        b.setCancelable(false);
        b.setPositiveButton("同意并继续", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                Prefs.setAgreed(MainActivity.this);
                maybeRequestNotifPermission();
                requestStorage();
                Toast.makeText(MainActivity.this, "欢迎使用 G-ai！请依次完成下方权限授权", Toast.LENGTH_LONG).show();
            }
        });
        b.setNegativeButton("不同意", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { finish(); }
        });
        AlertDialog d = b.create();
        d.show();
        TextView tv = (TextView) d.findViewById(android.R.id.message);
        if (tv != null) tv.setMovementMethod(new ScrollingMovementMethod());
    }
}
