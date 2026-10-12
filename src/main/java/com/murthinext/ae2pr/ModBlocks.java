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

import com.murthinext.ae2pr.block.DisassemblableBlock;
import com.murthinext.ae2pr.block.DisassemblableGlassBlock;
import com.murthinext.ae2pr.block.alien_lava.AlienLavaBlock;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineModuleBlock;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineUnitBlock;
import com.murthinext.ae2pr.block.assembly_line.CrystalAssemblyLineBlock;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberBlock;
import com.murthinext.ae2pr.block.lava_smelter.HighReactivityLavaSmelterBlock;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerBlock;
import com.murthinext.ae2pr.block.meteor_steel_pipe.MeteorSteelPipeBlock;
import com.murthinext.ae2pr.block.meteorite.MeteoriteOreBlock;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlock;
import com.murthinext.ae2pr.multiblock.module.ModuleDefinition;

/**
 * 方块注册入口。
 */
public final class ModBlocks {

    private ModBlocks() {
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ae2pr.MODID);

    public static final RegistryObject<Block> CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING = BLOCKS.register(
            "crystal_reinforced_composite_machine_casing", () -> new DisassemblableBlock(casingProperties()));

    public static final RegistryObject<CrystalAssemblyLineBlock> CRYSTAL_ASSEMBLY_LINE = BLOCKS.register(
            "crystal_assembly_line", CrystalAssemblyLineBlock::new);

    public static final RegistryObject<Block> CRYSTAL_ASSEMBLY_LINE_CASING = BLOCKS.register(
            "crystal_assembly_line_casing", () -> new DisassemblableBlock(casingProperties()));

    public static final RegistryObject<AssemblyLineUnitBlock> CRYSTAL_ASSEMBLY_LINE_UNIT = BLOCKS.register(
            "crystal_assembly_line_unit", AssemblyLineUnitBlock::new);

    public static final RegistryObject<AssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_CHARGING_UNIT = BLOCKS.register(
            "crystal_assembly_line_charging_unit",
            () -> new AssemblyLineModuleBlock(ModuleDefinition.of(ModModules.CHARGING, 1)));

    public static final RegistryObject<AssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_ADVANCED_CHARGING_UNIT = BLOCKS
            .register("crystal_assembly_line_advanced_charging_unit",
                    () -> new AssemblyLineModuleBlock(ModuleDefinition.of(ModModules.CHARGING, 2)));

    public static final RegistryObject<AssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_PARALLEL_UNIT = BLOCKS.register(
            "crystal_assembly_line_parallel_unit",
            () -> new AssemblyLineModuleBlock(ModuleDefinition.of(ModModules.PARALLEL, 1)));

    public static final RegistryObject<AssemblyLineModuleBlock> CRYSTAL_ASSEMBLY_LINE_SPEED_UNIT = BLOCKS.register(
            "crystal_assembly_line_speed_unit",
            () -> new AssemblyLineModuleBlock(ModuleDefinition.of(ModModules.SPEED, 1)));

    public static final RegistryObject<Block> CRYSTAL_ASSEMBLY_LINE_GRATING = BLOCKS.register(
            "crystal_assembly_line_grating", () -> new DisassemblableBlock(casingProperties()));

