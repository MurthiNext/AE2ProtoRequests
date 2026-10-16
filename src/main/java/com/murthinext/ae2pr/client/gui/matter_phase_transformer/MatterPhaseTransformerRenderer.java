package com.murthinext.ae2pr.client.gui.matter_phase_transformer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;

/**
 * 相变仪空腔粒子、蓄能光束与中央核心自发光。
 */
public class MatterPhaseTransformerRenderer
        implements BlockEntityRenderer<MatterPhaseTransformerControllerBlockEntity> {

    private static final RenderType PARTICLE = ForgeRenderTypes.getUnlitTranslucent(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/particle/matter_particle.png"));
    private static final RenderType CORE = ForgeRenderTypes.getUnlitTranslucent(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/block/high_energy_core_glow.png"));
    private static final int PARTICLES_PER_CHAMBER = 20;
    private static final float[] BEAM_COLOR = { 1.0F, 1.0F, 1.0F };

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
            renderBeams(pose, buffers, time, beamStrength);
        }
        float activity = controller.getEffectActivity(partialTick);
        if (activity > 0.001F) {
            renderCore(pose, buffers.getBuffer(CORE), (int) (255.0F * activity));
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
            int alpha = (int) (255.0 * Math.min(1.0, Math.min(progress, 1.0 - progress) * 8.0)
                    * (1.0 - beamStrength * 0.8));
            pose.pushPose();
            pose.translate(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            renderParticle(consumer, pose.last(), size * 1.55F, alpha / 3);
            renderParticle(consumer, pose.last(), size, alpha);
            pose.popPose();
        }
    }

    private static void renderParticle(VertexConsumer consumer, PoseStack.Pose pose, float size, int alpha) {
        vertex(consumer, pose, -size, -size, 0, 0, 1, alpha, 0, 0, 1);
        vertex(consumer, pose, size, -size, 0, 1, 1, alpha, 0, 0, 1);
        vertex(consumer, pose, size, size, 0, 1, 0, alpha, 0, 0, 1);
        vertex(consumer, pose, -size, size, 0, 0, 0, alpha, 0, 0, 1);
    }

    private static void renderBeams(PoseStack pose, MultiBufferSource buffers, double time, float strength) {
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        long ticks = (long) Math.floor(time);
        float partialTick = (float) (time - ticks);
        for (int bottom = -3; bottom <= 1; bottom += 4) {
            BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION,
                    partialTick, 1.0F, ticks, bottom, 3, BEAM_COLOR, 0.12F * strength, 0.2F * strength);
        }
        pose.popPose();
    }

    private static void renderCore(PoseStack pose, VertexConsumer consumer, int alpha) {
        pose.pushPose();
        pose.translate(0, 0.5, 0);
        for (Direction direction : Direction.values()) {
            pose.pushPose();
            pose.mulPose(direction.getRotation());
            vertex(consumer, pose.last(), -0.5F, 0.501F, -0.5F, 0, 0, alpha, 0, 1, 0);
            vertex(consumer, pose.last(), -0.5F, 0.501F, 0.5F, 0, 1, alpha, 0, 1, 0);
            vertex(consumer, pose.last(), 0.5F, 0.501F, 0.5F, 1, 1, alpha, 0, 1, 0);
            vertex(consumer, pose.last(), 0.5F, 0.501F, -0.5F, 1, 0, alpha, 0, 1, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int alpha, float nx, float ny, float nz) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
