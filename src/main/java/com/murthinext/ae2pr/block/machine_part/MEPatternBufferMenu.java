package com.murthinext.ae2pr.block.machine_part;

import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.COLUMNS;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.ROWS;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SIZE;

import appeng.api.crafting.PatternDetailsHelper;
import com.murthinext.nextui.layout.PlayerInventoryLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public final class MEPatternBufferMenu extends AbstractContainerMenu {

    public static final MenuType<MEPatternBufferMenu> TYPE = IForgeMenuType.create(
            (id, inventory, data) -> new MEPatternBufferMenu(id, inventory, data.readBlockPos(), null));

    private static final int SLOT_SIZE = PlayerInventoryLayout.SLOT_SIZE;
    private static final int FIRST_X = PlayerInventoryLayout.STANDARD.x();
    private static final int CONTENT_TOP = 23;
    private static final int CONTENT_BOTTOM = PlayerInventoryLayout.STANDARD.y() - 3;
    // 槽位在标题分隔线与背包分隔线之间居中
    private static final int FIRST_Y = CONTENT_TOP + (CONTENT_BOTTOM - CONTENT_TOP - ROWS * SLOT_SIZE) / 2;

    private final BlockPos pos;

    @Nullable
    private final MEPatternBufferBlockEntity buffer;

    private final SimpleContainerData state = new SimpleContainerData(ROWS + 1);

    MEPatternBufferMenu(int id, Inventory inventory, BlockPos pos, @Nullable MEPatternBufferBlockEntity buffer) {
        super(TYPE, id);
        this.pos = pos;
        this.buffer = buffer;
        var patterns = buffer == null ? new ItemStackHandler(SIZE) : buffer.patterns();
        for (int i = 0; i < SIZE; i++) {
            int index = i;
            addSlot(
                    new SlotItemHandler(
                            patterns,
                            index,
                            FIRST_X + index % COLUMNS * SLOT_SIZE,
                            FIRST_Y + index / COLUMNS * SLOT_SIZE) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            if (isLocked(index)) {
                                return false;
                            }
                            if (buffer != null) {
                                return super.mayPlace(stack);
                            }
                            var details = PatternDetailsHelper.decodePattern(stack, inventory.player.level());
                            return details != null && details.supportsPushInputsToExternalInventory();
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return !isLocked(index) && super.mayPickup(player);
                        }

                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }

                        @Override
                        public int getMaxStackSize(ItemStack stack) {
                            return 1;
                        }
                    });
        }
        PlayerInventoryLayout.STANDARD.forEachSlot((index, x, y) -> addSlot(new Slot(inventory, index, x, y)));
        addDataSlots(state);
    }

    boolean isLocked(int slot) {
        return buffer == null ? (state.get(slot / COLUMNS) & (1 << (slot % COLUMNS))) != 0 : buffer.isLocked(slot);
    }

    boolean isOnline() {
        return buffer == null ? state.get(ROWS) != 0 : buffer.isOnline();
    }

    @Override
    public void broadcastChanges() {
        if (buffer != null) {
            for (int row = 0; row < ROWS; row++) {
                int locked = 0;
                for (int column = 0; column < COLUMNS; column++) {
                    if (buffer.isLocked(row * COLUMNS + column)) {
                        locked |= 1 << column;
                    }
                }
                state.set(row, locked);
            }
            state.set(ROWS, buffer.isOnline() ? 1 : 0);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return buffer == null
                || (!buffer.isRemoved()
                        && ContainerLevelAccess.create(player.level(), pos)
                                .evaluate(
                                        (level, at) -> level.getBlockEntity(at) == buffer
                                                && player.distanceToSqr(
                                                                at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5)
                                                        <= 64,
                                        false));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!(index < SIZE
                ? moveItemStackTo(stack, SIZE, slots.size(), true)
                : moveItemStackTo(stack, 0, SIZE, false))) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
