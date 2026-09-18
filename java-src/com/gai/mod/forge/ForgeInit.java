package com.gai.mod.forge;

import com.gai.mod.GaiActions;
import com.gai.mod.GaiAvatarEntity;
import com.gai.mod.GaiCommands;
import com.gai.mod.GaiConfig;
import com.gai.mod.GaiMemory;
import com.gai.mod.GaiMod;
import com.gai.mod.client.GaiAvatarModel;
import com.gai.mod.client.GaiAvatarRenderer;
import com.gai.mod.client.GaiModelLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;

/**
 * Forge 26.1.2 入口：新事件总线 API（各事件自带静态 BUS，直接 addListener）。
 * 方块/物品/实体必须注册在 RegisterEvent 回调中（mod 构造阶段 registry 已冻结，直接注册会
 * 抛 "Registry minecraft:block is already frozen"）。
 */
@Mod(GaiMod.MOD_ID)
public class ForgeInit {

    public ForgeInit() {
        // Forge 注册时机：RegisterEvent（BLOCK/ITEM/ENTITY_TYPE 各自回调，此时 registry 未冻结）
        RegisterEvent.getBus(BusGroup.DEFAULT).addListener(e -> {
            if (e.getRegistryKey() == Registries.BLOCK) {
                GaiMod.registerBlocks();
            } else if (e.getRegistryKey() == Registries.ITEM) {
                GaiMod.registerItems();
            } else if (e.getRegistryKey() == Registries.ENTITY_TYPE) {
                GaiMod.registerEntity();
            }
        });

        GaiConfig.init(FMLPaths.GAMEDIR.get());
        GaiMemory.init(FMLPaths.GAMEDIR.get());

        EntityAttributeCreationEvent.BUS.addListener(e ->
                e.put(GaiMod.AVATAR, GaiAvatarEntity.createAttributes().build()));

        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().identifier().getPath().equals("building_blocks")) {
                e.accept(GaiMod.FLAG_ITEM);
                e.accept(GaiMod.FIVE_STAR_ITEM);
                e.accept(GaiMod.CORE_ITEM);
            }
        });

        RegisterCommandsEvent.BUS.addListener(e -> GaiCommands.register(e.getDispatcher()));
        ServerStartingEvent.BUS.addListener(e -> GaiActions.onServerStart(e.getServer()));
        ServerStoppingEvent.BUS.addListener(e -> GaiActions.onServerStop());
        ServerChatEvent.BUS.addListener((java.util.function.Consumer<ServerChatEvent>) e ->
                GaiActions.handleChat(e.getPlayer().getGameProfile().name(), e.getRawText()));

        // 玩家进入世界：自动创建 AI 躯体（无需软件/命令，加载 mod 即生效）
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(
                (java.util.function.Consumer<net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent>) e -> {
                    try {
                        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                            GaiActions.onPlayerJoin(sp);
                        }
                    } catch (Throwable t) { /* ignore */ }
                });

        // 客户端专属注册（专用服务器上不执行）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientReg.register();
        }
    }

    /** 客户端渲染注册（懒加载，避免专用服务器 NoClassDefFoundError）。 */
    private static final class ClientReg {
        static void register() {
            EntityRenderersEvent.RegisterRenderers.BUS.addListener(e ->
                    e.registerEntityRenderer(GaiMod.AVATAR, GaiAvatarRenderer::new));
            EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(e ->
                    e.registerLayerDefinition(GaiModelLayers.AVATAR, GaiAvatarModel::createBodyLayer));

            // 悬浮控制台：G 键开关 + HUD 悬浮球（与手机版一致）
            net.minecraftforge.event.TickEvent.ClientTickEvent.Post.BUS.addListener(
                    (java.util.function.Consumer<net.minecraftforge.event.TickEvent.ClientTickEvent.Post>) e ->
                            com.gai.mod.client.GaiPanel.onClientTick());
            net.minecraftforge.client.event.AddGuiOverlayLayersEvent.BUS.addListener(e ->
                    e.getLayeredDraw().add(GaiMod.id("orb"),
                            (gge, dt) -> com.gai.mod.client.GaiPanel.drawOrb(gge, dt)));
        }
    }
}
