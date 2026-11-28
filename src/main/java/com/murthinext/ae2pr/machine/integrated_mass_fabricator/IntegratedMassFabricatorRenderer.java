package com.murthinext.ae2pr.machine.integrated_mass_fabricator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murthinext.ae2pr.api.Ids;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class IntegratedMassFabricatorRenderer
        implements BlockEntityRenderer<IntegratedMassFabricatorControllerBlockEntity> {

    // 信标光束着色器直接使用纹理颜色，不会把朝下的天花板花纹压暗。
    private static final RenderType FIELD = RenderType.beaconBeam(Ids.of("textures/vfx/mass_field_inlay.png"), true);
    private static final float HALF = IntegratedMassFabricatorStructure.FIELD_WIDTH / 2.0F;
    private static final float OFFSET = 0.003F;

    public IntegratedMassFabricatorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            IntegratedMassFabricatorControllerBlockEntity controller,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        if (!controller.isFormed()) {
            return;
        }
        var floor = controller.floorCenter().subtract(controller.getBlockPos());
        pose.pushPose();
        pose.translate(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        var buffer = buffers.getBuffer(FIELD);
        plane(pose.last(), buffer, 1 + OFFSET, 1);
        plane(pose.last(), buffer, IntegratedMassFabricatorStructure.PLANE_SEPARATION - OFFSET, -1);
        pose.popPose();
    }

    private static void plane(PoseStack.Pose pose, VertexConsumer buffer, float y, int normal) {
        vertex(pose, buffer, -HALF, y, -HALF * normal, 0, 0, normal);
        vertex(pose, buffer, -HALF, y, HALF * normal, 0, 1, normal);
        vertex(pose, buffer, HALF, y, HALF * normal, 1, 1, normal);
        vertex(pose, buffer, HALF, y, -HALF * normal, 1, 0, normal);
    }

    private static void vertex(
            PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int normal) {
        buffer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, normal, 0)
                .endVertex();
    }
}
