package com.murthinext.ae2pr.multiblock;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

// 结构模式中的相对方向：由控制器朝向与镜像推算实际方向
public enum RelativeDirection {

    UP, DOWN, LEFT, RIGHT, FRONT, BACK;

    public Direction getFacing(Direction front, Direction up, boolean flipSide, boolean flipFront) {
        return switch (this) {
            case UP -> up;
            case DOWN -> up.getOpposite();
            case LEFT -> flipSide ? front.getClockWise() : front.getCounterClockWise();
            case RIGHT -> flipSide ? front.getCounterClockWise() : front.getClockWise();
            case FRONT -> flipFront ? front.getOpposite() : front;
            case BACK -> flipFront ? front : front.getOpposite();
        };
    }

    public Comparator<BlockPos> getPosComparator(Direction front, Direction up, boolean flipSide, boolean flipFront) {
        Direction facing = getFacing(front, up, flipSide, flipFront);
        return switch (facing) {
            case UP -> Comparator.comparingInt(BlockPos::getY);
            case DOWN -> Comparator.comparingInt(pos -> -pos.getY());
            case EAST -> Comparator.comparingInt(BlockPos::getX);
            case WEST -> Comparator.comparingInt(pos -> -pos.getX());
            case SOUTH -> Comparator.comparingInt(BlockPos::getZ);
            case NORTH -> Comparator.comparingInt(pos -> -pos.getZ());
        };
    }

    int axisIndex() {
        return ordinal() / 2;
    }
}