    public static final RegistryObject<GlassBlock> CRYSTAL_GLASS = BLOCKS.register(
            "crystal_glass",
            () -> new DisassemblableGlassBlock(Block.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.8F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final RegistryObject<GlassBlock> FIREPROOF_CRYSTAL_GLASS = BLOCKS.register(
            "fireproof_crystal_glass",
            () -> new DisassemblableGlassBlock(Block.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.8F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final RegistryObject<MachinePartBlock> CERTUS_QUARTZ_CRYSTAL_INPUT_BUS = BLOCKS.register(
            "certus_quartz_crystal_input_bus", MachinePartBlock::new);

    public static final RegistryObject<MachinePartBlock> CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH = BLOCKS.register(
            "certus_quartz_crystal_input_hatch", MachinePartBlock::new);

    public static final RegistryObject<MachinePartBlock> CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS = BLOCKS.register(
            "certus_quartz_crystal_output_bus", MachinePartBlock::new);

    public static final RegistryObject<MachinePartBlock> CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH = BLOCKS.register(
            "certus_quartz_crystal_output_hatch", MachinePartBlock::new);

    public static final RegistryObject<Block> AEV_MACHINE_CASING = BLOCKS.register(
            "aev_machine_casing", () -> new DisassemblableBlock(casingProperties()));

    public static final RegistryObject<MachinePartBlock> AEV_INPUT_BUS = BLOCKS.register(
            "aev_input_bus", MachinePartBlock::new);

    public static final RegistryObject<MachinePartBlock> AEV_OUTPUT_BUS = BLOCKS.register(
            "aev_output_bus", MachinePartBlock::new);

    public static final RegistryObject<MachinePartBlock> AEV_INPUT_HATCH = BLOCKS.register(
            "aev_input_hatch", MachinePartBlock::new);

    public static final RegistryObject<MachinePartBlock> AEV_OUTPUT_HATCH = BLOCKS.register(
            "aev_output_hatch", MachinePartBlock::new);

    public static final RegistryObject<FluixCrystalEnergyHatchBlock> FLUIX_CRYSTAL_ENERGY_HATCH = BLOCKS.register(
            "fluix_crystal_energy_hatch", FluixCrystalEnergyHatchBlock::new);

    public static final RegistryObject<HighReactivityLavaSmelterBlock> HIGH_REACTIVITY_LAVA_SMELTER = BLOCKS.register(
            "high_reactivity_lava_smelter", HighReactivityLavaSmelterBlock::new);

    public static final RegistryObject<Block> LOGISTICS_CONTROL_CASING = BLOCKS.register(
            "logistics_control_casing", () -> new DisassemblableBlock(Block.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

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

    public static final RegistryObject<NamingFactoryBlock> NAMING_FACTORY = BLOCKS.register(
            "naming_factory", NamingFactoryBlock::new);

    public static final RegistryObject<MeteoriteOreBlock> METEORITE_IRON_ORE = BLOCKS.register(
            "meteorite_iron_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    public static final RegistryObject<MeteoriteOreBlock> METEORITE_COPPER_ORE = BLOCKS.register(
            "meteorite_copper_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    public static final RegistryObject<MeteoriteOreBlock> METEORITE_GOLD_ORE = BLOCKS.register(
            "meteorite_gold_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), ConstantInt.of(0)));

    public static final RegistryObject<MeteoriteOreBlock> METEORITE_LAPIS_ORE = BLOCKS.register(
            "meteorite_lapis_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(2, 5)));

    public static final RegistryObject<MeteoriteOreBlock> METEORITE_DIAMOND_ORE = BLOCKS.register(
            "meteorite_diamond_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    public static final RegistryObject<MeteoriteOreBlock> METEORITE_EMERALD_ORE = BLOCKS.register(
            "meteorite_emerald_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    public static final RegistryObject<MeteoriteOreBlock> METEORITE_ZIRCON_ORE = BLOCKS.register(
            "meteorite_zircon_ore", () -> new MeteoriteOreBlock(meteoriteOreProperties(), UniformInt.of(3, 7)));

    public static final RegistryObject<Block> METEOR_STEEL_BLOCK = BLOCKS.register(
            "meteor_steel_block", () -> new Block(storageBlockProperties(MapColor.METAL)));

    public static final RegistryObject<MeteorSteelPipeBlock> METEOR_STEEL_PIPE_BLOCK = BLOCKS.register(
            "meteor_steel_pipe_block", MeteorSteelPipeBlock::new);

    public static final RegistryObject<Block> ZIRCON_BLOCK = BLOCKS.register(
            "zircon_block", () -> new Block(storageBlockProperties(MapColor.COLOR_LIGHT_GRAY)));

    public static final RegistryObject<Block> DENSE_SKY_STONE_BLOCK = BLOCKS.register(
            "dense_sky_stone_block", () -> new Block(Block.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(5.0F, 150.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> HIGH_ENERGY_CORE = BLOCKS.register(
            "high_energy_core", () -> new DisassemblableBlock(casingProperties()));

    public static final RegistryObject<MatterPhaseTransformerBlock> MATTER_PHASE_TRANSFORMER = BLOCKS.register(
            "matter_phase_transformer", MatterPhaseTransformerBlock::new);

    public static final RegistryObject<CrystalReactionChamberBlock> CRYSTAL_REACTION_CHAMBER = BLOCKS.register(
            "crystal_reaction_chamber", CrystalReactionChamberBlock::new);

    public static final RegistryObject<Block> ZIRCONIA_CORUNDUM_BRICKS = BLOCKS.register(
            "zirconia_corundum_bricks", () -> new DisassemblableBlock(Block.Properties.of()
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
