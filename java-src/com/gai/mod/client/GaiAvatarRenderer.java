package com.gai.mod.client;

import com.gai.mod.GaiAvatarEntity;
import com.gai.mod.GaiMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

/**
 * AI 本体渲染器（26.2：LivingEntityRenderer<T, S, M> + EntityRenderState）。
 */
public class GaiAvatarRenderer extends LivingEntityRenderer<GaiAvatarEntity, GaiAvatarRenderState, GaiAvatarModel> {

    public GaiAvatarRenderer(EntityRendererProvider.Context context) {
        super(context, new GaiAvatarModel(context.bakeLayer(GaiModelLayers.AVATAR)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(GaiAvatarRenderState state) {
        return GaiMod.id("textures/entity/gai_avatar.png");
    }

    @Override
    public GaiAvatarRenderState createRenderState() {
        return new GaiAvatarRenderState();
    }

    @Override
    public void extractRenderState(GaiAvatarEntity entity, GaiAvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }
}
