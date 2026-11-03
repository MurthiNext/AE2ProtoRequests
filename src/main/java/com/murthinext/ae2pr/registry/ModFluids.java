package com.murthinext.ae2pr.registry;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.SimpleFluidType;
import com.murthinext.ae2pr.fluid.AlienLavaFluid;
import com.murthinext.ae2pr.fluid.AlienLavaFluidType;

public final class ModFluids {

    private ModFluids() {
    }

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister
            .create(ForgeRegistries.Keys.FLUID_TYPES, ae2pr.MODID);

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister
            .create(ForgeRegistries.FLUIDS, ae2pr.MODID);

    // 普通流体的三件套：类型、源方块、流动方块，外加对应的桶；本身即源流体的 Supplier
    public static final class Set implements Supplier<ForgeFlowingFluid> {

        private final RegistryObject<FluidType> type;
        private final RegistryObject<ForgeFlowingFluid> source;
        private final RegistryObject<ForgeFlowingFluid> flowing;

        private Set(String id, Supplier<SimpleFluidType> typeFactory,
                Supplier<RegistryObject<? extends Item>> bucket) {
            this.type = FLUID_TYPES.register(id, typeFactory);
            this.source = FLUIDS.register(id, () -> new ForgeFlowingFluid.Source(properties(bucket)));
            this.flowing = FLUIDS.register("flowing_" + id,
                    () -> new ForgeFlowingFluid.Flowing(properties(bucket)));
        }

        private ForgeFlowingFluid.Properties properties(Supplier<RegistryObject<? extends Item>> bucket) {
            return new ForgeFlowingFluid.Properties(type, source, flowing).bucket(() -> bucket.get().get());
        }

        public RegistryObject<FluidType> type() {
            return type;
        }

        public RegistryObject<ForgeFlowingFluid> source() {
            return source;
        }

        public RegistryObject<ForgeFlowingFluid> flowing() {
            return flowing;
        }

        @Override
        public ForgeFlowingFluid get() {
            return source.get();
        }
    }

    private static Set water(String id, int lightLevel, int density, int viscosity, int temperature,
            Supplier<RegistryObject<? extends Item>> bucket) {
        return new Set(id, () -> SimpleFluidType.waterLike(id, lightLevel, density, viscosity, temperature), bucket);
    }

    private static Set lava(String id, int lightLevel, int density, int viscosity, int temperature,
            Supplier<RegistryObject<? extends Item>> bucket) {
        return new Set(id, () -> SimpleFluidType.lavaLike(id, lightLevel, density, viscosity, temperature), bucket);
    }

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

    public static final Set LIQUID_CERTUS_QUARTZ = water("liquid_certus_quartz", 8, 2000, 3000, 400,
            () -> ModItems.LIQUID_CERTUS_QUARTZ_BUCKET);

    public static final Set LIQUID_FLUIX_CRYSTAL = water("liquid_fluix_crystal", 8, 2000, 3000, 400,
            () -> ModItems.LIQUID_FLUIX_CRYSTAL_BUCKET);

    public static final Set LIQUID_AMETHYST = water("liquid_amethyst", 8, 2000, 3000, 400,
            () -> ModItems.LIQUID_AMETHYST_BUCKET);

    public static final Set LIQUID_CRYSTALINE = water("liquid_crystaline", 8, 2000, 3000, 400,
            () -> ModItems.LIQUID_CRYSTALINE_BUCKET);

    public static final Set CRYSTAL_ADHESIVE = water("crystal_adhesive", 6, 2400, 6000, 360,
            () -> ModItems.CRYSTAL_ADHESIVE_BUCKET);

    public static final Set LIQUID_ZIRCONIUM = lava("liquid_zirconium", 8, 2600, 5000, 400,
            () -> ModItems.LIQUID_ZIRCONIUM_BUCKET);

    public static final Set COARSE_METEORITE_SLURRY = water("coarse_meteorite_slurry", 0, 3000, 8000, 350,
            () -> ModItems.COARSE_METEORITE_SLURRY_BUCKET);

    public static final Set COARSE_METEORITE_SOLUTION = water("coarse_meteorite_solution", 0, 1600, 2200, 310,
            () -> ModItems.COARSE_METEORITE_SOLUTION_BUCKET);
}
