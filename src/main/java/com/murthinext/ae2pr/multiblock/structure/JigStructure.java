package com.murthinext.ae2pr.multiblock.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

public final class JigStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(2, 2)
            .slice("#####", "##S##", "#SKS#", "#SSS#", "S###S", "#####")
            .slice("##S##", "#SSS#", "S###S", "SGGGS", "S###S", "#####")
            .slice("#SSS#", "SSLSS", "S###S", "SGGGS", "S###S", "S###S")
            .slice("##S##", "#SSS#", "S###S", "SGGGS", "S###S", "S###S")
            .slice("#####", "##S##", "#SSS#", "#SSS#", "#SSS#", "#SSS#")
            .where('K', StructurePredicate.blocks(ModBlocks.JIG.get()))
            .where('S', StructurePredicate.blocks(ModBlocks.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), ModBlocks.AEV_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), ModBlocks.AEV_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), ModBlocks.AEV_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(), ModBlocks.AEV_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).maxCount(1)))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()))
            .where('#', StructurePredicate.blocks(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR))
            .build();

    public static final JigStructure INSTANCE = new JigStructure();

    private JigStructure() {
        super(PATTERN, MachinePartFacade.ZIRCONIUM_IRON);
    }

    public static Direction flowDirection(Direction facing, boolean mirrorFront) {
        return RelativeDirection.FRONT.getFacing(facing, Direction.UP, false, mirrorFront);
    }

    public static Direction sideDirection(Direction facing, boolean mirrorSide) {
        return RelativeDirection.RIGHT.getFacing(facing, Direction.UP, mirrorSide, false);
    }

    public static List<BlockPos> waterCells(BlockPos controllerPos, Direction facing, boolean mirrorSide,
            boolean mirrorFront) {
        Direction back = flowDirection(facing, mirrorFront).getOpposite();
        Direction side = sideDirection(facing, mirrorSide);
        List<BlockPos> cells = new ArrayList<>(12);
        for (int depth = 0; depth <= 3; depth++) {
            for (int offset = -1; offset <= 1; offset++) {
                cells.add(controllerPos.relative(back, depth).relative(side, offset).above(2));
            }
        }
        return List.copyOf(cells);
    }
}
