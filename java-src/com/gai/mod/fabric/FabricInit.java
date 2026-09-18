package com.gai.mod.fabric;

import com.gai.mod.GaiActions;
import com.gai.mod.GaiAvatarEntity;
import com.gai.mod.GaiCommands;
import com.gai.mod.GaiConfig;
import com.gai.mod.GaiMemory;
import com.gai.mod.GaiMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;

public class FabricInit implements ModInitializer {

    @Override
    public void onInitialize() {
        GaiMod.register();
        FabricDefaultAttributeRegistry.register(GaiMod.AVATAR, GaiAvatarEntity.createAttributes());
        GaiConfig.init(FabricLoader.getInstance().getGameDir());
        GaiMemory.init(FabricLoader.getInstance().getGameDir());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GaiCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(GaiActions::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GaiActions.onServerStop());

        // 玩家进入世界：自动创建 AI 躯体（无需软件/命令，加载 mod 即生效）
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            try {
                GaiActions.onPlayerJoin(handler.getPlayer());
            } catch (Throwable t) { /* ignore */ }
        });

        // 玩家聊天：@AI 触发 AI 回复（26.2 签名：PlayerChatMessage, ServerPlayer, ChatType$Bound）
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            GaiActions.handleChat(sender.getGameProfile().name(), message.decoratedContent().getString());
        });
    }
}
