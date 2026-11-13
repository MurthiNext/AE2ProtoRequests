package com.murthinext.ae2pr.fluid;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidInteractionRegistry;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.registry.ModFluids;

public final class AlienLavaInteractions {

    private AlienLavaInteractions() {
    }

    public static void register() {
        FluidInteractionRegistry.addInteraction(ModFluids.ALIEN_LAVA_TYPE.get(),
                                                new FluidInteractionRegistry.InteractionInformation(
                                                        ForgeMod.WATER_TYPE.get(),
                                                        AEBlocks.SKY_STONE_BLOCK.block().defaultBlockState()));
    }
}
