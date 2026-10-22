package com.murthinext.ae2pr.client.multiblock;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

public final class MultiblockPreviewRenderer {

    private final MultiblockPreview preview;
    private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new BufferBuilder(262144));
    private final RandomSource random = RandomSource.create();
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private float yaw = 135.0F;
    private float pitch = 25.0F;
    private float zoom = 1.0F;

    public MultiblockPreviewRenderer(MultiblockPreview preview, int x, int y, int width, int height) {
        this.preview = preview;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void rotate(double dragX, double dragY) {
        yaw = (yaw + (float) dragX * 0.7F) % 360.0F;
        pitch = Mth.clamp(pitch + (float) dragY * 0.7F, -89.0F, 89.0F);
    }

    public void zoom(double delta) {
        zoom = Mth.clamp(zoom * (float) Math.pow(1.12, delta), 0.35F, 4.0F);
    }

    public void reset() {
        yaw = 135.0F;
        pitch = 25.0F;
        zoom = 1.0F;
    }

    private Matrix4f transform() {
        AABB bounds = preview.bounds();
        double diameter = Math.sqrt(bounds.getXsize() * bounds.getXsize() + bounds.getYsize() * bounds.getYsize()
                + bounds.getZsize() * bounds.getZsize());
        float scale = (float) (Math.min(width, height) * 0.88 / diameter) * zoom;
        Vec3 center = bounds.getCenter();
        return new Matrix4f().translation(x + width / 2.0F, y + height / 2.0F, 150.0F)
                .scale(scale, -scale, scale)
                .rotateX(pitch * Mth.DEG_TO_RAD).rotateY(yaw * Mth.DEG_TO_RAD)
                .translate((float) -center.x, (float) -center.y, (float) -center.z);
    }

    @Nullable
    public MultiblockPreview.Cell pick(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return null;
        }
        Matrix4f inverse = transform().invert();
        Vector3f near = inverse.transformPosition(new Vector3f((float) mouseX, (float) mouseY, 1000.0F));
        Vector3f far = inverse.transformPosition(new Vector3f((float) mouseX, (float) mouseY, -1000.0F));
        Vec3 from = new Vec3(near.x, near.y, near.z);
        Vec3 to = new Vec3(far.x, far.y, far.z);
        double nearest = Double.MAX_VALUE;
        MultiblockPreview.Cell result = null;
        for (MultiblockPreview.Cell cell : preview.cells()) {
            if (!preview.isVisible(cell.pos())) {
                continue;
            }
            BlockHitResult hit = preview.getBlockState(cell.pos()).getShape(preview, cell.pos())
                    .clip(from, to, cell.pos());
            if (hit != null && hit.getLocation().distanceToSqr(from) < nearest) {
                nearest = hit.getLocation().distanceToSqr(from);
                result = cell;
            }
        }
        return result;
    }

    public void render(GuiGraphics graphics, @Nullable BlockPos selected, @Nullable BlockPos hovered) {
        graphics.flush();
        Matrix4f guiTransform = graphics.pose().last().pose();
        Vector3f topLeft = guiTransform.transformPosition(new Vector3f(x, y, 0));
        Vector3f bottomRight = guiTransform.transformPosition(new Vector3f(x + width, y + height, 0));
        graphics.enableScissor(Mth.floor(topLeft.x), Mth.floor(topLeft.y),
                Mth.ceil(bottomRight.x), Mth.ceil(bottomRight.y));
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableDepthTest();
            pose.mulPoseMatrix(transform());
            pose.last().normal().mul(new Matrix3f().scaling(1.0F, -1.0F, 1.0F)
                    .rotateX(pitch * Mth.DEG_TO_RAD).rotateY(yaw * Mth.DEG_TO_RAD));
            var dispatcher = Minecraft.getInstance().getBlockRenderer();
            for (RenderType layer : RenderType.chunkBufferLayers()) {
                for (MultiblockPreview.Cell cell : preview.cells()) {
                    if (!preview.isVisible(cell.pos())) {
                        continue;
                    }
                    BlockState state = preview.getBlockState(cell.pos());
                    var model = dispatcher.getBlockModel(state);
                    ModelData data = model.getModelData(preview, cell.pos(), state, ModelData.EMPTY);
                    random.setSeed(state.getSeed(cell.pos()));
                    if (!model.getRenderTypes(state, random, data).contains(layer)) {
                        continue;
                    }
                    pose.pushPose();
                    pose.translate(cell.pos().getX(), cell.pos().getY(), cell.pos().getZ());
                    dispatcher.renderBatched(state, cell.pos(), preview, pose,
                            buffers.getBuffer(RenderTypeHelper.getEntityRenderType(layer, false)), true,
                            random, data, layer);
                    pose.popPose();
                }
                buffers.endBatch();
            }
            if (selected != null && preview.isVisible(selected)) {
                LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()),
                        new AABB(selected).inflate(0.006), 0.25F, 0.9F, 1.0F, 1.0F);
            }
            if (hovered != null && !hovered.equals(selected)) {
                LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()),
                        new AABB(hovered).inflate(0.004), 1.0F, 1.0F, 1.0F, 0.7F);
            }
            buffers.endBatch();
        } finally {
            pose.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            graphics.disableScissor();
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
