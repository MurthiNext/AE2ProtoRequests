package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterMenu;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterMenu;
import com.murthinext.ae2pr.block.machine_part.FluidHatchMenu;
import com.murthinext.ae2pr.block.machine_part.ItemBusMenu;
import com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ae2pr.MODID);

    public static final RegistryObject<MenuType<MultiLevelEmitterMenu>> MULTI_LEVEL_EMITTER =
            MENUS.register("multi_level_emitter", () -> MultiLevelEmitterMenu.TYPE);

    public static final RegistryObject<MenuType<MultiThresholdLevelEmitterMenu>> MULTI_THRESHOLD_LEVEL_EMITTER =
            MENUS.register("multi_threshold_level_emitter", () -> MultiThresholdLevelEmitterMenu.TYPE);

    public static final RegistryObject<MenuType<ItemBusMenu>> MACHINE_PART_ITEM_BUS =
            MENUS.register("machine_part_item_bus", () -> ItemBusMenu.TYPE);

    public static final RegistryObject<MenuType<FluidHatchMenu>> MACHINE_PART_FLUID_HATCH =
            MENUS.register("machine_part_fluid_hatch", () -> FluidHatchMenu.TYPE);

    public static final RegistryObject<MenuType<MEPatternBufferMenu>> ME_PATTERN_BUFFER =
            MENUS.register("me_pattern_buffer", () -> MEPatternBufferMenu.TYPE);
}
