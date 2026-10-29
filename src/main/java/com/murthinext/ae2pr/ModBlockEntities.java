package com.murthinext.ae2pr;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.block.crystal_assembly_line.CrystalAssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.molecular_assembly_core.MolecularAssemblyCoreBlockEntity;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationControllerBlockEntity;
import com.murthinext.ae2pr.block.crystal_reactor.CrystalReactorCoreBlockEntity;
import com.murthinext.ae2pr.block.jig.JigControllerBlockEntity;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryControllerBlockEntity;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity;

public final class ModBlockEntities {

    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, ae2pr.MODID);

    public static final RegistryObject<BlockEntityType<CrystalAssemblyLineControllerBlockEntity>> CRYSTAL_ASSEMBLY_LINE = BLOCK_ENTITIES
            .register("crystal_assembly_line", () -> BlockEntityType.Builder.of(
                    CrystalAssemblyLineControllerBlockEntity::new,
                    ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BlockEntity>> CERTUS_QUARTZ_CRYSTAL_MACHINE_PART = BLOCK_ENTITIES
            .register("certus_quartz_crystal_machine_part", () -> BlockEntityType.Builder.of(
                    MachinePartBlock::createBlockEntity,
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(),
                    ModBlocks.AEV_INPUT_BUS.get(),
                    ModBlocks.AEV_INPUT_HATCH.get(),
                    ModBlocks.AEV_OUTPUT_BUS.get(),
                    ModBlocks.AEV_OUTPUT_HATCH.get(),
                    ModBlocks.AEV_DUAL_INPUT_HATCH.get(),
                    ModBlocks.AEV_DUAL_OUTPUT_HATCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<MENetworkAccessHatchBlockEntity>> ME_NETWORK_ACCESS_HATCH = BLOCK_ENTITIES
            .register("me_network_access_hatch", () -> BlockEntityType.Builder.of(
                    MENetworkAccessHatchBlockEntity::new,
                    ModBlocks.ME_NETWORK_ACCESS_HATCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<LavaSmelterControllerBlockEntity>> HIGH_REACTIVITY_LAVA_SMELTER = BLOCK_ENTITIES
            .register("high_reactivity_lava_smelter", () -> BlockEntityType.Builder.of(
                    LavaSmelterControllerBlockEntity::new,
                    ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MatterPhaseTransformerControllerBlockEntity>> MATTER_PHASE_TRANSFORMER = BLOCK_ENTITIES
            .register("matter_phase_transformer", () -> BlockEntityType.Builder.of(
                    MatterPhaseTransformerControllerBlockEntity::new,
                    ModBlocks.MATTER_PHASE_TRANSFORMER.get()).build(null));

    public static final RegistryObject<BlockEntityType<CrystalReactionChamberControllerBlockEntity>> CRYSTAL_REACTION_CHAMBER = BLOCK_ENTITIES
            .register("crystal_reaction_chamber", () -> BlockEntityType.Builder.of(
                    CrystalReactionChamberControllerBlockEntity::new,
                    ModBlocks.CRYSTAL_REACTION_CHAMBER.get()).build(null));

    public static final RegistryObject<BlockEntityType<NamingFactoryBlockEntity>> NAMING_FACTORY = BLOCK_ENTITIES
            .register("naming_factory", () -> BlockEntityType.Builder.of(
                    NamingFactoryBlockEntity::new,
                    ModBlocks.NAMING_FACTORY.get()).build(null));

    public static final RegistryObject<BlockEntityType<CrystalWorkstationControllerBlockEntity>> CRYSTAL_WORKSTATION = BLOCK_ENTITIES
            .register("crystal_workstation", () -> BlockEntityType.Builder.of(
                    CrystalWorkstationControllerBlockEntity::new, ModBlocks.CRYSTAL_WORKSTATION.get()).build(null));

    public static final RegistryObject<BlockEntityType<CrystalReactorCoreBlockEntity>> CRYSTAL_REACTOR_CORE = BLOCK_ENTITIES
            .register("crystal_reactor_core", () -> BlockEntityType.Builder.of(
                    CrystalReactorCoreBlockEntity::new,
                    ModBlocks.HIGH_ENERGY_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<JigControllerBlockEntity>> JIG = BLOCK_ENTITIES
            .register("jig", () -> BlockEntityType.Builder.of(JigControllerBlockEntity::new,
                    ModBlocks.JIG.get()).build(null));

    public static final RegistryObject<BlockEntityType<MultiFactoryControllerBlockEntity>> MULTI_FACTORY = BLOCK_ENTITIES
            .register("multi_factory", () -> BlockEntityType.Builder.of(
                    MultiFactoryControllerBlockEntity::new, ModBlocks.MULTI_FACTORY.get()).build(null));

    public static final RegistryObject<BlockEntityType<MolecularAssemblyCoreBlockEntity>> MOLECULAR_ASSEMBLY_CORE = BLOCK_ENTITIES
            .register("molecular_assembly_core",
                    () -> BlockEntityType.Builder.of(MolecularAssemblyCoreBlockEntity::new,
                            ModBlocks.MOLECULAR_ASSEMBLY_CORE.get()).build(null));
}
