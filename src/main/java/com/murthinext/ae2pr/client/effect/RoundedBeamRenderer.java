package com.murthinext.ae2pr.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

// 竖直光柱
public final class RoundedBeamRenderer {

    private static final int SEGMENTS = 16;
    private static final int ROWS = 12;
    private static final double TAU = Math.PI * 2.0;
    private static final double END_TAPER = 0.55;

    private RoundedBeamRenderer() {
    }

    public static void renderColumn(PoseStack pose, VertexConsumer consumer, Vec3 view, double bottom, double top,
            double time, float strength, float[] color) {
        double width = 0.55 + 0.45 * Math.min(1.0F, strength);
        renderLayer(pose, consumer, view, bottom, top, time, strength, color, 0.34 * width, 0.30, 0.0);
        renderLayer(pose, consumer, view, bottom, top, time, strength, color, 0.19 * width, 0.70, 0.5);
        renderLayer(pose, consumer, view, bottom, top, time, strength, color, 0.08 * width, 1.00, 0.25);
    }

    private static void renderLayer(PoseStack pose, VertexConsumer consumer, Vec3 view, double bottom, double top,
            double time, float strength, float[] color, double radius, double alphaScale, double phase) {
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
                        alphaScale, phase);
                vertex(consumer, last, view, radius, angle0, y1, u0, v1, bottom, top, time, strength, color,
                        alphaScale, phase);
                vertex(consumer, last, view, radius, angle1, y1, u1, v1, bottom, top, time, strength, color,
                        alphaScale, phase);
                vertex(consumer, last, view, radius, angle1, y0, u1, v0, bottom, top, time, strength, color,
                        alphaScale, phase);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 view, double radius, double angle,
            double y, float u, float v, double bottom, double top, double time, float strength, float[] color,
            double alphaScale, double phase) {
        double rest = Math.min(y - bottom, top - y);
        double taper = Math.max(0.0, Math.min(1.0, rest / END_TAPER));
        double reach = radius * Math.sqrt(1.0 - (1.0 - taper) * (1.0 - taper));
        double facing = Math.min(1.0, Math.cos(angle) * view.x + Math.sin(angle) * view.z);
        // 正对相机的母线最亮，越靠近轮廓越暗
        float rim = (float) Math.max(0.0, facing * facing * (3.0 - 2.0 * facing));
        double pulse = 0.85 + 0.15 * Math.sin(TAU * (y * 0.35 - time * 0.02 + phase));
        float alpha = (float) (strength * alphaScale * rim * taper * taper * pulse);
        consumer.vertex(pose.pose(), (float) (Math.cos(angle) * reach), (float) y, (float) (Math.sin(angle) * reach))
                .color(color[0], color[1], color[2], Math.min(1.0F, alpha))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), (float) Math.cos(angle), 0.0F, (float) Math.sin(angle))
                .endVertex();
    }
}
