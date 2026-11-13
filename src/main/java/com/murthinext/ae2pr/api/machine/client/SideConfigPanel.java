package com.murthinext.ae2pr.api.machine.client;

import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.side.SideConfig;
import com.murthinext.ae2pr.api.machine.side.SideMode;

// 主界面左侧独立弹出的面配置窗口
public final class SideConfigPanel {

    public static final int WIDTH = 104;
    public static final int HEIGHT = 84;

    private static final int BUTTON = GuiTextures.SIZE;
    private static final int STEP = 22;
    private static final int ORIGIN_X = 6;
    private static final int ORIGIN_Y = 18;
    private static final int TOGGLE_X = 78;

    // 布局（列, 行）：顶 / 左 前 右 / 后 下
    private static final int[][] FACE_CELLS = {
        { 1, 0 }, // 顶
        { 0, 1 }, // 左
        { 1, 1 }, // 前
        { 2, 1 }, // 右
        { 0, 2 }, // 后
        { 1, 2 }, // 下
    };

    private static final ResourceLocation TEXTURE_PANEL = Ids.of("textures/gui/panel_popup.png");
    private static final int COLOR_TITLE = 0x55FFFF;

    private final SideConfig config;
    private final Direction facing;
    private final IntConsumer clickHandler;

    public SideConfigPanel(SideConfig config, Direction facing, IntConsumer clickHandler) {
        this.config = config;
        this.facing = facing;
        this.clickHandler = clickHandler;
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public void render(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
        graphics.blit(TEXTURE_PANEL, x, y, 0, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
        graphics.drawString(font, Component.translatable("gui.ae2pr.side_config.title"), x + ORIGIN_X, y + 5,
                            COLOR_TITLE, false);

        for (int index = 0; index < FACE_CELLS.length; index++) {
            int bx = x + cellX(FACE_CELLS[index]);
            int by = y + cellY(FACE_CELLS[index]);
            drawFaceButton(graphics, bx, by, config.get(orderedDirection(index)), mouseX, mouseY);
        }

        drawToggle(graphics, x, y, 0, GuiTextures.PULL_ON, GuiTextures.PULL_OFF, config.isAutoPull(), mouseX, mouseY);
        drawToggle(graphics, x, y, 1, GuiTextures.PUSH_ON, GuiTextures.PUSH_OFF, config.isAutoPush(), mouseX, mouseY);
        drawAction(graphics, x, y, 2, GuiTextures.CLEAR, mouseX, mouseY);

        renderTooltip(graphics, font, x, y, mouseX, mouseY);
    }

    // 返回 true 表示点击已被窗口消费
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y) {
        if (button != 0 || !contains(mouseX, mouseY, x, y)) {
            return false;
        }
        for (int index = 0; index < FACE_CELLS.length; index++) {
            int bx = x + cellX(FACE_CELLS[index]);
            int by = y + cellY(FACE_CELLS[index]);
            if (isInside(mouseX, mouseY, bx, by)) {
                clickHandler.accept(SideConfig.directionButton(orderedDirection(index)));
                return true;
            }
        }
        for (int row = 0; row < 3; row++) {
            if (isInside(mouseX, mouseY, x + toggleX(), y + rowY(row))) {
                clickHandler.accept(switch (row) {
                    case 0 -> SideConfig.BUTTON_AUTO_PULL;
                    case 1 -> SideConfig.BUTTON_AUTO_PUSH;
                    default -> SideConfig.BUTTON_CLEAR;
                });
                return true;
            }
        }
        return true;
    }

    // 六个面按钮只靠纯色区分模式，不用图标
    private static void drawFaceButton(GuiGraphics graphics, int x, int y, SideMode mode, int mouseX, int mouseY) {
        GuiTextures.solidBlock(graphics, x, y, modeColor(mode), isInside(mouseX, mouseY, x, y));
    }

    private static int modeColor(SideMode mode) {
        return switch (mode) {
            case NONE -> GuiTextures.COLOR_NONE;
            case INPUT -> GuiTextures.COLOR_INPUT;
            case OUTPUT -> GuiTextures.COLOR_OUTPUT;
            case BOTH -> GuiTextures.COLOR_BOTH;
        };
    }

    private void drawToggle(GuiGraphics graphics, int x, int y, int row, ResourceLocation on, ResourceLocation off,
                            boolean enabled, int mouseX, int mouseY) {
        drawIcon(graphics, x + toggleX(), y + rowY(row), enabled ? on : off, mouseX, mouseY);
    }

    private void drawAction(GuiGraphics graphics, int x, int y, int row, ResourceLocation icon, int mouseX,
                            int mouseY) {
        drawIcon(graphics, x + toggleX(), y + rowY(row), icon, mouseX, mouseY);
    }

    private static void drawIcon(GuiGraphics graphics, int bx, int by, ResourceLocation icon, int mouseX, int mouseY) {
        drawFrame(graphics, bx, by, mouseX, mouseY);
        graphics.blit(icon, bx, by, 0, 0.0F, 0.0F, BUTTON, BUTTON, BUTTON, BUTTON);
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        GuiTextures.frame(graphics, isInside(mouseX, mouseY, x, y) ? GuiTextures.BUTTON_HOVER : GuiTextures.BUTTON, x,
                          y);
    }

    private void renderTooltip(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
        for (int index = 0; index < FACE_CELLS.length; index++) {
            int bx = x + cellX(FACE_CELLS[index]);
            int by = y + cellY(FACE_CELLS[index]);
            if (!isInside(mouseX, mouseY, bx, by)) {
                continue;
            }
            Direction dir = orderedDirection(index);
            GuiTooltips.render(graphics, font,
                               List.of(Component.translatable("gui.ae2pr.side_config.face." + faceKey(dir)),
                                       config.get(dir).displayName()),
                               mouseX, mouseY);
            return;
        }
        int toggleX = x + toggleX();
        if (isInside(mouseX, mouseY, toggleX, y + rowY(0))) {
            GuiTooltips.render(graphics, font,
                               List.of(Component.translatable("gui.ae2pr.side_config.auto_pull"),
                                       stateLine(config.isAutoPull())),
                               mouseX, mouseY);
            return;
        }
        if (isInside(mouseX, mouseY, toggleX, y + rowY(1))) {
            GuiTooltips.render(graphics, font,
                               List.of(Component.translatable("gui.ae2pr.side_config.auto_push"),
                                       stateLine(config.isAutoPush())),
                               mouseX, mouseY);
            return;
        }
        if (isInside(mouseX, mouseY, toggleX, y + rowY(2))) {
            GuiTooltips.render(graphics, font, List.of(Component.translatable("gui.ae2pr.side_config.clear")), mouseX,
                               mouseY);
        }
    }

    private static Component stateLine(boolean enabled) {
        return Component.translatable(enabled ? "gui.ae2pr.side_config.enabled" : "gui.ae2pr.side_config.disabled");
    }

    // 0 顶，1 左，2 前，3 右，4 后，5 下；左右按玩家面向正面的直觉换算
    private Direction orderedDirection(int index) {
        return switch (index) {
            case 0 -> Direction.UP;
            case 1 -> facing.getClockWise();
            case 2 -> facing;
            case 3 -> facing.getCounterClockWise();
            case 4 -> facing.getOpposite();
            default -> Direction.DOWN;
        };
    }

    private String faceKey(Direction dir) {
        if (dir == Direction.UP) {
            return "up";
        }
        if (dir == Direction.DOWN) {
            return "down";
        }
        if (dir == facing) {
            return "front";
        }
        if (dir == facing.getOpposite()) {
            return "back";
        }
        return dir == facing.getClockWise() ? "left" : "right";
    }

    private static int cellX(int[] cell) {
        return ORIGIN_X + cell[0] * STEP;
    }

    private static int cellY(int[] cell) {
        return ORIGIN_Y + cell[1] * STEP;
    }

    private static int rowY(int row) {
        return ORIGIN_Y + row * STEP;
    }

    private static int toggleX() {
        return TOGGLE_X;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BUTTON && mouseY >= y && mouseY < y + BUTTON;
    }
}
