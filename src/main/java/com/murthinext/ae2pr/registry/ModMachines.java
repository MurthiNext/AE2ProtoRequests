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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class ModMachines {

    // 核心方块为水晶反应器时同步运行指示灯
    private static final CoreLight CRYSTAL_REACTOR_CORE = new CoreLight() {
        @Override
        public boolean matches(BlockState state) {
            return state.is(CRYSTAL_REACTOR.block());
        }

        @Override
        public void apply(
                net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, BlockState state, boolean lit) {
            if (state.getValue(HighEnergyCoreBlock.LIT) != lit) {
                level.setBlock(
                        pos,
                        state.setValue(HighEnergyCoreBlock.LIT, lit),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
    };

    public static MultiblockMachineDefinition<MachineControllerBlockEntity> FILTER_PRESS;
    public static MultiblockMachineDefinition<JigControllerBlockEntity> JIG;
    public static MultiblockMachineDefinition<CrystalReactionChamberControllerBlockEntity> CRYSTAL_REACTION_CHAMBER;
    public static MultiblockMachineDefinition<CrystalAssemblyLineControllerBlockEntity> CRYSTAL_ASSEMBLY_LINE;
    public static MultiblockMachineDefinition<MatterPhaseTransformerControllerBlockEntity> MATTER_PHASE_TRANSFORMER;
    public static MultiblockMachineDefinition<HighReactivityLavaSmelterControllerBlockEntity>
            HIGH_REACTIVITY_LAVA_SMELTER;
    public static MultiblockMachineDefinition<CrystalWorkstationControllerBlockEntity> CRYSTAL_WORKSTATION;
    public static MultiblockMachineDefinition<MultiFactoryControllerBlockEntity> MULTI_FACTORY;
    public static MultiblockMachineDefinition<IntegratedMassFabricatorControllerBlockEntity> INTEGRATED_MASS_FABRICATOR;
    public static SingleblockMachineDefinition<NamingFactoryBlockEntity> NAMING_FACTORY;
    public static MachineDefinition<CrystalReactorCoreBlockEntity> CRYSTAL_REACTOR;

    public static List<MachineDefinition<?>> ALL;

    private static boolean registered;

    private ModMachines() {}

    // 静态字段不做初始化，由模组构造阶段显式调用，避免首次访问落在注册事件之后
    public static synchronized void register() {
        if (registered) {
            throw new IllegalStateException("ModMachines already registered");
        }
        registered = true;
        FILTER_PRESS =
                MultiblockMachineDefinition.of(MachineSpec.builder("filter_press", MachineControllerBlockEntity.class)
                        .controller(MachineControllerBlockEntity::new)
                        .properties(ModMachines::metalProperties)
                        .structure(() -> FilterPressStructure.INSTANCE)
                        .parallel(ServerConfig::filterPressBaseParallel)
                        .parallelPerRepeatSlice(ServerConfig::filterPressParallelPerRepeatSlice)
                        .recipe(RecipeSpec.simple(200, RecipeConstraints.ENERGY_ONLY))
                        .build());
        JIG = MultiblockMachineDefinition.of(MachineSpec.builder("jig", JigControllerBlockEntity.class)
                .controller(JigControllerBlockEntity::new)
                .properties(ModMachines::crystalProperties)
                .structure(() -> JigStructure.INSTANCE)
                .parallel(ServerConfig::jigBaseParallel)
                .recipe(RecipeSpec.simple(200, RecipeConstraints.ENERGY_ONLY))
                .build());
        CRYSTAL_REACTION_CHAMBER = MultiblockMachineDefinition.of(
                MachineSpec.builder("crystal_reaction_chamber", CrystalReactionChamberControllerBlockEntity.class)
                        .controller(CrystalReactionChamberControllerBlockEntity::new)
                        .properties(ModMachines::crystalProperties)
                        .structure(() -> CrystalReactionChamberStructure.INSTANCE)
                        .parallel(ServerConfig::crystalReactionBaseParallel)
                        .recipe(RecipeSpec.simple(200, RecipeConstraints.ENERGY_ONLY))
                        .coreLight(ModMachines.CRYSTAL_REACTOR_CORE)
                        .build());
        CRYSTAL_ASSEMBLY_LINE = MultiblockMachineDefinition.of(
                MachineSpec.builder("crystal_assembly_line", CrystalAssemblyLineControllerBlockEntity.class)
                        .controller(CrystalAssemblyLineControllerBlockEntity::new)
                        .properties(ModMachines::metalProperties)
                        .structure(() -> CrystalAssemblyLineStructure.INSTANCE)
                        .parallel(ServerConfig::assemblyBaseParallel)
                        .parallelPerRepeatSlice(ServerConfig::assemblyParallelPerRepeatSlice)
                        .recipe(RecipeSpec.custom("crystal_assembly_line", CrystalAssemblyLineRecipe.Serializer::new))
                        .build());
        MATTER_PHASE_TRANSFORMER = MultiblockMachineDefinition.of(
                MachineSpec.builder("matter_phase_transformer", MatterPhaseTransformerControllerBlockEntity.class)
                        .controller(MatterPhaseTransformerControllerBlockEntity::new)
                        .properties(ModMachines::stoneProperties)
                        .structure(() -> MatterPhaseTransformerStructure.INSTANCE)
                        .parallel(ServerConfig::matterPhaseTransformerBaseParallel)
                        .modes(MatterPhaseTransformerMode.class)
                        .coreLight(ModMachines.CRYSTAL_REACTOR_CORE)
                        .build());
        HIGH_REACTIVITY_LAVA_SMELTER = MultiblockMachineDefinition.of(MachineSpec.builder(
                        "high_reactivity_lava_smelter", HighReactivityLavaSmelterControllerBlockEntity.class)
                .controller(HighReactivityLavaSmelterControllerBlockEntity::new)
                .properties(ModMachines::stoneProperties)
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
        CRYSTAL_WORKSTATION = MultiblockMachineDefinition.of(
                MachineSpec.builder("crystal_workstation", CrystalWorkstationControllerBlockEntity.class)
                        .controller(CrystalWorkstationControllerBlockEntity::new)
                        .properties(ModMachines::crystalProperties)
                        .structure(() -> CrystalWorkstationStructure.INSTANCE)
                        .parallel(ServerConfig::crystalWorkstationBaseParallel)
                        .modes(CrystalWorkstationMode.class)
                        .coreLight(ModMachines.CRYSTAL_REACTOR_CORE)
                        .build());
        MULTI_FACTORY = MultiblockMachineDefinition.of(
                MachineSpec.builder("multi_factory", MultiFactoryControllerBlockEntity.class)
                        .controller(MultiFactoryControllerBlockEntity::new)
                        .properties(ModMachines::crystalProperties)
                        .structure(() -> MultiFactoryStructure.INSTANCE)
                        .parallel(ServerConfig::multiFactoryBaseParallel)
                        .modes(MultiFactoryMode.class)
                        .build());
        INTEGRATED_MASS_FABRICATOR = MultiblockMachineDefinition.of(
                MachineSpec.builder("integrated_mass_fabricator", IntegratedMassFabricatorControllerBlockEntity.class)
                        .controller(IntegratedMassFabricatorControllerBlockEntity::new)
                        .properties(ModMachines::metalProperties)
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
        NAMING_FACTORY = SingleblockMachineDefinition.create(
                "naming_factory",
                NamingFactoryBlockEntity.class,
                ModMachines::namingFactoryProperties,
                NamingFactoryBlock::new,
                NamingFactoryBlockEntity::new,
                () -> NamingFactoryMenu.TYPE,
                NamingFactoryMenu::new,
                4,
                NamingFactoryBlockEntity.SLOT_COUNT,
                ServerConfig::namingFactoryMaxPower);
        CRYSTAL_REACTOR = MachineDefinition.custom(
                "high_energy_core",
                "crystal_reactor_core",
                CrystalReactorCoreBlockEntity.class,
                () -> new HighEnergyCoreBlock(metalProperties()),
                CrystalReactorCoreBlockEntity::new,
                null,
                0);

        ALL = List.of(
                FILTER_PRESS,
                JIG,
                CRYSTAL_REACTION_CHAMBER,
                CRYSTAL_ASSEMBLY_LINE,
                MATTER_PHASE_TRANSFORMER,
                HIGH_REACTIVITY_LAVA_SMELTER,
                CRYSTAL_WORKSTATION,
                MULTI_FACTORY,
                INTEGRATED_MASS_FABRICATOR,
                NAMING_FACTORY,
                CRYSTAL_REACTOR);
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
