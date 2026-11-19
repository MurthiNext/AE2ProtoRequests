package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record PhaseChamberMesh(PoseStack.Pose pose, VertexConsumer vertices, float surfaceOffset) {

    PhaseChamberMesh(PoseStack.Pose pose, VertexConsumer vertices) {
        this(pose, vertices, 0);
    }

    record Rim(float y, float radius, int color) {}

    record Edge(Vec3 left, Vec3 right, int color) {
        Edge lerp(Edge other, float t) {
            return new Edge(left.lerp(other.left, t), right.lerp(other.right, t), ARGB32.lerp(t, color, other.color));
        }
    }

    private enum Mapping {
        CRYSTAL(0.25F), // 64 像素的晶面贴图覆盖 4 格
        LIQUID(1),
        POOL(1);

        final float scale;

        Mapping(float scale) {
            this.scale = scale;
        }
    }

    private record Face(Vec3 left, Vec3 right, Vec3 tangent, float centerU) {
        Vec3 point(float y, float radius, double across) {
            return new Vec3(radius * Mth.lerp(across, left.x, right.x), y, radius * Mth.lerp(across, left.z, right.z));
        }

        Edge edge(Rim rim) {
            return new Edge(point(rim.y, rim.radius, 0), point(rim.y, rim.radius, 1), rim.color);
        }
    }

    static final int CRYSTAL_FACES = 6;
    private static final Face[] HEXAGON = faces(CRYSTAL_FACES);
    private static final Face[] ROUND = faces(12);

    private static Face[] faces(int count) {
        Vec3[] corners = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double angle = Math.PI / 12 + i * (2 * Math.PI / count);
            corners[i] = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }
        Face[] faces = new Face[count];
        for (int i = 0; i < count; i++) {
            Vec3 left = corners[i];
            Vec3 right = corners[(i + 1) % count];
            faces[i] = new Face(left, right, right.subtract(left).normalize(), 0.2F + 0.6F * (i + 0.5F) / count);
        }
        return faces;
    }

    static Vec3 onCrystalFace(int face, float y, float radius, double across) {
        return HEXAGON[face].point(y, radius, across);
    }

    void crystal(Rim[] profile) {
        bands(HEXAGON, Mapping.CRYSTAL, profile);
    }

    void body(Rim... profile) {
        bands(ROUND, Mapping.LIQUID, profile);
    }

    void pool(Rim... profile) {
        bands(ROUND, Mapping.POOL, profile);
    }

    float film(int face, Edge top, Edge bottom, float v) {
        return strip(HEXAGON[face], Mapping.LIQUID, top, bottom, v);
    }

    private void bands(Face[] faces, Mapping mapping, Rim[] profile) {
        for (Face face : faces) {
            Edge top = face.edge(profile[profile.length - 1]);
            float v = 0;
            for (int row = profile.length - 2; row >= 0; row--) {
                Edge bottom = face.edge(profile[row]);
                v = strip(face, mapping, top, bottom, v);
                top = bottom;
            }
        }
    }

    private float strip(Face face, Mapping mapping, Edge top, Edge bottom, float v0) {
        Vec3 tangent = face.tangent;
        double dy = top.left.y - bottom.left.y;
        double dr = (bottom.left.x - top.left.x) * tangent.z - (bottom.left.z - top.left.z) * tangent.x;
        double slant = Math.hypot(dy, dr);
        if (slant < 1.0E-6) {
            return v0;
        }
        Vec3 normal = new Vec3(tangent.z * dy / slant, dr / slant, -tangent.x * dy / slant);
        float v1 = v0 + (float) slant * mapping.scale;
        if (mapping == Mapping.LIQUID) {
            // 图集不能直接平铺：在整格 UV 处切开，避免插值跨到相邻流体贴图。
            int tile = Mth.floor(v0);
            Edge start = top;
            float startV = v0 - tile;
            while (tile + 1 < v1) {
                Edge cut = top.lerp(bottom, (++tile - v0) / (v1 - v0));
                quad(face, mapping, start, cut, startV, 1, normal);
                start = cut;
                startV = 0;
            }
            quad(face, mapping, start, bottom, startV, v1 - tile, normal);
        } else {
            quad(face, mapping, top, bottom, v0, v1, normal);
        }
        return v1;
    }

    private void quad(Face face, Mapping mapping, Edge top, Edge bottom, float v0, float v1, Vec3 normal) {
        materialVertex(face, mapping, bottom.left, v1, bottom.color, normal);
        materialVertex(face, mapping, top.left, v0, top.color, normal);
        materialVertex(face, mapping, top.right, v0, top.color, normal);
        materialVertex(face, mapping, bottom.right, v1, bottom.color, normal);
    }

    private void materialVertex(Face face, Mapping mapping, Vec3 p, float v, int color, Vec3 normal) {
        float u = face.centerU + (float) face.tangent.dot(p) * mapping.scale;
        if (mapping == Mapping.POOL) {
            u = 0.5F + (float) p.x;
            v = 0.5F + (float) p.z;
        } else if (mapping == Mapping.LIQUID) {
            v = Mth.clamp(v, 0.001F, 0.999F);
        }
        float x = (float) (p.x + normal.x * surfaceOffset);
        float y = (float) (p.y + normal.y * surfaceOffset);
        float z = (float) (p.z + normal.z * surfaceOffset);
        vertex(x, y, z, u, v, color, (float) normal.x, (float) normal.y, (float) normal.z);
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
