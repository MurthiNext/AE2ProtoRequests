package com.murthinext.ae2pr.api.material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;

import com.murthinext.ae2pr.api.FluidSet;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.SimpleFluidType;

public final class MaterialFluids {

    private static final String TEXTURE_ROOT = "block/material_sets/";
    private static final String TEXTURE = "liquid";

    private static final Map<Material, FluidSet> REGISTRY = new LinkedHashMap<>();

    public static Material register(DeferredRegister<Item> items, DeferredRegister<FluidType> types,
            DeferredRegister<Fluid> fluids, Material material) {
        FluidSpec spec = material.fluid();
        if (spec == null) {
            throw new IllegalStateException("Material %s declares no fluid".formatted(material.id()));
        }
        String root = TEXTURE_ROOT + material.modelSet() + "/" + TEXTURE;
        FluidSet set = FluidSet.of(spec.id(), types, fluids, items, () -> SimpleFluidType.of(
                SimpleFluidType.fluidProperties(spec.lavaLike(), spec.lightLevel(), spec.density(), spec.viscosity(),
                        spec.temperature()),
                Ids.of(root), Ids.of(root), material.argb(0)));
        REGISTRY.put(material, set);
        return material;
    }

    public static FluidSet get(Material material) {
        FluidSet set = REGISTRY.get(material);
        if (set == null) {
            throw new IllegalArgumentException("No fluid for material %s".formatted(material.id()));
        }
        return set;
    }

    public static Collection<FluidSet> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    private MaterialFluids() {
    }
}
