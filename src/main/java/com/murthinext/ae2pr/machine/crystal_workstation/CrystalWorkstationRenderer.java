package com.murthinext.ae2pr.machine.crystal_workstation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineBlock;
import com.murthinext.ae2pr.core.registry.ModMachines;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// 晶能工作站成型特效
public class CrystalWorkstationRenderer implements BlockEntityRenderer<CrystalWorkstationControllerBlockEntity> {

    private static final RenderType CORE =
            RenderType.entityTranslucentEmissive(Ids.of("textures/vfx/core_cube.png"), false);

    // 内腔半深
    private static final float CAVITY_HALF = 1.49F;

    private static final float CUBE_MIN_HALF = 0.5F;
    private static final float CUBE_MAX_HALF = 1.0F;
    private static final double CUBE_PERIOD = 320.0;
    // 尺寸判据的平滑柔度：绝对值、取大与封顶的硬拐角会让逐帧的缩放速度突变，观感上就是旋转时的轻微抽搐
    private static final float FIT_SOFTNESS = 0.04F;

    private final CrystalWorkstationTideSurface tide = new CrystalWorkstationTideSurface();

    public CrystalWorkstationRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            CrystalWorkstationControllerBlockEntity controller,
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
        if (!level.getBlockState(core).is(ModMachines.crystalReactor().block())) {
            return;
        }
        // 核心方块被模型层隐藏后才接管其渲染，避免未同步时出现重影
        if (!CrystalWorkstationShell.isCore(core)) {
            return;
        }
        Direction facing = controller.getBlockState().getValue(MachineBlock.FACING);
        Direction sideAxis =
                CrystalWorkstationStructure.sideAxis(facing, controller.isMirrorSide(), controller.isMirrorFront());
        Direction depthAxis =
                CrystalWorkstationStructure.depthAxis(facing, controller.isMirrorSide(), controller.isMirrorFront());
        BlockPos origin = controller.getBlockPos();
        double time = (double) level.getGameTime() + partialTick;
        float activity = controller.tideStrength(partialTick);

        pose.pushPose();
        pose.translate(
                core.getX() - origin.getX() + 0.5,
                core.getY() - origin.getY() + 0.5,
                core.getZ() - origin.getZ() + 0.5);
        float contact = renderCubes(pose, buffers.getBuffer(CORE), time, activity, depthAxis, sideAxis);
        double tideTime = time + (core.hashCode() & 255);
        var water = buffers.getBuffer(CrystalWorkstationTideSurface.MATERIAL);
        var left = tide.render(pose.last(), water, sideAxis.getOpposite(), depthAxis, tideTime, activity);
        var right = tide.render(
                pose.last(),
                water,
                sideAxis,
                depthAxis,
                tideTime + CrystalWorkstationTideSurface.SIDE_OFFSET_TICKS,
                activity);
        CrystalWorkstationInfusion.render(pose.last(), buffers, core, left, right, contact, activity);
        pose.popPose();
    }

    // 两颗立方体：棱长各自在 1~2 格之间起伏并周期交错，因此会互相换到内外位置
    private static float renderCubes(
            PoseStack pose,
            VertexConsumer consumer,
            double time,
            float activity,
            Direction depthAxis,
            Direction sideAxis) {
        double wave = Math.sin(time * (Math.PI * 2.0 / CUBE_PERIOD));
        float first = (float) (CUBE_MIN_HALF + (CUBE_MAX_HALF - CUBE_MIN_HALF) * (0.5 + 0.5 * wave));
        float second = CUBE_MAX_HALF - (first - CUBE_MIN_HALF);
        // 绕三轴以互不相关的速度旋转，姿态可覆盖任意方向；尺寸互补所以内外位置会互换
        float firstContact = renderCube(
                pose,
                consumer,
                first,
                spin(time * 0.011),
                spin(time * 0.0073),
                spin(time * 0.0046),
                depthAxis,
                sideAxis,
                (int) ((96 + 26 * Math.sin(time * 0.055)) * activity),
                0.72F,
                0.95F,
                1.0F);
        float secondContact = renderCube(
                pose,
                consumer,
                second,
                spin(-time * 0.0083),
                spin(time * 0.0121),
                spin(-time * 0.0061),
                depthAxis,
                sideAxis,
                (int) ((84 + 22 * Math.cos(time * 0.049)) * activity),
                0.78F,
                0.74F,
                1.0F);
        return Math.max(firstContact, secondContact);
    }

    // 角度折算到一圈以内，避免长时间运行后精度下降
    private static float spin(double angle) {
        return (float) (angle % (Math.PI * 2.0));
    }

    private static float renderCube(
            PoseStack pose,
            VertexConsumer consumer,
            float half,
            float yaw,
            float pitch,
            float roll,
            Direction depthAxis,
            Direction sideAxis,
            int alpha,
            float red,
            float green,
            float blue) {
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
            consumer.vertex(pose.last().pose(), -half, half, -half)
                    .color(red, green, blue, alpha / 255.0F)
                    .uv(0, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0)
                    .endVertex();
            consumer.vertex(pose.last().pose(), -half, half, half)
                    .color(red, green, blue, alpha / 255.0F)
                    .uv(0, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0)
                    .endVertex();
            consumer.vertex(pose.last().pose(), half, half, half)
                    .color(red, green, blue, alpha / 255.0F)
                    .uv(1, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0)
                    .endVertex();
            consumer.vertex(pose.last().pose(), half, half, -half)
                    .color(red, green, blue, alpha / 255.0F)
                    .uv(1, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), 0, 1, 0)
                    .endVertex();
            pose.popPose();
        }
        pose.popPose();
        // 在立方体局部空间求束轴与外表面的交点；两侧关于核心中心对称。
        Vector3f ray = rotation.transformInverse(new Vector3f(sideAxis.getStepX(), 0, sideAxis.getStepZ()));
        return half * scale / Math.max(Math.abs(ray.x), Math.max(Math.abs(ray.y), Math.abs(ray.z)));
    }

    // 旋转后的投影越过内腔时缩小，保证任意姿态下都不穿出玻璃
    private static float fitScale(Quaternionf rotation, float half, Direction depthAxis) {
        float x = depthAxis.getStepX();
        float y = depthAxis.getStepY();
        float z = depthAxis.getStepZ();
        Vector3f axisX = rotation.transform(new Vector3f(1, 0, 0));
        Vector3f axisY = rotation.transform(new Vector3f(0, 1, 0));
        Vector3f axisZ = rotation.transform(new Vector3f(0, 0, 1));
        float depth = softAbs(half * (axisX.x * x + axisX.y * y + axisX.z * z))
                + softAbs(half * (axisY.x * x + axisY.y * y + axisY.z * z))
                + softAbs(half * (axisZ.x * x + axisZ.y * y + axisZ.z * z));
        float vertical = softAbs(half * axisX.y) + softAbs(half * axisY.y) + softAbs(half * axisZ.y);
        float margin = CAVITY_HALF / softMax(depth, vertical) - 1.0F;
        return 1.0F + 0.5F * (margin - (float) Math.sqrt(margin * margin + FIT_SOFTNESS * FIT_SOFTNESS));
    }

    private static float softAbs(float value) {
        return (float) Math.sqrt(value * value + FIT_SOFTNESS * FIT_SOFTNESS);
    }

    private static float softMax(float a, float b) {
        float diff = a - b;
        return 0.5F * (a + b + (float) Math.sqrt(diff * diff + FIT_SOFTNESS * FIT_SOFTNESS));
    }
}
