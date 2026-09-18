package com.gai.mod;

import com.gai.mod.block.GaiCoreBlock;
import com.gai.mod.block.GaiFlagBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * G-ai 模组注册入口（公共部分，被 Fabric/Forge 加载器调用）。
 * 注册：AI 本体实体、G-ai 核心方块、苍天旗与五星红旗（方块+物品）。
 */
public final class GaiMod {
    public static final String MOD_ID = "gai";
    public static final String NAME = "G-ai";
    public static final String VERSION = "1.6.0";

    public static EntityType<GaiAvatarEntity> AVATAR;
    public static Block FLAG_BLOCK;
    public static Block FIVE_STAR_BLOCK;
    public static Block CORE_BLOCK;
    public static Item FLAG_ITEM;
    public static Item FIVE_STAR_ITEM;
    public static Item CORE_ITEM;

    private GaiMod() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** 由加载器在 mod 初始化时调用（必须保证只调用一次）。Fabric 直接调用；Forge 在 RegisterEvent 回调中调用对应分组。 */
    public static void register() {
        registerBlocks();
        registerItems();
        registerEntity();
    }

    /** 方块注册（Fabric：初始化即调用；Forge：RegisterEvent BLOCK 回调中调用）。 */
    public static void registerBlocks() {
        // 26.2：Block 构造前 Properties 必须已绑定 Registry Key（setId），否则启动即 "Block id not set"
        ResourceKey<Block> flagKey = ResourceKey.create(Registries.BLOCK, id("flag"));
        ResourceKey<Block> fiveStarKey = ResourceKey.create(Registries.BLOCK, id("five_star"));
        ResourceKey<Block> coreKey = ResourceKey.create(Registries.BLOCK, id("ai_core"));
        FLAG_BLOCK = Registry.register(BuiltInRegistries.BLOCK, flagKey, new GaiFlagBlock(flagKey));
        FIVE_STAR_BLOCK = Registry.register(BuiltInRegistries.BLOCK, fiveStarKey, new GaiFlagBlock(fiveStarKey));
        CORE_BLOCK = Registry.register(BuiltInRegistries.BLOCK, coreKey, new GaiCoreBlock(coreKey));
    }

    /** 物品注册（Forge：RegisterEvent ITEM 回调中调用；Fabric：初始化即调用）。 */
    public static void registerItems() {
        // 用注册表回查方块（Forge 下 ITEM 事件在 BLOCK 事件之后触发，字段已赋值；这里双保险）
        Block flag = FLAG_BLOCK != null ? FLAG_BLOCK : BuiltInRegistries.BLOCK.getValue(id("flag"));
        Block five = FIVE_STAR_BLOCK != null ? FIVE_STAR_BLOCK : BuiltInRegistries.BLOCK.getValue(id("five_star"));
        Block core = CORE_BLOCK != null ? CORE_BLOCK : BuiltInRegistries.BLOCK.getValue(id("ai_core"));
        FLAG_ITEM = Registry.register(BuiltInRegistries.ITEM, id("flag"),
                new BlockItem(flag, new Item.Properties()));
        FIVE_STAR_ITEM = Registry.register(BuiltInRegistries.ITEM, id("five_star"),
                new BlockItem(five, new Item.Properties()));
        CORE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("ai_core"),
                new BlockItem(core, new Item.Properties()));
    }

    /** 实体注册（Forge：RegisterEvent ENTITY_TYPE 回调中调用；Fabric：初始化即调用）。 */
    public static void registerEntity() {
        AVATAR = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("avatar"),
                EntityType.Builder.of(GaiAvatarEntity::new, MobCategory.MISC)
                        .sized(0.6F, 1.9F)
                        .clientTrackingRange(10)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, id("avatar"))));
    }

    /** 属性注册（加载器各自调用）。 */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder avatarAttributes() {
        return GaiAvatarEntity.createAttributes();
    }
}
