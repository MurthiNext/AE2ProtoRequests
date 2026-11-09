package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.murthinext.ae2pr.api.Ids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.murthinext.ae2pr.client.effect.EmissiveCubeRenderer;
import com.murthinext.ae2pr.client.effect.RoundedBeamRenderer;
import com.murthinext.ae2pr.registry.ModMachines;

import org.joml.Quaternionf;

// 相变仪空腔粒子、蓄能光柱与中央核心自发光
public class MatterPhaseTransformerRenderer
        implements BlockEntityRenderer<MatterPhaseTransformerControllerBlockEntity> {

    // 三层统一使用原版信标光束层
    private static final RenderType PARTICLE = RenderType.beaconBeam(
            Ids.of("textures/particle/matter_particle.png"), true);
    private static final RenderType CORE = RenderType.beaconBeam(
            Ids.of("textures/block/high_energy_core_glow.png"), true);
    private static final RenderType BEAM = RenderType.beaconBeam(
            Ids.of("textures/entity/matter_beam.png"), true);
    private static final int PARTICLES_PER_CHAMBER = 96;
    private static final float[] WHITE = { 1.0F, 1.0F, 1.0F };

    public MatterPhaseTransformerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MatterPhaseTransformerControllerBlockEntity controller, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (!controller.isFormed() || controller.getLevel() == null) {
            return;
        }
        BlockPos core = controller.getCorePos();
        if (!controller.getLevel().getBlockState(core).is(ModMachines.CRYSTAL_REACTOR.block())) {
            return;
        }
        BlockPos origin = controller.getBlockPos();
        pose.pushPose();
        pose.translate(core.getX() - origin.getX() + 0.5, core.getY() - origin.getY(),
                core.getZ() - origin.getZ() + 0.5);
        double time = controller.getParticleTime(partialTick);
        float activity = controller.getEffectActivity(partialTick);
        float beamStrength = controller.getBeamStrength(partialTick);
        VertexConsumer particles = buffers.getBuffer(PARTICLE);
        renderChamber(pose, particles, time, activity, beamStrength, false);
        renderChamber(pose, particles, time, activity, beamStrength, true);
        if (beamStrength > 0.001F) {
            double ticks = controller.getLevel().getGameTime() + partialTick;
            renderBeams(pose, buffers.getBuffer(BEAM), core, ticks, beamStrength);
        }
        if (activity > 0.001F) {
            renderCore(pose, buffers.getBuffer(CORE), activity);
        }
        pose.popPose();
    }

    private static void renderChamber(PoseStack pose, VertexConsumer consumer, double time, float activity,
            float beamStrength, boolean upper) {
        if (beamStrength >= 0.999F) {
            return;
        }
        double focus = activity * activity * (3.0 - 2.0 * activity);
        Quaternionf cameraOrientation = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        for (int i = 0; i < PARTICLES_PER_CHAMBER; i++) {
            double seed = i * 2.399963229728653 + (upper ? 1.7 : 0.0);
            double progress = time * (0.0045 + (i % 5) * 0.0004) + i * 0.618033988749895;
            progress -= Math.floor(progress);
            double travel = progress * progress * (3.0 - 2.0 * progress);
            double angle = seed + (time * 0.018 + travel * focus * Math.PI) * (upper ? -1.0 : 1.0);
            double radius = 0.12 + (Math.sin(seed * 3.0) + 1.0) * 0.13;
            radius *= 1.0 - focus * (0.72 + travel * 0.24);
            double y = upper ? 3.9 - progress * 2.8 : -2.9 + progress * 2.8;
            float size = (0.01F + (i % 4) * 0.01F) * (1.0F - activity * 0.25F);
            double fade = Math.min(1.0, Math.min(progress, 1.0 - progress) * 8.0);
            float alpha = (float) (fade * fade * (3.0 - 2.0 * fade)
                    * (0.4 + (i % 3) * 0.1) * (1.0 - beamStrength));
            pose.pushPose();
            pose.translate(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            pose.mulPose(cameraOrientation);
            renderParticle(consumer, pose.last(), size * 1.8F, alpha * 0.18F);
            renderParticle(consumer, pose.last(), size, alpha);
            pose.popPose();
        }
    }

    private static void renderParticle(VertexConsumer consumer, PoseStack.Pose pose, float size, float alpha) {
        // 信标层开启了背面剔除，而广告牌朝向取决于视角；正反两种绕序各写一遍，
        // 被剔除的总是其中一半，结果仍是一张可见的四边形
        vertex(consumer, pose, -size, -size, 0, 0, 1, alpha);
        vertex(consumer, pose, size, -size, 0, 1, 1, alpha);
        vertex(consumer, pose, size, size, 0, 1, 0, alpha);
        vertex(consumer, pose, -size, size, 0, 0, 0, alpha);
        vertex(consumer, pose, -size, -size, 0, 0, 1, alpha);
        vertex(consumer, pose, -size, size, 0, 0, 0, alpha);
        vertex(consumer, pose, size, size, 0, 1, 0, alpha);
        vertex(consumer, pose, size, -size, 0, 1, 1, alpha);
    }

    private static void renderBeams(PoseStack pose, VertexConsumer consumer, BlockPos core, double ticks,
            float strength) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 view = new Vec3(camera.x - (core.getX() + 0.5), 0, camera.z - (core.getZ() + 0.5));
        view = view.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : view.normalize();
        // 核心方块本体遮挡中段，光柱分上下两截
        RoundedBeamRenderer.renderColumn(pose, consumer, view, -3.0, 0.0, true, ticks, strength, WHITE);
        RoundedBeamRenderer.renderColumn(pose, consumer, view, 1.0, 4.0, false, ticks, strength, WHITE);
    }

    private static void renderCore(PoseStack pose, VertexConsumer consumer, float alpha) {
        pose.pushPose();
        pose.translate(0, 0.5, 0);
        EmissiveCubeRenderer.render(pose, consumer, alpha);
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, float alpha) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(WHITE[0], WHITE[1], WHITE[2], alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
