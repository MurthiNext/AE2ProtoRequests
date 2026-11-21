package com.murthinext.ae2pr.machine.crystal_workstation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murthinext.ae2pr.api.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record CrystalWorkstationInfusion(Direction outward, float sourceReach, float beat) {

    private static final RenderType BEAM =
            RenderType.beaconBeam(Ids.of("textures/vfx/infusion_beam.png"), true);
    private static final RenderType FLARE =
            RenderType.beaconBeam(Ids.of("textures/particle/workstation_mote.png"), true);
    private static final int SECTIONS = 16;
    private static final float FLIGHT_CYCLES = 0.55F;
    private static final float FADE_DISTANCE = 0.80F;
    private static final float ABSORPTION_DEPTH = 0.16F;

    static void render(
            PoseStack.Pose pose,
            MultiBufferSource buffers,
            BlockPos core,
            CrystalWorkstationInfusion left,
            CrystalWorkstationInfusion right,
            float contact,
            float activity) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition().subtract(Vec3.atCenterOf(core));
        var beams = buffers.getBuffer(BEAM);
        left.beam(pose, beams, eye, contact, activity);
        right.beam(pose, beams, eye, contact, activity);
        var flares = buffers.getBuffer(FLARE);
        Vec3 across = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 up = new Vec3(camera.getUpVector());
        left.flares(pose, flares, across, up, contact, activity);
        right.flares(pose, flares, across, up, contact, activity);
    }

    private void beam(PoseStack.Pose pose, VertexConsumer vertices, Vec3 eye, float contact, float activity) {
        var axis = Vec3.atLowerCornerOf(outward.getNormal());
        var width = axis.cross(eye);
        width = width.lengthSqr() < 1.0E-8 ? new Vec3(0, 1, 0) : width.normalize();
        float endProgress = 1 + ABSORPTION_DEPTH / (sourceReach - contact);
        Edge previous = section(axis, width, contact, 0, activity);
        for (int i = 1; i <= SECTIONS; i++) {
            Edge next = section(axis, width, contact, endProgress * i / SECTIONS, activity);
            quad(pose, vertices, previous, next);
            previous = next;
        }
    }

    private Edge section(Vec3 axis, Vec3 width, float contact, float progress, float activity) {
        float packet = pulse(progress);
        float lip = Math.max(0, 1 - progress);
        float radius = (0.19F + 0.22F * lip * lip * lip + 0.085F * packet) * (0.35F + 0.65F * activity);
        float reach = Mth.lerp(progress, sourceReach, contact);
        float alpha = (0.72F + 0.28F * packet) * activity * transmission(reach - contact);
        return Edge.at(axis.scale(reach), width.scale(radius), FLIGHT_CYCLES * progress - beat, color(0xF4FFFF, alpha));
    }

    private static float transmission(float distanceToFace) {
        float t = Mth.clamp((distanceToFace + ABSORPTION_DEPTH) / (FADE_DISTANCE + ABSORPTION_DEPTH), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private void flares(
            PoseStack.Pose pose, VertexConsumer vertices, Vec3 across, Vec3 up, float contact, float activity) {
        Vec3 axis = Vec3.atLowerCornerOf(outward.getNormal());
        float launch = pulse(0);
        float arrival = pulse(1);
        flare(
                pose,
                vertices,
                axis.scale(sourceReach),
                across,
                up,
                (0.19F + 0.07F * launch) * activity,
                color(0x8EE8F4, (0.38F + 0.36F * launch) * activity));
        flare(
                pose,
                vertices,
                axis.scale(contact),
                across,
                up,
                (0.22F + 0.08F * arrival) * activity,
                color(0xC5E8F4, transmission(0) * (0.35F + 0.55F * arrival) * activity));
    }

    private float pulse(float progress) {
        float light = 0.5F + 0.5F * Mth.cos(Mth.TWO_PI * (beat - progress * FLIGHT_CYCLES));
        return light * light * light * light;
    }

    private static void flare(
            PoseStack.Pose pose, VertexConsumer vertices, Vec3 center, Vec3 across, Vec3 up, float radius, int color) {
        Vec3 a = across.scale(radius);
        Vec3 b = up.scale(radius * 0.83);
        quad(pose, vertices, Edge.at(center.add(b), a, 0, color), Edge.at(center.subtract(b), a, 1, color));
    }

    private record Edge(Vec3 left, Vec3 right, float v, int color) {
        static Edge at(Vec3 center, Vec3 span, float v, int color) {
            return new Edge(center.subtract(span), center.add(span), v, color);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vertices, Edge start, Edge end) {
        vertex(pose, vertices, start.left, 0, start.v, start.color);
        vertex(pose, vertices, end.left, 0, end.v, end.color);
        vertex(pose, vertices, end.right, 1, end.v, end.color);
        vertex(pose, vertices, start.right, 1, start.v, start.color);
    }

    private static int color(int rgb, float alpha) {
        return Math.round(255 * alpha) << 24 | rgb;
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, Vec3 point, float u, float v, int color) {
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(color)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();
    }
}
