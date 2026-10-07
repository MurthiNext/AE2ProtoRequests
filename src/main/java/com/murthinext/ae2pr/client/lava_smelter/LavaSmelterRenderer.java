package com.murthinext.ae2pr.client.lava_smelter;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.lava_smelter.HighReactivityLavaSmelterBlock;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterStructure;

/**
 * 炉膛假熔岩渲染。
 */
public class LavaSmelterRenderer implements BlockEntityRenderer<LavaSmelterControllerBlockEntity> {

    private static final ResourceLocation ALIEN_LAVA_STILL = new ResourceLocation(ae2pr.MODID,
            "block/fluid/alien_lava_still");
    private static final ResourceLocation LAVA_STILL = new ResourceLocation("minecraft", "block/lava_still");

    /** 液面高度（方块内） */
    private static final float SURFACE_HEIGHT = 0.875F;
    /** 与炉壁的防重叠内缩 */
    private static final float INSET = 0.02F;

    public LavaSmelterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LavaSmelterControllerBlockEntity controller, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        if (!controller.isFormed()) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        Direction facing = controller.getBlockState().getValue(HighReactivityLavaSmelterBlock.FACING);
        List<BlockPos> cells = LavaSmelterStructure.poolCells(pos, facing, controller.isMirrorSide(),
                controller.isMirrorFront());

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int bottomY = cells.get(0).getY();
        for (BlockPos cell : cells) {
            minX = Math.min(minX, cell.getX());
            minZ = Math.min(minZ, cell.getZ());
            maxX = Math.max(maxX, cell.getX());
            maxZ = Math.max(maxZ, cell.getZ());
        }

        float x0 = minX - pos.getX() + INSET;
        float x1 = maxX + 1 - pos.getX() - INSET;
        float z0 = minZ - pos.getZ() + INSET;
        float z1 = maxZ + 1 - pos.getZ() - INSET;
        float y0 = bottomY - pos.getY() + INSET;
        float y1 = bottomY - pos.getY() + SURFACE_HEIGHT;

        TextureAtlasSprite sprite = sprite(controller.getDurability() > 0 ? ALIEN_LAVA_STILL : LAVA_STILL);
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        Matrix4f matrix = pose.last().pose();

        // 顶面
        quad(consumer, matrix, sprite, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0.0F, 1.0F, 0.0F);
        // 底面
        quad(consumer, matrix, sprite, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0.0F, -1.0F, 0.0F);
        // 北面
        quad(consumer, matrix, sprite, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0.0F, 0.0F, -1.0F);
        // 南面
        quad(consumer, matrix, sprite, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, 0.0F, 0.0F, 1.0F);
        // 东面
        quad(consumer, matrix, sprite, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1.0F, 0.0F, 0.0F);
        // 西面
        quad(consumer, matrix, sprite, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1.0F, 0.0F, 0.0F);
    }

    private static TextureAtlasSprite sprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
    }

    /** 按逆时针（从外侧看）顺序输出一个四边形。 */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, TextureAtlasSprite sprite,
            float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
            float dx, float dy, float dz, float nx, float ny, float nz) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        vertex(consumer, matrix, ax, ay, az, u0, v0, nx, ny, nz);
        vertex(consumer, matrix, bx, by, bz, u1, v0, nx, ny, nz);
        vertex(consumer, matrix, cx, cy, cz, u1, v1, nx, ny, nz);
        vertex(consumer, matrix, dx, dy, dz, u0, v1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
            float u, float v, float nx, float ny, float nz) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(nx, ny, nz)
                .endVertex();
    }
}
