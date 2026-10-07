package com.murthinext.ae2pr.client.assembly_line;

import java.text.NumberFormat;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import appeng.util.ReadableNumberConverter;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineMenu;

/**
 * 水晶装配线主机界面。
 */
public class AssemblyLineScreen extends AbstractContainerScreen<AssemblyLineMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/crystal_assembly_line.png");

    private static final int TEXT_X = 7;
    private static final int TITLE_Y = 9;
    private static final int STATUS_Y = 30;
    private static final int POWER_Y = 44;
    private static final int ERROR_Y = 92;

    /** 作业展示区：标签行与图标行（与 GUI 贴图留白对齐） */
    private static final int JOB_LABEL_Y = 58;
    private static final int JOB_ITEM_X = 7;
    private static final int JOB_ITEM_Y = 70;
    private static final int JOB_TEXT_X = 27;
    private static final int JOB_TEXT_Y = 74;
    /** 名称区与数量/时间块之间的间距 */
    private static final int JOB_TAIL_GAP = 4;
    /** 名称滚动速度（像素/秒）与循环间距 */
    private static final double JOB_NAME_SCROLL_SPEED = 12.0D;
    private static final int JOB_NAME_SCROLL_GAP = 24;

    private static final int COLOR_TITLE = 0x55FFFF;
    private static final int COLOR_OK = 0x55FF55;
    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_RUNNING = 0xFFD060;
    private static final int COLOR_PAUSED = 0xFFDE00;
    private static final int COLOR_POWER = 0xACE9FF;
    private static final int COLOR_GRAY = 0x7A8794;

    public AssemblyLineScreen(AssemblyLineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
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
        // 作业产物图标（无作业时不绘制）
        ItemStack output = menu.getJobOutput();
        if (!output.isEmpty() && menu.getJobDuration() > 0) {
            graphics.renderItem(output, leftPos + JOB_ITEM_X, topPos + JOB_ITEM_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 第一行：结构名（左对齐）
        graphics.drawString(font, title, TEXT_X, TITLE_Y, COLOR_TITLE, false);
        // 第二行：状态（左对齐）
        graphics.drawString(font, statusText(), TEXT_X, STATUS_Y, statusColor(), false);
        // 第三行：电力连接情况（左对齐）
        graphics.drawString(font, powerText(), TEXT_X, POWER_Y,
                menu.isEnergyConnected() ? COLOR_POWER : COLOR_GRAY, false);
        // 作业行下方：暂停原因（左对齐，仅暂停时显示）
        Component error = errorText();
        if (error != null) {
            graphics.drawString(font, error, TEXT_X, ERROR_Y, COLOR_FAIL, false);
        }
        // 作业区：标签行 + 图标行（名称超宽时缓慢滚动，数量与时间固定右对齐）
        ItemStack output = menu.getJobOutput();
        int duration = menu.getJobDuration();
        if (output.isEmpty() || duration <= 0) {
            graphics.drawString(font, Component.translatable("gui.ae2pr.crystal_assembly_line.job.idle"),
                    TEXT_X, JOB_LABEL_Y, COLOR_GRAY, false);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.ae2pr.crystal_assembly_line.job.label"),
                TEXT_X, JOB_LABEL_Y, COLOR_POWER, false);
        String count = "x" + NumberFormat.getIntegerInstance().format(output.getCount());
        Component tail = Component.literal(count).append(Component.translatable(
                "gui.ae2pr.crystal_assembly_line.job.time",
                seconds(menu.getJobElapsed()), seconds(duration)));
        int tailX = imageWidth - 2 - font.width(tail);
        renderJobName(graphics, output.getHoverName().getString(), tailX - JOB_TAIL_GAP - JOB_TEXT_X);
        graphics.drawString(font, tail, tailX, JOB_TEXT_Y, COLOR_POWER, false);
    }

    /** 作业名称：空间不足时缓慢向左循环滚动，并裁剪在名称区内。 */
    private void renderJobName(GuiGraphics graphics, String name, int maxWidth) {
        if (maxWidth <= 0) {
            return;
        }
        int width = font.width(name);
        if (width <= maxWidth) {
            graphics.drawString(font, name, JOB_TEXT_X, JOB_TEXT_Y, COLOR_POWER, false);
            return;
        }
        int cycle = width + JOB_NAME_SCROLL_GAP;
        int shift = (int) (Util.getMillis() * JOB_NAME_SCROLL_SPEED / 1000.0D % cycle);
        // 名称按局部坐标绘制，裁剪矩形则用屏幕绝对坐标（不受 renderLabels 位姿平移影响）
        graphics.flush();
        graphics.enableScissor(leftPos + JOB_TEXT_X, topPos + JOB_TEXT_Y,
                leftPos + JOB_TEXT_X + maxWidth, topPos + JOB_TEXT_Y + font.lineHeight);
        graphics.drawString(font, name, JOB_TEXT_X - shift, JOB_TEXT_Y, COLOR_POWER, false);
        graphics.drawString(font, name, JOB_TEXT_X - shift + cycle, JOB_TEXT_Y, COLOR_POWER, false);
        graphics.flush();
        graphics.disableScissor();
    }

    /** 作业区物品悬停：显示产物 tooltip。 */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack output = menu.getJobOutput();
        if (!output.isEmpty() && isHoveringJobItem(mouseX, mouseY)) {
            graphics.renderTooltip(font, Screen.getTooltipFromItem(Minecraft.getInstance(), output),
                    output.getTooltipImage(), output, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean isHoveringJobItem(int mouseX, int mouseY) {
        int x = leftPos + JOB_ITEM_X;
        int y = topPos + JOB_ITEM_Y;
        return mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17;
    }

    private static String seconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    /** 暂停原因文本（无暂停时为 null）。 */
    private Component errorText() {
        return switch (menu.getErrorCode()) {
            case 1 -> Component.translatable("gui.ae2pr.crystal_assembly_line.error.power");
            case 2 -> Component.translatable("gui.ae2pr.crystal_assembly_line.error.output");
            default -> null;
        };
    }

    /** 电力连接情况：未连接 ME 网络，或显示所接网络的可用能量。 */
    private Component powerText() {
        if (!menu.isEnergyConnected()) {
            return Component.translatable("gui.ae2pr.crystal_assembly_line.power.disconnected");
        }
        return Component.translatable("gui.ae2pr.crystal_assembly_line.power.stored",
                ReadableNumberConverter.format(menu.getNetworkStoredPower(), 5) + " AE");
    }

    private Component statusText() {
        String key;
        if (!menu.isFormed()) {
            key = "gui.ae2pr.crystal_assembly_line.status.unformed";
        } else if (menu.isRunning()) {
            key = "gui.ae2pr.crystal_assembly_line.status.running";
        } else if (menu.isPaused()) {
            key = "gui.ae2pr.crystal_assembly_line.status.paused";
        } else {
            key = "gui.ae2pr.crystal_assembly_line.status.formed";
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
