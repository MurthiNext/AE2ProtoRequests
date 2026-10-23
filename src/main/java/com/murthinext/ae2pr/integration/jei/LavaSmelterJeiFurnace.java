package com.murthinext.ae2pr.integration.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 熔岩冶炼炉 JEI 配方的炉体外观与槽位坐标。
 */
public final class LavaSmelterJeiFurnace {

    public static final int ITEM_COLS = 2;
    public static final int ITEM_ROWS = 2;
    public static final int ITEM_COUNT = ITEM_COLS * ITEM_ROWS;
    public static final int FLUID_COUNT = 2;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/gui/jei/lava_smelter_furnace.png");

    private static final int X = 7;
    private static final int Y = 4;
    private static final int WIDTH = 104;
    private static final int HEIGHT = 54;

    private static final int SLOT_X = X + 14;
    private static final int SLOT_Y = Y + 9;
    private static final int STEP = 18;
    // 流体列在炉膛第三列
    private static final int FLUID_COL = 2;

    private LavaSmelterJeiFurnace() {
    }

    public static int itemSlotX(int index) {
        return SLOT_X + (index % ITEM_COLS) * STEP;
    }

    public static int itemSlotY(int index) {
        return SLOT_Y + (index / ITEM_COLS) * STEP;
    }

    public static int fluidSlotX(int index) {
        return SLOT_X + FLUID_COL * STEP;
    }

    public static int fluidSlotY(int index) {
        return SLOT_Y + index * STEP;
    }

    public static void draw(GuiGraphics graphics) {
        graphics.blit(TEXTURE, X, Y, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }
}
