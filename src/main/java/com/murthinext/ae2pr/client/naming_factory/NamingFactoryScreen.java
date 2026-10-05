package com.murthinext.ae2pr.client.naming_factory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryMenu;

/**
 * 名称压印工厂界面：三槽位 + 加工进度条 + 名称文字。
 */
public class NamingFactoryScreen extends AbstractContainerScreen<NamingFactoryMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/naming_factory.png");

    /** 进度条内槽（与生成脚本 NF_PROGRESS 对应） */
    private static final int PROGRESS_X = 35;
    private static final int PROGRESS_Y = 95;
    private static final int PROGRESS_WIDTH = 106;
    private static final int PROGRESS_HEIGHT = 4;
    private static final int PROGRESS_COLOR = 0xFF8ABBEF;

    private static final int TEXT_X = 7;
    private static final int TITLE_Y = 9;
    private static final int NAME_Y = 103;

    private static final int COLOR_TITLE = 0x55FFFF;
    private static final int COLOR_VALUE = 0xACE9FF;
    private static final int COLOR_TEXT = 0xAAB8C6;
    private static final int COLOR_GRAY = 0x7A8794;

    public NamingFactoryScreen(NamingFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        NamingFactoryBlockEntity blockEntity = menu.getBlockEntity();
        float progress = blockEntity != null ? blockEntity.getWorkProgress(partialTick) : 0;
        int fill = Math.round(PROGRESS_WIDTH * progress);
        if (fill > 0) {
            graphics.fill(leftPos + PROGRESS_X, topPos + PROGRESS_Y,
                    leftPos + PROGRESS_X + fill, topPos + PROGRESS_Y + PROGRESS_HEIGHT, PROGRESS_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TEXT_X, TITLE_Y, COLOR_TITLE, false);

        drawSlotLabelAbove(graphics, "gui.ae2pr.naming_factory.template", 120, 50);
        drawSlotLabelLeft(graphics, "gui.ae2pr.naming_factory.input", 80, 26);
        drawSlotLabelLeft(graphics, "gui.ae2pr.naming_factory.output", 80, 74);

        NamingFactoryBlockEntity blockEntity = menu.getBlockEntity();
        String name = blockEntity != null ? blockEntity.getImprintName() : null;
        if (name == null) {
            graphics.drawString(font, Component.translatable("gui.ae2pr.naming_factory.name.none"),
                    TEXT_X, NAME_Y, COLOR_GRAY, false);
        } else {
            String text = Component.translatable("gui.ae2pr.naming_factory.name", name).getString();
            graphics.drawString(font, clip(text, imageWidth - TEXT_X * 2), TEXT_X, NAME_Y, COLOR_VALUE, false);
        }
    }

    /** 槽位上方居中标签（用于右侧模板槽）。 */
    private void drawSlotLabelAbove(GuiGraphics graphics, String key, int slotX, int slotY) {
        Component label = Component.translatable(key);
        graphics.drawString(font, label, slotX + (16 - font.width(label)) / 2, slotY - 10, COLOR_TEXT, false);
    }

    /** 槽位左侧标签（用于中列输入/输出槽）。 */
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
