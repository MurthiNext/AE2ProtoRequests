package com.murthinext.ae2pr.client.jig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

import com.murthinext.ae2pr.block.jig.JigControllerBlockEntity;

public class JigRenderer implements BlockEntityRenderer<JigControllerBlockEntity> {

    private static final ResourceLocation WATER_STILL = ResourceLocation.fromNamespaceAndPath("minecraft",
            "block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.fromNamespaceAndPath("minecraft",
            "block/water_flow");
    private static final float SOURCE_HEIGHT = 8.0F / 9.0F;

    public JigRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(JigControllerBlockEntity controller, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        Level level = controller.getLevel();
        if (level == null || !controller.isRunning()) {
            return;
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite still = atlas.apply(WATER_STILL);
        TextureAtlasSprite flow = atlas.apply(WATER_FLOW);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        Direction back = controller.getFlowDirection().getOpposite();
        Direction side = controller.getSideDirection();
        BlockPos origin = controller.getBlockPos();
        float phase = ((level.getGameTime() % 40) + partialTick) / 40.0F;
        for (BlockPos cell : controller.getWaterCells()) {
            int dx = cell.getX() - origin.getX();
            int dz = cell.getZ() - origin.getZ();
            int depth = dx * back.getStepX() + dz * back.getStepZ();
            int across = dx * side.getStepX() + dz * side.getStepZ();
            boolean source = depth == 3;
            float frontHeight = source ? SOURCE_HEIGHT : (depth * 2 + 2) / 9.0F;
            float backHeight = source ? SOURCE_HEIGHT : (depth * 2 + 4) / 9.0F;
            int color = BiomeColors.getAverageWaterColor(level, cell);
            int cellLight = LevelRenderer.getLightColor(level, cell);
            float x = dx + 0.5F;
            float y = cell.getY() - origin.getY();
            float z = dz + 0.5F;
            if (source) {
                surface(consumer, pose.last(), still, side, back, x, y, z, frontHeight, backHeight,
                        -0.5F, 0.5F, 0.0F, 1.0F, color, cellLight);
            } else {
                // 在纹理边界拆分斜面，滚动 UV 始终限制在当前图集精灵内。
                surface(consumer, pose.last(), flow, side, back, x, y, z, frontHeight, backHeight,
                        -0.5F, 0.5F - phase, phase, 1.0F, color, cellLight);
                if (phase > 0.0F) {
                    surface(consumer, pose.last(), flow, side, back, x, y, z, frontHeight, backHeight,
                            0.5F - phase, 0.5F, 0.0F, phase, color, cellLight);
                }
            }
            if (depth == 0) {
                endFace(consumer, pose.last(), flow, side, back, x, y, z,
                        -0.5F, frontHeight, color, cellLight);
            } else if (source && !level.getBlockState(cell.relative(back)).canOcclude()) {
                endFace(consumer, pose.last(), still, side, back, x, y, z,
                        0.5F, backHeight, color, cellLight);
            }
            if (Math.abs(across) == 1 && !level.getBlockState(cell.relative(side, across)).canOcclude()) {
                sideFace(consumer, pose.last(), flow, side, back, x, y, z,
                        across * 0.5F, frontHeight, backHeight, color, cellLight);
            }
        }
    }

    private static void surface(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            Direction side, Direction back, float x, float y, float z, float frontHeight, float backHeight,
            float start, float end, float v0, float v1, int color, int light) {
        float y0 = y + frontHeight + (backHeight - frontHeight) * (start + 0.5F);
        float y1 = y + frontHeight + (backHeight - frontHeight) * (end + 0.5F);
        vertex(consumer, pose, sprite, x, y0, z, side, back, -0.5F, start, 0.0F, v0, color, light, Direction.UP);
        vertex(consumer, pose, sprite, x, y1, z, side, back, -0.5F, end, 0.0F, v1, color, light, Direction.UP);
        vertex(consumer, pose, sprite, x, y1, z, side, back, 0.5F, end, 1.0F, v1, color, light, Direction.UP);
        vertex(consumer, pose, sprite, x, y0, z, side, back, 0.5F, start, 1.0F, v0, color, light, Direction.UP);
    }

    private static void endFace(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            Direction side, Direction back, float x, float y, float z, float depth, float height,
            int color, int light) {
        Direction normal = depth < 0 ? back.getOpposite() : back;
        float v = 1.0F - height;
        vertex(consumer, pose, sprite, x, y, z, side, back, -0.5F, depth, 0.0F, 1.0F, color, light, normal);
        vertex(consumer, pose, sprite, x, y, z, side, back, 0.5F, depth, 1.0F, 1.0F, color, light, normal);
        vertex(consumer, pose, sprite, x, y + height, z, side, back, 0.5F, depth, 1.0F, v, color, light, normal);
        vertex(consumer, pose, sprite, x, y + height, z, side, back, -0.5F, depth, 0.0F, v, color, light, normal);
    }

    private static void sideFace(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            Direction side, Direction back, float x, float y, float z, float across,
            float frontHeight, float backHeight, int color, int light) {
        Direction normal = across < 0 ? side.getOpposite() : side;
        vertex(consumer, pose, sprite, x, y, z, side, back, across, -0.5F, 0.0F, 1.0F, color, light, normal);
        vertex(consumer, pose, sprite, x, y, z, side, back, across, 0.5F, 1.0F, 1.0F, color, light, normal);
        vertex(consumer, pose, sprite, x, y + backHeight, z, side, back, across, 0.5F,
                1.0F, 1.0F - backHeight, color, light, normal);
        vertex(consumer, pose, sprite, x, y + frontHeight, z, side, back, across, -0.5F,
                0.0F, 1.0F - frontHeight, color, light, normal);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x, float y, float z, Direction side, Direction back, float across, float depth,
            float u, float v, int color, int light, Direction normal) {
        consumer.vertex(pose.pose(), x + side.getStepX() * across + back.getStepX() * depth, y,
                z + side.getStepZ() * across + back.getStepZ() * depth)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, 190)
                .uv(sprite.getU(u * 16.0), sprite.getV(v * 16.0))
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), normal.getStepX(), normal.getStepY(), normal.getStepZ())
                .endVertex();
    }
}
