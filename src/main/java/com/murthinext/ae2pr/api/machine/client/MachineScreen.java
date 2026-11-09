package com.murthinext.ae2pr.api.machine.client;

import com.murthinext.ae2pr.api.machine.client.MachineStyles;

import com.murthinext.ae2pr.api.machine.MachineMenu;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerScreen;

public final class MachineScreen<M extends AbstractMultiblockControllerMenu<?>> extends AbstractMultiblockControllerScreen<M> {

    private MachineModeButton modeButton;

    public MachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int toolbarButtons() {
        return hasModeSwitch() ? 2 : 1;
    }

    @Override
    protected void init() {
        super.init();
        if (menu instanceof MachineMenu<?> machineMenu && machineMenu.getMode() != null) {
            modeButton = new MachineModeButton(leftPos + TOOLBAR_X + 1,
                    topPos + TOOLBAR_Y + 1 + TOOLBAR_ROW_STEP, machineMenu.getMode(),
                    MachineStyles.of(menu.definition()).modeIcons(), () -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachineMenu.SWITCH_MODE);
                        }
                    });
            addRenderableWidget(modeButton);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (modeButton != null && menu instanceof MachineMenu<?> machineMenu && machineMenu.getMode() != null) {
            modeButton.setMode(machineMenu.getMode());
            modeButton.active = !menu.isBusy();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (modeLabel() != null) {
            graphics.drawString(font, modeLabel(), TEXT_X, EXTRA_Y, titleColor(), false);
        }
    }

    @Override
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (super.renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return true;
        }
        if (modeButton == null || !modeButton.isHovered() || modeLabel() == null) {
            return false;
        }
        GuiTooltips.render(graphics, font, List.of(modeLabel(),
                Component.translatable(menu.isBusy() ? "gui.ae2pr.multiblock.mode.locked"
                        : "gui.ae2pr.multiblock.mode.switch")),
                mouseX, mouseY);
        return true;
    }

    private boolean hasModeSwitch() {
        return menu instanceof MachineMenu<?> machineMenu && machineMenu.hasModeSwitch();
    }

    private Component modeLabel() {
        return menu instanceof MachineMenu<?> machineMenu && machineMenu.getMode() != null
                ? Component.translatable("gui.ae2pr.multiblock.mode", machineMenu.getMode().displayName())
                : null;
    }
}
