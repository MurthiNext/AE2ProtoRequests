package com.murthinext.ae2pr.machine.integrated_mass_fabricator;

import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class IntegratedMassFabricatorControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public IntegratedMassFabricatorControllerBlockEntity(
            MultiblockMachineDefinition<IntegratedMassFabricatorControllerBlockEntity> definition,
            BlockPos pos,
            BlockState state) {
        super(definition, pos, state);
    }

    BlockPos floorCenter() {
        return IntegratedMassFabricatorStructure.floorCenter(worldPosition, getFacing(), mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        int radius = IntegratedMassFabricatorStructure.FIELD_WIDTH / 2;
        return new AABB(floorCenter())
                .inflate(radius, 0, radius)
                .expandTowards(0, IntegratedMassFabricatorStructure.PLANE_SEPARATION, 0);
    }
}
