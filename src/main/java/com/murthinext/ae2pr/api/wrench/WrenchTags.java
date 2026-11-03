package com.murthinext.ae2pr.api.wrench;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class WrenchTags {

    public static final TagKey<Item> WRENCH = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("forge", "tools/wrench"));

    public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("ae2", "knife"));

    private WrenchTags() {
    }
}
