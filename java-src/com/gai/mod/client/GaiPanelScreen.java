package com.gai.mod.client;

import com.gai.mod.GaiConfig;
import com.gai.mod.GaiMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * G-ai 悬浮控制台（电脑版"悬浮窗"）：半透明窗口覆盖在游戏上，不暂停游戏，
 * 支持拖动标题栏、配置自定义 AI、连接服务、创建 AI 躯体、AI 聊天/建造、
 * AI 操控（4 分钟自动交还）、导入光影/结构、检查更新、清空记忆。
 * 26.2 渲染模型：GuiGraphicsExtractor + extractRenderState / extractWidgetRenderState。
 */
public class GaiPanelScreen extends Screen {

    private static final int WIN_W = 340;
    private static final int WIN_H = 444;

    private int winX;
    private int winY;
    private int grabDx;
    private int grabDy;

    private EditBox urlBox;
    private EditBox keyBox;
    private EditBox modelBox;
    private EditBox chatBox;
    private EditBox taskBox;
    private EditBox shaderBox;
    private EditBox structureBox;

    private final List<Object[]> movable = new ArrayList<>(); // {widget, rx, ry}

    private String status = "就绪。按 G 或 ESC 可关闭面板";
    private String status2 = "";
    private long lastStatusAt = 0L;

    public GaiPanelScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("G-ai 悬浮控制台"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏，保持"悬浮窗"效果
    }

    @Override
    protected void init() {
        movable.clear();
        winX = (this.width - WIN_W) / 2;
        winY = Math.max(4, (this.height - WIN_H) / 2);

        // 标题栏（可拖动）与关闭按钮
        addRenderableWidget(new TitleBar(winX, winY, WIN_W - 24, 16));
        CloseButton closeBtn = new CloseButton(winX + WIN_W - 22, winY + 2, 20, 14);
        addRenderableWidget(closeBtn);
        movable.add(new Object[]{closeBtn, WIN_W - 22, 2});

        int pad = 10;
        int w = WIN_W - pad * 2;
        int row = 26;

        // ---- 自定义 AI ----
        addLabel(winX + pad, winY + row, "自定义 AI（OpenAI 兼容，支持任意接口）");
        row += 16;

        urlBox = box(winX + pad, winY + row, w, 18, "接口完整地址（含 /chat/completions）");
        row += 26;
        keyBox = box(winX + pad, winY + row, w, 18, "API Key");
        row += 26;
        modelBox = box(winX + pad, winY + row, w - 86, 18, "模型名称，如 deepseek-chat");
        btn(winX + pad + w - 80, winY + row - 1, 80, 20, "保存配置", b -> saveConfig());
        row += 28;

        // ---- 功能按钮 ----
        int bw = (w - 10) / 3;
        btn(winX + pad, winY + row, bw, 20, "启动连接", b -> cmd("/gai start", "正在启动连接服务…"));
        btn(winX + pad + bw + 5, winY + row, bw, 20, "停止连接", b -> cmd("/gai stop", "正在停止连接服务…"));
        btn(winX + pad + 2 * (bw + 5), winY + row, bw, 20, "创建AI躯体", b -> cmd("/gai spawn", "AI 本体（HIM）降临…"));
        row += 26;

        btn(winX + pad, winY + row, bw, 20, "AI 操控4分钟", b -> cmd("/gai control", "AI 接管 4 分钟，到时自动交还…"));
        btn(winX + pad + bw + 5, winY + row, bw, 20, "检查更新", b -> cmd("/gai update", "正在检查 GitHub 最新版…"));
        btn(winX + pad + 2 * (bw + 5), winY + row, bw, 20, "苍天会宣言", b -> cmd("/gai hero", "苍天有眼！我乃苍天会"));
        row += 28;

        // ---- 导入 ----
        addLabel(winX + pad, winY + row, "导入光影 / 结构（填写文件路径后点导入）");
        row += 14;
        shaderBox = box(winX + pad, winY + row, w - 70, 18, "光影包 .zip 路径，如 D:/packs/shader.zip");
        btn(winX + pad + w - 64, winY + row - 1, 64, 20, "导入光影", b -> cmd("/gai shader import " + trim(shaderBox.getValue()), "正在导入光影…"));
        row += 24;
        structureBox = box(winX + pad, winY + row, w - 70, 18, "结构 .nbt 路径，如 D:/builds/house.nbt");
        btn(winX + pad + w - 64, winY + row - 1, 64, 20, "导入结构", b -> cmd("/gai structure import " + trim(structureBox.getValue()), "正在导入结构…"));
        row += 28;

        // ---- AI 聊天 ----
        addLabel(winX + pad, winY + row, "AI 聊天（@AI 回复，AI 有意识、有永久记忆）");
        row += 14;
        chatBox = box(winX + pad, winY + row, w - 86, 18, "想对 AI 说的话…");
        btn(winX + pad + w - 80, winY + row - 1, 80, 20, "发送聊天", b -> sendChat());
        row += 26;

        // ---- AI 任务 ----
        addLabel(winX + pad, winY + row, "AI 帮助 / 建造（让 AI 干活，可真实执行命令）");
        row += 14;
        taskBox = box(winX + pad, winY + row, w - 86, 18, "如：在我身边建一座小房子");
        btn(winX + pad + w - 80, winY + row - 1, 80, 20, "执行任务", b -> askTask());
        row += 28;

        // ---- 状态区 ----
        addLabel(winX + pad, winY + row, "状态：" + status);
        row += 14;
        addLabel(winX + pad, winY + row, status2);
        row += 18;

        btn(winX + pad, winY + row, w, 18, "清空 AI 记忆", b -> cmd("/gai memory clear", "AI 记忆已清空"));
        row += 22;
        addLabel(winX + pad, winY + row, "G-ai v" + GaiMod.VERSION + " · 按 G 或 ESC 关闭 · 拖动标题栏移动窗口");

        // 预填配置（客户端读取本机 config/gai.properties）
        try {
            urlBox.setValue(GaiConfig.url());
            keyBox.setValue(GaiConfig.key());
            modelBox.setValue(GaiConfig.model());
        } catch (Throwable t) { /* ignore */ }
    }

