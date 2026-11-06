package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.api.material.FluidSpec;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialFluids;
import com.murthinext.ae2pr.api.material.MaterialItems;

import java.util.List;

public final class ModMaterials {

    private ModMaterials() {
    }

    // 材质声明即完成物品登记，物品随 ModItems.ITEMS 一并提交
    public static final Material METEOR_STEEL = MaterialItems.register(ModItems.ITEMS, Material.builder("meteor_steel")
            .ingot().plate().dust().color(0xE1EBF8).modelSet("meteor").build());

    public static final Material ZIRCONIUM = both(Material.builder("zirconium")
            .ingot().plate().dust().color(0xDDEDF4)
            .fluid(FluidSpec.lava("liquid_zirconium", 8, 2600, 5000, 400)));

    public static final Material ZIRCONIUM_IRON_ALLOY = MaterialItems.register(ModItems.ITEMS,
            Material.builder("zirconium_iron_alloy").ingot().plate().dust().color(0x6B7BB5).build());

    public static final Material AMETHYST = MaterialItems.register(ModItems.ITEMS,
            Material.builder("amethyst").dust().color(0xC78DF3).build());

    public static final Material AMETHYST_STEEL = MaterialItems.register(ModItems.ITEMS,
            Material.builder("amethyst_steel").ingot().plate().dust().color(0x9C93C8).build());

    public static final Material STRONTIUM = MaterialItems.register(ModItems.ITEMS, Material.builder("strontium")
            .ingot().plate().dust().color(0xFDFAE0).build());

    public static final Material BARIUM = MaterialItems.register(ModItems.ITEMS, Material.builder("barium")
            .ingot().plate().dust().color(0xF5FCFF).build());

    public static final Material COAL = MaterialItems.register(ModItems.ITEMS,
            Material.builder("coal").dust().color(0x8D7867).build());

    public static final Material CARBON = MaterialItems.register(ModItems.ITEMS,
            Material.builder("carbon").dust().color(0x222528).build());

    public static final Material CARBON_ZIRCONIUM_MIXTURE = MaterialItems.register(ModItems.ITEMS,
            Material.builder("carbon_zirconium_mixture").dust().color(0x6F818D).build());

    public static final Material ZIRCON = MaterialItems.register(ModItems.ITEMS,
            Material.builder("zircon").dust().color(0xE8E8E8).build());

    public static final Material COPPER_OXIDE = MaterialItems.register(ModItems.ITEMS,
            Material.builder("copper_oxide").dust().color(0x33363A).build());

    public static final Material IRON_OXIDE = MaterialItems.register(ModItems.ITEMS,
            Material.builder("iron_oxide").dust().color(0xA55837).build());

    public static final Material GOLD_OXIDE = MaterialItems.register(ModItems.ITEMS,
            Material.builder("gold_oxide").dust().color(0xAF8D2E).build());

    public static final Material PSEUDO_STRONTIANITE = MaterialItems.register(ModItems.ITEMS,
            Material.builder("pseudo_strontianite").dust().color(0xB2BD9D).build());

    public static final Material STRONTIUM_CARBONATE = MaterialItems.register(ModItems.ITEMS,
            Material.builder("strontium_carbonate").dust().color(0xD9D9D0).build());

    public static final Material BARIUM_SULFATE = MaterialItems.register(ModItems.ITEMS,
            Material.builder("barium_sulfate").dust().color(0xDCE0E5).build());

    public static final Material LIQUID_CERTUS_QUARTZ = fluid(Material.builder("liquid_certus_quartz")
            .color(0x94B7FF).fluid(FluidSpec.water("liquid_certus_quartz", 8, 2000, 3000, 400)));

    public static final Material LIQUID_FLUIX_CRYSTAL = fluid(Material.builder("liquid_fluix_crystal")
            .color(0xAD94FF).fluid(FluidSpec.water("liquid_fluix_crystal", 8, 2000, 3000, 400)));

    public static final Material LIQUID_AMETHYST = fluid(Material.builder("liquid_amethyst")
            .color(0xC78DF3).fluid(FluidSpec.water("liquid_amethyst", 8, 2000, 3000, 400)));

    public static final Material LIQUID_CRYSTALINE = fluid(Material.builder("liquid_crystaline")
            .color(0x94CDFF).fluid(FluidSpec.water("liquid_crystaline", 8, 2000, 3000, 400)));

    public static final Material CRYSTAL_ADHESIVE = fluid(Material.builder("crystal_adhesive")
            .color(0xFACD8E).fluid(FluidSpec.water("crystal_adhesive", 6, 2400, 6000, 360)));

    // 金属优先，其后是化合物与中间产物；标签页与配方生成共用此顺序
    public static final List<Material> ALL = List.of(METEOR_STEEL, ZIRCONIUM, ZIRCONIUM_IRON_ALLOY, AMETHYST_STEEL,
            STRONTIUM, BARIUM, AMETHYST, COAL, CARBON, CARBON_ZIRCONIUM_MIXTURE, ZIRCON, COPPER_OXIDE, IRON_OXIDE,
            GOLD_OXIDE, PSEUDO_STRONTIANITE, STRONTIUM_CARBONATE, BARIUM_SULFATE);

    private static Material both(Material.Builder builder) {
        return MaterialFluids.register(ModItems.ITEMS, ModFluids.FLUID_TYPES, ModFluids.FLUIDS,
                MaterialItems.register(ModItems.ITEMS, builder.build()));
    }

    private static Material fluid(Material.Builder builder) {
        return MaterialFluids.register(ModItems.ITEMS, ModFluids.FLUID_TYPES, ModFluids.FLUIDS, builder.build());
    }

    public static void init() {
    }
}
