package com.murthinext.ae2pr.api.machine.client;

import java.util.Map;
import java.util.Objects;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.murthinext.ae2pr.api.machine.Mode;

import net.minecraft.world.item.ItemStack;

public final class MachineModeButton extends AbstractWidget {

    private static final int SIZE = GuiTextures.SIZE;

    private final Map<Mode, Icon> icons;
    private final Runnable onPress;
    private Icon icon;

    public MachineModeButton(int x, int y, Mode mode, Map<Mode, Icon> icons, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.icons = icons;
        this.onPress = onPress;
        this.icon = Objects.requireNonNull(icons.get(mode), "Missing machine mode icon: " + mode.id());
        if (icon instanceof ItemIcon) {
            setMessage(Component.translatable("gui.ae2pr.multiblock.mode.switch"));
        }
    }

    public void setMode(Mode mode) {
        icon = Objects.requireNonNull(icons.get(mode), "Missing machine mode icon: " + mode.id());
        if (icon instanceof ItemIcon) {
            setMessage(Component.translatable("gui.ae2pr.multiblock.mode", mode.displayName()));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTextures.frame(graphics,
                isHovered() && active ? GuiTextures.BUTTON_HOVER : GuiTextures.BUTTON, getX(), getY());
        icon.render(graphics, getX() + 1, getY() + 1, active);
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

    public sealed interface Icon permits TextureIcon, ItemIcon {
        void render(GuiGraphics graphics, int x, int y, boolean active);
    }

    public record TextureIcon(ResourceLocation texture) implements Icon {
        @Override
        public void render(GuiGraphics graphics, int x, int y, boolean active) {
            if (!active) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 0.4F);
            }
            graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
            if (!active) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    public record ItemIcon(ItemStack stack) implements Icon {
        @Override
        public void render(GuiGraphics graphics, int x, int y, boolean active) {
            graphics.renderItem(stack, x, y);
            if (!active) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(x, y, x + 16, y + 16, 0x99101418);
                graphics.pose().popPose();
            }
        }
    }
}
