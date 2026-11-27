package com.murthinext.ae2pr.api.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 材料的形态模板：决定默认物品 ID、Forge 标签目录与每件物品对应的流体量。
 */
public final class Form {

    public static final Form INGOT = new Form("ingot", "ingots", 144);
    public static final Form PLATE = new Form("plate", "plates", 144);
    public static final Form DUST = new Form("dust", "dusts", 144);
    public static final Form GEM = new Form("gem", "gems", 144);
    public static final Form BLOCK = new Form("block", "storage_blocks", 1296);

    private static final String FORGE_NAMESPACE = "forge";

    private final String name;
    private final String tagDirectory;
    private final int defaultAmountMb;

    private Form(String name, String tagDirectory, int defaultAmountMb) {
        this.name = name;
        this.tagDirectory = tagDirectory;
        this.defaultAmountMb = defaultAmountMb;
    }

    public String name() {
        return name;
    }

    public String itemId(String materialId) {
        return materialId + "_" + name;
    }

    public int defaultAmountMb() {
        return defaultAmountMb;
    }

    public TagKey<Item> tag(String materialId) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(FORGE_NAMESPACE, tagDirectory + "/" + materialId));
    }

    public TagKey<Item> aggregateTag() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(FORGE_NAMESPACE, tagDirectory));
    }
}
