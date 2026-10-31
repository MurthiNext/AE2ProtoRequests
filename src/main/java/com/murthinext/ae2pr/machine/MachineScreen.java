package com.murthinext.ae2pr.machine;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.murthinext.ae2pr.client.MachineClients;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerScreen;

public final class MachineScreen<M extends AbstractMultiblockControllerMenu<?>> extends AbstractMultiblockControllerScreen<M> {

    private MachineModeButton modeButton;

    public MachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int toolbarButtons() {
        return menu instanceof ModeMachineMenu<?, ?> ? 2 : 1;
    }

    @Override
    protected void init() {
        super.init();
        if (menu instanceof ModeMachineMenu<?, ?> modeMenu) {
            modeButton = new MachineModeButton(leftPos + TOOLBAR_X + 1,
                    topPos + TOOLBAR_Y + 1 + TOOLBAR_ROW_STEP, modeMenu.getMode(),
                    MachineClients.style(menu.definition()).modeIcons(), () -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ModeMachineMenu.SWITCH_MODE);
                        }
                    });
            addRenderableWidget(modeButton);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (modeButton != null && menu instanceof ModeMachineMenu<?, ?> modeMenu) {
            modeButton.setMode(modeMenu.getMode());
            modeButton.active = !menu.isBusy();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu instanceof ModeMachineMenu<?, ?> modeMenu) {
            graphics.drawString(font, Component.translatable("gui.ae2pr.multiblock.mode", modeMenu.getMode().displayName()),
                    TEXT_X, EXTRA_Y, titleColor(), false);
        }
    }

    @Override
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (super.renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return true;
        }
        if (modeButton == null || !modeButton.isHovered() || !(menu instanceof ModeMachineMenu<?, ?> modeMenu)) {
            return false;
        }
        graphics.renderComponentTooltip(font, List.of(
                Component.translatable("gui.ae2pr.multiblock.mode", modeMenu.getMode().displayName()),
                Component.translatable(menu.isBusy() ? "gui.ae2pr.multiblock.mode.locked" : "gui.ae2pr.multiblock.mode.switch")),
                mouseX, mouseY);
        return true;
    }
}
