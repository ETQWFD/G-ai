package com.gai.mod.fabric;

import com.gai.mod.GaiMod;
import com.gai.mod.client.GaiAvatarModel;
import com.gai.mod.client.GaiAvatarRenderer;
import com.gai.mod.client.GaiModelLayers;
import com.gai.mod.client.GaiPanel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class FabricClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GaiMod.AVATAR, GaiAvatarRenderer::new);
        ModelLayerRegistry.registerModelLayer(GaiModelLayers.AVATAR, GaiAvatarModel::createBodyLayer);

        // 悬浮控制台：G 键开关 + HUD 悬浮球
        ClientTickEvents.END_CLIENT_TICK.register(mc -> GaiPanel.onClientTick());
        HudElementRegistry.addLast(GaiMod.id("orb"), (gge, dt) -> GaiPanel.drawOrb(gge, dt));
    }
}
