package com.murthinext.ae2pr.api.machine.singleblock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkPowerBlockEntity;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.api.EnergyAmount;
import com.murthinext.ae2pr.api.machine.MachineTuning;
import com.murthinext.ae2pr.api.machine.side.SideConfig;
import com.murthinext.ae2pr.api.machine.side.SideMode;

public abstract class AbstractSingleblockMachineBlockEntity extends AENetworkPowerBlockEntity {

    public static final int UPGRADE_SLOTS = 4;

    // 每个面每 tick 的自动搬运上限
    private static final int AUTO_TRANSFER_LIMIT = 64;
    private static final double ENERGY_EPSILON = 0.01;

    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_SIDE_CONFIG = "sideConfig";

    private final int slotCount;
    private final NonNullList<ItemStack> items;
    private final Container inventory;

    private final IUpgradeInventory upgrades;

    private final Map<Direction, LazyOptional<IItemHandler>> sidedHandlers = new EnumMap<>(Direction.class);
    private final LazyOptional<IItemHandler> unsidedHandler;

    private final SideConfig sideConfig = new SideConfig();

    protected AbstractSingleblockMachineBlockEntity(SingleblockMachineDefinition<?, ?, ?> definition, BlockPos pos,
                                                    BlockState state) {
        super(definition.blockEntityType(), pos, state);
        this.slotCount = definition.slots();
        this.items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        this.inventory = new MachineContainer();
        this.upgrades = UpgradeInventories.forMachine(definition.item(), UPGRADE_SLOTS, this::onUpgradesChanged);
        // 默认不耗待机电力、不占频道，仅作为网络上的用电节点
        getMainNode().setIdlePowerUsage(0);
        getMainNode().setFlags();
        setInternalMaxPower(definition.powerCapacity());
        setInternalPublicPowerStorage(true);
        setInternalPowerFlow(AccessRestriction.WRITE);

        this.unsidedHandler = LazyOptional.of(() -> new SidedItemHandler(null));
        for (Direction direction : Direction.values()) {
            this.sidedHandlers.put(direction, LazyOptional.of(() -> new SidedItemHandler(direction)));
        }
    }

    // 槽位与物流

    public final int slotCount() {
        return slotCount;
    }

    // 参与外部物流的输入槽，模板槽之类不参与物流的槽位保持默认 false
    protected boolean isInputSlot(int slot) {
        return false;
    }

    protected boolean isOutputSlot(int slot) {
        return false;
    }

