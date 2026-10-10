package com.murthinext.ae2pr.client.proto_terminal;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** 入场数据块的流动轨迹与合并外轮廓。 */
final class OpeningPixelStream {

    private static final int MARGIN = 80;

    private final BitSet[] rows;
    private final BitSet[] panelRows;
    private final BitSet[] outlineRows;
    private final byte[][] strengths;
    private final BitSet border = new BitSet();
    private final BitSet exposed = new BitSet();
    private final BitSet occupied = new BitSet();
    private final Random random = new Random();
    private final int maskWidth;
    private int firstRow;
    private int lastRow;
    private int drawX;
    private int drawY;
    private float fillOpacity;
    private float outlineOpacity;

    OpeningPixelStream(int width, int height) {
        maskWidth = width + MARGIN * 2 + 1;
        rows = new BitSet[height + MARGIN * 2 + 1];
        panelRows = new BitSet[rows.length];
        outlineRows = new BitSet[rows.length];
        strengths = new byte[rows.length][maskWidth];
        for (int row = 0; row < rows.length; row++) {
            rows[row] = new BitSet(maskWidth);
            panelRows[row] = new BitSet(maskWidth);
            outlineRows[row] = new BitSet(maskWidth);
        }
        firstRow = rows.length;
        lastRow = -1;
    }

    void render(GuiGraphics graphics, int x, int y, int width, int height, float slant,
            float progress, float seconds, int spaceY, int spaceHeight) {
        if (progress <= 0 || progress >= 1 || width <= slant || height <= 0) {
            return;
        }
        drawX = x - MARGIN;
        drawY = y - MARGIN;
        updateShape(width, height, slant, progress, seconds);
        fillOpacity = CrystalNarrativeGraphics.ease(progress / 0.1F);
        outlineOpacity = CrystalNarrativeGraphics.ease(progress);
        graphics.drawManaged(() -> {
            for (int row = firstRow; row <= lastRow; row++) {
                drawRow(graphics, rows[row], row,
                        CrystalNarrativeGraphics.spaceColor(drawY + row, spaceY, spaceHeight), fillOpacity, false);
            }
        });
        renderOutline(graphics);
    }

    /** 绘制入场像素流与主面板共用的外轮廓。 */
    private void renderOutline(GuiGraphics graphics) {
        graphics.drawManaged(() -> {
            for (int row = firstRow; row <= lastRow; row++) {
                drawRow(graphics, outlineRows[row], row, CrystalNarrativeGraphics.BORDER, outlineOpacity, true);
            }
        });
    }

    private void updateShape(int width, int height, float slant, float progress, float seconds) {
        for (int row = firstRow; row <= lastRow; row++) {
            rows[row].clear();
            outlineRows[row].clear();
            Arrays.fill(strengths[row], (byte) 0);
        }
        firstRow = rows.length;
        lastRow = -1;
        for (int row = 0; row < panelRows.length; row++) {
            panelRows[row].clear();
            int localY = row - MARGIN;
            if (localY >= 0 && localY < height) {
                float left = slant * (1 - (localY + 0.5F) / height);
                panelRows[row].set(MARGIN + (int) Math.ceil(left - 0.5F),
                        MARGIN + (int) Math.ceil(left + width - slant - 0.5F));
            }
        }
        for (int edge = 0; edge < 4; edge++) {
            addEdge(width, height, slant, edge, progress, seconds);
        }
        firstRow = Math.min(firstRow, MARGIN);
        lastRow = Math.max(lastRow, MARGIN + height - 1);
        for (int row = firstRow; row <= lastRow; row++) {
            occupied.clear();
            occupied.or(rows[row]);
            occupied.or(panelRows[row]);
            border.clear();
            for (int left = occupied.nextSetBit(0); left >= 0;) {
                int right = occupied.nextClearBit(left);
                border.set(left);
                border.set(right - 1);
                left = occupied.nextSetBit(right);
            }
            exposed.clear();
            exposed.or(occupied);
            if (row > 0) {
                exposed.andNot(rows[row - 1]);
                exposed.andNot(panelRows[row - 1]);
            }
            border.or(exposed);
            exposed.clear();
            exposed.or(occupied);
            if (row + 1 < rows.length) {
                exposed.andNot(rows[row + 1]);
                exposed.andNot(panelRows[row + 1]);
            }
            border.or(exposed);
            outlineRows[row].or(border);
        }
    }

