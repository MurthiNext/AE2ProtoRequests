package com.murthinext.ae2pr.api.material;

import java.util.List;

import net.minecraft.world.item.Item;

/**
 * 材质的形态模板：决定物品 id 后缀与物品类型。
 */
public final class TagPrefix {

    public static final TagPrefix INGOT = new TagPrefix("ingot", "%s_ingot");
    public static final TagPrefix PLATE = new TagPrefix("plate", "%s_plate");
    public static final TagPrefix DUST = new TagPrefix("dust", "%s_dust");

    public static final List<TagPrefix> VALUES = List.of(INGOT, PLATE, DUST);

    private final String name;
    private final String idPattern;

    private TagPrefix(String name, String idPattern) {
        this.name = name;
        this.idPattern = idPattern;
    }

    public String name() {
        return name;
    }

    public String idFor(String materialId) {
        return idPattern.formatted(materialId);
    }

    public Item createItem(Material material) {
        return new MaterialItem(new Item.Properties(), material);
    }
}
