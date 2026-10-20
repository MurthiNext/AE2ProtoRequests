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
 * 水晶黏合剂流体。
 */
public class CrystalAdhesiveFluidType extends FluidType {

    public CrystalAdhesiveFluidType() {
        super(FluidType.Properties.create()
                .canSwim(false)
                .canDrown(false)
                .pathType(BlockPathTypes.WATER)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .lightLevel(6)
                .density(2400)
                .viscosity(6000)
                .temperature(360));
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "block/fluid/crystal_adhesive_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "block/fluid/crystal_adhesive_flow");
            }
        });
    }
}
