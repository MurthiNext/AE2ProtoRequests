package com.murthinext.ae2pr.multiblock.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

// 高反应性熔岩冶炼炉结构定义
public final class LavaSmelterStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            .origin(0, 0)
            .slice("CBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#PB#", "#P##", "#P##")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#BB#", "####", "####")
            .slice("BBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .where('C', StructurePredicate.blocks(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()))
            .where('B', StructurePredicate.casingWithParts(ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get()))
            .where('P', StructurePredicate.blocks(ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('#', StructurePredicate.any())
            .build();

    public static final LavaSmelterStructure INSTANCE = new LavaSmelterStructure();

    public static final int CAVITY_SIZE = 2;

    private LavaSmelterStructure() {
        super(PATTERN, MachinePartFacade.ZIRCONIA);
    }

    // 炉膛底面 2×2 池面的世界坐标，用于假熔岩渲染与伤害判定
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

    // 炉膛空腔（底面 2×2、高 2）的世界包围盒，用于熔岩伤害判定
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
