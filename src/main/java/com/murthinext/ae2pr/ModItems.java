package com.murthinext.ae2pr;

import com.murthinext.ae2pr.item.level_emitter.MultiLevelEmitterPartItem;
import com.murthinext.ae2pr.item.level_emitter.MultiThresholdLevelEmitterPartItem;
import com.murthinext.ae2pr.item.filter_cell.AdvancedFilterCellItem;
import com.murthinext.ae2pr.item.filter_cell.FilterCellItem;
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

    /** 陨钢锭 */
    public static final RegistryObject<Item> METEOR_STEEL_INGOT = ITEMS.register("meteor_steel_ingot",
            () -> new Item(new Item.Properties()));

    /** 异星熔岩桶 */
    public static final RegistryObject<BucketItem> ALIEN_LAVA_BUCKET = ITEMS.register("alien_lava_bucket",
            () -> new BucketItem(ModFluids.ALIEN_LAVA,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /** 名称压印工厂 */
    public static final RegistryObject<BlockItem> NAMING_FACTORY = ITEMS.register("naming_factory",
            () -> new BlockItem(ModBlocks.NAMING_FACTORY.get(), new Item.Properties()));
}
