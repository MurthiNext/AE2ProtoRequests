package com.murthinext.ae2pr.block.assembly_line;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
 * 赛特斯石英水晶输入/输出总线方块实体：单格物品存储，仅存一类，上限 32K（32768）件。
 * <p>
 * 输出总线只接收配方输出，禁止玩家/外部存入。
 * <p>
 * 对外通过 {@code ITEM_HANDLER} 能力暴露存储，供其他模组的物流交互；
 * 默认开启自动搬运。
 */
public class ItemBusBlockEntity extends BlockEntity {

    /** 存储上限：32K 件 */
    public static final int CAPACITY = 32 * 1024;
    /** 可存储的类型数（预留多种类扩展） */
    public static final int TYPE_CAPACITY = 1;

    private static final String STORAGE_ID = "storage";
    private static final String AUTO_TRANSFER_ID = "autoTransfer";

    /** 自动搬运开关：输入总线拉取、输出总线推出，仅作用于朝向面，默认启用。 */
    private boolean autoTransfer = true;

    private final ItemStackHandler storage = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return CAPACITY;
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            // 原版实现还会按物品自身堆叠上限（64）截断，这里按槽位上限放开
            return getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 仅一类：空槽接受任意物品，非空槽只接受同种物品
            ItemStack current = getStackInSlot(slot);
            return current.isEmpty() || ItemStack.isSameItemSameTags(current, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** 输出总线对外视图：拒绝外部存入，仅允许抽出。 */
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

    /** 对外暴露的存储：输入总线双向可交互，输出总线只出不进。 */
    private final IItemHandler exposedStorage;
    private final LazyOptional<IItemHandler> storageCapability;

    public ItemBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CERTUS_QUARTZ_CRYSTAL_MACHINE_PART.get(), pos, state);
        this.exposedStorage = isOutputBus() ? externalStorage : storage;
        this.storageCapability = LazyOptional.of(() -> exposedStorage);
    }

    public ItemStackHandler getStorage() {
        return storage;
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

    /** 是否允许玩家存入（输出总线仅接受配方输出）。 */
    public boolean acceptsPlayerInsert() {
        return !isOutputBus();
    }

    /** 是否是输出总线。 */
    public boolean isOutputBus() {
        return getBlockState().is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get());
    }

    public boolean isAutoTransfer() {
        return autoTransfer;
    }

    public void setAutoTransfer(boolean autoTransfer) {
        this.autoTransfer = autoTransfer;
        setChanged();
    }

    /** 服务端 tick：与朝向面的外部容器搬运物品（输入拉取 / 输出推出），直到装满或清空。 */
    public void serverTick() {
        if (!autoTransfer || level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(CertusQuartzCrystalMachinePartBlock.FACING);
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

    /** 从外部容器拉取物品，直到总线存满或外部无可取之物。 */
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

    /** 向外部容器推出物品，直到总线清空或外部容器存满。 */
    private void pushItems(IItemHandler target) {
        ItemStack content = storage.getStackInSlot(0);
        if (content.isEmpty()) {
            return;
        }
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, content.copy(), false);
        // 单次抽取最多只能取走物品的堆叠上限（64），按实际推入量分批移除
        int remaining = content.getCount() - remainder.getCount();
        while (remaining > 0) {
            ItemStack taken = storage.extractItem(0, remaining, false);
            if (taken.isEmpty()) {
                break;
            }
            remaining -= taken.getCount();
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
