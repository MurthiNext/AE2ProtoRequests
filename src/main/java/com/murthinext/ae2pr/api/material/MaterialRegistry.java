package com.murthinext.ae2pr.api.material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;

/**
 * 材料注册上下文：绑定物品、流体类型与流体注册表，并维护声明顺序。
 */
public final class MaterialRegistry {

    private final DeferredRegister<Item> items;
    private final DeferredRegister<FluidType> fluidTypes;
    private final DeferredRegister<Fluid> fluids;
    private final List<Material> all = new ArrayList<>();
    private final Map<String, Material> byId = new LinkedHashMap<>();

    public MaterialRegistry(
            DeferredRegister<Item> items, DeferredRegister<FluidType> fluidTypes, DeferredRegister<Fluid> fluids) {
        this.items = items;
        this.fluidTypes = fluidTypes;
        this.fluids = fluids;
    }

    public MaterialBuilder define(String id) {
        return new MaterialBuilder(this, id);
    }

    public List<Material> all() {
        return List.copyOf(all);
    }

    public Material byId(String id) {
        Material material = byId.get(id);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + id);
        }
        return material;
    }

    Material add(Material material) {
        if (byId.putIfAbsent(material.id(), material) != null) {
            throw new IllegalStateException("Duplicate material id: " + material.id());
        }
        all.add(material);
        return material;
    }

    DeferredRegister<Item> items() {
        return items;
    }

    DeferredRegister<FluidType> fluidTypes() {
        return fluidTypes;
    }

    DeferredRegister<Fluid> fluids() {
        return fluids;
    }
}
