package com.murthinext.ae2pr.item.proto_terminal.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ShapeRenderer extends RenderStateShard {

    private static final VertexFormat FORMAT = new VertexFormat(ImmutableMap.of(
            "Position", DefaultVertexFormat.ELEMENT_POSITION,
            "Color", DefaultVertexFormat.ELEMENT_COLOR,
            "ShapePoint", DefaultVertexFormat.ELEMENT_UV0,
            "ShapeSize", DefaultVertexFormat.ELEMENT_UV0,
            "BorderColor", DefaultVertexFormat.ELEMENT_COLOR));
    private static ShaderInstance shader;
    private static final RenderType SHAPES = RenderType.create(
            "ae2pr_terminal_shape",
            FORMAT,
            VertexFormat.Mode.QUADS,
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> shader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    private ShapeRenderer() {
        super("ae2pr_terminal_shape", () -> {}, () -> {});
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), Ids.of("terminal_shape"), FORMAT),
                loaded -> shader = loaded);
    }

    public static boolean isReady() {
        return shader != null;
    }

    public static VertexConsumer buffer(GuiGraphics graphics) {
        return graphics.bufferSource().getBuffer(SHAPES);
    }

    public static float pixelSize(Matrix4f pose) {
        return 1 / (float) (pose.m00() * Minecraft.getInstance().getWindow().getGuiScale());
    }

    public static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color, float width) {
        float length = (float) Math.hypot(x2 - x1, y2 - y1);
        if (length <= 0 || width <= 0 || color >>> 24 == 0) {
            return;
        }
        float dx = (x2 - x1) / length;
        float dy = (y2 - y1) / length;
        float halfWidth = width / 2;
        rectangle(graphics, x1 + dy * halfWidth, y1 - dx * halfWidth, dx, dy, -dy, dx, length, width, color, 0);
    }

    public static void panel(
            GuiGraphics graphics, float x, float y, float width, float height, float slant, int fill, int border) {
        rectangle(graphics, x + slant, y, 1, 0, -slant / height, 1, width - slant, height, fill, border);
    }

    private static void rectangle(
            GuiGraphics graphics,
            float x,
            float y,
            float ux,
            float uy,
            float vx,
            float vy,
            float width,
            float height,
            int fill,
            int border) {
        Matrix4f pose = graphics.pose().last().pose();
        float pixel = pixelSize(pose);
        // 扩大几何范围，给中心落在轮廓外的像素也留出覆盖率采样空间。
        float padU = pixel * (Math.abs(vx) + Math.abs(vy));
        float padV = pixel * (Math.abs(ux) + Math.abs(uy));
        float left = -padU;
        float right = width + padU;
        float top = -padV;
        float bottom = height + padV;
        VertexConsumer vertices = buffer(graphics);
        vertex(
                vertices,
                pose,
                x + ux * left + vx * top,
                y + uy * left + vy * top,
                left,
                top,
                width,
                height,
                fill,
                border);
        vertex(
                vertices,
                pose,
                x + ux * left + vx * bottom,
                y + uy * left + vy * bottom,
                left,
                bottom,
                width,
                height,
                fill,
                border);
        vertex(
                vertices,
                pose,
                x + ux * right + vx * bottom,
                y + uy * right + vy * bottom,
                right,
                bottom,
                width,
                height,
                fill,
                border);
        vertex(
                vertices,
                pose,
                x + ux * right + vx * top,
                y + uy * right + vy * top,
                right,
                top,
                width,
                height,
                fill,
                border);
    }

    public static void vertex(
            VertexConsumer vertices,
            Matrix4f pose,
            float x,
            float y,
            float u,
            float v,
            float width,
            float height,
            int fill,
            int border) {
        vertices.vertex(pose, x, y, 0)
                .color(fill)
                .uv(u, v)
                .uv(width, height)
                .color(border)
                .endVertex();
    }
}
