package com.murthinext.ae2pr.client.crystal_workstation;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMode;

/**
 * 左侧工具栏的工作模式切换按钮，图标随当前模式变化。
 */
public class CrystalWorkstationModeButton extends AbstractWidget {

    private static final int SIZE = 18;
    private static final int ICON_OFFSET = 1;
    private static final int COLOR_BACKGROUND = 0xFF101418;
    private static final int COLOR_BORDER = 0xFF39424E;
    private static final int COLOR_BORDER_HOVER = 0xFF5A7A9A;
    private static final float DISABLED_OPACITY = 0.4F;

    private static final ResourceLocation ICON_ETCHING = icon("workstation_etching");
    private static final ResourceLocation ICON_CUTTING = icon("workstation_cutting");

    private final Runnable onPress;
    private CrystalWorkstationMode mode = CrystalWorkstationMode.ETCHING;

    public CrystalWorkstationModeButton(int x, int y, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.onPress = onPress;
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/guis/" + name + ".png");
    }

    public void setMode(CrystalWorkstationMode mode) {
        this.mode = mode;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlight = isHovered() && active;
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                highlight ? COLOR_BORDER_HOVER : COLOR_BORDER);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, COLOR_BACKGROUND);
        if (!active) {
            graphics.setColor(1.0F, 1.0F, 1.0F, DISABLED_OPACITY);
        }
        ResourceLocation texture = mode == CrystalWorkstationMode.CUTTING ? ICON_CUTTING : ICON_ETCHING;
        // 图标 16x16，面板内缩 1px 后居中
        graphics.blit(texture, getX() + ICON_OFFSET, getY() + ICON_OFFSET, 0, 0, 16, 16, 16, 16);
        if (!active) {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (active) {
            onPress.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
