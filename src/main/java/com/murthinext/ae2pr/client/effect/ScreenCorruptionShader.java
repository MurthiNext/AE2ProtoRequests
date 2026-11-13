package com.murthinext.ae2pr.client.effect;

import java.io.IOException;
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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ScreenCorruptionShader {
    private static final ScreenCorruptionMask MASK = new ScreenCorruptionMask();
    private static ShaderInstance shader;
    private static ShaderInstance impactShader;
    private static TextureTarget scene;

    private ScreenCorruptionShader() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        clear();
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Ids.of("screen_corruption"),
                                                DefaultVertexFormat.POSITION_TEX),
                             loaded -> shader = loaded);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Ids.of("screen_corruption_impact"),
                                                DefaultVertexFormat.POSITION_TEX),
                             loaded -> impactShader = loaded);
    }

    static void clear() {
        MASK.close();
        releaseScene();
    }

    static void render(GuiGraphics graphics, List<ScreenCorruptionState.Region> regions, int screenWidth,
                       int screenHeight, double time) {
        if (shader == null || regions.isEmpty() || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        shader.setSampler("RegionMask", MASK.update(graphics, regions, screenWidth, screenHeight, time));
        shader.safeGetUniform("GuiSize").set((float) screenWidth, (float) screenHeight);
        shader.safeGetUniform("Time").set((float) time);
        shader.safeGetUniform("Seed").set(regions.get(0).seed());
        RenderSystem.setShader(() -> shader);
        draw(graphics, screenWidth, screenHeight);
    }

    static void renderImpact(GuiGraphics graphics, int screenWidth, int screenHeight, float progress, float strength,
                             float seed) {
        if (impactShader == null || strength <= 0 || progress >= 1 || screenWidth <= 0 || screenHeight <= 0) {
            releaseScene();
            return;
        }

        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try (var stack = MemoryStack.stackPush()) {
            var viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int width = viewport.get(2);
            int height = viewport.get(3);
            if (width <= 0 || height <= 0) {
                return;
            }
            try {
                if (scene == null || scene.width != width || scene.height != height) {
                    releaseScene();
                    scene = new TextureTarget(width, height, false, Minecraft.ON_OSX);
                    scene.setFilterMode(GL11.GL_LINEAR);
                }
                // 先复制当前画面，避免从正在写入的颜色附件采样。
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, drawFramebuffer);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
                GlStateManager._glBlitFrameBuffer(viewport.get(0), viewport.get(1), viewport.get(0) + width,
                                                  viewport.get(1) + height, 0, 0, width, height,
                                                  GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            } finally {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
                RenderSystem.viewport(viewport.get(0), viewport.get(1), width, height);
                RenderSystem.disableDepthTest();
            }
        }

        impactShader.setSampler("Scene", scene.getColorTextureId());
        impactShader.safeGetUniform("ScreenSize").set((float) scene.width, (float) scene.height);
        impactShader.safeGetUniform("Progress").set(progress);
        impactShader.safeGetUniform("Strength").set(strength);
        impactShader.safeGetUniform("Seed").set(seed);
        RenderSystem.setShader(() -> impactShader);
        draw(graphics, screenWidth, screenHeight);
    }

    private static void releaseScene() {
        if (scene == null) {
            return;
        }
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        scene.destroyBuffers();
        scene = null;
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
    }

    private static void draw(GuiGraphics graphics, int screenWidth, int screenHeight) {
        var matrix = graphics.pose().last().pose();
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, 0, 0, 0).uv(0, 0).endVertex();
        buffer.vertex(matrix, 0, screenHeight, 0).uv(0, 1).endVertex();
        buffer.vertex(matrix, screenWidth, screenHeight, 0).uv(1, 1).endVertex();
        buffer.vertex(matrix, screenWidth, 0, 0).uv(1, 0).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }
}
