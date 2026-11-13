package com.murthinext.ae2pr.api.machine.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class MachineTabButton extends AbstractButton {
    public static final int HEIGHT = 24;
    public static final int WIDTH = (int) Math.round(HEIGHT * Math.sqrt(2));
    private static final int COLOR_INACTIVE = 0xFF171D24;
    private static final int COLOR_HOVER = 0xFF202D39;
    private static final int COLOR_TEXT = 0xFFAAB8C6;

    private final BooleanSupplier selected;
    private final Runnable onPress;
    private final int accent;

    public MachineTabButton(int x, int y, Component label, int accent, BooleanSupplier selected, Runnable onPress) {
        super(x, y, WIDTH, HEIGHT, label);
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
        if (isFocused()) {
            graphics.fill(x + 8, bottom - 4, right - 8, bottom - 3, accent);
        }
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, getMessage(), x + (width - font.width(getMessage())) / 2,
                            getY() + (height - font.lineHeight) / 2 + 1,
                            current || isHoveredOrFocused() ? accent : COLOR_TEXT, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
