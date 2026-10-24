package com.murthinext.ae2pr.block.machine_part;

/**
 * 机器部件界面的槽位与储罐几何，菜单与界面共用。
 */
public final class MachinePartLayout {

    public static final int ITEM_Y = 47;
    public static final int SLOT_STEP = 18;

    private static final int ITEM_SINGLE_X = 80;
    private static final int ITEM_MULTI_X = 52;
    private static final int ITEM_DUAL_X = 98;

    private MachinePartLayout() {
    }

    public static int itemSlotX(boolean dual, int slotCount, int index) {
        if (dual) {
            return ITEM_DUAL_X + index % 2 * SLOT_STEP;
        }
        return slotCount > 1 ? ITEM_MULTI_X + index * SLOT_STEP : ITEM_SINGLE_X;
    }

    public static int itemSlotY(boolean dual, int index) {
        return ITEM_Y + (dual ? index / 2 * SLOT_STEP : 0);
    }
}
