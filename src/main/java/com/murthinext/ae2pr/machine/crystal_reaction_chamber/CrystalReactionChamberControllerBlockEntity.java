package com.murthinext.ae2pr.machine.crystal_reaction_chamber;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;

public class CrystalReactionChamberControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public CrystalReactionChamberControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.CRYSTAL_REACTION_CHAMBER, pos, state);
    }

    public BlockPos getBuddingQuartzPos() {
        return CrystalReactionChamberStructure.buddingQuartzPos(worldPosition, getFacing(), mirrorSide,
                mirrorFront);
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
