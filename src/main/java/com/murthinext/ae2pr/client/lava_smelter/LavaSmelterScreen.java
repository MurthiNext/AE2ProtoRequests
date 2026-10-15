package com.murthinext.ae2pr.client.lava_smelter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.AbstractMachineScreen;
import com.murthinext.ae2pr.client.gui.MachineGui;
import com.murthinext.ae2pr.client.gui.MultiblockInfoLayout;

/**
 * 高反应性熔岩冶炼炉主机界面。
 */
public class LavaSmelterScreen extends AbstractMachineScreen<LavaSmelterMenu> {

    private static final int TITLE_X = MultiblockInfoLayout.TEXT_X;
    private static final int TITLE_Y = 9;
    private static final int COLOR_TITLE = 0xFFC46B;
    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_DURABILITY = 0xFFC46B;

    private static final int DUST_SLOT_X = 151;
    private static final int DUST_SLOT_Y = 95;

    public LavaSmelterScreen(LavaSmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.LAVA_SMELTER_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get();
    }

    /**
     * 客户端同步入口，仅由同步包调用。
     */
    public static void applyJobSync(BlockPos pos, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof LavaSmelterControllerBlockEntity controller) {
            controller.applyClientJobOutput(stack);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        MachineGui.drawSlot(graphics, leftPos + DUST_SLOT_X, topPos + DUST_SLOT_Y);
    }

    @Override
    protected int separatorAccent() {
        return MachineGui.ACCENT_EMBER;
    }

    @Override
    protected int separatorAccentDark() {
        return MachineGui.ACCENT_EMBER_DARK;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, COLOR_TITLE, false);
        MultiblockInfoLayout.render(graphics, font, leftPos, topPos, new MultiblockInfoLayout.Info(
                menu.isFormed(), menu.isRunning(), menu.isPaused(), menu.isEnergyConnected(),
                menu.getNetworkStoredPower(), menu.getMaxParallel(), menu.getJobOutput(),
                menu.getJobElapsed(), menu.getJobDuration(), errorText()));
        int durability = menu.getDurability();
        graphics.drawString(font, Component.translatable("gui.ae2pr.lava_smelter.durability",
                durability, menu.getMaxDurability()),
                MultiblockInfoLayout.TEXT_X, MultiblockInfoLayout.EXTRA_Y,
                durability > 0 ? COLOR_DURABILITY : COLOR_FAIL, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderToolbarTooltip(graphics, mouseX, mouseY) || renderUpgradeTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        ItemStack output = menu.getJobOutput();
        if (!output.isEmpty() && MultiblockInfoLayout.isHoveringJobItem(leftPos, topPos, mouseX, mouseY)) {
            graphics.renderTooltip(font, Screen.getTooltipFromItem(Minecraft.getInstance(), output),
                    output.getTooltipImage(), output, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private Component errorText() {
        return switch (menu.getErrorCode()) {
            case 1 -> Component.translatable("gui.ae2pr.lava_smelter.error.durability");
            case 2 -> Component.translatable("gui.ae2pr.lava_smelter.error.output");
            default -> null;
        };
    }
}
