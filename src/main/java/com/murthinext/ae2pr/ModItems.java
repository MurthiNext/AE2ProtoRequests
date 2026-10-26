package com.murthinext.ae2pr;

import com.murthinext.ae2pr.item.NoBlockFluidBucketItem;
import com.murthinext.ae2pr.item.ComputationCellComponentItem;
import com.murthinext.ae2pr.item.CrystalProcessorItem;
import com.murthinext.ae2pr.item.crystaline.CrystalineItem;
import com.murthinext.ae2pr.item.level_emitter.MultiLevelEmitterPartItem;
import com.murthinext.ae2pr.item.level_emitter.MultiThresholdLevelEmitterPartItem;
import com.murthinext.ae2pr.item.filter_cell.AdvancedFilterCellItem;
import com.murthinext.ae2pr.item.filter_cell.FilterCellItem;
import com.murthinext.ae2pr.item.ProtoTerminalItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册入口。
 */
public final class ModItems {

    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ae2pr.MODID);

    public static final RegistryObject<FilterCellItem> FILTER_CELL = ITEMS.register("filter_cell",
            () -> new FilterCellItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<AdvancedFilterCellItem> ADVANCED_FILTER_CELL = ITEMS.register(
            "advanced_filter_cell", () -> new AdvancedFilterCellItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PROTO_TERMINAL = ITEMS.register("proto_terminal",
            () -> new ProtoTerminalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<ComputationCellComponentItem> COMPUTATION_CELL_COMPONENT_1TOPS = ITEMS.register(
            "computation_cell_component_1tops", () -> new ComputationCellComponentItem(new Item.Properties(), 4));

    public static final RegistryObject<ComputationCellComponentItem> COMPUTATION_CELL_COMPONENT_4TOPS = ITEMS.register(
            "computation_cell_component_4tops", () -> new ComputationCellComponentItem(new Item.Properties(), 3));

    public static final RegistryObject<ComputationCellComponentItem> COMPUTATION_CELL_COMPONENT_16TOPS = ITEMS.register(
            "computation_cell_component_16tops", () -> new ComputationCellComponentItem(new Item.Properties(), 2));

    public static final RegistryObject<ComputationCellComponentItem> COMPUTATION_CELL_COMPONENT_64TOPS = ITEMS.register(
            "computation_cell_component_64tops", () -> new ComputationCellComponentItem(new Item.Properties(), 1));

    public static final RegistryObject<ComputationCellComponentItem> COMPUTATION_CELL_COMPONENT_256TOPS = ITEMS
            .register("computation_cell_component_256tops",
                    () -> new ComputationCellComponentItem(new Item.Properties(),
                            ComputationCellComponentItem.AEV_TIER));

    public static final RegistryObject<MultiLevelEmitterPartItem> MULTI_LEVEL_EMITTER = ITEMS.register(
            "multi_level_emitter", () -> new MultiLevelEmitterPartItem(new Item.Properties()));

    public static final RegistryObject<MultiThresholdLevelEmitterPartItem> MULTI_THRESHOLD_LEVEL_EMITTER = ITEMS.register(
            "multi_threshold_level_emitter", () -> new MultiThresholdLevelEmitterPartItem(new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING = ITEMS.register(
            "crystal_reinforced_composite_machine_casing",
            () -> new BlockItem(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE = ITEMS.register("crystal_assembly_line",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_CASING = ITEMS.register(
            "crystal_assembly_line_casing",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_UNIT = ITEMS.register(
            "crystal_assembly_line_unit",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_CHARGING_UNIT = ITEMS.register(
            "crystal_assembly_line_charging_unit",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CHARGING_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_ADVANCED_CHARGING_UNIT = ITEMS.register(
            "crystal_assembly_line_advanced_charging_unit",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_ADVANCED_CHARGING_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_PARALLEL_UNIT = ITEMS.register(
            "crystal_assembly_line_parallel_unit",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_PARALLEL_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_SPEED_UNIT = ITEMS.register(
            "crystal_assembly_line_speed_unit",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_SPEED_UNIT.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_GRATING = ITEMS.register(
            "crystal_assembly_line_grating",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_GLASS = ITEMS.register("crystal_glass",
            () -> new BlockItem(ModBlocks.CRYSTAL_GLASS.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> FIREPROOF_CRYSTAL_GLASS = ITEMS.register("fireproof_crystal_glass",
            () -> new BlockItem(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_INPUT_BUS = ITEMS.register("certus_quartz_crystal_input_bus",
            () -> new BlockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH = ITEMS.register(
            "certus_quartz_crystal_input_hatch",
            () -> new BlockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS = ITEMS.register("certus_quartz_crystal_output_bus",
            () -> new BlockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH = ITEMS.register(
            "certus_quartz_crystal_output_hatch",
            () -> new BlockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> AEV_MACHINE_CASING = ITEMS.register("aev_machine_casing",
            () -> new BlockItem(ModBlocks.AEV_MACHINE_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> REINFORCED_HIGH_POWER_CASING = ITEMS.register(
            "reinforced_high_power_casing",
            () -> new BlockItem(ModBlocks.REINFORCED_HIGH_POWER_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> HIGH_ENERGY_STABILITY_CASING = ITEMS.register(
            "high_energy_stability_casing",
            () -> new BlockItem(ModBlocks.HIGH_ENERGY_STABILITY_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> ZIRCONIUM_IRON_ALLOY_MACHINE_CASING = ITEMS.register(
            "zirconium_iron_alloy_machine_casing",
            () -> new BlockItem(ModBlocks.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> ME_COMPUTER_CASING = ITEMS.register("me_computer_casing",
            () -> new BlockItem(ModBlocks.ME_COMPUTER_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> ME_ADVANCED_COMPUTER_CASING = ITEMS.register(
            "me_advanced_computer_casing",
            () -> new BlockItem(ModBlocks.ME_ADVANCED_COMPUTER_CASING.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> AEV_INPUT_BUS = ITEMS.register("aev_input_bus",
            () -> new BlockItem(ModBlocks.AEV_INPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> AEV_OUTPUT_BUS = ITEMS.register("aev_output_bus",
            () -> new BlockItem(ModBlocks.AEV_OUTPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> AEV_INPUT_HATCH = ITEMS.register("aev_input_hatch",
            () -> new BlockItem(ModBlocks.AEV_INPUT_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> AEV_OUTPUT_HATCH = ITEMS.register("aev_output_hatch",
            () -> new BlockItem(ModBlocks.AEV_OUTPUT_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> AEV_DUAL_INPUT_HATCH = ITEMS.register("aev_dual_input_hatch",
            () -> new BlockItem(ModBlocks.AEV_DUAL_INPUT_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> AEV_DUAL_OUTPUT_HATCH = ITEMS.register("aev_dual_output_hatch",
            () -> new BlockItem(ModBlocks.AEV_DUAL_OUTPUT_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> ME_NETWORK_ACCESS_HATCH = ITEMS.register(
            "me_network_access_hatch",
            () -> new BlockItem(ModBlocks.ME_NETWORK_ACCESS_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> HIGH_REACTIVITY_LAVA_SMELTER = ITEMS.register(
            "high_reactivity_lava_smelter",
            () -> new BlockItem(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> LOGISTICS_CONTROL_CASING = ITEMS.register(
            "logistics_control_casing",
            () -> new BlockItem(ModBlocks.LOGISTICS_CONTROL_CASING.get(), new Item.Properties()));

    public static final RegistryObject<Item> METEOR_STEEL_INGOT = ITEMS.register("meteor_steel_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZIRCONIUM_INGOT = ITEMS.register("zirconium_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZIRCONIUM_IRON_ALLOY_INGOT = ITEMS.register("zirconium_iron_alloy_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> METEOR_STEEL_PLATE = ITEMS.register("meteor_steel_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZIRCONIUM_PLATE = ITEMS.register("zirconium_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FINE_FLUIX_WIRE = ITEMS.register("fine_fluix_wire",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SINGLE_PLATE_PRESS = ITEMS.register("single_plate_press",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DIAMOND_WAFER = ITEMS.register("diamond_wafer",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DIAMOND_SOC_WAFER = ITEMS.register("diamond_soc_wafer",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DIAMOND_SOC = ITEMS.register("diamond_soc",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<CrystalProcessorItem> CRYSTAL_PROCESSOR = ITEMS.register("crystal_processor",
            () -> new CrystalProcessorItem(new Item.Properties(), "aev_d2"));

    public static final RegistryObject<CrystalProcessorItem> CRYSTAL_PROCESSOR_ASSEMBLY = ITEMS.register(
            "crystal_processor_assembly", () -> new CrystalProcessorItem(new Item.Properties(), "aev_d1"));

    public static final RegistryObject<CrystalProcessorItem> CRYSTAL_PROCESSOR_COMPUTER = ITEMS.register(
            "crystal_processor_computer", () -> new CrystalProcessorItem(new Item.Properties(), "aev"));

    public static final RegistryObject<CrystalProcessorItem> CRYSTAL_PROCESSOR_MAINFRAME = ITEMS.register(
            "crystal_processor_mainframe", () -> new CrystalProcessorItem(new Item.Properties(), "xpv"));

    public static final RegistryObject<Item> ZIRCON_CIRCUIT_BOARD = ITEMS.register("zircon_circuit_board",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZIRCON_PRINTED_CIRCUIT_BOARD = ITEMS.register(
            "zircon_printed_circuit_board", () -> new Item(new Item.Properties()));


    public static final RegistryObject<Item> UNCHARGED_SINGULARITY = ITEMS.register("uncharged_singularity",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HIGH_ENERGY_SINGULARITY = ITEMS.register("high_energy_singularity",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<BucketItem> ALIEN_LAVA_BUCKET = ITEMS.register("alien_lava_bucket",
            () -> new BucketItem(ModFluids.ALIEN_LAVA,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> LIQUID_CERTUS_QUARTZ_BUCKET = ITEMS.register(
            "liquid_certus_quartz_bucket", () -> new NoBlockFluidBucketItem(ModFluids.LIQUID_CERTUS_QUARTZ,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> LIQUID_FLUIX_CRYSTAL_BUCKET = ITEMS.register(
            "liquid_fluix_crystal_bucket", () -> new NoBlockFluidBucketItem(ModFluids.LIQUID_FLUIX_CRYSTAL,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> LIQUID_AMETHYST_BUCKET = ITEMS.register(
            "liquid_amethyst_bucket", () -> new NoBlockFluidBucketItem(ModFluids.LIQUID_AMETHYST,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> LIQUID_CRYSTALINE_BUCKET = ITEMS.register(
            "liquid_crystaline_bucket", () -> new NoBlockFluidBucketItem(ModFluids.LIQUID_CRYSTALINE,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> CRYSTAL_ADHESIVE_BUCKET = ITEMS.register(
            "crystal_adhesive_bucket", () -> new NoBlockFluidBucketItem(ModFluids.CRYSTAL_ADHESIVE,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> LIQUID_ZIRCONIUM_BUCKET = ITEMS.register(
            "liquid_zirconium_bucket", () -> new NoBlockFluidBucketItem(ModFluids.LIQUID_ZIRCONIUM,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BucketItem> COARSE_METEORITE_SLURRY_BUCKET = ITEMS.register(
            "coarse_meteorite_slurry_bucket", () -> new NoBlockFluidBucketItem(ModFluids.COARSE_METEORITE_SLURRY,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<BlockItem> NAMING_FACTORY = ITEMS.register("naming_factory",
            () -> new BlockItem(ModBlocks.NAMING_FACTORY.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEORITE_IRON_ORE = ITEMS.register("meteorite_iron_ore",
            () -> new BlockItem(ModBlocks.METEORITE_IRON_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEORITE_COPPER_ORE = ITEMS.register("meteorite_copper_ore",
            () -> new BlockItem(ModBlocks.METEORITE_COPPER_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEORITE_GOLD_ORE = ITEMS.register("meteorite_gold_ore",
            () -> new BlockItem(ModBlocks.METEORITE_GOLD_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEORITE_LAPIS_ORE = ITEMS.register("meteorite_lapis_ore",
            () -> new BlockItem(ModBlocks.METEORITE_LAPIS_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEORITE_DIAMOND_ORE = ITEMS.register("meteorite_diamond_ore",
            () -> new BlockItem(ModBlocks.METEORITE_DIAMOND_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEORITE_EMERALD_ORE = ITEMS.register("meteorite_emerald_ore",
            () -> new BlockItem(ModBlocks.METEORITE_EMERALD_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEORITE_ZIRCON_ORE = ITEMS.register("meteorite_zircon_ore",
            () -> new BlockItem(ModBlocks.METEORITE_ZIRCON_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ZIRCON = ITEMS.register("zircon", () -> new Item(new Item.Properties()));

    public static final RegistryObject<CrystalineItem> CRYSTALINE = ITEMS.register("crystaline",
            () -> new CrystalineItem(new Item.Properties()));

    public static final RegistryObject<BlockItem> METEOR_STEEL_BLOCK = ITEMS.register("meteor_steel_block",
            () -> new BlockItem(ModBlocks.METEOR_STEEL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METEOR_STEEL_PIPE_BLOCK = ITEMS.register("meteor_steel_pipe_block",
            () -> new BlockItem(ModBlocks.METEOR_STEEL_PIPE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> ZIRCON_BLOCK = ITEMS.register("zircon_block",
            () -> new BlockItem(ModBlocks.ZIRCON_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> DENSE_SKY_STONE_BLOCK = ITEMS.register("dense_sky_stone_block",
            () -> new BlockItem(ModBlocks.DENSE_SKY_STONE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> METEOR_STEEL_DUST = ITEMS.register("meteor_steel_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COAL_DUST = ITEMS.register("coal_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CARBON_DUST = ITEMS.register("carbon_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZIRCONIUM_DUST = ITEMS.register("zirconium_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CARBON_ZIRCONIUM_MIXTURE_DUST = ITEMS.register(
            "carbon_zirconium_mixture_dust", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZIRCON_DUST = ITEMS.register("zircon_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COPPER_OXIDE_DUST = ITEMS.register("copper_oxide_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> IRON_OXIDE_DUST = ITEMS.register("iron_oxide_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GOLD_OXIDE_DUST = ITEMS.register("gold_oxide_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZIRCON_BRICK = ITEMS.register("zircon_brick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<BlockItem> HIGH_ENERGY_CORE = ITEMS.register("high_energy_core",
            () -> new BlockItem(ModBlocks.HIGH_ENERGY_CORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> MATTER_PHASE_TRANSFORMER = ITEMS.register("matter_phase_transformer",
            () -> new BlockItem(ModBlocks.MATTER_PHASE_TRANSFORMER.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_REACTION_CHAMBER = ITEMS.register("crystal_reaction_chamber",
            () -> new BlockItem(ModBlocks.CRYSTAL_REACTION_CHAMBER.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_WORKSTATION = ITEMS.register("crystal_workstation",
            () -> new BlockItem(ModBlocks.CRYSTAL_WORKSTATION.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> JIG = ITEMS.register("jig",
            () -> new BlockItem(ModBlocks.JIG.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> ZIRCONIA_CORUNDUM_BRICKS = ITEMS.register("zirconia_corundum_bricks",
            () -> new BlockItem(ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get(), new Item.Properties()));
}
