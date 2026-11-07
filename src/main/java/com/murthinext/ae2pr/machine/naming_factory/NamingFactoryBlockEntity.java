package com.murthinext.ae2pr.machine.naming_factory;

import org.jetbrains.annotations.Nullable;

import appeng.core.definitions.AEItems;
import appeng.items.materials.NamePressItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.api.machine.singleblock.AbstractSingleblockMachineBlockEntity;
import com.murthinext.ae2pr.api.machine.singleblock.SingleblockMachineDefinition;

// 名称压印工厂方块实体
public class NamingFactoryBlockEntity extends AbstractSingleblockMachineBlockEntity {

    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    public static final int CHARGE_TICKS = 40;
    public static final int PRESS_TICKS = 10;
    public static final int RETRACT_TICKS = 20;

    public static final float TRAVEL = 6.0F;

    private static final String TAG_STATE = "state";
    private static final String TAG_WORK_START = "workStart";
    private static final String TAG_CHARGE_TICKS = "chargeTicks";
    private static final String TAG_PRESS_TICKS = "pressTicks";
    private static final String TAG_RETRACT_TICKS = "retractTicks";

    public enum WorkState {
        IDLE,
        CHARGING,
        PRESSING,
        RETRACTING
    }

    private WorkState state = WorkState.IDLE;
    private long workStart;
    private int chargeTicks = CHARGE_TICKS;
    private int pressTicks = PRESS_TICKS;
    private int retractTicks = RETRACT_TICKS;

    public NamingFactoryBlockEntity(SingleblockMachineDefinition<?, ?, ?> definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    // 模板槽只接受 AE2 名称压印模板
    @Override
    protected boolean isValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_TEMPLATE) {
            return AEItems.NAME_PRESS.isSameAs(stack);
        }
        return super.isValidForSlot(slot, stack);
    }

    // 压印阶段输入已被整体结算，禁止再放入；其余阶段都可继续收货
    @Override
    public boolean acceptsInputs() {
        return state != WorkState.PRESSING;
    }

    public ItemStack getTemplateStack() {
        return getInventory().getItem(SLOT_TEMPLATE);
    }

    public ItemStack getInputStack() {
        return getInventory().getItem(SLOT_INPUT);
    }

    public ItemStack getOutputStack() {
        return getInventory().getItem(SLOT_OUTPUT);
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

    // 名称压印工厂沿用自身的加速档位，与多方块机器的有损超频不同
    @Override
    public double getSpeedMultiplier() {
        return switch (getSpeedCardCount()) {
            case 1 -> 1.5D;
            case 2 -> 2.5D;
            case 3 -> 5.0D;
            case 4 -> 25.0D;
            default -> 1.0D;
        };
    }

    // 服务端每 tick 推进状态机：蓄力 → 下压输出 → 回升
    @Override
    protected void tickMachine() {
        long elapsed = level.getGameTime() - workStart;
        switch (state) {
            case IDLE -> {
                if (canWork() && hasEnergy(cost(getInputStack().getCount()))) {
                    updatePhaseDurations();
                    beginState(WorkState.CHARGING);
                }
            }
            case CHARGING -> {
                // 蓄力期间仍可继续收货，时长不重置；压印前按当时数量一次性结算
                if (elapsed >= chargeTicks) {
                    if (canWork() && consumeEnergy(cost(getInputStack().getCount()))) {
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

    // 加速卡只加速蓄力阶段
    private void updatePhaseDurations() {
        chargeTicks = scaledDuration(CHARGE_TICKS);
        pressTicks = PRESS_TICKS;
        retractTicks = RETRACT_TICKS;
    }

    private static double cost(int parallel) {
        return ModConfig.namingFactoryEnergyPerItem() * parallel;
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
            getInventory().setItem(SLOT_OUTPUT, result);
        } else if (ItemStack.isSameItemSameTags(output, result)) {
            output.grow(result.getCount());
            getInventory().setItem(SLOT_OUTPUT, output);
        } else {
            return;
        }
        getInventory().setItem(SLOT_INPUT, ItemStack.EMPTY);
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

    @Override
    protected void saveMachineData(CompoundTag tag) {
        tag.putByte(TAG_STATE, (byte) state.ordinal());
        tag.putLong(TAG_WORK_START, workStart);
        tag.putInt(TAG_CHARGE_TICKS, chargeTicks);
        tag.putInt(TAG_PRESS_TICKS, pressTicks);
        tag.putInt(TAG_RETRACT_TICKS, retractTicks);
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        if (tag.contains(TAG_STATE)) {
            int ordinal = tag.getByte(TAG_STATE);
            state = ordinal >= 0 && ordinal < WorkState.values().length ? WorkState.values()[ordinal] : WorkState.IDLE;
        }
        workStart = tag.getLong(TAG_WORK_START);
        chargeTicks = tag.contains(TAG_CHARGE_TICKS) ? tag.getInt(TAG_CHARGE_TICKS) : CHARGE_TICKS;
        pressTicks = tag.contains(TAG_PRESS_TICKS) ? tag.getInt(TAG_PRESS_TICKS) : PRESS_TICKS;
        retractTicks = tag.contains(TAG_RETRACT_TICKS) ? tag.getInt(TAG_RETRACT_TICKS) : RETRACT_TICKS;
    }

    @Override
    protected void writeMachineStream(FriendlyByteBuf data) {
        data.writeByte(state.ordinal());
        data.writeVarLong(workStart);
        data.writeVarInt(chargeTicks);
        data.writeVarInt(pressTicks);
        data.writeVarInt(retractTicks);
    }

    @Override
    protected void readMachineStream(FriendlyByteBuf data) {
        state = WorkState.values()[data.readByte()];
        workStart = data.readVarLong();
        chargeTicks = data.readVarInt();
        pressTicks = data.readVarInt();
        retractTicks = data.readVarInt();
    }
}
