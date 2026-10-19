package com.murthinext.ae2pr.multiblock.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

/**
 * 水晶反应仓结构定义。
 */
public final class CrystalReactionChamberStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(1, 0)
            .slice("S###S", "KBBBS", "SBBBS", "SBBBS", "#SSS#", "#####")
            .slice("#SSS#", "B###B", "B###B", "B###B", "SDDDS", "#BBB#")
            .slice("#SSS#", "B###B", "B#F#B", "B###B", "SEGES", "#BBB#")
            .slice("#SSS#", "B###B", "B###B", "B###B", "SDDDS", "#BBB#")
            .slice("S###S", "SBBBS", "SBBBS", "SBBBS", "#SSS#", "#####")
            .where('K', StructurePredicate.blocks(ModBlocks.CRYSTAL_REACTION_CHAMBER.get()))
            .where('S', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).maxCount(1)))
            .where('B', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            .where('D', StructurePredicate.blocks(ModBlocks.METEOR_STEEL_BLOCK.get()))
            .where('E', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('F', StructurePredicate.blocks(AEBlocks.FLAWLESS_BUDDING_QUARTZ.block()))
            .where('G', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_CORE.get()))
            .where('#', StructurePredicate.any())
            .build();

    public static final CrystalReactionChamberStructure INSTANCE = new CrystalReactionChamberStructure();

    private CrystalReactionChamberStructure() {
        super(PATTERN, MachinePartFacade.CRYSTAL);
    }

    public static BlockPos buddingQuartzPos(BlockPos controllerPos, Direction facing, boolean mirrorSide,
            boolean mirrorFront) {
        Direction back = RelativeDirection.BACK.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
        Direction right = RelativeDirection.RIGHT.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
        return controllerPos.relative(back, 2).relative(right, 2).above();
    }
}
