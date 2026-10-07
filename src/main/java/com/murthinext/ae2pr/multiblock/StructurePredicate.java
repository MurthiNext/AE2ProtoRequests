package com.murthinext.ae2pr.multiblock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 结构单元格谓词：判断某位置是否匹配，并支持整结构数量上下限。
 * <p>
 * {@link #test} 返回实际命中的叶子谓词（{@link #or} 组合只返回第一个命中的分支），匹配器据此计数。
 */
public abstract class StructurePredicate {

    private int minCount;
    private int maxCount = -1;
    private int count;

    /** 整结构最小出现次数。 */
    public StructurePredicate minCount(int minCount) {
        this.minCount = minCount;
        return this;
    }

    /** 整结构最大出现次数；-1 表示不限制。 */
    public StructurePredicate maxCount(int maxCount) {
        this.maxCount = maxCount;
        return this;
    }

    public int minCount() {
        return minCount;
    }

    public int maxCount() {
        return maxCount;
    }

    int count() {
        return count;
    }

    void resetCount() {
        count = 0;
    }

    void incrementCount() {
        count++;
    }

    /** 测试并返回命中的叶子谓词；不匹配返回 null。 */
    @Nullable
    public abstract StructurePredicate test(Level level, BlockPos pos, BlockState state);

    /** 匹配任意方块（不参与计数与诊断）。 */
    public boolean isAny() {
        return false;
    }

    /** 或组合。 */
    public StructurePredicate or(StructurePredicate other) {
        return new Or(List.of(this, other));
    }

    /** 全部叶子谓词，用于重置计数与数量校验。 */
    public List<StructurePredicate> leaves() {
        return List.of(this);
    }

    public static StructurePredicate blocks(Block... blocks) {
        return new Blocks(new HashSet<>(List.of(blocks)));
    }

    public static StructurePredicate any() {
        return Any.INSTANCE;
    }

    /** 方块集合谓词。 */
    private static final class Blocks extends StructurePredicate {

        private final Set<Block> blocks;

        private Blocks(Set<Block> blocks) {
            this.blocks = blocks;
        }

        @Nullable
        @Override
        public StructurePredicate test(Level level, BlockPos pos, BlockState state) {
            return blocks.contains(state.getBlock()) ? this : null;
        }
    }

    /** 或组合谓词：返回第一个命中的叶子。 */
    private static final class Or extends StructurePredicate {

        private final List<StructurePredicate> parts;

        private Or(List<StructurePredicate> parts) {
            this.parts = parts;
        }

        @Nullable
        @Override
        public StructurePredicate test(Level level, BlockPos pos, BlockState state) {
            for (StructurePredicate part : parts) {
                StructurePredicate matched = part.test(level, pos, state);
                if (matched != null) {
                    return matched;
                }
            }
            return null;
        }

        @Override
        public List<StructurePredicate> leaves() {
            List<StructurePredicate> result = new ArrayList<>();
            for (StructurePredicate part : parts) {
                result.addAll(part.leaves());
            }
            return result;
        }
    }

    /** 任意方块谓词。 */
    private static final class Any extends StructurePredicate {

        private static final Any INSTANCE = new Any();

        @Nullable
        @Override
        public StructurePredicate test(Level level, BlockPos pos, BlockState state) {
            return this;
        }

        @Override
        public boolean isAny() {
            return true;
        }
    }
}
