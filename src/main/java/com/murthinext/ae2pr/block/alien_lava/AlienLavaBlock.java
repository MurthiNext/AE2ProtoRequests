package com.murthinext.ae2pr.block.alien_lava;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidInteractionRegistry;

import com.murthinext.ae2pr.fluid.AlienLavaFluid;
import com.murthinext.ae2pr.logic.world_interaction.WorldInteractionCatalyst;

/**
 * 异星熔岩方块，丢入的物品不会被岩浆销毁。
 */
public class AlienLavaBlock extends LiquidBlock implements WorldInteractionCatalyst {

    private static final float DAMAGE = 8.0F;
    private static final int FIRE_SECONDS = 15;

    public AlienLavaBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AlienLavaFluid.CONVERSIONS);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        int level = state.getValue(LEVEL);
        FlowingFluid fluid = getFluid();
        if (level == 0) {
            return fluid.getSource(false).setValue(AlienLavaFluid.CONVERSIONS,
                    state.getValue(AlienLavaFluid.CONVERSIONS));
        }
        if (level >= 8) {
            return fluid.getFlowing(8, true);
        }
        return fluid.getFlowing(8 - level, false);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // 只伤害生物，物品不受伤也不会被销毁
        if (!level.isClientSide && entity instanceof LivingEntity living && !living.fireImmune()) {
            living.setSecondsOnFire(FIRE_SECONDS);
            living.hurt(living.damageSources().lava(), DAMAGE);
        }
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return !getFluid().defaultFluidState().is(FluidTags.LAVA);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.getFluidState().getType().isSame(getFluid());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!FluidInteractionRegistry.canInteract(level, pos)) {
            level.scheduleTick(pos, state.getFluidState().getType(), getFluid().getTickDelay(level));
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
            boolean isMoving) {
        if (!FluidInteractionRegistry.canInteract(level, pos)) {
            level.scheduleTick(pos, state.getFluidState().getType(), getFluid().getTickDelay(level));
        }
    }

    @Override
    public int getRemainingInteractions(BlockState state) {
        return state.getValue(LEVEL) == 0
                ? AlienLavaFluid.MAX_CONVERSIONS - state.getValue(AlienLavaFluid.CONVERSIONS)
                : 0;
    }

    @Override
    public void consumeInteractions(Level level, BlockPos pos, BlockState state, int count) {
        int conversions = Math.min(AlienLavaFluid.MAX_CONVERSIONS,
                state.getValue(AlienLavaFluid.CONVERSIONS) + count);
        level.setBlockAndUpdate(pos, state.setValue(AlienLavaFluid.CONVERSIONS, conversions));
        if (conversions >= AlienLavaFluid.MAX_CONVERSIONS) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AlienLavaFluid.CONVERSIONS) < AlienLavaFluid.MAX_CONVERSIONS) {
            return;
        }
        // 转化次数用尽后，等流体中的物品实体全部离开再恢复为普通熔岩
        if (!level.getEntitiesOfClass(ItemEntity.class, new AABB(pos)).isEmpty()) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
    }

    @Override
    public ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(LEVEL) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            return new ItemStack(getFluid().getBucket());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return getFluid().getPickupSound();
    }
}
