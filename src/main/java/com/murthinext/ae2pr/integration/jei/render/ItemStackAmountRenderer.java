package com.murthinext.ae2pr.integration.jei.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murthinext.ae2pr.api.text.AmountText;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

public class ItemStackAmountRenderer implements IIngredientRenderer<ItemStack> {

    public static final ItemStackAmountRenderer INSTANCE = new ItemStackAmountRenderer();

    private static final int AMOUNT_MAX_CHARS = 5;

    @Override
    public void render(GuiGraphics graphics, ItemStack ingredient) {
        render(graphics, ingredient, 0, 0);
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack ingredient, int posX, int posY) {
        RenderSystem.enableDepthTest();
        graphics.renderFakeItem(ingredient, posX, posY);
        int count = ingredient.getCount();
        graphics.renderItemDecorations(
                Minecraft.getInstance().font,
                ingredient,
                posX,
                posY,
                count > 1 ? AmountText.compact(count, AMOUNT_MAX_CHARS) : null);
        RenderSystem.disableBlend();
    }

    @Override
    public List<Component> getTooltip(ItemStack ingredient, @Nullable Player player, TooltipFlag tooltipFlag) {
        List<Component> lines = ingredient.getTooltipLines(player, tooltipFlag);
        int count = ingredient.getCount();
        if (!AmountText.abbreviated(count)) {
            return lines;
        }
        List<Component> exact = new ArrayList<>(lines);
        exact.add(Component.translatable("gui.ae2pr.amount.count", AmountText.exact(count))
                .withStyle(ChatFormatting.GRAY));
        return exact;
    }

    @Override
    @Deprecated(since = "15.54.0", forRemoval = true)
    @SuppressWarnings("removal")
    public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
        return getTooltip(ingredient, Minecraft.getInstance().player, tooltipFlag);
    }
}
