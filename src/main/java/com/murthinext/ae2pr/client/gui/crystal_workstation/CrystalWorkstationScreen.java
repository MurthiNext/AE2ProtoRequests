package com.murthinext.ae2pr.client.gui.crystal_workstation;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.AbstractMachineScreen;
import com.murthinext.ae2pr.client.gui.MultiblockInfoLayout;

/**
 * 晶能工作站主机界面。
 */
public class CrystalWorkstationScreen extends AbstractMachineScreen<CrystalWorkstationMenu> {

    private static final int TITLE_X = MultiblockInfoLayout.TEXT_X;
    private static final int TITLE_Y = 9;
    private static final int COLOR_TITLE = 0x8EE6E0;

    private WorkstationModeButton modeButton;

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
    protected int toolbarButtons() {
        return 2;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = new WorkstationModeButton(leftPos + TOOLBAR_X + 1,
                topPos + TOOLBAR_Y + 1 + TOOLBAR_ROW_STEP, () -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                CrystalWorkstationMenu.SWITCH_MODE);
                    }
                });
        addRenderableWidget(modeButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMode(menu.getMode());
        modeButton.active = !menu.isBusy();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, COLOR_TITLE, false);
        Component error = switch (menu.getErrorCode()) {
            case 1 -> Component.translatable("gui.ae2pr.crystal_workstation.error.power");
            case 2 -> Component.translatable("gui.ae2pr.crystal_workstation.error.output");
            default -> null;
        };
        MultiblockInfoLayout.render(graphics, font, leftPos, topPos, new MultiblockInfoLayout.Info(
                menu.isFormed(), menu.isRunning(), menu.isPaused(), menu.isEnergyConnected(),
                menu.getNetworkStoredPower(), menu.getMaxParallel(), menu.getJobItem(),
                menu.getJobElapsed(), menu.getJobDuration(), error));
        if (menu.getJobItem().isEmpty()) {
            MultiblockInfoLayout.renderFluidJob(graphics, font, leftPos, topPos, menu.getJobFluid(),
                    menu.getJobElapsed(), menu.getJobDuration());
        }
        // 模式按钮在左侧工具栏，此处补一行文字说明当前模式
        graphics.drawString(font, Component.translatable("gui.ae2pr.crystal_workstation.mode",
                menu.getMode().displayName()), TITLE_X, MultiblockInfoLayout.EXTRA_Y, COLOR_TITLE, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderToolbarTooltip(graphics, mouseX, mouseY) || renderUpgradeTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        if (MultiblockInfoLayout.isHoveringJobItem(leftPos, topPos, mouseX, mouseY)) {
            ItemStack item = menu.getJobItem();
            if (!item.isEmpty()) {
                graphics.renderTooltip(font, Screen.getTooltipFromItem(minecraft, item), item.getTooltipImage(),
                        item, mouseX, mouseY);
                return;
            }
            FluidStack fluid = menu.getJobFluid();
            if (!fluid.isEmpty()) {
                graphics.renderComponentTooltip(font, List.of(fluid.getDisplayName(),
                        Component.translatable("gui.ae2pr.machine_part.tank.mb", fluid.getAmount())),
                        mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
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
        hint.add(Component.translatable("gui.ae2pr.crystal_workstation.mode", menu.getMode().displayName()));
        if (menu.isBusy()) {
            hint.add(Component.translatable("gui.ae2pr.crystal_workstation.mode.busy"));
        }
        graphics.renderComponentTooltip(font, hint, mouseX, mouseY);
        return true;
    }
}
