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

/**
 * 机器界面基类。
 */
public abstract class AbstractMachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
        implements GuiExtraAreas {

    @Nullable
    private GuideButton guideButton;

    protected AbstractMachineScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = MachineGui.WIDTH;
        this.imageHeight = MachineGui.HEIGHT;
    }

    protected abstract ResourceLocation guidePage();

    protected abstract ItemLike upgradeMachine();

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        return MachineGui.exclusionAreas(leftPos, topPos, toolbarButtons(), true);
    }

    protected int separatorAccent() {
        return MachineGui.ACCENT_CRYSTAL;
    }

    protected int separatorAccentDark() {
        return MachineGui.ACCENT_CRYSTAL_DARK;
    }

    @Override
    protected void init() {
        super.init();
        guideButton = new GuideButton(leftPos + MachineGui.TOOLBAR_X + 1, topPos + MachineGui.TOOLBAR_Y + 1,
                () -> ModGuide.openAt(guidePage()));
        addRenderableWidget(guideButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGui.drawPanel(graphics, leftPos, topPos);
        MachineGui.drawSeparator(graphics, leftPos, topPos, separatorAccent(), separatorAccentDark());
        MachineGui.drawInventorySlots(graphics, leftPos, topPos);
        renderToolbar(graphics);
        renderUpgradeSlots(graphics);
    }

    protected void renderToolbar(GuiGraphics graphics) {
        AutoTransferButton.renderToolbar(graphics, leftPos + MachineGui.TOOLBAR_X, topPos + MachineGui.TOOLBAR_Y,
                toolbarButtons());
    }

    /**
     * 左侧工具栏占用的按钮格数，子类增加按钮时覆盖。
     */
    protected int toolbarButtons() {
        return 1;
    }

    protected void renderUpgradeSlots(GuiGraphics graphics) {
        int x = leftPos + MachineGui.UPGRADE_PANEL_X;
        int y = topPos + MachineGui.UPGRADE_PANEL_Y;
        AutoTransferButton.renderToolbar(graphics, x, y, MachineGui.UPGRADE_SLOTS);
        for (int i = 0; i < MachineGui.UPGRADE_SLOTS; i++) {
            // 面板内缩 1px，槽位组件自带 1px 描边
            MachineGui.drawSlot(graphics, x + 2, y + 2 + i * MachineGui.UPGRADE_SLOT_STEP);
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
        int x = leftPos + MachineGui.UPGRADE_PANEL_X;
        int y = topPos + MachineGui.UPGRADE_PANEL_Y;
        int height = MachineGui.UPGRADE_SLOTS * MachineGui.UPGRADE_SLOT_STEP + 2;
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + height;
    }
}
