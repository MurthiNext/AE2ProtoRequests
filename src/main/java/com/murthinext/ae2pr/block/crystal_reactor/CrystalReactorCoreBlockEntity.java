package com.murthinext.ae2pr.block.crystal_reactor;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.HighEnergyCoreBlock;

/**
 * 高能核心方块实体，水晶反应器。
 */
public class CrystalReactorCoreBlockEntity extends BlockEntity {

    private static final String ACTIVE_ID = "active";
    private static final String PROGRESS_ID = "progress";
    private static final String ELAPSED_ID = "elapsed";
    private static final String DURATION_ID = "duration";

    private boolean active;
    private int progress;
    private int elapsed;
    private int duration = 1;
    private int soundBudget;

    public CrystalReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_REACTOR_CORE.get(), pos, state);
    }

    public boolean isActive() {
        return active;
    }

    // 空手右键触发
    public boolean activate() {
        Level level = this.level;
        if (level == null || level.isClientSide || active) {
            return false;
        }
        if (!CrystalReactorStructure.matches(level, worldPosition)) {
            return false;
        }
        // 外壳立刻置换为随机母岩，核心自身暂时保留以驱动扩散
        RandomSource random = level.getRandom();
        soundBudget = Config.crystalReactorBreakSoundsPerTick();
        for (BlockPos offset : CrystalReactorStructure.shellOffsets()) {
            replace(level, worldPosition.offset(offset), random);
        }
        duration = Math.max(1, Config.crystalReactorDurationTicks());
        progress = 0;
        elapsed = 0;
        active = true;
        setChanged();
        level.setBlock(worldPosition, getBlockState().setValue(HighEnergyCoreBlock.LIT, true), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.7F);
        return true;
    }

    public void serverTick() {
        Level level = this.level;
        if (level == null || level.isClientSide || !active) {
            return;
        }
        if (!getBlockState().is(ModBlocks.HIGH_ENERGY_CORE.get())) {
            active = false;
            return;
        }
        List<BlockPos> offsets = CrystalReactorStructure.expansionOffsets();
        int target = (int) Math.min(offsets.size(), (long) offsets.size() * ++elapsed / duration);
        RandomSource random = level.getRandom();
        soundBudget = Config.crystalReactorBreakSoundsPerTick();
        while (progress < target) {
            BlockPos pos = offsets.get(progress).offset(worldPosition);
            // 未加载的区块留待后续 tick 处理，避免静默跳过
            if (!level.isLoaded(pos)) {
                break;
            }
            replace(level, pos, random);
            progress++;
        }
        if (progress >= offsets.size()) {
            finish(level, random);
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 3, 1.5, 0.5, 1.5, 0.0);
        }
        setChanged();
    }

    private void replace(Level level, BlockPos pos, RandomSource random) {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return;
        }
        if (state.is(ModBlocks.HIGH_ENERGY_CORE.get())) {
            return;
        }
        if (soundBudget > 0) {
            soundBudget--;
            // 以被破坏方块自身的破坏音效回放，音调随机化
            level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.6F,
                    0.8F + random.nextFloat() * 0.4F);
        }
        level.setBlock(pos, CrystalBuddingBlocks.roll(random), Block.UPDATE_ALL);
    }

    private void finish(Level level, RandomSource random) {
        active = false;
        progress = 0;
        elapsed = 0;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        // 反应结束：核心自身也转为母岩
        level.setBlock(worldPosition, CrystalBuddingBlocks.roll(random), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(ACTIVE_ID, active);
        tag.putInt(PROGRESS_ID, progress);
        tag.putInt(ELAPSED_ID, elapsed);
        tag.putInt(DURATION_ID, duration);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean(ACTIVE_ID);
        progress = Math.max(0, tag.getInt(PROGRESS_ID));
        elapsed = Math.max(0, tag.getInt(ELAPSED_ID));
        duration = Math.max(1, tag.getInt(DURATION_ID));
    }
}
