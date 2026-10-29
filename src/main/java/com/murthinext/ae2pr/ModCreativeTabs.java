package com.murthinext.ae2pr;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

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
                        // 功能物品
                        output.accept(ModItems.FILTER_CELL.get());
                        output.accept(ModItems.ADVANCED_FILTER_CELL.get());
                        output.accept(ModItems.PROTO_TERMINAL.get());

                        // 材料 / 中间产物
                        output.accept(ModItems.SINGLE_PLATE_PRESS.get());
                        output.accept(ModItems.ZIRCON_CIRCUIT_BOARD.get());
                        output.accept(ModItems.ZIRCON_PRINTED_CIRCUIT_BOARD.get());
                        output.accept(ModItems.DIAMOND_WAFER.get());
                        output.accept(ModItems.DIAMOND_SOC_WAFER.get());
                        output.accept(ModItems.DIAMOND_SOC.get());
                        output.accept(ModItems.FINE_FLUIX_WIRE.get());

                        // 电路板
                        output.accept(ModItems.COMPUTATION_CELL_COMPONENT_1TOPS.get());
                        output.accept(ModItems.COMPUTATION_CELL_COMPONENT_4TOPS.get());
                        output.accept(ModItems.COMPUTATION_CELL_COMPONENT_16TOPS.get());
                        output.accept(ModItems.COMPUTATION_CELL_COMPONENT_64TOPS.get());
                        output.accept(ModItems.COMPUTATION_CELL_COMPONENT_256TOPS.get());
                        output.accept(ModItems.CRYSTAL_PROCESSOR.get());
                        output.accept(ModItems.CRYSTAL_PROCESSOR_ASSEMBLY.get());
                        output.accept(ModItems.CRYSTAL_PROCESSOR_COMPUTER.get());
                        output.accept(ModItems.CRYSTAL_PROCESSOR_MAINFRAME.get());

                        // 奇点
                        output.accept(ModItems.UNCHARGED_SINGULARITY.get());
                        output.accept(ModItems.HIGH_ENERGY_SINGULARITY.get());

                        // 锭 & 宝石
                        output.accept(ModItems.METEOR_STEEL_INGOT.get());
                        output.accept(ModItems.ZIRCONIUM_INGOT.get());
                        output.accept(ModItems.ZIRCONIUM_IRON_ALLOY_INGOT.get());
                        output.accept(ModItems.ZIRCON_BRICK.get());
                        output.accept(ModItems.ZIRCON.get());

                        // 特殊物品
                        output.accept(ModItems.CRYSTALINE.get());

                        // 粉末
                        output.accept(ModItems.METEOR_STEEL_DUST.get());
                        output.accept(ModItems.COAL_DUST.get());
                        output.accept(ModItems.CARBON_DUST.get());
                        output.accept(ModItems.ZIRCONIUM_DUST.get());
                        output.accept(ModItems.CARBON_ZIRCONIUM_MIXTURE_DUST.get());
                        output.accept(ModItems.ZIRCON_DUST.get());
                        output.accept(ModItems.COPPER_OXIDE_DUST.get());
                        output.accept(ModItems.IRON_OXIDE_DUST.get());
                        output.accept(ModItems.GOLD_OXIDE_DUST.get());

                        // 单层板
                        output.accept(ModItems.METEOR_STEEL_PLATE.get());
                        output.accept(ModItems.ZIRCONIUM_PLATE.get());

                        // 桶
                        output.accept(ModItems.ALIEN_LAVA_BUCKET.get());
                        output.accept(ModItems.LIQUID_CERTUS_QUARTZ_BUCKET.get());
                        output.accept(ModItems.LIQUID_FLUIX_CRYSTAL_BUCKET.get());
                        output.accept(ModItems.LIQUID_AMETHYST_BUCKET.get());
                        output.accept(ModItems.LIQUID_CRYSTALINE_BUCKET.get());
                        output.accept(ModItems.CRYSTAL_ADHESIVE_BUCKET.get());
                        output.accept(ModItems.LIQUID_ZIRCONIUM_BUCKET.get());
                        output.accept(ModItems.COARSE_METEORITE_SLURRY_BUCKET.get());

                        // 储存方块
                        output.accept(ModItems.METEOR_STEEL_BLOCK.get());
                        output.accept(ModItems.ZIRCON_BLOCK.get());
                        output.accept(ModItems.DENSE_SKY_STONE_BLOCK.get());

                        // 矿石
                        output.accept(ModItems.METEORITE_IRON_ORE.get());
                        output.accept(ModItems.METEORITE_COPPER_ORE.get());
                        output.accept(ModItems.METEORITE_GOLD_ORE.get());
                        output.accept(ModItems.METEORITE_LAPIS_ORE.get());
                        output.accept(ModItems.METEORITE_DIAMOND_ORE.get());
                        output.accept(ModItems.METEORITE_EMERALD_ORE.get());
                        output.accept(ModItems.METEORITE_ZIRCON_ORE.get());

                        // 线缆部件
                        output.accept(ModItems.MULTI_LEVEL_EMITTER.get());
                        output.accept(ModItems.MULTI_THRESHOLD_LEVEL_EMITTER.get());

                        // 单方块机器
                        output.accept(ModItems.NAMING_FACTORY.get());

                        // 多方块结构 - 主机
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE.get());
                        output.accept(ModItems.HIGH_REACTIVITY_LAVA_SMELTER.get());
                        output.accept(ModItems.MATTER_PHASE_TRANSFORMER.get());
                        output.accept(ModItems.CRYSTAL_REACTION_CHAMBER.get());
                        output.accept(ModItems.CRYSTAL_WORKSTATION.get());
                        output.accept(ModItems.JIG.get());
                        output.accept(ModItems.MULTI_FACTORY.get());
                        output.accept(ModItems.MULTI_FACTORY.get());

                        // 多方块结构 - 普通结构方块
                        output.accept(ModItems.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get());
                        output.accept(ModItems.AEV_MACHINE_CASING.get());
                        output.accept(ModItems.REINFORCED_HIGH_POWER_CASING.get());
                        output.accept(ModItems.HIGH_ENERGY_STABILITY_CASING.get());
                        output.accept(ModItems.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING.get());
                        output.accept(ModItems.ME_COMPUTER_CASING.get());
                        output.accept(ModItems.ME_ADVANCED_COMPUTER_CASING.get());
                        output.accept(ModItems.ZIRCONIA_CORUNDUM_BRICKS.get());
                        output.accept(ModItems.CRYSTAL_GLASS.get());
                        output.accept(ModItems.FIREPROOF_CRYSTAL_GLASS.get());

                        // 多方块结构 - 特色结构方块
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_UNIT.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_CHARGING_UNIT.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_ADVANCED_CHARGING_UNIT.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_PARALLEL_UNIT.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_SPEED_UNIT.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_CASING.get());
                        output.accept(ModItems.CRYSTAL_ASSEMBLY_LINE_GRATING.get());
                        output.accept(ModItems.LOGISTICS_CONTROL_CASING.get());
                        output.accept(ModItems.METEOR_STEEL_PIPE_BLOCK.get());
                        output.accept(ModItems.HIGH_ENERGY_CORE.get());
                        output.accept(ModItems.MOLECULAR_ASSEMBLY_CORE.get());

                        // 多方块结构 - 仓室
                        output.accept(ModItems.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get());
                        output.accept(ModItems.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get());
                        output.accept(ModItems.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get());
                        output.accept(ModItems.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get());
                        output.accept(ModItems.ME_NETWORK_ACCESS_HATCH.get());
                        output.accept(ModItems.AEV_INPUT_BUS.get());
                        output.accept(ModItems.AEV_INPUT_HATCH.get());
                        output.accept(ModItems.AEV_OUTPUT_BUS.get());
                        output.accept(ModItems.AEV_OUTPUT_HATCH.get());
                        output.accept(ModItems.AEV_DUAL_INPUT_HATCH.get());
                        output.accept(ModItems.AEV_DUAL_OUTPUT_HATCH.get());
                    })
                    .build());
}
