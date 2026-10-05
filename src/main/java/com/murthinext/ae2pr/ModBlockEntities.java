package com.murthinext.ae2pr;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.block.assembly_line.AssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity;
import com.murthinext.ae2pr.block.redstone_requester.RedstoneRequesterBlockEntity;

/**
 * 方块实体注册入口。
 */
public final class ModBlockEntities {

    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, ae2pr.MODID);

    /** ME 红石请求器 */
    public static final RegistryObject<BlockEntityType<RedstoneRequesterBlockEntity>> REDSTONE_REQUESTER = BLOCK_ENTITIES
            .register("redstone_requester", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new RedstoneRequesterBlockEntity(
                            ModBlockEntities.REDSTONE_REQUESTER.get(), pos, state),
                    ModBlocks.REDSTONE_REQUESTER.get()).build(null));

    /** 水晶装配线控制器 */
    public static final RegistryObject<BlockEntityType<AssemblyLineControllerBlockEntity>> CRYSTAL_ASSEMBLY_LINE = BLOCK_ENTITIES
            .register("crystal_assembly_line", () -> BlockEntityType.Builder.of(
                    AssemblyLineControllerBlockEntity::new,
                    ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()).build(null));

    /** 赛特斯石英水晶机器部件（输入总线 / 输入仓 / 输出总线） */
    public static final RegistryObject<BlockEntityType<BlockEntity>> CERTUS_QUARTZ_CRYSTAL_MACHINE_PART = BLOCK_ENTITIES
            .register("certus_quartz_crystal_machine_part", () -> BlockEntityType.Builder.of(
                    (pos, state) -> state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get())
                            ? new FluidHatchBlockEntity(pos, state)
                            : new ItemBusBlockEntity(pos, state),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(),
                    ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()).build(null));

    /** 福鲁伊克斯水晶能源仓 */
    public static final RegistryObject<BlockEntityType<FluixCrystalEnergyHatchBlockEntity>> FLUIX_CRYSTAL_ENERGY_HATCH = BLOCK_ENTITIES
            .register("fluix_crystal_energy_hatch", () -> BlockEntityType.Builder.of(
                    FluixCrystalEnergyHatchBlockEntity::new,
                    ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).build(null));

    /** 名称压印工厂 */
    public static final RegistryObject<BlockEntityType<NamingFactoryBlockEntity>> NAMING_FACTORY = BLOCK_ENTITIES
            .register("naming_factory", () -> BlockEntityType.Builder.of(
                    NamingFactoryBlockEntity::new,
                    ModBlocks.NAMING_FACTORY.get()).build(null));
}
