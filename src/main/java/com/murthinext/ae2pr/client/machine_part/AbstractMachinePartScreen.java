package com.murthinext.ae2pr.client.machine_part;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.GuideButton;

/**
 * 机器部件界面基类。
 */
public abstract class AbstractMachinePartScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    /** 左侧工具栏位置 */
    private static final int TOOLBAR_X = -22;
    private static final int TOOLBAR_Y = 2;
    private static final int TOOLBAR_ROW_STEP = 20;

    private static final int TITLE_X = 7;
    private static final int TITLE_Y = 9;

    private static final int COLOR_TITLE = 0x55FFFF;

    @Nullable
    private GuideButton guideButton;
    @Nullable
    private AutoTransferButton autoTransferButton;

    protected AbstractMachinePartScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    /** 输出部件（自动推出）还是输入部件（自动拉取）。 */
    protected abstract boolean isOutput();

    /** 自动搬运开关当前状态（客户端为同步数据）。 */
    protected abstract boolean autoTransferEnabled();

    @Override
    protected void init() {
        super.init();
        guideButton = new GuideButton(leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1,
                () -> ModGuide.openAt(ModGuide.CHAMBERS_PAGE));
        addRenderableWidget(guideButton);
        autoTransferButton = new AutoTransferButton(leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1 + TOOLBAR_ROW_STEP,
                isOutput() ? AutoTransferButton.Type.PUSH : AutoTransferButton.Type.PULL,
                this::autoTransferEnabled,
                () -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0));
        addRenderableWidget(autoTransferButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /** 绘制左侧工具栏底板（在 {@link #renderBg} 中调用）。 */
    protected void renderToolbar(GuiGraphics graphics) {
        AutoTransferButton.renderToolbar(graphics, leftPos + TOOLBAR_X, topPos + TOOLBAR_Y, 2);
    }

    /** 工具栏按钮的悬停提示；返回 true 表示已处理。 */
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (guideButton != null && guideButton.renderTooltipIfHovered(graphics, font, mouseX, mouseY)) {
            return true;
        }
        if (autoTransferButton == null || !autoTransferButton.isHovered()) {
            return false;
        }
        graphics.renderComponentTooltip(font, List.of(
                Component.translatable(isOutput() ? "gui.ae2pr.machine_part.auto.push"
                        : "gui.ae2pr.machine_part.auto.pull"),
                Component.translatable(autoTransferEnabled() ? "gui.ae2pr.machine_part.auto.enabled"
                        : "gui.ae2pr.machine_part.auto.disabled"),
                Component.translatable("gui.ae2pr.machine_part.auto.desc")),
                mouseX, mouseY);
        return true;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, COLOR_TITLE, false);
    }
}
