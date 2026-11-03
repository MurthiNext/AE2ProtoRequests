package com.murthinext.ae2pr.api.material;

import net.minecraft.world.item.Item;

public class MaterialItem extends Item {

    private final Material material;

    public MaterialItem(Item.Properties properties, Material material) {
        super(properties);
        this.material = material;
    }

    public Material material() {
        return material;
    }
}
