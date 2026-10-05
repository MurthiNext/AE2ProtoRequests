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

    /**
     * 扳手：用于旋转本模组的机器与部件。
     */
    public static final TagKey<Item> WRENCHES = ItemTags.create(new ResourceLocation(ae2pr.MODID, "wrenches"));

    /**
     * 石英切割刀：右击本模组机器可为其写入自定义名称。
     */
    public static final TagKey<Item> KNIVES = ItemTags.create(new ResourceLocation("ae2", "knife"));

    public static final TagKey<Fluid> ALIEN_LAVA = TagKey.create(Registries.FLUID,
            new ResourceLocation(ae2pr.MODID, "alien_lava"));
}
