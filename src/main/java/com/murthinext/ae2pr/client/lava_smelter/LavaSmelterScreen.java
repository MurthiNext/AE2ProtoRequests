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
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.AbstractMachineScreen;
import com.murthinext.ae2pr.client.gui.MultiblockInfoLayout;

/**
 * 高反应性熔岩冶炼炉主机界面；信息区使用多方块机器统一布局，末尾补加配方耐久。
 */
public class LavaSmelterScreen extends AbstractMachineScreen<LavaSmelterMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/high_reactivity_lava_smelter.png");

    private static final int TITLE_X = MultiblockInfoLayout.TEXT_X;
    private static final int TITLE_Y = 9;
    private static final int COLOR_TITLE = 0xFFC46B;
    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_DURABILITY = 0xFFC46B;

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

    /** 客户端同步入口：把服务端当前作业产物写入本地方块实体（仅由同步包调用）。 */
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
        // 布局末尾：配方耐久
        int durability = menu.getDurability();
        graphics.drawString(font, Component.translatable("gui.ae2pr.lava_smelter.durability",
                durability, menu.getMaxDurability()),
                MultiblockInfoLayout.TEXT_X, MultiblockInfoLayout.EXTRA_Y,
                durability > 0 ? COLOR_DURABILITY : COLOR_FAIL, false);
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
            case 1 -> Component.translatable("gui.ae2pr.lava_smelter.error.durability");
            case 2 -> Component.translatable("gui.ae2pr.lava_smelter.error.power");
            case 3 -> Component.translatable("gui.ae2pr.lava_smelter.error.output");
            default -> null;
        };
    }
}
