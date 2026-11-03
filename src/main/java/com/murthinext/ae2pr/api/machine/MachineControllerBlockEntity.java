package com.murthinext.ae2pr.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;

public final class MachineControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public MachineControllerBlockEntity(MultiblockMachineDefinition<?, ?> definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }
}
