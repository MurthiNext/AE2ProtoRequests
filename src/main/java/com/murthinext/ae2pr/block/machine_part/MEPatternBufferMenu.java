package com.murthinext.ae2pr.block.machine_part;

import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.COLUMNS;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.ROWS;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SHARED_COLUMNS;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SHARED_SIZE;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SHARED_TANK_CAPACITY;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SIZE;

import appeng.api.crafting.PatternDetailsHelper;
import com.murthinext.ae2pr.core.network.PatternBufferFluidPacket;
import com.murthinext.ae2pr.core.registry.ModNetwork;
import com.murthinext.nextui.layout.PlayerInventoryLayout;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import org.jetbrains.annotations.Nullable;

public final class MEPatternBufferMenu extends AbstractContainerMenu {

    public static final MenuType<MEPatternBufferMenu> TYPE = IForgeMenuType.create(
            (id, inventory, data) -> new MEPatternBufferMenu(id, inventory, data.readBlockPos(), null));

    static final int PANEL_CLOSED = 0;
    static final int ITEM_PANEL = 1;
    static final int FLUID_PANEL = 2;
    static final int FLUID_CLICK_START = FLUID_PANEL + 1;

    private static final int SLOT_SIZE = PlayerInventoryLayout.SLOT_SIZE;
    private static final int FIRST_X = PlayerInventoryLayout.STANDARD.x();
    private static final int CONTENT_TOP = 23;
    private static final int CONTENT_BOTTOM = PlayerInventoryLayout.STANDARD.y() - 3;
    // 槽位在标题分隔线与背包分隔线之间居中
    private static final int FIRST_Y = CONTENT_TOP + (CONTENT_BOTTOM - CONTENT_TOP - ROWS * SLOT_SIZE) / 2;
    private static final int SHARED_GRID_TOP = 18;
    static final int SHARED_PANEL_WIDTH = 82;
    static final int SHARED_PANEL_HEIGHT = SHARED_GRID_TOP + SHARED_COLUMNS * SLOT_SIZE + 6;
    static final int SHARED_PANEL_X = -SHARED_PANEL_WIDTH - 24;
    static final int SHARED_PANEL_Y = PlayerInventoryLayout.STANDARD.y();

    private static final int PLAYER_SLOTS_START = SIZE + SHARED_SIZE;
    private static final int PANEL_DATA = ROWS + 1;

    private final BlockPos pos;
    private final Player owner;

    @Nullable
    private final MEPatternBufferBlockEntity buffer;

    private final SimpleContainerData state = new SimpleContainerData(PANEL_DATA + 1);
    private final FluidStack[] fluids = new FluidStack[SHARED_SIZE];

    MEPatternBufferMenu(int id, Inventory inventory, BlockPos pos, @Nullable MEPatternBufferBlockEntity buffer) {
        super(TYPE, id);
        this.pos = pos;
        this.owner = inventory.player;
        this.buffer = buffer;
        Arrays.fill(fluids, FluidStack.EMPTY);
        var patterns = buffer == null ? new ItemStackHandler(SIZE) : buffer.patterns();
        for (int i = 0; i < SIZE; i++) {
            addSlot(new BufferSlot(
                    patterns, i, FIRST_X + i % COLUMNS * SLOT_SIZE, FIRST_Y + i / COLUMNS * SLOT_SIZE, false));
        }
        var sharedItems = buffer == null ? new ItemStackHandler(SHARED_SIZE) : buffer.sharedItems();
        for (int i = 0; i < SHARED_SIZE; i++) {
            addSlot(new BufferSlot(sharedItems, i, sharedX(i), sharedY(i), true));
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

    int activePanel() {
        return state.get(PANEL_DATA);
    }

    static int sharedX(int slot) {
        return SHARED_PANEL_X
                + (SHARED_PANEL_WIDTH - SHARED_COLUMNS * SLOT_SIZE) / 2
                + 1
                + slot % SHARED_COLUMNS * SLOT_SIZE;
    }

    static int sharedY(int slot) {
        return SHARED_PANEL_Y + SHARED_GRID_TOP + 1 + slot / SHARED_COLUMNS * SLOT_SIZE;
    }

    FluidStack sharedFluid(int slot) {
        return fluids[slot];
    }

    public void applyFluidSync(int slot, FluidStack fluid) {
        if (buffer == null && slot >= 0 && slot < SHARED_SIZE) {
            fluids[slot] = fluid;
        }
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
        if (buffer != null && owner instanceof ServerPlayer player && activePanel() == FLUID_PANEL) {
            for (int i = 0; i < SHARED_SIZE; i++) {
                FluidStack current = buffer.sharedTank(i).getFluid();
                if (!current.isFluidStackIdentical(fluids[i])) {
                    fluids[i] = current.copy();
                    ModNetwork.sendToPlayer(player, new PatternBufferFluidPacket(containerId, i, fluids[i]));
                }
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (buffer == null) {
            return false;
        }
        if (id >= PANEL_CLOSED && id <= FLUID_PANEL) {
            resetQuickCraft();
            state.set(PANEL_DATA, id);
            return true;
        }
        int click = id - FLUID_CLICK_START;
        if (activePanel() != FLUID_PANEL
                || click < 0
                || click >= SHARED_SIZE * 2
                || getCarried().isEmpty()) {
            return false;
        }
        var tank = buffer.sharedTank(click / 2);
        var inventory = new PlayerMainInvWrapper(player.getInventory());
        var result = click % 2 == 0
                ? FluidUtil.tryFillContainerAndStow(getCarried(), tank, inventory, SHARED_TANK_CAPACITY, null, true)
                : FluidUtil.tryEmptyContainerAndStow(getCarried(), tank, inventory, SHARED_TANK_CAPACITY, null, true);
        if (result.isSuccess()) {
            setCarried(result.getResult());
            return true;
        }
        return false;
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
    public void clicked(int index, int button, ClickType type, Player player) {
        if (index >= 0 && index < PLAYER_SLOTS_START && !getSlot(index).isActive()) {
            return;
        }
        super.clicked(index, button, type, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.isActive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < PLAYER_SLOTS_START
                ? moveItemStackTo(stack, PLAYER_SLOTS_START, slots.size(), true)
                : activePanel() == ITEM_PANEL
                        ? moveItemStackTo(stack, SIZE, PLAYER_SLOTS_START, false)
                        : moveItemStackTo(stack, 0, SIZE, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private final class BufferSlot extends SlotItemHandler {
        private final boolean shared;

        private BufferSlot(ItemStackHandler inventory, int index, int x, int y, boolean shared) {
            super(inventory, index, x, y);
            this.shared = shared;
        }

        @Override
        public boolean isActive() {
            return !shared || activePanel() == ITEM_PANEL;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive() || isLockedPattern()) {
                return false;
            }
            if (shared || buffer != null) {
                return super.mayPlace(stack);
            }
            var details = PatternDetailsHelper.decodePattern(stack, owner.level());
            return details != null && details.supportsPushInputsToExternalInventory();
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && !isLockedPattern() && super.mayPickup(player);
        }

        @Override
        public int getMaxStackSize() {
            return shared ? super.getMaxStackSize() : 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }

        @Override
        public void setChanged() {
            if (shared && buffer != null) {
                buffer.setChanged();
            }
        }

        private boolean isLockedPattern() {
            return !shared && isLocked(getSlotIndex());
        }
    }
}
