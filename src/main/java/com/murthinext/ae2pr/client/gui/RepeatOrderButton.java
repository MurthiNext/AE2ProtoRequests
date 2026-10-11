package com.murthinext.ae2pr.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murthinext.ae2pr.ae2pr;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 工具栏"重复下单"按钮：自绘循环箭头图标。
 */
public class RepeatOrderButton extends IconButton {

    private static final Blitter ICON = Blitter.texture(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/guis/repeat_order.png"), 16, 16);

    public RepeatOrderButton(OnPress onPress) {
        super(onPress);
    }

    @Override
    protected Icon getIcon() {
        // 图标由 renderWidget 自绘；此返回值仅用于满足基类契约
        return Icon.HELP;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY()).blit(guiGraphics);
        // 自有贴图没有 src 矩形，必须显式给出目标尺寸，否则为零面积四边形（不可见）
        ICON.copy()
                .dest(getX(), getY(), getWidth(), getHeight())
                .blit(guiGraphics);

        RenderSystem.enableDepthTest();
    }
}
