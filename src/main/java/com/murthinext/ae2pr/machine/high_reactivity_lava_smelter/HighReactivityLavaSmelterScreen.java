package com.murthinext.ae2pr.machine.high_reactivity_lava_smelter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;

public class HighReactivityLavaSmelterScreen extends AbstractMultiblockControllerScreen<HighReactivityLavaSmelterMenu> {

    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_DURABILITY = 0xFFC46B;

    private static final int DUST_SLOT_X = 151;
    private static final int DUST_SLOT_Y = 95;

    public HighReactivityLavaSmelterScreen(HighReactivityLavaSmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX,
            int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        drawSlot(graphics, leftPos + DUST_SLOT_X, topPos + DUST_SLOT_Y);
    }

    @Override
    protected void renderExtraLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        int durability = menu.getDurability();
        graphics.drawString(font, Component.translatable("gui.ae2pr.high_reactivity_lava_smelter.durability",
                durability, menu.getMaxDurability()),
                TEXT_X, EXTRA_Y,
                durability > 0 ? COLOR_DURABILITY : COLOR_FAIL, false);
    }

    @Override
    protected Component errorText(AbstractMultiblockControllerBlockEntity.Error error) {
        if (error == AbstractMultiblockControllerBlockEntity.Error.DURABILITY) {
            return Component.translatable("gui.ae2pr.high_reactivity_lava_smelter.error.durability");
        }
        return super.errorText(error);
    }
}
