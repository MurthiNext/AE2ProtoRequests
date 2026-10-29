package com.murthinext.ae2pr.client.multi_factory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryMenu;
import com.murthinext.ae2pr.client.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.client.GeneralMachineInfoLayout;
import com.murthinext.ae2pr.client.GeneralMachineScreen;
import com.murthinext.ae2pr.client.ModGuide;

public class MultiFactoryScreen extends AbstractMultiblockControllerScreen<MultiFactoryMenu> {

    private static final int COLOR_TITLE = 0xCDA6F0;

    private MultiFactoryModeButton modeButton;

    public MultiFactoryScreen(MultiFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.MULTI_FACTORY_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.MULTI_FACTORY.get();
    }

    @Override
    protected int titleColor() {
        return COLOR_TITLE;
    }

    @Override
    protected int toolbarButtons() {
        return 2;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = new MultiFactoryModeButton(leftPos + GeneralMachineScreen.TOOLBAR_X + 1,
                topPos + GeneralMachineScreen.TOOLBAR_Y + 1 + GeneralMachineScreen.TOOLBAR_ROW_STEP, () -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MultiFactoryMenu.SWITCH_MODE);
                    }
                });
        addRenderableWidget(modeButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMode(menu.getMode());
        modeButton.active = !menu.isBusy();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, currentMode(), GeneralMachineInfoLayout.TEXT_X,
                GeneralMachineInfoLayout.EXTRA_Y, COLOR_TITLE, false);
    }

    private Component currentMode() {
        return Component.translatable("gui.ae2pr.multi_factory.mode.current", menu.getMode().displayName());
    }

    @Override
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (super.renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return true;
        }
        if (modeButton == null || !modeButton.isHovered()) {
            return false;
        }
        List<Component> hint = new ArrayList<>();
        hint.add(currentMode());
        hint.add(Component.translatable(menu.isBusy()
                ? "gui.ae2pr.multi_factory.mode.locked" : "gui.ae2pr.multi_factory.mode.switch"));
        graphics.renderComponentTooltip(font, hint, mouseX, mouseY);
        return true;
    }
}
