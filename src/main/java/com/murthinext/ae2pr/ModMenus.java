package com.murthinext.ae2pr;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.block.assembly_line.AssemblyLineMenu;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterMenu;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterMenu;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterMenu;
import com.murthinext.ae2pr.block.machine_part.FluidHatchMenu;
import com.murthinext.ae2pr.block.machine_part.ItemBusMenu;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryMenu;
import com.murthinext.ae2pr.block.redstone_requester.RedstoneRequesterMenu;

/**
 * 菜单类型注册入口。
 */
public final class ModMenus {

    private ModMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            ae2pr.MODID);

    public static final RegistryObject<MenuType<MultiLevelEmitterMenu>> MULTI_LEVEL_EMITTER = MENUS
            .register("multi_level_emitter", () -> MultiLevelEmitterMenu.TYPE);

    public static final RegistryObject<MenuType<MultiThresholdLevelEmitterMenu>> MULTI_THRESHOLD_LEVEL_EMITTER = MENUS
            .register("multi_threshold_level_emitter", () -> MultiThresholdLevelEmitterMenu.TYPE);

    public static final RegistryObject<MenuType<RedstoneRequesterMenu>> REDSTONE_REQUESTER = MENUS
            .register("redstone_requester", () -> RedstoneRequesterMenu.TYPE);

    public static final RegistryObject<MenuType<AssemblyLineMenu>> CRYSTAL_ASSEMBLY_LINE = MENUS
            .register("crystal_assembly_line", () -> AssemblyLineMenu.TYPE);

    public static final RegistryObject<MenuType<LavaSmelterMenu>> HIGH_REACTIVITY_LAVA_SMELTER = MENUS
            .register("high_reactivity_lava_smelter", () -> LavaSmelterMenu.TYPE);

    public static final RegistryObject<MenuType<ItemBusMenu>> CERTUS_QUARTZ_CRYSTAL_ITEM_BUS = MENUS
            .register("certus_quartz_crystal_item_bus", () -> ItemBusMenu.TYPE);

    public static final RegistryObject<MenuType<FluidHatchMenu>> CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH = MENUS
            .register("certus_quartz_crystal_input_hatch", () -> FluidHatchMenu.TYPE);

    public static final RegistryObject<MenuType<NamingFactoryMenu>> NAMING_FACTORY = MENUS
            .register("naming_factory", () -> NamingFactoryMenu.TYPE);
}
