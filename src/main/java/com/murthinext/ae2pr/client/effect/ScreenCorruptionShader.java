package com.murthinext.ae2pr.client.effect;

import java.io.IOException;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ScreenCorruptionShader {
    private static final ScreenCorruptionMask MASK = new ScreenCorruptionMask();
    private static ShaderInstance shader;

    private ScreenCorruptionShader() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Ids.of("screen_corruption"),
                DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
    }

    static void clear() {
        MASK.close();
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
