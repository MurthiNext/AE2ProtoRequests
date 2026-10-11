package com.murthinext.ae2pr.client.matter_phase_transformer;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.AbstractMachineScreen;
import com.murthinext.ae2pr.client.gui.MachineGui;
import com.murthinext.ae2pr.client.gui.MultiblockInfoLayout;

/**
 * 物质相变仪主机界面。
 */
public class MatterPhaseTransformerScreen extends AbstractMachineScreen<MatterPhaseTransformerMenu> {

    private static final int TITLE_X = MultiblockInfoLayout.TEXT_X;
    private static final int TITLE_Y = 9;
    private static final int COLOR_TITLE = 0xFFC46B;

    public MatterPhaseTransformerScreen(MatterPhaseTransformerMenu menu, Inventory playerInventory,
            Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.MATTER_PHASE_TRANSFORMER_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.MATTER_PHASE_TRANSFORMER.get();
    }

    @Override
    protected int separatorAccent() {
        return MachineGui.ACCENT_EMBER;
    }

    @Override
    protected int separatorAccentDark() {
        return MachineGui.ACCENT_EMBER_DARK;
    }

    /**
     * 客户端同步入口，仅由同步包调用。
     */
    public static void applyJobSync(BlockPos pos, FluidStack fluid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof MatterPhaseTransformerControllerBlockEntity controller) {
            controller.applyClientJobFluid(fluid);
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
                menu.getNetworkStoredPower(), menu.getMaxParallel(), ItemStack.EMPTY,
                menu.getJobElapsed(), menu.getJobDuration(), errorText()));
        MultiblockInfoLayout.renderFluidJob(graphics, font, leftPos, topPos, menu.getJobFluid(),
                menu.getJobElapsed(), menu.getJobDuration());
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderToolbarTooltip(graphics, mouseX, mouseY) || renderUpgradeTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        FluidStack output = menu.getJobFluid();
        if (!output.isEmpty() && MultiblockInfoLayout.isHoveringJobItem(leftPos, topPos, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    output.getDisplayName(),
                    Component.translatable("gui.ae2pr.machine_part.tank.mb",
                            String.format(java.util.Locale.ROOT, "%,d", output.getAmount()))),
                    mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private Component errorText() {
        return switch (menu.getErrorCode()) {
            case 1 -> Component.translatable("gui.ae2pr.matter_phase.error.power");
            case 2 -> Component.translatable("gui.ae2pr.matter_phase.error.output");
            default -> null;
        };
    }
}
