package com.murthinext.ae2pr;

import com.murthinext.ae2pr.fluid.AlienLavaFluid;
import com.murthinext.ae2pr.fluid.AlienLavaFluidType;
import com.murthinext.ae2pr.fluid.LiquidCertusQuartzFluidType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 流体注册入口。
 */
public final class ModFluids {

    private ModFluids() {
    }

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister
            .create(ForgeRegistries.Keys.FLUID_TYPES, ae2pr.MODID);

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister
            .create(ForgeRegistries.FLUIDS, ae2pr.MODID);

    /** 异星熔岩流体类型 */
    public static final RegistryObject<FluidType> ALIEN_LAVA_TYPE = FLUID_TYPES.register("alien_lava",
            AlienLavaFluidType::new);

    /** 源异星熔岩 */
    public static final RegistryObject<ForgeFlowingFluid> ALIEN_LAVA = FLUIDS.register("alien_lava",
            () -> new AlienLavaFluid.Source(alienLavaProperties()));

    /** 流动异星熔岩 */
    public static final RegistryObject<ForgeFlowingFluid> FLOWING_ALIEN_LAVA = FLUIDS.register("flowing_alien_lava",
            () -> new AlienLavaFluid.Flowing(alienLavaProperties()));

    /** 异星熔岩的流体属性 */
    private static ForgeFlowingFluid.Properties alienLavaProperties() {
        return new ForgeFlowingFluid.Properties(ALIEN_LAVA_TYPE, ALIEN_LAVA, FLOWING_ALIEN_LAVA)
                .bucket(ModItems.ALIEN_LAVA_BUCKET)
                .block(ModBlocks.ALIEN_LAVA)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

    /** 液态赛特斯石英水晶流体类型 */
    public static final RegistryObject<FluidType> LIQUID_CERTUS_QUARTZ_TYPE = FLUID_TYPES.register(
            "liquid_certus_quartz", LiquidCertusQuartzFluidType::new);

    /** 源液态赛特斯石英水晶（无桶、无源方块，只能存在于容器与管道中） */
    public static final RegistryObject<ForgeFlowingFluid> LIQUID_CERTUS_QUARTZ = FLUIDS.register(
            "liquid_certus_quartz", () -> new ForgeFlowingFluid.Source(liquidCertusQuartzProperties()));

    /** 流动液态赛特斯石英水晶 */
    public static final RegistryObject<ForgeFlowingFluid> FLOWING_LIQUID_CERTUS_QUARTZ = FLUIDS.register(
            "flowing_liquid_certus_quartz", () -> new ForgeFlowingFluid.Flowing(liquidCertusQuartzProperties()));

    /** 液态赛特斯石英水晶的流体属性（带桶，但无源方块） */
    private static ForgeFlowingFluid.Properties liquidCertusQuartzProperties() {
        return new ForgeFlowingFluid.Properties(LIQUID_CERTUS_QUARTZ_TYPE, LIQUID_CERTUS_QUARTZ,
                FLOWING_LIQUID_CERTUS_QUARTZ)
                .bucket(ModItems.LIQUID_CERTUS_QUARTZ_BUCKET);
    }
}
