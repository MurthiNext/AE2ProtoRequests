package com.murthinext.ae2pr.machine.matter_phase_transformer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.registry.ModMachines;

public class MatterPhaseTransformerControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    private final MatterPhaseTransformerAnimation animation = new MatterPhaseTransformerAnimation();

    public MatterPhaseTransformerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.MATTER_PHASE_TRANSFORMER, pos, state);
    }

    @Override
    public BlockPos getCorePos() {
        return MatterPhaseTransformerStructure.corePos(worldPosition, getFacing(), mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos core = getCorePos();
        return new AABB(core.getX(), core.getY() - 3, core.getZ(), core.getX() + 1, core.getY() + 4, core.getZ() + 1);
    }

    // 客户端动画

    public void clientTick() {
        animation.tick(isFormed(), isRunning());
    }

    public double getParticleTime(float partialTick) {
        return animation.getParticleTime(partialTick);
    }

    public float getEffectActivity(float partialTick) {
        return animation.getActivity(partialTick);
    }

    public float getBeamStrength(float partialTick) {
        return animation.getBeamStrength(partialTick);
    }
}
