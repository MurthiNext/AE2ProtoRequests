package com.murthinext.ae2pr.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;

import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerMenu;

public final class ModeMachineMenu<E extends AbstractMultiblockControllerBlockEntity & ModeMachine<T>,
        T extends Enum<T> & Mode> extends AbstractMultiblockControllerMenu<E> {

    public static final int SWITCH_MODE = 0;
    private static final int DATA_MODE = DATA_COUNT;

    private final List<T> modes;

    public ModeMachineMenu(MachineDefinition<?, E, ?> definition, int id, Inventory inventory,
            @Nullable E controller, List<T> modes) {
        super(definition, id, inventory, controller, 1);
        this.modes = modes;
    }

    public ModeMachineMenu(MachineDefinition<?, E, ?> definition, int id, Inventory inventory,
            FriendlyByteBuf buffer, List<T> modes) {
        this(definition, id, inventory, readController(inventory.player, buffer, definition), modes);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clientSide || id != SWITCH_MODE || controller == null || !stillValid(player)
                || !controller.switchMode()) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override
    protected void writeExtraData(SimpleContainerData data) {
        data.set(DATA_MODE, controller.getMode().ordinal());
    }

    public T getMode() {
        int ordinal = data().get(DATA_MODE);
        return ordinal >= 0 && ordinal < modes.size() ? modes.get(ordinal) : modes.get(0);
    }
}
