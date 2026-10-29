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
            .where('#', StructurePredicate.blocks(ModBlocks.REINFORCED_HIGH_POWER_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), ModBlocks.AEV_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), ModBlocks.AEV_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), ModBlocks.AEV_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(), ModBlocks.AEV_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_DUAL_INPUT_HATCH.get(), ModBlocks.AEV_DUAL_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.ME_NETWORK_ACCESS_HATCH.get()).maxCount(1)))
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
