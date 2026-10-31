package com.murthinext.ae2pr.machine;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerBlock;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerBlockEntity;

public final class MachineBlock<E extends AbstractMultiblockControllerBlockEntity>
        extends AbstractMultiblockControllerBlock<E> {

    private final MultiblockMachineDefinition<E, ?> definition;

    public MachineBlock(MultiblockMachineDefinition<E, ?> definition, Properties properties) {
        super(properties);
        this.definition = definition;
    }

    public MultiblockMachineDefinition<E, ?> definition() {
        return definition;
    }

    @Override
    protected E newController(BlockPos pos, BlockState state) {
        return definition.createBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != definition.blockEntityType()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            E controller = definition.controller(entity);
            if (controller != null) {
                if (tickLevel.isClientSide) {
                    clientTick(controller);
                } else {
                    controller.serverTick();
                }
            }
        };
    }

    @Override
    public String machineKey() {
        return definition.id();
    }

    @Override
    protected MenuProvider menuProvider(E controller) {
        return new SimpleMenuProvider((id, inventory, player) -> definition.createMenu(id, inventory, controller),
                getName());
    }

    @Override
    protected void clientTick(E controller) {
        definition.clientTick(controller);
    }
}
