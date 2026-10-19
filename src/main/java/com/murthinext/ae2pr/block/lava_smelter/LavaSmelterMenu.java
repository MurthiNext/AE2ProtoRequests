package com.murthinext.ae2pr.block.lava_smelter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.SlotItemHandler;

import appeng.api.upgrades.Upgrades;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

/**
 * 高反应性熔岩冶炼炉主机的容器菜单。
 */
public class LavaSmelterMenu extends AbstractMultiblockControllerMenu<LavaSmelterControllerBlockEntity> {

    public static final MenuType<LavaSmelterMenu> TYPE = IForgeMenuType.create(LavaSmelterMenu::new);

    private static final int DUST_SLOT_X = 151;
    private static final int DUST_SLOT_Y = 95;
    // 升级槽之后紧随粉尘槽
    private static final int DUST_SLOT = UPGRADE_SLOTS;

    private static final int DATA_DURABILITY = DATA_COUNT;
    private static final int EXTRA_DATA = 1;

    public LavaSmelterMenu(int id, Inventory playerInventory, LavaSmelterControllerBlockEntity controller) {
        super(TYPE, id, playerInventory, controller, EXTRA_DATA);
    }

    public LavaSmelterMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, readController(playerInventory.player, buffer,
                LavaSmelterControllerBlockEntity.class));
    }

    @Override
    protected int extraSlots() {
        return 1;
    }

    @Override
    protected void addExtraSlots(LavaSmelterControllerBlockEntity controller) {
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
        return Config.lavaSmelterMaxDurability();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerSlotFirst = UPGRADE_SLOTS + 1;
        if (index < playerSlotFirst) {
            if (!moveItemStackTo(stack, playerSlotFirst, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 加速卡优先进入升级槽
            boolean moved = Upgrades.isUpgradeCardItem(stack)
                    && moveItemStackTo(stack, 0, UPGRADE_SLOTS, false);
            if (!moved && !moveItemStackTo(stack, DUST_SLOT, DUST_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
