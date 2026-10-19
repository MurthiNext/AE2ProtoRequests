package com.murthinext.ae2pr.client.gui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

/**
 * 多方块控制器界面的公共基底：信息布局、产物显示与工具提示。
 */
public abstract class AbstractMultiblockControllerScreen<M extends AbstractMultiblockControllerMenu<?>>
        extends AbstractMachineScreen<M> {

    private static final int TITLE_X = MultiblockInfoLayout.TEXT_X;
    private static final int TITLE_Y = 9;

    protected AbstractMultiblockControllerScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected abstract ResourceLocation guidePage();

    @Override
    protected abstract ItemLike upgradeMachine();

    protected abstract int titleColor();

    /**
     * 暂停原因，无暂停返回 null。
     */
    @Nullable
    protected Component errorText() {
        int code = menu.getErrorCode();
        if (code <= 0) {
            return null;
        }
        String key = switch (code) {
            case 1 -> "power";
            case 2 -> "output";
            case 3 -> "durability";
            default -> null;
        };
        return key == null ? null : Component.translatable(
                "gui.ae2pr." + menu.machineKey() + ".error." + key);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, titleColor(), false);
        MultiblockInfoLayout.render(graphics, font, leftPos, topPos, new MultiblockInfoLayout.Info(
                menu.isFormed(), menu.isRunning(), menu.isPaused(), menu.isEnergyConnected(),
                menu.getNetworkStoredPower(), menu.getMaxParallel(), menu.getJobItem(),
                menu.getJobElapsed(), menu.getJobDuration(), errorText()));
        if (menu.getJobItem().isEmpty()) {
            MultiblockInfoLayout.renderFluidJob(graphics, font, leftPos, topPos, menu.getJobFluid(),
                    menu.getJobElapsed(), menu.getJobDuration());
        }
        renderExtraLabels(graphics, mouseX, mouseY);
    }

    /**
     * 机器专属的信息行。
     */
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
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
                        Component.translatable("gui.ae2pr.machine_part.tank.mb",
                                String.format(java.util.Locale.ROOT, "%,d", fluid.getAmount()))),
                        mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }
}
