package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.block.feature.HighEnergyCoreBlock;
import com.murthinext.ae2pr.core.registry.ModMachines;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MatterPhaseTransformerRenderer
        implements BlockEntityRenderer<MatterPhaseTransformerControllerBlockEntity> {

    private static final RenderType FLARE =
            RenderType.beaconBeam(Ids.of("textures/particle/matter_particle.png"), true);
    private static final RenderType BEAM = RenderType.beaconBeam(Ids.of("textures/vfx/matter_beam.png"), true);
    private static final RenderType APERTURE_FACE =
            RenderType.entityTranslucent(Ids.of("textures/vfx/matter_aperture.png"));
    private static final double PACKET_TICKS = 9;
    private static final float APERTURE_INSET = 0.002F;
    private static final double LENS_HALF = 0.45;
    private static final Vec3 EMITTER = new Vec3(0, MatterPhaseTransformerStructure.EMITTER_Y - 0.008, 0);
    private static final Vec3[] POLES = new Vec3[3];

    static {
        for (int i = 0; i < POLES.length; i++) {
            double angle = i * 2 * Math.PI / POLES.length;
            POLES[i] = EMITTER.add(0.64 * LENS_HALF * Math.cos(angle), 0, 0.64 * LENS_HALF * Math.sin(angle));
        }
    }

    public MatterPhaseTransformerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            MatterPhaseTransformerControllerBlockEntity controller,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        var level = controller.getLevel();
        if (!controller.isFormed() || level == null) {
            return;
        }
        BlockPos core = controller.getCorePos();
        var coreState = level.getBlockState(core);
        if (!coreState.is(ModMachines.crystalReactor().block())
                || !coreState.getValue(HighEnergyCoreBlock.PHASE_HIDDEN)) {
            return;
        }
        BlockPos origin = controller.getBlockPos();
        var animation = controller.animation();
        float power = animation.power(partialTick);
        float heat = animation.heat(partialTick);
        double pourTime = animation.pourTime(partialTick);
        double time = (double) level.getGameTime() + partialTick + (core.hashCode() & 255);
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition().subtract(Vec3.atBottomCenterOf(core));

        pose.pushPose();
        pose.translate(
                core.getX() - origin.getX() + 0.5, core.getY() - origin.getY(), core.getZ() - origin.getZ() + 0.5);
        var cap = new PhaseChamberMesh(pose.last(), buffers.getBuffer(APERTURE_FACE), APERTURE_INSET);
        cap.panel(MatterPhaseTransformerStructure.EMITTER_Y, 0xFFFFFFFF);
        PhaseMeltRenderer.crystal(pose, buffers, heat);
        Vec3 focus = PhaseMeltRenderer.IMPACT;
        PhaseMeltRenderer.pour(pose, buffers, controller.melt(), pourTime, heat);
        float phase = (float) Mth.frac(time / PACKET_TICKS);
        beams(new PhaseChamberMesh(pose.last(), buffers.getBuffer(BEAM)), eye, focus, phase, power);
        var flares = new PhaseChamberMesh(pose.last(), buffers.getBuffer(FLARE));
        discharge(flares, camera, eye, focus, phase, power);
        if (power > 0) {
            sparks(flares, eye, focus, time, power);
        }
        pose.popPose();
    }

    private static void beams(PhaseChamberMesh mesh, Vec3 eye, Vec3 focus, float phase, float power) {
        mesh.ribbon(
                eye,
                EMITTER,
                focus,
                0.07 + 0.12 * power,
                0.04 + 0.065 * power,
                -phase,
                2 - phase,
                tint(0xFFFFFF, 0.18 + 0.82 * power));
        if (power > 0) {
            for (Vec3 pole : POLES) {
                mesh.ribbon(
                        eye,
                        pole,
                        focus,
                        0.080 * power,
                        0.037 * power,
                        -phase,
                        2 - phase,
                        tint(0xB3C8FF, 0.83 * power));
            }
        }
    }

    private static void discharge(
            PhaseChamberMesh mesh, Camera camera, Vec3 eye, Vec3 focus, float phase, float power) {
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 up = new Vec3(camera.getUpVector());
        for (Vec3 pole : POLES) {
            mesh.sprite(pole.add(0, -0.05, 0), right.scale(0.10), up.scale(0.046), tint(0x8CEBFF, 0.37 + 0.57 * power));
            for (int i = 0; i < 2; i++) {
                double head = (phase + i) * 0.5;
                Vec3 tail = pole.lerp(focus, Math.max(0, head - 0.025 - 0.072 * power));
                mesh.ribbon(
                        eye,
                        tail,
                        pole.lerp(focus, head),
                        0.009,
                        0.026 + 0.013 * power,
                        0,
                        1,
                        tint(0xD9FAFF, 0.32 + 0.68 * power));
            }
        }
        double pulse = Math.pow(0.5 + 0.5 * Math.cos(2 * Math.PI * phase), 4);
        double size = 0.11 + 0.11 * power + 0.045 * pulse * power;
        mesh.sprite(
                focus.add(0, 0.009, 0),
                right.scale(size),
                up.scale(size * 0.65),
                tint(0xFF9C39, power * (0.72 + 0.28 * pulse)));
        mesh.sprite(
                focus.add(0, 0.015, 0),
                right.scale(size * 0.72),
                up.scale(size * 0.27),
                tint(0xFFF6D8, 0.2 + 0.8 * power));
    }

    private static void sparks(PhaseChamberMesh mesh, Vec3 eye, Vec3 focus, double time, float power) {
        for (int i = 0; i < 8; i++) {
            double beat = time / PACKET_TICKS - i * 0.037;
            double f = Mth.frac(beat);
            double tail = Math.max(0, f - 0.17);
            double angle = ((long) Math.floor(beat) & 255) * 1.73 + i * 2.39996;
            double reach = 0.21 + 0.045 * (i % 3);
            double x = Math.cos(angle) * reach;
            double z = Math.sin(angle) * reach;
            double rise = 1.18 + 0.23 * (i % 4);
            Vec3 a = focus.add(tail * x, rise * tail * (1 - tail), tail * z);
            Vec3 b = focus.add(f * x, rise * f * (1 - f), f * z);
            mesh.ribbon(eye, a, b, 0.009, 0.023, 0, 1, tint(i % 3 == 0 ? 0xFF8839 : 0xFFE6A3, power * (1 - f * f)));
        }
    }

    private static int tint(int rgb, double alpha) {
        return (int) (255 * alpha) << 24 | rgb;
    }
}
