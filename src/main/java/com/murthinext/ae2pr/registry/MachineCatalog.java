package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.ServerConfig;
import com.murthinext.ae2pr.api.BlockProperties;
import com.murthinext.ae2pr.api.machine.CoreLight;
import com.murthinext.ae2pr.api.machine.MachineControllerBlockEntity;
import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.MachineSpec;
import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.api.machine.RecipeConstraints;
import com.murthinext.ae2pr.api.machine.RecipeSpec;
import com.murthinext.ae2pr.api.machine.singleblock.SingleblockMachineDefinition;
import com.murthinext.ae2pr.block.HighEnergyCoreBlock;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineRecipe;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineStructure;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberStructure;
import com.murthinext.ae2pr.machine.crystal_reactor.CrystalReactorCoreBlockEntity;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationStructure;
import com.murthinext.ae2pr.machine.filter_press.FilterPressStructure;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterMenu;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterStructure;
import com.murthinext.ae2pr.machine.integrated_mass_fabricator.IntegratedMassFabricatorControllerBlockEntity;
import com.murthinext.ae2pr.machine.integrated_mass_fabricator.IntegratedMassFabricatorStructure;
import com.murthinext.ae2pr.machine.jig.JigControllerBlockEntity;
import com.murthinext.ae2pr.machine.jig.JigStructure;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerMode;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryControllerBlockEntity;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryStructure;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlock;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlockEntity;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class MachineCatalog {

    private static MachineCatalog instance;

    // 核心方块为水晶反应器时同步运行指示灯
    private static final CoreLight CRYSTAL_REACTOR_CORE = new CoreLight() {
        @Override
        public boolean matches(BlockState state) {
            return state.is(get().crystalReactor.block());
        }

        @Override
        public void apply(Level level, BlockPos pos, BlockState state, boolean lit) {
            if (state.getValue(HighEnergyCoreBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(HighEnergyCoreBlock.LIT, lit), Block.UPDATE_ALL);
            }
        }
    };

    public final MultiblockMachineDefinition<MachineControllerBlockEntity> filterPress;
    public final MultiblockMachineDefinition<JigControllerBlockEntity> jig;
    public final MultiblockMachineDefinition<CrystalReactionChamberControllerBlockEntity> crystalReactionChamber;
    public final MultiblockMachineDefinition<CrystalAssemblyLineControllerBlockEntity> crystalAssemblyLine;
    public final MultiblockMachineDefinition<MatterPhaseTransformerControllerBlockEntity> matterPhaseTransformer;
    public final MultiblockMachineDefinition<HighReactivityLavaSmelterControllerBlockEntity> highReactivityLavaSmelter;
    public final MultiblockMachineDefinition<CrystalWorkstationControllerBlockEntity> crystalWorkstation;
    public final MultiblockMachineDefinition<MultiFactoryControllerBlockEntity> multiFactory;
    public final MultiblockMachineDefinition<IntegratedMassFabricatorControllerBlockEntity> integratedMassFabricator;
    public final SingleblockMachineDefinition<NamingFactoryBlockEntity> namingFactory;
    public final MachineDefinition<CrystalReactorCoreBlockEntity> crystalReactor;
    public final List<MachineDefinition<?>> all;

    private MachineCatalog() {
        filterPress =
                MultiblockMachineDefinition.of(MachineSpec.builder("filter_press", MachineControllerBlockEntity.class)
                        .controller(MachineControllerBlockEntity::new)
                        .properties(MachineCatalog::metalProperties)
                        .structure(() -> FilterPressStructure.INSTANCE)
                        .parallel(ServerConfig::filterPressBaseParallel)
                        .parallelPerRepeatSlice(ServerConfig::filterPressParallelPerRepeatSlice)
                        .recipe(RecipeSpec.simple(200, RecipeConstraints.ENERGY_ONLY))
                        .build());
        jig = MultiblockMachineDefinition.of(MachineSpec.builder("jig", JigControllerBlockEntity.class)
                .controller(JigControllerBlockEntity::new)
                .properties(MachineCatalog::crystalProperties)
                .structure(() -> JigStructure.INSTANCE)
                .parallel(ServerConfig::jigBaseParallel)
                .recipe(RecipeSpec.simple(200, RecipeConstraints.ENERGY_ONLY))
                .build());
        crystalReactionChamber = MultiblockMachineDefinition.of(
                MachineSpec.builder("crystal_reaction_chamber", CrystalReactionChamberControllerBlockEntity.class)
                        .controller(CrystalReactionChamberControllerBlockEntity::new)
                        .properties(MachineCatalog::crystalProperties)
                        .structure(() -> CrystalReactionChamberStructure.INSTANCE)
                        .parallel(ServerConfig::crystalReactionBaseParallel)
                        .recipe(RecipeSpec.simple(200, RecipeConstraints.ENERGY_ONLY))
                        .coreLight(CRYSTAL_REACTOR_CORE)
                        .build());
        crystalAssemblyLine = MultiblockMachineDefinition.of(
                MachineSpec.builder("crystal_assembly_line", CrystalAssemblyLineControllerBlockEntity.class)
                        .controller(CrystalAssemblyLineControllerBlockEntity::new)
                        .properties(MachineCatalog::metalProperties)
                        .structure(() -> CrystalAssemblyLineStructure.INSTANCE)
                        .parallel(ServerConfig::assemblyBaseParallel)
                        .parallelPerRepeatSlice(ServerConfig::assemblyParallelPerRepeatSlice)
                        .recipe(RecipeSpec.custom("crystal_assembly_line", CrystalAssemblyLineRecipe.Serializer::new))
                        .build());
        matterPhaseTransformer = MultiblockMachineDefinition.of(
                MachineSpec.builder("matter_phase_transformer", MatterPhaseTransformerControllerBlockEntity.class)
                        .controller(MatterPhaseTransformerControllerBlockEntity::new)
                        .properties(MachineCatalog::stoneProperties)
                        .structure(() -> MatterPhaseTransformerStructure.INSTANCE)
                        .parallel(ServerConfig::matterPhaseTransformerBaseParallel)
                        .modes(MatterPhaseTransformerMode.class)
                        .coreLight(CRYSTAL_REACTOR_CORE)
                        .build());
        highReactivityLavaSmelter = MultiblockMachineDefinition.of(MachineSpec.builder(
                        "high_reactivity_lava_smelter", HighReactivityLavaSmelterControllerBlockEntity.class)
                .controller(HighReactivityLavaSmelterControllerBlockEntity::new)
                .properties(MachineCatalog::stoneProperties)
                .structure(() -> HighReactivityLavaSmelterStructure.INSTANCE)
                .parallel(ServerConfig::highReactivityLavaSmelterBaseParallel)
                .recipe(RecipeSpec.simple(
                        100,
                        RecipeConstraints.builder()
                                .itemInputs(0, 4)
                                .fluidInputs(0, 2)
                                .itemOutputs(1, 2)
                                .fluidOutputs(0, 0)
                                .build()))
                .menu(() -> HighReactivityLavaSmelterMenu.TYPE, HighReactivityLavaSmelterMenu::new)
                .build());
        crystalWorkstation = MultiblockMachineDefinition.of(
                MachineSpec.builder("crystal_workstation", CrystalWorkstationControllerBlockEntity.class)
                        .controller(CrystalWorkstationControllerBlockEntity::new)
                        .properties(MachineCatalog::crystalProperties)
                        .structure(() -> CrystalWorkstationStructure.INSTANCE)
                        .parallel(ServerConfig::crystalWorkstationBaseParallel)
                        .modes(CrystalWorkstationMode.class)
                        .coreLight(CRYSTAL_REACTOR_CORE)
                        .build());
        multiFactory = MultiblockMachineDefinition.of(
                MachineSpec.builder("multi_factory", MultiFactoryControllerBlockEntity.class)
                        .controller(MultiFactoryControllerBlockEntity::new)
                        .properties(MachineCatalog::crystalProperties)
                        .structure(() -> MultiFactoryStructure.INSTANCE)
                        .parallel(ServerConfig::multiFactoryBaseParallel)
                        .modes(MultiFactoryMode.class)
                        .build());
        integratedMassFabricator = MultiblockMachineDefinition.of(
                MachineSpec.builder("integrated_mass_fabricator", IntegratedMassFabricatorControllerBlockEntity.class)
                        .controller(IntegratedMassFabricatorControllerBlockEntity::new)
                        .properties(MachineCatalog::metalProperties)
                        .structure(() -> IntegratedMassFabricatorStructure.INSTANCE)
                        .parallel(() -> 1)
                        .recipe(RecipeSpec.simple(
                                200,
                                RecipeConstraints.builder()
                                        .itemInputs(0, 4)
                                        .fluidInputs(0, 1)
                                        .itemOutputs(1, 2)
                                        .fluidOutputs(0, 0)
                                        .requireEnergy()
                                        .build()))
                        .build());
        namingFactory = SingleblockMachineDefinition.create(
                "naming_factory",
                NamingFactoryBlockEntity.class,
                MachineCatalog::namingFactoryProperties,
                NamingFactoryBlock::new,
                NamingFactoryBlockEntity::new,
                () -> NamingFactoryMenu.TYPE,
                NamingFactoryMenu::new,
                4,
                NamingFactoryBlockEntity.SLOT_COUNT,
                ServerConfig::namingFactoryMaxPower);
        crystalReactor = MachineDefinition.custom(
                "high_energy_core",
                "crystal_reactor_core",
                CrystalReactorCoreBlockEntity.class,
                () -> new HighEnergyCoreBlock(metalProperties()),
                CrystalReactorCoreBlockEntity::new,
                null,
                0);

        all = List.copyOf(List.of(
                filterPress,
                jig,
                crystalReactionChamber,
                crystalAssemblyLine,
                matterPhaseTransformer,
                highReactivityLavaSmelter,
                crystalWorkstation,
                multiFactory,
                integratedMassFabricator,
                namingFactory,
                crystalReactor));
    }

    public static synchronized void bootstrap() {
        if (instance != null) {
            throw new IllegalStateException("MachineCatalog already bootstrapped");
        }
        instance = new MachineCatalog();
    }

    public static MachineCatalog get() {
        if (instance == null) {
            throw new IllegalStateException("MachineCatalog not bootstrapped");
        }
        return instance;
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
