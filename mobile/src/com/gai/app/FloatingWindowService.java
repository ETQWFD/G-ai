package com.gai.app;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 悬浮窗服务 v1.0.0：双窗口（可拖动图标 + 独立可输入面板）。
 * 修复：菜单可反复打开/关闭、拖动图标自动收起菜单、面板标题栏可拖动、
 * 所有回调全量防崩溃、输入框强制弹键盘、AI 玩家意识+记忆、AI 自由活动、连接后不闪退。
 */
public class FloatingWindowService extends Service {

    private static final int NOTIF_ID = 1;
    public static volatile boolean gameConnected;
    private static volatile boolean announcedHero; // 是否已广播苍天会宣言

    private WindowManager wm;
    private ImageView iconView;
    private View panelView;
    private WindowManager.LayoutParams iconParams, panelParams;
    private boolean panelShown;
    private boolean panelDragging; // 面板是否在拖动（防止拖动时误触发子控件）

    private TextView wsStatus, controlTimer;
    private ToggleButton btnConnect, btnBuild, btnControl;
    private EditText etPrompt;

    private WsServer ws;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final ExecutorService aiExec = Executors.newSingleThreadExecutor();
    private volatile boolean buildOn;
    private volatile boolean controlOn;
    private volatile boolean loopRunning;
    private volatile boolean aiBusy;
    private int controlRemain = 240;
    private Runnable controlTick;
    private Runnable autoLifeTick;
    private long lastAutoLife = 0;
    private volatile boolean bodyExists; // AI 本体是否已降临

    private float rawX, rawY, startX, startY;
    private boolean dragging;

