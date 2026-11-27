package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.api.material.FluidSpec;
import com.murthinext.ae2pr.api.material.Form;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialRegistry;
import java.util.List;

public final class ModMaterials {

    private static final int CRYSTALS_PER_BLOCK = 4;

    private static final MaterialRegistry MATERIALS =
            new MaterialRegistry(ModItems.ITEMS, ModFluids.FLUID_TYPES, ModFluids.FLUIDS);

    public static Material METEOR_STEEL;
    public static Material ZIRCONIUM;
    public static Material ZIRCONIUM_IRON_ALLOY;
    public static Material AMETHYST_STEEL;
    public static Material CRYSTAL_STEEL;
    public static Material STRONTIUM;
    public static Material BARIUM;
    public static Material AMETHYST;
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
    public static Material CERTUS_QUARTZ;
    public static Material FLUIX_CRYSTAL;
    public static Material CRYSTALINE;
    public static Material CRYSTAL_ADHESIVE;

    // 声明顺序即标签页与配方生成顺序
    public static List<Material> ALL;

    private ModMaterials() {}

    // 静态字段不做初始化，由模组构造阶段显式调用，避免首次访问落在注册事件之后
    public static void register() {
        METEOR_STEEL = MATERIALS
                .define("meteor_steel")
                .ingot()
                .plate()
                .dust()
                .color(0xE1EBF8)
                .modelSet("meteor")
                .register();
        ZIRCONIUM = MATERIALS
                .define("zirconium")
                .ingot()
                .plate()
                .dust()
                .optionalTag(Form.BLOCK, "forge:storage_blocks/zirconium", Form.BLOCK.defaultAmountMb())
                .color(0xDDEDF4)
                .fluid(FluidSpec.lava("liquid_zirconium", 8, 2600, 5000, 400))
                .register();
        ZIRCONIUM_IRON_ALLOY = MATERIALS
                .define("zirconium_iron_alloy")
                .ingot()
                .plate()
                .dust()
                .color(0x6B7BB5)
                .register();
        AMETHYST_STEEL = MATERIALS
                .define("amethyst_steel")
                .ingot()
                .plate()
                .dust()
                .color(0x9C93C8)
                .register();
        CRYSTAL_STEEL = MATERIALS
                .define("crystal_steel")
                .ingot()
                .plate()
                .dust()
                .color(0xB7C8D8)
                .register();
        STRONTIUM = MATERIALS
                .define("strontium")
                .ingot()
                .plate()
                .dust()
                .color(0xFDFAE0)
                .register();
        BARIUM = MATERIALS
                .define("barium")
                .ingot()
                .plate()
                .dust()
                .color(0xF5FCFF)
                .register();
        AMETHYST = MATERIALS
                .define("amethyst")
                .dust()
                .tag(Form.GEM, "forge:gems/amethyst")
                .item(Form.BLOCK, "minecraft:amethyst_block", CRYSTALS_PER_BLOCK * Form.INGOT.defaultAmountMb())
                .color(0xC78DF3)
                .fluid(FluidSpec.water("liquid_amethyst", 8, 2000, 3000, 400))
                .register();
        COAL = MATERIALS.define("coal").dust().color(0x8D7867).register();
        CARBON = MATERIALS.define("carbon").dust().color(0x222528).register();
        CARBON_ZIRCONIUM_MIXTURE = MATERIALS
                .define("carbon_zirconium_mixture")
                .dust()
                .color(0x6F818D)
                .register();
        ZIRCON = MATERIALS.define("zircon").dust().color(0xE8E8E8).register();
        COPPER_OXIDE = MATERIALS.define("copper_oxide").dust().color(0x33363A).register();
        IRON_OXIDE = MATERIALS.define("iron_oxide").dust().color(0xA55837).register();
        GOLD_OXIDE = MATERIALS.define("gold_oxide").dust().color(0xAF8D2E).register();
        PSEUDO_STRONTIANITE =
                MATERIALS.define("pseudo_strontianite").dust().color(0xB2BD9D).register();
        STRONTIUM_CARBONATE =
                MATERIALS.define("strontium_carbonate").dust().color(0xD9D9D0).register();
        BARIUM_SULFATE =
                MATERIALS.define("barium_sulfate").dust().color(0xDCE0E5).register();

        CERTUS_QUARTZ = MATERIALS
                .define("certus_quartz")
                .tag(Form.GEM, "ae2:all_certus_quartz")
                .tag(Form.DUST, "forge:dusts/certus_quartz")
                .tag(
                        Form.BLOCK,
                        "forge:storage_blocks/certus_quartz",
                        CRYSTALS_PER_BLOCK * Form.INGOT.defaultAmountMb())
                .color(0x94B7FF)
                .fluid(FluidSpec.water("liquid_certus_quartz", 8, 2000, 3000, 400))
                .register();
        FLUIX_CRYSTAL = MATERIALS
                .define("fluix_crystal")
                .tag(Form.GEM, "ae2:all_fluix")
                .tag(Form.DUST, "forge:dusts/fluix")
                .item(Form.BLOCK, "ae2:fluix_block", CRYSTALS_PER_BLOCK * Form.INGOT.defaultAmountMb())
                .color(0xAD94FF)
                .fluid(FluidSpec.water("liquid_fluix_crystal", 8, 2000, 3000, 400))
                .register();
        CRYSTALINE = MATERIALS
                .define("crystaline")
                .color(0x94CDFF)
                .fluid(FluidSpec.water("liquid_crystaline", 8, 2000, 3000, 400))
                .register();
        CRYSTAL_ADHESIVE = MATERIALS
                .define("crystal_adhesive")
                .color(0xFACD8E)
                .fluid(FluidSpec.water("crystal_adhesive", 6, 2400, 6000, 360))
                .register();

        ALL = MATERIALS.all();
    }
}
