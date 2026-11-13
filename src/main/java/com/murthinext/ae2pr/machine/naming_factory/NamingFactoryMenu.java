package com.murthinext.ae2pr.machine.naming_factory;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;

import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.singleblock.AbstractSingleblockMachineMenu;
import com.murthinext.ae2pr.registry.ModMachines;

// 名称压印工厂容器菜单
public class NamingFactoryMenu extends AbstractSingleblockMachineMenu<NamingFactoryBlockEntity> {

    public static final MenuType<NamingFactoryMenu> TYPE = IForgeMenuType.create(NamingFactoryMenu::create);

    private static final int TEMPLATE_X = 120;
    private static final int TEMPLATE_Y = 50;
    private static final int INPUT_X = 80;
    private static final int INPUT_Y = 26;
    private static final int OUTPUT_X = 80;
    private static final int OUTPUT_Y = 74;

    public NamingFactoryMenu(MachineDefinition<?, NamingFactoryBlockEntity, NamingFactoryMenu> definition, int id,
                             Inventory playerInventory, @Nullable NamingFactoryBlockEntity machine) {
        super(definition, id, playerInventory, machine);
    }

    public static NamingFactoryMenu create(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new NamingFactoryMenu(ModMachines.NAMING_FACTORY, id, playerInventory,
                                     readMachine(playerInventory.player, buffer, ModMachines.NAMING_FACTORY));
    }

    @Override
    protected void addMachineSlots(NamingFactoryBlockEntity machine) {
        addSlot(machineSlot(machine, NamingFactoryBlockEntity.SLOT_TEMPLATE, TEMPLATE_X, TEMPLATE_Y));
        addSlot(machineSlot(machine, NamingFactoryBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y));
        addSlot(outputSlot(machine, NamingFactoryBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y));
    }

    // 名称压印模板优先进模板槽，其余物品进输入槽
    @Override
    protected boolean moveToMachine(ItemStack stack) {
        if (AEItems.NAME_PRESS.isSameAs(stack)) {
            return moveItemStackTo(stack, NamingFactoryBlockEntity.SLOT_TEMPLATE,
                                   NamingFactoryBlockEntity.SLOT_TEMPLATE + 1, false);
        }
        return moveItemStackTo(stack, NamingFactoryBlockEntity.SLOT_INPUT, NamingFactoryBlockEntity.SLOT_INPUT + 1,
                               false);
    }
}
