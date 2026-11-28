package com.murthinext.ae2pr.registry;

import java.util.List;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public final class ModRegistries {

    private static final List<DeferredRegister<?>> ALL = List.of(
            ModBlocks.BLOCKS,
            ModBlockEntities.BLOCK_ENTITIES,
            ModItems.ITEMS,
            ModMenus.MENUS,
            ModFluids.FLUID_TYPES,
            ModFluids.FLUIDS,
            ModRecipes.RECIPE_TYPES,
            ModRecipes.SERIALIZERS,
            ModSounds.SOUNDS,
            ModEffects.EFFECTS,
            ModCreativeTabs.CREATIVE_TABS);

    private ModRegistries() {}

    public static void registerAll(IEventBus bus) {
        ALL.forEach(register -> register.register(bus));
    }
}
