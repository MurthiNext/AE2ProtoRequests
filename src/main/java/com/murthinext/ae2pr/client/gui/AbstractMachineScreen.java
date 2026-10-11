package com.murthinext.ae2pr.client.gui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ItemLike;

import appeng.api.upgrades.Upgrades;

import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.machine_part.AutoTransferButton;

/**
 * 机器界面基类。
 */
public abstract class AbstractMachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    /** 左侧工具栏位置（相对 GUI 左上角） */
    private static final int TOOLBAR_X = -22;
    private static final int TOOLBAR_Y = 2;

    /** 右侧升级槽栏位置（相对 GUI 左上角）：面板 / 槽位装饰 / 单排行距 */
    private static final int UPGRADE_PANEL_X = 178;
    private static final int UPGRADE_PANEL_Y = 2;
    private static final int UPGRADE_SLOTS = 4;
    private static final int UPGRADE_SLOT_STEP = 20;

    @Nullable
    private GuideButton guideButton;

    protected AbstractMachineScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = MachineGui.WIDTH;
        this.imageHeight = MachineGui.HEIGHT;
    }

    /** 指南按钮要打开的页面。 */
    protected abstract ResourceLocation guidePage();

    /** 升级槽对应的机器物品（用于面板悬停提示）。 */
    protected abstract ItemLike upgradeMachine();

    /** 标题分隔线的机器点缀色。 */
    protected int separatorAccent() {
        return MachineGui.ACCENT_CRYSTAL;
    }

    /** 标题分隔线点缀色的暗色（分隔线下方 1px）。 */
    protected int separatorAccentDark() {
        return MachineGui.ACCENT_CRYSTAL_DARK;
    }

    @Override
    protected void init() {
        super.init();
        guideButton = new GuideButton(leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1,
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

    /** 绘制左侧工具栏底板（在 {@link #renderBg} 中调用）。 */
    protected void renderToolbar(GuiGraphics graphics) {
        AutoTransferButton.renderToolbar(graphics, leftPos + TOOLBAR_X, topPos + TOOLBAR_Y, 1);
    }

    /** 绘制右侧升级槽栏（在 {@link #renderBg} 中调用；槽位物品位与菜单中的坐标一致）。 */
    protected void renderUpgradeSlots(GuiGraphics graphics) {
        int x = leftPos + UPGRADE_PANEL_X;
        int y = topPos + UPGRADE_PANEL_Y;
        AutoTransferButton.renderToolbar(graphics, x, y, UPGRADE_SLOTS);
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            // 面板内缩 1px，槽位组件再按物品位左上角绘制（组件自带 1px 描边）
            MachineGui.drawSlot(graphics, x + 2, y + 2 + i * UPGRADE_SLOT_STEP);
        }
    }

    /** 工具栏按钮的悬停提示；返回 true 表示已处理。 */
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return guideButton != null && guideButton.renderTooltipIfHovered(graphics, font, mouseX, mouseY);
    }

    /** 升级槽栏的悬停提示（槽内已有物品时交给原版物品提示）；返回 true 表示已处理。 */
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
        int x = leftPos + UPGRADE_PANEL_X;
        int y = topPos + UPGRADE_PANEL_Y;
        int height = UPGRADE_SLOTS * UPGRADE_SLOT_STEP + 2;
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + height;
    }
}
