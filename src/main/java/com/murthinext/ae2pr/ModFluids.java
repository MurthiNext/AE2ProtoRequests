package com.murthinext.ae2pr;

import com.murthinext.ae2pr.fluid.AlienLavaFluid;
import com.murthinext.ae2pr.fluid.AlienLavaFluidType;
import com.murthinext.ae2pr.fluid.LiquidAmethystFluidType;
import com.murthinext.ae2pr.fluid.LiquidCertusQuartzFluidType;
import com.murthinext.ae2pr.fluid.LiquidCrystalineFluidType;
import com.murthinext.ae2pr.fluid.LiquidFluixCrystalFluidType;
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

    public static final RegistryObject<FluidType> ALIEN_LAVA_TYPE = FLUID_TYPES.register("alien_lava",
            AlienLavaFluidType::new);

    public static final RegistryObject<ForgeFlowingFluid> ALIEN_LAVA = FLUIDS.register("alien_lava",
            () -> new AlienLavaFluid.Source(alienLavaProperties()));

    public static final RegistryObject<ForgeFlowingFluid> FLOWING_ALIEN_LAVA = FLUIDS.register("flowing_alien_lava",
            () -> new AlienLavaFluid.Flowing(alienLavaProperties()));

    private static ForgeFlowingFluid.Properties alienLavaProperties() {
        return new ForgeFlowingFluid.Properties(ALIEN_LAVA_TYPE, ALIEN_LAVA, FLOWING_ALIEN_LAVA)
                .bucket(ModItems.ALIEN_LAVA_BUCKET)
                .block(ModBlocks.ALIEN_LAVA)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

    public static final RegistryObject<FluidType> LIQUID_CERTUS_QUARTZ_TYPE = FLUID_TYPES.register(
            "liquid_certus_quartz", LiquidCertusQuartzFluidType::new);

    public static final RegistryObject<ForgeFlowingFluid> LIQUID_CERTUS_QUARTZ = FLUIDS.register(
            "liquid_certus_quartz", () -> new ForgeFlowingFluid.Source(liquidCertusQuartzProperties()));

    public static final RegistryObject<ForgeFlowingFluid> FLOWING_LIQUID_CERTUS_QUARTZ = FLUIDS.register(
            "flowing_liquid_certus_quartz", () -> new ForgeFlowingFluid.Flowing(liquidCertusQuartzProperties()));

    private static ForgeFlowingFluid.Properties liquidCertusQuartzProperties() {
        return new ForgeFlowingFluid.Properties(LIQUID_CERTUS_QUARTZ_TYPE, LIQUID_CERTUS_QUARTZ,
                FLOWING_LIQUID_CERTUS_QUARTZ)
                .bucket(ModItems.LIQUID_CERTUS_QUARTZ_BUCKET);
    }

    public static final RegistryObject<FluidType> LIQUID_FLUIX_CRYSTAL_TYPE = FLUID_TYPES.register(
            "liquid_fluix_crystal", LiquidFluixCrystalFluidType::new);

    public static final RegistryObject<ForgeFlowingFluid> LIQUID_FLUIX_CRYSTAL = FLUIDS.register(
            "liquid_fluix_crystal", () -> new ForgeFlowingFluid.Source(liquidFluixCrystalProperties()));

    public static final RegistryObject<ForgeFlowingFluid> FLOWING_LIQUID_FLUIX_CRYSTAL = FLUIDS.register(
            "flowing_liquid_fluix_crystal", () -> new ForgeFlowingFluid.Flowing(liquidFluixCrystalProperties()));

    private static ForgeFlowingFluid.Properties liquidFluixCrystalProperties() {
        return new ForgeFlowingFluid.Properties(LIQUID_FLUIX_CRYSTAL_TYPE, LIQUID_FLUIX_CRYSTAL,
                FLOWING_LIQUID_FLUIX_CRYSTAL)
                .bucket(ModItems.LIQUID_FLUIX_CRYSTAL_BUCKET);
    }

    public static final RegistryObject<FluidType> LIQUID_AMETHYST_TYPE = FLUID_TYPES.register(
            "liquid_amethyst", LiquidAmethystFluidType::new);

    public static final RegistryObject<ForgeFlowingFluid> LIQUID_AMETHYST = FLUIDS.register(
            "liquid_amethyst", () -> new ForgeFlowingFluid.Source(liquidAmethystProperties()));

    public static final RegistryObject<ForgeFlowingFluid> FLOWING_LIQUID_AMETHYST = FLUIDS.register(
            "flowing_liquid_amethyst", () -> new ForgeFlowingFluid.Flowing(liquidAmethystProperties()));

    private static ForgeFlowingFluid.Properties liquidAmethystProperties() {
        return new ForgeFlowingFluid.Properties(LIQUID_AMETHYST_TYPE, LIQUID_AMETHYST,
                FLOWING_LIQUID_AMETHYST)
                .bucket(ModItems.LIQUID_AMETHYST_BUCKET);
    }

    public static final RegistryObject<FluidType> LIQUID_CRYSTALINE_TYPE = FLUID_TYPES.register(
            "liquid_crystaline", LiquidCrystalineFluidType::new);

    public static final RegistryObject<ForgeFlowingFluid> LIQUID_CRYSTALINE = FLUIDS.register(
            "liquid_crystaline", () -> new ForgeFlowingFluid.Source(liquidCrystalineProperties()));

    public static final RegistryObject<ForgeFlowingFluid> FLOWING_LIQUID_CRYSTALINE = FLUIDS.register(
            "flowing_liquid_crystaline", () -> new ForgeFlowingFluid.Flowing(liquidCrystalineProperties()));

    private static ForgeFlowingFluid.Properties liquidCrystalineProperties() {
        return new ForgeFlowingFluid.Properties(LIQUID_CRYSTALINE_TYPE, LIQUID_CRYSTALINE,
                FLOWING_LIQUID_CRYSTALINE)
                .bucket(ModItems.LIQUID_CRYSTALINE_BUCKET);
    }
}
