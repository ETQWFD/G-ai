package com.gai.mod.block;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * G-ai 核心方块：AI 本体生成的锚点。
 * 26.2 要求：Block 构造前 Properties 必须已通过 setId() 绑定 Registry Key（否则 Block id not set）。
 */
public class GaiCoreBlock extends Block {
    public GaiCoreBlock(ResourceKey<Block> key) {
        super(BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0F)
                .sound(SoundType.STONE));
    }
}
