package com.murthinext.ae2pr.client.proto_terminal;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/** 水晶叙事界面的连续矢量面板与描边。 */
final class CrystalNarrativeGraphics {

    static final int BACKGROUND = 0xFF030817;
    static final int PANEL = 0xFF070E1D;
    static final int BORDER = 0xFF6190B9;
    static final int ACCENT = 0xFF9AC8E8;
    static final int TEXT = 0xFFD2E6FA;
    static final int MUTED = 0xFF8AA6C4;
    static final float EDGE_SLOPE = 38.0F / 324;

    private CrystalNarrativeGraphics() {
    }

    static float slant(float height) {
        return height * EDGE_SLOPE;
    }

    static void panel(GuiGraphics graphics, float x, float y, int width, int height, float slant,
            int fill, int border) {
        if (width <= slant || height <= 0) {
            return;
        }
        graphics.drawManaged(() -> {
            quad(graphics, x + slant, y, x, y + height, x + width - slant, y + height, x + width, y, fill, fill);
            outline(graphics, x, y, width, height, slant, border);
        });
    }

    /** 固定星云底面与连续的平行四边形边框。 */
    static void cosmicPanel(GuiGraphics graphics, int x, int y, int width, int height, float slant,
            float progress, float seconds, int spaceY, int spaceHeight) {
        if (width <= slant || height <= 0 || progress <= 0) {
            return;
        }
        graphics.drawManaged(() -> {
            for (int top = y; top < y + height;) {
                int bottom = Math.min(y + height, spaceY + (Math.floorDiv(top - spaceY, 8) + 1) * 8);
                float leftTop = x + slant * (1 - (top - y) / (float) height);
                float leftBottom = x + slant * (1 - (bottom - y) / (float) height);
                quad(graphics, leftTop, top, leftBottom, bottom, leftBottom + width - slant, bottom,
                        leftTop + width - slant, top, spaceColor(top, spaceY, spaceHeight),
                        spaceColor(bottom, spaceY, spaceHeight));
                top = bottom;
            }
            outline(graphics, x, y, width, height, slant, alpha(BORDER, ease(progress)));
            if (progress < 1) {
                for (int edge = 0; edge < 4; edge++) {
                    openingPixels(graphics, x, y, width, height, slant, edge, progress, seconds);
                }
            }
        });
    }

    /** 入场时沿四条边缘汇聚的离散像素。 */
    private static void openingPixels(GuiGraphics graphics, int x, int y, int width, int height,
            float slant, int edge, float progress, float seconds) {
        boolean horizontal = edge < 2;
        int length = horizontal ? (int) Math.ceil(width - slant) : height;
        float remaining = 1 - progress;
        for (int step = 0; step < length; step += 4) {
            int hash = (step + edge * 739) * 0x45D9F3B;
            hash = (hash ^ (hash >>> 16)) * 0x45D9F3B;
            float seed = (hash & 0xFFFF) / 65535.0F;
            float phase = seconds * (11 + seed * 9) + seed * 35;
            float scatter = remaining * remaining;
            float px = horizontal ? x + step + (edge == 0 ? slant : 0)
                    : x + slant * (1 - step / (float) height) + (edge == 3 ? width - slant : 0);
            float py = horizontal ? y + (edge == 1 ? height : 0) : y + step;
            int dx = Math.round(Mth.sin(phase) * (6 + seed * 19) * scatter);
            int dy = Math.round(Mth.cos(phase * 1.3F) * (6 + (1 - seed) * 19) * scatter);
            int size = 1 + Math.round(seed * 3 * remaining);
            int color = alpha(seed > 0.7F ? ACCENT : BORDER, (0.3F + seed * 0.5F) * remaining);
            graphics.fill(Math.round(px) + dx, Math.round(py) + dy,
                    Math.round(px) + dx + size, Math.round(py) + dy + size, color);
        }
    }

