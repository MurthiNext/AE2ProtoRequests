package com.murthinext.ae2pr.block.naming_factory;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModItems;

/**
 * 名称压印工厂方块实体：放入 AE2 名称压印模板与待命名物品，一次压印把整组物品改成模板中的名称。
 */
public class NamingFactoryBlockEntity extends AENetworkPowerBlockEntity {

    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    /** 蓄力时长 */
    public static final int CHARGE_TICKS = 40;
    /** 下压时长 */
    public static final int PRESS_TICKS = 10;
    /** 回升时长 */
    public static final int RETRACT_TICKS = 20;

    /** 下压行程 */
    public static final float TRAVEL = 6.0F;

    /** 能量结算的容差 */
    private static final double ENERGY_EPSILON = 0.01;

    public enum WorkState {
        IDLE,
        /** 蓄力读条 */
        CHARGING,
        /** 压印下压 */
        PRESSING,
        /** 回升冷却 */
        RETRACTING
    }

    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_STATE = "state";
    private static final String TAG_WORK_START = "workStart";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_CHARGE_TICKS = "chargeTicks";
    private static final String TAG_PRESS_TICKS = "pressTicks";
    private static final String TAG_RETRACT_TICKS = "retractTicks";

    /** 升级槽：4 个，仅可安装加速卡 */
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(ModItems.NAMING_FACTORY.get(), 4,
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

    private WorkState state = WorkState.IDLE;
    private long workStart;
    /** 本次压印各阶段时长 */
    private int chargeTicks = CHARGE_TICKS;
    private int pressTicks = PRESS_TICKS;
    private int retractTicks = RETRACT_TICKS;

    public NamingFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NAMING_FACTORY.get(), pos, state);
        // 不耗待机电力；内部缓存 16k AE，允许 ME 网络充电但禁止其他机器抽取
        getMainNode().setIdlePowerUsage(0);
        setInternalMaxPower(Config.namingFactoryMaxPower());
        setInternalPublicPowerStorage(true);
        setInternalPowerFlow(AccessRestriction.WRITE);
        this.unsidedHandler = LazyOptional.of(() -> new SidedItemHandler(null));
        for (Direction direction : Direction.values()) {
            this.sidedHandlers.put(direction, LazyOptional.of(() -> new SidedItemHandler(direction)));
        }
    }

    public Container getInventory() {
        return inventory;
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    /** 升级变化回调 */
    private void onUpgradesChanged() {
        setChanged();
        markForUpdate();
    }

    /** 加速卡档位 */
    private int speedSteps() {
        return switch (upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 50;
            default -> 2;
        };
    }

    /** 当前加速卡下的速度倍率 */
    public double getSpeedMultiplier() {
        return speedSteps() / 2.0D;
    }

    /** 开工时折算时长：加速卡只加速蓄力阶段。 */
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

    /** 界面标题显示的机器名称：自定义名称为空时回退为方块名。 */
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

    /** 模板中写入的名称；模板缺失或未写入名称时为 null。 */
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

    /** 读条进度（0~1）：蓄力阶段 0→1，下压阶段保持满，冷却阶段为空。 */
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

    /** forge 当前的下落距离（像素），供客户端渲染使用；蓄力阶段不动。 */
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

    /** 服务端每 tick：空闲时检查加工条件与能量；蓄力读条满后下压，触底输出产物，回升即冷却。 */
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        long elapsed = level.getGameTime() - workStart;
        switch (state) {
            case IDLE -> {
                if (canWork() && consumeEnergy(getInputStack().getCount())) {
                    updatePhaseDurations();
                    beginState(WorkState.CHARGING);
                }
            }
            case CHARGING -> {
                if (elapsed >= chargeTicks) {
                    beginState(WorkState.PRESSING);
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

    /** 是否满足开工条件：模板名称可用、有输入、且整组能放进输出槽。 */
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

    /**
     * 支付一次压印的能量：优先消耗本机缓存，不足的部分再向 ME 网络抽取；
     * 总耗能 = 每物品耗能 × 并行数（本次整组输入的物品数）。
     */
    private boolean consumeEnergy(int parallel) {
        double cost = Config.namingFactoryEnergyPerItem() * parallel;
        if (cost <= 0) {
            return true;
        }
        // 先尽量从本机缓存扣，缺口再向 ME 网络抽取；两者都按缺口模拟、一次结算
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

    /** 落地瞬间：把整组输入改名后移入输出槽。 */
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

    /** 进入新状态并记录本阶段起点（各阶段时长相对于自身起点计算）。 */
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

    /** 内部库存由本机自行持久化与掉落，不交给 AE 基类的库存系统。 */
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
        upgrades.readFromNBT(tag, TAG_UPGRADES);
    }

    /** 客户端可见状态：工作状态、阶段起点与时长、槽位内容与自定义名称（渲染器/界面需要）。 */
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

        @Nullable
        private final Direction side;

        private SidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        private boolean isSingleSlot() {
            return side != null;
        }

        private int machineSlot(int slot) {
            if (!isSingleSlot()) {
                return slot;
            }
            if (side == Direction.UP) {
                return SLOT_INPUT;
            }
            if (side == Direction.DOWN) {
                return SLOT_OUTPUT;
            }

            return SLOT_TEMPLATE;
        }

        @Override
        public int getSlots() {
            return isSingleSlot() ? 1 : SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getItem(machineSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int target = machineSlot(slot);
            if (!inventory.canPlaceItem(target, stack)) {
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
            return inventory.canPlaceItem(machineSlot(slot), stack);
        }
    }
}
