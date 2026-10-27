package com.murthinext.ae2pr.client.crystal_workstation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;
import com.murthinext.ae2pr.multiblock.structure.CrystalWorkstationStructure;

// 晶能工作站成型特效
public class CrystalWorkstationRenderer
        implements BlockEntityRenderer<CrystalWorkstationControllerBlockEntity> {

    private static final RenderType CORE = RenderType.entityTranslucentEmissive(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/block/crystal_workstation/core_cube.png"),
            false);
    private static final RenderType FLUID = RenderType.entityTranslucentEmissive(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/block/crystal_workstation/portal_liquid.png"),
            false);
    private static final RenderType MOTES = RenderType.entityTranslucentEmissive(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/particle/workstation_mote.png"), false);

    // 内腔半深
    private static final float CAVITY_HALF = 1.49F;

    private static final float PANEL_REACH = 3.47F;
    private static final float PANEL_EXTENT = 1.5F;

    private static final float CUBE_MIN_HALF = 0.5F;
    private static final float CUBE_MAX_HALF = 1.0F;
    private static final double CUBE_PERIOD = 320.0;

    private static final int SEA_GRID = 8;
    private static final int SEA_LAYERS = 3;

    private static final int MOTES_PER_SIDE = 5;
    private static final double TRAVEL_TICKS = 190.0;
    private static final double MOTE_REACH = 3.42;
    private static final double MOTE_SPREAD = 1.2;
    private static final float[] MOTE_COLOR = { 0.74F, 0.95F, 1.0F };

    public CrystalWorkstationRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrystalWorkstationControllerBlockEntity controller, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (!controller.isFormed() || controller.getLevel() == null) {
            return;
        }
        BlockPos core = controller.getCorePos();
        if (!controller.getLevel().getBlockState(core).is(ModBlocks.HIGH_ENERGY_CORE.get())) {
            return;
        }
        // 核心方块被模型层隐藏后才接管其渲染，避免未同步时出现重影
        if (!CrystalWorkstationShell.isCore(core)) {
            return;
        }
        Direction facing = controller.getBlockState().getValue(AbstractMultiblockControllerBlock.FACING);
        Direction sideAxis = CrystalWorkstationStructure.sideAxis(facing, controller.isMirrorSide(),
                controller.isMirrorFront());
        Direction depthAxis = CrystalWorkstationStructure.depthAxis(facing, controller.isMirrorSide(),
                controller.isMirrorFront());
        BlockPos origin = controller.getBlockPos();
        double time = controller.getLevel().getGameTime() + partialTick;
        float activity = controller.isRunning() ? 1.0F : 0.6F;

        pose.pushPose();
        pose.translate(core.getX() - origin.getX() + 0.5, core.getY() - origin.getY() + 0.5,
                core.getZ() - origin.getZ() + 0.5);
        renderCubes(pose, buffers.getBuffer(CORE), time, activity, depthAxis);
        renderFluid(pose, buffers.getBuffer(FLUID), time, activity, sideAxis, depthAxis);
        renderMotes(pose, buffers.getBuffer(MOTES), time, activity, sideAxis, depthAxis);
        pose.popPose();
    }

    // 立方体

    // 两颗立方体：棱长各自在 1~2 格之间起伏并周期交错，因此会互相换到内外位置
    private static void renderCubes(PoseStack pose, VertexConsumer consumer, double time, float activity,
            Direction depthAxis) {
        double wave = Math.sin(time * (Math.PI * 2.0 / CUBE_PERIOD));
        float first = (float) (CUBE_MIN_HALF + (CUBE_MAX_HALF - CUBE_MIN_HALF) * (0.5 + 0.5 * wave));
        float second = (float) (CUBE_MAX_HALF - (first - CUBE_MIN_HALF));
        // 绕三轴以互不相关的速度旋转，姿态可覆盖任意方向；尺寸互补所以内外位置会互换
        renderCube(pose, consumer, first, spin(time * 0.011), spin(time * 0.0073), spin(time * 0.0046), depthAxis,
                (int) ((96 + 26 * Math.sin(time * 0.055)) * activity), 0.72F, 0.95F, 1.0F);
        renderCube(pose, consumer, second, spin(-time * 0.0083), spin(time * 0.0121), spin(-time * 0.0061),
                depthAxis, (int) ((84 + 22 * Math.cos(time * 0.049)) * activity), 0.78F, 0.74F, 1.0F);
    }

    // 角度折算到一圈以内，避免长时间运行后精度下降
    private static float spin(double angle) {
        return (float) (angle % (Math.PI * 2.0));
    }

    private static void renderCube(PoseStack pose, VertexConsumer consumer, float half, float yaw, float pitch,
            float roll, Direction depthAxis, int alpha, float red, float green, float blue) {
        Quaternionf rotation = new Quaternionf(Axis.YP.rotation(yaw))
                .mul(Axis.XP.rotation(pitch))
                .mul(Axis.ZP.rotation(roll));
        float scale = fitScale(rotation, half, depthAxis);
        pose.pushPose();
        pose.mulPose(rotation);
        pose.scale(scale, scale, scale);
        for (Direction direction : Direction.values()) {
            pose.pushPose();
            pose.mulPose(direction.getRotation());
            consumer.vertex(pose.last().pose(), -half, half, -half).color(red, green, blue, alpha / 255.0F)
                    .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0).endVertex();
            consumer.vertex(pose.last().pose(), -half, half, half).color(red, green, blue, alpha / 255.0F)
                    .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0).endVertex();
            consumer.vertex(pose.last().pose(), half, half, half).color(red, green, blue, alpha / 255.0F)
                    .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0).endVertex();
            consumer.vertex(pose.last().pose(), half, half, -half).color(red, green, blue, alpha / 255.0F)
                    .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0).endVertex();
            pose.popPose();
        }
        pose.popPose();
    }

    // 旋转后的投影越过内腔时缩小，保证任意姿态下都不穿出玻璃
    private static float fitScale(Quaternionf rotation, float half, Direction depthAxis) {
        float x = depthAxis.getStepX();
        float y = depthAxis.getStepY();
        float z = depthAxis.getStepZ();
        Vector3f axisX = rotation.transform(new Vector3f(1, 0, 0));
        Vector3f axisY = rotation.transform(new Vector3f(0, 1, 0));
        Vector3f axisZ = rotation.transform(new Vector3f(0, 0, 1));
        float depth = half * (Math.abs(axisX.x * x + axisX.y * y + axisX.z * z)
                + Math.abs(axisY.x * x + axisY.y * y + axisY.z * z)
                + Math.abs(axisZ.x * x + axisZ.y * y + axisZ.z * z));
        float vertical = half * (Math.abs(axisX.y) + Math.abs(axisY.y) + Math.abs(axisZ.y));
        return Math.min(1.0F, CAVITY_HALF / Math.max(0.0001F, Math.max(depth, vertical)));
    }

    private static void renderFluid(PoseStack pose, VertexConsumer consumer, double time, float activity,
            Direction sideAxis, Direction depthAxis) {
        for (int sign : new int[] { 1, -1 }) {
            Panel panel = new Panel(pose.last(), sideAxis, depthAxis, sign);
            renderSea(consumer, panel, time, activity);
        }
    }

    private static void renderSea(VertexConsumer consumer, Panel panel, double time, float activity) {
        int grid = SEA_GRID;
        float step = PANEL_EXTENT * 2.0F / grid;
        for (int layer = 0; layer < SEA_LAYERS; layer++) {
            // 越靠内的层振幅略大、更透明，叠出水体厚度
            float baseline = 0.03F + layer * 0.075F;
            float amplitude = 0.05F + layer * 0.022F;
            int alpha = (int) ((96 - layer * 26) * activity);
            if (alpha <= 2) {
                continue;
            }
            double phase = time * (0.019 - layer * 0.004);
            float[][] height = new float[grid + 1][grid + 1];
            for (int i = 0; i <= grid; i++) {
                for (int j = 0; j <= grid; j++) {
                    float a = -PANEL_EXTENT + i * step;
                    float b = -PANEL_EXTENT + j * step;
                    // 单向推挤：始终在面板内侧，绝不越过面板平面
                    float crest = 0.5F + 0.5F * swell(a, b, phase, layer);
                    height[i][j] = -(baseline + amplitude * crest);
                }
            }
            for (int i = 0; i < grid; i++) {
                for (int j = 0; j < grid; j++) {
                    float a0 = -PANEL_EXTENT + i * step;
                    float a1 = a0 + step;
                    float b0 = -PANEL_EXTENT + j * step;
                    float b1 = b0 + step;
                    // UV 严格落在 sprite 内，避免采到图集里相邻贴图形成黑斑
                    float u0 = i / (float) grid;
                    float u1 = (i + 1) / (float) grid;
                    float v0 = j / (float) grid;
                    float v1 = (j + 1) / (float) grid;
                    // 浪峰更亮、浪谷更暗，强化起伏观感
                    float crest = (height[i][j] + height[i + 1][j] + height[i][j + 1] + height[i + 1][j + 1])
                            * -0.25F;
                    float lit = 0.55F + Math.min(1.0F, (crest - baseline) / Math.max(0.0001F, amplitude)) * 0.45F;
                    panel.vertex(consumer, a0, b0, height[i][j], u0, v0, 0.62F * lit, 0.90F * lit, lit, alpha);
                    panel.vertex(consumer, a0, b1, height[i][j + 1], u0, v1, 0.62F * lit, 0.90F * lit, lit, alpha);
                    panel.vertex(consumer, a1, b1, height[i + 1][j + 1], u1, v1, 0.62F * lit, 0.90F * lit, lit,
                            alpha);
                    panel.vertex(consumer, a1, b0, height[i + 1][j], u1, v0, 0.62F * lit, 0.90F * lit, lit, alpha);
                }
            }
        }
    }

    private static float swell(float a, float b, double time, int layer) {
        double t = time + layer * 1.7;
        double h = Math.sin(a * 1.35 + b * 0.55 + t) * 0.55
                + Math.sin(b * 1.9 - a * 0.42 - t * 0.83) * 0.32
                + Math.sin((a + b) * 2.6 + t * 1.37) * 0.18
                + Math.cos((a - b) * 3.4 - t * 0.61) * 0.12;
        return (float) h;
    }

    private record Panel(PoseStack.Pose pose, Direction normal, Direction depth, int sign) {

        void vertex(VertexConsumer consumer, float a, float b, float push, float u, float v, float red, float green,
                float blue, int alpha) {
            float reach = (PANEL_REACH + push) * sign;
            consumer.vertex(pose.pose(),
                    normal.getStepX() * reach + depth.getStepX() * b,
                    normal.getStepY() * reach + a,
                    normal.getStepZ() * reach + depth.getStepZ() * b)
                    .color(red, green, blue, alpha / 255.0F)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.normal(), normal.getStepX(), normal.getStepY(), normal.getStepZ())
                    .endVertex();
        }
    }

    private static void renderMotes(PoseStack pose, VertexConsumer consumer, double time, float activity,
            Direction side, Direction depth) {
        for (int sign : new int[] { 1, -1 }) {
            for (int i = 0; i < MOTES_PER_SIDE; i++) {
                // 相位错开保持稀疏
                double seed = i * 0.618033988749895 + (sign > 0 ? 0.0 : 0.5);
                double progress = (time / TRAVEL_TICKS + seed) % 1.0;
                double remaining = 1.0 - progress * progress * (3.0 - 2.0 * progress);
                double swirlAngle = progress * Math.PI * 1.7 + seed * 6.283185307179586;
                double swirl = Math.sin(progress * Math.PI) * 0.16;
                double a = (hash(seed * 3.1) * 2.0 - 1.0) * MOTE_SPREAD * remaining + Math.cos(swirlAngle) * swirl;
                double b = (hash(seed * 7.7) * 2.0 - 1.0) * MOTE_SPREAD * remaining + Math.sin(swirlAngle) * swirl;
                double reach = MOTE_REACH * sign * remaining;
                double fade = Math.min(1.0, progress / 0.14) * Math.min(1.0, (1.0 - progress) / 0.3);
                int alpha = (int) (225.0 * fade * activity);
                if (alpha <= 2) {
                    continue;
                }
                float size = 0.045F + (float) hash(seed * 11.3) * 0.025F;
                float tint = 0.85F + (float) hash(seed * 5.9) * 0.15F;
                pose.pushPose();
                pose.translate(side.getStepX() * reach + depth.getStepX() * b,
                        side.getStepY() * reach + a,
                        side.getStepZ() * reach + depth.getStepZ() * b);
                pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                float red = MOTE_COLOR[0] * tint;
                float green = MOTE_COLOR[1] * tint;
                float blue = MOTE_COLOR[2];
                consumer.vertex(pose.last().pose(), -size, -size, 0).color(red, green, blue, alpha / 255.0F).uv(0, 1)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.last().normal(), 0, 0, 1).endVertex();
                consumer.vertex(pose.last().pose(), size, -size, 0).color(red, green, blue, alpha / 255.0F).uv(1, 1)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.last().normal(), 0, 0, 1).endVertex();
                consumer.vertex(pose.last().pose(), size, size, 0).color(red, green, blue, alpha / 255.0F).uv(1, 0)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.last().normal(), 0, 0, 1).endVertex();
                consumer.vertex(pose.last().pose(), -size, size, 0).color(red, green, blue, alpha / 255.0F).uv(0, 0)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.last().normal(), 0, 0, 1).endVertex();
                pose.popPose();
            }
        }
    }

    private static double hash(double value) {
        double x = Math.sin(value * 12.9898) * 43758.5453;
        return x - Math.floor(x);
    }
}
