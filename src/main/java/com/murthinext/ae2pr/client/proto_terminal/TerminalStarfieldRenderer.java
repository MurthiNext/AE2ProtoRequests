package com.murthinext.ae2pr.client.proto_terminal;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

// 终端星空背景
final class TerminalStarfieldRenderer {

    private static final int SEED = 0xAE2C1257;
    private static final int FIELD_SIZE = 256;
    private static final int FIELD_MASK = FIELD_SIZE - 1;
    private static final int FIELD_PERIOD = 2048;
    private static final int MESH_STEP = 10;
    private static final int[] NEBULA = createNebula();
    private static final int[] STAR_COLORS = { 0xC8DBFF, 0xE1EEFF, 0xBCDDE8, 0xF8DEBC };

    private final float centerX;
    private final float centerY;
    private final float halfWidth;
    private final float halfHeight;
    private final Layer nebula = new Layer(0.32F, 0, 0, 0);
    private final Layer[] stars = {
            new Layer(0.22F, 24, 0.65F, 0.55F),
            new Layer(0.55F, 37, 0.58F, 0.8F),
            new Layer(1, 63, 0.62F, 1.1F)
    };

    TerminalStarfieldRenderer(int x, int y, int width, int height) {
        halfWidth = width / 2.0F;
        halfHeight = height / 2.0F;
        centerX = x + halfWidth;
        centerY = y + halfHeight;
    }

    // 星云与星层的鼠标锚点变换
    private static final class Layer {
        private final float depth;
        private final int spacing;
        private final float density;
        private final float size;
        private float scale = 1;
        private float offsetX;
        private float offsetY;

        private Layer(float depth, int spacing, float density, float size) {
            this.depth = depth;
            this.spacing = spacing;
            this.density = density;
            this.size = size;
        }

        private void zoom(float zoom, float anchorX, float anchorY) {
            float target = (float) Math.pow(zoom, depth);
            float ratio = target / scale;
            offsetX = anchorX - (anchorX - offsetX) * ratio;
            offsetY = anchorY - (anchorY - offsetY) * ratio;
            scale = target;
        }
    }

    void zoom(float zoom, float anchorX, float anchorY) {
        nebula.zoom(zoom, anchorX, anchorY);
        for (Layer layer : stars) {
            layer.zoom(zoom, anchorX, anchorY);
        }
    }

    void renderNebula(GuiGraphics graphics, int x, int y, int width, int height, float slant, float seconds) {
        if (width <= slant || height <= 0) {
            return;
        }
        graphics.drawManaged(() -> {
            Matrix4f matrix = graphics.pose().last().pose();
            VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
            float span = width - slant;
            for (int row = 0; row < height; row += MESH_STEP) {
                int bottom = Math.min(row + MESH_STEP, height);
                float leftTop = x + slant * (1 - row / (float) height);
                float leftBottom = x + slant * (1 - bottom / (float) height);
                for (int column = 0; column < span; column += MESH_STEP) {
                    float right = Math.min(column + MESH_STEP, span);
                    nebulaVertex(vertices, matrix, leftTop + column, y + row, seconds);
                    nebulaVertex(vertices, matrix, leftBottom + column, y + bottom, seconds);
                    nebulaVertex(vertices, matrix, leftBottom + right, y + bottom, seconds);
                    nebulaVertex(vertices, matrix, leftTop + right, y + row, seconds);
                }
            }
        });
    }

