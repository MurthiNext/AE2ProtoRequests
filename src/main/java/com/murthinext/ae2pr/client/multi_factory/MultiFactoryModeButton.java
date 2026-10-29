package com.murthinext.ae2pr.client.multi_factory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.block.multi_factory.MultiFactoryMode;

public class MultiFactoryModeButton extends AbstractWidget {

    private static final int SIZE = 18;
    private static final int COLOR_BACKGROUND = 0xFF101418;
    private static final int COLOR_BORDER = 0xFF39424E;
    private static final int COLOR_BORDER_HOVER = 0xFF5A7A9A;
    private static final ItemStack INSCRIBER = new ItemStack(AEBlocks.INSCRIBER.block());
    private static final ItemStack CHARGER = new ItemStack(AEBlocks.CHARGER.block());

    private final Runnable onPress;
    private MultiFactoryMode mode = MultiFactoryMode.INSCRIBING;

    public MultiFactoryModeButton(int x, int y, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.translatable("gui.ae2pr.multi_factory.mode.switch"));
        this.onPress = onPress;
    }

    public void setMode(MultiFactoryMode mode) {
        this.mode = mode;
        setMessage(Component.translatable("gui.ae2pr.multi_factory.mode.current", mode.displayName()));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                isHovered() && active ? COLOR_BORDER_HOVER : COLOR_BORDER);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, COLOR_BACKGROUND);
        graphics.renderItem(mode == MultiFactoryMode.CHARGING ? CHARGER : INSCRIBER, getX() + 1, getY() + 1);
        if (!active) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x99101418);
            graphics.pose().popPose();
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
