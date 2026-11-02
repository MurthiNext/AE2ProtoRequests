package com.murthinext.ae2pr.machine.jig;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.machine.multiblock.AbstractMultiblockControllerBlockEntity;

public class JigControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public JigControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.JIG, pos, state);
    }

    public Direction getFlowDirection() {
        return JigStructure.flowDirection(getFacing(), mirrorFront);
    }

    public Direction getSideDirection() {
        return JigStructure.sideDirection(getFacing(), mirrorSide);
    }

    public List<BlockPos> getWaterCells() {
        return JigStructure.waterCells(worldPosition, getFacing(), mirrorSide, mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        List<BlockPos> cells = getWaterCells();
        return new AABB(cells.get(0)).minmax(new AABB(cells.get(cells.size() - 1)))
                .minmax(new AABB(worldPosition));
    }
}
