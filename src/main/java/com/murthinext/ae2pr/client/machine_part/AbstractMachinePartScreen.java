package com.murthinext.ae2pr.client.machine_part;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.murthinext.ae2pr.client.GuiExtraAreas;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.GuideButton;
import com.murthinext.ae2pr.client.MachineGui;

/**
 * 机器部件界面基类。
 */
public abstract class AbstractMachinePartScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
        implements GuiExtraAreas {

    private static final int TITLE_X = 7;
    private static final int TITLE_Y = 9;

    private static final int COLOR_TITLE = 0x55FFFF;

    @Nullable
    private GuideButton guideButton;
    @Nullable
    private AutoTransferButton autoTransferButton;

    protected AbstractMachinePartScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = MachineGui.WIDTH;
        this.imageHeight = MachineGui.HEIGHT;
    }

    protected abstract boolean isOutput();

    protected abstract boolean autoTransferEnabled();

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        return MachineGui.exclusionAreas(leftPos, topPos, toolbarButtons(), false);
    }

    protected int toolbarButtons() {
        return 2;
    }

    @Override
    protected void init() {
        super.init();
        guideButton = new GuideButton(leftPos + MachineGui.TOOLBAR_X + 1, topPos + MachineGui.TOOLBAR_Y + 1,
                () -> ModGuide.openAt(ModGuide.CHAMBERS_PAGE));
        addRenderableWidget(guideButton);
        autoTransferButton = new AutoTransferButton(leftPos + MachineGui.TOOLBAR_X + 1,
                topPos + MachineGui.TOOLBAR_Y + 1 + MachineGui.TOOLBAR_ROW_STEP,
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

    protected void renderPanel(GuiGraphics graphics) {
        MachineGui.drawPanel(graphics, leftPos, topPos);
        MachineGui.drawSeparator(graphics, leftPos, topPos, MachineGui.ACCENT_CRYSTAL, MachineGui.ACCENT_CRYSTAL_DARK);
        MachineGui.drawInventorySlots(graphics, leftPos, topPos);
    }

    protected void renderToolbar(GuiGraphics graphics) {
        AutoTransferButton.renderToolbar(graphics, leftPos + MachineGui.TOOLBAR_X, topPos + MachineGui.TOOLBAR_Y,
                toolbarButtons());
    }

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
