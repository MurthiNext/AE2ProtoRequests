package com.murthinext.ae2pr.machine.naming_factory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.murthinext.ae2pr.api.machine.singleblock.AbstractSingleblockMachineScreen;

public class NamingFactoryScreen extends AbstractSingleblockMachineScreen<NamingFactoryMenu> {

    private static final int TEMPLATE_SLOT_X = 120;
    private static final int TEMPLATE_SLOT_Y = 50;
    private static final int INPUT_SLOT_X = 80;
    private static final int INPUT_SLOT_Y = 26;
    private static final int OUTPUT_SLOT_X = 80;
    private static final int OUTPUT_SLOT_Y = 74;
    private static final int PRESS_ARROW_X = 83;
    private static final int PRESS_ARROW_Y = 52;
    // 箭头右侧的竖向进度条
    private static final int PROGRESS_X = 96;
    private static final int PROGRESS_Y = 49;

    private static final int TEXT_X = 7;
    private static final int NAME_Y = 103;

    private static final int COLOR_VALUE = 0xACE9FF;
    private static final int COLOR_TEXT = 0xAAB8C6;
    private static final int COLOR_GRAY = 0x7A8794;

    public NamingFactoryScreen(NamingFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        drawSlot(graphics, leftPos + TEMPLATE_SLOT_X, topPos + TEMPLATE_SLOT_Y);
        drawSlot(graphics, leftPos + INPUT_SLOT_X, topPos + INPUT_SLOT_Y);
        drawSlot(graphics, leftPos + OUTPUT_SLOT_X, topPos + OUTPUT_SLOT_Y);
        drawArrow(graphics, leftPos + PRESS_ARROW_X, topPos + PRESS_ARROW_Y, true, true);
        NamingFactoryBlockEntity machine = menu.getMachine();
        float progress = machine != null ? machine.getWorkProgress(partialTick) : 0;
        drawProgressBar(graphics, leftPos + PROGRESS_X, topPos + PROGRESS_Y, progress);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        drawSlotLabelAbove(graphics, "gui.ae2pr.naming_factory.template", TEMPLATE_SLOT_X, TEMPLATE_SLOT_Y);
        drawSlotLabelLeft(graphics, "gui.ae2pr.naming_factory.input", INPUT_SLOT_X, INPUT_SLOT_Y);
        drawSlotLabelLeft(graphics, "gui.ae2pr.naming_factory.output", OUTPUT_SLOT_X, OUTPUT_SLOT_Y);

        NamingFactoryBlockEntity machine = menu.getMachine();
        String name = machine != null ? machine.getImprintName() : null;
        if (name == null) {
            graphics.drawString(font, Component.translatable("gui.ae2pr.naming_factory.name.none"),
                    TEXT_X, NAME_Y, COLOR_GRAY, false);
        } else {
            String text = Component.translatable("gui.ae2pr.naming_factory.name", name).getString();
            graphics.drawString(font, clip(text, imageWidth - TEXT_X * 2), TEXT_X, NAME_Y, COLOR_VALUE, false);
        }
    }

    private void drawSlotLabelAbove(GuiGraphics graphics, String key, int slotX, int slotY) {
        Component label = Component.translatable(key);
        graphics.drawString(font, label, slotX + (16 - font.width(label)) / 2, slotY - 10, COLOR_TEXT, false);
    }

    private void drawSlotLabelLeft(GuiGraphics graphics, String key, int slotX, int slotY) {
        Component label = Component.translatable(key);
        graphics.drawString(font, label, slotX - 4 - font.width(label), slotY + 4, COLOR_TEXT, false);
    }

    private String clip(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("…")) + "…";
    }
}
