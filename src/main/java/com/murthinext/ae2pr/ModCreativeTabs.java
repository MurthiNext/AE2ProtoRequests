package com.murthinext.ae2pr;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式标签页注册入口。
 */
public final class ModCreativeTabs {

    private ModCreativeTabs() {
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, ae2pr.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2pr"))
                    .icon(() -> new ItemStack(ModItems.FILTER_CELL.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FILTER_CELL.get());
                        output.accept(ModItems.ADVANCED_FILTER_CELL.get());
                        output.accept(ModItems.MULTI_LEVEL_EMITTER.get());
                        output.accept(ModItems.MULTI_THRESHOLD_LEVEL_EMITTER.get());
                        output.accept(ModItems.REDSTONE_REQUESTER.get());

                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_UNIT.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_CASING.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_GRATING.get());
                        output.accept(ModItems.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get());
                        output.accept(ModItems.CRYSTAL_GLASS.get());
                        output.accept(ModItems.FIREPROOF_CRYSTAL_GLASS.get());
                        output.accept(ModItems.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get());
                        output.accept(ModItems.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get());
                        output.accept(ModItems.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get());
                        output.accept(ModItems.FLUIX_CRYSTAL_ENERGY_HATCH.get());
                        output.accept(ModItems.HIGH_REACTIVITY_LAVA_SMELTER.get());
                        output.accept(ModItems.LOGISTICS_CONTROL_CASING.get());

                        output.accept(ModItems.METEOR_STEEL_INGOT.get());
                        output.accept(ModItems.METEOR_STEEL_PLATE.get());
                        output.accept(ModItems.METEOR_STEEL_DUST.get());
                        output.accept(ModItems.METEOR_STEEL_BLOCK.get());
                        output.accept(ModItems.METEOR_STEEL_PIPE_BLOCK.get());
                        output.accept(ModItems.SINGLE_PLATE_PRESS.get());
                        output.accept(ModItems.ALIEN_LAVA_BUCKET.get());
                        output.accept(ModItems.NAMING_FACTORY.get());
                        output.accept(ModItems.METEORITE_IRON_ORE.get());
                        output.accept(ModItems.METEORITE_COPPER_ORE.get());
                        output.accept(ModItems.METEORITE_GOLD_ORE.get());
                        output.accept(ModItems.METEORITE_LAPIS_ORE.get());
                        output.accept(ModItems.METEORITE_DIAMOND_ORE.get());
                        output.accept(ModItems.METEORITE_EMERALD_ORE.get());
                        output.accept(ModItems.METEORITE_ZIRCON_ORE.get());
                        output.accept(ModItems.ZIRCON.get());
                        output.accept(ModItems.ZIRCON_DUST.get());
                        output.accept(ModItems.ZIRCON_BLOCK.get());
                        output.accept(ModItems.ZIRCON_BRICK.get());
                        output.accept(ModItems.ZIRCONIA_CORUNDUM_BRICKS.get());
                    })
                    .build());
}
