package com.murthinext.ae2pr.core.datagen;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.core.datagen.advanced_ae.ReactionRecipeProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var existingFileHelper = event.getExistingFileHelper();

        if (event.includeServer()) {
            generator.addProvider(true, new MaterialRecipeProvider(output));
            generator.addProvider(
                    true, new MaterialTagProvider(output, event.getLookupProvider(), ae2pr.MODID, existingFileHelper));
            generator.addProvider(true, new ReactionRecipeProvider(output));
            generator.addProvider(true, ModBlockLoot.provider(output));
            generator.addProvider(
                    true, new ModBlockTagsProvider(output, event.getLookupProvider(), existingFileHelper));
        }

        if (event.includeClient()) {
            generator.addProvider(true, new MaterialModelProvider(output, ae2pr.MODID, existingFileHelper));
            generator.addProvider(true, new ModItemModelProvider(output, ae2pr.MODID, existingFileHelper));
            generator.addProvider(true, new ModBlockStateProvider(output, ae2pr.MODID, existingFileHelper));
        }
    }

    private ModDataGen() {}
}
