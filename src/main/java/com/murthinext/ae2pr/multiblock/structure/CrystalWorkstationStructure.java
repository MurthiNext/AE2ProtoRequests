package com.murthinext.ae2pr.multiblock.structure;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.StructureResult;

public final class CrystalWorkstationStructure {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(0, 4)
            .slice("AAAAKAAAA", "B#######B", "B#######B", "B#######B", "AAAAAAAAA")
            .slice("AAAAAAAAA", "CDDDDDDDC", "CDDDDDDDC", "CDDDDDDDC", "AAAAAAAAA")
            .slice("AAAAAAAAA", "C#######C", "C###G###C", "C#######C", "AAAAAAAAA")
            .slice("AAAAAAAAA", "CDDDDDDDC", "CDDDDDDDC", "CDDDDDDDC", "AAAAAAAAA")
            .slice("AAAAAAAAA", "B#######B", "B#######B", "B#######B", "AAAAAAAAA")
            .where('K', StructurePredicate.blocks(ModBlocks.CRYSTAL_WORKSTATION.get()))
            .where('A', StructurePredicate.blocks(ModBlocks.REINFORCED_HIGH_POWER_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), ModBlocks.AEV_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), ModBlocks.AEV_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), ModBlocks.AEV_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(), ModBlocks.AEV_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).maxCount(1)))
            .where('B', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()))
            .where('C', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_STABILITY_CASING.get()))
            .where('D', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_CORE.get()))
            .where('#', StructurePredicate.blocks(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR))
            .build();

    private CrystalWorkstationStructure() {
    }

    public static BlockPos corePos(BlockPos controllerPos, Direction facing, boolean mirrorFront) {
        return controllerPos.relative(RelativeDirection.BACK.getFacing(facing, Direction.UP, false, mirrorFront), 2)
                .above(2);
    }

    public static void updateFormed(Level level, BlockPos pos, Direction facing, StructureResult result) {
        if (result.formed()) {
            applyParts(level, result.cells(), true);
        } else {
            for (boolean[] mirror : MultiblockPattern.MIRRORS) {
                applyParts(level, PATTERN.cells(pos, facing, Direction.UP, mirror[0], mirror[1], PATTERN.minSlices()), false);
            }
        }
    }

    private static void applyParts(Level level, List<BlockPos> cells, boolean formed) {
        int style = formed ? MachinePartFacade.HIGH_POWER.style() : MachinePartBlock.DEFAULT_STYLE;
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            BlockState updated;
            if (state.getBlock() instanceof MachinePartBlock) {
                updated = state.setValue(MachinePartBlock.FORMED, formed).setValue(MachinePartBlock.STYLE, style);
            } else if (state.getBlock() instanceof FluixCrystalEnergyHatchBlock) {
                updated = state.setValue(FluixCrystalEnergyHatchBlock.FORMED, formed)
                        .setValue(FluixCrystalEnergyHatchBlock.STYLE, style);
            } else {
                continue;
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }
}
