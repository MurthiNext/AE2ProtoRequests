package com.murthinext.ae2pr.logic.world_interaction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import com.murthinext.ae2pr.recipe.ComplexWorldInteractionRecipe;

// 物品实体的交互计时和产物伤害保护状态
public final class WorldInteractionProgress {

    private static final int TRANSFORM_DELAY_TICKS = 60;

    @Nullable
    private ComplexWorldInteractionRecipe recipe;
    @Nullable
    private BlockPos pos;
    private int ticks;
    @Nullable
    private Fluid protectedFluid;
    private long explosionProtectionTick = Long.MIN_VALUE;

    public boolean advance(ComplexWorldInteractionRecipe recipe, BlockPos pos) {
        if (recipe.convertsFluid()) {
            return true;
        }
        if (this.recipe != recipe || !pos.equals(this.pos)) {
            this.recipe = recipe;
            this.pos = pos.immutable();
            ticks = 0;
        }
        if (ticks < TRANSFORM_DELAY_TICKS) {
            ticks++;
        }
        return ticks >= TRANSFORM_DELAY_TICKS;
    }

    public void reset() {
        recipe = null;
        pos = null;
        ticks = 0;
    }

    public void protectInFluid(Fluid fluid) {
        protectedFluid = fluid;
    }

    public boolean isProtectedInFluid(FluidState state) {
        if (protectedFluid != null && !state.isEmpty() && protectedFluid.isSame(state.getType())) {
            return true;
        }
        protectedFluid = null;
        return false;
    }

    public void protectFromExplosion(long tick) {
        explosionProtectionTick = tick;
    }

    public boolean isProtectedFromExplosion(long tick) {
        return explosionProtectionTick == tick;
    }
}
