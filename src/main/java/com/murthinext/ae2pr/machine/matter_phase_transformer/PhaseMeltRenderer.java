package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.machine.matter_phase_transformer.PhaseChamberMesh.Edge;
import com.murthinext.ae2pr.machine.matter_phase_transformer.PhaseChamberMesh.Rim;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

final class PhaseMeltRenderer {

    private static final RenderType CRUST = RenderType.entitySolid(Ids.of("textures/vfx/matter_crust.png"));
    private static final RenderType MELT_OVERLAY =
            RenderType.beaconBeam(Ids.of("textures/vfx/matter_melt_overlay.png"), true);
    private static final float OVERLAY_OUTSET = 0.001F;
    private static final RenderType LIQUID = RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
    private static final ResourceLocation UNTINTED_LIQUID = Ids.of("block/material_sets/metallic/liquid");
    private static final float HEIGHT = 2.6F;
    private static final float TIP_Y = 0.5F - HEIGHT / 2;
    private static final float CROWN_Y = TIP_Y + HEIGHT;
    static final Vec3 IMPACT = new Vec3(0, CROWN_Y, 0);
    private static final Section[] CRYSTAL = {
        new Section(TIP_Y, 0, 0.42F),
        new Section(-0.38F, 0.37F, 0.22F),
        new Section(1.16F, 0.455F, 0.82F),
        new Section(CROWN_Y, 0, 1)
    };
    static final float FLOOR = MatterPhaseTransformerStructure.COLLECTOR_Y + 0.007F;
    private static final int INCANDESCENT = 0xFFFFBD5A;
    private static final int WHITE = 0xFFFFFFFF;
    private static final Rim[] BASE =
            Arrays.stream(CRYSTAL).map(section -> section.rim(WHITE)).toArray(Rim[]::new);
    private static final int SURFACE_TICKS = 20;
    private static final int FALL_TICKS = 16;
    private static final int FLOW_BEAT = 15;

    private record Section(float y, float radius, float heatWeight) {
        Rim rim(int color) {
            return new Rim(y, radius, color);
        }
    }

    private PhaseMeltRenderer() {}

    static void crystal(PoseStack pose, MultiBufferSource buffers, float heat) {
        new PhaseChamberMesh(pose.last(), buffers.getBuffer(CRUST)).crystal(BASE);
        if (heat <= 0) {
            return;
        }
        Rim[] overlay = new Rim[CRYSTAL.length];
        for (int i = 0; i < CRYSTAL.length; i++) {
            Section section = CRYSTAL[i];
            int alpha = Math.round(255 * Math.max(0, section.heatWeight - (1 - heat)));
            overlay[i] = section.rim(alpha << 24 | 0xFFFFFF);
        }
        new PhaseChamberMesh(pose.last(), buffers.getBuffer(MELT_OVERLAY), OVERLAY_OUTSET).crystal(overlay);
    }

    static void pour(PoseStack pose, MultiBufferSource buffers, FluidStack product, double time, float heat) {
        if (time <= 0 || heat <= 0) {
            return;
        }
        ResourceLocation texture = UNTINTED_LIQUID;
        int tint = INCANDESCENT;
        if (!product.isEmpty()) {
            var fluid = IClientFluidTypeExtensions.of(product.getFluid());
            var still = fluid.getStillTexture(product);
            if (still != null) {
                texture = still;
            }
            tint = fluid.getTintColor(product);
        }
        var sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        var mesh = new PhaseChamberMesh(pose.last(), sprite.wrap(buffers.getBuffer(LIQUID)));
        float flow = heat * (float) Math.min(1, time / 7);
        int hot = ARGB32.lerp(0.12F, INCANDESCENT, tint);
        int outlet = ARGB32.lerp(0.32F, hot, tint);
        wetFaces(mesh, time, flow, hot, outlet);
        double sinceTip = time - SURFACE_TICKS;
        if (sinceTip <= 0) {
            return;
        }
        double sinceLanding = sinceTip - FALL_TICKS;
        float fill = heat * (float) Mth.clamp(sinceLanding / 20, 0, 1);
        float landingY = fill > 0 ? pool(mesh, fill, (float) Mth.frac(sinceLanding / FLOW_BEAT), tint) : FLOOR;
        stream(mesh, sinceTip, flow, landingY, outlet, tint);
    }

