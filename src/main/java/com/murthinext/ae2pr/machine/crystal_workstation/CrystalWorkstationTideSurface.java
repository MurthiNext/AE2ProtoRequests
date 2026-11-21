package com.murthinext.ae2pr.machine.crystal_workstation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murthinext.ae2pr.api.Ids;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class CrystalWorkstationTideSurface {

    static final RenderType MATERIAL =
            RenderType.entityTranslucentEmissive(Ids.of("textures/block/crystal_workstation/portal_liquid.png"), false);
    private static final int GRID = 20;
    private static final float HALF_SPAN = CrystalWorkstationStructure.PANEL_SIZE * 0.5F - 0.125F;
    private static final float PANEL_FACE = 3.48F;
    private static final double TIDE_TICKS = 38;
    static final double SIDE_OFFSET_TICKS = TIDE_TICKS * 0.43;

    private final Sample[][] mesh = new Sample[GRID + 1][GRID + 1];
    private final Vector3f tangent = new Vector3f();

    CrystalWorkstationTideSurface() {
        for (int y = 0; y <= GRID; y++) {
            for (int x = 0; x <= GRID; x++) {
                mesh[y][x] = new Sample((2 * x - GRID) * HALF_SPAN / GRID, (2 * y - GRID) * HALF_SPAN / GRID);
            }
        }
    }

    CrystalWorkstationInfusion render(
            PoseStack.Pose pose,
            VertexConsumer water,
            Direction outward,
            Direction depth,
            double time,
            float activity) {
        float beat = (float) Mth.frac(time / TIDE_TICKS);
        float pull = activity * (0.16F + 0.025F * Mth.cos(Mth.TWO_PI * beat));
        float swell = Mth.TWO_PI * (float) Mth.frac(time / 71.0);
        float drift = (float) Mth.frac(time / 93.0);
        for (Sample[] row : mesh) {
            for (Sample point : row) {
                point.move(swell, drift, pull, activity);
            }
        }
        for (int y = 0; y <= GRID; y++) {
            var below = mesh[Math.max(y - 1, 0)];
            var above = mesh[Math.min(y + 1, GRID)];
            var row = mesh[y];
            for (int x = 0; x <= GRID; x++) {
                tangent.set(row[Math.min(x + 1, GRID)].position).sub(row[Math.max(x - 1, 0)].position);
                Sample point = row[x];
                point.normal
                        .set(above[x].position)
                        .sub(below[x].position)
                        .cross(tangent)
                        .normalize();
                point.shade = Mth.clamp(point.shade + 0.24F * point.normal.y - 0.12F * point.normal.x, 0.01F, 0.99F);
            }
        }

        var basis =
                new Matrix3f(depth.getStepX(), 0, depth.getStepZ(), 0, 1, 0, outward.getStepX(), 0, outward.getStepZ());
        var positions = new Matrix4f(pose.pose()).mul(new Matrix4f(basis));
        var normals = new Matrix3f(pose.normal()).mul(basis);
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                mesh[y][x].emit(water, positions, normals);
                mesh[y + 1][x].emit(water, positions, normals);
                mesh[y + 1][x + 1].emit(water, positions, normals);
                mesh[y][x + 1].emit(water, positions, normals);
            }
        }
        return new CrystalWorkstationInfusion(outward, mesh[GRID / 2][GRID / 2].position.z, beat);
    }

    private static final class Sample {
        final float x, y, pin, pullWeight;
        final Vector3f position = new Vector3f();
        final Vector3f normal = new Vector3f();
        float shade, grain;
        int alpha;

        Sample(float x, float y) {
            this.x = x;
            this.y = y;
            float across = x + 0.22F * y;
            pullWeight = (float) Math.exp(-5.8F * across * across - 4.2F * y * y);
            float rim = Mth.clamp((HALF_SPAN - Math.max(Math.abs(x), Math.abs(y))) / 0.36F, 0, 1);
            pin = rim * rim * (3 - 2 * rim);
        }

        void move(float swell, float drift, float pull, float activity) {
            float cross = Mth.sin(1.25F * x - 2.6F * y + Mth.TWO_PI * drift);
            float wave = Mth.sin(2.45F * x + 1.35F * y - swell + 0.22F * cross);
            float lift = 0.055F + activity * (0.032F * wave + 0.016F * cross) + pullWeight * pull;
            position.set(x, y, PANEL_FACE - pin * lift);
            shade = 0.48F + activity * (0.12F * wave + 0.05F * cross) + pullWeight * (0.055F + pull);
            grain = y * 0.58F - x * 0.34F + 0.045F * wave - drift;
            alpha = Math.round(255 * pin * (0.66F + 0.24F * activity));
        }

        void emit(VertexConsumer water, Matrix4f positions, Matrix3f normals) {
            // 材质横向是浪谷到浪尖的色阶，纵向是沿水面流动的晶纹。
            water.vertex(positions, position.x, position.y, position.z)
                    .color(255, 255, 255, alpha)
                    .uv(shade, grain)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(normals, normal.x, normal.y, normal.z)
                    .endVertex();
        }
    }
}
