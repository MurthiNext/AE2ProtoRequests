package com.murthinext.ae2pr.integration.jei.category;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.api.Ids;

// 熔岩冶炼炉炉膛贴图与其内部的槽位坐标
public final class HighReactivityLavaSmelterFurnace {

    public static final int ITEM_COLS = 2;
    public static final int ITEM_ROWS = 2;
    public static final int ITEM_COUNT = ITEM_COLS * ITEM_ROWS;
    public static final int FLUID_COUNT = 2;
    public static final int SLOT_X = 21;
    public static final int SLOT_Y = 13;

    private static final ResourceLocation TEXTURE = Ids.of(
            "textures/gui/jei/high_reactivity_lava_smelter_furnace.png");
    private static final int X = 7;
    private static final int Y = 4;
    private static final int WIDTH = 104;
    private static final int HEIGHT = 54;
    private static final int STEP = 18;
    // 流体列在炉膛第三列
    private static final int FLUID_COL = 2;

    private HighReactivityLavaSmelterFurnace() {
    }

    public static int fluidSlotX(int index) {
        return SLOT_X + FLUID_COL * STEP;
    }

    public static void draw(GuiGraphics graphics) {
        graphics.blit(TEXTURE, X, Y, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }
}
