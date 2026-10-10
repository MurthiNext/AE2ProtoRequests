package com.murthinext.ae2pr.client.assembly_line;

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
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.AbstractMachineScreen;
import com.murthinext.ae2pr.client.gui.MultiblockInfoLayout;

/**
 * 水晶装配线主机界面；信息区使用多方块机器统一布局。
 */
public class AssemblyLineScreen extends AbstractMachineScreen<AssemblyLineMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/crystal_assembly_line.png");

    private static final int TITLE_X = MultiblockInfoLayout.TEXT_X;
    private static final int TITLE_Y = 9;
    private static final int COLOR_TITLE = 0x55FFFF;

    public AssemblyLineScreen(AssemblyLineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CRYSTAL_ASSEMBLY_LINE_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.CRYSTAL_ASSEMBLY_LINE.get();
    }

    /** 客户端同步入口：把服务端当前作业产物写入本地方块实体（仅由同步包调用）。 */
    public static void applyJobSync(BlockPos pos, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity controller) {
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
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderToolbar(graphics);
        renderUpgradeSlots(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 第一行：结构名（左对齐）
        graphics.drawString(font, title, TITLE_X, TITLE_Y, COLOR_TITLE, false);
        // 统一信息布局：成型状态 / 网络状态 / 最多同时执行的配方数 / 合成状态
        MultiblockInfoLayout.render(graphics, font, leftPos, topPos, new MultiblockInfoLayout.Info(
                menu.isFormed(), menu.isRunning(), menu.isPaused(), menu.isEnergyConnected(),
                menu.getNetworkStoredPower(), menu.getMaxParallel(), menu.getJobOutput(),
                menu.getJobElapsed(), menu.getJobDuration(), errorText()));
    }

    /** 作业区物品悬停：显示产物 tooltip。 */
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

    /** 暂停原因文本（无暂停时为 null）。 */
    private Component errorText() {
        return switch (menu.getErrorCode()) {
            case 1 -> Component.translatable("gui.ae2pr.crystal_assembly_line.error.power");
            case 2 -> Component.translatable("gui.ae2pr.crystal_assembly_line.error.output");
            default -> null;
        };
    }
}
