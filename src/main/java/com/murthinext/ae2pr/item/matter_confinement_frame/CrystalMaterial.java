package com.murthinext.ae2pr.item.matter_confinement_frame;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.joml.Vector3d;

final class CrystalMaterial {

    private final Vector3d light;
    private final Vector3d halfway;
    private final Vector3d reflectionTint;
    private final double ambient;
    private final double diffuse;
    private final double opacity;
    private final double edgeOpacity;
    private final double specular;
    private final double specularPower;
    private final double rimStrength;

    public CrystalMaterial(JsonObject settings) {
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
        opacity = GsonHelper.getAsDouble(settings, "opacity");
        edgeOpacity = GsonHelper.getAsDouble(settings, "edge_opacity");
        specular = GsonHelper.getAsDouble(settings, "specular");
        double roughness = Mth.clamp(GsonHelper.getAsDouble(settings, "roughness"), 0.1, 1);
        specularPower = 2 / (roughness * roughness) - 2;
        rimStrength = GsonHelper.getAsDouble(settings, "rim_strength");
    }

    public Lighting shade(Vector3d normal) {
        double edge = 1 - Math.abs(normal.z);
        double body = ambient + diffuse * Math.max(0, light.dot(normal));
        double highlight = specular * Math.pow(Math.max(0, halfway.dot(normal)), specularPower);
        double rim = rimStrength * edge * edge;
        double alpha = Mth.lerp(edge * edge, opacity, edgeOpacity) + highlight * (1 - edgeOpacity);
        return new Lighting(
                (int) Math.round(Mth.clamp(alpha, 0, 1) * 255),
                body * (1 - Math.min(0.9, highlight + rim)),
                highlight + rim * reflectionTint.x,
                highlight + rim * reflectionTint.y,
                highlight + rim * reflectionTint.z);
    }

    public record Lighting(int alpha, double body, double redReflection, double greenReflection, double blueReflection) {

        public int color(double[] pigment) {
            int red = toSrgb(pigment[0] * body + redReflection);
            int green = toSrgb(pigment[1] * body + greenReflection);
            int blue = toSrgb(pigment[2] * body + blueReflection);
            return alpha << 24 | blue << 16 | green << 8 | red;
        }

        private static int toSrgb(double linear) {
            double value = linear <= 0.0031308 ? 12.92 * linear : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
            return (int) Math.round(Mth.clamp(value, 0, 1) * 255);
        }
    }
}
