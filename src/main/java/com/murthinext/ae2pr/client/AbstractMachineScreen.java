package com.murthinext.ae2pr.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ItemLike;

import appeng.api.upgrades.Upgrades;

import com.murthinext.ae2pr.client.machine_part.AutoTransferButton;

public abstract class AbstractMachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
        implements GuiExtraAreas {

    @Nullable
    private GuideButton guideButton;

    protected AbstractMachineScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GeneralMachineScreen.WIDTH;
        this.imageHeight = GeneralMachineScreen.HEIGHT;
    }

    protected abstract ResourceLocation guidePage();

    protected abstract ItemLike upgradeMachine();

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        return GeneralMachineScreen.exclusionAreas(leftPos, topPos, toolbarButtons(), true);
    }

    protected int separatorAccent() {
        return GeneralMachineScreen.ACCENT_CRYSTAL;
    }

    protected int separatorAccentDark() {
        return GeneralMachineScreen.ACCENT_CRYSTAL_DARK;
    }

    @Override
    protected void init() {
        super.init();
        guideButton = new GuideButton(leftPos + GeneralMachineScreen.TOOLBAR_X + 1, topPos + GeneralMachineScreen.TOOLBAR_Y + 1,
                () -> ModGuide.openAt(guidePage()));
        addRenderableWidget(guideButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GeneralMachineScreen.drawPanel(graphics, leftPos, topPos);
        GeneralMachineScreen.drawSeparator(graphics, leftPos, topPos, separatorAccent(), separatorAccentDark());
        GeneralMachineScreen.drawInventorySlots(graphics, leftPos, topPos);
        renderToolbar(graphics);
        renderUpgradeSlots(graphics);
    }

    protected void renderToolbar(GuiGraphics graphics) {
        AutoTransferButton.renderToolbar(graphics, leftPos + GeneralMachineScreen.TOOLBAR_X, topPos + GeneralMachineScreen.TOOLBAR_Y,
                toolbarButtons());
    }

    // 左侧工具栏占用的按钮格数，子类增加按钮时覆盖
    protected int toolbarButtons() {
        return 1;
    }

    protected void renderUpgradeSlots(GuiGraphics graphics) {
        int x = leftPos + GeneralMachineScreen.UPGRADE_PANEL_X;
        int y = topPos + GeneralMachineScreen.UPGRADE_PANEL_Y;
        AutoTransferButton.renderToolbar(graphics, x, y, GeneralMachineScreen.UPGRADE_SLOTS);
        for (int i = 0; i < GeneralMachineScreen.UPGRADE_SLOTS; i++) {
            // 面板内缩 1px，槽位组件自带 1px 描边
            GeneralMachineScreen.drawSlot(graphics, x + 2, y + 2 + i * GeneralMachineScreen.UPGRADE_SLOT_STEP);
        }
    }

    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return guideButton != null && guideButton.renderTooltipIfHovered(graphics, font, mouseX, mouseY);
    }

    protected boolean renderUpgradeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHoveringUpgradePanel(mouseX, mouseY)) {
            return false;
        }
        Slot slot = getSlotUnderMouse();
        if (slot != null && !slot.getItem().isEmpty()) {
            return false;
        }
        List<Component> lines = Upgrades.getTooltipLinesForMachine(upgradeMachine());
        if (lines.isEmpty()) {
            return false;
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
    }

    private boolean isHoveringUpgradePanel(int mouseX, int mouseY) {
        int x = leftPos + GeneralMachineScreen.UPGRADE_PANEL_X;
        int y = topPos + GeneralMachineScreen.UPGRADE_PANEL_Y;
        int height = GeneralMachineScreen.UPGRADE_SLOTS * GeneralMachineScreen.UPGRADE_SLOT_STEP + 2;
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + height;
    }
}
