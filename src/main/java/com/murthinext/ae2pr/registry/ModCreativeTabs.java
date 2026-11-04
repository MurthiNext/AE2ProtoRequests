package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.material.MaterialFluids;
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
                        output.accept(ModMaterials.METEOR_STEEL_INGOT.get());
                        output.accept(ModMaterials.ZIRCONIUM_INGOT.get());
                        output.accept(ModMaterials.ZIRCONIUM_IRON_ALLOY_INGOT.get());
                        output.accept(ModMaterials.STRONTIUM_INGOT.get());
                        output.accept(ModMaterials.BARIUM_INGOT.get());
                        output.accept(ModItems.ZIRCON_BRICK.get());
                        output.accept(ModItems.ZIRCON.get());

                        // 特殊物品
                        output.accept(ModItems.CRYSTALINE.get());

                        // 粉末
                        output.accept(ModMaterials.METEOR_STEEL_DUST.get());
                        output.accept(ModMaterials.COAL_DUST.get());
                        output.accept(ModMaterials.CARBON_DUST.get());
                        output.accept(ModMaterials.ZIRCONIUM_DUST.get());
                        output.accept(ModMaterials.CARBON_ZIRCONIUM_MIXTURE_DUST.get());
                        output.accept(ModMaterials.ZIRCON_DUST.get());
                        output.accept(ModMaterials.COPPER_OXIDE_DUST.get());
                        output.accept(ModMaterials.IRON_OXIDE_DUST.get());
                        output.accept(ModMaterials.GOLD_OXIDE_DUST.get());
                        output.accept(ModMaterials.PSEUDO_STRONTIANITE_DUST.get());
                        output.accept(ModMaterials.STRONTIUM_CARBONATE_DUST.get());
                        output.accept(ModMaterials.BARIUM_SULFATE_DUST.get());
                        output.accept(ModMaterials.STRONTIUM_DUST.get());
                        output.accept(ModMaterials.BARIUM_DUST.get());

                        // 单层板
                        output.accept(ModMaterials.METEOR_STEEL_PLATE.get());
                        output.accept(ModMaterials.ZIRCONIUM_PLATE.get());
                        output.accept(ModMaterials.STRONTIUM_PLATE.get());
                        output.accept(ModMaterials.BARIUM_PLATE.get());

                        // 桶
                        output.accept(ModItems.ALIEN_LAVA_BUCKET.get());
                        output.accept(MaterialFluids.get(ModMaterials.LIQUID_CERTUS_QUARTZ).bucket().get());
                        output.accept(MaterialFluids.get(ModMaterials.LIQUID_FLUIX_CRYSTAL).bucket().get());
                        output.accept(MaterialFluids.get(ModMaterials.LIQUID_AMETHYST).bucket().get());
                        output.accept(MaterialFluids.get(ModMaterials.LIQUID_CRYSTALINE).bucket().get());
                        output.accept(MaterialFluids.get(ModMaterials.CRYSTAL_ADHESIVE).bucket().get());
                        output.accept(MaterialFluids.get(ModMaterials.ZIRCONIUM).bucket().get());
                        output.accept(ModFluids.COARSE_METEORITE_SLURRY.bucket().get());
                        output.accept(ModFluids.COARSE_METEORITE_SOLUTION.bucket().get());

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
                        output.accept(ModMachines.NAMING_FACTORY.item());

                        // 多方块结构 - 主机
                        output.accept(ModMachines.CRYSTAL_ASSEMBLY_LINE.item());
                        output.accept(ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.item());
                        output.accept(ModMachines.MATTER_PHASE_TRANSFORMER.item());
                        output.accept(ModMachines.CRYSTAL_REACTION_CHAMBER.item());
                        output.accept(ModMachines.CRYSTAL_WORKSTATION.item());
                        output.accept(ModMachines.JIG.item());
                        output.accept(ModMachines.FILTER_PRESS.item());
                        output.accept(ModMachines.MULTI_FACTORY.item());

                        // 多方块结构 - 普通结构方块
                        output.accept(ModItems.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get());
                        output.accept(ModItems.AEV_MACHINE_CASING.get());
                        output.accept(ModItems.CRYSTAL_ENERGY_CASING.get());
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
                        output.accept(ModItems.FILTER_PRESS_PLATE.get());
                        output.accept(ModMachines.CRYSTAL_REACTOR.item());
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
