package com.gai.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * 无障碍服务：检测我的世界是否在前台。
 * 连接服务开启时，游戏打开会收到连接提示，方便用户直接输入 /connect。
 */
public class GaiAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            if (pkg != null && pkg.toString().equals("com.mojang.minecraftpe")) {
                if (FloatingWindowService.isGameConnected()) {
                    Notify.event(this, "G-ai",
                            "检测到我的世界已打开：在聊天框输入 /connect 127.0.0.1:" + Prefs.port(this) + " 连接 AI 分身");
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Notify.event(this, "G-ai", "无障碍服务已就绪");
    }
}
