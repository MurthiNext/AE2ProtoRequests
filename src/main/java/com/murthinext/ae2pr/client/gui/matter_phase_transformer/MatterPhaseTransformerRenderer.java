package com.murthinext.ae2pr.client.gui.matter_phase_transformer;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;
import com.murthinext.ae2pr.client.effect.EmissiveCubeRenderer;
import com.murthinext.ae2pr.client.effect.RoundedBeamRenderer;

/**
 * 相变仪空腔粒子、蓄能光柱与中央核心自发光。
 */
public class MatterPhaseTransformerRenderer
        implements BlockEntityRenderer<MatterPhaseTransformerControllerBlockEntity> {

    // 三层统一使用原版信标光束层：它的着色器只做 texture * vertexColor，不参与 mix_light 方向光，
    // 自发光体不会因视角变化发灰；光影包也把它映射到 gbuffers_beaconbeam。
    // Forge 的 unlit 层不在光影管线里，会被 DepthColorStorage 关掉写色导致整片不可见。
    private static final RenderType PARTICLE = RenderType.beaconBeam(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/particle/matter_particle.png"), true);
    private static final RenderType CORE = RenderType.beaconBeam(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/block/high_energy_core_glow.png"), true);
    private static final RenderType BEAM = RenderType.beaconBeam(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/entity/matter_beam.png"), true);
    private static final int PARTICLES_PER_CHAMBER = 20;
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
        if (!controller.getLevel().getBlockState(core).is(ModBlocks.HIGH_ENERGY_CORE.get())) {
            return;
        }
        BlockPos origin = controller.getBlockPos();
        pose.pushPose();
        pose.translate(core.getX() - origin.getX() + 0.5, core.getY() - origin.getY(),
                core.getZ() - origin.getZ() + 0.5);
        double time = controller.getParticleTime(partialTick);
        float beamStrength = controller.getBeamStrength(partialTick);
        VertexConsumer particles = buffers.getBuffer(PARTICLE);
        renderChamber(pose, particles, time, beamStrength, false);
        renderChamber(pose, particles, time, beamStrength, true);
        if (beamStrength > 0.001F) {
            double ticks = controller.getLevel().getGameTime() + partialTick;
            renderBeams(pose, buffers.getBuffer(BEAM), core, ticks, beamStrength);
        }
        float activity = controller.getEffectActivity(partialTick);
        if (activity > 0.001F) {
            renderCore(pose, buffers.getBuffer(CORE), activity);
        }
        pose.popPose();
    }

    private static void renderChamber(PoseStack pose, VertexConsumer consumer, double time, float beamStrength,
            boolean upper) {
        for (int i = 0; i < PARTICLES_PER_CHAMBER; i++) {
            double seed = i * 2.399963229728653 + (upper ? 1.7 : 0.0);
            double progress = time * (0.006 + (i % 4) * 0.0006) + i * 0.618033988749895;
            progress -= Math.floor(progress);
            double angle = seed + time * 0.025 * (upper ? -1.0 : 1.0);
            double radius = 0.12 + (Math.sin(seed * 3.0) + 1.0) * 0.07;
            radius *= 1.0 - beamStrength * 0.85;
            double y = upper ? 1.25 + (1.0 - progress) * 2.5 : -2.75 + progress * 2.5;
            float size = 0.07F + (i % 3) * 0.014F;
            size *= 1.0F - beamStrength * 0.45F;
            float alpha = (float) (Math.min(1.0, Math.min(progress, 1.0 - progress) * 8.0)
                    * (1.0 - beamStrength * 0.8));
            pose.pushPose();
            pose.translate(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            renderParticle(consumer, pose.last(), size * 1.55F, alpha / 3);
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
        RoundedBeamRenderer.renderColumn(pose, consumer, view, -3.0, 0.0, ticks, strength, WHITE);
        RoundedBeamRenderer.renderColumn(pose, consumer, view, 1.0, 4.0, ticks, strength, WHITE);
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
