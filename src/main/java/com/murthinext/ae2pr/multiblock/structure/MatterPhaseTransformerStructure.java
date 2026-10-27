package com.murthinext.ae2pr.multiblock.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

// 物质相变仪结构定义
public final class MatterPhaseTransformerStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(0, 2)
            .slice("##C##", "##B##", "##B##", "##B##", "##G##", "##G##", "##G##", "##B##", "##B##", "##B##",
                    "#####")
            .slice("#BBB#", "##B##", "##G##", "##G##", "##G##", "#GGG#", "##G##", "##G##", "##G##", "##B##",
                    "#BBB#")
            .slice("#BBB#", "BBLBB", "BG#GB", "BG#GB", "GG#GG", "GGKGG", "GG#GG", "BG#GB", "BG#GB", "BBLBB",
                    "#BBB#")
            .slice("#BBB#", "##B##", "##G##", "##G##", "##G##", "#GGG#", "##G##", "##G##", "##G##", "##B##",
                    "#BBB#")
            .slice("#####", "##B##", "##B##", "##B##", "##G##", "##G##", "##G##", "##B##", "##B##", "##B##",
                    "#####")
            .where('C', StructurePredicate.blocks(ModBlocks.MATTER_PHASE_TRANSFORMER.get()))
            .where('B', StructurePredicate.blocks(ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_DUAL_INPUT_HATCH.get(), ModBlocks.AEV_DUAL_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.ME_NETWORK_ACCESS_HATCH.get()).maxCount(1)))
            .where('G', StructurePredicate.blocks(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('K', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_CORE.get()))
            .where('#', StructurePredicate.any())
            .build();

    public static final MatterPhaseTransformerStructure INSTANCE = new MatterPhaseTransformerStructure();

    private MatterPhaseTransformerStructure() {
        super(PATTERN, MachinePartFacade.ZIRCONIA);
    }

    public static BlockPos corePos(BlockPos controllerPos, Direction facing, boolean mirrorFront) {
        Direction back = RelativeDirection.BACK.getFacing(facing, Direction.UP, false, mirrorFront);
        return controllerPos.relative(back, 2).above(5);
    }
}
