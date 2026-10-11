package com.murthinext.ae2pr.multiblock.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.StructureResult;

/**
 * 高反应性熔岩冶炼炉结构定义与外观同步。
 */
public final class LavaSmelterStructure {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            .origin(0, 0)
            .slice("CBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#PB#", "#P##", "#P##")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#BB#", "####", "####")
            .slice("BBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .where('C', StructurePredicate.blocks(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()))
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
            .where('P', StructurePredicate.blocks(ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('#', StructurePredicate.any())
            .build();

    public static final int CAVITY_SIZE = 2;

    private LavaSmelterStructure() {
    }

    /**
     * 同步结构内输入/输出总线的外观。
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
            // 冶炼炉成型外观：锆刚玉砖块
            int style = formed ? MachinePartFacade.ZIRCONIA.style() : MachinePartBlock.DEFAULT_STYLE;
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

    /**
     * 炉膛底面 2×2 池面的世界坐标，用于假熔岩渲染与伤害判定。
     */
    public static List<BlockPos> poolCells(BlockPos controllerPos, Direction facing, boolean mirrorSide,
            boolean mirrorFront) {
        Direction sliceFacing = RelativeDirection.RIGHT.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
        Direction charFacing = RelativeDirection.BACK.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
        List<BlockPos> cells = new ArrayList<>(CAVITY_SIZE * CAVITY_SIZE);
        for (int slice = 1; slice <= CAVITY_SIZE; slice++) {
            for (int col = 1; col <= CAVITY_SIZE; col++) {
                cells.add(controllerPos.relative(sliceFacing, slice).relative(Direction.UP, 1)
                        .relative(charFacing, col));
            }
        }
        return cells;
    }

    /**
     * 炉膛空腔（底面 2×2、高 2）的世界包围盒，用于熔岩伤害判定。
     */
    public static AABB cavityAabb(BlockPos controllerPos, Direction facing, boolean mirrorSide,
            boolean mirrorFront) {
        List<BlockPos> cells = poolCells(controllerPos, facing, mirrorSide, mirrorFront);
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int y = cells.get(0).getY();
        for (BlockPos cell : cells) {
            minX = Math.min(minX, cell.getX());
            minZ = Math.min(minZ, cell.getZ());
            maxX = Math.max(maxX, cell.getX());
            maxZ = Math.max(maxZ, cell.getZ());
        }
        return new AABB(minX, y, minZ, maxX + 1, y + CAVITY_SIZE, maxZ + 1);
    }
}
