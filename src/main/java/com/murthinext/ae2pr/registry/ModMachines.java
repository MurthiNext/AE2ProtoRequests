package com.murthinext.ae2pr.registry;

import java.util.List;

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.api.BlockProperties;
import com.murthinext.ae2pr.api.machine.CoreLight;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.block.HighEnergyCoreBlock;
import com.murthinext.ae2pr.api.machine.MachineControllerBlockEntity;
import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.MachineMenu;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.api.machine.RecipeConstraints;
import com.murthinext.ae2pr.api.machine.RecipeDefinition;
import com.murthinext.ae2pr.api.machine.singleblock.SingleblockMachineDefinition;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineRecipe;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_reactor.CrystalReactorCoreBlockEntity;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterMenu;
import com.murthinext.ae2pr.machine.jig.JigControllerBlockEntity;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryControllerBlockEntity;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlock;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlockEntity;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryMenu;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineStructure;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberStructure;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationStructure;
import com.murthinext.ae2pr.machine.filter_press.FilterPressStructure;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterStructure;
import com.murthinext.ae2pr.machine.jig.JigStructure;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryStructure;

public final class ModMachines {

    // 核心方块为水晶反应器时同步运行指示灯
    private static final CoreLight CRYSTAL_REACTOR_CORE = new CoreLight() {
        @Override
        public boolean matches(BlockState state) {
            return state.is(CRYSTAL_REACTOR.block());
        }

        @Override
        public void apply(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, BlockState state,
                boolean lit) {
            if (state.getValue(HighEnergyCoreBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(HighEnergyCoreBlock.LIT, lit),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
    };

    public static final RecipeDefinition<MachineRecipe> FILTER_PRESS_RECIPE = RecipeDefinition.simple(
            "filter_press", 200, RecipeConstraints.builder().requireEnergy().build());
    public static final RecipeDefinition<MachineRecipe> JIG_RECIPE = RecipeDefinition.simple(
            "jig", 200, RecipeConstraints.builder().requireEnergy().build());
    public static final RecipeDefinition<MachineRecipe> CRYSTAL_REACTION_CHAMBER_RECIPE = RecipeDefinition.simple(
            "crystal_reaction_chamber", 200, RecipeConstraints.builder().requireEnergy().build());
    public static final RecipeDefinition<MachineRecipe> HIGH_REACTIVITY_LAVA_SMELTER_RECIPE = RecipeDefinition.simple(
            "high_reactivity_lava_smelter", 100,
            RecipeConstraints.builder().itemInputs(0, 4).fluidInputs(0, 2).itemOutputs(1, 2).fluidOutputs(0, 0).build());
    public static final RecipeDefinition<MachineRecipe> MATTER_PHASE_TRANSFORMER_RECIPE = RecipeDefinition.simple(
            "matter_phase_transformer", 200,
            RecipeConstraints.builder().itemInputs(1, -1).fluidInputs(0, 0).fluidOutputs(1, 1).requireEnergy().build());
    public static final RecipeDefinition<MachineRecipe> CRYSTAL_WORKSTATION_ETCHING_RECIPE = RecipeDefinition.simple(
            "crystal_workstation_etching", 200, workstationConstraints());
    public static final RecipeDefinition<MachineRecipe> CRYSTAL_WORKSTATION_CUTTING_RECIPE = RecipeDefinition.simple(
            "crystal_workstation_cutting", 200, workstationConstraints());

    public static final RecipeDefinition<CrystalAssemblyLineRecipe> CRYSTAL_ASSEMBLY_LINE_RECIPE = RecipeDefinition.custom(
            "crystal_assembly_line", CrystalAssemblyLineRecipe.Serializer::new);

    public static final MultiblockMachineDefinition<MachineControllerBlockEntity, MachineMenu<MachineControllerBlockEntity>> FILTER_PRESS =
            MultiblockMachineDefinition.standard("filter_press", ModMachines::metalProperties,
                    () -> FilterPressStructure.INSTANCE, ModConfig::filterPressBaseParallel, FILTER_PRESS_RECIPE::type);

    public static final MultiblockMachineDefinition<JigControllerBlockEntity, MachineMenu<JigControllerBlockEntity>> JIG =
            MultiblockMachineDefinition.standard("jig", JigControllerBlockEntity.class, JigControllerBlockEntity::new,
                    ModMachines::crystalProperties, () -> JigStructure.INSTANCE, ModConfig::jigBaseParallel,
                    JIG_RECIPE::type);

    public static final MultiblockMachineDefinition<CrystalReactionChamberControllerBlockEntity, MachineMenu<CrystalReactionChamberControllerBlockEntity>> CRYSTAL_REACTION_CHAMBER =
            MultiblockMachineDefinition.standard("crystal_reaction_chamber", CrystalReactionChamberControllerBlockEntity.class,
                    CrystalReactionChamberControllerBlockEntity::new, ModMachines::crystalProperties,
                    () -> CrystalReactionChamberStructure.INSTANCE, ModConfig::crystalReactionBaseParallel,
                    CRYSTAL_REACTION_CHAMBER_RECIPE::type).coreLight(ModMachines.CRYSTAL_REACTOR_CORE);

    public static final MultiblockMachineDefinition<CrystalAssemblyLineControllerBlockEntity, MachineMenu<CrystalAssemblyLineControllerBlockEntity>> CRYSTAL_ASSEMBLY_LINE =
            MultiblockMachineDefinition.standard("crystal_assembly_line", CrystalAssemblyLineControllerBlockEntity.class,
                    CrystalAssemblyLineControllerBlockEntity::new, ModMachines::metalProperties,
                    () -> CrystalAssemblyLineStructure.INSTANCE, ModConfig::assemblyBaseParallel,
                    CRYSTAL_ASSEMBLY_LINE_RECIPE::type);

    public static final MultiblockMachineDefinition<MatterPhaseTransformerControllerBlockEntity, MachineMenu<MatterPhaseTransformerControllerBlockEntity>> MATTER_PHASE_TRANSFORMER =
            MultiblockMachineDefinition.standard("matter_phase_transformer", MatterPhaseTransformerControllerBlockEntity.class,
                    MatterPhaseTransformerControllerBlockEntity::new, ModMachines::stoneProperties,
                    () -> MatterPhaseTransformerStructure.INSTANCE, ModConfig::matterPhaseTransformerBaseParallel,
                    MATTER_PHASE_TRANSFORMER_RECIPE::type).coreLight(ModMachines.CRYSTAL_REACTOR_CORE);

    public static final MultiblockMachineDefinition<HighReactivityLavaSmelterControllerBlockEntity, HighReactivityLavaSmelterMenu> HIGH_REACTIVITY_LAVA_SMELTER =
            MultiblockMachineDefinition.customMenu("high_reactivity_lava_smelter", HighReactivityLavaSmelterControllerBlockEntity.class,
                    HighReactivityLavaSmelterControllerBlockEntity::new, ModMachines::stoneProperties,
                    () -> HighReactivityLavaSmelterStructure.INSTANCE, ModConfig::highReactivityLavaSmelterBaseParallel,
                    HIGH_REACTIVITY_LAVA_SMELTER_RECIPE::type, () -> HighReactivityLavaSmelterMenu.TYPE,
                    (definition, id, inventory, controller) -> new HighReactivityLavaSmelterMenu(id, inventory, controller));

    public static final MultiblockMachineDefinition<CrystalWorkstationControllerBlockEntity, MachineMenu<CrystalWorkstationControllerBlockEntity>> CRYSTAL_WORKSTATION =
            MultiblockMachineDefinition.withModes("crystal_workstation", CrystalWorkstationControllerBlockEntity.class,
                    CrystalWorkstationControllerBlockEntity::new, ModMachines::crystalProperties,
                    () -> CrystalWorkstationStructure.INSTANCE, ModConfig::crystalWorkstationBaseParallel,
                            CrystalWorkstationMode.ETCHING::recipeType, List.of(CrystalWorkstationMode.values()))
                    .coreLight(ModMachines.CRYSTAL_REACTOR_CORE);

    public static final MultiblockMachineDefinition<MultiFactoryControllerBlockEntity, MachineMenu<MultiFactoryControllerBlockEntity>> MULTI_FACTORY =
            MultiblockMachineDefinition.withModes("multi_factory", MultiFactoryControllerBlockEntity.class,
                    MultiFactoryControllerBlockEntity::new, ModMachines::crystalProperties,
                    () -> MultiFactoryStructure.INSTANCE, ModConfig::multiFactoryBaseParallel,
                    MultiFactoryMode.INSCRIBING::recipeType, List.of(MultiFactoryMode.values()));

    public static final SingleblockMachineDefinition<NamingFactoryBlock, NamingFactoryBlockEntity, NamingFactoryMenu> NAMING_FACTORY =
            SingleblockMachineDefinition.create("naming_factory", NamingFactoryBlockEntity.class,
                    ModMachines::namingFactoryProperties, NamingFactoryBlock::new,
                    NamingFactoryBlockEntity::new, () -> NamingFactoryMenu.TYPE,
                    (definition, id, inventory, machine) -> new NamingFactoryMenu(definition, id, inventory, machine),
                    4, NamingFactoryBlockEntity.SLOT_COUNT, ModConfig::namingFactoryMaxPower);

    public static final MachineDefinition<HighEnergyCoreBlock, CrystalReactorCoreBlockEntity, AbstractContainerMenu> CRYSTAL_REACTOR =
            MachineDefinition.custom("high_energy_core", "crystal_reactor_core", CrystalReactorCoreBlockEntity.class,
                    () -> new HighEnergyCoreBlock(metalProperties()), CrystalReactorCoreBlockEntity::new, null, null, 0);

    public static final List<MachineDefinition<?, ?, ?>> ALL = List.of(FILTER_PRESS, JIG, CRYSTAL_REACTION_CHAMBER,
            CRYSTAL_ASSEMBLY_LINE, MATTER_PHASE_TRANSFORMER, HIGH_REACTIVITY_LAVA_SMELTER, CRYSTAL_WORKSTATION,
            MULTI_FACTORY, NAMING_FACTORY, CRYSTAL_REACTOR);

    private ModMachines() {
    }

    public static void init() {
    }

    private static RecipeConstraints workstationConstraints() {
        return RecipeConstraints.builder().itemInputs(0, 6).fluidInputs(0, 3).itemOutputs(0, 6).fluidOutputs(0, 3)
                .requireEnergy().build();
    }

    private static Block.Properties metalProperties() {
        return BlockProperties.metal();
    }

    // 单方块机器多为无遮挡模型
    private static Block.Properties namingFactoryProperties() {
        return BlockProperties.metal().noOcclusion();
    }

    private static Block.Properties crystalProperties() {
        return BlockProperties.metal(MapColor.COLOR_LIGHT_BLUE);
    }

    private static Block.Properties stoneProperties() {
        return BlockProperties.stone();
    }
}
