package com.murthinext.ae2pr.api.material;

import com.murthinext.ae2pr.api.FluidSet;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.SimpleFluidType;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * 材料构建器：所有标准形态在链式调用中直接注册物品，{@link #register()} 为唯一终点。
 */
public final class MaterialBuilder {

    private static final String TEXTURE_ROOT = "block/material_sets/";

    private final MaterialRegistry registry;
    private final String id;
    private final Map<Form, MaterialForm> forms = new LinkedHashMap<>();
    private int color = 0xFFFFFF;
    private String modelSet = "metallic";
    private FluidSpec fluidSpec;
    private Material material;
    private boolean registered;

    MaterialBuilder(MaterialRegistry registry, String id) {
        this.registry = registry;
        this.id = id;
    }

    public MaterialBuilder ingot() {
        return own(Form.INGOT);
    }

    public MaterialBuilder plate() {
        return own(Form.PLATE);
    }

    public MaterialBuilder dust() {
        return own(Form.DUST);
    }

    public MaterialBuilder item(Form form, String itemId) {
        return item(form, itemId, form.defaultAmountMb());
    }

    public MaterialBuilder item(Form form, String itemId, int amountMb) {
        return addForm(
                new MaterialForm(form, new MaterialForm.Origin.ExternalItem(ResourceLocation.parse(itemId)), amountMb));
    }

    public MaterialBuilder tag(Form form, String tagId) {
        return tag(form, tagId, form.defaultAmountMb());
    }

    public MaterialBuilder tag(Form form, String tagId, int amountMb) {
        return addForm(new MaterialForm(
                form,
                new MaterialForm.Origin.ExternalTag(
                        TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId)), false),
                amountMb));
    }

    public MaterialBuilder optionalTag(Form form, String tagId) {
        return optionalTag(form, tagId, form.defaultAmountMb());
    }

    public MaterialBuilder optionalTag(Form form, String tagId, int amountMb) {
        return addForm(new MaterialForm(
                form,
                new MaterialForm.Origin.ExternalTag(
                        TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId)), true),
                amountMb));
    }

    public MaterialBuilder color(int rgb) {
        color = rgb & 0xFFFFFF;
        return this;
    }

    public MaterialBuilder modelSet(String modelSet) {
        this.modelSet = modelSet;
        return this;
    }

    public MaterialBuilder fluid(FluidSpec fluidSpec) {
        this.fluidSpec = fluidSpec;
        return this;
    }

    public Material register() {
        if (registered) {
            throw new IllegalStateException("Material builder already registered: " + id);
        }
        registered = true;
        if (forms.isEmpty() && fluidSpec == null) {
            throw new IllegalStateException("Material must declare at least one form or fluid: " + id);
        }

        material = new Material(id, forms, color, modelSet);
        if (fluidSpec != null) {
            String texture = TEXTURE_ROOT + modelSet + "/liquid";
            FluidSet fluidSet = FluidSet.of(
                    fluidSpec.id(),
                    registry.fluidTypes(),
                    registry.fluids(),
                    registry.items(),
                    () -> SimpleFluidType.of(
                            SimpleFluidType.fluidProperties(
                                    fluidSpec.lavaLike(),
                                    fluidSpec.lightLevel(),
                                    fluidSpec.density(),
                                    fluidSpec.viscosity(),
                                    fluidSpec.temperature()),
                            Ids.of(texture),
                            Ids.of(texture),
                            material.argb(0)));
            material.attachFluid(fluidSet);
        }
        return registry.add(material);
    }

    private MaterialBuilder own(Form form) {
        ensureNotRegistered();
        RegistryObject<Item> item = registry.items()
                .register(form.itemId(id), () -> new MaterialItem(new Item.Properties(), requireMaterial()));
        return addForm(new MaterialForm(form, new MaterialForm.Origin.Own(item, form.tag(id)), form.defaultAmountMb()));
    }

    private MaterialBuilder addForm(MaterialForm form) {
        ensureNotRegistered();
        if (forms.putIfAbsent(form.form(), form) != null) {
            throw new IllegalArgumentException(
                    "Duplicate material form %s for %s".formatted(form.form().name(), id));
        }
        return this;
    }

    private Material requireMaterial() {
        if (material == null) {
            throw new IllegalStateException("Material was not registered: " + id);
        }
        return material;
    }

    private void ensureNotRegistered() {
        if (registered) {
            throw new IllegalStateException("Material builder already registered: " + id);
        }
    }
}
