package com.murthinext.ae2pr.client.level_emitter;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.level_emitter.CombineMode;

/**
 * 左侧工具栏的 AND/OR 组合模式切换按钮。
 */
public class CombineModeButton extends IconButton {

    private static final ResourceLocation AND_ICON = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/guis/combine_and.png");
    private static final ResourceLocation OR_ICON = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/guis/combine_or.png");

    private final Consumer<CombineMode> onToggle;
    private CombineMode mode = CombineMode.OR;

    public CombineModeButton(Consumer<CombineMode> onToggle) {
        super(button -> {
        });
        this.onToggle = onToggle;
    }

    public void setMode(CombineMode mode) {
        this.mode = mode;
    }

    @Override
    public void onPress() {
        this.mode = this.mode == CombineMode.AND ? CombineMode.OR : CombineMode.AND;
        this.onToggle.accept(this.mode);
    }

    @Override
    protected Icon getIcon() {
        return Icon.TOOLBAR_BUTTON_BACKGROUND;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!this.visible) {
            return;
        }

        Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter()
                .dest(getX(), getY())
                .opacity(this.active ? 1f : 0.5f)
                .blit(guiGraphics);

        var icon = this.mode == CombineMode.AND ? AND_ICON : OR_ICON;
        if (!this.active) {
            guiGraphics.setColor(1f, 1f, 1f, 0.5f);
        }
        guiGraphics.blit(icon, getX(), getY(), 0, 0, 16, 16, 16, 16);
        if (!this.active) {
            guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(this.mode == CombineMode.AND
                ? Component.translatable("gui.ae2pr.multi_level_emitter.combine.and")
                : Component.translatable("gui.ae2pr.multi_level_emitter.combine.or"));
    }
}
