package com.murthinext.ae2pr.block.lava_smelter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeMenuType;

/**
 * 高反应性熔岩冶炼炉主机的容器菜单：同步结构状态与诊断信息并承载玩家背包。
 */
public class LavaSmelterMenu extends AbstractContainerMenu {

    public static final MenuType<LavaSmelterMenu> TYPE = IForgeMenuType.create(LavaSmelterMenu::new);

    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 3;
    private static final int SLOT = 18;

    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    /** 同步数据位：bit0=已成型，bit1=正在运行，bit2=暂停，bit3=存在不符位置 */
    private static final int FLAG_FORMED = 1;
    private static final int FLAG_RUNNING = 2;
    private static final int FLAG_PAUSED = 4;
    private static final int FLAG_HAS_MISMATCH_POS = 8;

    private final LavaSmelterControllerBlockEntity controller;
    private final Player owner;
    private final SimpleContainerData data = new SimpleContainerData(4);
    private final boolean clientSide;

    public LavaSmelterMenu(int id, Inventory playerInventory, LavaSmelterControllerBlockEntity controller) {
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

    public LavaSmelterMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, (LavaSmelterControllerBlockEntity) playerInventory.player.level()
                .getBlockEntity(buffer.readBlockPos()));
    }

    /** 每 tick 把成型/运行/暂停状态与不符诊断写入同步数据。 */
    @Override
    public void broadcastChanges() {
        if (!clientSide && controller != null) {
            BlockState state = controller.getBlockState();
            int flags = (controller.isFormed() ? FLAG_FORMED : 0)
                    | (state.getValue(HighReactivityLavaSmelterBlock.RUNNING) ? FLAG_RUNNING : 0)
                    | (state.getValue(HighReactivityLavaSmelterBlock.PAUSED) ? FLAG_PAUSED : 0);
            BlockPos mismatchPos = controller.getLastMismatchPos();
            long packed = mismatchPos != null ? mismatchPos.asLong() : 0L;
            if (mismatchPos != null) {
                flags |= FLAG_HAS_MISMATCH_POS;
            }
            data.set(0, flags);
            data.set(1, controller.getLastMismatches());
            data.set(2, (int) (packed & 0xFFFFFFFFL));
            data.set(3, (int) (packed >>> 32));
        }
        super.broadcastChanges();
    }

    public boolean isFormed() {
        return (data.get(0) & FLAG_FORMED) != 0;
    }

    public boolean isRunning() {
        return (data.get(0) & FLAG_RUNNING) != 0;
    }

    public boolean isPaused() {
        return (data.get(0) & FLAG_PAUSED) != 0;
    }

    /** 最近一次检测的不符方块数量。 */
    public int getMismatches() {
        return data.get(1);
    }

    /** 是否存在首个不符位置。 */
    public boolean hasMismatchPos() {
        return (data.get(0) & FLAG_HAS_MISMATCH_POS) != 0;
    }

    /** 首个不符位置（不存在时返回原点）。 */
    public BlockPos getMismatchPos() {
        long packed = (data.get(2) & 0xFFFFFFFFL) | ((long) data.get(3) << 32);
        return BlockPos.of(packed);
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
