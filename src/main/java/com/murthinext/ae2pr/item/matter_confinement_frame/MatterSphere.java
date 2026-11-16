package com.murthinext.ae2pr.item.matter_confinement_frame;

import appeng.api.util.AEColor;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

final class MatterSphere {

    private final int segments;
    private final int rings;
    private final double radius;
    private final int animationFps;
    private final long animationStart = Util.getMillis();
    private final MatterSpherePaintPattern pattern;
    private final SurfaceVertex[][] vertices;

    MatterSphere(JsonObject settings) {
        segments = Mth.clamp(GsonHelper.getAsInt(settings, "segments"), 16, 128);
        rings = Mth.clamp(GsonHelper.getAsInt(settings, "rings"), 8, 64);
        radius = Mth.clamp(GsonHelper.getAsDouble(settings, "radius"), 0.5, 4.0) / 16;
        animationFps = Mth.clamp(GsonHelper.getAsInt(settings, "animation_fps"), 1, 120);
        pattern = new MatterSpherePaintPattern(settings);
        var material = new MatterSphereMaterial(GsonHelper.getAsJsonObject(settings, "material"));
        vertices = new SurfaceVertex[rings + 1][segments + 1];
        for (int ring = 0; ring <= rings; ring++) {
            double latitude = Math.PI * ring / rings;
            double horizontal = ring == 0 || ring == rings ? 0 : Math.sin(latitude);
            double y = Math.cos(latitude);
            for (int segment = 0; segment <= segments; segment++) {
                double longitude = segment == segments ? 0 : 2 * Math.PI * segment / segments;
                double x = horizontal * Math.cos(longitude);
                double z = horizontal * Math.sin(longitude);
                vertices[ring][segment] = new SurfaceVertex(x, y, z, material.shade(x, y, z));
            }
        }
    }

    Surface createSurface(Map<AEColor, Integer> contents, TextureAtlasSprite sprite) {
        return new Surface(pattern.createPalette(contents), sprite);
    }

    private List<BakedQuad> createQuads(TextureAtlasSprite sprite) {
        var data = new int[rings + 1][segments + 1][];
        for (int ring = 0; ring <= rings; ring++) {
            for (int segment = 0; segment <= segments; segment++) {
                data[ring][segment] = vertices[ring][segment].bake(radius, sprite);
            }
        }
        var quads = new ArrayList<BakedQuad>(rings * segments);
        int stride = DefaultVertexFormat.BLOCK.getIntegerSize();
        for (int ring = 0; ring < rings; ring++) {
            for (int segment = 0; segment < segments; segment++) {
                var quad = new int[stride * 4];
                System.arraycopy(data[ring][segment], 0, quad, 0, stride);
                System.arraycopy(data[ring][segment + 1], 0, quad, stride, stride);
                System.arraycopy(data[ring + 1][segment + 1], 0, quad, stride * 2, stride);
                System.arraycopy(data[ring + 1][segment], 0, quad, stride * 3, stride);
                var normal = vertices[ring][segment];
                quads.add(new BakedQuad(
                        quad,
                        -1,
                        Direction.getNearest((float) normal.x(), (float) normal.y(), (float) normal.z()),
                        sprite,
                        false));
            }
        }
        return List.copyOf(quads);
    }

    final class Surface {

        private final MatterSpherePaintPattern.Palette palette;
        private final List<BakedQuad> quads;
        private final int[][] colors = new int[rings + 1][segments + 1];
        private final double[] sample = new double[3];
        private long lastFrame = -1;

        private Surface(MatterSpherePaintPattern.Palette palette, TextureAtlasSprite sprite) {
            this.palette = palette;
            quads = createQuads(sprite);
        }

        List<BakedQuad> quads() {
            return quads;
        }

        void update() {
            long frame = (Util.getMillis() - animationStart) * animationFps / 1000;
            if (frame == lastFrame) {
                return;
            }
            double seconds = (double) frame / animationFps;
            for (int ring = 0; ring <= rings; ring++) {
                for (int segment = 0; segment < segments; segment++) {
                    var vertex = vertices[ring][segment];
                    palette.sample(vertex.x(), vertex.y(), vertex.z(), seconds, sample);
                    colors[ring][segment] = vertex.lighting().color(sample);
                }
                colors[ring][segments] = colors[ring][0];
            }
            int stride = DefaultVertexFormat.BLOCK.getIntegerSize();
            // 只更新已缓存网格的顶点颜色，避免动画逐帧重新分配面片。
            for (int ring = 0; ring < rings; ring++) {
                for (int segment = 0; segment < segments; segment++) {
                    var data = quads.get(ring * segments + segment).getVertices();
                    data[3] = colors[ring][segment];
                    data[stride + 3] = colors[ring][segment + 1];
                    data[stride * 2 + 3] = colors[ring + 1][segment + 1];
                    data[stride * 3 + 3] = colors[ring + 1][segment];
                }
            }
            lastFrame = frame;
        }
    }

    private record SurfaceVertex(double x, double y, double z, MatterSphereMaterial.Lighting lighting) {

        int[] bake(double radius, TextureAtlasSprite sprite) {
            var data = new int[DefaultVertexFormat.BLOCK.getIntegerSize()];
            data[0] = Float.floatToRawIntBits((float) (0.5 + radius * x));
            data[1] = Float.floatToRawIntBits((float) (0.5 + radius * y));
            data[2] = Float.floatToRawIntBits((float) (0.5 + radius * z));
            data[3] = 0xFFFFFFFF;
            data[4] = Float.floatToRawIntBits(sprite.getU(8));
            data[5] = Float.floatToRawIntBits(sprite.getV(8));
            data[6] = LightTexture.FULL_BRIGHT;
            data[7] = (int) (Math.round(x * 127) & 255)
                    | (int) (Math.round(y * 127) & 255) << 8
                    | (int) (Math.round(z * 127) & 255) << 16;
            return data;
        }
    }
}
