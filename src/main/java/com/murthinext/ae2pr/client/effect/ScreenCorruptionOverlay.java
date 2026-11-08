package com.murthinext.ae2pr.client.effect;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.registry.ModEffects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScreenCorruptionOverlay {
    private static final ScreenCorruptionState STATE = new ScreenCorruptionState();
    private static ClientLevel level;

    private ScreenCorruptionOverlay() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            render();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATE.clear();
        level = null;
    }

    private static void render() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || !player.isAlive()) {
            STATE.clear();
            ScreenCorruptionShader.clear();
            level = null;
            return;
        }
        var effect = player.getEffect(ModEffects.SCREEN_CORRUPTION.get());
        if (effect == null) {
            STATE.clear();
            ScreenCorruptionShader.clear();
            return;
        }
        if (level != minecraft.level) {
            STATE.clear();
            ScreenCorruptionShader.clear();
            level = minecraft.level;
        }
        STATE.update(player.getUUID(), effect.getAmplifier() + 1, System.nanoTime() * 1.0e-9);
        if (minecraft.getOverlay() != null) {
            return;
        }

        var graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        var previousShader = RenderSystem.getShader();
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();
        graphics.flush();
        modelView.pushPose();
        try {
            float farPlane = ForgeHooksClient.getGuiFarPlane();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, 1000, farPlane),
                    VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity();
            modelView.translate(0, 0, 1000 - farPlane);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableScissor();
            ScreenCorruptionShader.render(graphics, STATE.regions(), width, height, STATE.time());
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.depthMask(depthMask);
            if (scissor) {
                GlStateManager._enableScissorTest();
            }
            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
        }
    }
}
