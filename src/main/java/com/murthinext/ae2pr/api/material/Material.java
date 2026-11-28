package com.murthinext.ae2pr.api.material;

import com.murthinext.ae2pr.api.FluidSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

/**
 * 材质数据：描述“这个材质存在哪些形态”以及运行时着色。
 * 物品与流体的注册由 {@link MaterialBuilder} 在 {@link MaterialRegistry} 中完成。
 */
public final class Material {

    private final String id;
    private final Map<Form, MaterialForm> forms;
    private final int color;
    private final String modelSet;
    @Nullable
    private final String dustOverlay;
    private FluidSet fluidSet;

    Material(
            String id,
            Map<Form, MaterialForm> forms,
            int color,
            String modelSet,
            @Nullable String dustOverlay) {
        this.id = id;
        this.forms = Collections.unmodifiableMap(new LinkedHashMap<>(forms));
        this.color = color;
        this.modelSet = modelSet;
        this.dustOverlay = dustOverlay;
    }

    public String id() {
        return id;
    }

    public Collection<MaterialForm> forms() {
        return forms.values();
    }

    public Optional<MaterialForm> form(Form form) {
        return Optional.ofNullable(forms.get(form));
    }

    public boolean has(Form form) {
        return forms.containsKey(form);
    }

    public boolean owns(Form form) {
        return form(form).map(MaterialForm::ownsItem).orElse(false);
    }

    public Optional<RegistryObject<? extends Item>> item(Form form) {
        return form(form).flatMap(MaterialForm::ownItem);
    }

    public String modelSet() {
        return modelSet;
    }

    public Optional<String> dustOverlay() {
        return Optional.ofNullable(dustOverlay);
    }

    public int color() {
        return color;
    }

    public boolean hasFluid() {
        return fluidSet != null;
    }

    public Optional<FluidSet> fluid() {
        return Optional.ofNullable(fluidSet);
    }

    void attachFluid(FluidSet fluidSet) {
        if (this.fluidSet != null) {
            throw new IllegalStateException("Fluid already attached to material " + id);
        }
        this.fluidSet = fluidSet;
    }

    /**
     * layer 0：材料主色；layer 1：副色（包边与暗部）；layer 2+：不染色（高光、彩点等预烘焙图层）。
     */
    public int argb(int layer) {
        if (layer >= 2) {
            return -1;
        }
        return 0xFF000000 | (layer == 1 ? secondaryColor() : color);
    }

    // 包边色：与 GTM 的 secondaryColor 同一量级，约为主色的一半并略微提亮
    private int secondaryColor() {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int sr = Math.min(255, (int) (r * 0.55) + 8);
        int sg = Math.min(255, (int) (g * 0.55) + 9);
        int sb = Math.min(255, (int) (b * 0.55) + 10);
        return (sr << 16) | (sg << 8) | sb;
    }
}
