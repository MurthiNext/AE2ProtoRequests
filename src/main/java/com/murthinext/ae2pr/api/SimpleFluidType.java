package com.murthinext.ae2pr.api;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

/**
 * 流体类型模板：贴图默认为 block/fluid/&lt;id&gt;_still 与 _flow；
 * 材质流体则显式传入共用的灰度贴图与材质色，由 {@link IClientFluidTypeExtensions#getTintColor()} 染色。
 */
public class SimpleFluidType extends FluidType {

    private static final String TEXTURE_ROOT = "block/fluid/";
    public static final int NO_TINT = 0xFFFFFFFF;

    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final int tint;

    protected SimpleFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture,
            int tint) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.tint = tint;
    }

    public static SimpleFluidType of(Properties properties, ResourceLocation stillTexture,
            ResourceLocation flowingTexture, int tint) {
        return new SimpleFluidType(properties, stillTexture, flowingTexture, tint);
    }

    public static SimpleFluidType waterLike(String id, int lightLevel, int density, int viscosity, int temperature) {
        return of(fluidProperties(false, lightLevel, density, viscosity, temperature),
                Ids.of(TEXTURE_ROOT + id + "_still"), Ids.of(TEXTURE_ROOT + id + "_flow"), NO_TINT);
    }

    public static Properties fluidProperties(boolean lavaLike, int lightLevel, int density, int viscosity,
            int temperature) {
        return common(lavaLike ? BlockPathTypes.LAVA : BlockPathTypes.WATER,
                lavaLike ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL,
                lavaLike ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
                lightLevel, density, viscosity, temperature);
    }

    protected static Properties common(BlockPathTypes pathType, SoundEvent fillSound, SoundEvent emptySound,
            int lightLevel, int density, int viscosity, int temperature) {
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

            @Override
            public int getTintColor() {
                return tint;
            }
        });
    }
}
