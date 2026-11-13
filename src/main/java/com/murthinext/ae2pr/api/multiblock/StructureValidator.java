package com.murthinext.ae2pr.api.multiblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

// 控制器侧的结构验证器：比较方块类型、朝向属性与方块实体引用，不比较连接纹理等形状状态
public final class StructureValidator {

    private final MultiblockPattern pattern;
    private final Map<BlockPos, Snapshot> blocks = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    @Nullable
    private StructureResult lastFormed;

    // 成型时的方块快照：类型 + 朝向，忽略成型换装会改动的其它属性
    private record Snapshot(Block block, @Nullable Direction facing, @Nullable Direction.Axis axis) {

        private static Snapshot of(BlockState state) {
            Direction facing = state.hasProperty(BlockStateProperties.FACING)
                    ? state.getValue(BlockStateProperties.FACING)
                    : state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : null;
            Direction.Axis axis = state.hasProperty(BlockStateProperties.AXIS)
                    ? state.getValue(BlockStateProperties.AXIS)
                    : state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
                    ? state.getValue(BlockStateProperties.HORIZONTAL_AXIS)
                    : null;
            return new Snapshot(state.getBlock(), facing, axis);
        }

        private boolean matches(BlockState state) {
            Snapshot current = of(state);
            return block == current.block && facing == current.facing && axis == current.axis;
        }
    }

    public StructureValidator(MultiblockPattern pattern) {
        this.pattern = pattern;
    }

    // 验证结构；{@link StructureResult#fromCache()} 为 true 时无需刷新外观与部件引用
    public StructureResult validate(Level level, BlockPos controllerPos, Direction front, Direction up, int maxSlices) {
        if (lastFormed != null && verified(level)) {
            return lastFormed.asCached();
        }
        StructureResult result = pattern.match(level, controllerPos, front, up, maxSlices);
        if (result.formed()) {
            lastFormed = result;
            capture(level, result.cells());
        } else {
            invalidate();
        }
        return result;
    }

    public void invalidate() {
        lastFormed = null;
        blocks.clear();
        blockEntities.clear();
    }

    private void capture(Level level, List<BlockPos> cells) {
        blocks.clear();
        blockEntities.clear();
        for (BlockPos pos : cells) {
            blocks.put(pos, Snapshot.of(level.getBlockState(pos)));
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntities.put(pos, blockEntity);
            }
        }
    }

    private boolean verified(Level level) {
        for (Map.Entry<BlockPos, Snapshot> entry : blocks.entrySet()) {
            if (!entry.getValue().matches(level.getBlockState(entry.getKey()))) {
                return false;
            }
        }
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            if (level.getBlockEntity(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}
