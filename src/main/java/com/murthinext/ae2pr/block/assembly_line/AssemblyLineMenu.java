package com.murthinext.ae2pr.block.assembly_line;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.network.AssemblyLineJobPacket;

/**
 * 水晶装配线主机的容器菜单。
 */
public class AssemblyLineMenu extends AbstractContainerMenu {

    public static final MenuType<AssemblyLineMenu> TYPE = IForgeMenuType.create(AssemblyLineMenu::new);

    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 3;
    private static final int SLOT = 18;

    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    private final AssemblyLineControllerBlockEntity controller;
    private final Player owner;
    private final SimpleContainerData data = new SimpleContainerData(9);
    private final boolean clientSide;
    private ItemStack lastJobOutput = ItemStack.EMPTY;
    private boolean jobOutputSynced;

    public AssemblyLineMenu(int id, Inventory playerInventory, AssemblyLineControllerBlockEntity controller) {
        super(TYPE, id);
        this.controller = controller;
        this.owner = playerInventory.player;
        this.clientSide = playerInventory.player.level().isClientSide;

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

    public AssemblyLineMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, (AssemblyLineControllerBlockEntity) playerInventory.player.level()
                .getBlockEntity(buffer.readBlockPos()));
    }

    /** 每 tick 把成型/运行/暂停状态、ME 电力情况、暂停原因与作业进度写入同步数据：bit0=已成型，bit1=正在运行，bit2=暂停。 */
    @Override
    public void broadcastChanges() {
        if (!clientSide && controller != null) {
            int flags = (controller.isFormed() ? 1 : 0) | (controller.isRunning() ? 2 : 0)
                    | (controller.isPaused() ? 4 : 0);
            long power = (long) Math.min(Math.max(controller.getNetworkStoredPower(), 0), Long.MAX_VALUE);
            data.set(0, flags);
            data.set(1, controller.isEnergyConnected() ? 1 : 0);
            data.set(2, (int) (power & 0xFFFFFFFFL));
            data.set(3, (int) (power >>> 32));
            data.set(4, controller.getError().ordinal());
            data.set(5, controller.getJobElapsed());
            data.set(6, controller.getJobDuration());
            data.set(7, controller.maxParallel());
            data.set(8, (int) Math.round(controller.getSpeedMultiplier() * 100.0));

            ItemStack jobOutput = controller.getJobOutput();
            if (owner instanceof ServerPlayer serverPlayer
                    && (!jobOutputSynced || !ItemStack.matches(jobOutput, lastJobOutput))) {
                jobOutputSynced = true;
                lastJobOutput = jobOutput.copy();
                ModNetwork.sendToPlayer(serverPlayer,
                        new AssemblyLineJobPacket(controller.getBlockPos(), lastJobOutput));
            }
        }
        super.broadcastChanges();
    }

    public boolean isFormed() {
        return (data.get(0) & 1) != 0;
    }

    public boolean isRunning() {
        return (data.get(0) & 2) != 0;
    }

    /** 是否处于暂停态（电力或输出不足）。 */
    public boolean isPaused() {
        return (data.get(0) & 4) != 0;
    }

    /** 暂停原因序号（0 = 无，1 = 电力不足，2 = 输出不足）。 */
    public int getErrorCode() {
        return data.get(4);
    }

    /** 结构内是否有能源仓接入 ME 网络。 */
    public boolean isEnergyConnected() {
        return (data.get(1) & 1) != 0;
    }

    /** 结构内能源仓所接 ME 网络的可用能量合计（AE）。 */
    public long getNetworkStoredPower() {
        return (data.get(2) & 0xFFFFFFFFL) | ((long) data.get(3) << 32);
    }

    /** 当前作业的展示产物（数量已乘并行数）；空闲返回空。 */
    public ItemStack getJobOutput() {
        return controller != null ? controller.getClientJobOutput() : ItemStack.EMPTY;
    }

    /** 当前作业已进行的时间（tick）；空闲为 0。 */
    public int getJobElapsed() {
        return data.get(5);
    }

    /** 当前作业的实际总耗时（tick，含速度模块加成）；空闲为 0。 */
    public int getJobDuration() {
        return data.get(6);
    }

    /** 当前模块加成下的并行上限。 */
    public int getMaxParallel() {
        return data.get(7);
    }

    /** 当前模块加成下的速度百分比（100 = 基础速度）。 */
    public int getSpeedPercent() {
        return data.get(8);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 仅玩家背包，无需跨容器搬运
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return controller != null && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64.0;
    }
}
