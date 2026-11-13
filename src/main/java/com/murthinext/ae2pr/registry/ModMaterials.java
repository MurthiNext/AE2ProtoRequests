package com.murthinext.ae2pr.registry;

import java.util.List;

import com.murthinext.ae2pr.api.material.FluidSpec;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialFluids;
import com.murthinext.ae2pr.api.material.MaterialItems;

public final class ModMaterials {

    public static Material METEOR_STEEL;
    public static Material ZIRCONIUM;
    public static Material ZIRCONIUM_IRON_ALLOY;
    public static Material AMETHYST;
    public static Material AMETHYST_STEEL;
    public static Material STRONTIUM;
    public static Material BARIUM;
    public static Material COAL;
    public static Material CARBON;
    public static Material CARBON_ZIRCONIUM_MIXTURE;
    public static Material ZIRCON;
    public static Material COPPER_OXIDE;
    public static Material IRON_OXIDE;
    public static Material GOLD_OXIDE;
    public static Material PSEUDO_STRONTIANITE;
    public static Material STRONTIUM_CARBONATE;
    public static Material BARIUM_SULFATE;
    public static Material LIQUID_CERTUS_QUARTZ;
    public static Material LIQUID_FLUIX_CRYSTAL;
    public static Material LIQUID_AMETHYST;
    public static Material LIQUID_CRYSTALINE;
    public static Material CRYSTAL_ADHESIVE;

    // 金属优先，其后是化合物与中间产物；标签页与配方生成共用此顺序
    public static List<Material> ALL;

    private ModMaterials() {
    }

    // 静态字段不做初始化，由模组构造阶段显式调用，避免首次访问落在注册事件之后
    public static void register() {
        METEOR_STEEL = MaterialItems.register(
                ModItems.ITEMS,
                Material.builder("meteor_steel").ingot().plate().dust().color(0xE1EBF8).modelSet("meteor").build());
        ZIRCONIUM = both(Material.builder("zirconium")
                                 .ingot()
                                 .plate()
                                 .dust()
                                 .color(0xDDEDF4)
                                 .fluid(FluidSpec.lava("liquid_zirconium", 8, 2600, 5000, 400)));
        ZIRCONIUM_IRON_ALLOY = MaterialItems.register(
                ModItems.ITEMS,
                Material.builder("zirconium_iron_alloy").ingot().plate().dust().color(0x6B7BB5).build());
        AMETHYST = MaterialItems.register(ModItems.ITEMS, Material.builder("amethyst").dust().color(0xC78DF3).build());
        AMETHYST_STEEL = MaterialItems.register(
                ModItems.ITEMS, Material.builder("amethyst_steel").ingot().plate().dust().color(0x9C93C8).build());
        STRONTIUM = MaterialItems.register(
                ModItems.ITEMS, Material.builder("strontium").ingot().plate().dust().color(0xFDFAE0).build());
        BARIUM = MaterialItems.register(ModItems.ITEMS,
                                        Material.builder("barium").ingot().plate().dust().color(0xF5FCFF).build());
        COAL = MaterialItems.register(ModItems.ITEMS, Material.builder("coal").dust().color(0x8D7867).build());
        CARBON = MaterialItems.register(ModItems.ITEMS, Material.builder("carbon").dust().color(0x222528).build());
        CARBON_ZIRCONIUM_MIXTURE = MaterialItems.register(
                ModItems.ITEMS, Material.builder("carbon_zirconium_mixture").dust().color(0x6F818D).build());
        ZIRCON = MaterialItems.register(ModItems.ITEMS, Material.builder("zircon").dust().color(0xE8E8E8).build());
        COPPER_OXIDE = MaterialItems.register(ModItems.ITEMS,
                                              Material.builder("copper_oxide").dust().color(0x33363A).build());
        IRON_OXIDE = MaterialItems.register(ModItems.ITEMS,
                                            Material.builder("iron_oxide").dust().color(0xA55837).build());
        GOLD_OXIDE = MaterialItems.register(ModItems.ITEMS,
                                            Material.builder("gold_oxide").dust().color(0xAF8D2E).build());
        PSEUDO_STRONTIANITE = MaterialItems.register(
                ModItems.ITEMS, Material.builder("pseudo_strontianite").dust().color(0xB2BD9D).build());
        STRONTIUM_CARBONATE = MaterialItems.register(
                ModItems.ITEMS, Material.builder("strontium_carbonate").dust().color(0xD9D9D0).build());
        BARIUM_SULFATE = MaterialItems.register(ModItems.ITEMS,
                                                Material.builder("barium_sulfate").dust().color(0xDCE0E5).build());

        LIQUID_CERTUS_QUARTZ = fluid(Material.builder("liquid_certus_quartz")
                                             .color(0x94B7FF)
                                             .fluid(FluidSpec.water("liquid_certus_quartz", 8, 2000, 3000, 400)));
        LIQUID_FLUIX_CRYSTAL = fluid(Material.builder("liquid_fluix_crystal")
                                             .color(0xAD94FF)
                                             .fluid(FluidSpec.water("liquid_fluix_crystal", 8, 2000, 3000, 400)));
        LIQUID_AMETHYST = fluid(Material.builder("liquid_amethyst")
                                        .color(0xC78DF3)
                                        .fluid(FluidSpec.water("liquid_amethyst", 8, 2000, 3000, 400)));
        LIQUID_CRYSTALINE = fluid(Material.builder("liquid_crystaline")
                                          .color(0x94CDFF)
                                          .fluid(FluidSpec.water("liquid_crystaline", 8, 2000, 3000, 400)));
        CRYSTAL_ADHESIVE = fluid(Material.builder("crystal_adhesive")
                                         .color(0xFACD8E)
                                         .fluid(FluidSpec.water("crystal_adhesive", 6, 2400, 6000, 360)));

        ALL = List.of(METEOR_STEEL, ZIRCONIUM, ZIRCONIUM_IRON_ALLOY, AMETHYST_STEEL, STRONTIUM, BARIUM, AMETHYST, COAL,
                      CARBON, CARBON_ZIRCONIUM_MIXTURE, ZIRCON, COPPER_OXIDE, IRON_OXIDE, GOLD_OXIDE,
                      PSEUDO_STRONTIANITE, STRONTIUM_CARBONATE, BARIUM_SULFATE);
    }

    private static Material both(Material.Builder builder) {
        return MaterialFluids.register(ModItems.ITEMS, ModFluids.FLUID_TYPES, ModFluids.FLUIDS,
                                       MaterialItems.register(ModItems.ITEMS, builder.build()));
    }

    private static Material fluid(Material.Builder builder) {
        return MaterialFluids.register(ModItems.ITEMS, ModFluids.FLUID_TYPES, ModFluids.FLUIDS, builder.build());
    }
}
