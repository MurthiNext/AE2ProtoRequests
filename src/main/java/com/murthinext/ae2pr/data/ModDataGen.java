package com.murthinext.ae2pr.data;

import com.murthinext.ae2pr.ae2pr;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        if (event.includeServer()) {
            var generator = event.getGenerator();
            generator.addProvider(true, new MaterialRecipeProvider(generator.getPackOutput()));
            generator.addProvider(true, new AaeReactionRecipeProvider(generator.getPackOutput()));
        }
    }

    private ModDataGen() {
    }
}
