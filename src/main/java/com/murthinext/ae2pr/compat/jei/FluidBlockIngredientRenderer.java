package com.murthinext.ae2pr.compat.jei;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.ingredients.IIngredientRenderer;

/** 流体方块。 */
public class FluidBlockIngredientRenderer implements IIngredientRenderer<FluidStack> {

    @Override
    public void render(GuiGraphics graphics, FluidStack fluidStack) {
        FluidBlockRendering.render(graphics, fluidStack.getFluid(), 0, 0, 18, 18);
    }

    @Override
    public List<Component> getTooltip(FluidStack fluidStack, TooltipFlag tooltipFlag) {
        return List.of(fluidStack.getDisplayName(), Component.literal(fluidStack.getAmount() + " mB"));
    }

    @Override
    public int getWidth() {
        return 18;
    }

    @Override
    public int getHeight() {
        return 18;
    }
}
