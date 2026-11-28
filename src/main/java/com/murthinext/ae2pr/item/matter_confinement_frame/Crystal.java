package com.murthinext.ae2pr.item.matter_confinement_frame;

import appeng.api.util.AEColor;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.client.model.IQuadTransformer;
import org.joml.Vector3d;

public final class Crystal {

    private final double radius;
    private final double spinSpeed;
    private final int animationFps;
    private final long animationStart = Util.getMillis();
    private final ColorPattern pattern;
    private final CrystalMaterial material;
    private final List<Facet> facets;

    public Crystal(JsonObject settings) {
        radius = Mth.clamp(GsonHelper.getAsDouble(settings, "radius"), 0.5, 4.0) / 16;
        spinSpeed = Math.toRadians(GsonHelper.getAsDouble(settings, "spin_degrees_per_second"));
        animationFps = Mth.clamp(GsonHelper.getAsInt(settings, "animation_fps"), 1, 120);
        pattern = new ColorPattern(settings);
        material = new CrystalMaterial(GsonHelper.getAsJsonObject(settings, "material"));
        int sides = Mth.clamp(GsonHelper.getAsInt(settings, "sides"), 3, 16);
        double rotation = Math.toRadians(GsonHelper.getAsDouble(settings, "rotation"));
        double tableHeight = GsonHelper.getAsDouble(settings, "table_height");
        var center = new Vector3d(0, tableHeight, 0);
        var table =
                ring(sides, rotation + Math.PI / sides, GsonHelper.getAsDouble(settings, "table_radius"), tableHeight);
        var shoulder = ring(sides, rotation, 1, GsonHelper.getAsDouble(settings, "shoulder_height"));
        var cut = new ArrayList<Facet>();
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            addMirroredPair(cut, center, table[next], table[i]);
            addMirroredPair(cut, shoulder[i], table[i], shoulder[next]);
            addMirroredPair(cut, shoulder[next], table[i], table[next]);
            cut.add(new Facet(shoulder[i], shoulder[next], mirrorY(shoulder[next]), mirrorY(shoulder[i])));
        }
        facets = List.copyOf(cut);
    }

    private static void addMirroredPair(List<Facet> cut, Vector3d a, Vector3d b, Vector3d c) {
        cut.add(new Facet(a, b, c));
        cut.add(new Facet(mirrorY(c), mirrorY(b), mirrorY(a)));
    }

    private static Vector3d mirrorY(Vector3d point) {
        return new Vector3d(point.x, -point.y, point.z);
    }

    private static Vector3d[] ring(int sides, double rotation, double width, double height) {
        var points = new Vector3d[sides];
        for (int i = 0; i < sides; i++) {
            double angle = rotation + 2 * Math.PI * i / sides;
            points[i] = new Vector3d(width * Math.cos(angle), height, width * Math.sin(angle));
        }
        return points;
    }

    public Surface createSurface(Map<AEColor, Integer> contents, TextureAtlasSprite sprite) {
        return new Surface(pattern.createPalette(contents), sprite);
    }

    private static final class Facet {

        private final Vector3d[] corners;
        private final Vector3d normal;

        private Facet(Vector3d... corners) {
            this.corners = corners;
            normal = new Vector3d(corners[1])
                    .sub(corners[0])
                    .cross(new Vector3d(corners[2]).sub(corners[0]))
                    .normalize();
        }

        private Vector3d corner(int i) {
            // 三角切面复用末顶点，以四边形格式提交。
            return corners[Math.min(i, corners.length - 1)];
        }
    }

    private final class CrystalQuad extends BakedQuad {

        private final Facet facet;
        private final double[] pigment = new double[3];
        private final Vector3d rotatedNormal = new Vector3d();
        private Direction facing;

        private CrystalQuad(Facet facet, TextureAtlasSprite sprite) {
            super(new int[4 * IQuadTransformer.STRIDE], -1, Direction.UP, sprite, false, false);
            this.facet = facet;
            for (int i = 0; i < 4; i++) {
                int offset = i * IQuadTransformer.STRIDE;
                vertices[offset + IQuadTransformer.POSITION + 1] = coordinate(facet.corner(i).y);
                vertices[offset + IQuadTransformer.UV0] = Float.floatToRawIntBits(sprite.getU(8));
                vertices[offset + IQuadTransformer.UV0 + 1] = Float.floatToRawIntBits(sprite.getV(8));
                vertices[offset + IQuadTransformer.UV2] = LightTexture.FULL_BRIGHT;
            }
        }

        private int coordinate(double value) {
            return Float.floatToRawIntBits((float) (0.5 + radius * value));
        }

        private void sample(ColorPattern.Palette palette, double seconds) {
            var n = facet.normal;
            palette.sample(n.x, n.y, n.z, seconds, pigment);
        }

        private void rotate(double sin, double cos) {
            var n = facet.normal;
            rotatedNormal.set(n.x * cos + n.z * sin, n.y, n.z * cos - n.x * sin);
            facing = Direction.getNearest(rotatedNormal.x, rotatedNormal.y, rotatedNormal.z);
            int normal = ((int) Math.round(rotatedNormal.x * 127) & 255)
                    | ((int) Math.round(rotatedNormal.y * 127) & 255) << 8
                    | ((int) Math.round(rotatedNormal.z * 127) & 255) << 16;
            int color = material.shade(rotatedNormal).color(pigment);
            for (int i = 0; i < 4; i++) {
                var p = facet.corner(i);
                int offset = i * IQuadTransformer.STRIDE;
                vertices[offset + IQuadTransformer.POSITION] = coordinate(p.x * cos + p.z * sin);
                vertices[offset + IQuadTransformer.POSITION + 2] = coordinate(p.z * cos - p.x * sin);
                vertices[offset + IQuadTransformer.NORMAL] = normal;
                vertices[offset + IQuadTransformer.COLOR] = color;
            }
        }

        @Override
        public Direction getDirection() {
            return facing;
        }
    }

    public final class Surface {

        private final ColorPattern.Palette palette;
        private final List<CrystalQuad> quads;
        private long lastFrame = -1;

        private Surface(ColorPattern.Palette palette, TextureAtlasSprite sprite) {
            this.palette = palette;
            quads = facets.stream().map(facet -> new CrystalQuad(facet, sprite)).toList();
            update();
        }

        public List<BakedQuad> quads() {
            return List.copyOf(quads);
        }

        public void update() {
            long frame = (Util.getMillis() - animationStart) * animationFps / 1000;
            if (spinSpeed == 0 && !palette.isAnimated()) {
                frame = 0;
            }
            if (frame == lastFrame) {
                return;
            }
            double seconds = (double) frame / animationFps;
            double sin = Math.sin(seconds * spinSpeed);
            double cos = Math.cos(seconds * spinSpeed);
            boolean sample = lastFrame < 0 || palette.isAnimated();
            for (var quad : quads) {
                if (sample) {
                    quad.sample(palette, seconds);
                }
                quad.rotate(sin, cos);
            }
            lastFrame = frame;
        }
    }
}
