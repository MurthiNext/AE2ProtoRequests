package com.murthinext.ae2pr.api.machine;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CoreLight {

    boolean matches(BlockState state);

    void apply(Level level, BlockPos pos, BlockState state, boolean lit);
    @Nullable
    CoreLight NONE = null;
}
