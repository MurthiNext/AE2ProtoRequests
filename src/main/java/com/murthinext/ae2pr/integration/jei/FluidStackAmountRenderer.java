package com.murthinext.ae2pr.integration.jei;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;

public class FluidStackAmountRenderer implements IIngredientRenderer<FluidStack> {

    public static final FluidStackAmountRenderer INSTANCE = new FluidStackAmountRenderer();

    private static final float TEXT_SCALE = 0.5F;

    @Override
    public void render(GuiGraphics graphics, FluidStack ingredient) {
        render(graphics, ingredient, 0, 0);
    }

    @Override
    public void render(GuiGraphics graphics, FluidStack ingredient, int posX, int posY) {
        drawFluid(graphics, ingredient, posX, posY);
        Font font = Minecraft.getInstance().font;
        String amount = JeiAmountFormat.fluid(ingredient.getAmount());
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(posX + 17, posY + 17, 0);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        graphics.drawString(font, amount, -font.width(amount), -font.lineHeight, 0xFFFFFF, true);
        pose.popPose();
    }

    @Override
    public List<Component> getTooltip(FluidStack ingredient, TooltipFlag tooltipFlag) {
        return tooltipLines(ingredient, tooltipFlag);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, FluidStack ingredient, @Nullable Player player,
            TooltipFlag tooltipFlag) {
        tooltip.addAll(tooltipLines(ingredient, tooltipFlag));
    }

    private static void drawFluid(GuiGraphics graphics, FluidStack fluid, int posX, int posY) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extensions.getStillTexture(fluid));
        int color = extensions.getTintColor(fluid);
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
        graphics.blit(posX, posY, 0, 16, 16, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static List<Component> tooltipLines(FluidStack fluid, TooltipFlag tooltipFlag) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(displayName(fluid));
        if (tooltipFlag.isAdvanced()) {
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
            if (id != null) {
                lines.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lines.add(Component.translatable("jei.tooltip.liquid.amount", JeiAmountFormat.exact(fluid.getAmount()))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static Component displayName(FluidStack fluid) {
        Fluid type = fluid.getFluid();
        if (type.isSource(type.defaultFluidState())) {
            return fluid.getDisplayName();
        }
        return Component.translatable("jei.tooltip.liquid.flowing", fluid.getDisplayName());
    }
}
