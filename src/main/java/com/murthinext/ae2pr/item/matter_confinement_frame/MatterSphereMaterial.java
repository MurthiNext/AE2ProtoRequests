package com.murthinext.ae2pr.item.matter_confinement_frame;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.joml.Vector3d;

final class MatterSphereMaterial {

    private final Vector3d light;
    private final Vector3d halfway;
    private final Vector3d reflectionTint;
    private final double ambient;
    private final double diffuse;
    private final double internalGlow;
    private final double edgeDepth;
    private final double specular;
    private final double specularPower;
    private final double softReflection;
    private final double rimStrength;

    MatterSphereMaterial(JsonObject settings) {
        var direction = settings.getAsJsonArray("light_direction");
        light = new Vector3d(
                        direction.get(0).getAsDouble(),
                        direction.get(1).getAsDouble(),
                        direction.get(2).getAsDouble())
                .normalize();
        halfway = new Vector3d(light).add(0, 0, 1).normalize();
        var tint = settings.getAsJsonArray("reflection_tint");
        reflectionTint = new Vector3d(
                tint.get(0).getAsDouble(),
                tint.get(1).getAsDouble(),
                tint.get(2).getAsDouble());
        ambient = GsonHelper.getAsDouble(settings, "ambient");
        diffuse = GsonHelper.getAsDouble(settings, "diffuse");
        internalGlow = GsonHelper.getAsDouble(settings, "internal_glow");
        edgeDepth = GsonHelper.getAsDouble(settings, "edge_depth");
        specular = GsonHelper.getAsDouble(settings, "specular");
        double roughness = Mth.clamp(GsonHelper.getAsDouble(settings, "roughness"), 0.1, 1);
        specularPower = 2 / (roughness * roughness) - 2;
        softReflection = GsonHelper.getAsDouble(settings, "soft_reflection");
        rimStrength = GsonHelper.getAsDouble(settings, "rim_strength");
    }

    Lighting shade(double x, double y, double z) {
        double facing = Math.abs(z);
        double illumination = Math.max(0, light.dot(x, y, facing));
        double reflection = Math.max(0, halfway.dot(x, y, facing));
        double edge = 1 - facing;
        double body = (ambient + diffuse * illumination) * (1 - edgeDepth * edge * edge)
                + internalGlow * facing * facing * (1 - 0.5 * illumination);
        double highlight = specular * Math.pow(reflection, specularPower)
                + softReflection * Math.pow(reflection, specularPower * 0.25);
        double rim = rimStrength * Math.pow(edge, 5) * (0.35 + 0.65 * Math.max(0, -light.x * x - light.y * y));
        return new Lighting(
                body * (1 - Math.min(0.9, highlight + rim)),
                highlight + rim * reflectionTint.x,
                highlight + rim * reflectionTint.y,
                highlight + rim * reflectionTint.z);
    }

    record Lighting(double body, double redReflection, double greenReflection, double blueReflection) {

        int color(double[] pigment) {
            int red = toSrgb(pigment[0] * body + redReflection);
            int green = toSrgb(pigment[1] * body + greenReflection);
            int blue = toSrgb(pigment[2] * body + blueReflection);
            return 0xFF000000 | blue << 16 | green << 8 | red;
        }

        private static int toSrgb(double linear) {
            double value = linear <= 0.0031308 ? 12.92 * linear : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
            return (int) Math.round(Mth.clamp(value, 0, 1) * 255);
        }
    }
}
