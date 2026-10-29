package com.murthinext.ae2pr.multiblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.multiblock.module.ModuleInstance;
import com.murthinext.ae2pr.multiblock.module.ModuleProvider;
import com.murthinext.ae2pr.multiblock.module.ModuleType;

// 结构单元格谓词：判断某位置是否匹配，支持整结构数量上下限
public abstract class StructurePredicate {

    private int minCount;
    private int maxCount = -1;
    private int count;

    public StructurePredicate minCount(int minCount) {
        this.minCount = minCount;
        return this;
    }

    // 整结构最大出现次数；-1 表示不限制
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

    // 测试并返回命中的叶子谓词；不匹配返回 null
    @Nullable
    public abstract StructurePredicate test(Level level, BlockPos pos, BlockState state);

    public void collectModules(BlockPos pos, BlockState state, List<ModuleInstance> modules) {
    }

    // 匹配任意方块，不参与计数与诊断
    public boolean isAny() {
        return false;
    }

    public StructurePredicate or(StructurePredicate other) {
        return new Or(List.of(this, other));
    }

    // 全部叶子谓词，用于重置计数与数量校验
    public List<StructurePredicate> leaves() {
        return List.of(this);
    }

    public List<Block> previewBlocks() {
        return List.of();
    }

    public static StructurePredicate blocks(Block... blocks) {
        return new Blocks(new LinkedHashSet<>(List.of(blocks)));
    }

    // 机壳格：接受机壳或任意仓室，ME 接入仓最多 1 个
    public static StructurePredicate casingWithParts(Block casing) {
        StructurePredicate predicate = blocks(casing);
        for (Block part : MachinePartBlock.allParts()) {
            predicate = predicate.or(blocks(part));
        }
        return predicate.or(blocks(ModBlocks.ME_NETWORK_ACCESS_HATCH.get()).maxCount(1));
    }

    // 模块插槽：接受基础方块或提供允许功能的模块方块
    public static StructurePredicate moduleSlot(Block base, ModuleType... allowed) {
        return new ModuleSlot(base, Set.of(allowed));
    }

    public static StructurePredicate any() {
        return Any.INSTANCE;
    }

    // 方块集合谓词
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

        @Override
        public List<Block> previewBlocks() {
            return List.copyOf(blocks);
        }
    }

    // 模块插槽谓词：基础方块不计模块，模块方块计入并收集
    private static final class ModuleSlot extends StructurePredicate {

        private final Block base;
        private final Set<ModuleType> allowed;

        private ModuleSlot(Block base, Set<ModuleType> allowed) {
            this.base = base;
            this.allowed = allowed;
        }

        @Nullable
        @Override
        public StructurePredicate test(Level level, BlockPos pos, BlockState state) {
            if (state.is(base)) {
                return this;
            }
            return state.getBlock() instanceof ModuleProvider provider
                    && allowed.contains(provider.moduleDefinition().type()) ? this : null;
        }

        @Override
        public void collectModules(BlockPos pos, BlockState state, List<ModuleInstance> modules) {
            if (state.getBlock() instanceof ModuleProvider provider) {
                modules.add(new ModuleInstance(pos, provider.moduleDefinition()));
            }
        }

        @Override
        public List<Block> previewBlocks() {
            List<Block> result = new ArrayList<>();
            result.add(base);
            BuiltInRegistries.BLOCK.stream()
                    .filter(block -> block instanceof ModuleProvider provider
                            && allowed.contains(provider.moduleDefinition().type()))
                    .sorted(Comparator.comparingInt((Block block) ->
                            ((ModuleProvider) block).moduleDefinition().level())
                            .thenComparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                    .forEach(result::add);
            return List.copyOf(result);
        }
    }

    // 或组合谓词：返回第一个命中的叶子
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

        @Override
        public List<Block> previewBlocks() {
            return parts.stream().flatMap(part -> part.previewBlocks().stream()).distinct().toList();
        }
    }

    // 任意方块谓词
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
