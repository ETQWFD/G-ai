package com.gai.mod.block;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 苍天旗方块：灰色旗面 + 大"苍"字，可放置/可插起。
 * 26.2 要求：Block 构造前 Properties 必须已通过 setId() 绑定 Registry Key（否则 Block id not set）。
 */
public class GaiFlagBlock extends Block {
    public GaiFlagBlock(ResourceKey<Block> key) {
        super(BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(0.6F)
                .sound(SoundType.STONE));
    }
}
