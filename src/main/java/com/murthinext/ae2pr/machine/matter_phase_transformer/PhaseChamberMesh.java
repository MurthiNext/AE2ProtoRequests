package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record PhaseChamberMesh(PoseStack.Pose pose, VertexConsumer vertices) {

    record Rim(float y, float radius, float v, int color) {
        Rim(float y, float radius, int color) {
            this(y, radius, 0, color);
        }
    }

    record Edge(Vec3 left, Vec3 right, float v, int color) {}

    static final int CRYSTAL_FACES = 6;
    private static final int SIDES = 2 * CRYSTAL_FACES;
    private static final float[] X = new float[SIDES + 1];
    private static final float[] Z = new float[SIDES + 1];

    static {
        for (int i = 0; i <= SIDES; i++) {
            double angle = (i % SIDES + 0.5) * (2 * Math.PI / SIDES);
            X[i] = (float) Math.cos(angle);
            Z[i] = (float) Math.sin(angle);
        }
    }

    void body(Rim... profile) {
        bands(1, 0, profile);
    }

    void crystal(Rim... profile) {
        bands(2, 0, profile);
    }

    static Vec3 onCrystalFace(int face, float y, float radius, double across) {
        int i = face * 2;
        return new Vec3(radius * Mth.lerp(across, X[i], X[i + 2]), y, radius * Mth.lerp(across, Z[i], Z[i + 2]));
    }

    void pool(float radius, Rim... profile) {
        bands(1, 0.49F / radius, profile);
    }

    private void bands(int stride, float planarScale, Rim[] profile) {
        for (int row = 1; row < profile.length; row++) {
            Rim a = profile[row - 1];
            Rim b = profile[row];
            float dy = b.y - a.y;
            float dr = a.radius - b.radius;
            float length = (float) Math.hypot(dy, dr);
            if (length < 1.0E-6F) {
                continue;
            }
            for (int i = 0; i < SIDES; i += stride) {
                int j = i + stride;
                float nx = (X[i] + X[j]) * 0.5F * dy / length;
                float nz = (Z[i] + Z[j]) * 0.5F * dy / length;
                float ny = dr / length;
                rimVertex(a, i, nx, ny, nz, planarScale);
                rimVertex(b, i, nx, ny, nz, planarScale);
                rimVertex(b, j, nx, ny, nz, planarScale);
                rimVertex(a, j, nx, ny, nz, planarScale);
            }
        }
    }

    private void rimVertex(Rim rim, int i, float nx, float ny, float nz, float planarScale) {
        float x = X[i] * rim.radius;
        float z = Z[i] * rim.radius;
        float u = planarScale > 0 ? 0.5F + x * planarScale : (float) i / SIDES;
        float v = planarScale > 0 ? 0.5F + z * planarScale : rim.v;
        vertex(x, rim.y, z, u, v, rim.color, nx, ny, nz);
    }

    void film(Edge top, Edge bottom) {
        Vec3 normal = top.right
                .subtract(bottom.left)
                .cross(bottom.right.subtract(top.left))
                .normalize();
        filmVertex(bottom.left, 0.08F, bottom, normal);
        filmVertex(top.left, 0.08F, top, normal);
        filmVertex(top.right, 0.92F, top, normal);
        filmVertex(bottom.right, 0.92F, bottom, normal);
    }

    private void filmVertex(Vec3 point, float u, Edge edge, Vec3 normal) {
        vertex(
                (float) point.x,
                (float) point.y,
                (float) point.z,
                u,
                edge.v,
                edge.color,
                (float) normal.x,
                (float) normal.y,
                (float) normal.z);
    }

    void ribbon(Vec3 eye, Vec3 start, Vec3 end, double startWidth, double endWidth, float v0, float v1, int color) {
        Vec3 axis = end.subtract(start);
        Vec3 side = eye.subtract(start).cross(axis);
        if (side.lengthSqr() < 1.0E-10) {
            side = axis.cross(Math.abs(axis.y) > Math.abs(axis.x) ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0));
        }
        side = side.normalize();
        Vec3 a = side.scale(startWidth);
        Vec3 b = side.scale(endWidth);
        vertex(start.subtract(a), 0, v0, color);
        vertex(end.subtract(b), 0, v1, color);
        vertex(end.add(b), 1, v1, color);
        vertex(start.add(a), 1, v0, color);
    }

    void sprite(Vec3 center, Vec3 right, Vec3 up, int color) {
        Vec3 left = center.subtract(right);
        Vec3 edge = center.add(right);
        vertex(left.subtract(up), 0, 1, color);
        vertex(edge.subtract(up), 1, 1, color);
        vertex(edge.add(up), 1, 0, color);
        vertex(left.add(up), 0, 0, color);
    }

    private void vertex(Vec3 point, float u, float v, int color) {
        vertex((float) point.x, (float) point.y, (float) point.z, u, v, color, 0, 0, 1);
    }

    private void vertex(float x, float y, float z, float u, float v, int color, float nx, float ny, float nz) {
        vertices.vertex(pose.pose(), x, y, z)
                .color(color)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
