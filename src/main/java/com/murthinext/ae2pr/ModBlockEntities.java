package com.murthinext.ae2pr;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.block.assembly_line.AssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;
import com.murthinext.ae2pr.block.crystal_reactor.CrystalReactorCoreBlockEntity;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity;

/**
 * 方块实体注册入口。
 */
public final class ModBlockEntities {

    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, ae2pr.MODID);

    public static final RegistryObject<BlockEntityType<AssemblyLineControllerBlockEntity>> CRYSTAL_ASSEMBLY_LINE = BLOCK_ENTITIES
            .register("crystal_assembly_line", () -> BlockEntityType.Builder.of(
                    AssemblyLineControllerBlockEntity::new,
                    ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BlockEntity>> CERTUS_QUARTZ_CRYSTAL_MACHINE_PART = BLOCK_ENTITIES
            .register("certus_quartz_crystal_machine_part", () -> BlockEntityType.Builder.of(
                    (pos, state) -> MachinePartBlock.isFluidPart(state)
                            ? new FluidHatchBlockEntity(pos, state)
                            : new ItemBusBlockEntity(pos, state),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(),
                    ModBlocks.AEV_INPUT_BUS.get(),
                    ModBlocks.AEV_INPUT_HATCH.get(),
                    ModBlocks.AEV_OUTPUT_BUS.get(),
                    ModBlocks.AEV_OUTPUT_HATCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluixCrystalEnergyHatchBlockEntity>> FLUIX_CRYSTAL_ENERGY_HATCH = BLOCK_ENTITIES
            .register("fluix_crystal_energy_hatch", () -> BlockEntityType.Builder.of(
                    FluixCrystalEnergyHatchBlockEntity::new,
                    ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).build(null));

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

    public static final RegistryObject<BlockEntityType<CrystalReactorCoreBlockEntity>> CRYSTAL_REACTOR_CORE = BLOCK_ENTITIES
            .register("crystal_reactor_core", () -> BlockEntityType.Builder.of(
                    CrystalReactorCoreBlockEntity::new,
                    ModBlocks.HIGH_ENERGY_CORE.get()).build(null));
}