    private static void wetFaces(PhaseChamberMesh mesh, double time, float flow, int hot, int outlet) {
        float front = Mth.lerp((float) Math.min(1, time / SURFACE_TICKS), CROWN_Y, TIP_Y);
        for (int face = 0; face < PhaseChamberMesh.CRYSTAL_FACES; face += 2) {
            Edge previous = filmEdge(face, CROWN_Y, 0, time, flow, hot, outlet);
            float v = 0;
            for (int i = CRYSTAL.length - 1; i > 0 && CRYSTAL[i].y > front; i--) {
                Section upper = CRYSTAL[i];
                Section lower = CRYSTAL[i - 1];
                int rows = Mth.ceil((upper.y - lower.y) * 5);
                for (int row = 1; row <= rows; row++) {
                    float y = Math.max(front, Mth.lerp((float) row / rows, upper.y, lower.y));
                    float radius = Mth.lerp((upper.y - y) / (upper.y - lower.y), upper.radius, lower.radius);
                    Edge next = filmEdge(face, y, radius, time, flow, hot, outlet);
                    v = mesh.film(face, previous, next, v);
                    previous = next;
                    if (y <= front) {
                        break;
                    }
                }
            }
        }
    }

    private static Edge filmEdge(int face, float y, float radius, double time, float flow, int hot, int outlet) {
        float depth = (CROWN_Y - y) / HEIGHT;
        float glint = pulse(time - depth * SURFACE_TICKS);
        double center = 0.5 + 0.14 * Math.sin(7 * depth + face);
        double width = (0.10 + 0.04 * glint + 0.035 * (1 - depth)) * flow;
        float skin = radius * 1.022F;
        int color = ARGB32.lerp(0.18F * glint, ARGB32.lerp(depth, hot, outlet), WHITE);
        int alpha = Math.round(ARGB32.alpha(color) * flow * (0.69F + 0.19F * glint));
        return new Edge(
                PhaseChamberMesh.onCrystalFace(face, y, skin, center - width),
                PhaseChamberMesh.onCrystalFace(face, y, skin, center + width),
                alpha << 24 | (color & 0xFFFFFF));
    }

    private static void stream(PhaseChamberMesh mesh, double time, float flow, float surface, int outlet, int tint) {
        float front = (float) Math.min(1, time / FALL_TICKS);
        float width = flow * (float) Math.min(1, time / 3);
        int rows = Mth.ceil(front * 10);
        Rim[] profile = new Rim[rows + 3];
        for (int i = 0; i <= rows; i++) {
            float p = Math.min(front, i / 10F);
            float fall = p * (0.28F + 0.72F * p);
            float glint = pulse(time - p * FALL_TICKS);
            float radius =
                    width * (0.057F - 0.029F * fall + 0.01F * glint + 0.049F * Mth.clamp((fall - 0.86F) / 0.14F, 0, 1));
            int color = ARGB32.lerp(0.18F * glint, ARGB32.lerp(fall, outlet, tint), WHITE);
            profile[rows + 1 - i] = new Rim(Mth.lerp(fall, TIP_Y, surface), radius, color);
        }
        Rim head = profile[1];
        profile[0] = new Rim(head.y() - 0.035F * width * (1 - front), 0, head.color());
        profile[rows + 2] = new Rim(TIP_Y + 0.054F, 0.045F * width, outlet);
        mesh.body(profile);
    }

    private static float pulse(double age) {
        float wave = (float) (0.5 + 0.5 * Math.cos(age * (2 * Math.PI / FLOW_BEAT)));
        return wave * wave * wave;
    }

    private static float pool(PhaseChamberMesh mesh, float fill, float wave, int tint) {
        float surface = FLOOR + 0.17F * fill;
        float radius = 0.43F * (float) Math.sqrt(fill);
        float crest = (0.14F + 0.66F * wave) * radius;
        float lift = 0.05F * (1 - wave) * fill;
        float landingY = surface - lift * 0.4F;
        int reflection = ARGB32.lerp(0.46F * (1 - wave), tint, WHITE);
        Rim lip = new Rim(surface, radius * 0.93F, tint);
        mesh.body(new Rim(FLOOR, radius * 0.88F, tint), new Rim(surface - 0.035F * fill, radius, tint), lip);
        mesh.pool(
                lip,
                new Rim(surface, crest + 0.09F * radius, tint),
                new Rim(surface + lift, crest, reflection),
                new Rim(surface, crest - 0.09F * radius, tint),
                new Rim(landingY, 0, tint));
        return landingY;
    }
}
