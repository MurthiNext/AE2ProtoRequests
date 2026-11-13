package com.murthinext.ae2pr.api.multiblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.murthinext.ae2pr.api.multiblock.module.ModuleInstance;
import com.murthinext.ae2pr.api.multiblock.module.ModuleProvider;
import com.murthinext.ae2pr.api.multiblock.module.ModuleType;

// 结构单元格谓词：判断某位置是否匹配，支持整结构数量上下限与可选的朝向要求
public abstract class StructurePredicate {

    private int minCount;
    private int maxCount = -1;
    private int count;
    private final List<RelativeDirection> facings = new ArrayList<>();
    private final List<RelativeDirection> axes = new ArrayList<>();

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

    // 方块朝向必须是这些相对方向之一
    public StructurePredicate facing(RelativeDirection... allowed) {
        facings.addAll(List.of(allowed));
        return this;
    }

    // 方块朝向轴必须是这些相对方向的轴之一
    public StructurePredicate axis(RelativeDirection... allowed) {
        axes.addAll(List.of(allowed));
        return this;
    }

    public boolean hasOrientation() {
        return !facings.isEmpty() || !axes.isEmpty();
    }

    // 方块类型与数量是否命中，不考虑朝向
    @Nullable
    public abstract StructurePredicate testBlock(Level level, BlockPos pos, BlockState state);

    // 类型与朝向同时命中才算匹配
    @Nullable
    public StructurePredicate test(Level level, BlockPos pos, BlockState state, StructureOrientation orientation) {
        StructurePredicate matched = testBlock(level, pos, state);
        return matched != null && oriented(state, orientation) ? matched : null;
    }

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

    public BlockState orient(BlockState state, StructureOrientation orientation) {
        return applyAxes(applyFacing(state, orientation), orientation);
    }

    protected final List<RelativeDirection> facings() {
        return facings;
    }

    protected final List<RelativeDirection> axes() {
        return axes;
    }

    protected final boolean oriented(BlockState state, StructureOrientation orientation) {
        return facingMatched(state, orientation) && axisMatched(state, orientation);
    }

    private BlockState applyFacing(BlockState state, StructureOrientation orientation) {
        if (facings.isEmpty()) {
            return state;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, orientation.direction(facings.get(0)));
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            for (RelativeDirection relative : facings) {
                Direction direction = orientation.direction(relative);
                if (direction.getAxis().isHorizontal()) {
                    return state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                }
            }
        }
        return state;
    }

    private BlockState applyAxes(BlockState state, StructureOrientation orientation) {
        if (axes.isEmpty()) {
            return state;
        }
        Direction direction = orientation.direction(axes.get(0));
        Direction.Axis axis = direction.getAxis();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            return axis.isHorizontal() ? state.setValue(BlockStateProperties.HORIZONTAL_AXIS, axis) : state;
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.setValue(BlockStateProperties.AXIS, axis);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, direction);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && axis.isHorizontal()) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        }
        return state;
    }

    private boolean facingMatched(BlockState state, StructureOrientation orientation) {
        if (facings.isEmpty()) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return contains(orientation, facings, state.getValue(BlockStateProperties.FACING));
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return contains(orientation, facings, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return true;
    }

    private boolean axisMatched(BlockState state, StructureOrientation orientation) {
        if (axes.isEmpty()) {
            return true;
        }
        Set<Direction.Axis> allowed = EnumSet.noneOf(Direction.Axis.class);
        for (RelativeDirection relative : axes) {
            allowed.add(orientation.direction(relative).getAxis());
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            return allowed.contains(state.getValue(BlockStateProperties.HORIZONTAL_AXIS));
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return allowed.contains(state.getValue(BlockStateProperties.AXIS));
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return allowed.contains(state.getValue(BlockStateProperties.FACING).getAxis());
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return allowed.contains(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis());
        }
        return true;
    }

    private static boolean contains(StructureOrientation orientation, List<RelativeDirection> facings,
                                    Direction value) {
        for (RelativeDirection relative : facings) {
            if (orientation.direction(relative) == value) {
                return true;
            }
        }
        return false;
    }

    public static StructurePredicate blocks(Block... blocks) {
        return new Blocks(new LinkedHashSet<>(List.of(blocks)));
    }

    // 机壳格：接受机壳或任意仓室，ME 接入仓最多 1 个
    public static StructurePredicate casingWithParts(Block casing) {
        StructurePredicate predicate = blocks(casing);
        for (Block part : StructureHooks.parts()) {
            predicate = predicate.or(blocks(part));
        }
        Block networkAccessHatch = StructureHooks.networkAccessHatch();
        return networkAccessHatch == null ? predicate : predicate.or(blocks(networkAccessHatch).maxCount(1));
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
        public StructurePredicate testBlock(Level level, BlockPos pos, BlockState state) {
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
        public StructurePredicate testBlock(Level level, BlockPos pos, BlockState state) {
            if (state.is(base)) {
                return this;
            }
            return state.getBlock() instanceof ModuleProvider provider &&
                            allowed.contains(provider.moduleDefinition().type())
                    ? this
                    : null;
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
                    .filter(block
                            -> block instanceof ModuleProvider provider &&
                                    allowed.contains(provider.moduleDefinition().type()))
                    .sorted(Comparator
                                    .comparingInt((Block block) -> ((ModuleProvider) block).moduleDefinition().level())
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
        public StructurePredicate testBlock(Level level, BlockPos pos, BlockState state) {
            for (StructurePredicate part : parts) {
                StructurePredicate matched = part.testBlock(level, pos, state);
                if (matched != null) {
                    return matched;
                }
            }
            return null;
        }

        // 组自身与命中的叶子都需满足朝向要求
        @Nullable
        @Override
        public StructurePredicate test(Level level, BlockPos pos, BlockState state, StructureOrientation orientation) {
            if (!oriented(state, orientation)) {
                return null;
            }
            for (StructurePredicate part : parts) {
                StructurePredicate matched = part.test(level, pos, state, orientation);
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

        @Override
        public boolean hasOrientation() {
            return super.hasOrientation() || parts.stream().anyMatch(StructurePredicate::hasOrientation);
        }

        // 朝向要求可能挂在组内某一片上，取第一个有要求的叶子来摆姿态
        @Override
        public BlockState orient(BlockState state, StructureOrientation orientation) {
            if (!facings().isEmpty() || !axes().isEmpty()) {
                return super.orient(state, orientation);
            }
            for (StructurePredicate part : parts) {
                if (part.hasOrientation()) {
                    return part.orient(state, orientation);
                }
            }
            return state;
        }
    }

    // 任意方块谓词
    private static final class Any extends StructurePredicate {

        private static final Any INSTANCE = new Any();

        @Nullable
        @Override
        public StructurePredicate testBlock(Level level, BlockPos pos, BlockState state) {
            return this;
        }

        @Override
        public boolean isAny() {
            return true;
        }
    }
}
