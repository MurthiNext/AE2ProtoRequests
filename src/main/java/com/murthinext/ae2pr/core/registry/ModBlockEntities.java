package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.molecular_assembly_core.MolecularAssemblyCoreBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ae2pr.MODID);

    public static final RegistryObject<BlockEntityType<BlockEntity>> CERTUS_QUARTZ_CRYSTAL_MACHINE_PART =
            BLOCK_ENTITIES.register(
                    "certus_quartz_crystal_machine_part",
                    () -> BlockEntityType.Builder.of(
                                    MachinePartBlock::createBlockEntity,
                                    ModBlocks.machineParts().stream()
                                            .map(BlockDef::get)
                                            .toArray(Block[]::new))
                            .build(null));

    public static final RegistryObject<BlockEntityType<MENetworkAccessHatchBlockEntity>> ME_NETWORK_ACCESS_HATCH =
            BLOCK_ENTITIES.register(
                    "me_network_access_hatch",
                    () -> BlockEntityType.Builder.of(
                                    MENetworkAccessHatchBlockEntity::new, ModBlocks.ME_NETWORK_ACCESS_HATCH.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<MEPatternBufferBlockEntity>> ME_PATTERN_BUFFER =
            BLOCK_ENTITIES.register(
                    "me_pattern_buffer",
                    () -> BlockEntityType.Builder.of(MEPatternBufferBlockEntity::new, ModBlocks.ME_PATTERN_BUFFER.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<MolecularAssemblyCoreBlockEntity>> MOLECULAR_ASSEMBLY_CORE =
            BLOCK_ENTITIES.register(
                    "molecular_assembly_core",
                    () -> BlockEntityType.Builder.of(
                                    MolecularAssemblyCoreBlockEntity::new, ModBlocks.MOLECULAR_ASSEMBLY_CORE.get())
                            .build(null));
}
