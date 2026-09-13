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

/**
 * Forge 26.2 入口：新事件总线 API（各事件自带静态 BUS，直接 addListener）。
 */
@Mod(GaiMod.MOD_ID)
public class ForgeInit {

    public ForgeInit() {
        GaiMod.register();
        GaiConfig.init(FMLPaths.GAMEDIR.get());
        GaiMemory.init(FMLPaths.GAMEDIR.get());

        EntityAttributeCreationEvent.BUS.addListener(e ->
                e.put(GaiMod.AVATAR, GaiAvatarEntity.createAttributes().build()));

        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().identifier().getPath().equals("building_blocks")) {
                e.accept(GaiMod.FLAG_ITEM);
                e.accept(GaiMod.CORE_ITEM);
            }
        });

        RegisterCommandsEvent.BUS.addListener(e -> GaiCommands.register(e.getDispatcher()));
        ServerStartingEvent.BUS.addListener(e -> GaiActions.onServerStart(e.getServer()));
        ServerStoppingEvent.BUS.addListener(e -> GaiActions.onServerStop());
        ServerChatEvent.BUS.addListener((java.util.function.Consumer<ServerChatEvent>) e ->
                GaiActions.handleChat(e.getPlayer().getGameProfile().name(), e.getRawText()));

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
        }
    }
}
