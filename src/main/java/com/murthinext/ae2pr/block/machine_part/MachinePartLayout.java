package com.murthinext.ae2pr.block.machine_part;

public final class MachinePartLayout {

    private static final int ITEM_Y = 47;
    private static final int SLOT_STEP = 18;
    private static final int ITEM_SINGLE_X = 80;
    private static final int ITEM_MULTI_X = 52;
    private static final int ITEM_DUAL_X = 98;
    private static final int ITEM_GRID_X = 62;
    private static final int ITEM_DUAL_GRID_X = 108;
    private static final int ITEM_GRID_Y = 43;

    private MachinePartLayout() {}

    public static int itemSlotX(boolean dual, int slotCount, int index) {
        if (slotCount > 4) {
            return (dual ? ITEM_DUAL_GRID_X : ITEM_GRID_X) + index % 3 * SLOT_STEP;
        }
        if (dual) {
            return ITEM_DUAL_X + index % 2 * SLOT_STEP;
        }
        return slotCount > 1 ? ITEM_MULTI_X + index * SLOT_STEP : ITEM_SINGLE_X;
    }

    public static int itemSlotY(boolean dual, int slotCount, int index) {
        if (slotCount > 4) {
            return ITEM_GRID_Y + index / 3 * SLOT_STEP;
        }
        return ITEM_Y + (dual ? index / 2 * SLOT_STEP : 0);
    }

    public static int itemCenterX(boolean dual, int slotCount) {
        int columns = slotCount > 4 ? 3 : dual ? 2 : slotCount;
        return (itemSlotX(dual, slotCount, 0) + itemSlotX(dual, slotCount, columns - 1)) / 2 + 8;
    }
}
