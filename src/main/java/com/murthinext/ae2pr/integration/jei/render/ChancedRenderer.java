package com.murthinext.ae2pr.integration.jei.render;

import com.murthinext.ae2pr.api.text.AmountText;
import com.murthinext.ae2pr.integration.jei.JeiRecipeInfo;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * 在任意原料渲染器上叠加概率角标，物品与流体共用。
 */
public class ChancedRenderer<T> implements IIngredientRenderer<T> {

    private static final float TEXT_SCALE = 0.5F;
    // 概率颜色
    private static final float MIN_HUE = 29.0F;
    private static final float MAX_HUE = 167.0F;

    private final IIngredientRenderer<T> delegate;
    private final float chance;

    public ChancedRenderer(IIngredientRenderer<T> delegate, float chance) {
        this.delegate = delegate;
        this.chance = chance;
    }

    @Override
    public void render(GuiGraphics graphics, T ingredient) {
        delegate.render(graphics, ingredient);
    }

    @Override
    public void render(GuiGraphics graphics, T ingredient, int posX, int posY) {
        delegate.render(graphics, ingredient, posX, posY);
        if (chance >= 1.0F) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        String text = AmountText.percent(chance, 1);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(posX + 16, posY + 2, 300);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        graphics.drawString(font, text, -font.width(text), 0, color(chance), true);
        pose.popPose();
    }

    @Override
    public List<Component> getTooltip(T ingredient, @Nullable Player player, TooltipFlag tooltipFlag) {
        List<Component> lines = new ArrayList<>(delegate.getTooltip(ingredient, player, tooltipFlag));
        if (chance < 1.0F) {
            lines.add(chanceTooltip(chance));
        }
        return lines;
    }

    @Override
    @Deprecated(since = "15.54.0", forRemoval = true)
    @SuppressWarnings("removal")
    public List<Component> getTooltip(T ingredient, TooltipFlag tooltipFlag) {
        return getTooltip(ingredient, Minecraft.getInstance().player, tooltipFlag);
    }

    public static Component chanceTooltip(float chance) {
        return JeiRecipeInfo.chance(chance);
    }

    public static int color(float chance) {
        float hue = Mth.lerp(Mth.clamp(chance, 0.0F, 1.0F), MIN_HUE, MAX_HUE) / 360.0F;
        return 0xFF000000 | (Mth.hsvToRgb(hue, 1.0F, 1.0F) & 0xFFFFFF);
    }
}
