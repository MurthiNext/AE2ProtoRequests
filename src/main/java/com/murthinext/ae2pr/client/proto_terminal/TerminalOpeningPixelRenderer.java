package com.murthinext.ae2pr.client.proto_terminal;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * 从中心撕开的像素裂隙、辉光前沿与数据尘埃。
 */
final class TerminalOpeningPixelRenderer {

    private static final int MARGIN = 32;
    private static final int CELL = 3;
    private static final int SURFACE_STEP = 10;
    private static final float ROUGHNESS = 22;

    private final int width;
    private final int height;
    private final int originX;
    private final int originY;
    private final float centerX;
    private final float centerY;
    private final BitSet[] rows;
    private final float[][] noise;
    private final BitSet exposed = new BitSet();
    private final List<Band> bands = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Mote> motes = new ArrayList<>();
    private int firstRow;
    private int lastRow;
    private float halfSpan;
    private float halfHeight;
    private float energy;
    private float settle;

    record Band(int left, int top, int right, int bottom) {
    }

    private record Edge(float x1, float y1, float x2, float y2) {
    }

    private record Mote(float x, float y, float size, float phase, float brightness) {
    }

    TerminalOpeningPixelRenderer(int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        originX = x - MARGIN;
        originY = y - MARGIN;
        centerX = x + width / 2.0F;
        centerY = y + height / 2.0F;
        rows = new BitSet[height + MARGIN * 2];
        for (int row = 0; row < rows.length; row++) {
            rows[row] = new BitSet(width + MARGIN * 2);
        }
        firstRow = rows.length;
        lastRow = -1;
        noise = new float[(rows.length + CELL - 1) / CELL][(width + MARGIN * 2 + CELL - 1) / CELL];
        Random random = new Random();
        int seed = random.nextInt();
        for (int row = 0; row < noise.length; row++) {
            for (int column = 0; column < noise[row].length; column++) {
                noise[row][column] = sample(column / 4, row / 4, seed) * 0.65F
                        + sample(column / 2, row / 2, seed + 1) * 0.25F
                        + sample(column, row, seed + 2) * 0.1F;
            }
        }
        for (int i = 0; i < 280; i++) {
            motes.add(new Mote(x + random.nextFloat() * width, y + random.nextFloat() * height,
                    0.6F + random.nextFloat() * 1.5F, random.nextFloat() * Mth.TWO_PI,
                    0.25F + random.nextFloat() * 0.65F));
        }
    }

    void update(float progress, float seconds) {
        for (int row = firstRow; row <= lastRow; row++) {
            rows[row].clear();
        }
        bands.clear();
        edges.clear();
        firstRow = rows.length;
        lastRow = -1;
        if (progress <= 0 || progress >= 1) {
            return;
        }
        float openingWidth = width * (float) Math.pow(smooth(progress / 0.82F), 0.75);
        halfHeight = height * (float) Math.pow(smooth(progress / 0.94F), 1.3) / 2;
        halfSpan = (openingWidth - TerminalGraphicsRenderer.slant(halfHeight * 2)) / 2;
        if (halfSpan <= 0 || halfHeight < 0.5F) {
            return;
        }
        settle = smooth((progress - 0.74F) / 0.26F);
        energy = smooth(progress / 0.14F) * (1 - smooth((progress - 0.72F) / 0.28F));
        float roughness = Math.min(ROUGHNESS, Math.min(halfSpan, halfHeight) * 0.65F) * (1 - settle);
        float pixelate = smooth(Math.min(halfSpan, halfHeight) / CELL) * (1 - settle);
        float reach = roughness * 1.12F + CELL;
        firstRow = Math.max(0, Mth.floor(centerY - halfHeight - reach - originY));
        lastRow = Math.min(rows.length - 1, Mth.ceil(centerY + halfHeight + reach - originY));
        for (int row = firstRow; row <= lastRow; row++) {
            float py = originY + row + 0.5F + ((row / CELL * CELL + CELL / 2.0F) - row - 0.5F) * pixelate;
            float dy = Math.abs(py - centerY) - halfHeight;
            float middle = centerX - (py - centerY) * TerminalGraphicsRenderer.EDGE_SLOPE - originX;
            int left = Math.max(0, Mth.floor(middle - halfSpan - reach));
            int right = Math.min(width + MARGIN * 2, Mth.ceil(middle + halfSpan + reach));
            int coreLeft = Math.max(left, Mth.ceil(middle - halfSpan + reach));
            int coreRight = Math.min(right, Mth.floor(middle + halfSpan - reach));
            float ripple = Mth.sin(seconds * 7 + row / CELL * 0.31F) * 0.12F;
            for (int column = left; column < right; column++) {
                if (dy < -reach && column >= coreLeft && column < coreRight) {
                    rows[row].set(column, coreRight);
                    column = coreRight - 1;
                    continue;
                }
                float px = column + 0.5F + ((column / CELL * CELL + CELL / 2.0F) - column - 0.5F) * pixelate;
                float distance = Math.max(Math.abs(px - middle) - halfSpan, dy);
                float displacement = (noise[row / CELL][column / CELL] + ripple) * roughness;
                if (distance <= displacement) {
                    rows[row].set(column);
                }
            }
        }
        buildContours(reach);
    }

