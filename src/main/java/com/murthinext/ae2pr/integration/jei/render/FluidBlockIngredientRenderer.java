package com.murthinext.ae2pr.integration.jei.render;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.ingredients.IIngredientRenderer;

import com.murthinext.ae2pr.integration.jei.JeiAmountFormat;

public class FluidBlockIngredientRenderer implements IIngredientRenderer<FluidStack> {

    @Override
    public void render(GuiGraphics graphics, FluidStack fluidStack) {
        FluidBlockRendering.render(graphics, fluidStack.getFluid(), 0, 0, 18, 18);
    }

    @NotNull
    @Override
    public List<Component> getTooltip(FluidStack fluidStack, @Nullable Player player, TooltipFlag tooltipFlag) {
        return List.of(fluidStack.getDisplayName(),
                Component.translatable("jei.tooltip.liquid.amount", JeiAmountFormat.exact(fluidStack.getAmount())));
    }

    @Override
    @Deprecated(since = "15.54.0", forRemoval = true)
    @SuppressWarnings("removal")
    public List<Component> getTooltip(FluidStack fluidStack, TooltipFlag tooltipFlag) {
        return getTooltip(fluidStack, Minecraft.getInstance().player, tooltipFlag);
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