    public static boolean isGameConnected() { return gameConnected; }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        // 全量防崩溃：任何未捕获异常都记录并吞掉，保证悬浮窗永不闪退
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    Notify.event(FloatingWindowService.this, "G-ai",
                            "检测到异常已自动防护：" + safeMsg(e));
                } catch (Throwable ignored) { /* ignore */ }
            }
        });
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Notify.ensureChannels(this);
        try { buildIconWindow(); } catch (Throwable t) { /* 图标构建失败不致命 */ }
        startForegroundCompat();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopAll();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    // ---------- 图标窗口（可拖动） ----------

    private void buildIconWindow() {
        int type = overlayType();
        iconParams = new WindowManager.LayoutParams(
                dp(56), dp(56), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 60;
        iconParams.y = 200;

        iconView = new ImageView(this);
        iconView.setImageResource(R.mipmap.ic_launcher);
        wm.addView(iconView, iconParams);

        iconView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        rawX = e.getRawX();
                        rawY = e.getRawY();
                        startX = rawX;
                        startY = rawY;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - rawX;
                        float dy = e.getRawY() - rawY;
                        if (Math.abs(e.getRawX() - startX) + Math.abs(e.getRawY() - startY) > 12) {
                            if (!dragging) {
                                // 开始拖动：自动收起悬浮菜单
                                dragging = true;
                                if (panelShown) hidePanel();
                            }
                        }
                        if (dragging) {
                            iconParams.x += (int) dx;
                            iconParams.y += (int) dy;
                            rawX = e.getRawX();
                            rawY = e.getRawY();
                            try { wm.updateViewLayout(iconView, iconParams); } catch (Exception ex) { /* ignore */ }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!dragging) togglePanel();
                        dragging = false;
                        return true;
                }
                return false;
            }
        });
    }

    // ---------- 面板窗口（独立、可输入、可拖动） ----------

    private void togglePanel() {
        try {
            if (panelShown) hidePanel();
            else showPanel();
        } catch (Throwable t) { /* ignore */ }
    }

    private void showPanel() {
        try {
            if (panelShown) return;
            if (panelView == null) {
                panelView = LayoutInflater.from(this).inflate(R.layout.float_panel, null);
                bindPanel();
            }
            if (panelParams == null) {
                panelParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType(),
                        // 不设 NOT_FOCUSABLE：允许输入框调起系统键盘
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);
                panelParams.gravity = Gravity.TOP | Gravity.START;
            }
            movePanelNearIcon();
            if (!panelView.isAttachedToWindow()) {
                wm.addView(panelView, panelParams);
            } else {
                wm.updateViewLayout(panelView, panelParams);
            }
            panelShown = true;
            refreshPanelStatus();
            // 自动弹出键盘
            try {
                if (etPrompt != null) {
                    etPrompt.requestFocus();
                    panelView.postDelayed(new Runnable() {
                        public void run() {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null && etPrompt != null) {
                                imm.showSoftInput(etPrompt, InputMethodManager.SHOW_FORCED);
                            }
                        }
                    }, 350);
                }
            } catch (Throwable t) { /* ignore */ }
        } catch (Throwable t) { /* ignore */ }
    }

    private void hidePanel() {
        try {
            if (!panelShown) return;
            hideKeyboard();
            if (panelView != null && panelView.isAttachedToWindow()) {
                wm.removeView(panelView);
            }
            panelShown = false;
        } catch (Throwable t) { /* ignore */ }
    }

    private void movePanelNearIcon() {
        if (panelParams == null || iconParams == null) return;
        panelParams.x = iconParams.x;
        panelParams.y = iconParams.y + dp(60);
        try {
            if (panelShown && panelView != null && panelView.isAttachedToWindow()) {
                wm.updateViewLayout(panelView, panelParams);
            }
        } catch (Exception e) { /* ignore */ }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && panelView != null) {
                imm.hideSoftInputFromWindow(panelView.getWindowToken(), 0);
            }
        } catch (Exception e) { /* ignore */ }
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void bindPanel() {
        wsStatus = (TextView) panelView.findViewById(R.id.wsStatus);
        controlTimer = (TextView) panelView.findViewById(R.id.controlTimer);
        btnConnect = (ToggleButton) panelView.findViewById(R.id.btnConnect);
        btnBuild = (ToggleButton) panelView.findViewById(R.id.btnBuild);
        btnControl = (ToggleButton) panelView.findViewById(R.id.btnControl);
        etPrompt = (EditText) panelView.findViewById(R.id.etPrompt);

        ((TextView) panelView.findViewById(R.id.panelVersion)).setText("v1.0.0");

        // 标题栏拖动面板
        panelView.findViewById(R.id.panelHeader).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        rawX = e.getRawX();
                        rawY = e.getRawY();
                        startX = rawX;
                        startY = rawY;
                        panelDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - rawX;
                        float dy = e.getRawY() - rawY;
                        if (Math.abs(e.getRawX() - startX) + Math.abs(e.getRawY() - startY) > 10) {
                            panelDragging = true;
                        }
                        if (panelDragging) {
                            panelParams.x += (int) dx;
                            panelParams.y += (int) dy;
                            rawX = e.getRawX();
                            rawY = e.getRawY();
                            iconParams.x = panelParams.x;
                            iconParams.y = panelParams.y - dp(60);
                            try {
                                wm.updateViewLayout(panelView, panelParams);
                                wm.updateViewLayout(iconView, iconParams);
                            } catch (Exception ex) { /* ignore */ }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        boolean wasDrag = panelDragging;
                        panelDragging = false;
                        return wasDrag; // 拖动过则消费，否则放行给子控件
                }
                return false;
            }
        });

        panelView.findViewById(R.id.btnCollapse).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { hidePanel(); } // 只收起，点图标可再次打开
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (btnConnect.isChecked()) startServer();
                    else stopServer();
                } catch (Throwable t) { toastSafe("连接操作失败：" + safeMsg(t)); }
            }
        });

        // 复制连接命令
        panelView.findViewById(R.id.btnCopyCmd).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { try { copyConnectCmd(); } catch (Throwable t) { toastSafe("复制失败：" + safeMsg(t)); } }
        });

        btnBuild.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    buildOn = btnBuild.isChecked();
                    if (buildOn) {
                        if (!gameConnected) {
                            toastSafe("请先「连接游戏」");
                            btnBuild.setChecked(false);
                            buildOn = false;
                            return;
                        }
                        Notify.event(FloatingWindowService.this, "G-ai", "AI 建造已开启");
                        ensureLoop();
                    } else {
                        Notify.event(FloatingWindowService.this, "G-ai", "AI 建造已停止");
                    }
                } catch (Throwable t) { toastSafe("操作失败：" + safeMsg(t)); }
            }
        });

        btnControl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (btnControl.isChecked()) {
                        if (!gameConnected) {
                            toastSafe("请先「连接游戏」");
                            btnControl.setChecked(false);
                            return;
                        }
                        startControl();
                    } else {
                        stopControl();
                    }
                } catch (Throwable t) { toastSafe("操作失败：" + safeMsg(t)); }
            }
        });

        panelView.findViewById(R.id.btnImport).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent(FloatingWindowService.this, ImportActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    toastSafe("无法打开文件选择器: " + safeMsg(e));
                }
            }
        });

        panelView.findViewById(R.id.btnHelp).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { try { showHelp(); } catch (Throwable t) { toastSafe("帮助打开失败：" + safeMsg(t)); } }
        });

        panelView.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { try { sendPrompt(); } catch (Throwable t) { toastSafe("发送失败：" + safeMsg(t)); } }
        });

        // 点击输入框强制弹键盘
        if (etPrompt != null) {
            etPrompt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(etPrompt, InputMethodManager.SHOW_FORCED);
                    } catch (Throwable t) { /* ignore */ }
                }
            });
        }

        // 键盘回车发送
        etPrompt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                        || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                        && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                    try { sendPrompt(); } catch (Throwable t) { toastSafe("发送失败：" + safeMsg(t)); }
                    return true;
                }
                return false;
            }
        });
    }

    private void toastSafe(String s) {
        try { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); } catch (Throwable t) { /* ignore */ }
    }

    private void refreshPanelStatus() {
        if (wsStatus == null) return;
        try {
            if (ws == null || !ws.isRunning()) {
                wsStatus.setText("游戏连接：未开启（点「连接游戏」，再点「复制连接命令」粘贴到游戏聊天框）");
            } else if (gameConnected) {
                wsStatus.setText("已连接 · AI 分身" + (bodyExists ? "（本体已降临）" : "") + "工作中（聊天框 @AI 可对话）");
            } else {
                wsStatus.setText("连接服务已启动，等待游戏连接… 游戏聊天框输入 /connect 本机IP:" + Prefs.port(this));
            }
        } catch (Throwable t) { /* ignore */ }
    }

    // ---------- 复制连接命令 ----------

    private void copyConnectCmd() {
        String ip = getLanIp();
        String cmd = "/connect " + ip + ":" + Prefs.port(this);
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("G-ai 连接命令", cmd));
            Toast.makeText(this, "已复制连接命令：" + cmd, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "复制失败：" + safeMsg(e), Toast.LENGTH_LONG).show();
        }
    }

    private String getLanIp() {
        try {
            android.net.wifi.WifiManager w = (android.net.wifi.WifiManager) getApplicationContext()
                    .getSystemService(WIFI_SERVICE);
            if (w != null && w.getConnectionInfo() != null) {
                int a = w.getConnectionInfo().getIpAddress();
                if (a != 0) {
                    return (a & 0xFF) + "." + ((a >> 8) & 0xFF) + "." + ((a >> 16) & 0xFF) + "." + ((a >> 24) & 0xFF);
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "127.0.0.1";
    }

    // ---------- 游戏连接（WebSocket） ----------

    private void startServer() {
        if (ws != null && ws.isRunning()) return;
        final int port = Prefs.port(this);
        ws = new WsServer(port, new WsServer.Listener() {
            public void onStatus(final String s) {
                h.post(new Runnable() { public void run() { if (wsStatus != null) wsStatus.setText(s); } });
            }

            public void onConnected() {
                h.post(new Runnable() { public void run() { try { onGameConnected(); } catch (Throwable t) { toastSafe("连接处理异常：" + safeMsg(t)); } } });
            }

            public void onDisconnected() {
                h.post(new Runnable() {
                    public void run() {
                        try {
                            gameConnected = false;
                            bodyExists = false;
                            Prefs.setConnected(FloatingWindowService.this, false);
                            if (wsStatus != null) wsStatus.setText("游戏已断开，等待重新连接…");
                            Notify.event(FloatingWindowService.this, "G-ai", "游戏连接已断开");
                        } catch (Throwable t) { /* ignore */ }
                    }
                });
            }

            public void onCommandResponse(final String cmd, final int code, final String msg) {
                h.post(new Runnable() { public void run() { try { handleCommandResult(cmd, code, msg); } catch (Throwable t) { toastSafe("命令回调异常：" + safeMsg(t)); } } });
            }

            public void onPlayerMessage(final String sender, final String message) {
                h.post(new Runnable() { public void run() { try { handlePlayerMessage(sender, message); } catch (Throwable t) { toastSafe("消息处理异常：" + safeMsg(t)); } } });
            }

            public void onError(final String err) {
                h.post(new Runnable() { public void run() { if (wsStatus != null) wsStatus.setText(err); } });
            }
        });
        try {
            ws.start();
            gameConnected = false;
            bodyExists = false;
            Notify.event(this, "G-ai", "连接服务已启动，端口 " + port);
            Toast.makeText(this, "连接服务已启动\n「复制连接命令」粘贴到游戏聊天框即可", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            try { btnConnect.setChecked(false); } catch (Throwable t) { /* ignore */ }
            Toast.makeText(this, "启动连接服务失败: " + safeMsg(e), Toast.LENGTH_LONG).show();
        }
    }

    private void stopServer() {
        if (ws != null) ws.stop();
        gameConnected = false;
        bodyExists = false;
        Prefs.setConnected(this, false);
        try {
            if (wsStatus != null) wsStatus.setText("游戏连接：未开启");
            if (btnConnect != null) btnConnect.setChecked(false);
        } catch (Throwable t) { /* ignore */ }
    }

    private void onGameConnected() {
        gameConnected = true;
        bodyExists = false;
        announcedHero = false;
        Prefs.setConnected(this, true);
        if (wsStatus != null) wsStatus.setText("已连接游戏！正在为 AI 创建本体…");
        Notify.event(this, "G-ai", "已连接我的世界，AI 分身进入中…");
        sendCmdSafe("/say [G-ai] AI分身已进入世界");
        sendCmdSafe("/summon gai:ai_body ~ ~1 ~");
        sendCmdSafe("/setblock ~ ~ ~ gai:ai_core");
        scheduleAutoLife();
    }

    private void sendCmdSafe(String cmd) {
        try {
            if (ws != null) ws.sendCommand(cmd);
        } catch (Throwable t) { /* ignore */ }
    }

    private void handleCommandResult(String cmd, int code, String msg) {
        if (cmd == null) return;
        if (cmd.contains("summon")) {
            if (code == 0) {
                bodyExists = true;
                if (!announcedHero) {
                    announcedHero = true;
                    sendCmdSafe("/say [苍天会] 苍天有眼！我乃苍天会");
                    sendCmdSafe("/say [G-ai] AI本体（HIM分身）已降临！我是苍天会的玩家，陪我一起玩吧——@AI 可以和我聊天，AI建造/AI操控可以让我干活");
                }
                if (wsStatus != null) wsStatus.setText("AI 本体已降临（HIM 分身）！可用 AI 建造 / AI 操控 / @AI 聊天");
                Notify.event(this, "G-ai", "AI 本体（HIM 分身）已降临");
                scheduleAutoLife();
            } else {
                bodyExists = false;
                if (wsStatus != null) wsStatus.setText("未检测到 G-ai 资源包，AI 以纯命令模式工作（无本体）");
                Notify.event(this, "G-ai", "提示：安装 G-ai 资源包后 AI 才有本体，可自主建造");
            }
        } else {
            if (wsStatus != null) wsStatus.setText("命令: " + cmd + " → " + (code == 0 ? "执行成功" : "执行失败 " + msg));
        }
    }

    // ---------- AI 聊天（游戏聊天框 @AI） ----------

    private void handlePlayerMessage(String sender, String message) {
        String m = message == null ? "" : message.trim();
        if (m.isEmpty() || !gameConnected) return;
        boolean mention = m.contains("@AI") || m.contains("@ai")
                || m.contains("苍天") || m.contains("AI：") || m.contains("AI:")
                || m.contains("AI，") || m.contains("AI,");
        if (!mention) return;
        if (aiBusy) {
            sendCmdSafe("/say [G-ai] 我正在忙，稍等片刻再 @AI");
            return;
        }
        if (wsStatus != null) wsStatus.setText("AI 聊天中：收到 " + sender + " 的消息");
        aiBusy = true;
        final String question = m;
        aiExec.execute(new Runnable() {
            public void run() {
                try {
                    List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                    if (Prefs.aiAware(FloatingWindowService.this)) {
                        msgs.add(AiClient.msg("system", getString(R.string.chat_prompt)));
                        List<Map<String, Object>> mem = Memory.load(FloatingWindowService.this);
                        if (!mem.isEmpty()) {
                            msgs.add(AiClient.msg("system", "你的记忆（之前发生过的对话）：" + prettyMemory(mem)));
                        }
                    } else {
                        msgs.add(AiClient.msg("system",
                                "你是我的世界中的 AI 分身 G-ai，身份是苍天会的使者。用简体中文、口语化、简短地回复玩家聊天。"));
                    }
                    msgs.add(AiClient.msg("user", "玩家「" + sender + "」对你说：" + question));
                    String reply = AiClient.chat(Prefs.url(FloatingWindowService.this),
                            Prefs.key(FloatingWindowService.this),
                            Prefs.model(FloatingWindowService.this), msgs, 0.9);
                    aiBusy = false;
                    final String say = sanitizeChat(reply);
                    Memory.remember(FloatingWindowService.this, question, say);
                    h.post(new Runnable() {
                        public void run() {
                            if (ws != null && ws.isConnected()) {
                                sendCmdSafe("/say [G-ai] " + say);
                            }
                            if (wsStatus != null) wsStatus.setText("AI 已回复：" + say);
                        }
                    });
                } catch (final Exception e) {
                    aiBusy = false;
                    h.post(new Runnable() {
                        public void run() {
                            if (ws != null && ws.isConnected()) {
                                sendCmdSafe("/say [G-ai] 我刚刚走神了，网络有点卡，稍后再 @AI");
                            }
                            if (wsStatus != null) wsStatus.setText("AI 聊天失败：" + safeMsg(e));
                        }
                    });
                }
            }
        });
    }

    private static String prettyMemory(List<Map<String, Object>> mem) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> m : mem) {
            String role = Json.gs(m, "role");
            String content = Json.gs(m, "content");
            if (content == null) continue;
            if (sb.length() > 0) sb.append("；");
            if ("user".equals(role)) sb.append("玩家说过：").append(content);
            else sb.append("你回应过：").append(content);
        }
        String s = sb.toString();
        return s.length() > 900 ? s.substring(0, 900) : s;
    }

    private static String sanitizeChat(String s) {
        if (s == null) return "…";
        s = s.replace('\n', ' ').replace('\r', ' ').trim();
        if (s.startsWith("/say")) s = s.substring(4).trim();
        if (s.length() > 180) s = s.substring(0, 180);
        return s;
    }

    // ---------- AI 自由活动（有本体时自主陪玩，绝不毁服） ----------

    private void scheduleAutoLife() {
        try {
            if (autoLifeTick != null) h.removeCallbacks(autoLifeTick);
            autoLifeTick = new Runnable() {
                public void run() {
                    autoLifeTick = null;
                    try {
                        if (gameConnected && bodyExists && !aiBusy && !buildOn && !controlOn) {
                            long now = System.currentTimeMillis();
                            if (now - lastAutoLife >= 90000) {
                                lastAutoLife = now;
                                aiBusy = true;
                                aiExec.execute(new Runnable() {
                                    public void run() {
                                        try {
                                            List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                                            msgs.add(AiClient.msg("system", getString(R.string.chat_prompt)));
                                            List<Map<String, Object>> mem = Memory.load(FloatingWindowService.this);
                                            if (!mem.isEmpty()) {
                                                msgs.add(AiClient.msg("system", "你的记忆：" + prettyMemory(mem)));
                                            }
                                            msgs.add(AiClient.msg("user",
                                                    "你现在在游戏世界里（AI 本体已降临），玩家就在附近。自由发挥一下："
                                                            + "说一句符合你性格的话，或者做一件小事（比如 /emote 一个动作、发 /say 问候、在空地上放一个好看的方块当装饰）。"
                                                            + "规则：禁止 /kill、禁止破坏他人建筑、禁止刷物品、禁止大面积 /fill 清除。一次只做一件小事，简短。"));
                                            String reply = AiClient.chat(Prefs.url(FloatingWindowService.this),
                                                    Prefs.key(FloatingWindowService.this),
                                                    Prefs.model(FloatingWindowService.this), msgs, 1.0);
                                            aiBusy = false;
                                            final String act = sanitizeChat(reply);
                                            Memory.remember(FloatingWindowService.this, "（AI 自由活动）", act);
                                            h.post(new Runnable() {
                                                public void run() {
                                                    if (ws != null && ws.isConnected()) {
                                                        if (act.startsWith("/")) {
                                                            sendCmdSafe(act);
                                                        } else {
                                                            sendCmdSafe("/say [G-ai] " + act);
                                                        }
                                                    }
                                                }
                                            });
                                        } catch (final Exception e) {
                                            aiBusy = false;
                                        }
                                    }
                                });
                            }
                        }
                    } catch (Throwable t) { /* ignore */ }
                    // 每 30 秒检查一次
                    h.postDelayed(this, 30000);
                }
            };
            h.postDelayed(autoLifeTick, 30000);
        } catch (Throwable t) { /* ignore */ }
    }

    // ---------- AI 建造 / AI 操控 ----------

    private void startControl() {
        controlOn = true;
        controlRemain = 240;
        if (controlTimer != null) {
            controlTimer.setVisibility(View.VISIBLE);
            updateTimerText();
        }
        Notify.event(this, "G-ai", "AI 操控已开启，4 分钟后自动交还控制权");
        sendCmdSafe("/say [G-ai] AI开始操控，4分钟后自动交还控制权");
        ensureLoop();
        controlTick = new Runnable() {
            public void run() {
                controlRemain--;
                updateTimerText();
                if (controlRemain <= 0) {
                    stopControl();
                } else {
                    h.postDelayed(this, 1000);
                }
            }
        };
        h.postDelayed(controlTick, 1000);
    }

    private void stopControl() {
        controlOn = false;
        try {
            if (btnControl != null) btnControl.setChecked(false);
            h.removeCallbacks(controlTick);
            if (controlTimer != null) controlTimer.setVisibility(View.GONE);
        } catch (Throwable t) { /* ignore */ }
        if (ws != null && ws.isConnected()) {
            sendCmdSafe("/say [G-ai] AI操控结束，控制权已交还用户");
        }
        if (wsStatus != null) wsStatus.setText("AI 操控结束，控制权已交还用户");
        Notify.event(this, "AI 操控结束", "4 分钟操控时间已到，控制权已交还用户");
    }

    private void updateTimerText() {
        try {
            if (controlTimer == null) return;
            int m = controlRemain / 60;
            int s = controlRemain % 60;
            controlTimer.setText("AI 操控剩余：" + (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s) + "（到点自动交还控制权）");
        } catch (Throwable t) { /* ignore */ }
    }

    private void ensureLoop() {
        if (loopRunning) return;
        loopRunning = true;
        aiExec.execute(new Runnable() {
            public void run() {
                while (buildOn || controlOn) {
                    try {
                        boolean ctrl = controlOn;
                        String prompt = ctrl
                                ? "你现在处于AI操控模式。请输出下一条要在我的世界游戏中执行的命令，仅输出命令本身，每行一条，以/开头。记住：禁止毁服（禁止/kill @e、禁止大范围/fill清除、禁止破坏他人建筑）。"
                                : "继续AI建造任务。请输出下一条要在游戏中执行的命令，仅输出命令本身，每行一条，以/开头。若任务已完成，输出 /say 建造完成。禁止毁服（禁止/kill @e、禁止破坏他人建筑、禁止刷物品）。";
                        aiBusy = true;
                        List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                        if (Prefs.aiAware(FloatingWindowService.this)) {
                            msgs.add(AiClient.msg("system", getString(R.string.system_prompt)));
                            List<Map<String, Object>> mem = Memory.load(FloatingWindowService.this);
                            if (!mem.isEmpty()) {
                                msgs.add(AiClient.msg("system", "你的记忆（之前发生过的对话）：" + prettyMemory(mem)));
                            }
                        } else {
                            msgs.add(AiClient.msg("system", getString(R.string.system_prompt)));
                        }
                        msgs.add(AiClient.msg("user", prompt));
                        String reply = AiClient.chat(Prefs.url(FloatingWindowService.this),
                                Prefs.key(FloatingWindowService.this),
                                Prefs.model(FloatingWindowService.this), msgs, 0.7);
                        aiBusy = false;
                        final List<String> cmds = parseCommands(reply);
                        h.post(new Runnable() {
                            public void run() {
                                if (wsStatus != null) {
                                    wsStatus.setText("AI " + (controlOn ? "操控" : "建造") + "中：已生成 " + cmds.size() + " 条命令");
                                }
                            }
                        });
                        for (String cmd : cmds) {
                            if (!buildOn && !controlOn) break;
                            sendCmdSafe(cmd);
                            try { Thread.sleep(700); } catch (InterruptedException e) { break; }
                        }
                        try { Thread.sleep(3500); } catch (InterruptedException e) { break; }
                    } catch (final Exception e) {
                        aiBusy = false;
                        h.post(new Runnable() {
                            public void run() {
                                if (wsStatus != null) wsStatus.setText("AI 请求失败：" + safeMsg(e));
                            }
                        });
                        try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
                    }
                }
                loopRunning = false;
            }
        });
    }

    private void sendPrompt() {
        final String prompt = etPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            toastSafe("请输入要 AI 执行的任务");
            return;
        }
        if (!gameConnected || ws == null || !ws.isConnected()) {
            toastSafe("请先「连接游戏」并确认已连接");
            return;
        }
        if (aiBusy) {
            toastSafe("AI 正在执行其他任务，请稍候");
            return;
        }
        etPrompt.setText("");
        hideKeyboard();
        if (wsStatus != null) wsStatus.setText("AI 思考中…");
        aiBusy = true;
        aiExec.execute(new Runnable() {
            public void run() {
                try {
                    List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                    if (Prefs.aiAware(FloatingWindowService.this)) {
                        msgs.add(AiClient.msg("system", getString(R.string.system_prompt)));
                        List<Map<String, Object>> mem = Memory.load(FloatingWindowService.this);
                        if (!mem.isEmpty()) {
                            msgs.add(AiClient.msg("system", "你的记忆（之前发生过的对话）：" + prettyMemory(mem)));
                        }
                    } else {
                        msgs.add(AiClient.msg("system", getString(R.string.system_prompt)));
                    }
                    msgs.add(AiClient.msg("user", prompt));
                    String reply = AiClient.chat(Prefs.url(FloatingWindowService.this),
                            Prefs.key(FloatingWindowService.this),
                            Prefs.model(FloatingWindowService.this), msgs, 0.7);
                    aiBusy = false;
                    final List<String> cmds = parseCommands(reply);
                    Memory.remember(FloatingWindowService.this, prompt, cmds.isEmpty() ? "（没有可执行命令）" : "执行了命令：" + join(cmds));
                    h.post(new Runnable() {
                        public void run() {
                            if (wsStatus != null) {
                                if (cmds.isEmpty()) {
                                    wsStatus.setText("AI 未返回可执行命令");
                                } else {
                                    wsStatus.setText("AI 已生成 " + cmds.size() + " 条命令，正在执行…");
                                }
                            }
                        }
                    });
                    for (String cmd : cmds) {
                        if (ws != null && ws.isConnected()) sendCmdSafe(cmd);
                        try { Thread.sleep(700); } catch (InterruptedException e) { break; }
                    }
                } catch (final Exception e) {
                    aiBusy = false;
                    h.post(new Runnable() {
                        public void run() {
                            if (wsStatus != null) wsStatus.setText("AI 请求失败：" + safeMsg(e));
                        }
                    });
                }
            }
        });
    }

    private static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size() && i < 3; i++) {
            if (i > 0) sb.append(" ");
            sb.append(l.get(i));
        }
        if (l.size() > 3) sb.append(" 等").append(l.size()).append("条");
        return sb.toString();
    }

    static List<String> parseCommands(String reply) {
        List<String> out = new ArrayList<String>();
        if (reply == null) return out;
        for (String line : reply.split("\n")) {
            String t = line.trim();
            if (t.startsWith("```")) continue;
            if (t.startsWith("/")) out.add(t);
        }
        return out;
    }

    private static String safeMsg(Throwable t) {
        if (t == null) return "未知错误";
        String m = t.getMessage();
        return (m == null || m.isEmpty()) ? t.getClass().getSimpleName() : m;
    }

    // ---------- 帮助 ----------

    private void showHelp() {
        final int port = Prefs.port(this);
        String ip = getLanIp();
        String text =
                "【1. 连接游戏】\n" +
                "· 点「连接游戏」开启连接服务（端口 " + port + "）\n" +
                "· 点「复制连接命令」，到游戏聊天框粘贴发送：\n" +
                "  /connect " + ip + ":" + port + "\n" +
                "· 看到「AI分身已进入世界」即连接成功；安装 G-ai 行为包+资源包后 AI 会以 HIM 本体降临。\n\n" +
                "【2. 给 AI 下指令】\n" +
                "在下方输入框输入任务（如：帮我建一座小木屋），点「发送给 AI 执行」，AI 会生成命令并在游戏内真实执行。\n\n" +
                "【3. AI 建造 / AI 操控】\n" +
                "建造：AI 持续生成建造命令并执行。\n" +
                "操控：AI 接管游戏操作约 4 分钟，到点自动交还控制权。\n\n" +
                "【4. AI 聊天】\n" +
                "连接后在游戏聊天框输入 @AI 开头（或带“苍天”）的消息，AI 会在聊天框回复你。\n\n" +
                "【5. 导入结构】\n" +
                "选择 .mcstructure 文件导入游戏世界目录，游戏中用 /structure load 文件名 加载。\n\n" +
                "【6. 旗帜】\n" +
                "安装 G-ai 行为包+资源包后：苍天旗（灰色混凝土+2木棍+黑色混凝土）、五星红旗（红色混凝土+木棍+黄色混凝土）可在工作台合成并插地。\n\n" +
                "【7. 后台运行】\n" +
                "G-ai 常驻后台（任务栏可见），只有点「停止运行」才会退出。\n\n" +
                "版本 v1.0.0 · MIT License";
        new AlertDialog.Builder(this)
                .setTitle("AI 帮助 · 连接教程")
                .setMessage(text)
                .setPositiveButton("知道了", null)
                .show();
    }

    // ---------- 生命周期 ----------

    private void startForegroundCompat() {
        String model = Prefs.model(this);
        NotificationCompat(NOTIF_ID, Notify.running(this,
                "悬浮窗运行中 · 模型 " + (model.isEmpty() ? "未配置" : model)));
    }

    private void NotificationCompat(int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(id, n);
        }
    }

    private void stopAll() {
        buildOn = false;
        controlOn = false;
        h.removeCallbacks(controlTick);
        h.removeCallbacks(autoLifeTick);
        if (ws != null) ws.stop();
        gameConnected = false;
        Prefs.setConnected(this, false);
        try { if (panelView != null) wm.removeView(panelView); } catch (Exception e) { /* ignore */ }
        try { if (iconView != null) wm.removeView(iconView); } catch (Exception e) { /* ignore */ }
        panelView = null;
        iconView = null;
        panelShown = false;
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        buildOn = false;
        controlOn = false;
        h.removeCallbacks(controlTick);
        h.removeCallbacks(autoLifeTick);
        if (ws != null) ws.stop();
        gameConnected = false;
        try {
            if (panelView != null && panelView.isAttachedToWindow()) wm.removeView(panelView);
        } catch (Exception e) { /* ignore */ }
        try {
            if (iconView != null && iconView.isAttachedToWindow()) wm.removeView(iconView);
        } catch (Exception e) { /* ignore */ }
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
