package com.murthinext.ae2pr.api.machine.singleblock;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.SlotItemHandler;

import appeng.api.upgrades.Upgrades;

import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.side.SideConfig;

public abstract class AbstractSingleblockMachineMenu<E extends AbstractSingleblockMachineBlockEntity>
        extends AbstractContainerMenu {

    protected static final int INV_COLS = 9;
    protected static final int INV_ROWS = 3;
    protected static final int SLOT_SIZE = 18;
    protected static final int INV_X = 8;
    protected static final int INV_Y = 122;
    protected static final int HOTBAR_Y = 180;
    protected static final int UPGRADE_X = 180;
    protected static final int UPGRADE_Y = 4;
    protected static final int UPGRADE_STEP = 20;

    private static final int PLAYER_SLOTS = INV_COLS * (INV_ROWS + 1);

    @Nullable
    protected final E machine;
    private final MachineDefinition<?, E, ?> definition;
    private final int upgradeSlotStart;
    private final int playerSlotStart;

    protected AbstractSingleblockMachineMenu(MachineDefinition<?, E, ?> definition, int id, Inventory playerInventory,
                                             @Nullable E machine) {
        super(definition.menuType(), id);
        this.definition = definition;
        this.machine = machine;
        this.upgradeSlotStart = machine == null ? 0 : machine.slotCount();
        this.playerSlotStart = upgradeSlotStart +
                (machine == null ? 0 : AbstractSingleblockMachineBlockEntity.UPGRADE_SLOTS);

        if (machine != null) {
            addMachineSlots(machine);
            var upgrades = machine.getUpgrades().toItemHandler();
            for (int i = 0; i < AbstractSingleblockMachineBlockEntity.UPGRADE_SLOTS; i++) {
                addSlot(new SlotItemHandler(upgrades, i, UPGRADE_X, UPGRADE_Y + i * UPGRADE_STEP));
            }
        }
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * INV_COLS + INV_COLS, INV_X + col * SLOT_SIZE,
                                 INV_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    // 子类按机器布局添加机器自身的槽位
    protected abstract void addMachineSlots(E machine);

    @Nullable
    public E getMachine() {
        return machine;
    }

    public MachineDefinition<?, E, ?> definition() {
        return definition;
    }

    // 可放入的机器槽位：判定交给方块实体，作业中不可收货
    protected final Slot machineSlot(E machine, int slot, int x, int y) {
        return new Slot(machine.getInventory(), slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canInsert(slot, stack);
            }
        };
    }

    // 只能取出、不能放入的机器槽位
    protected final Slot outputSlot(E machine, int slot, int x, int y) {
        return new Slot(machine.getInventory(), slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    // 背包 shift 点击时的目标槽位，默认放入机器槽位
    protected boolean moveToMachine(ItemStack stack) {
        return moveItemStackTo(stack, 0, upgradeSlotStart, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < playerSlotStart) {
            moved = moveItemStackTo(stack, playerSlotStart, playerSlotStart + PLAYER_SLOTS, true);
        } else if (Upgrades.isUpgradeCardItem(stack)) {
            moved = moveItemStackTo(stack, upgradeSlotStart, playerSlotStart, false);
        } else {
            moved = moveToMachine(stack);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine != null && !machine.isRemoved() && machine.getLevel() == player.level() &&
                player.distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) <= 64.0;
    }

    // 面配置按钮：0-5 对应六个面，6/7 自动拉取/推出，8 清空
    @Override
    public boolean clickMenuButton(Player player, int id) {
        return machine != null && stillValid(player) && machine.applySideConfigButton(id);
    }

    public SideConfig getSideConfig() {
        return machine != null ? machine.getSideConfig() : new SideConfig();
    }

    @Nullable
    protected static <E extends AbstractSingleblockMachineBlockEntity>
            E readMachine(Player player, FriendlyByteBuf buffer, MachineDefinition<?, E, ?> definition) {
        BlockEntity entity = player.level().getBlockEntity(buffer.readBlockPos());
        return definition.controller(entity);
    }
}
