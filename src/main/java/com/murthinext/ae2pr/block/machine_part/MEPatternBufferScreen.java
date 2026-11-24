package com.murthinext.ae2pr.block.machine_part;

import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.registry.ModGuide;
import com.murthinext.nextui.client.GuiSlots;
import com.murthinext.nextui.client.GuiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MEPatternBufferScreen extends AbstractMachineScreen<MEPatternBufferMenu> {

    private static final GuiTheme THEME = new GuiTheme(0xFFE0D0FF, 0xFFD1A6FF, 0x80634C8B);
    private static final int ONLINE = 0xFFCBABFF;
    private static final int OFFLINE = 0xFFAAA5B2;
    private static final int BUFFERED = 0xFFFFC46F;

    public MEPatternBufferScreen(MEPatternBufferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CHAMBERS_PAGE;
    }

    @Override
    protected GuiTheme theme() {
        return THEME;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        for (int i = 0; i < MEPatternBufferBlockEntity.SIZE; i++) {
            var slot = menu.getSlot(i);
            GuiSlots.draw(graphics, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        boolean online = menu.isOnline();
        var status = Component.translatable(
                online ? "gui.ae2pr.me_pattern_buffer.online" : "gui.ae2pr.me_pattern_buffer.offline");
        graphics.drawString(font, status, imageWidth - 8 - font.width(status), 9, online ? ONLINE : OFFLINE, false);
        for (int i = 0; i < MEPatternBufferBlockEntity.SIZE; i++) {
            if (menu.isLocked(i)) {
                var slot = menu.getSlot(i);
                graphics.fill(slot.x + 13, slot.y - 1, slot.x + 16, slot.y + 2, BUFFERED);
            }
        }
    }
}
