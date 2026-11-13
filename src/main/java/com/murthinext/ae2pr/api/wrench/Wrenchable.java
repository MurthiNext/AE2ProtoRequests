package com.murthinext.ae2pr.api.wrench;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public interface Wrenchable {

    @Nullable
    default DirectionProperty wrenchFacing() {
        return null;
    }

    default BlockState rotateWithWrench(BlockState state) {
        DirectionProperty facing = wrenchFacing();
        if (facing == null) {
            return state;
        }
        Direction current = state.getValue(facing);
        Direction next = facing == BlockStateProperties.HORIZONTAL_FACING ? current.getClockWise() : switch (current) {
            case DOWN -> Direction.UP;
            case UP -> Direction.NORTH;
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.EAST;
            case EAST -> Direction.DOWN;
        };
        return state.setValue(facing, next);
    }

    default boolean canRotateWithWrench(BlockState state) {
        return true;
    }

    default boolean canDisassembleWithWrench(BlockState state) {
        return true;
    }

    // 扳手旋转成功后的服务端回调
    default void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
    }
}
