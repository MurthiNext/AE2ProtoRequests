package com.murthinext.ae2pr.api.machine.client;

import com.murthinext.nextui.client.GuiPanels;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MachineTabButton extends AbstractButton {
    // 页签为正方形，边长取按钮边长的 √2 倍。
    public static final int SIZE = (int) Math.round(GuiTextures.SIZE * Math.sqrt(2));
    private static final int COLOR_INACTIVE = 0xFF171D24;
    private static final int COLOR_HOVER = 0xFF202D39;

    private final ResourceLocation icon;
    private final BooleanSupplier selected;
    private final Runnable onPress;
    private final int accent;

    public MachineTabButton(
            int x,
            int y,
            ResourceLocation icon,
            Component narration,
            int accent,
            BooleanSupplier selected,
            Runnable onPress) {
        super(x, y, SIZE, SIZE, narration);
        this.icon = icon;
        this.accent = accent;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean current = selected.getAsBoolean();
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;
        int background = current ? GuiPanels.COLOR_BACKGROUND : isHoveredOrFocused() ? COLOR_HOVER : COLOR_INACTIVE;

        graphics.fill(x, y, right, bottom, GuiPanels.COLOR_BORDER);
        // 选中页签覆盖底板的顶边，内腔与主面板保持同色连通。
        graphics.fill(x + 1, y + 1, right - 1, bottom - (current ? 0 : 1), background);
        if (current || isHoveredOrFocused()) {
            graphics.fill(x + 1, y + 1, right - 1, y + 2, current ? accent : GuiPanels.COLOR_HIGHLIGHT);
        }
        graphics.blit(
                icon,
                x + Math.round((width - GuiTextures.SIZE) / 2.0F),
                y + Math.round((height - GuiTextures.SIZE) / 2.0F),
                0,
                0.0F,
                0.0F,
                GuiTextures.SIZE,
                GuiTextures.SIZE,
                GuiTextures.SIZE,
                GuiTextures.SIZE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
