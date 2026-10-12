package com.murthinext.ae2pr.client.crystal_reaction_chamber;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.AbstractMachineScreen;
import com.murthinext.ae2pr.client.gui.MultiblockInfoLayout;

/**
 * 水晶反应仓主机界面。
 */
public class CrystalReactionChamberScreen extends AbstractMachineScreen<CrystalReactionChamberMenu> {

    private static final int TITLE_X = MultiblockInfoLayout.TEXT_X;
    private static final int TITLE_Y = 9;
    private static final int COLOR_TITLE = 0x8EE6E0;

    public CrystalReactionChamberScreen(CrystalReactionChamberMenu menu, Inventory playerInventory,
            Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CRYSTAL_REACTION_CHAMBER_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.CRYSTAL_REACTION_CHAMBER.get();
    }

    /**
     * 客户端同步入口，仅由同步包调用。
     */
    public static void applyJobSync(BlockPos pos, ItemStack item, FluidStack fluid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level
                .getBlockEntity(pos) instanceof CrystalReactionChamberControllerBlockEntity controller) {
            controller.applyClientJob(item, fluid);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, COLOR_TITLE, false);
        MultiblockInfoLayout.render(graphics, font, leftPos, topPos, new MultiblockInfoLayout.Info(
                menu.isFormed(), menu.isRunning(), menu.isPaused(), menu.isEnergyConnected(),
                menu.getNetworkStoredPower(), menu.getMaxParallel(), menu.getJobItem(),
                menu.getJobElapsed(), menu.getJobDuration(), errorText()));
        // 无物品产物时改显示流体产物
        if (menu.getJobItem().isEmpty()) {
            MultiblockInfoLayout.renderFluidJob(graphics, font, leftPos, topPos, menu.getJobFluid(),
                    menu.getJobElapsed(), menu.getJobDuration());
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderToolbarTooltip(graphics, mouseX, mouseY) || renderUpgradeTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        if (MultiblockInfoLayout.isHoveringJobItem(leftPos, topPos, mouseX, mouseY)) {
            ItemStack item = menu.getJobItem();
            if (!item.isEmpty()) {
                graphics.renderTooltip(font, Screen.getTooltipFromItem(Minecraft.getInstance(), item),
                        item.getTooltipImage(), item, mouseX, mouseY);
                return;
            }
            FluidStack fluid = menu.getJobFluid();
            if (!fluid.isEmpty()) {
                graphics.renderComponentTooltip(font, List.of(
                        fluid.getDisplayName(),
                        Component.translatable("gui.ae2pr.machine_part.tank.mb",
                                String.format(java.util.Locale.ROOT, "%,d", fluid.getAmount()))),
                        mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private Component errorText() {
        return switch (menu.getErrorCode()) {
            case 1 -> Component.translatable("gui.ae2pr.crystal_reaction_chamber.error.power");
            case 2 -> Component.translatable("gui.ae2pr.crystal_reaction_chamber.error.output");
            default -> null;
        };
    }
}
