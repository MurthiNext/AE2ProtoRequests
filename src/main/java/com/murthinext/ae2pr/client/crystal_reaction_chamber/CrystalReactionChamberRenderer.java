package com.murthinext.ae2pr.client.crystal_reaction_chamber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;
import com.murthinext.ae2pr.client.effect.EmissiveCubeRenderer;

/**
 * 水晶母岩放射电弧与高能核心自发光。
 */
public class CrystalReactionChamberRenderer
        implements BlockEntityRenderer<CrystalReactionChamberControllerBlockEntity> {

    private static final RenderType CORE = RenderType.beaconBeam(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/block/high_energy_core_glow.png"), true);
    private static final int ARC_COUNT = 12;
    private static final int SEGMENTS = 7;
    private static final double ARC_REACH = 1.42;

    public CrystalReactionChamberRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrystalReactionChamberControllerBlockEntity controller, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (!controller.isRunning() || controller.getLevel() == null) {
            return;
        }
        BlockPos quartz = controller.getBuddingQuartzPos();
        if (!controller.getLevel().getBlockState(quartz).is(AEBlocks.FLAWLESS_BUDDING_QUARTZ.block())) {
            return;
        }
        BlockPos origin = controller.getBlockPos();
        pose.pushPose();
        pose.translate(quartz.getX() - origin.getX() + 0.5, quartz.getY() - origin.getY() + 0.5,
                quartz.getZ() - origin.getZ() + 0.5);
        double time = controller.getLevel().getGameTime() + partialTick;
        Vec3 view = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
                .subtract(Vec3.atCenterOf(quartz));
        renderArcs(pose.last(), buffers.getBuffer(RenderType.lightning()), view, time, quartz.asLong());
        if (controller.getLevel().getBlockState(controller.getCorePos()).is(ModBlocks.HIGH_ENERGY_CORE.get())) {
            pose.translate(0, 2, 0);
            renderCore(pose, buffers.getBuffer(CORE));
        }
        pose.popPose();
    }

    private static void renderArcs(PoseStack.Pose pose, VertexConsumer consumer, Vec3 view, double time, long seed) {
        for (int arc = 0; arc < ARC_COUNT; arc++) {
            double clock = time + arc * 1.7;
            long epoch = (long) Math.floor(clock / 4.0);
            RandomSource random = RandomSource.create(seed ^ epoch * 73428767L ^ arc * 912931L);
            double angle = arc * 2.399963229728653 + (random.nextDouble() - 0.5) * 0.55;
            double y = 1.0 - 2.0 * (arc + 0.5) / ARC_COUNT;
            double radius = Math.sqrt(1.0 - y * y);
            Vec3 direction = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            double extent = Math.max(Math.abs(direction.x), Math.max(Math.abs(direction.y), Math.abs(direction.z)));
            Vec3 target = direction.scale(ARC_REACH / extent);
            float strength = (float) (0.45 + 0.55 * (1.0 - (clock / 4.0 - epoch)));
            Vec3 previous = Vec3.ZERO;
            for (int segment = 1; segment <= SEGMENTS; segment++) {
                double progress = (double) segment / SEGMENTS;
                double jitter = segment == SEGMENTS ? 0 : 0.23;
                Vec3 next = target.scale(progress).add((random.nextDouble() - 0.5) * jitter * 2,
                        (random.nextDouble() - 0.5) * jitter * 2, (random.nextDouble() - 0.5) * jitter * 2);
                renderDischarge(pose, consumer, previous, next, view, strength, 1.0);
                if (segment == 3 || segment == 5) {
                    Vec3 fork = next.add(direction.scale(0.25)).add((random.nextDouble() - 0.5) * 0.5,
                            (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
                    fork = new Vec3(Mth.clamp(fork.x, -ARC_REACH, ARC_REACH),
                            Mth.clamp(fork.y, -ARC_REACH, ARC_REACH), Mth.clamp(fork.z, -ARC_REACH, ARC_REACH));
                    Vec3 tip = fork.add(direction.scale(0.25));
                    tip = new Vec3(Mth.clamp(tip.x, -ARC_REACH, ARC_REACH),
                            Mth.clamp(tip.y, -ARC_REACH, ARC_REACH), Mth.clamp(tip.z, -ARC_REACH, ARC_REACH));
                    renderDischarge(pose, consumer, next, fork, view, strength * 0.65F, 0.55);
                    renderDischarge(pose, consumer, fork, tip, view, strength * 0.35F, 0.3);
                }
                previous = next;
            }
        }
    }

    private static void renderDischarge(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end,
            Vec3 view, float strength, double scale) {
        Vec3 direction = end.subtract(start);
        Vec3 side = view.subtract(start).cross(direction);
        if (side.lengthSqr() < 1.0E-8) {
            side = direction.cross(Math.abs(direction.y) < direction.length() * 0.9
                    ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
        }
        side = side.normalize().scale(scale);
        renderRibbon(pose, consumer, start, end, side.scale(0.038), 0.15F, 0.45F, 1.0F, strength * 0.16F);
        renderRibbon(pose, consumer, start, end, side.scale(0.017), 0.35F, 0.8F, 1.0F, strength * 0.6F);
        renderRibbon(pose, consumer, start, end, side.scale(0.006), 0.9F, 0.98F, 1.0F, strength);
    }

    private static void renderRibbon(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 side,
            float red, float green, float blue, float alpha) {
        for (int corner = 0; corner < 4; corner++) {
            Vec3 point = corner == 1 || corner == 2 ? end : start;
            double sign = corner < 2 ? -1.0 : 1.0;
            consumer.vertex(pose.pose(), (float) (point.x + side.x * sign), (float) (point.y + side.y * sign),
                    (float) (point.z + side.z * sign))
                    .color(red, green, blue, alpha).endVertex();
        }
    }

    private static void renderCore(PoseStack pose, VertexConsumer consumer) {
        EmissiveCubeRenderer.render(pose, consumer, 1.0F);
    }
}
