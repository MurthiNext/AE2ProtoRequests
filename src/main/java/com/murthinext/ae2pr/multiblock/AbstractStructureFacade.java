package com.murthinext.ae2pr.multiblock;

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

/**
 * 多方块结构定义与成型外观同步的公共基底。
 */
public abstract class AbstractStructureFacade {

    protected final MultiblockPattern pattern;
    protected final MachinePartFacade facade;

    protected AbstractStructureFacade(MultiblockPattern pattern, MachinePartFacade facade) {
        this.pattern = pattern;
        this.facade = facade;
    }

    public MultiblockPattern pattern() {
        return pattern;
    }

    public MachinePartFacade facade() {
        return facade;
    }

    /**
     * 允许的最大片数，可重复片的结构需要覆盖。
     */
    public int maxSlices() {
        return pattern.minSlices();
    }

    /**
     * 同步结构内部件的外观；未成型时逐一复位全部镜像组合。
     *
     * @param active 机器是否处于运行状态，供需要工作外观的结构使用
     */
    public void updateFormed(Level level, BlockPos controllerPos, Direction facing, StructureResult result,
            boolean active) {
        if (result.formed()) {
            applyParts(level, pattern.cells(controllerPos, facing, Direction.UP, result.mirrorSide(),
                    result.mirrorFront(), result.slices()), result.slices(), true, active);
            return;
        }
        for (boolean[] mirror : MultiblockPattern.MIRRORS) {
            applyParts(level, pattern.cells(controllerPos, facing, Direction.UP, mirror[0], mirror[1],
                    maxSlices()), 0, false, false);
        }
    }

    protected void applyParts(Level level, List<BlockPos> cells, int slices, boolean formed, boolean active) {
        int style = formed ? facade.style() : MachinePartBlock.DEFAULT_STYLE;
        for (BlockPos pos : cells) {
            onPartCell(level, pos, active);
            BlockState state = level.getBlockState(pos);
            BlockState updated = state;
            if (state.is(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get())) {
                if (state.getValue(FluixCrystalEnergyHatchBlock.FORMED) != formed) {
                    updated = updated.setValue(FluixCrystalEnergyHatchBlock.FORMED, formed);
                }
                if (updated.getValue(FluixCrystalEnergyHatchBlock.STYLE) != style) {
                    updated = updated.setValue(FluixCrystalEnergyHatchBlock.STYLE, style);
                }
            } else if (isPart(state)) {
                if (state.getValue(MachinePartBlock.FORMED) != formed) {
                    updated = updated.setValue(MachinePartBlock.FORMED, formed);
                }
                if (updated.getValue(MachinePartBlock.STYLE) != style) {
                    updated = updated.setValue(MachinePartBlock.STYLE, style);
                }
            } else {
                continue;
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }

    /**
     * 每个结构单元格刷新前的额外处理，例如装配线的控制外壳工作外观。
     */
    protected void onPartCell(Level level, BlockPos pos, boolean active) {
    }

    /**
     * 结构零件：普通机器部件，能源仓单独处理。
     */
    protected boolean isPart(BlockState state) {
        return isCommonPart(state.getBlock());
    }

    public static boolean isCommonPart(Block block) {
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
