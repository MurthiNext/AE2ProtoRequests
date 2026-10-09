package com.murthinext.ae2pr;

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

    /** 过滤元件 */
    public static final RegistryObject<FilterCellItem> FILTER_CELL = ITEMS.register("filter_cell",
            () -> new FilterCellItem(new Item.Properties().stacksTo(1)));

    /** 高级过滤元件 */
    public static final RegistryObject<AdvancedFilterCellItem> ADVANCED_FILTER_CELL = ITEMS.register(
            "advanced_filter_cell", () -> new AdvancedFilterCellItem(new Item.Properties().stacksTo(1)));

    /** 源始终端 */
    public static final RegistryObject<Item> PROTO_TERMINAL = ITEMS.register("proto_terminal",
            () -> new ProtoTerminalItem(new Item.Properties().stacksTo(1)));

    /** 1TOPS ME计算组件 */
    public static final RegistryObject<Item> COMPUTATION_CELL_COMPONENT_1TOPS = ITEMS.register("computation_cell_component_1tops",
            () -> new Item(new Item.Properties()));

    /** 4TOPS ME计算组件 */
    public static final RegistryObject<Item> COMPUTATION_CELL_COMPONENT_4TOPS = ITEMS.register("computation_cell_component_4tops",
            () -> new Item(new Item.Properties()));

    /** 16TOPS ME计算组件 */
    public static final RegistryObject<Item> COMPUTATION_CELL_COMPONENT_16TOPS = ITEMS.register("computation_cell_component_16tops",
            () -> new Item(new Item.Properties()));

    /** 64TOPS ME计算组件 */
    public static final RegistryObject<Item> COMPUTATION_CELL_COMPONENT_64TOPS = ITEMS.register("computation_cell_component_64tops",
            () -> new Item(new Item.Properties()));

    /** 256TOPS ME计算组件 */
    public static final RegistryObject<Item> COMPUTATION_CELL_COMPONENT_256TOPS = ITEMS.register("computation_cell_component_256tops",
            () -> new Item(new Item.Properties()));

    /** ME 通式标准发信器 */
    public static final RegistryObject<MultiLevelEmitterPartItem> MULTI_LEVEL_EMITTER = ITEMS.register(
            "multi_level_emitter", () -> new MultiLevelEmitterPartItem(new Item.Properties()));

    /** ME 通式阈值发信器 */
    public static final RegistryObject<MultiThresholdLevelEmitterPartItem> MULTI_THRESHOLD_LEVEL_EMITTER = ITEMS.register(
            "multi_threshold_level_emitter", () -> new MultiThresholdLevelEmitterPartItem(new Item.Properties()));

    /** ME 红石请求器 */
    public static final RegistryObject<BlockItem> REDSTONE_REQUESTER = ITEMS.register("redstone_requester",
            () -> new BlockItem(ModBlocks.REDSTONE_REQUESTER.get(), new Item.Properties()));

    /** 水晶强化复合机械方块 */
    public static final RegistryObject<BlockItem> CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING = ITEMS.register(
            "crystal_reinforced_composite_machine_casing",
            () -> new BlockItem(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get(), new Item.Properties()));

    /** 水晶装配线（控制器） */
    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE = ITEMS.register("crystal_assembly_line",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get(), new Item.Properties()));

    /** 水晶装配线外壳 */
    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_CASING = ITEMS.register(
            "crystal_assembly_line_casing",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CASING.get(), new Item.Properties()));

    /** 水晶装配线控制外壳 */
    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_UNIT = ITEMS.register(
            "crystal_assembly_line_unit",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT.get(), new Item.Properties()));

    /** 水晶装配线格栅 */
    public static final RegistryObject<BlockItem> CRYSTAL_ASSEMBLY_LINE_GRATING = ITEMS.register(
            "crystal_assembly_line_grating",
            () -> new BlockItem(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get(), new Item.Properties()));

    /** 水晶玻璃 */
    public static final RegistryObject<BlockItem> CRYSTAL_GLASS = ITEMS.register("crystal_glass",
            () -> new BlockItem(ModBlocks.CRYSTAL_GLASS.get(), new Item.Properties()));

    /** 耐火水晶玻璃 */
    public static final RegistryObject<BlockItem> FIREPROOF_CRYSTAL_GLASS = ITEMS.register("fireproof_crystal_glass",
            () -> new BlockItem(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get(), new Item.Properties()));

    /** 赛特斯石英水晶输入总线 */
    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_INPUT_BUS = ITEMS.register("certus_quartz_crystal_input_bus",
            () -> new BlockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), new Item.Properties()));

    /** 赛特斯石英水晶输入仓 */
    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH = ITEMS.register(
            "certus_quartz_crystal_input_hatch",
            () -> new BlockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), new Item.Properties()));

    /** 赛特斯石英水晶输出总线 */
    public static final RegistryObject<BlockItem> CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS = ITEMS.register("certus_quartz_crystal_output_bus",
            () -> new BlockItem(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), new Item.Properties()));

    /** 福鲁伊克斯水晶能源仓 */
    public static final RegistryObject<BlockItem> FLUIX_CRYSTAL_ENERGY_HATCH = ITEMS.register(
            "fluix_crystal_energy_hatch",
            () -> new BlockItem(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get(), new Item.Properties()));

    /** 高反应性熔岩冶炼炉 */
    public static final RegistryObject<BlockItem> HIGH_REACTIVITY_LAVA_SMELTER = ITEMS.register(
            "high_reactivity_lava_smelter",
            () -> new BlockItem(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get(), new Item.Properties()));

    /** 物流控制机械方块 */
    public static final RegistryObject<BlockItem> LOGISTICS_CONTROL_CASING = ITEMS.register(
            "logistics_control_casing",
            () -> new BlockItem(ModBlocks.LOGISTICS_CONTROL_CASING.get(), new Item.Properties()));

    /** 陨钢锭 */
    public static final RegistryObject<Item> METEOR_STEEL_INGOT = ITEMS.register("meteor_steel_ingot",
            () -> new Item(new Item.Properties()));

    /** 锆锭 */
    public static final RegistryObject<Item> ZIRCONIUM_INGOT = ITEMS.register("zirconium_ingot",
            () -> new Item(new Item.Properties()));

    /** 陨钢板 */
    public static final RegistryObject<Item> METEOR_STEEL_PLATE = ITEMS.register("meteor_steel_plate",
            () -> new Item(new Item.Properties()));

    /** 单层板压印模板 */
    public static final RegistryObject<Item> SINGLE_PLATE_PRESS = ITEMS.register("single_plate_press",
            () -> new Item(new Item.Properties()));

    /** 异星熔岩桶 */
    public static final RegistryObject<BucketItem> ALIEN_LAVA_BUCKET = ITEMS.register("alien_lava_bucket",
            () -> new BucketItem(ModFluids.ALIEN_LAVA,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /** 名称压印工厂 */
    public static final RegistryObject<BlockItem> NAMING_FACTORY = ITEMS.register("naming_factory",
            () -> new BlockItem(ModBlocks.NAMING_FACTORY.get(), new Item.Properties()));

    /** 陨石铁矿石 */
    public static final RegistryObject<BlockItem> METEORITE_IRON_ORE = ITEMS.register("meteorite_iron_ore",
            () -> new BlockItem(ModBlocks.METEORITE_IRON_ORE.get(), new Item.Properties()));

    /** 陨石铜矿石 */
    public static final RegistryObject<BlockItem> METEORITE_COPPER_ORE = ITEMS.register("meteorite_copper_ore",
            () -> new BlockItem(ModBlocks.METEORITE_COPPER_ORE.get(), new Item.Properties()));

    /** 陨石金矿石 */
    public static final RegistryObject<BlockItem> METEORITE_GOLD_ORE = ITEMS.register("meteorite_gold_ore",
            () -> new BlockItem(ModBlocks.METEORITE_GOLD_ORE.get(), new Item.Properties()));

    /** 陨石青金石矿石 */
    public static final RegistryObject<BlockItem> METEORITE_LAPIS_ORE = ITEMS.register("meteorite_lapis_ore",
            () -> new BlockItem(ModBlocks.METEORITE_LAPIS_ORE.get(), new Item.Properties()));

    /** 陨石钻石矿石 */
    public static final RegistryObject<BlockItem> METEORITE_DIAMOND_ORE = ITEMS.register("meteorite_diamond_ore",
            () -> new BlockItem(ModBlocks.METEORITE_DIAMOND_ORE.get(), new Item.Properties()));

    /** 陨石绿宝石矿石 */
    public static final RegistryObject<BlockItem> METEORITE_EMERALD_ORE = ITEMS.register("meteorite_emerald_ore",
            () -> new BlockItem(ModBlocks.METEORITE_EMERALD_ORE.get(), new Item.Properties()));

    /** 陨石锆英石矿石 */
    public static final RegistryObject<BlockItem> METEORITE_ZIRCON_ORE = ITEMS.register("meteorite_zircon_ore",
            () -> new BlockItem(ModBlocks.METEORITE_ZIRCON_ORE.get(), new Item.Properties()));

    /** 锆英石 */
    public static final RegistryObject<Item> ZIRCON = ITEMS.register("zircon", () -> new Item(new Item.Properties()));

    /** 陨钢块 */
    public static final RegistryObject<BlockItem> METEOR_STEEL_BLOCK = ITEMS.register("meteor_steel_block",
            () -> new BlockItem(ModBlocks.METEOR_STEEL_BLOCK.get(), new Item.Properties()));

    /** 陨钢管道方块 */
    public static final RegistryObject<BlockItem> METEOR_STEEL_PIPE_BLOCK = ITEMS.register("meteor_steel_pipe_block",
            () -> new BlockItem(ModBlocks.METEOR_STEEL_PIPE_BLOCK.get(), new Item.Properties()));

    /** 锆英石块 */
    public static final RegistryObject<BlockItem> ZIRCON_BLOCK = ITEMS.register("zircon_block",
            () -> new BlockItem(ModBlocks.ZIRCON_BLOCK.get(), new Item.Properties()));

    /** 陨钢粉 */
    public static final RegistryObject<Item> METEOR_STEEL_DUST = ITEMS.register("meteor_steel_dust",
            () -> new Item(new Item.Properties()));

    /** 煤炭粉 */
    public static final RegistryObject<Item> COAL_DUST = ITEMS.register("coal_dust",
            () -> new Item(new Item.Properties()));

    /** 碳粉 */
    public static final RegistryObject<Item> CARBON_DUST = ITEMS.register("carbon_dust",
            () -> new Item(new Item.Properties()));

    /** 锆粉 */
    public static final RegistryObject<Item> ZIRCONIUM_DUST = ITEMS.register("zirconium_dust",
            () -> new Item(new Item.Properties()));

    /** 碳锆混合物粉 */
    public static final RegistryObject<Item> CARBON_ZIRCONIUM_MIXTURE_DUST = ITEMS.register(
            "carbon_zirconium_mixture_dust", () -> new Item(new Item.Properties()));

    /** 锆英石粉 */
    public static final RegistryObject<Item> ZIRCON_DUST = ITEMS.register("zircon_dust",
            () -> new Item(new Item.Properties()));

    /** 锆英石砖 */
    public static final RegistryObject<Item> ZIRCON_BRICK = ITEMS.register("zircon_brick",
            () -> new Item(new Item.Properties()));

    /** 锆刚玉砖块 */
    public static final RegistryObject<BlockItem> ZIRCONIA_CORUNDUM_BRICKS = ITEMS.register("zirconia_corundum_bricks",
            () -> new BlockItem(ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get(), new Item.Properties()));
}