    // 是否允许在指定槽位放入该物品，默认拒绝所有输出槽
    protected boolean isValidForSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < slotCount && !isOutputSlot(slot);
    }

    // 菜单、外部物流与内部容器共用的放入判定：作业中关闭收货只影响输入槽
    public final boolean canInsert(int slot, ItemStack stack) {
        return isValidForSlot(slot, stack) && (!isInputSlot(slot) || acceptsInputs());
    }

    // 作业中可关闭收货，关闭后外部与自动搬运都不再放入输入槽
    public boolean acceptsInputs() {
        return true;
    }

    public Container getInventory() {
        return inventory;
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    // 加速卡速度倍率，单方块机器默认与多方块机器共用同一配置
    public double getSpeedMultiplier() {
        return 1.0 + getSpeedCardCount() * MachineTuning.speedPerCard();
    }

    // 按速度倍率折算时长，至少 1 tick
    protected int scaledDuration(int baseTicks) {
        double speed = Math.min(getSpeedMultiplier(), Integer.MAX_VALUE);
        return (int) Math.max(1L, Math.min((long) Math.ceil(baseTicks / speed), Integer.MAX_VALUE));
    }

    protected void onUpgradesChanged() {
        setChanged();
        markForUpdate();
    }

    // 面配置

    public SideConfig getSideConfig() {
        return sideConfig;
    }

    // 由菜单按钮触发的配置变更
    public boolean applySideConfigButton(int id) {
        if (!sideConfig.pressButton(id)) {
            return false;
        }
        setChanged();
        markForUpdate();
        return true;
    }

    public Component getDisplayName() {
        Component custom = getCustomName();
        if (custom != null && !custom.getString().isEmpty()) {
            return custom;
        }
        return getName();
    }

    // 服务端每 tick 先做自动搬运，再推进机器自身逻辑
    public final void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        tickAutoTransfer();
        tickMachine();
    }

    // 子类在此推进自身的作业状态机
    protected abstract void tickMachine();

    // 按面配置自动搬运：先向允许抽出的面推出输出，空闲时才从允许插入的面拉取
    private void tickAutoTransfer() {
        boolean push = sideConfig.isAutoPush();
        boolean pull = sideConfig.isAutoPull() && acceptsInputs();
        if (!pull && !push) {
            return;
        }
        for (Direction dir : Direction.values()) {
            SideMode mode = sideConfig.get(dir);
            boolean doPush = push && mode.allowsExtract();
            boolean doPull = pull && mode.allowsInsert();
            if (!doPush && !doPull) {
                continue;
            }
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbour == null) {
                continue;
            }
            IItemHandler handler = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite())
                                           .orElse(null);
            if (handler == null) {
                continue;
            }
            boolean pushed = doPush && pushOutputs(handler);
            if (doPull && !pushed) {
                pullInputs(handler);
            }
        }
    }

    private boolean pushOutputs(IItemHandler target) {
        boolean moved = false;
        for (int slot = 0; slot < slotCount; slot++) {
            if (!isOutputSlot(slot)) {
                continue;
            }
            ItemStack output = inventory.getItem(slot);
            if (output.isEmpty()) {
                continue;
            }
            ItemStack toPush = output.copyWithCount(Math.min(output.getCount(), AUTO_TRANSFER_LIMIT));
            int leftover = ItemHandlerHelper.insertItemStacked(target, toPush, false).getCount();
            int movedCount = toPush.getCount() - leftover;
            if (movedCount <= 0) {
                continue;
            }
            int remaining = output.getCount() - movedCount;
            inventory.setItem(slot, remaining > 0 ? output.copyWithCount(remaining) : ItemStack.EMPTY);
            moved = true;
        }
        return moved;
    }

    private void pullInputs(IItemHandler source) {
        int budget = AUTO_TRANSFER_LIMIT;
        for (int slot = 0; slot < source.getSlots() && budget > 0; slot++) {
            ItemStack available = source.extractItem(slot, budget, true);
            if (available.isEmpty()) {
                continue;
            }
            int accepted = available.getCount() - insertInputs(available, true).getCount();
            if (accepted <= 0) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, accepted, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack leftover = insertInputs(taken, false);
            if (!leftover.isEmpty()) {
                ItemHandlerHelper.insertItemStacked(source, leftover, false);
            }
            budget -= taken.getCount() - leftover.getCount();
        }
    }

    // 逐个输入槽尝试放入，返回放不下的剩余部分
    private ItemStack insertInputs(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < slotCount; slot++) {
            if (!isInputSlot(slot) || !isValidForSlot(slot, remaining)) {
                continue;
            }
            remaining = insertSlot(slot, remaining, simulate);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    protected final ItemStack insertSlot(int slot, ItemStack stack, boolean simulate) {
        ItemStack current = items.get(slot);
        int limit = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
        int moved;
        if (current.isEmpty()) {
            moved = Math.min(limit, stack.getCount());
            if (!simulate && moved > 0) {
                inventory.setItem(slot, stack.copyWithCount(moved));
            }
        } else if (ItemStack.isSameItemSameTags(current, stack) && current.getCount() < limit) {
            moved = Math.min(limit - current.getCount(), stack.getCount());
            if (!simulate && moved > 0) {
                inventory.setItem(slot, current.copyWithCount(current.getCount() + moved));
            }
        } else {
            return stack;
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    // AE 能量：优先扣本机缓存，不足再抽 ME 网络

    protected boolean hasEnergy(double cost) {
        double amount = EnergyAmount.clamp(cost);
        if (amount <= 0) {
            return true;
        }
        double fromSelf = extractAEPower(amount, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (amount - fromSelf <= ENERGY_EPSILON) {
            return true;
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        double fromGrid = grid.getEnergyService().extractAEPower(amount - fromSelf, Actionable.SIMULATE,
                                                                 PowerMultiplier.CONFIG);
        return fromSelf + fromGrid >= amount - ENERGY_EPSILON;
    }

    protected boolean consumeEnergy(double cost) {
        double amount = EnergyAmount.clamp(cost);
        if (amount <= 0) {
            return true;
        }
        // 两者均先模拟、结算时一次性扣除
        double fromSelf = extractAEPower(amount, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        double fromGrid = 0;
        IGrid grid = null;
        if (amount - fromSelf > ENERGY_EPSILON) {
            grid = getMainNode().getGrid();
            if (grid == null) {
                return false;
            }
            fromGrid = grid.getEnergyService().extractAEPower(amount - fromSelf, Actionable.SIMULATE,
                                                              PowerMultiplier.CONFIG);
        }
        if (fromSelf + fromGrid < amount - ENERGY_EPSILON) {
            return false;
        }
        if (fromSelf > 0) {
            extractAEPower(fromSelf, Actionable.MODULATE, PowerMultiplier.CONFIG);
        }
        if (fromGrid > 0 && grid != null) {
            grid.getEnergyService().extractAEPower(fromGrid, Actionable.MODULATE, PowerMultiplier.CONFIG);
        }
        return true;
    }

    // 本机自行持久化与掉落库存，不使用 AE 基类库存系统
    @Override
    public InternalInventory getInternalInventory() {
        return InternalInventory.empty();
    }

    @Override
    public void onChangeInventory(InternalInventory inventory, int slot) {
    }

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        for (ItemStack upgrade : upgrades) {
            if (!upgrade.isEmpty()) {
                drops.add(upgrade.copy());
            }
        }
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < slotCount; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        upgrades.clear();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, items);
        tag.put(TAG_INVENTORY, inventoryTag);
        CompoundTag configTag = new CompoundTag();
        sideConfig.save(configTag);
        tag.put(TAG_SIDE_CONFIG, configTag);
        upgrades.writeToNBT(tag, TAG_UPGRADES);
        saveMachineData(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains(TAG_INVENTORY)) {
            for (int slot = 0; slot < slotCount; slot++) {
                items.set(slot, ItemStack.EMPTY);
            }
            ContainerHelper.loadAllItems(tag.getCompound(TAG_INVENTORY), items);
        }
        sideConfig.load(tag.contains(TAG_SIDE_CONFIG) ? tag.getCompound(TAG_SIDE_CONFIG) : new CompoundTag());
        upgrades.readFromNBT(tag, TAG_UPGRADES);
        loadMachineData(tag);
    }

    protected void saveMachineData(CompoundTag tag) {
    }

    protected void loadMachineData(CompoundTag tag) {
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        for (ItemStack stack : items) {
            data.writeItem(stack);
        }
        sideConfig.writeToStream(data);
        Component customName = getCustomName();
        data.writeUtf(customName != null ? customName.getString() : "");
        writeMachineStream(data);
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        for (int slot = 0; slot < slotCount; slot++) {
            items.set(slot, data.readItem());
        }
        sideConfig.readFromStream(data);
        setName(data.readUtf());
        readMachineStream(data);
        return changed;
    }

    protected void writeMachineStream(FriendlyByteBuf data) {
    }

    protected void readMachineStream(FriendlyByteBuf data) {
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return (side == null ? unsidedHandler : sidedHandlers.get(side)).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        unsidedHandler.invalidate();
        sidedHandlers.values().forEach(LazyOptional::invalidate);
    }

    private final class MachineContainer implements Container {

        @Override
        public int getContainerSize() {
            return slotCount;
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
            if (!removed.isEmpty()) {
                setChanged();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            AbstractSingleblockMachineBlockEntity.this.setChanged();
            markForUpdate();
        }

        @Override
        public boolean stillValid(Player player) {
            return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64.0;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return canInsert(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void clearContent() {
            AbstractSingleblockMachineBlockEntity.this.clearContent();
        }
    }

    // 按面暴露输入/输出槽，模板槽等内部槽位不参与物流
    private final class SidedItemHandler implements IItemHandler {

        @Nullable
        private final Direction side;

        private SidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        private SideMode mode() {
            return side == null ? SideMode.BOTH : sideConfig.get(side);
        }

        private boolean exposed(int slot) {
            SideMode mode = mode();
            return (mode.allowsInsert() && isInputSlot(slot)) || (mode.allowsExtract() && isOutputSlot(slot));
        }

        private int machineSlot(int index) {
            int seen = 0;
            for (int slot = 0; slot < slotCount; slot++) {
                if (!exposed(slot)) {
                    continue;
                }
                if (seen == index) {
                    return slot;
                }
                seen++;
            }
            return -1;
        }

        @Override
        public int getSlots() {
            int count = 0;
            for (int slot = 0; slot < slotCount; slot++) {
                if (exposed(slot)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            int target = machineSlot(slot);
            return target < 0 ? ItemStack.EMPTY : inventory.getItem(target);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int target = machineSlot(slot);
            if (target < 0 || !isInputSlot(target) || !acceptsInputs() || !isValidForSlot(target, stack)) {
                return stack;
            }
            return insertSlot(target, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int target = machineSlot(slot);
            if (target < 0 || !isOutputSlot(target) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack current = inventory.getItem(target);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int moved = Math.min(amount, current.getCount());
            ItemStack result = current.copyWithCount(moved);
            if (!simulate) {
                inventory.setItem(target,
                                  moved >= current.getCount() ? ItemStack.EMPTY
                                                              : current.copyWithCount(current.getCount() - moved));
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int target = machineSlot(slot);
            return target >= 0 && isInputSlot(target) && acceptsInputs() && isValidForSlot(target, stack);
        }
    }
}
