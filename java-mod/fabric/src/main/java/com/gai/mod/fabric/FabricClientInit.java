package com.gai.mod.fabric;

import com.gai.mod.GaiMod;
import com.gai.mod.client.GaiAvatarModel;
import com.gai.mod.client.GaiAvatarRenderer;
import com.gai.mod.client.GaiModelLayers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class FabricClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GaiMod.AVATAR, GaiAvatarRenderer::new);
        ModelLayerRegistry.registerModelLayer(GaiModelLayers.AVATAR, GaiAvatarModel::createBodyLayer);
    }
}
