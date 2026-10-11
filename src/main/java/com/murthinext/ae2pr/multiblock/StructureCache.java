package com.murthinext.ae2pr.multiblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 结构缓存：只比较方块类型与方块实体引用，不比较方块状态。
 */
public final class StructureCache {

    private final Map<BlockPos, Block> blocks = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    public void clear() {
        blocks.clear();
        blockEntities.clear();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public void capture(Level level, List<BlockPos> cells) {
        clear();
        for (BlockPos pos : cells) {
            blocks.put(pos, level.getBlockState(pos).getBlock());
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntities.put(pos, blockEntity);
            }
        }
    }

    public boolean verify(Level level) {
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
