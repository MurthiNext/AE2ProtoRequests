package com.murthinext.ae2pr.registry;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.FluidSet;
import com.murthinext.ae2pr.api.SimpleFluidType;
import com.murthinext.ae2pr.fluid.AlienLavaFluid;
import com.murthinext.ae2pr.fluid.AlienLavaFluidType;

public final class ModFluids {

    private ModFluids() {
    }

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
            ForgeRegistries.Keys.FLUID_TYPES, ae2pr.MODID);

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, ae2pr.MODID);

    public static final RegistryObject<FluidType> ALIEN_LAVA_TYPE = FLUID_TYPES.register("alien_lava",
                                                                                         AlienLavaFluidType::new);

    public static final RegistryObject<ForgeFlowingFluid> ALIEN_LAVA = FLUIDS.register(
            "alien_lava", () -> new AlienLavaFluid.Source(alienLavaProperties()));

    public static final RegistryObject<ForgeFlowingFluid> FLOWING_ALIEN_LAVA = FLUIDS.register(
            "flowing_alien_lava", () -> new AlienLavaFluid.Flowing(alienLavaProperties()));

    private static ForgeFlowingFluid.Properties alienLavaProperties() {
        return new ForgeFlowingFluid.Properties(ALIEN_LAVA_TYPE, ALIEN_LAVA, FLOWING_ALIEN_LAVA)
                .bucket(ModItems.ALIEN_LAVA_BUCKET)
                .block(ModBlocks.ALIEN_LAVA)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

    public static final FluidSet COARSE_METEORITE_SLURRY = FluidSet.of(
            "coarse_meteorite_slurry", FLUID_TYPES, FLUIDS, ModItems.ITEMS,
            () -> SimpleFluidType.waterLike("coarse_meteorite_slurry", 0, 3000, 8000, 350));

    public static final FluidSet COARSE_METEORITE_SOLUTION = FluidSet.of(
            "coarse_meteorite_solution", FLUID_TYPES, FLUIDS, ModItems.ITEMS,
            () -> SimpleFluidType.waterLike("coarse_meteorite_solution", 0, 1600, 2200, 310));
}