    private void buildContours(float reach) {
        for (int row = firstRow; row <= lastRow; row++) {
            BitSet occupied = rows[row];
            for (int left = occupied.nextSetBit(0); left >= 0;) {
                int right = occupied.nextClearBit(left);
                edges.add(new Edge(originX + left, originY + row, originX + left, originY + row + 1));
                edges.add(new Edge(originX + right, originY + row, originX + right, originY + row + 1));
                left = occupied.nextSetBit(right);
            }
            exposed.clear();
            exposed.or(occupied);
            if (row > 0) {
                exposed.andNot(rows[row - 1]);
            }
            addHorizontalEdges(row);
            exposed.clear();
            exposed.or(occupied);
            if (row + 1 < rows.length) {
                exposed.andNot(rows[row + 1]);
            }
            addHorizontalEdges(row + 1);
        }
        // 稳定内区合并成单个裁剪矩形。
        int coreTop = Math.max(firstRow, Mth.ceil(centerY - halfHeight + reach - originY));
        int coreBottom = Math.min(lastRow + 1, Mth.floor(centerY + halfHeight - reach - originY));
        int coreLeft = 0;
        int coreRight = 0;
        if (coreTop < coreBottom) {
            exposed.clear();
            exposed.or(rows[coreTop]);
            for (int row = coreTop + 1; row < coreBottom; row++) {
                exposed.and(rows[row]);
            }
            int middle = Mth.floor(centerX - originX);
            if (exposed.get(middle)) {
                coreLeft = exposed.previousClearBit(middle) + 1;
                coreRight = exposed.nextClearBit(middle);
                bands.add(new Band(originX + coreLeft, originY + coreTop,
                        originX + coreRight, originY + coreBottom));
            }
        }
        for (int top = firstRow; top <= lastRow;) {
            int bottom = top + 1;
            while (bottom <= lastRow && bottom != coreTop && bottom != coreBottom && rows[top].equals(rows[bottom])) {
                bottom++;
            }
            exposed.clear();
            exposed.or(rows[top]);
            if (top >= coreTop && top < coreBottom) {
                exposed.clear(coreLeft, coreRight);
            }
            for (int left = exposed.nextSetBit(0); left >= 0;) {
                int right = exposed.nextClearBit(left);
                bands.add(new Band(originX + left, originY + top, originX + right, originY + bottom));
                left = exposed.nextSetBit(right);
            }
            top = bottom;
        }
    }

    private void addHorizontalEdges(int row) {
        for (int left = exposed.nextSetBit(0); left >= 0;) {
            int right = exposed.nextClearBit(left);
            edges.add(new Edge(originX + left, originY + row, originX + right, originY + row));
            left = exposed.nextSetBit(right);
        }
    }

    List<Band> bands() {
        return bands;
    }

    boolean contains(float left, float top, float right, float bottom) {
        int x1 = Mth.floor(left - originX);
        int x2 = Mth.ceil(right - originX);
        int y1 = Mth.floor(top - originY);
        int y2 = Mth.ceil(bottom - originY);
        if (x1 < 0 || x2 > width + MARGIN * 2 || y1 < firstRow || y2 > lastRow + 1) {
            return false;
        }
        for (int row = y1; row < y2; row++) {
            if (rows[row].nextClearBit(x1) < x2) {
                return false;
            }
        }
        return y1 < y2 && x1 < x2;
    }

    void renderSurface(GuiGraphics graphics, TerminalStarfieldRenderer starfield, float seconds) {
        if (bands.isEmpty()) {
            return;
        }
        graphics.drawManaged(() -> {
            Matrix4f matrix = graphics.pose().last().pose();
            VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
            for (Band band : bands) {
                for (int top = band.top(); top < band.bottom(); top += SURFACE_STEP) {
                    int bottom = Math.min(top + SURFACE_STEP, band.bottom());
                    for (int left = band.left(); left < band.right(); left += SURFACE_STEP) {
                        int right = Math.min(left + SURFACE_STEP, band.right());
                        vertex(vertices, matrix, left, top, starfield.nebulaColor(left, top, seconds));
                        vertex(vertices, matrix, left, bottom, starfield.nebulaColor(left, bottom, seconds));
                        vertex(vertices, matrix, right, bottom, starfield.nebulaColor(right, bottom, seconds));
                        vertex(vertices, matrix, right, top, starfield.nebulaColor(right, top, seconds));
                    }
                }
            }
        });
    }

