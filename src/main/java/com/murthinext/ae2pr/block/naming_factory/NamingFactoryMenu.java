package com.murthinext.ae2pr.block.naming_factory;

import org.jetbrains.annotations.Nullable;

import appeng.core.definitions.AEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeMenuType;

/**
 * 名称压印工厂容器菜单：模板 / 输入 / 输出三槽 + 玩家背包。
 */
public class NamingFactoryMenu extends AbstractContainerMenu {

    public static final MenuType<NamingFactoryMenu> TYPE = IForgeMenuType.create(NamingFactoryMenu::create);

    private static final int MACHINE_SLOTS = NamingFactoryBlockEntity.SLOT_COUNT;
    private static final int INV_COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    private final BlockPos pos;
    @Nullable
    private final NamingFactoryBlockEntity blockEntity;

    private NamingFactoryMenu(int id, Inventory playerInventory, BlockPos pos,
            @Nullable NamingFactoryBlockEntity blockEntity) {
        super(TYPE, id);
        this.pos = pos;
        this.blockEntity = blockEntity;

        Container inventory = blockEntity != null ? blockEntity.getInventory() : new SimpleContainer(MACHINE_SLOTS);
        addSlot(new Slot(inventory, NamingFactoryBlockEntity.SLOT_TEMPLATE, 120, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AEItems.NAME_PRESS.isSameAs(stack);
            }
        });
        addSlot(new Slot(inventory, NamingFactoryBlockEntity.SLOT_INPUT, 80, 26));
        addSlot(new Slot(inventory, NamingFactoryBlockEntity.SLOT_OUTPUT, 80, 74) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * INV_COLS + INV_COLS,
                        INV_X + col * SLOT_SIZE, INV_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    public NamingFactoryMenu(int id, Inventory playerInventory, NamingFactoryBlockEntity blockEntity) {
        this(id, playerInventory, blockEntity.getBlockPos(), blockEntity);
    }

    public static NamingFactoryMenu create(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return new NamingFactoryMenu(id, playerInventory, pos,
                blockEntity instanceof NamingFactoryBlockEntity factory ? factory : null);
    }

    @Nullable
    public NamingFactoryBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (AEItems.NAME_PRESS.isSameAs(stack)) {
            if (!moveItemStackTo(stack, NamingFactoryBlockEntity.SLOT_TEMPLATE,
                    NamingFactoryBlockEntity.SLOT_TEMPLATE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, NamingFactoryBlockEntity.SLOT_INPUT,
                    NamingFactoryBlockEntity.SLOT_INPUT + 1, false)) {
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

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0;
    }
}
