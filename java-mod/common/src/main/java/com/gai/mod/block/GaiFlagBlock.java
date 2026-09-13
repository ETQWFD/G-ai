package com.gai.mod.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 苍天旗方块：灰色旗面 + 大“苍”字，可放置/可插起。
 */
public class GaiFlagBlock extends Block {
    public GaiFlagBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(0.6F)
                .sound(SoundType.STONE));
    }
}
