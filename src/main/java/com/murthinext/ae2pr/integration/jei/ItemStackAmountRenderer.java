package com.murthinext.ae2pr.integration.jei;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;

/**
 * 物品槽渲染。
 */
public class ItemStackAmountRenderer implements IIngredientRenderer<ItemStack> {

    public static final ItemStackAmountRenderer INSTANCE = new ItemStackAmountRenderer();

    @Override
    public void render(GuiGraphics graphics, ItemStack ingredient) {
        render(graphics, ingredient, 0, 0);
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack ingredient, int posX, int posY) {
        RenderSystem.enableDepthTest();
        graphics.renderFakeItem(ingredient, posX, posY);
        int count = ingredient.getCount();
        graphics.renderItemDecorations(Minecraft.getInstance().font, ingredient, posX, posY,
                JeiAmountFormat.abbreviated(count) ? JeiAmountFormat.item(count) : null);
        RenderSystem.disableBlend();
    }

    @Override
    public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
        return tooltip(ingredient, null, tooltipFlag);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ItemStack ingredient, @Nullable Player player,
            TooltipFlag tooltipFlag) {
        tooltip.addAll(tooltip(ingredient, player, tooltipFlag));
    }

    static List<Component> tooltip(ItemStack ingredient, @Nullable Player player, TooltipFlag tooltipFlag) {
        List<Component> lines = ingredient.getTooltipLines(player, tooltipFlag);
        int count = ingredient.getCount();
        if (!JeiAmountFormat.abbreviated(count)) {
            return lines;
        }
        List<Component> exact = new ArrayList<>(lines);
        exact.add(Component.literal("x" + JeiAmountFormat.exact(count)).withStyle(ChatFormatting.GRAY));
        return exact;
    }
}
