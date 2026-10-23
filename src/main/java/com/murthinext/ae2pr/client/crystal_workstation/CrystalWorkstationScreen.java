package com.murthinext.ae2pr.client.crystal_workstation;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.client.MultiblockInfoLayout;

/**
 * 晶能工作站主机界面。
 */
public class CrystalWorkstationScreen extends AbstractMultiblockControllerScreen<CrystalWorkstationMenu> {

    private static final int COLOR_TITLE = 0x8EE6E0;

    private CrystalWorkstationModeButton modeButton;

    public CrystalWorkstationScreen(CrystalWorkstationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CRYSTAL_WORKSTATION_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.CRYSTAL_WORKSTATION.get();
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
        modeButton = new CrystalWorkstationModeButton(leftPos + TOOLBAR_X + 1,
                topPos + TOOLBAR_Y + 1 + TOOLBAR_ROW_STEP, () -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                CrystalWorkstationMenu.SWITCH_MODE);
                    }
                });
        addRenderableWidget(modeButton);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMode(menu.getMode());
        modeButton.active = !menu.isBusy();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderExtraLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.ae2pr.crystal_workstation.mode",
                menu.getMode().displayName()), MultiblockInfoLayout.TEXT_X, MultiblockInfoLayout.EXTRA_Y,
                COLOR_TITLE, false);
    }

    @Override
    protected boolean renderToolbarTooltip(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        if (super.renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return true;
        }
        if (modeButton == null || !modeButton.isHovered()) {
            return false;
        }
        java.util.List<Component> hint = new java.util.ArrayList<>();
        hint.add(Component.translatable("gui.ae2pr.crystal_workstation.mode", menu.getMode().displayName()));
        if (menu.isBusy()) {
            hint.add(Component.translatable("gui.ae2pr.crystal_workstation.mode.busy"));
        }
        graphics.renderComponentTooltip(font, hint, mouseX, mouseY);
        return true;
    }
}
