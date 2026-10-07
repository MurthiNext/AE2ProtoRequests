package com.murthinext.ae2pr.multiblock;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * 结构匹配结果。
 *
 * @param formed      是否成型
 * @param slices      成型时的片数（未成型为 0）
 * @param mirrorSide  匹配到的左右镜像
 * @param mirrorFront 匹配到的前后镜像
 * @param mismatches  不符方块数量（用于诊断）
 * @param mismatchPos 第一个不符方块的位置
 * @param expected    该位置期望的字符
 * @param found       该位置实际方块
 * @param cells       成型结构内的单元格（不含任意方块位，顺序为 片 → 行 → 列）
 * @param fromCache   结果来自缓存（结构未变化）
 */
public record StructureResult(boolean formed, int slices, boolean mirrorSide, boolean mirrorFront, int mismatches,
        @Nullable BlockPos mismatchPos, char expected, @Nullable Block found, List<BlockPos> cells,
        boolean fromCache) {

    public static final StructureResult EMPTY = new StructureResult(false, 0, false, false, 0, null, ' ', null,
            List.of(), false);

    /** 缓存命中版本。 */
    public StructureResult asCached() {
        return new StructureResult(formed, slices, mirrorSide, mirrorFront, mismatches, mismatchPos, expected, found,
                cells, true);
    }
}
