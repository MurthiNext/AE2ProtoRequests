package com.murthinext.ae2pr.client.lava_smelter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterMenu;

/**
 * 高反应性熔岩冶炼炉主机界面：显示结构状态与首个不符位置。
 */
public class LavaSmelterScreen extends AbstractContainerScreen<LavaSmelterMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/high_reactivity_lava_smelter.png");

    private static final int TEXT_X = 7;
    private static final int TITLE_Y = 9;
    private static final int STATUS_Y = 30;
    private static final int INFO_Y = 44;

    private static final int COLOR_TITLE = 0xFFC46B;
    private static final int COLOR_OK = 0x55FF55;
    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_RUNNING = 0xFFD060;
    private static final int COLOR_PAUSED = 0xFFDE00;
    private static final int COLOR_GRAY = 0x7A8794;

    public LavaSmelterScreen(LavaSmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
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
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 第一行：结构名（左对齐）
        graphics.drawString(font, title, TEXT_X, TITLE_Y, COLOR_TITLE, false);
        // 第二行：状态（左对齐）
        graphics.drawString(font, statusText(), TEXT_X, STATUS_Y, statusColor(), false);
        // 第三行：不符诊断（仅未成型时显示）
        if (!menu.isFormed()) {
            graphics.drawString(font, mismatchText(), TEXT_X, INFO_Y, COLOR_GRAY, false);
        }
    }

    /** 不符诊断：有位置时追加首个不符坐标。 */
    private Component mismatchText() {
        int count = menu.getMismatches();
        if (menu.hasMismatchPos()) {
            BlockPos pos = menu.getMismatchPos();
            return Component.translatable("gui.ae2pr.lava_smelter.mismatch", count,
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        }
        return Component.translatable("gui.ae2pr.lava_smelter.mismatch.none", count);
    }

    private Component statusText() {
        String key;
        if (!menu.isFormed()) {
            key = "gui.ae2pr.lava_smelter.status.unformed";
        } else if (menu.isRunning()) {
            key = "gui.ae2pr.lava_smelter.status.running";
        } else if (menu.isPaused()) {
            key = "gui.ae2pr.lava_smelter.status.paused";
        } else {
            key = "gui.ae2pr.lava_smelter.status.formed";
        }
        return Component.translatable(key);
    }

    private int statusColor() {
        if (!menu.isFormed()) {
            return COLOR_FAIL;
        }
        if (menu.isRunning()) {
            return COLOR_RUNNING;
        }
        return menu.isPaused() ? COLOR_PAUSED : COLOR_OK;
    }
}
