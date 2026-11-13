package com.murthinext.ae2pr.api.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;

import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;

public final class MachineMenu<E extends AbstractMultiblockControllerBlockEntity>
        extends AbstractMultiblockControllerMenu<E> {

    public static final int SWITCH_MODE = 0;

    private static final int DATA_MODE = DATA_COUNT;

    private final List<Mode> modes;

    public MachineMenu(MachineDefinition<?, E, ?> definition, int id, Inventory inventory, @Nullable E controller) {
        super(definition, id, inventory, controller, definition.modes().isEmpty() ? 0 : 1);
        this.modes = definition.modes();
    }

    public MachineMenu(MachineDefinition<?, E, ?> definition, int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(definition, id, inventory, readController(inventory.player, buffer, definition));
    }

    public boolean hasModeSwitch() {
        return !modes.isEmpty();
    }

    @Nullable
    public Mode getMode() {
        if (modes.isEmpty()) {
            return null;
        }
        int ordinal = data().get(DATA_MODE);
        return ordinal >= 0 && ordinal < modes.size() ? modes.get(ordinal) : modes.get(0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != SWITCH_MODE || !(controller instanceof ModeMachineBlockEntity<?> machine) || !stillValid(player) ||
            !machine.switchMode()) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override
    protected void writeExtraData(SimpleContainerData data) {
        if (!modes.isEmpty() && controller instanceof ModeMachineBlockEntity<?> machine) {
            data.set(DATA_MODE, machine.getMode().ordinal());
        }
    }
}
