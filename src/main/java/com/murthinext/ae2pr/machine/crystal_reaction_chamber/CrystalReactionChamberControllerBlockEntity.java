package com.murthinext.ae2pr.machine.crystal_reaction_chamber;

import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CrystalReactionChamberControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public CrystalReactionChamberControllerBlockEntity(
            MultiblockMachineDefinition<CrystalReactionChamberControllerBlockEntity> definition,
            BlockPos pos,
            BlockState state) {
        super(definition, pos, state);
    }

    public BlockPos getBuddingQuartzPos() {
        return CrystalReactionChamberStructure.buddingQuartzPos(worldPosition, getFacing(), mirrorSide, mirrorFront);
    }

    @Override
    public BlockPos getCorePos() {
        return getBuddingQuartzPos().above(2);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBuddingQuartzPos()).inflate(2.0);
    }
}
