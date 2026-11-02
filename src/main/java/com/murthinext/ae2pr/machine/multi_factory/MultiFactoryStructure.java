package com.murthinext.ae2pr.machine.multi_factory;

import com.murthinext.ae2pr.registry.ModMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.murthinext.ae2pr.registry.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.machine.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.machine.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.machine.multiblock.RelativeDirection;
import com.murthinext.ae2pr.machine.multiblock.StructurePredicate;

public final class MultiFactoryStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(1, 1)
            .slice("###", "#K#", "###")
            .slice("###", "#G#", "###")
            .slice("###", "###", "###")
            .where('K', StructurePredicate.blocks(ModMachines.MULTI_FACTORY.block()))
            .where('#', StructurePredicate.casingWithParts(ModBlocks.CRYSTAL_ENERGY_CASING.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.MOLECULAR_ASSEMBLY_CORE.get()))
            .build();

    public static final MultiFactoryStructure INSTANCE = new MultiFactoryStructure();

    private MultiFactoryStructure() {
        super(PATTERN, MachinePartFacade.CRYSTAL_ENERGY);
    }

    public static BlockPos corePos(BlockPos controllerPos, Direction facing, boolean mirrorFront) {
        return controllerPos.relative(RelativeDirection.BACK.getFacing(facing, Direction.UP, false, mirrorFront), 1);
    }
}
