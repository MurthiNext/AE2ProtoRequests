package com.murthinext.ae2pr.client.gui;

import java.text.NumberFormat;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import appeng.util.ReadableNumberConverter;

/**
 * 所有多方块机器界面共用的信息布局。
 */
public final class MultiblockInfoLayout {

    public static final int TEXT_X = 7;
    public static final int STATUS_Y = 30;
    public static final int POWER_Y = 41;
    public static final int PARALLEL_Y = 52;
    public static final int JOB_Y = 63;
    public static final int JOB_ITEM_X = 7;
    public static final int JOB_ITEM_Y = 74;
    public static final int JOB_TEXT_X = 27;
    public static final int JOB_TEXT_Y = 78;
    public static final int ERROR_Y = 92;
    public static final int EXTRA_Y = 104;

    private static final int WIDTH = 176;
    private static final int JOB_TAIL_GAP = 4;
    private static final double JOB_NAME_SCROLL_SPEED = 12.0D;
    private static final int JOB_NAME_SCROLL_GAP = 24;

    private static final int COLOR_OK = 0x55FF55;
    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_RUNNING = 0xFFD060;
    private static final int COLOR_PAUSED = 0xFFDE00;
    private static final int COLOR_INFO = 0xACE9FF;
    private static final int COLOR_GRAY = 0x7A8794;

    /**
     * 单台多方块机器的一次布局数据。
     */
    public record Info(boolean formed, boolean running, boolean paused, boolean energyConnected,
            double storedPower, int maxParallel, ItemStack jobOutput, int jobElapsed, int jobDuration,
            @Nullable Component error) {
    }

    private MultiblockInfoLayout() {
    }

    /**
     * 在 renderLabels 中调用。
     */
    public static void render(GuiGraphics graphics, Font font, int leftPos, int topPos, Info info) {
        // 成型状态
        graphics.drawString(font, Component.translatable(info.formed() ? "gui.ae2pr.multiblock.formed"
                : "gui.ae2pr.multiblock.unformed"),
                TEXT_X, STATUS_Y, info.formed() ? COLOR_OK : COLOR_FAIL, false);
        // 网络状态
        graphics.drawString(font, powerText(info), TEXT_X, POWER_Y,
                info.energyConnected() ? COLOR_INFO : COLOR_GRAY, false);
        // 最多同时执行的配方数
        graphics.drawString(font, Component.translatable("gui.ae2pr.multiblock.parallel", info.maxParallel()),
                TEXT_X, PARALLEL_Y, COLOR_INFO, false);
        // 合成状态
        graphics.drawString(font, Component.translatable(jobKey(info)), TEXT_X, JOB_Y, jobColor(info), false);
        // 暂停原因
        if (info.error() != null) {
            graphics.drawString(font, info.error(), TEXT_X, ERROR_Y, COLOR_FAIL, false);
        }
        renderJob(graphics, font, leftPos, topPos, info);
    }

    /**
     * mouseX/mouseY 为屏幕绝对坐标。
     */
    public static boolean isHoveringJobItem(int leftPos, int topPos, int mouseX, int mouseY) {
        int x = leftPos + JOB_ITEM_X;
        int y = topPos + JOB_ITEM_Y;
        return mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17;
    }

    public static void renderFluidJob(GuiGraphics graphics, Font font, int leftPos, int topPos, FluidStack fluid,
            int elapsed, int duration) {
        if (fluid.isEmpty() || duration <= 0) {
            return;
        }
        renderFluidIcon(graphics, fluid, JOB_ITEM_X, JOB_ITEM_Y);
        String amount = NumberFormat.getIntegerInstance().format(fluid.getAmount()) + " mB";
        Component tail = Component.literal(amount).append(Component.translatable(
                "gui.ae2pr.multiblock.job.time", seconds(elapsed), seconds(duration)));
        int tailX = WIDTH - 2 - font.width(tail);
        renderJobName(graphics, font, leftPos, topPos, fluid.getDisplayName().getString(),
                tailX - JOB_TAIL_GAP - JOB_TEXT_X);
        graphics.drawString(font, tail, tailX, JOB_TEXT_Y, COLOR_RUNNING, false);
    }

    private static void renderFluidIcon(GuiGraphics graphics, FluidStack fluid, int x, int y) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extensions.getStillTexture(fluid));
        int color = extensions.getTintColor(fluid);
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
        graphics.blit(x, y, 0, 16, 16, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderJob(GuiGraphics graphics, Font font, int leftPos, int topPos, Info info) {
        ItemStack output = info.jobOutput();
        int duration = info.jobDuration();
        if (output.isEmpty() || duration <= 0) {
            return;
        }
        graphics.renderItem(output, JOB_ITEM_X, JOB_ITEM_Y);
        String count = "x" + NumberFormat.getIntegerInstance().format(output.getCount());
        Component tail = Component.literal(count).append(Component.translatable(
                "gui.ae2pr.multiblock.job.time",
                seconds(info.jobElapsed()), seconds(duration)));
        int tailX = WIDTH - 2 - font.width(tail);
        renderJobName(graphics, font, leftPos, topPos, output.getHoverName().getString(),
                tailX - JOB_TAIL_GAP - JOB_TEXT_X);
        graphics.drawString(font, tail, tailX, JOB_TEXT_Y, COLOR_RUNNING, false);
    }

    private static void renderJobName(GuiGraphics graphics, Font font, int leftPos, int topPos, String name,
            int maxWidth) {
        if (maxWidth <= 0) {
            return;
        }
        int width = font.width(name);
        if (width <= maxWidth) {
            graphics.drawString(font, name, JOB_TEXT_X, JOB_TEXT_Y, COLOR_RUNNING, false);
            return;
        }
        int cycle = width + JOB_NAME_SCROLL_GAP;
        int shift = (int) (Util.getMillis() * JOB_NAME_SCROLL_SPEED / 1000.0D % cycle);
        // 名称用局部坐标，裁剪用屏幕绝对坐标
        graphics.flush();
        graphics.enableScissor(leftPos + JOB_TEXT_X, topPos + JOB_TEXT_Y,
                leftPos + JOB_TEXT_X + maxWidth, topPos + JOB_TEXT_Y + font.lineHeight);
        graphics.drawString(font, name, JOB_TEXT_X - shift, JOB_TEXT_Y, COLOR_RUNNING, false);
        graphics.drawString(font, name, JOB_TEXT_X - shift + cycle, JOB_TEXT_Y, COLOR_RUNNING, false);
        graphics.flush();
        graphics.disableScissor();
    }

    private static Component powerText(Info info) {
        if (!info.energyConnected()) {
            return Component.translatable("gui.ae2pr.multiblock.power.disconnected");
        }
        return Component.translatable("gui.ae2pr.multiblock.power.stored",
                ReadableNumberConverter.format((long) Math.min(Math.max(info.storedPower(), 0),
                        Long.MAX_VALUE), 5) + " AE");
    }

    private static String jobKey(Info info) {
        if (info.running()) {
            return "gui.ae2pr.multiblock.job.running";
        }
        return info.paused() ? "gui.ae2pr.multiblock.job.paused" : "gui.ae2pr.multiblock.job.idle";
    }

    private static int jobColor(Info info) {
        if (info.running()) {
            return COLOR_RUNNING;
        }
        return info.paused() ? COLOR_PAUSED : COLOR_GRAY;
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }
}
