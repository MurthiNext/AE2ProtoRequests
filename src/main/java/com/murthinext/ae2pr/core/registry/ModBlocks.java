package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.BlockProperties;
import com.murthinext.ae2pr.api.multiblock.module.ModuleDefinition;
import com.murthinext.ae2pr.block.alien_lava.AlienLavaBlock;
import com.murthinext.ae2pr.block.casing.CarbonFiberMachineFrameBlock;
import com.murthinext.ae2pr.block.casing.ComputerCasingBlock;
import com.murthinext.ae2pr.block.DisassemblableBlock;
import com.murthinext.ae2pr.block.DisassemblableGlassBlock;
import com.murthinext.ae2pr.block.feature.filter_press.FilterPressPlateBlock;
import com.murthinext.ae2pr.block.feature.MeteorSteelPipeBlock;
import com.murthinext.ae2pr.block.casing.StabilityCasingBlock;
import com.murthinext.ae2pr.block.casing.EntangledCasingBlock;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock.Storage;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock.Tier;
import com.murthinext.ae2pr.block.ore.MeteoriteOreBlock;
import com.murthinext.ae2pr.block.feature.molecular_assembly_core.MolecularAssemblyCoreBlock;
import com.murthinext.ae2pr.item.VoltageBlockItem;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineModuleBlock;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineUnitBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ae2pr.MODID);

    // 静态初始化按文本顺序执行，收集器必须在第一个 block(...) 之前
    private static final List<BlockDef<?>> ALL = new ArrayList<>();

    public static final BlockDef<Block> CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING = block(
            "crystal_reinforced_composite_machine_casing",
            BlockGroup.CASING,
            () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<Block> CRYSTAL_ASSEMBLY_LINE_CASING = block(
            "crystal_assembly_line_casing", BlockGroup.FEATURE, () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<CrystalAssemblyLineUnitBlock> CRYSTAL_ASSEMBLY_LINE_UNIT =
            block("crystal_assembly_line_unit", BlockGroup.FEATURE, CrystalAssemblyLineUnitBlock::new);

    public static final BlockDef<CrystalAssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_CHARGING_UNIT = block(
            "crystal_assembly_line_charging_unit",
            BlockGroup.FEATURE,
            () -> new CrystalAssemblyLineModuleBlock(ModuleDefinition.of(ModModules.CHARGING, 1)));

    public static final BlockDef<CrystalAssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_ADVANCED_CHARGING_UNIT = block(
            "crystal_assembly_line_advanced_charging_unit",
            BlockGroup.FEATURE,
            () -> new CrystalAssemblyLineModuleBlock(ModuleDefinition.of(ModModules.CHARGING, 2)));

    public static final BlockDef<CrystalAssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_PARALLEL_UNIT = block(
            "crystal_assembly_line_parallel_unit",
            BlockGroup.FEATURE,
            () -> new CrystalAssemblyLineModuleBlock(ModuleDefinition.of(ModModules.PARALLEL, 1)));

    public static final BlockDef<CrystalAssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_SPEED_UNIT = block(
            "crystal_assembly_line_speed_unit",
            BlockGroup.FEATURE,
            () -> new CrystalAssemblyLineModuleBlock(ModuleDefinition.of(ModModules.SPEED, 1)));

    public static final BlockDef<Block> CRYSTAL_ASSEMBLY_LINE_GRATING = block(
            "crystal_assembly_line_grating", BlockGroup.FEATURE, () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<GlassBlock> CRYSTAL_GLASS = block(
            "crystal_glass",
            BlockGroup.GLASS,
            () -> new DisassemblableGlassBlock(BlockProperties.glass(MapColor.COLOR_LIGHT_BLUE)));

    public static final BlockDef<GlassBlock> FIREPROOF_CRYSTAL_GLASS = block(
            "fireproof_crystal_glass",
            BlockGroup.GLASS,
            () -> new DisassemblableGlassBlock(BlockProperties.glass(MapColor.COLOR_ORANGE)));

    public static final MachinePartSet QUARTZ_PARTS = registerTier(Tier.QUARTZ);

    public static final MachinePartSet AEV_PARTS = registerTier(Tier.AEV);

    public static final MachinePartSet XPV_PARTS = registerTier(Tier.XPV);

    private static final List<MachinePartSet> TIERS = List.of(QUARTZ_PARTS, AEV_PARTS, XPV_PARTS);

    public static final BlockDef<Block> AEV_MACHINE_CASING = block(
            "aev_machine_casing", BlockGroup.CASING, () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<Block> XPV_MACHINE_CASING = block(
            "xpv_machine_casing", BlockGroup.CASING, () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<CarbonFiberMachineFrameBlock> CARBON_FIBER_MACHINE_FRAME =
            block("carbon_fiber_machine_frame", BlockGroup.CASING, CarbonFiberMachineFrameBlock::new);

    public static final BlockDef<Block> CRYSTAL_ENERGY_CASING = block(
            "crystal_energy_casing", BlockGroup.CASING, () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<Block> CRYSTAL_ENERGY_INJECTION_CASING = block(
            "crystal_energy_injection_casing", BlockGroup.CASING, () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<Block> STABILITY_CASING =
            block("stability_casing", BlockGroup.CASING, () -> new StabilityCasingBlock(casingProperties()));

    public static final BlockDef<Block> ENTANGLED_CASING =
            block("entangled_casing", BlockGroup.CASING, () -> new EntangledCasingBlock(casingProperties()));

    public static final BlockDef<Block> ZIRCONIUM_IRON_ALLOY_MACHINE_CASING = block(
            "zirconium_iron_alloy_machine_casing",
            BlockGroup.CASING,
            () -> new DisassemblableBlock(casingProperties()));

    public static final BlockDef<ComputerCasingBlock> ME_COMPUTER_CASING =
            block("me_computer_casing", BlockGroup.CASING, () -> new ComputerCasingBlock(casingProperties()));

    public static final BlockDef<ComputerCasingBlock> ME_ADVANCED_COMPUTER_CASING = block(
            "me_advanced_computer_casing", BlockGroup.CASING, () -> new ComputerCasingBlock(casingProperties()));

    public static final BlockDef<MENetworkAccessHatchBlock> ME_NETWORK_ACCESS_HATCH =
            block("me_network_access_hatch", BlockGroup.MACHINE_PART, MENetworkAccessHatchBlock::new);

    public static final BlockDef<MEPatternBufferBlock> ME_PATTERN_BUFFER =
            block("me_pattern_buffer", BlockGroup.MACHINE_PART, MEPatternBufferBlock::new);

    public static final BlockDef<Block> LOGISTICS_CONTROL_CASING = block(
            "logistics_control_casing",
            BlockGroup.FEATURE,
            () -> new DisassemblableBlock(BlockProperties.metal(MapColor.COLOR_PURPLE)));

    public static final BlockDef<AlienLavaBlock> ALIEN_LAVA = blockWithoutItem(
            "alien_lava",
            BlockGroup.NONE,
            () -> new AlienLavaBlock(
                    ModFluids.ALIEN_LAVA,
                    Block.Properties.of()
                            .mapColor(MapColor.FIRE)
                            .replaceable()
                            .noCollission()
                            .randomTicks()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .lightLevel(state -> 15)
                            .noLootTable()));

    public static final BlockDef<MeteoriteOreBlock> METEORITE_IRON_ORE = block(
            "meteorite_iron_ore",
            BlockGroup.ORE,
            () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    public static final BlockDef<MeteoriteOreBlock> METEORITE_COPPER_ORE = block(
            "meteorite_copper_ore",
            BlockGroup.ORE,
            () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    public static final BlockDef<MeteoriteOreBlock> METEORITE_GOLD_ORE = block(
            "meteorite_gold_ore",
            BlockGroup.ORE,
            () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    public static final BlockDef<MeteoriteOreBlock> METEORITE_LAPIS_ORE = block(
            "meteorite_lapis_ore",
            BlockGroup.ORE,
            () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(2, 5)));

    public static final BlockDef<MeteoriteOreBlock> METEORITE_DIAMOND_ORE = block(
            "meteorite_diamond_ore",
            BlockGroup.ORE,
            () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    public static final BlockDef<MeteoriteOreBlock> METEORITE_EMERALD_ORE = block(
            "meteorite_emerald_ore",
            BlockGroup.ORE,
            () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    public static final BlockDef<MeteoriteOreBlock> METEORITE_ZIRCON_ORE = block(
            "meteorite_zircon_ore",
            BlockGroup.ORE,
            () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    public static final BlockDef<Block> METEOR_STEEL_BLOCK = block(
            "meteor_steel_block", BlockGroup.STORAGE_BLOCK, () -> new Block(storageBlockProperties(MapColor.METAL)));

    public static final BlockDef<MeteorSteelPipeBlock> METEOR_STEEL_PIPE_BLOCK =
            block("meteor_steel_pipe_block", BlockGroup.FEATURE, MeteorSteelPipeBlock::new);

    public static final BlockDef<Block> ZIRCON_BLOCK = block(
            "zircon_block",
            BlockGroup.STORAGE_BLOCK,
            () -> new Block(storageBlockProperties(MapColor.COLOR_LIGHT_GRAY)));

    public static final BlockDef<Block> DENSE_SKY_STONE_BLOCK = block(
            "dense_sky_stone_block",
            BlockGroup.STORAGE_BLOCK,
            () -> new Block(Block.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(5.0F, 150.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final BlockDef<FilterPressPlateBlock> FILTER_PRESS_PLATE =
            block("filter_press_plate", BlockGroup.FEATURE, () -> new FilterPressPlateBlock(casingProperties()));

    public static final BlockDef<MolecularAssemblyCoreBlock> MOLECULAR_ASSEMBLY_CORE =
            block("molecular_assembly_core", BlockGroup.FEATURE, MolecularAssemblyCoreBlock::new);

    public static final BlockDef<Block> ZIRCONIA_CORUNDUM_BRICKS = block(
            "zirconia_corundum_bricks", BlockGroup.CASING, () -> new DisassemblableBlock(BlockProperties.stone()));

    // 结构模板无法静态引用具体仓室，初始化时注入
    public static void installStructureHooks() {
        MachinePartAppearance.install();
    }

    public static List<BlockDef<?>> all() {
        return List.copyOf(ALL);
    }

    public static List<BlockDef<?>> group(BlockGroup group) {
        return ALL.stream().filter(def -> def.group() == group).toList();
    }

    public static List<BlockDef<MachinePartBlock>> machineParts() {
        return TIERS.stream().flatMap(set -> set.all().stream()).toList();
    }

    static <T extends Block> BlockDef<T> block(String path, BlockGroup group, Supplier<T> factory) {
        RegistryObject<T> block = BLOCKS.register(path, factory);
        RegistryObject<BlockItem> item = ModItems.ITEMS.register(
                path, () -> new VoltageBlockItem(block.get(), new Item.Properties()));
        BlockDef<T> def = new BlockDef<>(path, group, block, item);
        ALL.add(def);
        return def;
    }

    static <T extends Block> BlockDef<T> blockWithoutItem(String path, BlockGroup group, Supplier<T> factory) {
        BlockDef<T> def = new BlockDef<>(path, group, BLOCKS.register(path, factory), null);
        ALL.add(def);
        return def;
    }

    private static MachinePartSet registerTier(Tier tier) {
        String prefix = tier.idPrefix();
        return new MachinePartSet(
                tier,
                part(prefix + "_input_bus", tier, Storage.ITEMS, false),
                part(prefix + "_output_bus", tier, Storage.ITEMS, true),
                part(prefix + "_input_hatch", tier, Storage.FLUIDS, false),
                part(prefix + "_output_hatch", tier, Storage.FLUIDS, true),
                tier.hasDualHatch() ? part(prefix + "_dual_input_hatch", tier, Storage.BOTH, false) : null,
                tier.hasDualHatch() ? part(prefix + "_dual_output_hatch", tier, Storage.BOTH, true) : null);
    }

    private static BlockDef<MachinePartBlock> part(String path, Tier tier, Storage storage, boolean output) {
        return block(path, BlockGroup.MACHINE_PART, () -> new MachinePartBlock(tier, storage, output));
    }

    private static Block.Properties storageBlockProperties(MapColor mapColor) {
        return BlockProperties.storageBlock(mapColor);
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
        return BlockProperties.metal();
    }
}
