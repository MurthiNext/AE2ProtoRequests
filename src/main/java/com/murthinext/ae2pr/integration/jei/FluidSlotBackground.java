package com.murthinext.ae2pr.integration.jei;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;

import com.murthinext.ae2pr.ae2pr;

/**
 * JEI 配方的流体槽背景。
 */
public final class FluidSlotBackground {

    public static final int SIZE = 18;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/gui/jei/fluid_slot.png");

    private FluidSlotBackground() {
    }

    public static IDrawable create(IGuiHelper guiHelper) {
        return guiHelper.drawableBuilder(TEXTURE, 0, 0, SIZE, SIZE).setTextureSize(SIZE, SIZE).build();
    }
}
