package com.murthinext.ae2pr.client.multi_factory;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public final class MultiFactoryRenderer implements BlockEntityRenderer<MultiFactoryControllerBlockEntity> {

    private static final float BASE_OFFSET = 0.003F;
    private static final float LIGHT_OFFSET = 0.005F;

    private final Function<ResourceLocation, TextureAtlasSprite> atlas;
    private final RandomSource random = RandomSource.create();

    public MultiFactoryRenderer(BlockEntityRendererProvider.Context context) {
        atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    }

    @Override
    public void render(MultiFactoryControllerBlockEntity controller, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int combinedLight, int combinedOverlay) {
        Level level = controller.getLevel();
        if (level == null || !controller.isFormed()) {
            return;
        }
        BlockState state = controller.getBlockState();
        BlockPos core = controller.getCorePos();
        Direction front = state.getValue(AbstractMultiblockControllerBlock.FACING);
        if (controller.isMirrorFront()) {
            front = front.getOpposite();
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());
        for (Direction normal : Direction.values()) {
            Surface surface = surface(front, normal);
            TextureAtlasSprite base = atlas.apply(surface.panel().base);
            ResourceLocation glowTexture = surface.panel().glow(
                    state.getValue(AbstractMultiblockControllerBlock.RUNNING),
                    state.getValue(AbstractMultiblockControllerBlock.PAUSED));
            TextureAtlasSprite glow = glowTexture == null ? null : atlas.apply(glowTexture);
            renderSurface(level, controller.getBlockPos(), core, surface, base, glow, poseStack, buffer,
                    combinedOverlay);
        }
    }

    private void renderSurface(Level level, BlockPos controllerPos, BlockPos core, Surface surface,
            TextureAtlasSprite base, @Nullable TextureAtlasSprite glow, PoseStack poseStack, VertexConsumer buffer,
            int overlay) {
        Direction normal = surface.normal();
        float shade = level.getShade(normal, true);
        int[] lights = surface.lights(level, core);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos cell = surface.cell(core, column, row);
                if (!level.hasChunkAt(cell)) {
                    continue;
                }
                BlockState state = level.getBlockState(cell);
                boolean host = cell.equals(controllerPos);
                boolean port = state.getBlock() instanceof MachinePartBlock
                        || state.getBlock() instanceof MENetworkAccessHatchBlock;
                boolean shell = host
                        || state.is(ModBlocks.CRYSTAL_ENERGY_CASING.get())
                        || port;
                // 成型外观统一覆盖机壳和仓室的外表面。
                if (!shell || !Block.shouldRenderFace(state, level, cell, normal, cell.relative(normal))) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(cell.getX() - controllerPos.getX() + 0.5,
                        cell.getY() - controllerPos.getY() + 0.5, cell.getZ() - controllerPos.getZ() + 0.5);
                renderTile(buffer, poseStack.last(), surface, base, column, row, BASE_OFFSET, lights, overlay, shade);
                if (glow != null) {
                    renderTile(buffer, poseStack.last(), surface, glow, column, row, LIGHT_OFFSET,
                            null, overlay, 1.0F);
                }
                if (host || port) {
                    poseStack.translate(-0.5 + normal.getStepX() * LIGHT_OFFSET,
                            -0.5 + normal.getStepY() * LIGHT_OFFSET,
                            -0.5 + normal.getStepZ() * LIGHT_OFFSET);
                    BlockState overlayState = host
                            ? state.setValue(AbstractMultiblockControllerBlock.FACING, normal) : state;
                    var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(overlayState);
                    random.setSeed(overlayState.getSeed(cell));
                    int light = LevelRenderer.getLightColor(level, cell.relative(normal));
                    // 主机与仓室共用原模型 overlay，机壳底面仍使用连续成型纹理。
                    for (var quad : model.getQuads(overlayState, normal, random, ModelData.EMPTY, null)) {
                        ResourceLocation texture = quad.getSprite().contents().name();
                        if (texture.getNamespace().equals(ae2pr.MODID)
                                && (texture.getPath().startsWith("block/overlay/")
                                        || host && texture.getPath().startsWith("block/controller_front/"))) {
                            float brightness = quad.isShade() ? shade : 1.0F;
                            buffer.putBulkData(poseStack.last(), quad, brightness, brightness, brightness,
                                    light, overlay);
                        }
                    }
                }
                poseStack.popPose();
            }
        }
    }

    private static void renderTile(VertexConsumer buffer, PoseStack.Pose pose, Surface surface,
            TextureAtlasSprite sprite, int column, int row, float offset, @Nullable int[] lights,
            int overlay, float shade) {
        float u0 = sprite.getU(column * 16.0F / 3);
        float u1 = sprite.getU((column + 1) * 16.0F / 3);
        float v0 = sprite.getV(row * 16.0F / 3);
        float v1 = sprite.getV((row + 1) * 16.0F / 3);
        int topLeft = lights == null ? LightTexture.FULL_BRIGHT : lights[row * 4 + column];
        int bottomLeft = lights == null ? LightTexture.FULL_BRIGHT : lights[(row + 1) * 4 + column];
        int bottomRight = lights == null ? LightTexture.FULL_BRIGHT : lights[(row + 1) * 4 + column + 1];
        int topRight = lights == null ? LightTexture.FULL_BRIGHT : lights[row * 4 + column + 1];
        vertex(buffer, pose, surface, -0.5F, -0.5F, offset, u0, v0, topLeft, overlay, shade);
        vertex(buffer, pose, surface, -0.5F, 0.5F, offset, u0, v1, bottomLeft, overlay, shade);
        vertex(buffer, pose, surface, 0.5F, 0.5F, offset, u1, v1, bottomRight, overlay, shade);
        vertex(buffer, pose, surface, 0.5F, -0.5F, offset, u1, v0, topRight, overlay, shade);
    }

    static int[] smoothLighting(int[] samples) {
        int[] lights = new int[16];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int index = row * 5 + column;
                int a = samples[index];
                int b = samples[index + 1];
                int c = samples[index + 5];
                int d = samples[index + 6];
                int block = ((a & 0xFFFF) + (b & 0xFFFF) + (c & 0xFFFF) + (d & 0xFFFF)) / 4;
                int sky = ((a >>> 16) + (b >>> 16) + (c >>> 16) + (d >>> 16)) / 4;
                lights[row * 4 + column] = block | sky << 16;
            }
        }
        return lights;
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Surface surface, float across,
            float down, float offset, float u, float v, int light, int overlay, float shade) {
        Direction normal = surface.normal();
        Direction right = surface.right();
        Direction vertical = surface.down();
        float depth = 0.5F + offset;
        float x = normal.getStepX() * depth + right.getStepX() * across + vertical.getStepX() * down;
        float y = normal.getStepY() * depth + right.getStepY() * across + vertical.getStepY() * down;
        float z = normal.getStepZ() * depth + right.getStepZ() * across + vertical.getStepZ() * down;
        buffer.vertex(pose.pose(), x, y, z).color(shade, shade, shade, 1.0F)
                .uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), normal.getStepX(), normal.getStepY(), normal.getStepZ()).endVertex();
    }

    static Surface surface(Direction front, Direction normal) {
        boolean horizontal = normal.getAxis().isHorizontal();
        Direction right = horizontal ? normal.getCounterClockWise() : front.getCounterClockWise();
        Direction down = horizontal ? Direction.DOWN : normal == Direction.UP ? front : front.getOpposite();
        Panel panel = !horizontal ? Panel.TOP : normal == front ? Panel.FRONT
                : normal == front.getOpposite() ? Panel.BACK : Panel.SIDE;
        return new Surface(normal, right, down, panel);
    }

    record Surface(Direction normal, Direction right, Direction down, Panel panel) {

        BlockPos cell(BlockPos core, int column, int row) {
            return core.relative(normal).relative(right, column - 1).relative(down, row - 1);
        }

        int[] lights(Level level, BlockPos core) {
            int[] samples = new int[25];
            for (int row = 0; row < 5; row++) {
                for (int column = 0; column < 5; column++) {
                    BlockPos pos = cell(core, column - 1, row - 1).relative(normal);
                    samples[row * 5 + column] = LevelRenderer.getLightColor(level, pos);
                }
            }
            // 相邻贴片共享顶点光照，面内连续插值。
            return smoothLighting(samples);
        }
    }

    enum Panel {
        FRONT("front"), SIDE("side"), BACK("back"), TOP("top");

        private final ResourceLocation base;
        private final ResourceLocation idle;
        private final ResourceLocation active;
        private final ResourceLocation paused;

        Panel(String face) {
            String path = "block/multi_factory/formed_" + face;
            base = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path);
            idle = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path + "_emissive");
            active = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path + "_active_emissive");
            paused = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path + "_paused_emissive");
        }

        @Nullable
        ResourceLocation glow(boolean running, boolean isPaused) {
            if (this != FRONT) {
                return null;
            }
            return running ? active : isPaused ? paused : idle;
        }
    }
}
