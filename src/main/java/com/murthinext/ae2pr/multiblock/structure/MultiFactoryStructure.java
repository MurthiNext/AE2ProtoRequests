package com.murthinext.ae2pr.multiblock.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

public final class MultiFactoryStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(1, 1)
            .slice("###", "#K#", "###")
            .slice("###", "#G#", "###")
            .slice("###", "###", "###")
            .where('K', StructurePredicate.blocks(ModBlocks.MULTI_FACTORY.get()))
            .where('#', StructurePredicate.casingWithParts(ModBlocks.REINFORCED_HIGH_POWER_CASING.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.MOLECULAR_ASSEMBLY_CORE.get()))
            .build();

    public static final MultiFactoryStructure INSTANCE = new MultiFactoryStructure();

    private MultiFactoryStructure() {
        super(PATTERN, MachinePartFacade.HIGH_POWER);
    }

    public static BlockPos corePos(BlockPos controllerPos, Direction facing, boolean mirrorFront) {
        return controllerPos.relative(RelativeDirection.BACK.getFacing(facing, Direction.UP, false, mirrorFront), 1);
    }
}
