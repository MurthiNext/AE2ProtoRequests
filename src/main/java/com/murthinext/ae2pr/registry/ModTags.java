package com.murthinext.ae2pr.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import com.murthinext.ae2pr.api.Ids;

public final class ModTags {

    private ModTags() {
    }

    public static final TagKey<Fluid> ALIEN_LAVA = TagKey.create(Registries.FLUID, Ids.of("alien_lava"));

    // 电路板

    public static final TagKey<Item> CIRCUITS = ItemTags.create(Ids.of("circuits"));

    public static final TagKey<Item> CIRCUIT_AEV = ItemTags.create(Ids.of("circuits/aev"));

    public static final TagKey<Item> CIRCUIT_AEV_D1 = ItemTags.create(Ids.of("circuits/aev_d1"));

    public static final TagKey<Item> CIRCUIT_AEV_D2 = ItemTags.create(Ids.of("circuits/aev_d2"));

    public static final TagKey<Item> CIRCUIT_AEV_D3 = ItemTags.create(Ids.of("circuits/aev_d3"));

    public static final TagKey<Item> CIRCUIT_AEV_D4 = ItemTags.create(Ids.of("circuits/aev_d4"));

    public static final TagKey<Item> CIRCUIT_XPV = ItemTags.create(Ids.of("circuits/xpv"));
}
