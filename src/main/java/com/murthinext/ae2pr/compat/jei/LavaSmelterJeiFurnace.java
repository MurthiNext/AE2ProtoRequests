package com.murthinext.ae2pr.compat.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 熔岩冶炼炉 JEI 配方的炉体外观：引用炉体与输出管道贴图，并给出炉膛槽位坐标。
 */
public final class LavaSmelterJeiFurnace {

    /** 炉膛输入槽数量（两行三列） */
    public static final int SLOT_COUNT = 6;

    /** 炉体与输出管道贴图 */
    private static final ResourceLocation TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/jei/lava_smelter_furnace.png");

    /** 贴图在配方界面中的位置与尺寸 */
    private static final int X = 7;
    private static final int Y = 4;
    private static final int WIDTH = 104;
    private static final int HEIGHT = 54;

    /** 炉膛槽位尺寸与相对贴图左上角的偏移 */
    private static final int SLOT = 18;
    private static final int SLOT_OFFSET_X = 13;
    private static final int SLOT_OFFSET_Y = 8;

    private LavaSmelterJeiFurnace() {
    }

    /** 第 index 个炉膛输入槽的 x 坐标 */
    public static int slotX(int index) {
        return X + SLOT_OFFSET_X + (index % 3) * SLOT;
    }

    /** 第 index 个炉膛输入槽的 y 坐标 */
    public static int slotY(int index) {
        return Y + SLOT_OFFSET_Y + (index / 3) * SLOT;
    }

    /** 绘制炉体与输出管道贴图 */
    public static void draw(GuiGraphics graphics) {
        graphics.blit(TEXTURE, X, Y, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }
}
