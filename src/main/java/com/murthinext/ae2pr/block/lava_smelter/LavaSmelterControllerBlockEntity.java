package com.murthinext.ae2pr.block.lava_smelter;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.StructureValidator;

/**
 * 高反应性熔岩冶炼炉主机方块实体：结构检测与外观状态。
 * <p>
 * 结构检测由 {@link StructureValidator} 完成：结构未变化时走缓存快速路径。
 * 运行 / 暂停外观（{@link HighReactivityLavaSmelterBlock#RUNNING} 与
 * {@link HighReactivityLavaSmelterBlock#PAUSED}）已就绪，供后续加工逻辑通过
 * {@link #setJobState(boolean, boolean)} 驱动；当前仅结构检测生效。
 */
public class LavaSmelterControllerBlockEntity extends BlockEntity {

    /** 结构检测周期（tick） */
    private static final int CHECK_INTERVAL = 20;

    private int tickCounter;

    /** 最近一次检测结果（供状态展示） */
    private int lastMismatches;
    @Nullable
    private BlockPos lastMismatchPos;

    /** 结构验证器：模式匹配 + 缓存快速路径 */
    private final StructureValidator structure = new StructureValidator(LavaSmelterStructure.PATTERN);

    public LavaSmelterControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIGH_REACTIVITY_LAVA_SMELTER.get(), pos, state);
    }

    /** 服务端 tick：每 {@link #CHECK_INTERVAL} tick 重新检测结构。 */
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            validateStructure();
        }
    }

    /** 立即检测一次结构并刷新主机外观；结构未变化时走缓存快速路径。 */
    public void validateStructure() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get())) {
            return;
        }
        Direction facing = state.getValue(HighReactivityLavaSmelterBlock.FACING);
        StructureResult result = structure.validate(level, worldPosition, facing, Direction.UP,
                LavaSmelterStructure.PATTERN.minSlices());
        if (result.fromCache()) {
            // 缓存命中：结构未变化，无需刷新外观
            return;
        }
        lastMismatches = result.mismatches();
        lastMismatchPos = result.mismatchPos();
        applyFormed(result.formed());
    }

    /** 更新成型标记；失活时同步清除运行 / 暂停外观。 */
    private void applyFormed(boolean formed) {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        BlockState updated = state;
        if (updated.getValue(HighReactivityLavaSmelterBlock.FORMED) != formed) {
            updated = updated.setValue(HighReactivityLavaSmelterBlock.FORMED, formed);
        }
        if (!formed) {
            updated = updated.setValue(HighReactivityLavaSmelterBlock.RUNNING, false);
            updated = updated.setValue(HighReactivityLavaSmelterBlock.PAUSED, false);
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    /** 供加工逻辑调用：切换运行 / 暂停外观（未成型时忽略）。 */
    public void setJobState(boolean running, boolean paused) {
        Level level = this.level;
        if (level == null || level.isClientSide || !isFormed()) {
            return;
        }
        BlockState state = getBlockState();
        BlockState updated = state
                .setValue(HighReactivityLavaSmelterBlock.RUNNING, running)
                .setValue(HighReactivityLavaSmelterBlock.PAUSED, paused && !running);
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    /** 控制器被移除 / 破坏时调用：清空结构缓存。 */
    public void onControllerRemoved() {
        structure.invalidate();
    }

    /** 是否已成型。 */
    public boolean isFormed() {
        return getBlockState().getValue(HighReactivityLavaSmelterBlock.FORMED);
    }

    /** 最近一次检测的不符方块数量。 */
    public int getLastMismatches() {
        return lastMismatches;
    }

    /** 最近一次检测的首个不符位置（可能为空）。 */
    @Nullable
    public BlockPos getLastMismatchPos() {
        return lastMismatchPos;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测（不持久化），交给首次 tick 完成，避免加载期修改世界
            tickCounter = CHECK_INTERVAL - 1;
        }
    }
}
