package com.murthinext.ae2pr.multiblock.structure;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.StructureResult;

/**
 * 水晶反应仓结构定义与外观同步。
 */
public final class CrystalReactionChamberStructure {

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

    private CrystalReactionChamberStructure() {
    }

    /**
     * 同步结构内输入/输出总线与能源仓的外观。
     */
    public static void updateFormed(Level level, BlockPos controllerPos, Direction facing, StructureResult result) {
        if (result.formed()) {
            applyParts(level, PATTERN.cells(controllerPos, facing, Direction.UP, result.mirrorSide(),
                    result.mirrorFront(), result.slices()), true);
            return;
        }
        // 未成型：逐一复位全部镜像组合
        for (boolean[] mirror : MultiblockPattern.MIRRORS) {
            applyParts(level, PATTERN.cells(controllerPos, facing, Direction.UP, mirror[0], mirror[1],
                    PATTERN.minSlices()), false);
        }
    }

    private static void applyParts(Level level, List<BlockPos> cells, boolean formed) {
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get())) {
                // 能源仓：切换成型外观为水晶机壳
                BlockState updated = state;
                if (state.getValue(FluixCrystalEnergyHatchBlock.FORMED) != formed) {
                    updated = updated.setValue(FluixCrystalEnergyHatchBlock.FORMED, formed);
                }
                int style = formed ? MachinePartFacade.CRYSTAL.style() : MachinePartBlock.DEFAULT_STYLE;
                if (updated.getValue(FluixCrystalEnergyHatchBlock.STYLE) != style) {
                    updated = updated.setValue(FluixCrystalEnergyHatchBlock.STYLE, style);
                }
                if (updated != state) {
                    level.setBlock(pos, updated, Block.UPDATE_ALL);
                }
                continue;
            }
            if (!isPart(state)) {
                continue;
            }
            BlockState updated = state;
            if (updated.getValue(MachinePartBlock.FORMED) != formed) {
                updated = updated.setValue(MachinePartBlock.FORMED, formed);
            }
            // 反应仓成型外观：水晶机壳
            int style = formed ? MachinePartFacade.CRYSTAL.style() : MachinePartBlock.DEFAULT_STYLE;
            if (updated.getValue(MachinePartBlock.STYLE) != style) {
                updated = updated.setValue(MachinePartBlock.STYLE, style);
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }

    private static boolean isPart(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()
                || block == ModBlocks.AEV_INPUT_BUS.get()
                || block == ModBlocks.AEV_OUTPUT_BUS.get()
                || block == ModBlocks.AEV_INPUT_HATCH.get()
                || block == ModBlocks.AEV_OUTPUT_HATCH.get();
    }
}
