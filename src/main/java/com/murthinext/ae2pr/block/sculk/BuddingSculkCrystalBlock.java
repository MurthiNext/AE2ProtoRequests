package com.murthinext.ae2pr.block.sculk;

import com.murthinext.ae2pr.core.registry.ModBlocks;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;

public final class BuddingSculkCrystalBlock extends Block {

    public enum Quality {
        FLAWLESS,
        FLAWED,
        CHIPPED,
        DAMAGED
    }

    private static final int GROWTH_CHANCE = 5;
    private static final int DECAY_CHANCE = 12;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final Quality quality;

    public BuddingSculkCrystalBlock(Quality quality, Properties properties) {
        super(properties);
        this.quality = quality;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROWTH_CHANCE) != 0) {
            return;
        }

        Direction direction = Util.getRandom(DIRECTIONS, random);
        BlockPos targetPos = pos.relative(direction);
        BlockState targetState = level.getBlockState(targetPos);
        Block newCluster = null;
        if (canClusterGrowAtState(targetState)) {
            newCluster = ModBlocks.SMALL_SCULK_CRYSTAL_BUD.get();
        } else if (targetState.is(ModBlocks.SMALL_SCULK_CRYSTAL_BUD.get())
                && targetState.getValue(SculkCrystalClusterBlock.FACING) == direction) {
            newCluster = ModBlocks.MEDIUM_SCULK_CRYSTAL_BUD.get();
        } else if (targetState.is(ModBlocks.MEDIUM_SCULK_CRYSTAL_BUD.get())
                && targetState.getValue(SculkCrystalClusterBlock.FACING) == direction) {
            newCluster = ModBlocks.LARGE_SCULK_CRYSTAL_BUD.get();
        } else if (targetState.is(ModBlocks.LARGE_SCULK_CRYSTAL_BUD.get())
                && targetState.getValue(SculkCrystalClusterBlock.FACING) == direction) {
            newCluster = ModBlocks.SCULK_CRYSTAL_CLUSTER.get();
        }

        if (newCluster == null) {
            return;
        }

        BlockState newClusterState = newCluster
                .defaultBlockState()
                .setValue(SculkCrystalClusterBlock.FACING, direction)
                .setValue(
                        SculkCrystalClusterBlock.WATERLOGGED,
                        targetState.getFluidState().getType() == Fluids.WATER);
        level.setBlockAndUpdate(targetPos, newClusterState);

        if (quality == Quality.FLAWLESS || random.nextInt(DECAY_CHANCE) != 0) {
            return;
        }
        Block decayed = switch (quality) {
            case FLAWED -> ModBlocks.CHIPPED_BUDDING_SCULK_CRYSTAL.get();
            case CHIPPED -> ModBlocks.DAMAGED_BUDDING_SCULK_CRYSTAL.get();
            case DAMAGED -> ModBlocks.SCULK_CRYSTAL_BLOCK.get();
            case FLAWLESS -> throw new IllegalStateException("Flawless budding block should not decay");
        };
        level.setBlockAndUpdate(pos, decayed.defaultBlockState());
    }

    public static boolean canClusterGrowAtState(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) && state.getFluidState().getAmount() == 8;
    }
}
