package com.murthinext.ae2pr.logic.world_interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// 世界交互催化方块的使用次数限制
public interface WorldInteractionCatalyst {

    int getRemainingInteractions(BlockState state);

    void consumeInteractions(Level level, BlockPos pos, BlockState state, int count);
}
