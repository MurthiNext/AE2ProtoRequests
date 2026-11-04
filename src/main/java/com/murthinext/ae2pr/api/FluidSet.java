package com.murthinext.ae2pr.api;

import java.util.function.Supplier;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.item.NoBlockFluidBucketItem;

/**
 * 普通流体的整套注册：类型、源、流动，以及配套的无方块桶。
 */
public final class FluidSet implements Supplier<ForgeFlowingFluid> {

    private final String id;
    private final RegistryObject<FluidType> type;
    private final RegistryObject<ForgeFlowingFluid> source;
    private final RegistryObject<ForgeFlowingFluid> flowing;
    private final RegistryObject<BucketItem> bucket;

    private FluidSet(String id, DeferredRegister<FluidType> types, DeferredRegister<Fluid> fluids,
            DeferredRegister<Item> items, Supplier<? extends FluidType> typeFactory) {
        this.id = id;
        this.type = types.register(id, typeFactory::get);
        this.source = fluids.register(id, () -> new ForgeFlowingFluid.Source(properties()));
        this.flowing = fluids.register("flowing_" + id, () -> new ForgeFlowingFluid.Flowing(properties()));
        this.bucket = items.register(id + "_bucket",
                () -> new NoBlockFluidBucketItem(this, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    }

    public static FluidSet of(String id, DeferredRegister<FluidType> types, DeferredRegister<Fluid> fluids,
            DeferredRegister<Item> items, Supplier<? extends FluidType> typeFactory) {
        return new FluidSet(id, types, fluids, items, typeFactory);
    }

    private ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(type, source, flowing).bucket(() -> bucket.get());
    }

    public String id() {
        return id;
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

    public RegistryObject<BucketItem> bucket() {
        return bucket;
    }

    @Override
    public ForgeFlowingFluid get() {
        return source.get();
    }
}
