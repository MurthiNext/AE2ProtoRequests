package com.murthinext.ae2pr.multiblock.structure;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.StructureResult;

/**
 * 物质相变仪结构定义与外观同步。
 */
public final class MatterPhaseTransformerStructure {

    /** 结构模式：主机位于第 0 行、第 2 列。 */
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
                    .or(StructurePredicate.blocks(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).maxCount(1)))
            .where('G', StructurePredicate.blocks(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('K', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_CORE.get()))
            .where('#', StructurePredicate.any())
            .build();

    private MatterPhaseTransformerStructure() {
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
        // 未成型：不确定此前是哪种镜像布局，逐一复位全部组合
        for (boolean[] mirror : MultiblockPattern.MIRRORS) {
            applyParts(level, PATTERN.cells(controllerPos, facing, Direction.UP, mirror[0], mirror[1],
                    PATTERN.minSlices()), false);
        }
    }

    private static void applyParts(Level level, List<BlockPos> cells, boolean formed) {
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get())) {
                // 能源仓：切换成型外观为锆刚玉砖块
                BlockState updated = state;
                if (state.getValue(FluixCrystalEnergyHatchBlock.FORMED) != formed) {
                    updated = updated.setValue(FluixCrystalEnergyHatchBlock.FORMED, formed);
                }
                int style = formed ? MachinePartFacade.ZIRCONIA.style() : MachinePartBlock.DEFAULT_STYLE;
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
            // 相变仪成型外观：锆刚玉砖块；未成型时回默认值
            int style = formed ? MachinePartFacade.ZIRCONIA.style() : MachinePartBlock.DEFAULT_STYLE;
            if (updated.getValue(MachinePartBlock.STYLE) != style) {
                updated = updated.setValue(MachinePartBlock.STYLE, style);
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }

    /** 可占据砖块位的机器部件（石英 / AEV 的输入、输出总线与输入、输出仓）。 */
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
