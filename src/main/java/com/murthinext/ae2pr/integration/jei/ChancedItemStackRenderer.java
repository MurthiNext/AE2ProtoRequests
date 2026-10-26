package com.murthinext.ae2pr.integration.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;

import com.murthinext.ae2pr.recipe.ChancedItemStack;

/**
 * 带概率的物品槽渲染。
 */
public class ChancedItemStackRenderer implements IIngredientRenderer<ItemStack> {

    private static final float TEXT_SCALE = 0.5F;
    // 概率颜色
    private static final float MIN_HUE = 29.0F;
    private static final float MAX_HUE = 167.0F;

    private final float chance;

    public ChancedItemStackRenderer(float chance) {
        this.chance = chance;
    }

    public static ChancedItemStackRenderer of(List<ChancedItemStack> outputs, int index) {
        return new ChancedItemStackRenderer(index < outputs.size() ? outputs.get(index).chance() : 1.0F);
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack ingredient) {
        render(graphics, ingredient, 0, 0);
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack ingredient, int posX, int posY) {
        ItemStackAmountRenderer.INSTANCE.render(graphics, ingredient, posX, posY);
        if (chance >= 1.0F) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        String text = percent(chance);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(posX + 16, posY + 2, 300);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        graphics.drawString(font, text, -font.width(text), 0, color(chance), true);
        pose.popPose();
    }

    @Override
    public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
        List<Component> lines = new ArrayList<>(ItemStackAmountRenderer.tooltip(ingredient, null, tooltipFlag));
        if (chance < 1.0F) {
            lines.add(chanceTooltip(chance));
        }
        return lines;
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ItemStack ingredient, @Nullable Player player,
            TooltipFlag tooltipFlag) {
        tooltip.addAll(ItemStackAmountRenderer.tooltip(ingredient, player, tooltipFlag));
        if (chance < 1.0F) {
            tooltip.add(chanceTooltip(chance));
        }
    }

    public static Component chanceTooltip(float chance) {
        return Component.translatable("jei.ae2pr.chance", percent(chance)).withStyle(ChatFormatting.GRAY);
    }

    public static int color(float chance) {
        float hue = Mth.lerp(Mth.clamp(chance, 0.0F, 1.0F), MIN_HUE, MAX_HUE) / 360.0F;
        return 0xFF000000 | (Mth.hsvToRgb(hue, 1.0F, 1.0F) & 0xFFFFFF);
    }

    public static String percent(float chance) {
        float value = chance * 100.0F;
        if (Math.abs(value - Math.round(value)) < 0.05F) {
            return Math.round(value) + "%";
        }
        return String.format(Locale.ROOT, "%.1f%%", value);
    }
}