    private void addLabel(int x, int y, String text) {
        addRenderableWidget(new Label(x, y, text));
    }

    private EditBox box(int x, int y, int w, int h, String hint) {
        EditBox e = new EditBox(this.font, x, y, w, h, Component.literal(hint));
        e.setMaxLength(512);
        e.setValue("");
        e.setSuggestion(hint);
        addRenderableWidget(e);
        movable.add(new Object[]{e, x - winX, y - winY});
        return e;
    }

    private Button btn(int x, int y, int w, int h, String label, Button.OnPress onPress) {
        Button b = Button.builder(Component.literal(label), onPress).bounds(x, y, w, h).build();
        addRenderableWidget(b);
        movable.add(new Object[]{b, x - winX, y - winY});
        return b;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    // ---------- 动作 ----------

    private void saveConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) { setStatus("请先进入世界再保存配置"); return; }
        String url = trim(urlBox.getValue());
        String key = trim(keyBox.getValue());
        String model = trim(modelBox.getValue());
        if (!url.isEmpty()) mc.player.connection.sendCommand("/gai config url " + url);
        if (!key.isEmpty()) mc.player.connection.sendCommand("/gai config key " + key);
        if (!model.isEmpty()) mc.player.connection.sendCommand("/gai config model " + model);
        setStatus("配置已保存（地址/Key/模型）");
    }

    private void sendChat() {
        Minecraft mc = Minecraft.getInstance();
        String msg = trim(chatBox.getValue());
        if (mc.player == null || mc.player.connection == null || msg.isEmpty()) { setStatus("先输入想对 AI 说的话"); return; }
        mc.player.connection.sendChat("@AI " + msg);
        chatBox.setValue("");
        setStatus("已发送给 AI：" + msg);
    }

    private void askTask() {
        Minecraft mc = Minecraft.getInstance();
        String task = trim(taskBox.getValue());
        if (mc.player == null || mc.player.connection == null || task.isEmpty()) { setStatus("先输入任务描述"); return; }
        mc.player.connection.sendCommand("/gai ask " + task);
        taskBox.setValue("");
        setStatus("AI 正在执行：" + task);
    }

    private void cmd(String command, String statusText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) { setStatus("请先进入世界再操作"); return; }
        try {
            String c = command.startsWith("/") ? command.substring(1) : command;
            mc.player.connection.sendCommand(c);
        } catch (Throwable t) { setStatus("发送失败：" + t.getMessage()); return; }
        setStatus(statusText);
    }

    private void setStatus(String s) {
        status = s;
        status2 = "";
        lastStatusAt = System.currentTimeMillis();
    }

    // ---------- 渲染 ----------

    @Override
    public void extractBackground(GuiGraphicsExtractor gge, int mouseX, int mouseY, float partialTick) {
        // 透明背景：游戏画面直接透出（悬浮窗效果）
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gge, int mouseX, int mouseY, float partialTick) {
        // 窗口底板 + 标题栏
        gge.fill(winX, winY, winX + WIN_W, winY + WIN_H, 0xE8101828);
        gge.fill(winX, winY, winX + WIN_W, winY + 18, 0xF06366F1);
        gge.outline(winX, winY, winX + WIN_W, winY + WIN_H, 0xFF6366F1);
        gge.text(this.font, "G-ai 悬浮控制台 v" + GaiMod.VERSION, winX + 8, winY + 4, 0xFFFFFFFF);
        super.extractRenderState(gge, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (System.currentTimeMillis() - lastStatusAt > 9000L && !status.startsWith("已") && !status.startsWith("AI")) {
            // 状态自动复原（避免打扰）
        }
    }

    @Override
    public void onClose() {
        GaiPanel.markClosed();
        super.onClose();
    }

    // ---------- 组件 ----------

    /** 纯文本标签。 */
    private static final class Label extends AbstractWidget {
        private final String text;

        Label(int x, int y, String text) {
            super(x, y, 0, 0, Component.literal(text));
            this.text = text == null ? "" : text;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor gge, int mouseX, int mouseY, float partialTick) {
            gge.text(Minecraft.getInstance().font, text, getX(), getY(), 0xFFB8C0D0);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
            // 标签无需朗读
        }
    }

    /** 关闭按钮（✕）。 */
    private static final class CloseButton extends AbstractWidget {
        CloseButton(int x, int y, int w, int h) {
            super(x, y, w, h, Component.literal("✕"));
        }

        @Override
        public void onClick(MouseButtonEvent e, boolean b) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof GaiPanelScreen) {
                mc.setScreenAndShow(null);
            }
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor gge, int mouseX, int mouseY, float partialTick) {
            gge.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x66FFFFFF);
            gge.text(Minecraft.getInstance().font, "✕", getX() + 6, getY() + 2, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
            // 无需朗读
        }
    }

    /** 标题栏：按住左键拖动整个面板。 */
    private final class TitleBar extends AbstractWidget {
        TitleBar(int x, int y, int w, int h) {
            super(x, y, w, h, Component.literal("G-ai title"));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean b) {
            if (event.button() == 0 && isMouseOver(event.x(), event.y())) {
                grabDx = (int) (event.x() - winX);
                grabDy = (int) (event.y() - winY);
                setFocused(true);
                return true;
            }
            return super.mouseClicked(event, b);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            if (event.button() == 0) {
                int nx = (int) (event.x() - grabDx);
                int ny = (int) (event.y() - grabDy);
                nx = Math.max(-WIN_W + 80, Math.min(nx, width - 40));
                ny = Math.max(0, Math.min(ny, height - 60));
                if (nx != winX || ny != winY) {
                    winX = nx;
                    winY = ny;
                    relayout();
                }
                return true;
            }
            return super.mouseDragged(event, dx, dy);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor gge, int mouseX, int mouseY, float partialTick) {
            // 标题栏由 Screen 统一绘制，这里无需渲染
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
            // 无需朗读
        }

        private void relayout() {
            for (Object[] m : movable) {
                Object wgt = m[0];
                if (wgt == null) continue;
                int rx = (Integer) m[1];
                int ry = (Integer) m[2];
                ((AbstractWidget) wgt).setX(winX + rx);
                ((AbstractWidget) wgt).setY(winY + ry);
            }
            setX(winX);
            setY(winY);
        }
    }
}
