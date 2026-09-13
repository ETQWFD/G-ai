package com.gai.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * G-ai 客户端控制器：G 键开关悬浮控制台、HUD 悬浮球绘制。
 * 双加载器（Fabric/Forge）各自在客户端 tick 与 HUD 阶段调用本类。
 */
public final class GaiPanel {

    private static boolean panelOpen = false;
    private static boolean lastKeyDown = false;

    private GaiPanel() {}

    /** 客户端每 tick 调用：轮询 G 键开关面板。 */
    public static void onClientTick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                lastKeyDown = false;
                return;
            }
            boolean down;
            try {
                down = InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_G);
            } catch (Throwable t) {
                down = false;
            }
            if (down && !lastKeyDown) {
                toggle(mc);
            }
            lastKeyDown = down;
        } catch (Throwable t) {
            // 永不因面板崩溃
        }
    }

    public static void toggle(Minecraft mc) {
        try {
            if (mc.gui.screen() instanceof GaiPanelScreen) {
                mc.setScreenAndShow(null);
                panelOpen = false;
            } else if (mc.level != null && mc.player != null) {
                mc.setScreenAndShow(new GaiPanelScreen());
                panelOpen = true;
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    public static void open(Minecraft mc) {
        try {
            if (mc.level != null && mc.player != null && !(mc.gui.screen() instanceof GaiPanelScreen)) {
                mc.setScreenAndShow(new GaiPanelScreen());
                panelOpen = true;
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    public static void close(Minecraft mc) {
        try {
            if (mc.gui.screen() instanceof GaiPanelScreen) {
                mc.setScreenAndShow(null);
                panelOpen = false;
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    public static boolean isOpen() {
        return panelOpen;
    }

    /** 面板关闭时回调。 */
    public static void markClosed() {
        panelOpen = false;
    }

    /** HUD 悬浮球：右上角显示 G-ai 徽标与按键提示。 */
    public static void drawOrb(GuiGraphicsExtractor gge, DeltaTracker dt) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.gui.screen() != null) return;
            int w = mc.getWindow().getGuiScaledWidth();
            int x = w - 44;
            int y = 8;
            gge.fill(x, y, x + 36, y + 36, 0xF06366F1);
            gge.outline(x, y, x + 36, y + 36, 0xFF8B5CF6);
            gge.text(mc.font, "G", x + 13, y + 10, 0xFFFFFFFF);
            gge.text(mc.font, "按 G 打开 G-ai 悬浮窗", x - 132, y + 11, 0xFFFFFFFF);
        } catch (Throwable t) {
            // HUD 绘制永不崩溃
        }
    }
}
