package com.murthinext.ae2pr.integration.jei;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.gui.drawable.IDrawable;

public final class AnimatedTextureDrawable implements IDrawable {

    private final ResourceLocation texture;
    private final int width;
    private final int height;
    private final int frameCount;
    private final int frameTimeMs;

    public AnimatedTextureDrawable(ResourceLocation texture, int width, int height, int frameCount, int frameTimeMs) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.frameCount = frameCount;
        this.frameTimeMs = frameTimeMs;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        int frame = (int) ((Util.getMillis() / frameTimeMs) % frameCount);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, xOffset, yOffset, 0.0F, frame * height, width, height, width, height * frameCount);
        RenderSystem.disableBlend();
    }
}
