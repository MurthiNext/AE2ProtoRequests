package com.murthinext.ae2pr.api.machine.client;

import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.nextui.client.GuiTooltips;
import com.murthinext.nextui.client.IconButton;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class MachineModeButton extends AbstractButton {

    private final Map<Mode, Icon> icons;
    private final Runnable onPress;
    private Icon icon;

    public MachineModeButton(int x, int y, Mode mode, Map<Mode, Icon> icons, Runnable onPress) {
        super(x, y, IconButton.SIZE, IconButton.SIZE, Component.empty());
        this.icons = icons;
        this.onPress = onPress;
        setMode(mode);
    }

    public void setMode(Mode mode) {
        icon = Objects.requireNonNull(icons.get(mode), "Missing machine mode icon: " + mode.id());
        setMessage(Component.translatable("gui.ae2pr.multiblock.mode", mode.displayName()));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTextures.BUTTON_SKIN.drawFrame(graphics, getX(), getY(), active && GuiTooltips.isTooltipTarget(this), false);
        icon.render(graphics, getX() + 1, getY() + 1, active);
    }

    @Override
    public void onPress() {
        onPress.run();
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