    void renderFrontier(GuiGraphics graphics, float progress, float seconds) {
        if (bands.isEmpty()) {
            return;
        }
        float outlineOpacity = 1 - smooth((progress - 0.9F) / 0.1F);
        graphics.drawManaged(() -> {
            Matrix4f matrix = graphics.pose().last().pose();
            VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
            for (Edge edge : edges) {
                float pulse = (float) Math.pow(Math.max(0,
                        Mth.sin((edge.x1() - centerX) * 0.035F + (edge.y1() - centerY) * 0.047F - seconds * 12)), 6);
                int color = TerminalGraphicsRenderer.mix(TerminalGraphicsRenderer.BORDER,
                        TerminalGraphicsRenderer.TEXT, energy * (0.28F + pulse * 0.55F));
                glow(vertices, matrix, edge, 3 + energy * 5, energy * (0.14F + pulse * 0.12F));
                glow(vertices, matrix, edge, 1.4F, energy * 0.22F);
                TerminalGraphicsRenderer.line(graphics, edge.x1(), edge.y1(), edge.x2(), edge.y2(),
                        TerminalGraphicsRenderer.alpha(color, outlineOpacity), 1);
            }
            renderMotes(graphics, seconds);
            float ignition = smooth(progress / 0.055F) * (1 - smooth((progress - 0.08F) / 0.24F));
            float radius = Math.min(halfSpan * 0.85F, 48);
            Edge seam = new Edge(centerX - radius, centerY, centerX + radius, centerY);
            glow(vertices, matrix, seam, Math.min(halfHeight, 6), ignition * 0.25F);
            TerminalGraphicsRenderer.line(graphics, seam.x1(), seam.y1(), seam.x2(), seam.y2(),
                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.TEXT, ignition * 0.8F), 0.8F);
            TerminalGraphicsRenderer.outline(graphics, originX + MARGIN, originY + MARGIN,
                    width, height, TerminalGraphicsRenderer.slant(height),
                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, 1 - outlineOpacity));
        });
    }

    private void renderMotes(GuiGraphics graphics, float seconds) {
        for (Mote mote : motes) {
            float dx = mote.x() - centerX + (mote.y() - centerY) * TerminalGraphicsRenderer.EDGE_SLOPE;
            float dy = mote.y() - centerY;
            float horizontal = Math.abs(dx) - halfSpan;
            float vertical = Math.abs(dy) - halfHeight;
            float distance = Math.max(horizontal, vertical);
            float wake = 1 - smooth(Math.abs(distance + 9) / Math.min(42, halfHeight + 1));
            float opacity = wake * energy * mote.brightness()
                    * (0.65F + 0.35F * Mth.sin(seconds * 13 + mote.phase()));
            if (opacity < 0.015F) {
                continue;
            }
            float drift = Mth.sin(seconds * 4 + mote.phase()) * (1 - settle) * 3;
            float x = mote.x() + (horizontal > vertical ? Math.signum(dx) * drift : 0);
            float y = mote.y() + (horizontal > vertical ? 0 : Math.signum(dy) * drift);
            int color = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.ACCENT, opacity);
            TerminalGraphicsRenderer.line(graphics, x, y, x + mote.size(), y, color, mote.size());
        }
    }

    private static void glow(VertexConsumer vertices, Matrix4f matrix, Edge edge, float radius, float opacity) {
        if (radius <= 0 || opacity < 1.0F / 255) {
            return;
        }
        float nx = edge.y1() == edge.y2() ? 0 : radius;
        float ny = edge.y1() == edge.y2() ? -radius : 0;
        int color = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, opacity);
        int transparent = color & 0xFFFFFF;
        vertex(vertices, matrix, edge.x1() - nx, edge.y1() - ny, transparent);
        vertex(vertices, matrix, edge.x2() - nx, edge.y2() - ny, transparent);
        vertex(vertices, matrix, edge.x2(), edge.y2(), color);
        vertex(vertices, matrix, edge.x1(), edge.y1(), color);
        vertex(vertices, matrix, edge.x1(), edge.y1(), color);
        vertex(vertices, matrix, edge.x2(), edge.y2(), color);
        vertex(vertices, matrix, edge.x2() + nx, edge.y2() + ny, transparent);
        vertex(vertices, matrix, edge.x1() + nx, edge.y1() + ny, transparent);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, int color) {
        vertices.vertex(matrix, x, y, 0).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).endVertex();
    }

    private static float smooth(float progress) {
        float value = Mth.clamp(progress, 0, 1);
        return value * value * (3 - 2 * value);
    }

    private static float sample(int x, int y, int seed) {
        int value = x * 0x1F1F1F1F ^ y * 0x6C8E9CF5 ^ seed;
        value = (value ^ value >>> 16) * 0x7FEB352D;
        value = (value ^ value >>> 15) * 0x846CA68B;
        return ((value ^ value >>> 16) >>> 8) / 8388608.0F - 1;
    }
}
