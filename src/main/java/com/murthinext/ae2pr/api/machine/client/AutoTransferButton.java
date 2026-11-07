package com.murthinext.ae2pr.api.machine.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AutoTransferButton extends AbstractWidget {

    public enum Type {
        PULL, PUSH
    }

    private static final int SIZE = GuiTextures.SIZE;
    private static final int COLOR_TOOLBAR_BACKGROUND = 0xFF101418;
    private static final int COLOR_TOOLBAR_BORDER = 0xFF39424E;

    private final Type type;
    private final BooleanSupplier state;
    private final Runnable onPress;

    public AutoTransferButton(int x, int y, Type type, BooleanSupplier state, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.type = type;
        this.state = state;
        this.onPress = onPress;
    }

    public static void renderToolbar(GuiGraphics graphics, int x, int y, int buttons) {
        int height = buttons * (SIZE + 2) + 2;
        graphics.fill(x, y, x + SIZE + 2, y + height, COLOR_TOOLBAR_BACKGROUND);
        graphics.fill(x, y, x + SIZE + 2, y + 1, COLOR_TOOLBAR_BORDER);
        graphics.fill(x, y + height - 1, x + SIZE + 2, y + height, COLOR_TOOLBAR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_TOOLBAR_BORDER);
        graphics.fill(x + SIZE + 1, y, x + SIZE + 2, y + height, COLOR_TOOLBAR_BORDER);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTextures.frame(graphics,
                isHovered() ? GuiTextures.BUTTON_HOVER : GuiTextures.BUTTON, getX(), getY());
        GuiTextures.frame(graphics, icon(), getX(), getY());
    }

    // 禁用态的斜线烘焙在 _off 材质里，这里只按状态选图
    private ResourceLocation icon() {
        boolean enabled = state.getAsBoolean();
        if (type == Type.PULL) {
            return enabled ? GuiTextures.PULL_ON : GuiTextures.PULL_OFF;
        }
        return enabled ? GuiTextures.PUSH_ON : GuiTextures.PUSH_OFF;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