    static int spaceColor(int y, int spaceY, int spaceHeight) {
        float position = Mth.clamp((y - spaceY) / (float) Math.max(1, spaceHeight - 1), 0, 1);
        float glow = Mth.sin(position * (float) Math.PI);
        return 0xFF000000 | (3 + Math.round(glow * 3)) << 16
                | (7 + Math.round(glow * 7)) << 8 | (19 + Math.round(glow * 13));
    }

    /** 沿水平段和节点平行斜段逐步绘制剧情连线。 */
    static void connection(GuiGraphics graphics, float x1, float y1, float x2, float y2,
            float nodeSlant, int nodeHeight, float progress, int color) {
        if (progress <= 0) {
            return;
        }
        float middle = (x1 + x2) / 2.0F;
        float diagonal = -(y2 - y1) * nodeSlant / (float) Math.max(1, nodeHeight);
        float[] xs = { x1, middle - diagonal / 2, middle + diagonal / 2, x2 };
        float[] ys = { y1, y1, y2, y2 };
        float[] lengths = new float[3];
        float total = 0;
        for (int i = 0; i < 3; i++) {
            lengths[i] = (float) Math.hypot(xs[i + 1] - xs[i], ys[i + 1] - ys[i]);
            total += lengths[i];
        }
        float remaining = total * Mth.clamp(progress, 0, 1);
        for (int i = 0; i < 3 && remaining > 0; i++) {
            if (lengths[i] == 0) {
                continue;
            }
            float fraction = Math.min(1, remaining / lengths[i]);
            line(graphics, xs[i], ys[i], xs[i] + (xs[i + 1] - xs[i]) * fraction,
                    ys[i] + (ys[i + 1] - ys[i]) * fraction, color);
            remaining -= lengths[i];
        }
    }

    static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color) {
        float length = (float) Math.hypot(x2 - x1, y2 - y1);
        if (length <= 0 || color >>> 24 == 0) {
            return;
        }
        float nx = -(y2 - y1) / length * 0.5F;
        float ny = (x2 - x1) / length * 0.5F;
        quad(graphics, x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, color, color);
    }

    static void outline(GuiGraphics graphics, float x, float y, int width, int height, float slant, int color) {
        if (color >>> 24 == 0) {
            return;
        }
        float slope = slant / (float) Math.max(1, height);
        quad(graphics, x + slant, y, x + slant - slope, y + 1, x + width - slope, y + 1, x + width, y, color, color);
        quad(graphics, x + slope, y + height - 1, x, y + height, x + width - slant, y + height,
                x + width - slant + slope, y + height - 1, color, color);
        quad(graphics, x + slant, y, x, y + height, x + 1, y + height, x + slant + 1, y, color, color);
        quad(graphics, x + width - 1, y, x + width - slant - 1, y + height,
                x + width - slant, y + height, x + width, y, color, color);
    }

    private static void quad(GuiGraphics graphics, float x1, float y1, float x2, float y2,
            float x3, float y3, float x4, float y4, int topColor, int bottomColor) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        vertex(vertices, matrix, x1, y1, topColor);
        vertex(vertices, matrix, x2, y2, bottomColor);
        vertex(vertices, matrix, x3, y3, bottomColor);
        vertex(vertices, matrix, x4, y4, topColor);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, int color) {
        vertices.vertex(matrix, x, y, 0).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).endVertex();
    }

    static boolean contains(double pointX, double pointY, float x, float y, int width, int height, float slant) {
        if (height <= 0 || pointY < y || pointY >= y + height) {
            return false;
        }
        double offset = slant * (1 - (pointY - y) / height);
        return pointX >= x + offset && pointX < x + offset + width - slant;
    }

    static int alpha(int color, float opacity) {
        return (Mth.clamp(Math.round(opacity * 255), 0, 255) << 24) | (color & 0xFFFFFF);
    }

    static float ease(float progress) {
        float value = Mth.clamp(progress, 0, 1);
        return 1 - (1 - value) * (1 - value) * (1 - value);
    }
}
