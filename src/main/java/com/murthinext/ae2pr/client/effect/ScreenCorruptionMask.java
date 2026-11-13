package com.murthinext.ae2pr.client.effect;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

final class ScreenCorruptionMask implements AutoCloseable {
    private TextureTarget target;
    private List<ScreenCorruptionState.Region> regions = List.of();
    private boolean expanding;

    int update(GuiGraphics graphics, List<ScreenCorruptionState.Region> regions, int width, int height, double time) {
        boolean resized = target == null || target.width != width || target.height != height;
        if (!resized && !expanding && this.regions.equals(regions)) {
            return target.getColorTextureId();
        }

        int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        float[] color = RenderSystem.getShaderColor().clone();
        try (var stack = MemoryStack.stackPush()) {
            var viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            try {
                if (resized) {
                    if (target != null) {
                        target.destroyBuffers();
                    }
                    target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
                    target.setClearColor(0, 0, 0, 0);
                }
                RenderSystem.disableDepthTest();
                RenderSystem.setShaderColor(1, 1, 1, 1);
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                var matrix = graphics.pose().last().pose();
                var buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                expanding = false;
                for (var region : regions) {
                    double scale = region.scale(time);
                    expanding |= scale < 1;
                    if (scale <= 0) {
                        continue;
                    }
                    float halfWidth = (float) (region.width() * width * scale * 0.5);
                    float halfHeight = (float) (region.height() * height * scale * 0.5);
                    float centerX = (float) ((region.x() + region.width() * 0.5) * width);
                    float centerY = (float) ((region.y() + region.height() * 0.5) * height);
                    buffer.vertex(matrix, centerX - halfWidth, centerY - halfHeight, 0)
                            .color(255, 255, 255, 255)
                            .endVertex();
                    buffer.vertex(matrix, centerX - halfWidth, centerY + halfHeight, 0)
                            .color(255, 255, 255, 255)
                            .endVertex();
                    buffer.vertex(matrix, centerX + halfWidth, centerY + halfHeight, 0)
                            .color(255, 255, 255, 255)
                            .endVertex();
                    buffer.vertex(matrix, centerX + halfWidth, centerY - halfHeight, 0)
                            .color(255, 255, 255, 255)
                            .endVertex();
                }
                BufferUploader.drawWithShader(buffer.end());
                this.regions = List.copyOf(regions);
            } finally {
                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            }
        }
        return target.getColorTextureId();
    }

    @Override
    public void close() {
        if (target != null) {
            int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            target.destroyBuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            target = null;
        }
        regions = List.of();
        expanding = false;
    }
}
