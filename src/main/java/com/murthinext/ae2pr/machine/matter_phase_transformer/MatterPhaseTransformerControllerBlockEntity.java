package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.murthinext.ae2pr.api.machine.MachineBlock;
import com.murthinext.ae2pr.api.machine.ModeMachineBlockEntity;
import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;

public class MatterPhaseTransformerControllerBlockEntity extends ModeMachineBlockEntity<MatterPhaseTransformerMode> {

    private final MatterPhaseTransformerAnimation animation = new MatterPhaseTransformerAnimation();
    private FluidStack melt = FluidStack.EMPTY;
    private boolean refreshMelt = true;

    public MatterPhaseTransformerControllerBlockEntity(
            MultiblockMachineDefinition<MatterPhaseTransformerControllerBlockEntity> definition,
            BlockPos pos,
            BlockState state) {
        super(definition, pos, state);
    }

    @Override
    public BlockPos getCorePos() {
        return MatterPhaseTransformerStructure.corePos(worldPosition, getFacing(), mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos core = getCorePos();
        return new AABB(
                core.offset(0, MatterPhaseTransformerStructure.COLLECTOR_Y, 0),
                core.offset(1, MatterPhaseTransformerStructure.EMITTER_Y, 1));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        refreshMelt = true;
    }

    public void clientTick() {
        boolean formed = isFormed();
        animation.tick(formed, getBlockState().getValue(MachineBlock.RUNNING));
        if (!formed) {
            melt = FluidStack.EMPTY;
            refreshMelt = true;
        } else if (refreshMelt && isBusy()) {
            melt = getJobFluid();
            refreshMelt = melt.isEmpty();
        }
    }

    MatterPhaseTransformerAnimation animation() {
        return animation;
    }

    FluidStack melt() {
        return melt;
    }
}
