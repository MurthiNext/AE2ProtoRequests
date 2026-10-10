package com.murthinext.ae2pr.client.machine_part;

import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 仓室界面左侧工具栏按钮，切换自动搬运。
 */
public class AutoTransferButton extends AbstractWidget {

    public enum Type {
        PULL, PUSH
    }

    private static final int SIZE = 18;
    private static final int COLOR_BACKGROUND = 0xFF101418;
    private static final int COLOR_BORDER = 0xFF39424E;
    private static final int COLOR_BORDER_HOVER = 0xFF5A7A9A;
    private static final int COLOR_DISABLED_LINE = 0xFFB05555;

    private static final ResourceLocation ICON_PULL_ON = new ResourceLocation(ae2pr.MODID,
            "textures/guis/auto_transfer_pull_on.png");
    private static final ResourceLocation ICON_PULL_OFF = new ResourceLocation(ae2pr.MODID,
            "textures/guis/auto_transfer_pull_off.png");
    private static final ResourceLocation ICON_PUSH_ON = new ResourceLocation(ae2pr.MODID,
            "textures/guis/auto_transfer_push_on.png");
    private static final ResourceLocation ICON_PUSH_OFF = new ResourceLocation(ae2pr.MODID,
            "textures/guis/auto_transfer_push_off.png");

    private final Type type;
    private final BooleanSupplier state;
    private final Runnable onPress;

    public AutoTransferButton(int x, int y, Type type, BooleanSupplier state, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.type = type;
        this.state = state;
        this.onPress = onPress;
    }

    /** 绘制纵向工具栏底板（容纳 buttons 个按钮）。 */
    public static void renderToolbar(GuiGraphics graphics, int x, int y, int buttons) {
        int height = buttons * (SIZE + 2) + 2;
        graphics.fill(x, y, x + SIZE + 2, y + height, COLOR_BACKGROUND);
        graphics.fill(x, y, x + SIZE + 2, y + 1, COLOR_BORDER);
        graphics.fill(x, y + height - 1, x + SIZE + 2, y + height, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
        graphics.fill(x + SIZE + 1, y, x + SIZE + 2, y + height, COLOR_BORDER);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean enabled = state.getAsBoolean();
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                isHovered() ? COLOR_BORDER_HOVER : COLOR_BORDER);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, COLOR_BACKGROUND);
        graphics.blit(getIcon(enabled), getX(), getY(), 0, 0, width, height, SIZE, SIZE);
        if (!enabled) {
            for (int i = 0; i < 12; i++) {
                graphics.fill(getX() + 3 + i, getY() + 14 - i, getX() + 4 + i, getY() + 15 - i,
                        COLOR_DISABLED_LINE);
            }
        }
    }

    /** 当前类型与启用状态对应的图标贴图。 */
    private ResourceLocation getIcon(boolean enabled) {
        if (type == Type.PULL) {
            return enabled ? ICON_PULL_ON : ICON_PULL_OFF;
        }
        return enabled ? ICON_PUSH_ON : ICON_PUSH_OFF;
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
