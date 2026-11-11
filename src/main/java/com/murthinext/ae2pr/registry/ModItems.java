package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.item.ComputationCellComponentItem;
import com.murthinext.ae2pr.item.CrystalProcessorItem;
import com.murthinext.ae2pr.item.crystaline.CrystalineItem;
import com.murthinext.ae2pr.item.level_emitter.MultiLevelEmitterPartItem;
import com.murthinext.ae2pr.item.level_emitter.MultiThresholdLevelEmitterPartItem;
import com.murthinext.ae2pr.item.filter_cell.AdvancedFilterCellItem;
import com.murthinext.ae2pr.item.filter_cell.FilterCellItem;
import com.murthinext.ae2pr.item.collapsed_cell.CollapsedItemCellItem;
import com.murthinext.ae2pr.item.proto_terminal.ProtoTerminalItem;
import com.murthinext.ae2pr.item.upgrade.SuperStackCardItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ae2pr.MODID);

    private static RegistryObject<BlockItem> blockItem(RegistryObject<? extends Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static final RegistryObject<FilterCellItem> FILTER_CELL = ITEMS.register("filter_cell",
            () -> new FilterCellItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<AdvancedFilterCellItem> ADVANCED_FILTER_CELL = ITEMS.register(
            "advanced_filter_cell", () -> new AdvancedFilterCellItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PROTO_TERMINAL = ITEMS.register("proto_terminal",
            () -> new ProtoTerminalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<SuperStackCardItem> SUPER_STACK_CARD = ITEMS.register("super_stack_card",
            () -> new SuperStackCardItem(new Item.Properties()));

    public static final RegistryObject<CollapsedItemCellItem> COLLAPSED_ITEM_CELL = ITEMS.register(
            "collapsed_item_cell", () -> new CollapsedItemCellItem(new Item.Properties().stacksTo(1)));

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

    public static final RegistryObject<BlockItem> CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING = blockItem(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING);

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_CASING = blockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CASING);

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_UNIT = blockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT);

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_CHARGING_UNIT = blockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CHARGING_UNIT);

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_ADVANCED_CHARGING_UNIT = blockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_ADVANCED_CHARGING_UNIT);

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_PARALLEL_UNIT = blockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_PARALLEL_UNIT);

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_SPEED_UNIT = blockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_SPEED_UNIT);

    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_GRATING = blockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING);

    public static final RegistryObject<BlockItem> CRYSTAL_GLASS = blockItem(ModBlocks.CRYSTAL_GLASS);

    public static final RegistryObject<BlockItem> FIREPROOF_CRYSTAL_GLASS = blockItem(ModBlocks.FIREPROOF_CRYSTAL_GLASS);

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_INPUT_BUS = blockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS);

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH = blockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH);

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS = blockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS);

    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH = blockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH);

    public static final RegistryObject<BlockItem> AEV_MACHINE_CASING = blockItem(ModBlocks.AEV_MACHINE_CASING);

    public static final RegistryObject<BlockItem> CRYSTAL_ENERGY_CASING = blockItem(ModBlocks.CRYSTAL_ENERGY_CASING);

    public static final RegistryObject<BlockItem> HIGH_ENERGY_STABILITY_CASING = blockItem(ModBlocks.HIGH_ENERGY_STABILITY_CASING);

    public static final RegistryObject<BlockItem> ZIRCONIUM_IRON_ALLOY_MACHINE_CASING = blockItem(ModBlocks.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING);

    public static final RegistryObject<BlockItem> ME_COMPUTER_CASING = blockItem(ModBlocks.ME_COMPUTER_CASING);

    public static final RegistryObject<BlockItem> ME_ADVANCED_COMPUTER_CASING = blockItem(ModBlocks.ME_ADVANCED_COMPUTER_CASING);

    public static final RegistryObject<BlockItem> AEV_INPUT_BUS = blockItem(ModBlocks.AEV_INPUT_BUS);

    public static final RegistryObject<BlockItem> AEV_OUTPUT_BUS = blockItem(ModBlocks.AEV_OUTPUT_BUS);

    public static final RegistryObject<BlockItem> AEV_INPUT_HATCH = blockItem(ModBlocks.AEV_INPUT_HATCH);

    public static final RegistryObject<BlockItem> AEV_OUTPUT_HATCH = blockItem(ModBlocks.AEV_OUTPUT_HATCH);

    public static final RegistryObject<BlockItem> AEV_DUAL_INPUT_HATCH = blockItem(ModBlocks.AEV_DUAL_INPUT_HATCH);

    public static final RegistryObject<BlockItem> AEV_DUAL_OUTPUT_HATCH = blockItem(ModBlocks.AEV_DUAL_OUTPUT_HATCH);

    public static final RegistryObject<BlockItem> ME_NETWORK_ACCESS_HATCH = blockItem(ModBlocks.ME_NETWORK_ACCESS_HATCH);

    public static final RegistryObject<BlockItem> LOGISTICS_CONTROL_CASING = blockItem(ModBlocks.LOGISTICS_CONTROL_CASING);

    public static final RegistryObject<BlockItem> FILTER_PRESS_PLATE = blockItem(ModBlocks.FILTER_PRESS_PLATE);

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

    public static final RegistryObject<BlockItem> METEORITE_IRON_ORE = blockItem(ModBlocks.METEORITE_IRON_ORE);

    public static final RegistryObject<BlockItem> METEORITE_COPPER_ORE = blockItem(ModBlocks.METEORITE_COPPER_ORE);

    public static final RegistryObject<BlockItem> METEORITE_GOLD_ORE = blockItem(ModBlocks.METEORITE_GOLD_ORE);

    public static final RegistryObject<BlockItem> METEORITE_LAPIS_ORE = blockItem(ModBlocks.METEORITE_LAPIS_ORE);

    public static final RegistryObject<BlockItem> METEORITE_DIAMOND_ORE = blockItem(ModBlocks.METEORITE_DIAMOND_ORE);

    public static final RegistryObject<BlockItem> METEORITE_EMERALD_ORE = blockItem(ModBlocks.METEORITE_EMERALD_ORE);

    public static final RegistryObject<BlockItem> METEORITE_ZIRCON_ORE = blockItem(ModBlocks.METEORITE_ZIRCON_ORE);

    public static final RegistryObject<Item> ZIRCON = ITEMS.register("zircon", () -> new Item(new Item.Properties()));

    public static final RegistryObject<CrystalineItem> CRYSTALINE = ITEMS.register("crystaline",
            () -> new CrystalineItem(new Item.Properties()));

    public static final RegistryObject<BlockItem> METEOR_STEEL_BLOCK = blockItem(ModBlocks.METEOR_STEEL_BLOCK);

    public static final RegistryObject<BlockItem> METEOR_STEEL_PIPE_BLOCK = blockItem(ModBlocks.METEOR_STEEL_PIPE_BLOCK);

    public static final RegistryObject<BlockItem> ZIRCON_BLOCK = blockItem(ModBlocks.ZIRCON_BLOCK);

    public static final RegistryObject<BlockItem> DENSE_SKY_STONE_BLOCK = blockItem(ModBlocks.DENSE_SKY_STONE_BLOCK);

    public static final RegistryObject<Item> ZIRCON_BRICK = ITEMS.register("zircon_brick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<BlockItem> MOLECULAR_ASSEMBLY_CORE = blockItem(ModBlocks.MOLECULAR_ASSEMBLY_CORE);

    public static final RegistryObject<BlockItem> ZIRCONIA_CORUNDUM_BRICKS = blockItem(ModBlocks.ZIRCONIA_CORUNDUM_BRICKS);
}
