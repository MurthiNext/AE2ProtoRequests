package com.murthinext.ae2pr;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.block.alien_lava.AlienLavaBlock;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineUnitBlock;
import com.murthinext.ae2pr.block.assembly_line.CertusQuartzCrystalMachinePartBlock;
import com.murthinext.ae2pr.block.assembly_line.CrystalAssemblyLineBlock;
import com.murthinext.ae2pr.block.assembly_line.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.meteorite.MeteoriteOreBlock;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlock;
import com.murthinext.ae2pr.block.redstone_requester.RedstoneRequesterBlock;

/**
 * 方块注册入口。
 */
public final class ModBlocks {

    private ModBlocks() {
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ae2pr.MODID);

    /** ME 红石请求器 */
    public static final RegistryObject<RedstoneRequesterBlock> REDSTONE_REQUESTER = BLOCKS.register(
            "redstone_requester", RedstoneRequesterBlock::new);

    /** 水晶强化复合机械方块 */
    public static final RegistryObject<Block> CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING = BLOCKS.register(
            "crystal_reinforced_composite_machine_casing", () -> new Block(casingProperties()));

    /** 水晶装配线 */
    public static final RegistryObject<CrystalAssemblyLineBlock> CRYSTAL_ASSEMBLY_LINE = BLOCKS.register(
            "crystal_assembly_line", CrystalAssemblyLineBlock::new);

    /** 水晶装配线外壳 */
    public static final RegistryObject<Block> CRYSTAL_ASSEMBLY_LINE_CASING = BLOCKS.register(
            "crystal_assembly_line_casing", () -> new Block(casingProperties()));

    /** 水晶装配线控制外壳 */
    public static final RegistryObject<AssemblyLineUnitBlock> CRYSTAL_ASSEMBLY_LINE_UNIT = BLOCKS.register(
            "crystal_assembly_line_unit", AssemblyLineUnitBlock::new);

    /** 水晶装配线格栅 */
    public static final RegistryObject<Block> CRYSTAL_ASSEMBLY_LINE_GRATING = BLOCKS.register(
            "crystal_assembly_line_grating", () -> new Block(casingProperties()));

    /** 水晶玻璃 */
    public static final RegistryObject<GlassBlock> CRYSTAL_GLASS = BLOCKS.register(
            "crystal_glass",
            () -> new GlassBlock(Block.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.8F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    /** 赛特斯石英水晶输入总线 */
    public static final RegistryObject<CertusQuartzCrystalMachinePartBlock> CERTUS_QUARTZ_CRYSTAL_INPUT_BUS = BLOCKS.register(
            "certus_quartz_crystal_input_bus", CertusQuartzCrystalMachinePartBlock::new);

    /** 赛特斯石英水晶输入仓 */
    public static final RegistryObject<CertusQuartzCrystalMachinePartBlock> CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH = BLOCKS.register(
            "certus_quartz_crystal_input_hatch", CertusQuartzCrystalMachinePartBlock::new);

    /** 赛特斯石英水晶输出总线 */
    public static final RegistryObject<CertusQuartzCrystalMachinePartBlock> CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS = BLOCKS.register(
            "certus_quartz_crystal_output_bus", CertusQuartzCrystalMachinePartBlock::new);

    /** 福鲁伊克斯水晶能源仓 */
    public static final RegistryObject<FluixCrystalEnergyHatchBlock> FLUIX_CRYSTAL_ENERGY_HATCH = BLOCKS.register(
            "fluix_crystal_energy_hatch", FluixCrystalEnergyHatchBlock::new);

    /** 异星熔岩 */
    public static final RegistryObject<AlienLavaBlock> ALIEN_LAVA = BLOCKS.register("alien_lava",
            () -> new AlienLavaBlock(ModFluids.ALIEN_LAVA, Block.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .replaceable()
                    .noCollission()
                    .randomTicks()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 15)
                    .noLootTable()));

    /** 名称压印工厂 */
    public static final RegistryObject<NamingFactoryBlock> NAMING_FACTORY = BLOCKS.register(
            "naming_factory", NamingFactoryBlock::new);

    /** 陨石铁矿石 */
    public static final RegistryObject<MeteoriteOreBlock> METEORITE_IRON_ORE = BLOCKS.register(
            "meteorite_iron_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    /** 陨石铜矿石 */
    public static final RegistryObject<MeteoriteOreBlock> METEORITE_COPPER_ORE = BLOCKS.register(
            "meteorite_copper_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    /** 陨石金矿石 */
    public static final RegistryObject<MeteoriteOreBlock> METEORITE_GOLD_ORE = BLOCKS.register(
            "meteorite_gold_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    /** 陨石青金石矿石 */
    public static final RegistryObject<MeteoriteOreBlock> METEORITE_LAPIS_ORE = BLOCKS.register(
            "meteorite_lapis_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(2, 5)));

    /** 陨石钻石矿石 */
    public static final RegistryObject<MeteoriteOreBlock> METEORITE_DIAMOND_ORE = BLOCKS.register(
            "meteorite_diamond_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    /** 陨石绿宝石矿石 */
    public static final RegistryObject<MeteoriteOreBlock> METEORITE_EMERALD_ORE = BLOCKS.register(
            "meteorite_emerald_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    /** 陨石锆英石矿石 */
    public static final RegistryObject<MeteoriteOreBlock> METEORITE_ZIRCON_ORE = BLOCKS.register(
            "meteorite_zircon_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    /** 陨钢块 */
    public static final RegistryObject<Block> METEOR_STEEL_BLOCK = BLOCKS.register(
            "meteor_steel_block", () -> new Block(storageBlockProperties(MapColor.METAL)));

    /** 锆英石块 */
    public static final RegistryObject<Block> ZIRCON_BLOCK = BLOCKS.register(
            "zircon_block", () -> new Block(storageBlockProperties(MapColor.COLOR_LIGHT_GRAY)));

    /** 锆刚玉砖块 */
    public static final RegistryObject<Block> ZIRCONIA_CORUNDUM_BRICKS = BLOCKS.register(
            "zirconia_corundum_bricks", () -> new Block(Block.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(3.5F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    private static Block.Properties storageBlockProperties(MapColor mapColor) {
        return Block.Properties.of()
                .mapColor(mapColor)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static Block.Properties meteoriteOreProperties() {
        return Block.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(50.0F, 150.0F)
                .requiresCorrectToolForDrops()
                .forceSolidOn();
    }

    private static Block.Properties casingProperties() {
        return Block.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
