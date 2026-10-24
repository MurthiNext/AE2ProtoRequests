package com.murthinext.ae2pr.block.machine_part;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;

/**
 * 机器部件物品总线方块实体，输出总线禁止外部存入。
 */
public class ItemBusBlockEntity extends BlockEntity {

    public static final int CAPACITY = 32 * 1024;
    public static final int AEV_CAPACITY = 2048;

    private static final String STORAGE_ID = "storage";
    private static final String AUTO_TRANSFER_ID = "autoTransfer";

    private boolean autoTransfer = true;

    private final ItemStackHandler storage;

    private final IItemHandler externalStorage = new IItemHandler() {
        @Override
        public int getSlots() {
            return storage.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return storage.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return storage.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return storage.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private final IItemHandler exposedStorage;
    private LazyOptional<IItemHandler> storageCapability;

    public ItemBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CERTUS_QUARTZ_CRYSTAL_MACHINE_PART.get(), pos, state);
        int slotLimit = MachinePartBlock.itemCapacity(state);
        this.storage = new ItemStackHandler(MachinePartBlock.itemSlotCount(state)) {
            @Override
            public int getSlotLimit(int slot) {
                return slotLimit;
            }

            @Override
            protected int getStackLimit(int slot, ItemStack stack) {
                // 放开原版 64 的堆叠截断，按槽位上限判断
                return getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // 单槽仅一类：空槽接受任意物品，非空槽只接受同种物品
                ItemStack current = getStackInSlot(slot);
                return current.isEmpty() || ItemStack.isSameItemSameTags(current, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public CompoundTag serializeNBT() {
                CompoundTag tag = super.serializeNBT();
                ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag item = items.getCompound(i);
                    item.putInt("Count", getStackInSlot(item.getInt("Slot")).getCount());
                }
                return tag;
            }

            @Override
            public void deserializeNBT(CompoundTag tag) {
                ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
                for (int slot = 0; slot < getSlots(); slot++) {
                    setStackInSlot(slot, ItemStack.EMPTY);
                }
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag item = items.getCompound(i).copy();
                    int slot = item.getInt("Slot");
                    if (slot >= 0 && slot < getSlots()) {
                        int count = item.getInt("Count");
                        item.putByte("Count", (byte) 1);
                        ItemStack stack = ItemStack.of(item);
                        stack.setCount(count);
                        setStackInSlot(slot, stack);
                    }
                }
            }
        };
        this.exposedStorage = isOutputBus() ? externalStorage : storage;
        this.storageCapability = LazyOptional.of(() -> exposedStorage);
    }

    public ItemStackHandler getStorage() {
        return storage;
    }

    public int getSlotCount() {
        return storage.getSlots();
    }

    public int getCapacityPerSlot() {
        return storage.getSlotLimit(0);
    }

    public List<ItemStack> getStorageSnapshots() {
        List<ItemStack> stacks = new ArrayList<>(storage.getSlots());
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            stacks.add(storage.getStackInSlot(slot).copy());
        }
        return stacks;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return storageCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        storageCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        storageCapability = LazyOptional.of(() -> exposedStorage);
    }

    public boolean acceptsPlayerInsert() {
        return !isOutputBus();
    }

    public boolean isOutputBus() {
        return getBlockState().is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get())
                || getBlockState().is(ModBlocks.AEV_OUTPUT_BUS.get())
                || getBlockState().is(ModBlocks.AEV_DUAL_OUTPUT_HATCH.get());
    }

    public boolean isAutoTransfer() {
        return autoTransfer;
    }

    public void setAutoTransfer(boolean autoTransfer) {
        this.autoTransfer = autoTransfer;
        setChanged();
    }

    /**
     * 与朝向面的外部容器搬运物品。
     */
    public void serverTick() {
        if (!autoTransfer || level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(MachinePartBlock.FACING);
        BlockEntity target = level.getBlockEntity(worldPosition.relative(facing));
        if (target == null) {
            return;
        }
        IItemHandler handler = target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite())
                .orElse(null);
        if (handler == null) {
            return;
        }
        if (isOutputBus()) {
            pushItems(handler);
        } else {
            pullItems(handler);
        }
    }

    private void pullItems(IItemHandler source) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack available = source.extractItem(slot, Integer.MAX_VALUE, true);
            if (available.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(storage, available, true);
            int movable = available.getCount() - remainder.getCount();
            if (movable <= 0) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, movable, false);
            ItemHandlerHelper.insertItemStacked(storage, taken, false);
        }
    }

    private void pushItems(IItemHandler target) {
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            ItemStack content = storage.getStackInSlot(slot);
            if (content.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, content.copy(), false);
            // 单次抽取上限 64，按实际推入量分批移除
            int remaining = content.getCount() - remainder.getCount();
            while (remaining > 0) {
                ItemStack taken = storage.extractItem(slot, remaining, false);
                if (taken.isEmpty()) {
                    break;
                }
                remaining -= taken.getCount();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(STORAGE_ID, storage.serializeNBT());
        tag.putBoolean(AUTO_TRANSFER_ID, autoTransfer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(STORAGE_ID)) {
            storage.deserializeNBT(tag.getCompound(STORAGE_ID));
        }
        if (tag.contains(AUTO_TRANSFER_ID)) {
            autoTransfer = tag.getBoolean(AUTO_TRANSFER_ID);
        }
    }
}
