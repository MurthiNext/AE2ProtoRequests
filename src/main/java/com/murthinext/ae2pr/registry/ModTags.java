package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.api.Ids;

import com.murthinext.ae2pr.ae2pr;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

public final class ModTags {

    private ModTags() {
    }

    public static final TagKey<Item> WRENCH = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("forge", "tools/wrench"));

    public static final TagKey<Item> KNIVES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("ae2", "knife"));

    public static final TagKey<Fluid> ALIEN_LAVA = TagKey.create(Registries.FLUID,
            Ids.of( "alien_lava"));

    // 电路板

    public static final TagKey<Item> CIRCUITS = ItemTags.create(
            Ids.of( "circuits"));

    public static final TagKey<Item> CIRCUIT_AEV = ItemTags.create(
            Ids.of( "circuits/aev"));

    public static final TagKey<Item> CIRCUIT_AEV_D1 = ItemTags.create(
            Ids.of( "circuits/aev_d1"));

    public static final TagKey<Item> CIRCUIT_AEV_D2 = ItemTags.create(
            Ids.of( "circuits/aev_d2"));

    public static final TagKey<Item> CIRCUIT_AEV_D3 = ItemTags.create(
            Ids.of( "circuits/aev_d3"));

    public static final TagKey<Item> CIRCUIT_AEV_D4 = ItemTags.create(
            Ids.of( "circuits/aev_d4"));

    public static final TagKey<Item> CIRCUIT_XPV = ItemTags.create(
            Ids.of( "circuits/xpv"));
}
