package com.murthinext.ae2pr.machine.naming_factory;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkPowerBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.items.materials.NamePressItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.api.EnergyAmount;
import com.murthinext.ae2pr.api.machine.side.SideConfig;
import com.murthinext.ae2pr.api.machine.side.SideMode;
import com.murthinext.ae2pr.registry.ModMachines;

// 名称压印工厂方块实体
public class NamingFactoryBlockEntity extends AENetworkPowerBlockEntity {

    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    public static final int CHARGE_TICKS = 40;
    public static final int PRESS_TICKS = 10;
    public static final int RETRACT_TICKS = 20;

    public static final float TRAVEL = 6.0F;

    private static final double ENERGY_EPSILON = 0.01;

    public enum WorkState {
        IDLE,
        CHARGING,
        PRESSING,
        RETRACTING
    }

    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_STATE = "state";
    private static final String TAG_WORK_START = "workStart";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_CHARGE_TICKS = "chargeTicks";
    private static final String TAG_PRESS_TICKS = "pressTicks";
    private static final String TAG_RETRACT_TICKS = "retractTicks";
    private static final String TAG_SIDE_CONFIG = "sideConfig";

    // 每个面每 tick 的自动搬运上限
    private static final int AUTO_TRANSFER_LIMIT = 64;

    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(ModMachines.NAMING_FACTORY.item(), 4,
            this::onUpgradesChanged);

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private final Container inventory = new Container() {
        @Override
        public int getContainerSize() {
            return SLOT_COUNT;
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
            NamingFactoryBlockEntity.this.setChanged();
            markForUpdate();
        }

        @Override
        public boolean stillValid(Player player) {
            return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64.0;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == SLOT_TEMPLATE) {
                return AEItems.NAME_PRESS.isSameAs(stack);
            }
            return slot != SLOT_OUTPUT;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                items.set(slot, ItemStack.EMPTY);
            }
        }
    };

    private final Map<Direction, LazyOptional<IItemHandler>> sidedHandlers = new EnumMap<>(Direction.class);
    private final LazyOptional<IItemHandler> unsidedHandler;

    private final SideConfig sideConfig = new SideConfig();

    private WorkState state = WorkState.IDLE;
    private long workStart;
    private int chargeTicks = CHARGE_TICKS;
    private int pressTicks = PRESS_TICKS;
    private int retractTicks = RETRACT_TICKS;

    public NamingFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.NAMING_FACTORY.blockEntityType(), pos, state);
        // 不耗待机电力；缓存 16k AE 仅允许充电、禁止抽取
        getMainNode().setIdlePowerUsage(0);
        // 不消耗频道：清空节点标记，仅作为网络上的用电/储物节点
        getMainNode().setFlags();
        setInternalMaxPower(ModConfig.namingFactoryMaxPower());
        setInternalPublicPowerStorage(true);
        setInternalPowerFlow(AccessRestriction.WRITE);
        this.unsidedHandler = LazyOptional.of(() -> new SidedItemHandler(null));
        for (Direction direction : Direction.values()) {
            this.sidedHandlers.put(direction, LazyOptional.of(() -> new SidedItemHandler(direction)));
        }
    }

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

    public Container getInventory() {
        return inventory;
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private void onUpgradesChanged() {
        setChanged();
        markForUpdate();
    }

    private int speedSteps() {
        return switch (upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 50;
            default -> 2;
        };
    }

    public double getSpeedMultiplier() {
        return speedSteps() / 2.0D;
    }

    // 折算各阶段时长：加速卡只加速蓄力阶段
    private void updatePhaseDurations() {
        chargeTicks = scaledDuration(CHARGE_TICKS, getSpeedMultiplier());
        pressTicks = PRESS_TICKS;
        retractTicks = RETRACT_TICKS;
    }

    private static int scaledDuration(int baseTicks, double speed) {
        return Math.max(1, (int) Math.ceil(baseTicks / speed));
    }

    public ItemStack getTemplateStack() {
        return inventory.getItem(SLOT_TEMPLATE);
    }

    public ItemStack getInputStack() {
        return inventory.getItem(SLOT_INPUT);
    }

    public ItemStack getOutputStack() {
        return inventory.getItem(SLOT_OUTPUT);
    }

    public Component getDisplayName() {
        Component custom = getCustomName();
        if (custom != null && !custom.getString().isEmpty()) {
            return custom;
        }
        return Component.translatable("block.ae2pr.naming_factory");
    }

    public WorkState getWorkState() {
        return state;
    }

    @Nullable
    public String getImprintName() {
        ItemStack template = getTemplateStack();
        if (!AEItems.NAME_PRESS.isSameAs(template)) {
            return null;
        }
        CompoundTag tag = template.getTag();
        if (tag == null) {
            return null;
        }
        String name = tag.getString(NamePressItem.TAG_INSCRIBE_NAME);
        return name.isEmpty() ? null : name;
    }

    public float getWorkProgress(float partialTicks) {
        if (level == null) {
            return 0;
        }
        float elapsed = level.getGameTime() + partialTicks - workStart;
        return switch (state) {
            case CHARGING -> Mth.clamp(elapsed / chargeTicks, 0, 1);
            case PRESSING -> 1;
            case RETRACTING, IDLE -> 0;
        };
    }

    public float getPressOffset(float partialTicks) {
        if (level == null) {
            return 0;
        }
        float elapsed = level.getGameTime() + partialTicks - workStart;
        return switch (state) {
            case PRESSING -> TRAVEL * easeOutCubic(Mth.clamp(elapsed / pressTicks, 0, 1));
            case RETRACTING -> TRAVEL * (1 - easeInOutCubic(Mth.clamp(elapsed / retractTicks, 0, 1)));
            case CHARGING, IDLE -> 0;
        };
    }

    // 服务端每 tick 推进状态机：蓄力 → 下压输出 → 回升
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        tickAutoTransfer();
        long elapsed = level.getGameTime() - workStart;
        switch (state) {
            case IDLE -> {
                if (canWork() && hasEnergy(getInputStack().getCount())) {
                    updatePhaseDurations();
                    beginState(WorkState.CHARGING);
                }
            }
            case CHARGING -> {
                // 蓄力期间仍可继续收货，时长不重置；压印前按当时数量一次性结算
                if (elapsed >= chargeTicks) {
                    if (canWork() && consumeEnergy(getInputStack().getCount())) {
                        beginState(WorkState.PRESSING);
                    } else {
                        beginState(WorkState.IDLE);
                    }
                }
            }
            case PRESSING -> {
                if (elapsed >= pressTicks) {
                    completeWork();
                    beginState(WorkState.RETRACTING);
                }
            }
            case RETRACTING -> {
                if (elapsed >= retractTicks) {
                    beginState(WorkState.IDLE);
                }
            }
        }
    }

    // 压印阶段输入已被整体结算，禁止再放入；其余阶段都可继续收货
    public boolean acceptsInputs() {
        return state != WorkState.PRESSING;
    }

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
            boolean pushed = doPush && pushOutput(handler);
            if (doPull && !pushed) {
                pullInput(handler);
            }
        }
    }

    private boolean pushOutput(IItemHandler target) {
        ItemStack output = getOutputStack();
        if (output.isEmpty()) {
            return false;
        }
        ItemStack toPush = output.copyWithCount(Math.min(output.getCount(), AUTO_TRANSFER_LIMIT));
        int requested = toPush.getCount();
        int leftover = ItemHandlerHelper.insertItemStacked(target, toPush, false).getCount();
        int moved = requested - leftover;
        if (moved <= 0) {
            return false;
        }
        int remaining = output.getCount() - moved;
        inventory.setItem(SLOT_OUTPUT, remaining > 0 ? output.copyWithCount(remaining) : ItemStack.EMPTY);
        return true;
    }

    private void pullInput(IItemHandler source) {
        int budget = AUTO_TRANSFER_LIMIT;
        for (int slot = 0; slot < source.getSlots() && budget > 0; slot++) {
            ItemStack available = source.extractItem(slot, budget, true);
            if (available.isEmpty()) {
                continue;
            }
            int accepted = available.getCount() - insertSlot(SLOT_INPUT, available, true).getCount();
            if (accepted <= 0) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, accepted, false);
            if (!taken.isEmpty()) {
                ItemStack leftover = insertSlot(SLOT_INPUT, taken, false);
                if (!leftover.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(source, leftover, false);
                }
                budget -= taken.getCount() - leftover.getCount();
            }
        }
    }

    private ItemStack insertSlot(int slot, ItemStack stack, boolean simulate) {
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

    private boolean canWork() {
        String name = getImprintName();
        if (name == null) {
            return false;
        }
        ItemStack input = getInputStack();
        if (input.isEmpty()) {
            return false;
        }
        ItemStack output = getOutputStack();
        if (output.isEmpty()) {
            return true;
        }
        ItemStack result = renamed(input, name);
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + input.getCount() <= output.getMaxStackSize();
    }

    // 仅模拟结算所需能量，真正扣除放到压印开始时
    private boolean hasEnergy(int parallel) {
        double cost = EnergyAmount.clamp(ModConfig.namingFactoryEnergyPerItem() * parallel);
        if (cost <= 0) {
            return true;
        }
        double fromSelf = extractAEPower(cost, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (cost - fromSelf <= ENERGY_EPSILON) {
            return true;
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        double fromGrid = grid.getEnergyService().extractAEPower(cost - fromSelf,
                Actionable.SIMULATE, PowerMultiplier.CONFIG);
        return fromSelf + fromGrid >= cost - ENERGY_EPSILON;
    }

    // 支付压印能量：优先扣本机缓存，不足再抽 ME 网络；耗能按整组输入物品数计
    private boolean consumeEnergy(int parallel) {
        double cost = EnergyAmount.clamp(ModConfig.namingFactoryEnergyPerItem() * parallel);
        if (cost <= 0) {
            return true;
        }
        // 两者均先模拟、结算时一次性扣除
        double fromSelf = extractAEPower(cost, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        double fromGrid = 0;
        IGrid grid = null;
        if (cost - fromSelf > ENERGY_EPSILON) {
            grid = getMainNode().getGrid();
            if (grid == null) {
                return false;
            }
            fromGrid = grid.getEnergyService().extractAEPower(cost - fromSelf,
                    Actionable.SIMULATE, PowerMultiplier.CONFIG);
        }
        if (fromSelf + fromGrid < cost - ENERGY_EPSILON) {
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

    private void completeWork() {
        String name = getImprintName();
        if (name == null) {
            return;
        }
        ItemStack input = getInputStack();
        if (input.isEmpty()) {
            return;
        }
        ItemStack result = renamed(input, name);
        ItemStack output = getOutputStack();
        if (output.isEmpty()) {
            inventory.setItem(SLOT_OUTPUT, result);
        } else if (ItemStack.isSameItemSameTags(output, result)) {
            output.grow(result.getCount());
            inventory.setItem(SLOT_OUTPUT, output);
        } else {
            return;
        }
        inventory.setItem(SLOT_INPUT, ItemStack.EMPTY);
    }

    private static ItemStack renamed(ItemStack input, String name) {
        ItemStack result = input.copy();
        result.setHoverName(Component.literal(name));
        return result;
    }

    // 进入新状态并记录起点；各阶段时长相对自身起点计算
    private void beginState(WorkState newState) {
        this.state = newState;
        this.workStart = level != null ? level.getGameTime() : 0;
        setChanged();
        markForUpdate();
    }

    private static float easeOutCubic(float x) {
        float inverse = 1 - x;
        return 1 - inverse * inverse * inverse;
    }

    private static float easeInOutCubic(float x) {
        return x < 0.5F ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, items);
        tag.put(TAG_INVENTORY, inventoryTag);
        tag.putByte(TAG_STATE, (byte) state.ordinal());
        tag.putLong(TAG_WORK_START, workStart);
        tag.putInt(TAG_CHARGE_TICKS, chargeTicks);
        tag.putInt(TAG_PRESS_TICKS, pressTicks);
        tag.putInt(TAG_RETRACT_TICKS, retractTicks);
        CompoundTag configTag = new CompoundTag();
        sideConfig.save(configTag);
        tag.put(TAG_SIDE_CONFIG, configTag);
        upgrades.writeToNBT(tag, TAG_UPGRADES);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains(TAG_INVENTORY)) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                items.set(slot, ItemStack.EMPTY);
            }
            ContainerHelper.loadAllItems(tag.getCompound(TAG_INVENTORY), items);
        }
        if (tag.contains(TAG_STATE)) {
            int ordinal = tag.getByte(TAG_STATE);
            state = ordinal >= 0 && ordinal < WorkState.values().length
                    ? WorkState.values()[ordinal]
                    : WorkState.IDLE;
        }
        workStart = tag.getLong(TAG_WORK_START);
        chargeTicks = tag.contains(TAG_CHARGE_TICKS) ? tag.getInt(TAG_CHARGE_TICKS) : CHARGE_TICKS;
        pressTicks = tag.contains(TAG_PRESS_TICKS) ? tag.getInt(TAG_PRESS_TICKS) : PRESS_TICKS;
        retractTicks = tag.contains(TAG_RETRACT_TICKS) ? tag.getInt(TAG_RETRACT_TICKS) : RETRACT_TICKS;
        sideConfig.load(tag.contains(TAG_SIDE_CONFIG) ? tag.getCompound(TAG_SIDE_CONFIG) : new CompoundTag());
        upgrades.readFromNBT(tag, TAG_UPGRADES);
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeByte(state.ordinal());
        data.writeVarLong(workStart);
        data.writeVarInt(chargeTicks);
        data.writeVarInt(pressTicks);
        data.writeVarInt(retractTicks);
        for (ItemStack stack : items) {
            data.writeItem(stack);
        }
        sideConfig.writeToStream(data);
        Component customName = getCustomName();
        data.writeUtf(customName != null ? customName.getString() : "");
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        state = WorkState.values()[data.readByte()];
        workStart = data.readVarLong();
        chargeTicks = data.readVarInt();
        pressTicks = data.readVarInt();
        retractTicks = data.readVarInt();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            items.set(slot, data.readItem());
        }
        sideConfig.readFromStream(data);
        setName(data.readUtf());
        return changed;
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

    private final class SidedItemHandler implements IItemHandler {

        private static final int SLOT_INDEX_INPUT = 0;
        private static final int SLOT_INDEX_OUTPUT = 1;

        @Nullable
        private final Direction side;

        private SidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        private SideMode mode() {
            return side == null ? SideMode.BOTH : sideConfig.get(side);
        }

        @Override
        public int getSlots() {
            SideMode mode = mode();
            if (mode == SideMode.NONE) {
                return 0;
            }
            return mode == SideMode.BOTH ? 2 : 1;
        }

        // 输入面只暴露输入槽，输出面只暴露输出槽，模板槽不参与物流
        private int machineSlot(int slot) {
            SideMode mode = mode();
            if (mode == SideMode.NONE) {
                return -1;
            }
            if (mode == SideMode.INPUT) {
                return slot == 0 ? SLOT_INPUT : -1;
            }
            if (mode == SideMode.OUTPUT) {
                return slot == 0 ? SLOT_OUTPUT : -1;
            }
            return switch (slot) {
                case SLOT_INDEX_INPUT -> SLOT_INPUT;
                case SLOT_INDEX_OUTPUT -> SLOT_OUTPUT;
                default -> -1;
            };
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            int target = machineSlot(slot);
            return target < 0 ? ItemStack.EMPTY : inventory.getItem(target);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int target = machineSlot(slot);
            if (target != SLOT_INPUT || !acceptsInputs() || !inventory.canPlaceItem(target, stack)) {
                return stack;
            }
            ItemStack current = inventory.getItem(target);
            if (current.isEmpty()) {
                int moved = Math.min(stack.getCount(), Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize()));
                if (!simulate) {
                    inventory.setItem(target, stack.copyWithCount(moved));
                }
                return moved >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
            }
            if (!ItemStack.isSameItemSameTags(current, stack)) {
                return stack;
            }
            int space = Math.min(inventory.getMaxStackSize(), current.getMaxStackSize()) - current.getCount();
            int moved = Math.min(space, stack.getCount());
            if (moved <= 0) {
                return stack;
            }
            if (!simulate) {
                inventory.setItem(target, current.copyWithCount(current.getCount() + moved));
            }
            return moved >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int target = machineSlot(slot);
            if (target != SLOT_OUTPUT) {
                return ItemStack.EMPTY;
            }
            ItemStack current = inventory.getItem(target);
            if (current.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int moved = Math.min(amount, current.getCount());
            ItemStack result = current.copyWithCount(moved);
            if (!simulate) {
                inventory.setItem(target, moved >= current.getCount()
                        ? ItemStack.EMPTY
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
            return target == SLOT_INPUT && acceptsInputs() && inventory.canPlaceItem(target, stack);
        }
    }
}
