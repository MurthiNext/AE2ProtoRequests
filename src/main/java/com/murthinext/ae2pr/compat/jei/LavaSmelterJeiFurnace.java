package com.murthinext.ae2pr.compat.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 熔岩冶炼炉 JEI 配方的炉体外观与槽位坐标。
 */
public final class LavaSmelterJeiFurnace {

    public static final int SLOT_COUNT = 6;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/gui/jei/lava_smelter_furnace.png");

    private static final int X = 7;
    private static final int Y = 4;
    private static final int WIDTH = 104;
    private static final int HEIGHT = 54;

    private static final int SLOT = 18;
    private static final int SLOT_OFFSET_X = 13;
    private static final int SLOT_OFFSET_Y = 8;

    private LavaSmelterJeiFurnace() {
    }

    public static int slotX(int index) {
        return X + SLOT_OFFSET_X + (index % 3) * SLOT;
    }

    public static int slotY(int index) {
        return Y + SLOT_OFFSET_Y + (index / 3) * SLOT;
    }

    public static void draw(GuiGraphics graphics) {
        graphics.blit(TEXTURE, X, Y, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }
}
