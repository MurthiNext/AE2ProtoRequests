package com.murthinext.ae2pr.machine.high_reactivity_lava_smelter;

import com.murthinext.ae2pr.ServerConfig;
import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.nextui.layout.PlayerInventoryLayout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.SlotItemHandler;

public class HighReactivityLavaSmelterMenu
        extends AbstractMultiblockControllerMenu<HighReactivityLavaSmelterControllerBlockEntity> {

    public static final MenuType<HighReactivityLavaSmelterMenu> TYPE =
            IForgeMenuType.create(HighReactivityLavaSmelterMenu::new);

    private static final int INVENTORY_COLUMNS = PlayerInventoryLayout.SLOT_COUNT - PlayerInventoryLayout.MAIN_SLOTS;
    static final int DUST_SLOT_X = INVENTORY_LAYOUT.x() + (INVENTORY_COLUMNS - 1) * PlayerInventoryLayout.SLOT_SIZE;
    static final int DUST_SLOT_Y = INVENTORY_LAYOUT.y() - PlayerInventoryLayout.SLOT_SIZE - 3;

    private static final int DATA_DURABILITY = DATA_COUNT;

    public HighReactivityLavaSmelterMenu(
            MachineDefinition<HighReactivityLavaSmelterControllerBlockEntity> definition,
            int id,
            Inventory playerInventory,
            HighReactivityLavaSmelterControllerBlockEntity controller) {
        super(definition, id, playerInventory, controller, 1);
    }

    public HighReactivityLavaSmelterMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(
                ModMachines.highReactivityLavaSmelter(),
                id,
                playerInventory,
                readController(playerInventory.player, buffer, ModMachines.highReactivityLavaSmelter()));
    }

    @Override
    protected int extraSlots() {
        return 1;
    }

    @Override
    protected void addExtraSlots(HighReactivityLavaSmelterControllerBlockEntity controller) {
        addSlot(new SlotItemHandler(controller.getDustSlot(), 0, DUST_SLOT_X, DUST_SLOT_Y));
    }

    @Override
    protected void writeExtraData(SimpleContainerData data) {
        data.set(DATA_DURABILITY, controller.getDurability());
    }

    public int getDurability() {
        return data().get(DATA_DURABILITY);
    }

    public int getMaxDurability() {
        return ServerConfig.highReactivityLavaSmelterMaxDurability();
    }
}
