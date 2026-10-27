package com.murthinext.ae2pr.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

// 发光立方体
public final class EmissiveCubeRenderer {

    private static final float HALF = 0.5F;
    private static final float OUTSET = 0.002F;
    private static final float[] WHITE = { 1.0F, 1.0F, 1.0F };

    private EmissiveCubeRenderer() {
    }

    public static void render(PoseStack pose, VertexConsumer consumer, float alpha) {
        for (Direction direction : Direction.values()) {
            Vec3 normal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            // 面内基向量取 u x v = 法线，从外侧看四个角是逆时针
            Vec3 shell = Math.abs(normal.y) > 0.5 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
            Vec3 u = shell.cross(normal).normalize().scale(HALF);
            Vec3 v = normal.cross(u).normalize().scale(HALF);
            Vec3 center = normal.scale(HALF + OUTSET);
            vertex(consumer, pose.last(), center.subtract(u).subtract(v), 0, 1, alpha, normal);
            vertex(consumer, pose.last(), center.add(u).subtract(v), 1, 1, alpha, normal);
            vertex(consumer, pose.last(), center.add(u).add(v), 1, 0, alpha, normal);
            vertex(consumer, pose.last(), center.subtract(u).add(v), 0, 0, alpha, normal);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, float u, float v,
            float alpha, Vec3 normal) {
        consumer.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z)
                .color(WHITE[0], WHITE[1], WHITE[2], alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }
}
