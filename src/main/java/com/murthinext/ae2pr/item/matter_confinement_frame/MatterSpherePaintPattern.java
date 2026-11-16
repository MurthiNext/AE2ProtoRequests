package com.murthinext.ae2pr.item.matter_confinement_frame;

import appeng.api.util.AEColor;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

final class MatterSpherePaintPattern {

    private final Map<AEColor, KeyColor> colors = new EnumMap<>(AEColor.class);
    private final int keyColorCount;
    private final double flowSpeed;

    MatterSpherePaintPattern(JsonObject settings) {
        keyColorCount = Mth.clamp(GsonHelper.getAsInt(settings, "key_color_count"), 1, 3);
        flowSpeed = Mth.clamp(GsonHelper.getAsDouble(settings, "flow_speed"), 0, 2.0);
        var palette = GsonHelper.getAsJsonObject(settings, "palette");
        for (var color : AEColor.VALID_COLORS) {
            int rgb = Integer.parseInt(GsonHelper.getAsString(palette, color.registryPrefix), 16);
            double red = toLinear(rgb >> 16 & 255);
            double green = toLinear(rgb >> 8 & 255);
            double blue = toLinear(rgb & 255);
            double l = Math.cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue);
            double m = Math.cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue);
            double s = Math.cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue);
            double lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
            double a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
            double b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
            colors.put(color, new KeyColor(color, lightness, Math.hypot(a, b), Math.atan2(b, a)));
        }
    }

    Palette createPalette(Map<AEColor, Integer> contents) {
        var selected = new ArrayList<>(colors.values().stream()
                .filter(color -> contents.getOrDefault(color.color(), 0) > 0)
                .sorted(Comparator.<KeyColor>comparingInt(color -> contents.get(color.color()))
                        .reversed()
                        .thenComparing(
                                Comparator.comparingDouble(KeyColor::chroma).reversed())
                        .thenComparingInt(color -> color.color().ordinal()))
                .limit(keyColorCount)
                .toList());
        selected.sort(Comparator.comparingDouble(KeyColor::hue));
        double largestGap = -1;
        int first = 0;
        for (int index = 0; index < selected.size(); index++) {
            double nextHue = index + 1 < selected.size()
                    ? selected.get(index + 1).hue()
                    : selected.get(0).hue() + 2 * Math.PI;
            double gap = nextHue - selected.get(index).hue();
            if (gap > largestGap) {
                largestGap = gap;
                first = (index + 1) % selected.size();
            }
        }
        Collections.rotate(selected, -first);
        var stops = new ArrayList<Stop>();
        double accumulated = 0;
        for (var color : selected) {
            double count = contents.get(color.color());
            stops.add(new Stop(accumulated + count * 0.5, color));
            accumulated += count;
        }
        return new Palette(List.copyOf(stops));
    }

    final class Palette {

        private final List<Stop> stops;
        private final double start;
        private final double span;

        private Palette(List<Stop> stops) {
            this.stops = stops;
            start = stops.get(0).position();
            span = stops.get(stops.size() - 1).position() - start;
        }

        void sample(double x, double y, double z, double seconds, double[] result) {
            double time = seconds * flowSpeed;
            double axisY = 0.35 + 0.15 * Math.sin(time * 0.4);
            double horizontal = Math.sqrt(1 - axisY * axisY);
            double angle = 0.2 + time * 0.35;
            double projection = horizontal * (x * Math.cos(angle) + z * Math.sin(angle)) + y * axisY;
            double position = start + Mth.clamp(0.5 + 0.5 * projection, 0, 1) * span;
            var from = stops.get(0);
            var to = from;
            for (int index = 1; index < stops.size(); index++) {
                to = stops.get(index);
                if (position <= to.position()) {
                    break;
                }
                from = to;
            }
            double fraction = from == to ? 0 : (position - from.position()) / (to.position() - from.position());
            fraction = Mth.clamp(fraction, 0, 1);
            fraction = fraction * fraction * (3 - 2 * fraction);
            var first = from.color();
            var second = to.color();
            double lightness = Mth.lerp(fraction, first.lightness(), second.lightness());
            double chroma = Mth.lerp(fraction, first.chroma(), second.chroma());
            double firstHue = first.chroma() < 0.02 ? second.hue() : first.hue();
            double secondHue = second.chroma() < 0.02 ? firstHue : second.hue();
            // 沿较短色相弧渐变，避免互补色直接平均产生灰色。
            double hueDelta = Math.atan2(Math.sin(secondHue - firstHue), Math.cos(secondHue - firstHue));
            double hue = firstHue + fraction * hueDelta;
            double a = chroma * Math.cos(hue);
            double b = chroma * Math.sin(hue);
            double l = lightness + 0.3963377774 * a + 0.2158037573 * b;
            double m = lightness - 0.1055613458 * a - 0.0638541728 * b;
            double s = lightness - 0.0894841775 * a - 1.2914855480 * b;
            l = l * l * l;
            m = m * m * m;
            s = s * s * s;
            double red = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
            double green = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
            double blue = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;
            double neutral = lightness * lightness * lightness;
            double minimum = Math.min(red, Math.min(green, blue));
            double maximum = Math.max(red, Math.max(green, blue));
            double gamut = 1;
            if (minimum < 0) {
                gamut = Math.min(gamut, neutral / (neutral - minimum));
            }
            if (maximum > 1) {
                gamut = Math.min(gamut, (1 - neutral) / (maximum - neutral));
            }
            result[0] = Mth.lerp(gamut, neutral, red);
            result[1] = Mth.lerp(gamut, neutral, green);
            result[2] = Mth.lerp(gamut, neutral, blue);
        }
    }

    private static double toLinear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private record KeyColor(AEColor color, double lightness, double chroma, double hue) {}

    private record Stop(double position, KeyColor color) {}
}
