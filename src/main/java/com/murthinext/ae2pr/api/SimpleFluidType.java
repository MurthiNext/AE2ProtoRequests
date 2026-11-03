package com.murthinext.ae2pr.api;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

/**
 * 流体类型模板：贴图按 id 推导为 block/fluid/<id>_still 与 _flow
 */
public class SimpleFluidType extends FluidType {

    private static final String TEXTURE_ROOT = "block/fluid/";

    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;

    protected SimpleFluidType(Properties properties, String id) {
        super(properties);
        this.stillTexture = Ids.of(TEXTURE_ROOT + id + "_still");
        this.flowingTexture = Ids.of(TEXTURE_ROOT + id + "_flow");
    }

    public static SimpleFluidType waterLike(String id, int lightLevel, int density, int viscosity, int temperature) {
        return new SimpleFluidType(common(BlockPathTypes.WATER, SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY,
                lightLevel, density, viscosity, temperature), id);
    }

    public static SimpleFluidType lavaLike(String id, int lightLevel, int density, int viscosity, int temperature) {
        return new SimpleFluidType(common(BlockPathTypes.LAVA, SoundEvents.BUCKET_FILL_LAVA,
                SoundEvents.BUCKET_EMPTY_LAVA, lightLevel, density, viscosity, temperature), id);
    }

    protected static Properties common(BlockPathTypes pathType, net.minecraft.sounds.SoundEvent fillSound,
            net.minecraft.sounds.SoundEvent emptySound, int lightLevel, int density, int viscosity, int temperature) {
        return FluidType.Properties.create()
                .canSwim(false)
                .canDrown(false)
                .pathType(pathType)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, fillSound)
                .sound(SoundActions.BUCKET_EMPTY, emptySound)
                .lightLevel(lightLevel)
                .density(density)
                .viscosity(viscosity)
                .temperature(temperature);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }
        });
    }
}
