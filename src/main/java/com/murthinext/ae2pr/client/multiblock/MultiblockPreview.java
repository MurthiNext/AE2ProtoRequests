package com.murthinext.ae2pr.client.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.block.meteor_steel_pipe.MeteorSteelPipeBlock;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.module.ModuleInstance;
import com.murthinext.ae2pr.multiblock.module.ModuleProvider;
import com.murthinext.ae2pr.multiblock.module.ModuleSnapshot;

/**
 * 只读预览世界，保留完整材料并按层提供渲染邻居。
 */
public final class MultiblockPreview implements BlockAndTintGetter {

    public record Cell(BlockPos pos, StructurePredicate predicate, List<Block> candidates) {
    }

    private final MultiblockPattern pattern;
    private final LevelLightEngine lightEngine;
    private final Map<StructurePredicate, List<Block>> candidates = new HashMap<>();
    private final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
    private List<Cell> cells = List.of();
    private List<ItemStack> materials = List.of();
    private AABB bounds = new AABB(BlockPos.ZERO);
    private int slices;
    private int layer = -1;

    public MultiblockPreview(MultiblockPattern pattern, LevelLightEngine lightEngine) {
        this.pattern = pattern;
        this.lightEngine = lightEngine;
        setSlices(pattern.minSlices());
    }

    public void setSlices(int slices) {
        this.slices = Mth.clamp(slices, pattern.minSlices(), pattern.maxSlices());
        Map<BlockPos, BlockState> previous = new HashMap<>(states);
        states.clear();
        List<Cell> result = new ArrayList<>();
        for (MultiblockPattern.PatternCell cell : pattern.patternCells(BlockPos.ZERO, Direction.NORTH, Direction.UP,
                false, false, this.slices)) {
            StructurePredicate predicate = pattern.predicate(cell.symbol());
            List<Block> blocks = candidates.computeIfAbsent(predicate, key -> key.previewBlocks().stream()
                    .filter(block -> !block.defaultBlockState().isAir()).toList());
            if (blocks.isEmpty()) {
                continue;
            }
            Cell previewCell = new Cell(cell.pos(), predicate, blocks);
            result.add(previewCell);
            states.put(cell.pos(), blocks.get(0).defaultBlockState());
            for (StructurePredicate leaf : predicate.leaves()) {
                candidates.computeIfAbsent(leaf, StructurePredicate::previewBlocks);
            }
        }
        cells = List.copyOf(result);
        int minX = states.keySet().stream().mapToInt(BlockPos::getX).min().orElse(0);
        int minY = states.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0);
        int minZ = states.keySet().stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxX = states.keySet().stream().mapToInt(BlockPos::getX).max().orElse(0);
        int maxY = states.keySet().stream().mapToInt(BlockPos::getY).max().orElse(0);
        int maxZ = states.keySet().stream().mapToInt(BlockPos::getZ).max().orElse(0);
        bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        for (Cell cell : cells) {
            BlockState old = previous.get(cell.pos());
            Block block = old != null && cell.candidates().contains(old.getBlock())
                    ? old.getBlock() : cell.candidates().get(0);
            states.put(cell.pos(), stateFor(cell, block));
        }
        setLayer(layer);
        updateMaterials();
    }

    public List<Cell> cells() {
        return cells;
    }

    public int slices() {
        return slices;
    }

    public AABB bounds() {
        return bounds;
    }

    public int layer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = Mth.clamp(layer, -1, getHeight() - 1);
    }

    public boolean isVisible(BlockPos pos) {
        return states.containsKey(pos) && (layer < 0 || pos.getY() == getMinBuildHeight() + layer);
    }

    public List<ItemStack> materials() {
        return materials;
    }

    public boolean canReplace(Cell selected, Block block) {
        if (!selected.candidates().contains(block)) {
            return false;
        }
        Map<StructurePredicate, Integer> counts = new HashMap<>();
        List<ModuleInstance> modules = new ArrayList<>();
        for (Cell cell : cells) {
            Block current = cell.pos().equals(selected.pos()) ? block : states.get(cell.pos()).getBlock();
            for (StructurePredicate leaf : cell.predicate().leaves()) {
                if (candidates.get(leaf).contains(current)) {
                    int count = counts.merge(leaf, 1, Integer::sum);
                    if (leaf.maxCount() >= 0 && count > leaf.maxCount()) {
                        return false;
                    }
                    break;
                }
            }
            if (current instanceof ModuleProvider provider) {
                modules.add(new ModuleInstance(cell.pos(), provider.moduleDefinition()));
            }
        }
        for (Map.Entry<StructurePredicate, List<Block>> entry : candidates.entrySet()) {
            if (entry.getKey().minCount() > counts.getOrDefault(entry.getKey(), 0)) {
                return false;
            }
        }
        return ModuleSnapshot.firstConflict(modules) == null;
    }

    public boolean replace(Cell selected, Block block) {
        if (!canReplace(selected, block)) {
            return false;
        }
        states.put(selected.pos(), stateFor(selected, block));
        updateMaterials();
        return true;
    }

    /**
     * 把玩家替换过的方块恢复为默认方块。
     */
    public void resetBlocks() {
        for (Cell cell : cells) {
            states.put(cell.pos(), stateFor(cell, cell.candidates().get(0)));
        }
        updateMaterials();
    }

    private BlockState stateFor(Cell cell, Block block) {
        BlockState state = block.defaultBlockState();
        Direction facing = Direction.NORTH;
        if (block instanceof MeteorSteelPipeBlock) {
            facing = Direction.UP;
        } else if (!cell.pos().equals(BlockPos.ZERO)) {
            double x = cell.pos().getX() + 0.5 - bounds.getCenter().x;
            double z = cell.pos().getZ() + 0.5 - bounds.getCenter().z;
            facing = Math.abs(x) > Math.abs(z) ? (x < 0 ? Direction.WEST : Direction.EAST)
                    : (z < 0 ? Direction.NORTH : Direction.SOUTH);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, facing);
        }
        return state;
    }

    private void updateMaterials() {
        Map<Block, Integer> totals = new LinkedHashMap<>();
        BlockState controller = states.get(BlockPos.ZERO);
        if (controller != null && controller.getBlock().asItem() != Items.AIR) {
            totals.put(controller.getBlock(), 0);
        }
        for (BlockState state : states.values()) {
            if (state.getBlock().asItem() != Items.AIR) {
                totals.merge(state.getBlock(), 1, Integer::sum);
            }
        }
        materials = totals.entrySet().stream().map(entry -> new ItemStack(entry.getKey(), entry.getValue())).toList();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return isVisible(pos) ? states.get(pos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getHeight() {
        return (int) bounds.getYsize();
    }

    @Override
    public int getMinBuildHeight() {
        return (int) bounds.minY;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        if (!shade) {
            return 1.0F;
        }
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return lightEngine;
    }

    @Override
    public int getBrightness(LightLayer lightLayer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int skyDarken) {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return 0xFFFFFF;
    }
}
