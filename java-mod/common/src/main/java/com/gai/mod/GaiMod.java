package com.gai.mod;

import com.gai.mod.block.GaiCoreBlock;
import com.gai.mod.block.GaiFlagBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * G-ai 模组注册入口（公共部分，被 Fabric/Forge 加载器调用）。
 * 注册：AI 本体实体、G-ai 核心方块、苍天旗（方块+物品）。
 */
public final class GaiMod {
    public static final String MOD_ID = "gai";
    public static final String NAME = "G-ai";
    public static final String VERSION = "1.0.0";

    public static EntityType<GaiAvatarEntity> AVATAR;
    public static Block FLAG_BLOCK;
    public static Block CORE_BLOCK;
    public static Item FLAG_ITEM;
    public static Item CORE_ITEM;

    private GaiMod() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** 由加载器在 mod 初始化时调用（必须保证只调用一次）。 */
    public static void register() {
        FLAG_BLOCK = Registry.register(BuiltInRegistries.BLOCK, id("flag"), new GaiFlagBlock());
        CORE_BLOCK = Registry.register(BuiltInRegistries.BLOCK, id("ai_core"), new GaiCoreBlock());

        FLAG_ITEM = Registry.register(BuiltInRegistries.ITEM, id("flag"),
                new BlockItem(FLAG_BLOCK, new Item.Properties()));
        CORE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("ai_core"),
                new BlockItem(CORE_BLOCK, new Item.Properties()));

        AVATAR = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("avatar"),
                EntityType.Builder.of(GaiAvatarEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                        .sized(0.6F, 1.9F)
                        .clientTrackingRange(10)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, id("avatar"))));
    }

    /** 属性注册（加载器各自调用）。 */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder avatarAttributes() {
        return GaiAvatarEntity.createAttributes();
    }
}
