package com.murthinext.ae2pr.block.crystal_reactor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

/**
 * 水晶反应器结构定义：以高能核心为中心的三乘三方块，形似旧版下界反应器。
 */
public final class CrystalReactorStructure {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.UP, RelativeDirection.BACK, RelativeDirection.RIGHT)
            .sliceOrigin(1)
            .origin(1, 1)
            .slice("CSC", "SSS", "CSC")
            .slice("S#S", "#K#", "S#S")
            .slice("#S#", "SSS", "#S#")
            .where('C', StructurePredicate.blocks(AEBlocks.QUARTZ_BLOCK.block()))
            .where('S', StructurePredicate.blocks(AEBlocks.SKY_STONE_BLOCK.block()))
            .where('K', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_CORE.get()))
            .where('#', StructurePredicate.any())
            .build();

    /** 外壳相对核心的偏移，共十八块。 */
    private static final List<BlockPos> SHELL_OFFSETS = buildShellOffsets();

    private static final Map<Long, List<BlockPos>> EXPANSION_CACHE = new HashMap<>();

    private CrystalReactorStructure() {
    }

    /**
     * 以核心为锚点校验三乘三结构。
     */
    public static boolean matches(Level level, BlockPos core) {
        return PATTERN.match(level, core, Direction.NORTH, Direction.UP, PATTERN.minSlices()).formed();
    }

    /**
     * 外壳相对核心的偏移。
     */
    public static List<BlockPos> shellOffsets() {
        return SHELL_OFFSETS;
    }

    /**
     * 扩散区域相对核心的偏移，按水平半径由内向外排序，已排除核心与外壳。
     */
    public static List<BlockPos> expansionOffsets() {
        int radius = Config.crystalReactorRadius();
        int depth = Config.crystalReactorDepth();
        long key = ((long) radius << 32) | (depth & 0xFFFFFFFFL);
        return EXPANSION_CACHE.computeIfAbsent(key, ignored -> buildExpansion(radius, depth));
    }

    private static List<BlockPos> buildShellOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                offsets.add(new BlockPos(dx, -1, dz));
            }
        }
        // 中层：四角（四边中点为空气，不参与置换）
        for (int dz = -1; dz <= 1; dz += 2) {
            for (int dx = -1; dx <= 1; dx += 2) {
                offsets.add(new BlockPos(dx, 0, dz));
            }
        }
        offsets.add(new BlockPos(0, 1, 0));
        offsets.add(new BlockPos(1, 1, 0));
        offsets.add(new BlockPos(-1, 1, 0));
        offsets.add(new BlockPos(0, 1, 1));
        offsets.add(new BlockPos(0, 1, -1));
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildExpansion(int radius, int depth) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int dy = 0; dy >= -depth; dy--) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    BlockPos offset = new BlockPos(dx, dy, dz);
                    if (SHELL_OFFSETS.contains(offset) || offset.equals(BlockPos.ZERO)) {
                        continue;
                    }
                    offsets.add(offset);
                }
            }
        }
        // 先按水平半径由内向外，再按核心层向下的顺序推进
        offsets.sort(Comparator
                .comparingInt((BlockPos pos) -> pos.getX() * pos.getX() + pos.getZ() * pos.getZ())
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ)
                .thenComparingInt(BlockPos::getX));
        return List.copyOf(offsets);
    }
}
