package com.murthinext.ae2pr.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** 在 JEI 界面中以正交视角绘制流体方块。 */
public final class FluidBlockRendering {

    private FluidBlockRendering() {
    }

    public static void render(GuiGraphics graphics, Fluid fluid, int x, int y, int width, int height) {
        FluidState fluidState = fluid.defaultFluidState();
        var renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
        renderType.setupRenderState();
        RenderSystem.disableDepthTest();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.mulPoseMatrix(graphics.pose().last().pose());
        modelViewStack.translate(x, y, 0);
        FogRenderer.setupNoFog();
        modelViewStack.translate(width / 2.0f, height / 2.0f, 0);
        modelViewStack.scale(width, height, 1);
        setupOrthographicProjection(modelViewStack);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(renderType.mode(), renderType.format());
        Minecraft.getInstance().getBlockRenderer().renderLiquid(BlockPos.ZERO,
                new FakeWorld(fluidState), builder, fluidState.createLegacyBlock(), fluidState);
        if (builder.building()) {
            tesselator.end();
        }

        renderType.clearRenderState();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void setupOrthographicProjection(PoseStack modelViewStack) {
        float angle = 36;
        float rotation = 45;
        modelViewStack.scale(1, 1, -1);
        modelViewStack.mulPose(new Quaternionf().rotationY(Mth.DEG_TO_RAD * -180));
        Quaternionf flip = new Quaternionf().rotationZ(Mth.DEG_TO_RAD * 180);
        flip.mul(new Quaternionf().rotationX(Mth.DEG_TO_RAD * angle));
        Quaternionf rotate = new Quaternionf().rotationY(Mth.DEG_TO_RAD * rotation);
        modelViewStack.mulPose(flip);
        modelViewStack.mulPose(rotate);
        modelViewStack.translate(-0.5f, -0.5f, -0.5f);
        RenderSystem.applyModelViewMatrix();
    }

    private static final class FakeWorld implements BlockAndTintGetter {

        private final FluidState fluidState;

        private FakeWorld(FluidState fluidState) {
            this.fluidState = fluidState;
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return 1.0f;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return 15;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int ambientDarkness) {
            return 15;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
            var level = Minecraft.getInstance().level;
            return level == null ? -1 : colorResolver.getColor(level.getBiome(pos).value(), 0, 0);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return BlockPos.ZERO.equals(pos) ? fluidState.createLegacyBlock() : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return BlockPos.ZERO.equals(pos) ? fluidState : Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public int getMinBuildHeight() {
            return 0;
        }
    }
}
