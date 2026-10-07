package com.murthinext.ae2pr.multiblock;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 结构模式中的相对方向：由控制器正面与上方推算实际方向，左右 / 前后镜像时取反。
 */
public enum RelativeDirection {

    UP, DOWN, LEFT, RIGHT, FRONT, BACK;

    /** 相对方向在当前朝向下对应的实际方向。 */
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

    /** 沿该方向递增的坐标比较器，用于对结构内部件排序（对应 GT 的 partSorter）。 */
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

    /** 轴向序号：UP/DOWN=0，LEFT/RIGHT=1，FRONT/BACK=2。 */
    int axisIndex() {
        return ordinal() / 2;
    }
}
