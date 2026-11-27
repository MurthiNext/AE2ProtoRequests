package com.murthinext.ae2pr.api.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 材质的形态模板：决定默认的物品 ID 和 Forge 标签。
 */
public final class TagPrefix {

    public static final TagPrefix INGOT = new TagPrefix("ingot", "ingots");
    public static final TagPrefix PLATE = new TagPrefix("plate", "plates");
    public static final TagPrefix DUST = new TagPrefix("dust", "dusts");
    public static final TagPrefix GEM = new TagPrefix("gem", "gems");
    public static final TagPrefix BLOCK = new TagPrefix("block", "storage_blocks");

    private final String name;
    private final String tagDirectory;

    private TagPrefix(String name, String tagDirectory) {
        this.name = name;
        this.tagDirectory = tagDirectory;
    }

    public String name() {
        return name;
    }

    public String idFor(String materialId) {
        return materialId + "_" + name;
    }

    public TagKey<Item> tag(String materialId) {
        return TagKey.create(
                Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", tagDirectory + "/" + materialId));
    }
}
