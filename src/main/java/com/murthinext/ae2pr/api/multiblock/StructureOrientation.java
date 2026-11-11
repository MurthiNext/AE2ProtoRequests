package com.murthinext.ae2pr.api.multiblock;

import net.minecraft.core.Direction;

public record StructureOrientation(Direction front, Direction up, boolean mirrorSide, boolean mirrorFront) {

    public Direction direction(RelativeDirection relative) {
        return relative.getFacing(front, up, mirrorSide, mirrorFront);
    }
}
