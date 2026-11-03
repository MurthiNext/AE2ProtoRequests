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

// 控制器侧的结构验证器：只比较方块类型与方块实体引用，不比较方块状态
public final class StructureValidator {

    private final MultiblockPattern pattern;
    private final Map<BlockPos, Block> blocks = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    @Nullable
    private StructureResult lastFormed;

    public StructureValidator(MultiblockPattern pattern) {
        this.pattern = pattern;
    }

    // 验证结构；{@link StructureResult#fromCache()} 为 true 时无需刷新外观与部件引用
    public StructureResult validate(Level level, BlockPos controllerPos, Direction front, Direction up,
            int maxSlices) {
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
            blocks.put(pos, level.getBlockState(pos).getBlock());
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntities.put(pos, blockEntity);
            }
        }
    }

    private boolean verified(Level level) {
        for (Map.Entry<BlockPos, Block> entry : blocks.entrySet()) {
            if (level.getBlockState(entry.getKey()).getBlock() != entry.getValue()) {
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
