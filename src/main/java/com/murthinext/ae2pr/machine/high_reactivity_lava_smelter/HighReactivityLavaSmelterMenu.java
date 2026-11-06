package com.murthinext.ae2pr.machine.high_reactivity_lava_smelter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.SlotItemHandler;

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.registry.ModMachines;

public class HighReactivityLavaSmelterMenu extends AbstractMultiblockControllerMenu<HighReactivityLavaSmelterControllerBlockEntity> {

    public static final MenuType<HighReactivityLavaSmelterMenu> TYPE = IForgeMenuType.create(HighReactivityLavaSmelterMenu::new);

    private static final int DUST_SLOT_X = 151;
    private static final int DUST_SLOT_Y = 95;

    private static final int DATA_DURABILITY = DATA_COUNT;

    public HighReactivityLavaSmelterMenu(int id, Inventory playerInventory, HighReactivityLavaSmelterControllerBlockEntity controller) {
        super(ModMachines.HIGH_REACTIVITY_LAVA_SMELTER, id, playerInventory, controller, 1);
    }

    public HighReactivityLavaSmelterMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, readController(playerInventory.player, buffer,
                ModMachines.HIGH_REACTIVITY_LAVA_SMELTER));
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
        return ModConfig.highReactivityLavaSmelterMaxDurability();
    }
}
