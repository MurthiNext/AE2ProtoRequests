package com.murthinext.ae2pr.api.material;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 材质数据：描述“这个材质存在哪些形态”以及运行时着色。
 * 物品本身由 {@link MaterialItems} 批量生成。
 */
public final class Material {

    private final String id;
    private final Set<TagPrefix> prefixes;
    private final int color;
    private final String modelSet;

    private Material(String id, Set<TagPrefix> prefixes, int color, String modelSet) {
        this.id = id;
        this.prefixes = Set.copyOf(prefixes);
        this.color = color;
        this.modelSet = modelSet;
    }

    public String id() {
        return id;
    }

    public boolean has(TagPrefix prefix) {
        return prefixes.contains(prefix);
    }

    public String modelSet() {
        return modelSet;
    }

    /**
     * layer 0：材料主色；layer 1：包边色；layer 2+：不染色（彩点等预烘焙图层）。
     */
    public int argb(int layer) {
        if (layer >= 2) {
            return -1;
        }
        return 0xFF000000 | (layer == 1 ? secondaryColor() : color);
    }

    // 包边色
    private int secondaryColor() {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int sr = Math.min(255, (int) (r * 0.22) + 12);
        int sg = Math.min(255, (int) (g * 0.23) + 14);
        int sb = Math.min(255, (int) (b * 0.28) + 18);
        return (sr << 16) | (sg << 8) | sb;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {

        private final String id;
        private final Set<TagPrefix> prefixes = new LinkedHashSet<>();
        private int color = 0xFFFFFF;
        private String modelSet = "metallic";

        private Builder(String id) {
            this.id = id;
        }

        public Builder ingot() {
            prefixes.add(TagPrefix.INGOT);
            return this;
        }

        public Builder plate() {
            prefixes.add(TagPrefix.PLATE);
            return this;
        }

        public Builder dust() {
            prefixes.add(TagPrefix.DUST);
            return this;
        }

        public Builder color(int rgb) {
            this.color = rgb & 0xFFFFFF;
            return this;
        }

        public Builder modelSet(String modelSet) {
            this.modelSet = modelSet;
            return this;
        }

        public Material build() {
            return new Material(id, prefixes, color, modelSet);
        }
    }
}
