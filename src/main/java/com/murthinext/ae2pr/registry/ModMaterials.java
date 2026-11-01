package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.material.MaterialItems;
import com.murthinext.ae2pr.material.Materials;
import com.murthinext.ae2pr.material.TagPrefix;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class ModMaterials {

    private ModMaterials() {
    }

    public static final RegistryObject<Item> METEOR_STEEL_INGOT = MaterialItems.get(TagPrefix.INGOT, Materials.METEOR_STEEL);

    public static final RegistryObject<Item> ZIRCONIUM_INGOT = MaterialItems.get(TagPrefix.INGOT, Materials.ZIRCONIUM);

    public static final RegistryObject<Item> ZIRCONIUM_IRON_ALLOY_INGOT = MaterialItems.get(TagPrefix.INGOT,
            Materials.ZIRCONIUM_IRON_ALLOY);

    public static final RegistryObject<Item> STRONTIUM_INGOT = MaterialItems.get(TagPrefix.INGOT, Materials.STRONTIUM);

    public static final RegistryObject<Item> BARIUM_INGOT = MaterialItems.get(TagPrefix.INGOT, Materials.BARIUM);

    public static final RegistryObject<Item> METEOR_STEEL_PLATE = MaterialItems.get(TagPrefix.PLATE, Materials.METEOR_STEEL);

    public static final RegistryObject<Item> ZIRCONIUM_PLATE = MaterialItems.get(TagPrefix.PLATE, Materials.ZIRCONIUM);

    public static final RegistryObject<Item> STRONTIUM_PLATE = MaterialItems.get(TagPrefix.PLATE, Materials.STRONTIUM);

    public static final RegistryObject<Item> BARIUM_PLATE = MaterialItems.get(TagPrefix.PLATE, Materials.BARIUM);

    public static final RegistryObject<Item> METEOR_STEEL_DUST = MaterialItems.get(TagPrefix.DUST,
            Materials.METEOR_STEEL);

    public static final RegistryObject<Item> COAL_DUST = MaterialItems.get(TagPrefix.DUST, Materials.COAL);

    public static final RegistryObject<Item> CARBON_DUST = MaterialItems.get(TagPrefix.DUST, Materials.CARBON);

    public static final RegistryObject<Item> ZIRCONIUM_DUST = MaterialItems.get(TagPrefix.DUST, Materials.ZIRCONIUM);

    public static final RegistryObject<Item> CARBON_ZIRCONIUM_MIXTURE_DUST = MaterialItems.get(TagPrefix.DUST,
            Materials.CARBON_ZIRCONIUM_MIXTURE);

    public static final RegistryObject<Item> ZIRCON_DUST = MaterialItems.get(TagPrefix.DUST, Materials.ZIRCON);

    public static final RegistryObject<Item> COPPER_OXIDE_DUST = MaterialItems.get(TagPrefix.DUST, Materials.COPPER_OXIDE);

    public static final RegistryObject<Item> IRON_OXIDE_DUST = MaterialItems.get(TagPrefix.DUST, Materials.IRON_OXIDE);

    public static final RegistryObject<Item> GOLD_OXIDE_DUST = MaterialItems.get(TagPrefix.DUST, Materials.GOLD_OXIDE);

    public static final RegistryObject<Item> PSEUDO_STRONTIANITE_DUST = MaterialItems.get(TagPrefix.DUST,
            Materials.PSEUDO_STRONTIANITE);

    public static final RegistryObject<Item> STRONTIUM_CARBONATE_DUST = MaterialItems.get(TagPrefix.DUST,
            Materials.STRONTIUM_CARBONATE);

    public static final RegistryObject<Item> BARIUM_SULFATE_DUST = MaterialItems.get(TagPrefix.DUST,
            Materials.BARIUM_SULFATE);

    public static final RegistryObject<Item> STRONTIUM_DUST = MaterialItems.get(TagPrefix.DUST, Materials.STRONTIUM);

    public static final RegistryObject<Item> BARIUM_DUST = MaterialItems.get(TagPrefix.DUST, Materials.BARIUM);
}
