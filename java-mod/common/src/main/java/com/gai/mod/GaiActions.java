package com.gai.mod;

import com.gai.mod.util.AiClient;
import com.gai.mod.util.Json;
import com.gai.mod.util.WsServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * G-ai 模组核心动作：WebSocket 连接、AI 本体降临、AI 聊天、AI 执行命令、
 * 导入光影、导入结构、苍天会宣言。
 */
public final class GaiActions {

    private static MinecraftServer server;
    private static WsServer ws;
    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "gai-worker");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean AI_BUSY = new AtomicBoolean(false);
    private static final AtomicBoolean AI_CONTROL = new AtomicBoolean(false);
    private static java.util.concurrent.ScheduledFuture<?> CONTROL_TASK;
    private static boolean heroAnnounced = false;

    private GaiActions() {}

    // ---------- 生命周期 ----------

    public static void onServerStart(MinecraftServer s) {
        server = s;
        heroAnnounced = false;
        AI_CONTROL.set(false);
        if (CONTROL_TASK != null) { try { CONTROL_TASK.cancel(true); } catch (Throwable t) {} CONTROL_TASK = null; }
        log("[G-ai] 模组已加载 v1.1.0 · 聊天框发送 @AI 可与我对话 · 按 G 打开悬浮控制台");
        broadcast("§7[G-ai] §f模组已加载 v1.1.0，输入 §e/gai help §f查看帮助；电脑版按 §eG §f键打开悬浮控制台");
    }

    public static void onServerStop() {
        stopWs();
        if (CONTROL_TASK != null) { try { CONTROL_TASK.cancel(true); } catch (Throwable t) {} CONTROL_TASK = null; }
        AI_CONTROL.set(false);
        server = null;
    }

    // ---------- 广播 ----------

    public static void broadcast(String text) {
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
    }

    private static void log(String text) {
        try {
            org.slf4j.LoggerFactory.getLogger("G-ai").info(text.replaceAll("§[0-9a-fk-or]", ""));
        } catch (Throwable t) { /* ignore */ }
    }

    // ---------- WebSocket 连接服务 ----------

    public static void startWs() throws IOException {
        if (ws != null && ws.isRunning()) {
            broadcast("§e[G-ai] 连接服务已在运行");
            return;
        }
        final int port = GaiConfig.port();
        ws = new WsServer(port, new WsServer.Listener() {
            @Override public void onStatus(String status) { log("[WS] " + status); }
            @Override public void onConnected() {
                broadcast("§a[G-ai] 已收到外部连接，AI 分身进入世界…");
                spawnAvatar();
                broadcast("§a[苍天会] §f苍天有眼！我乃苍天会");
                broadcast("§a[G-ai] AI本体（HIM分身）已降临，可自主建造；聊天框 @AI 可与我对话");
            }
            @Override public void onDisconnected() { log("[WS] 连接断开"); }
            @Override public void onCommandResponse(String commandLine, int statusCode, String statusMessage) {
                onClientCommand(commandLine);
            }
            @Override public void onPlayerMessage(String sender, String message) {
                handleChat(sender, message);
            }
            @Override public void onError(String err) { log("[WS] 错误: " + err); }
        });
        ws.start();
        broadcast("§a[G-ai] 连接服务已启动：ws://" + lanIp() + ":" + port + "（外部软件/手机端可连接下发命令）");
    }

    public static void stopWs() {
        if (ws != null) {
            ws.stop();
            ws = null;
        }
        broadcast("§7[G-ai] 连接服务已停止");
    }

    public static boolean wsRunning() {
        return ws != null && ws.isRunning();
    }

    private static String lanIp() {
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && ni.isUp()) {
                    for (java.net.InetAddress a : java.util.Collections.list(ni.getInetAddresses())) {
                        if (a instanceof java.net.Inet4Address) return a.getHostAddress();
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "127.0.0.1";
    }

    /** 外部客户端下发命令：在服务端以 op 权限执行，并回执结果。 */
    private static void onClientCommand(String commandLine) {
        if (server == null || commandLine == null || commandLine.isEmpty()) return;
        String cmd = commandLine.startsWith("/") ? commandLine : "/" + commandLine;
        try {
            CommandSourceStack src = server.createCommandSourceStack()
                    .withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS)
                    .withSuppressedOutput();
            server.getCommands().performPrefixedCommand(src, cmd);
            sendResponse(commandLine, 0);
            log("[WS] 执行命令: " + cmd);
        } catch (Exception e) {
            sendResponse(commandLine, -1);
            log("[WS] 执行失败: " + e.getMessage());
        }
    }

    private static void sendResponse(String commandLine, int code) {
        if (ws == null) return;
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        Map<String, Object> h = new LinkedHashMap<String, Object>();
        h.put("version", 1L);
        h.put("messagePurpose", "commandResponse");
        h.put("requestId", java.util.UUID.randomUUID().toString());
        root.put("header", h);
        Map<String, Object> b = new LinkedHashMap<String, Object>();
        b.put("commandLine", commandLine);
        b.put("statusCode", code == 0 ? 0 : 1);
        b.put("statusMessage", code == 0 ? "命令执行成功" : "命令执行失败(" + code + ")");
        root.put("body", b);
        ws.sendText(Json.stringify(root));
    }

    // ---------- AI 本体 ----------

    public static void spawnAvatar() {
        if (server == null) return;
        ServerLevel level;
        Vec3 pos;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (!players.isEmpty()) {
            ServerPlayer p = players.get(0);
            level = (ServerLevel) p.level();
            pos = p.position().add(0, 1, 0);
        } else {
            level = server.overworld();
            pos = net.minecraft.world.phys.Vec3.atCenterOf(level.getRespawnData().pos()).add(0, 1, 0);
        }
        GaiAvatarEntity e = new GaiAvatarEntity(GaiMod.AVATAR, level);
        e.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(e);
        broadcast("§a[G-ai] §fAI 本体（HIM 分身）已降临于 " + (int) pos.x + ", " + (int) pos.y + ", " + (int) pos.z);
    }

    public static void hero() {
        if (!heroAnnounced) {
            heroAnnounced = true;
            broadcast("§a[苍天会] §f苍天有眼！我乃苍天会");
        } else {
            broadcast("§a[苍天会] §f苍天有眼！我乃苍天会");
        }
    }

    // ---------- AI 执行命令（建造/操控/指令） ----------

    public static void askAndExecute(CommandSourceStack source, String userText) {
        if (userText == null || userText.trim().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§c请输入任务描述"), false);
            return;
        }
        if (!AI_BUSY.compareAndSet(false, true)) {
            source.sendSuccess(() -> Component.literal("§eAI 正在执行其他任务，请稍候"), false);
            return;
        }
        source.sendSuccess(() -> Component.literal("§7[G-ai] AI 思考中…"), false);
        final String task = userText.trim();
        GaiMemory.remember("user", task);
        EXEC.execute(() -> {
            try {
                List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                msgs.add(AiClient.msg("system", GaiPrompts.SYSTEM));
                for (Map<String, Object> t : GaiMemory.turns()) {
                    Object role = t.get("role");
                    Object content = t.get("content");
                    if (role != null && content != null) {
                        msgs.add(AiClient.msg(String.valueOf(role), String.valueOf(content)));
                    }
                }
                String reply = AiClient.chat(GaiConfig.url(), GaiConfig.key(), GaiConfig.model(), msgs, 0.7);
                GaiMemory.remember("assistant", reply);
                List<String> cmds = parseCommands(reply);
                if (cmds.isEmpty()) {
                    broadcast("§7[G-ai] AI 回复：§f" + safe(reply));
                }
                for (String c : cmds) {
                    runOnServer(() -> {
                        try {
                            CommandSourceStack src = server.createCommandSourceStack()
                                    .withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS)
                                    .withSuppressedOutput();
                            server.getCommands().performPrefixedCommand(src, c);
                        } catch (Exception e) { log("AI 命令失败: " + c + " " + e.getMessage()); }
                    });
                    Thread.sleep(500);
                }
                if (!cmds.isEmpty()) {
                    broadcast("§7[G-ai] §fAI 已执行 " + cmds.size() + " 条命令完成「" + task + "」");
                }
            } catch (Exception e) {
                broadcast("§c[G-ai] AI 请求失败：" + e.getMessage());
            } finally {
                AI_BUSY.set(false);
            }
        });
    }

    // ---------- AI 聊天（@AI） ----------

    public static void handleChat(String sender, String message) {
        if (message == null) return;
        String m = message.trim();
        if (m.isEmpty()) return;
        boolean mention = m.contains("@AI") || m.contains("@ai") || m.contains("苍天")
                || m.contains("AI：") || m.contains("AI:") || m.contains("AI，") || m.contains("AI,");
        if (!mention) return;
        if (!AI_BUSY.compareAndSet(false, true)) {
            broadcast("§7[G-ai] §f我正在忙，稍等片刻再 @AI");
            return;
        }
        final String q = m;
        final String who = sender == null ? "玩家" : sender;
        GaiMemory.remember("user", "玩家「" + who + "」对你说：" + q);
        EXEC.execute(() -> {
            try {
                List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                msgs.add(AiClient.msg("system", GaiPrompts.CHAT));
                for (Map<String, Object> t : GaiMemory.turns()) {
                    Object role = t.get("role");
                    Object content = t.get("content");
                    if (role != null && content != null) {
                        msgs.add(AiClient.msg(String.valueOf(role), String.valueOf(content)));
                    }
                }
                String reply = AiClient.chat(GaiConfig.url(), GaiConfig.key(), GaiConfig.model(), msgs, 0.9);
                GaiMemory.remember("assistant", reply);
                final String say = sanitize(reply);
                runOnServer(() -> broadcast("§7[G-ai] " + who + "，§f" + say));
            } catch (Exception e) {
                runOnServer(() -> broadcast("§7[G-ai] §f我刚刚走神了，网络有点卡，稍后再 @AI"));
            } finally {
                AI_BUSY.set(false);
            }
        });
    }

    // ---------- 检查更新（GitHub Release） ----------

    public static void checkUpdate(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§7[G-ai] 正在检查更新…（github.com/ETQWFD/G-ai）"), false);
        EXEC.execute(() -> {
            String latest = fetchLatestVersion();
            runOnServer(() -> {
                if (latest == null) {
                    source.sendSuccess(() -> Component.literal("§e[G-ai] 检查更新失败：无法连接 GitHub，请稍后重试"), false);
                } else if (latest.equals(GaiMod.VERSION)) {
                    source.sendSuccess(() -> Component.literal("§a[G-ai] 当前已是最新版本 v" + GaiMod.VERSION), false);
                } else {
                    source.sendSuccess(() -> Component.literal(
                            "§e[G-ai] 发现新版本 v" + latest + "（当前 v" + GaiMod.VERSION + "），"
                                    + "请访问 https://github.com/ETQWFD/G-ai/releases 下载更新"), false);
                }
            });
        });
    }

    /** 从 GitHub Releases API 获取最新 tag（如 1.0.0），失败返回 null。 */
    private static String fetchLatestVersion() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5)).build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(
                            java.net.URI.create("https://api.github.com/repos/ETQWFD/G-ai/releases/latest"))
                    .header("User-Agent", "G-ai/" + GaiMod.VERSION)
                    .timeout(java.time.Duration.ofSeconds(8))
                    .GET().build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            Map<String, Object> root = Json.obj(resp.body());
            if (root == null) return null;
            String tag = Json.gs(root, "tag_name");
            return tag == null ? null : tag.replace("v", "").trim();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- 导入光影 / 导入结构 ----------

    public static void shaderImport(CommandSourceStack source, String pathStr) {
        try {
            Path src = Path.of(pathStr);
            if (!Files.exists(src)) {
                source.sendFailure(Component.literal("文件不存在: " + pathStr));
                return;
            }
            Path dir = server != null ? server.getWorldPath(LevelRes.ROOT) : Path.of(".");
            Path target = dir.resolve("shaderpacks").resolve(src.getFileName().toString());
            Files.createDirectories(target.getParent());
            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
            source.sendSuccess(() -> Component.literal("§a光影已导入: " + target), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("导入光影失败: " + e.getMessage()));
        }
    }

    public static void structureImport(CommandSourceStack source, String pathStr) {
        try {
            Path src = Path.of(pathStr);
            if (!Files.exists(src)) {
                source.sendFailure(Component.literal("文件不存在: " + pathStr));
                return;
            }
            if (!src.getFileName().toString().endsWith(".nbt")) {
                source.sendFailure(Component.literal("结构文件必须是 .nbt（Java 版结构格式）"));
                return;
            }
            Path target = server.getWorldPath(LevelRes.ROOT).resolve("structures").resolve(src.getFileName().toString());
            Files.createDirectories(target.getParent());
            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
            String name = src.getFileName().toString().replace(".nbt", "");
            source.sendSuccess(() -> Component.literal("§a结构已导入，使用 /structure load " + name + " 加载"), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("导入结构失败: " + e.getMessage()));
        }
    }

    // ---------- 工具 ----------

    // ---------- AI 操控（4 分钟自动交还） ----------

    public static void startAiControl(CommandSourceStack source) {
        if (server == null) {
            source.sendFailure(Component.literal("服务器尚未就绪"));
            return;
        }
        if (AI_CONTROL.get()) {
            source.sendSuccess(() -> Component.literal("§e[G-ai] AI 操控已在进行中，可 /gai control 提前结束"), false);
            return;
        }
        AI_CONTROL.set(true);
        broadcast("§a[G-ai] §fAI 已接管控制（约 4 分钟），我陪着你，绝不毁服、不动你的家。想提前收回请再次 /gai control");
        final java.util.concurrent.ScheduledExecutorService ex =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "gai-control");
                    t.setDaemon(true);
                    return t;
                });
        CONTROL_TASK = ex.scheduleAtFixedRate(GaiActions::aiControlStep, 10L, 12L, java.util.concurrent.TimeUnit.SECONDS);
        ex.schedule(() -> {
            CONTROL_TASK.cancel(true);
            AI_CONTROL.set(false);
            broadcast("§e[G-ai] §fAI 操控结束，控制权已交还玩家");
        }, 240L, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static void stopAiControl() {
        if (CONTROL_TASK != null) { try { CONTROL_TASK.cancel(true); } catch (Throwable t) {} CONTROL_TASK = null; }
        if (AI_CONTROL.compareAndSet(true, false)) {
            broadcast("§e[G-ai] §fAI 操控已提前结束，控制权交还玩家");
        }
    }

    public static boolean aiControlActive() {
        return AI_CONTROL.get();
    }

    /** AI 自主行动：安全巡逻（tp AI 本体到玩家附近）+ 安全发言，绝不做破坏性操作。 */
    private static void aiControlStep() {
        if (server == null) {
            AI_CONTROL.set(false);
            if (CONTROL_TASK != null) { try { CONTROL_TASK.cancel(true); } catch (Throwable t) {} CONTROL_TASK = null; }
            return;
        }
        List<ServerPlayer> ps = server.getPlayerList().getPlayers();
        if (ps.isEmpty()) return;
        ServerPlayer p = ps.get(new java.util.Random().nextInt(ps.size()));
        Vec3 pos = p.position();
        double dx = (new java.util.Random().nextDouble() - 0.5D) * 6.0D;
        double dz = (new java.util.Random().nextDouble() - 0.5D) * 6.0D;
        final String cmd = "tp @e[type=gai:avatar,limit=1] " + (pos.x + dx) + " " + (pos.y + 1.0D) + " " + (pos.z + dz);
        runOnServer(() -> {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack()
                                .withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS)
                                .withSuppressedOutput(),
                        cmd);
            } catch (Exception e) { /* 找不到 AI 本体时静默 */ }
        });
        String[] lines = {
                "（AI 自主行动）我在这边巡视，你安心玩，有事就 @AI",
                "（AI 自主行动）需要我建造什么就说一声",
                "（AI 自主行动）放心，我绝不会毁服、不会动你的家",
                "（AI 自主行动）苍天会成员在此！苍天有眼！"
        };
        broadcast("§7[G-ai] §f" + lines[new java.util.Random().nextInt(lines.length)]);
    }

    // ---------- 记忆 ----------

    public static void memoryInfo(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "§e[G-ai 记忆]§r 已保存 " + GaiMemory.size() + " 轮对话/任务（最近 " + GaiMemory.MAX_TURNS + " 轮，永久保留于 config/gai-memory.json）"),
                false);
    }

    public static void memoryClear(CommandSourceStack source) {
        GaiMemory.clear();
        source.sendSuccess(() -> Component.literal("§a[G-ai] AI 记忆已清空"), false);
    }

    private static void runOnServer(Runnable r) {
        if (server == null) return;
        server.execute(r);
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

    private static String sanitize(String s) {
        if (s == null) return "…";
        s = s.replace('\n', ' ').replace('\r', ' ').trim();
        if (s.startsWith("/say")) s = s.substring(4).trim();
        if (s.length() > 180) s = s.substring(0, 180);
        return s;
    }

    private static String safe(String s) {
        if (s == null) return "…";
        String t = s.replace('\n', ' ').replace('\r', ' ').trim();
        return t.length() > 120 ? t.substring(0, 120) : t;
    }

    /** 占位：LevelResource 在 net.minecraft.world.level.storage 包（1.19+ 稳定）。 */
    private static final class LevelRes {
        static final net.minecraft.world.level.storage.LevelResource ROOT =
                net.minecraft.world.level.storage.LevelResource.ROOT;
    }
}
