package com.murthinext.ae2pr;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/**
 * 模组物品/流体标签。
 */
public final class ModTags {

    private ModTags() {
    }

    public static final TagKey<Item> WRENCHES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "wrenches"));

    public static final TagKey<Item> KNIVES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("ae2", "knife"));

    public static final TagKey<Item> CIRCUITS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "circuits"));

    // AEV 级电路（256TOPS）
    public static final TagKey<Item> CIRCUIT_AEV = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "circuits/aev"));

    // AEV 降级电路，d4 为最低档（1TOPS）
    public static final TagKey<Item> CIRCUIT_AEV_D1 = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "circuits/aev_d1"));

    public static final TagKey<Item> CIRCUIT_AEV_D2 = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "circuits/aev_d2"));

    public static final TagKey<Item> CIRCUIT_AEV_D3 = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "circuits/aev_d3"));

    public static final TagKey<Item> CIRCUIT_AEV_D4 = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "circuits/aev_d4"));

    // XPV 级电路
    public static final TagKey<Item> CIRCUIT_XPV = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "circuits/xpv"));

    public static final TagKey<Fluid> ALIEN_LAVA = TagKey.create(Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "alien_lava"));
}
