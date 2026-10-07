package com.murthinext.ae2pr.multiblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 结构缓存：记录成型时各单元格的方块类型与方块实体，用于快速校验结构是否仍然有效。
 * <p>
 * 只比较方块类型与方块实体引用，不比较方块状态：成型 / 运行时会自行切换外观状态
 * （控制器 FORMED/RUNNING/PAUSED、部件 FORMED、控制外壳 ACTIVE），这些不应导致缓存失效。
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

    /** 以当前世界状态重建缓存。 */
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

    /** 缓存中的方块与方块实体是否全部未变化。 */
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
