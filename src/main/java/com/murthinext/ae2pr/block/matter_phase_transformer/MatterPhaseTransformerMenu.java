package com.murthinext.ae2pr.block.matter_phase_transformer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.SlotItemHandler;

import appeng.api.upgrades.Upgrades;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.network.MatterPhaseJobPacket;

/**
 * 物质相变仪主机的容器菜单。
 */
public class MatterPhaseTransformerMenu extends AbstractContainerMenu {

    public static final MenuType<MatterPhaseTransformerMenu> TYPE = IForgeMenuType.create(MatterPhaseTransformerMenu::new);

    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 3;
    private static final int SLOT = 18;

    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    /** 升级槽 */
    private static final int UPGRADE_SLOTS = 4;
    private static final int UPGRADE_X = 180;
    private static final int UPGRADE_Y = 4;
    private static final int UPGRADE_STEP = 20;
    /** 玩家背包起始下标 */
    private static final int PLAYER_SLOT_FIRST = UPGRADE_SLOTS;

    /** 同步数据位：bit0=已成型，bit1=正在运行，bit2=暂停 */
    private static final int FLAG_FORMED = 1;
    private static final int FLAG_RUNNING = 2;
    private static final int FLAG_PAUSED = 4;

    /** 同步数据下标 */
    private static final int DATA_FLAGS = 0;
    private static final int DATA_ERROR = 1;
    private static final int DATA_ELAPSED = 2;
    private static final int DATA_DURATION = 3;
    private static final int DATA_ENERGY_CONNECTED = 4;
    private static final int DATA_POWER_LO = 5;
    private static final int DATA_POWER_HI = 6;
    private static final int DATA_PARALLEL = 7;

    private final MatterPhaseTransformerControllerBlockEntity controller;
    private final Player owner;
    private final SimpleContainerData data = new SimpleContainerData(8);
    private final boolean clientSide;
    private FluidStack lastJobFluid = FluidStack.EMPTY;
    private boolean jobFluidSynced;

    public MatterPhaseTransformerMenu(int id, Inventory playerInventory,
            MatterPhaseTransformerControllerBlockEntity controller) {
        super(TYPE, id);
        this.controller = controller;
        this.owner = playerInventory.player;
        this.clientSide = playerInventory.player.level().isClientSide;

        if (controller != null) {
            var upgrades = controller.getUpgrades().toItemHandler();
            for (int i = 0; i < UPGRADE_SLOTS; i++) {
                addSlot(new SlotItemHandler(upgrades, i, UPGRADE_X, UPGRADE_Y + i * UPGRADE_STEP));
            }
        }
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * INV_COLS + INV_COLS,
                        INV_X + col * SLOT, INV_Y + row * SLOT));
            }
        }
        for (int col = 0; col < INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * SLOT, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public MatterPhaseTransformerMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, (MatterPhaseTransformerControllerBlockEntity) playerInventory.player.level()
                .getBlockEntity(buffer.readBlockPos()));
    }

    /** 每 tick 把状态与作业进度写入同步数据，并在作业产物变化时推送同步包。 */
    @Override
    public void broadcastChanges() {
        if (!clientSide && controller != null) {
            BlockState state = controller.getBlockState();
            int flags = (controller.isFormed() ? FLAG_FORMED : 0)
                    | (state.getValue(MatterPhaseTransformerBlock.RUNNING) ? FLAG_RUNNING : 0)
                    | (state.getValue(MatterPhaseTransformerBlock.PAUSED) ? FLAG_PAUSED : 0);
            data.set(DATA_FLAGS, flags);
            data.set(DATA_ERROR, controller.getError().ordinal());
            data.set(DATA_ELAPSED, controller.getJobElapsed());
            data.set(DATA_DURATION, controller.getJobDuration());
            long power = (long) Math.min(Math.max(controller.getNetworkStoredPower(), 0), Long.MAX_VALUE);
            data.set(DATA_ENERGY_CONNECTED, controller.isEnergyConnected() ? 1 : 0);
            data.set(DATA_POWER_LO, (int) (power & 0xFFFFFFFFL));
            data.set(DATA_POWER_HI, (int) (power >>> 32));
            data.set(DATA_PARALLEL, controller.maxParallel());

            FluidStack jobFluid = controller.getJobFluid();
            if (owner instanceof ServerPlayer serverPlayer
                    && (!jobFluidSynced || !sameFluid(jobFluid, lastJobFluid))) {
                jobFluidSynced = true;
                lastJobFluid = jobFluid.copy();
                ModNetwork.sendToPlayer(serverPlayer,
                        new MatterPhaseJobPacket(controller.getBlockPos(), lastJobFluid));
            }
        }
        super.broadcastChanges();
    }

    /** 两份流体是否完全相同（种类、NBT 与数量；都为空视为相同）。 */
    private static boolean sameFluid(FluidStack a, FluidStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return a.isFluidEqual(b) && a.getAmount() == b.getAmount();
    }

    public boolean isFormed() {
        return (data.get(DATA_FLAGS) & FLAG_FORMED) != 0;
    }

    public boolean isRunning() {
        return (data.get(DATA_FLAGS) & FLAG_RUNNING) != 0;
    }

    public boolean isPaused() {
        return (data.get(DATA_FLAGS) & FLAG_PAUSED) != 0;
    }

    /** 暂停原因序号（0 = 无，1 = 电力不足，2 = 输出仓无法容纳）。 */
    public int getErrorCode() {
        return data.get(DATA_ERROR);
    }

    /** 当前作业已进行的时间（tick）；空闲为 0。 */
    public int getJobElapsed() {
        return data.get(DATA_ELAPSED);
    }

    /** 当前作业的实际总耗时（tick，含加速卡加成）；空闲为 0。 */
    public int getJobDuration() {
        return data.get(DATA_DURATION);
    }

    /** 当前作业的展示产物（流体）；空闲返回空。 */
    public FluidStack getJobFluid() {
        return controller != null ? controller.getClientJobFluid() : FluidStack.EMPTY;
    }

    /** 结构内是否有能源仓接入 ME 网络。 */
    public boolean isEnergyConnected() {
        return (data.get(DATA_ENERGY_CONNECTED) & 1) != 0;
    }

    /** 结构内能源仓所接 ME 网络的可用能量合计（AE）。 */
    public long getNetworkStoredPower() {
        return (data.get(DATA_POWER_LO) & 0xFFFFFFFFL) | ((long) data.get(DATA_POWER_HI) << 32);
    }

    /** 结构允许的最大并行数（即最多同时执行的配方数）。 */
    public int getMaxParallel() {
        return data.get(DATA_PARALLEL);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_SLOT_FIRST) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_FIRST, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!Upgrades.isUpgradeCardItem(stack)
                || !moveItemStackTo(stack, 0, PLAYER_SLOT_FIRST, false)) {
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
        return controller != null && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64.0;
    }
}
