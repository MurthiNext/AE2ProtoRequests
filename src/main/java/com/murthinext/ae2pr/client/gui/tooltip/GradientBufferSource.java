package com.murthinext.ae2pr.client.gui.tooltip;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

import org.joml.Matrix4f;

/**
 * 按字形顶点的行内横向位置重写颜色，得到子字符级平滑渐变。
 */
final class GradientBufferSource implements MultiBufferSource {

    private final int[] colors;
    private final VertexColorizer colorizer;

    private float originX;
    private float width;
    private float phase;

    GradientBufferSource(int[] colors) {
        this.colors = colors;
        this.colorizer = new VertexColorizer(this);
    }

    void configure(MultiBufferSource.BufferSource delegate, float originX, float width, float phase) {
        this.colorizer.delegate = delegate;
        this.originX = originX;
        this.width = width;
        this.phase = phase;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        this.colorizer.target = this.colorizer.delegate.getBuffer(type);
        return colorizer;
    }

    // 行内位置映射到循环渐变的插值颜色
    private int colorAt(float x) {
        float fraction = width <= 0.0F ? 0.0F : Mth.clamp((x - originX) / width, 0.0F, 1.0F);
        float scaled = ((fraction + phase) % 1.0F) * colors.length;
        int index = (int) scaled;
        float delta = scaled - index;
        int from = colors[index % colors.length];
        int to = colors[(index + 1) % colors.length];
        int red = mix(from >>> 16 & 0xFF, to >>> 16 & 0xFF, delta);
        int green = mix(from >>> 8 & 0xFF, to >>> 8 & 0xFF, delta);
        int blue = mix(from & 0xFF, to & 0xFF, delta);
        return red << 16 | green << 8 | blue;
    }

    private static int mix(int from, int to, float delta) {
        return Math.round(from + (to - from) * delta);
    }

    private static int scale(int channel, float brightness) {
        return Math.round(channel * brightness);
    }

    private final class VertexColorizer implements VertexConsumer {

        private final GradientBufferSource owner;
        private MultiBufferSource.BufferSource delegate;
        private VertexConsumer target;
        private float localX;

        private VertexColorizer(GradientBufferSource owner) {
            this.owner = owner;
        }

        @Override
        public VertexConsumer vertex(Matrix4f pose, float x, float y, float z) {
            this.localX = x;
            this.target.vertex(pose, x, y, z);
            return this;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.localX = (float) x;
            this.target.vertex(x, y, z);
            return this;
        }

        // 明暗强度沿用原版传入的亮度系数，阴影顶点自动压暗
        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            float brightness = red / 255.0F;
            int color = this.owner.colorAt(this.localX);
            this.target.color(scale(color >>> 16 & 0xFF, brightness), scale(color >>> 8 & 0xFF, brightness),
                    scale(color & 0xFF, brightness), alpha);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.target.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            this.target.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            this.target.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.target.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            this.target.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            this.target.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            this.target.unsetDefaultColor();
        }
    }
}