    /** 不规则贴边碎块逐渐收拢，少量脱离方块在近处淡出。 */
    private void addEdge(int width, int height, float slant, int edge, float progress, float seconds) {
        boolean horizontal = edge < 2;
        float length = horizontal ? width - slant : height;
        float remaining = 1 - progress;
        float settle = CrystalNarrativeGraphics.ease((progress - 0.74F) / 0.2F);
        float direction = edge == 0 || edge == 3 ? 1 : -1;
        float tangentX = horizontal ? direction : -slant / height * direction;
        float tangentY = horizontal ? 0 : direction;
        float normalX = horizontal ? 0 : (edge == 2 ? -1 : 1);
        float normalY = horizontal ? (edge == 0 ? -1 : 1) : 0;
        random.setSeed(0xAE2C1257L + edge * 739L);
        for (int step = 0; step < length; step += 4 + random.nextInt(6)) {
            float seed = random.nextFloat();
            float phase = seconds * (8 + seed * 6) + seed * Mth.TWO_PI;
            float flow = step + direction * seconds * (28 + edge * 5) * remaining
                    + Mth.sin(phase) * (2 + seed * 3) * remaining;
            float position = flow - (float) Math.floor(flow / length) * length;
            float inset = Math.min(6, length / 2);
            position = Mth.clamp(position, inset, length - inset);
            float px = horizontal ? position + (edge == 0 ? slant : 0)
                    : slant * (1 - position / height) + (edge == 3 ? width - slant : 0);
            float py = horizontal ? (edge == 1 ? height : 0) : position;
            int baseSize = 5 + random.nextInt(6);
            float anchor = baseSize * (0.15F + Mth.sin(phase * 0.7F) * 0.08F) * remaining
                    - (baseSize * 0.7F + 3) * settle;
            addBlock(Math.round(px + normalX * anchor) + MARGIN - baseSize / 2,
                    Math.round(py + normalY * anchor) + MARGIN - baseSize / 2, baseSize, 1);
            int branches = 1 + random.nextInt(3);
            for (int branch = 0; branch < branches; branch++) {
                int size = 5 + random.nextInt(6);
                float reach = Math.min(9 + random.nextFloat() * 4,
                        (baseSize + size) * (0.18F + random.nextFloat() * 0.08F) * (branch + 1));
                float distance = anchor + reach * (1 - settle);
                float bend = (random.nextFloat() - 0.5F) * (baseSize + size) * 0.65F * (1 - settle);
                addBlock(Math.round(px + normalX * distance + tangentX * bend) + MARGIN - size / 2,
                        Math.round(py + normalY * distance + tangentY * bend) + MARGIN - size / 2, size, 1);
            }
            int fragments = random.nextFloat() < 0.1F ? 1 : 0;
            for (int fragment = 0; fragment < fragments; fragment++) {
                int size = 5 + random.nextInt(6);
                float birth = 0.06F + random.nextFloat() * 0.28F;
                float lifetime = 0.3F + random.nextFloat() * 0.3F;
                float age = (progress - birth) / lifetime;
                float distance = 8 + random.nextFloat() * 3 + age * (3 + random.nextFloat() * 4);
                float drift = (random.nextFloat() - 0.5F) * 12;
                float bend = drift * age + Mth.sin(phase * (0.8F + seed) + fragment * 2.7F) * 2.5F;
                float brightness = 0.55F + random.nextFloat() * 0.45F;
                if (age <= 0 || age >= 1) {
                    continue;
                }
                float opacity = CrystalNarrativeGraphics.ease(age / 0.15F)
                        * (1 - CrystalNarrativeGraphics.ease((age - 0.25F) / 0.75F)) * brightness;
                addBlock(Math.round(px + normalX * distance + tangentX * bend) + MARGIN - size / 2,
                        Math.round(py + normalY * distance + tangentY * bend) + MARGIN - size / 2, size, opacity);
            }
        }
    }

    private void addBlock(int x, int y, int size, float opacity) {
        int strength = Math.round(opacity * 255);
        int left = Math.max(0, x);
        int right = Math.min(maskWidth, x + size);
        int top = Math.max(0, y);
        int bottom = Math.min(rows.length, y + size);
        if (strength <= 0 || left >= right || top >= bottom) {
            return;
        }
        for (int row = top; row < bottom; row++) {
            rows[row].set(left, right);
            for (int column = left; column < right; column++) {
                strengths[row][column] = (byte) Math.max(strengths[row][column] & 255, strength);
            }
        }
        firstRow = Math.min(firstRow, top);
        lastRow = Math.max(lastRow, bottom - 1);
    }

    private void drawRow(GuiGraphics graphics, BitSet pixels, int row, int color, float opacity, boolean outline) {
        for (int left = pixels.nextSetBit(0); left >= 0;) {
            int end = pixels.nextClearBit(left);
            while (left < end) {
                int strength = outline && panelRows[row].get(left) ? 255 : strengths[row][left] & 255;
                int right = left + 1;
                while (right < end
                        && (outline && panelRows[row].get(right) ? 255 : strengths[row][right] & 255) == strength) {
                    right++;
                }
                graphics.fill(drawX + left, drawY + row, drawX + right, drawY + row + 1,
                        CrystalNarrativeGraphics.alpha(color, opacity * strength / 255));
                left = right;
            }
            left = pixels.nextSetBit(end);
        }
    }
}