    private void nebulaVertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float seconds) {
        vertex(vertices, matrix, x, y, nebulaColor(x, y, seconds), 255);
    }

    // 采样缩放、漂移与暗角下的星云底色
    int nebulaColor(float x, float y, float seconds) {
        float worldX = (x - nebula.offsetX) / nebula.scale + seconds * 0.65F + 512;
        float worldY = (y - nebula.offsetY) / nebula.scale - seconds * 0.3F + 768;
        float sampleX = worldX * FIELD_SIZE / FIELD_PERIOD;
        float sampleY = worldY * FIELD_SIZE / FIELD_PERIOD;
        int ix = Mth.floor(sampleX);
        int iy = Mth.floor(sampleY);
        float fx = sampleX - ix;
        float fy = sampleY - iy;
        int topLeft = NEBULA[(iy & FIELD_MASK) * FIELD_SIZE + (ix & FIELD_MASK)];
        int topRight = NEBULA[(iy & FIELD_MASK) * FIELD_SIZE + ((ix + 1) & FIELD_MASK)];
        int bottomLeft = NEBULA[((iy + 1) & FIELD_MASK) * FIELD_SIZE + (ix & FIELD_MASK)];
        int bottomRight = NEBULA[((iy + 1) & FIELD_MASK) * FIELD_SIZE + ((ix + 1) & FIELD_MASK)];
        float edgeX = (x - centerX) / halfWidth;
        float edgeY = (y - centerY) / halfHeight;
        float vignette = 1 - 0.3F * Mth.clamp((edgeX * edgeX + edgeY * edgeY) * 0.5F, 0, 1);
        int red = channel(topLeft, topRight, bottomLeft, bottomRight, 16, fx, fy, vignette);
        int green = channel(topLeft, topRight, bottomLeft, bottomRight, 8, fx, fy, vignette);
        int blue = channel(topLeft, topRight, bottomLeft, bottomRight, 0, fx, fy, vignette);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int channel(int a, int b, int c, int d, int shift, float x, float y, float brightness) {
        float top = Mth.lerp(x, a >> shift & 255, b >> shift & 255);
        float bottom = Mth.lerp(x, c >> shift & 255, d >> shift & 255);
        return Math.round(Mth.lerp(y, top, bottom) * brightness);
    }

    void renderStars(GuiGraphics graphics, int x, int y, int width, int height, float slant,
            float seconds, float opacity, TerminalOpeningPixelRenderer aperture) {
        if (width <= slant || height <= 0 || opacity <= 0) {
            return;
        }
        graphics.drawManaged(() -> {
            Matrix4f matrix = graphics.pose().last().pose();
            VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
            for (int index = 0; index < stars.length; index++) {
                Layer layer = stars[index];
                float cellSize = layer.spacing * layer.scale;
                int left = Mth.floor((x - layer.offsetX - 8) / cellSize);
                int right = Mth.floor((x + width - layer.offsetX + 8) / cellSize);
                int top = Mth.floor((y - layer.offsetY - 8) / cellSize);
                int bottom = Mth.floor((y + height - layer.offsetY + 8) / cellSize);
                for (int row = top; row <= bottom; row++) {
                    for (int column = left; column <= right; column++) {
                        int seed = hash(column, row, SEED + index * 739);
                        if (random(seed, 1) > layer.density) {
                            continue;
                        }
                        float px = (column + random(seed, 2)) * cellSize + layer.offsetX;
                        float py = (row + random(seed, 3)) * cellSize + layer.offsetY;
                        float size = layer.size * (float) Math.sqrt(layer.scale);
                        float margin = size * 5;
                        boolean inside = aperture != null
                                ? aperture.contains(px - margin, py - margin, px + margin, py + margin)
                                : TerminalGraphicsRenderer.contains(px - margin, py - margin,
                                        x, y, width, height, slant)
                                        && TerminalGraphicsRenderer.contains(px + margin, py + margin,
                                                x, y, width, height, slant);
                        if (!inside) {
                            continue;
                        }
                        float light = starlight(seed, seconds);
                        float tint = random(seed, 4);
                        int color = STAR_COLORS[tint > 0.94F ? 3 : (int) (tint / 0.94F * 3)];
                        float brightness = (0.12F + layer.depth * 0.3F + light * (0.2F + layer.depth * 0.4F))
                                * opacity;
                        if (index > 0) {
                            glow(vertices, matrix, px, py, size * (2 + light), color, brightness * 0.07F);
                        }
                        if (index == 2 && light > 0.55F && random(seed, 5) > 0.55F) {
                            float reach = size * (2 + light * 2);
                            point(vertices, matrix, px - reach, py - size * 0.22F,
                                    px + reach, py + size * 0.22F, color, brightness * light * 0.24F);
                            point(vertices, matrix, px - size * 0.22F, py - reach,
                                    px + size * 0.22F, py + reach, color, brightness * light * 0.24F);
                        }
                        point(vertices, matrix, px - size / 2, py - size / 2,
                                px + size / 2, py + size / 2, color, brightness);
                    }
                }
            }
        });
    }

    // 每颗星的起伏与周期性闪光
    private static float starlight(int seed, float seconds) {
        float time = seconds / (3 + random(seed, 6) * 7) + random(seed, 7) * 80;
        int cycle = Mth.floor(time);
        float phase = time - cycle;
        int event = hash(seed, cycle, SEED);
        float center = 0.3F + random(event, 1) * 0.4F;
        float duration = 0.12F + random(event, 2) * 0.18F;
        float pulse = Math.max(0, 1 - Math.abs(phase - center) / duration);
        pulse = pulse * pulse * (3 - 2 * pulse);
        float shimmer = 0.5F + 0.5F * Mth.sin(seconds * (0.4F + random(seed, 8))
                + random(seed, 9) * Mth.TWO_PI);
        return shimmer * 0.2F + pulse * (0.35F + random(event, 3) * 0.45F);
    }

    private static void point(VertexConsumer vertices, Matrix4f matrix, float left, float top,
            float right, float bottom, int color, float opacity) {
        int alpha = Math.round(Mth.clamp(opacity, 0, 1) * 255);
        vertex(vertices, matrix, left, top, color, alpha);
        vertex(vertices, matrix, left, bottom, color, alpha);
        vertex(vertices, matrix, right, bottom, color, alpha);
        vertex(vertices, matrix, right, top, color, alpha);
    }

    private static void glow(VertexConsumer vertices, Matrix4f matrix, float x, float y,
            float radius, int color, float opacity) {
        int alpha = Math.round(opacity * 255);
        for (int sector = 0; sector < 8; sector++) {
            float angle = sector * Mth.TWO_PI / 8;
            float next = (sector + 1) * Mth.TWO_PI / 8;
            vertex(vertices, matrix, x, y, color, alpha);
            vertex(vertices, matrix, x + Mth.cos(next) * radius, y + Mth.sin(next) * radius, color, 0);
            vertex(vertices, matrix, x + Mth.cos(angle) * radius, y + Mth.sin(angle) * radius, color, 0);
            vertex(vertices, matrix, x, y, color, alpha);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, int color, int alpha) {
        vertices.vertex(matrix, x, y, 0).color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).endVertex();
    }

    private static int[] createNebula() {
        int[] colors = new int[FIELD_SIZE * FIELD_SIZE];
        for (int y = 0; y < FIELD_SIZE; y++) {
            for (int x = 0; x < FIELD_SIZE; x++) {
                float u = x / (float) FIELD_SIZE;
                float v = y / (float) FIELD_SIZE;
                float warpX = noise(u, v, 4, 17) * 0.14F;
                float warpY = noise(u, v, 4, 29) * 0.14F;
                float broad = noise(u + warpX, v + warpY, 8, 43);
                float fine = noise(u + warpX, v + warpY, 32, 71);
                float cloud = Math.max(0, 1 - Math.abs(broad * 0.75F + fine * 0.25F - 0.5F) * 5);
                cloud = cloud * cloud * cloud;
                float veil = noise(u, v, 4, 101);
                cloud *= 0.25F + veil * veil * 0.75F;
                float violet = noise(u, v, 8, 137);
                float filament = cloud * noise(u + warpX, v + warpY, 64, 163);
                int red = Math.round(2 + cloud * (8 + violet * 13) + filament * 2);
                int green = Math.round(4 + cloud * (24 - violet * 12) + filament * 3);
                int blue = Math.round(11 + cloud * (44 + violet * 9) + filament * 5);
                colors[y * FIELD_SIZE + x] = red << 16 | green << 8 | blue;
            }
        }
        return colors;
    }

    private static float noise(float x, float y, int cells, int salt) {
        float px = x * cells;
        float py = y * cells;
        int ix = Mth.floor(px);
        int iy = Mth.floor(py);
        float fx = px - ix;
        float fy = py - iy;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float top = Mth.lerp(fx, random(hash(ix & (cells - 1), iy & (cells - 1), salt), 0),
                random(hash((ix + 1) & (cells - 1), iy & (cells - 1), salt), 0));
        float bottom = Mth.lerp(fx, random(hash(ix & (cells - 1), (iy + 1) & (cells - 1), salt), 0),
                random(hash((ix + 1) & (cells - 1), (iy + 1) & (cells - 1), salt), 0));
        return Mth.lerp(fy, top, bottom);
    }

    private static int hash(int x, int y, int salt) {
        int value = x * 0x1F1F1F1F ^ y * 0x6C8E9CF5 ^ salt;
        value = (value ^ value >>> 16) * 0x7FEB352D;
        value = (value ^ value >>> 15) * 0x846CA68B;
        return value ^ value >>> 16;
    }

    private static float random(int seed, int salt) {
        return (hash(seed, salt, SEED) >>> 8) / 16777216.0F;
    }
}
