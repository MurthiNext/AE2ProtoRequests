package com.murthinext.ae2pr.fluid;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

import com.murthinext.ae2pr.ae2pr;

public class AlienLavaFluidType extends FluidType {

    public AlienLavaFluidType() {
        super(FluidType.Properties.create()
                .canSwim(false)
                .canDrown(false)
                .pathType(BlockPathTypes.LAVA)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .lightLevel(15)
                .density(3000)
                .viscosity(6000)
                .temperature(1300));
    }

    @Override
    public double motionScale(Entity entity) {
        return entity.level().dimensionType().ultraWarm() ? 0.007D : 0.0023333333333333335D;
    }

    @Override
    public void setItemMovement(ItemEntity entity) {
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.95F, motion.y + (motion.y < 0.06F ? 5.0E-4F : 0.0F), motion.z * 0.95F);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "block/fluid/alien_lava_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "block/fluid/alien_lava_flow");
            }
        });
    }
}
