package com.murthinext.ae2pr.block.molecular_assembly_core;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlockEntities;

public class MolecularAssemblyCoreBlockEntity extends BlockEntity {

    // 不同步转动
    public final float yawOffset;
    public final float pitchOffset;
    public final float rollOffset;
    public final float yawSpeed;
    public final float pitchSpeed;
    public final float rollSpeed;

    public MolecularAssemblyCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOLECULAR_ASSEMBLY_CORE.get(), pos, state);
        RandomSource random = RandomSource.create(pos.asLong());
        this.yawOffset = random.nextFloat() * 360.0F;
        this.pitchOffset = random.nextFloat() * 360.0F;
        this.rollOffset = random.nextFloat() * 360.0F;
        this.yawSpeed = 0.85F + random.nextFloat() * 0.3F;
        this.pitchSpeed = 0.55F + random.nextFloat() * 0.9F;
        this.rollSpeed = 0.55F + random.nextFloat() * 0.9F;
    }
}
