package com.murthinext.ae2pr.multiblock.structure;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModModules;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineUnitBlock;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.StructureResult;

/**
 * 水晶装配线结构定义与外观同步。
 */
public final class AssemblyLineStructure {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            .origin(2, 0)
            .slice("FIF", "RTR", "SAG", "#Y#")
            .sliceRepeatable(3, Integer.MAX_VALUE, "FIF", "RTR", "DAG", "#Y#")
            .slice("FOF", "RTR", "DAG", "#Y#")
            .where('S', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()))
            // 机壳位允许输入仓 / 能源仓替代，能源仓最多 1 个
            .where('F', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).maxCount(1)))
            .where('Y', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()))
            .where('I', StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()))
            .where('O', StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()))
            .where('A', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CASING.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()))
            .where('D', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()))
            .where('R', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            // 控制外壳位：外壳或允许的替换模块
            .where('T', StructurePredicate.moduleSlot(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT.get(),
                    ModModules.CHARGING, ModModules.PARALLEL, ModModules.SPEED))
            .where('#', StructurePredicate.any())
            .build();

    public static final int MIN_SLICES = PATTERN.minSlices();

    private AssemblyLineStructure() {
    }

    public static int maxSlices() {
        return Math.max(MIN_SLICES, Config.assemblyLineMaxSlices());
    }

    /**
     * 同步结构内“状态型”方块的外观。
     */
    public static void updateFormed(Level level, BlockPos controllerPos, Direction facing, StructureResult result,
            boolean running) {
        if (result.formed()) {
            applyState(level, PATTERN.cells(controllerPos, facing, Direction.UP, result.mirrorSide(),
                    result.mirrorFront(), result.slices()), true, running);
            return;
        }
        // 未成型：逐一复位全部镜像组合
        for (boolean[] mirror : MultiblockPattern.MIRRORS) {
            applyState(level, PATTERN.cells(controllerPos, facing, Direction.UP, mirror[0], mirror[1], maxSlices()),
                    false, false);
        }
    }

    private static void applyState(Level level, List<BlockPos> cells, boolean partsFormed, boolean unitActive) {
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            BlockState updated = null;
            if (state.getBlock() instanceof AssemblyLineUnitBlock) {
                // 普通控制外壳与替换模块共用工作态贴图
                if (state.getValue(AssemblyLineUnitBlock.ACTIVE) != unitActive) {
                    updated = state.setValue(AssemblyLineUnitBlock.ACTIVE, unitActive);
                }
            } else if (state.is(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get())) {
                BlockState candidate = state;
                if (candidate.getValue(FluixCrystalEnergyHatchBlock.FORMED) != partsFormed) {
                    candidate = candidate.setValue(FluixCrystalEnergyHatchBlock.FORMED, partsFormed);
                }
                // 由装配线成型时外观为水晶机壳
                int style = partsFormed ? MachinePartFacade.CRYSTAL.style() : MachinePartBlock.DEFAULT_STYLE;
                if (candidate.getValue(FluixCrystalEnergyHatchBlock.STYLE) != style) {
                    candidate = candidate.setValue(FluixCrystalEnergyHatchBlock.STYLE, style);
                }
                if (candidate != state) {
                    updated = candidate;
                }
            } else if (isPart(state)) {
                BlockState candidate = state;
                if (candidate.getValue(MachinePartBlock.FORMED) != partsFormed) {
                    candidate = candidate.setValue(MachinePartBlock.FORMED, partsFormed);
                }
                // 由装配线成型时外观为水晶机壳
                int style = MachinePartFacade.CRYSTAL.style();
                if (candidate.getValue(MachinePartBlock.STYLE) != style) {
                    candidate = candidate.setValue(MachinePartBlock.STYLE, style);
                }
                if (candidate != state) {
                    updated = candidate;
                }
            }
            if (updated != null) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }

    private static boolean isPart(BlockState state) {
        return state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get())
                || state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get())
                || state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get());
    }
}
