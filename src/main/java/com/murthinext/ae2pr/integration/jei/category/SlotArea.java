package com.murthinext.ae2pr.integration.jei.category;

// 一片按行列平铺的槽位区域，坐标为区域左上角；background 决定是否绘制槽位贴图
public record SlotArea(int count, int columns, int x, int y, boolean background) {

    public static final int SIZE = 18;

    public SlotArea(int count, int columns, int x, int y) {
        this(count, columns, x, y, true);
    }

    public static SlotArea empty() {
        return new SlotArea(0, 1, 0, 0, false);
    }

    public static SlotArea column(int count, int x, int y) {
        return new SlotArea(count, 1, x, y);
    }

    public int slotX(int index) {
        return x + index % columns * SIZE;
    }

    public int slotY(int index) {
        return y + index / columns * SIZE;
    }
}
