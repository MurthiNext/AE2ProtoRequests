package com.murthinext.ae2pr.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

// 竖直光柱
public final class RoundedBeamRenderer {

    private static final int SEGMENTS = 32;
    private static final int ROWS = 16;
    private static final double TAU = Math.PI * 2.0;
    private static final double END_FADE = 0.2;
    private static final double CORE_WIDTH_SCALE = 0.8;

    private RoundedBeamRenderer() {
    }

    public static void renderColumn(PoseStack pose, VertexConsumer consumer, Vec3 view, double bottom, double top,
            boolean coreAtTop, double time, float strength, float[] color) {
        double width = 0.35 + 0.65 * Math.min(1.0F, strength);
        renderLayer(pose, consumer, view, bottom, top, coreAtTop, time, strength, color, 0.36 * width, 0.16);
        renderLayer(pose, consumer, view, bottom, top, coreAtTop, time, strength, color, 0.16 * width, 0.48);
        renderLayer(pose, consumer, view, bottom, top, coreAtTop, time, strength, color, 0.052 * width, 0.95);
    }

    private static void renderLayer(PoseStack pose, VertexConsumer consumer, Vec3 view, double bottom, double top,
            boolean coreAtTop, double time, float strength, float[] color, double radius, double alphaScale) {
        PoseStack.Pose last = pose.last();
        float height = (float) (top - bottom);
        for (int row = 0; row < ROWS; row++) {
            // 余弦分布
            float v0 = (float) ((1.0 - Math.cos(Math.PI * row / ROWS)) * 0.5);
            float v1 = (float) ((1.0 - Math.cos(Math.PI * (row + 1) / ROWS)) * 0.5);
            double y0 = bottom + v0 * height;
            double y1 = bottom + v1 * height;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double angle0 = TAU * segment / SEGMENTS;
                double angle1 = TAU * (segment + 1) / SEGMENTS;
                float u0 = (float) segment / SEGMENTS;
                float u1 = (float) (segment + 1) / SEGMENTS;
                // 绕序朝外，背面由渲染层剔除
                vertex(consumer, last, view, radius, angle0, y0, u0, v0, bottom, top, time, strength, color,
                        alphaScale, coreAtTop);
                vertex(consumer, last, view, radius, angle0, y1, u0, v1, bottom, top, time, strength, color,
                        alphaScale, coreAtTop);
                vertex(consumer, last, view, radius, angle1, y1, u1, v1, bottom, top, time, strength, color,
                        alphaScale, coreAtTop);
                vertex(consumer, last, view, radius, angle1, y0, u1, v0, bottom, top, time, strength, color,
                        alphaScale, coreAtTop);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 view, double radius, double angle,
            double y, float u, float v, double bottom, double top, double time, float strength, float[] color,
            double alphaScale, boolean coreAtTop) {
        double rest = Math.min(y - bottom, top - y);
        double taper = Math.max(0.0, Math.min(1.0, rest / END_FADE));
        taper = taper * taper * (3.0 - 2.0 * taper);
        double distance = coreAtTop ? 1.0 - v : v;
        double flare = distance * distance * (3.0 - 2.0 * distance);
        double reach = radius * (CORE_WIDTH_SCALE + (1.0 - CORE_WIDTH_SCALE) * flare);
        double facing = Math.max(0.0, Math.min(1.0, Math.cos(angle) * view.x + Math.sin(angle) * view.z));
        // 正对相机的母线最亮，越靠近轮廓越暗
        float rim = (float) (facing * facing * facing);
        double pulse = 0.97 + 0.03 * Math.sin(TAU * (y * 0.2 - time * 0.008));
        float alpha = (float) (strength * alphaScale * rim * taper * pulse);
        consumer.vertex(pose.pose(), (float) (Math.cos(angle) * reach), (float) y, (float) (Math.sin(angle) * reach))
                .color(color[0], color[1], color[2], Math.min(1.0F, alpha))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), (float) Math.cos(angle), 0.0F, (float) Math.sin(angle))
                .endVertex();
    }
}
