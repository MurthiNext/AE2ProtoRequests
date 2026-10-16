package com.murthinext.ae2pr.fluid;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

import com.murthinext.ae2pr.ae2pr;

/**
 * 液态水晶素流体。
 */
public class LiquidCrystalineFluidType extends FluidType {

    public LiquidCrystalineFluidType() {
        super(FluidType.Properties.create()
                .canSwim(false)
                .canDrown(false)
                .pathType(BlockPathTypes.WATER)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .lightLevel(8)
                .density(2000)
                .viscosity(3000)
                .temperature(400));
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "block/fluid/liquid_crystaline_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "block/fluid/liquid_crystaline_flow");
            }
        });
    }
}
