package com.murthinext.ae2pr.machine;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerMenu;

public final class MachineMenu<E extends AbstractMultiblockControllerBlockEntity>
        extends AbstractMultiblockControllerMenu<E> {

    public MachineMenu(MachineDefinition<?, E, ?> definition, int id, Inventory inventory, @Nullable E controller) {
        super(definition, id, inventory, controller, 0);
    }

    public MachineMenu(MachineDefinition<?, E, ?> definition, int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(definition, id, inventory,
                readController(inventory.player, buffer, definition));
    }
}
