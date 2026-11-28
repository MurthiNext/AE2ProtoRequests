package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.material.Form;
import com.murthinext.ae2pr.api.material.Material;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ae2pr.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2pr"))
                    .icon(() -> new ItemStack(ModItems.FILTER_CELL.get()))
                    .displayItems((params, output) -> {
                        // 功能物品
                        output.accept(ModItems.FILTER_CELL.get());
                        output.accept(ModItems.ADVANCED_FILTER_CELL.get());
                        output.accept(ModItems.PROTO_TERMINAL.get());
                        output.accept(ModItems.SUPER_STACK_CARD.get());
                        output.accept(ModItems.AEV_SPEED_CARD.get());
                        output.accept(ModItems.AEV_PARALLEL_CARD.get());
                        output.accept(ModItems.AEV_ENERGY_CARD.get());
                        output.accept(ModItems.COLLAPSED_ITEM_STORAGE_CELL.get());

                        // 材料 / 催化剂
                        output.accept(ModItems.SINGLE_PLATE_PRESS.get());
                        output.accept(ModItems.LOGISTICS_PROCESSOR_PRESS.get());
                        output.accept(ModItems.MATTER_CONFINEMENT_FRAME.get());

                        // 材料 / 中间产物
                        output.accept(ModItems.PRINTED_LOGISTICS_PROCESSOR.get());
                        output.accept(ModItems.LOGISTICS_PROCESSOR.get());
                        output.accept(ModItems.ZIRCON_CIRCUIT_BOARD.get());
                        output.accept(ModItems.ZIRCON_PRINTED_CIRCUIT_BOARD.get());
                        output.accept(ModItems.CARBON_FIBER.get());
                        output.accept(ModItems.CARBON_FIBER_PLATE.get());
                        output.accept(ModItems.DIAMOND_WAFER.get());
                        output.accept(ModItems.DIAMOND_SOC_WAFER.get());
                        output.accept(ModItems.DIAMOND_SOC.get());
                        output.accept(ModItems.FINE_FLUIX_WIRE.get());
                        output.accept(ModItems.CRYSTALINE.get());

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

                        // 锭
                        acceptMaterials(output, Form.INGOT);
                        output.accept(ModItems.ZIRCON_BRICK.get());
                        output.accept(ModItems.ZIRCON.get());

                        // 粉末
                        acceptMaterials(output, Form.DUST);

                        // 单层板
                        acceptMaterials(output, Form.PLATE);

                        // 桶
                        output.accept(ModItems.ALIEN_LAVA_BUCKET.get());
                        for (Material material : ModMaterials.ALL) {
                            material.fluid()
                                    .ifPresent(fluid ->
                                            output.accept(fluid.bucket().get()));
                        }
                        output.accept(ModFluids.COARSE_METEORITE_SLURRY.bucket().get());
                        output.accept(
                                ModFluids.COARSE_METEORITE_SOLUTION.bucket().get());

                        // 储存方块
                        acceptGroup(output, BlockGroup.STORAGE_BLOCK);

                        // 矿石
                        acceptGroup(output, BlockGroup.ORE);

                        // 线缆部件
                        output.accept(ModItems.MULTI_LEVEL_EMITTER.get());
                        output.accept(ModItems.MULTI_THRESHOLD_LEVEL_EMITTER.get());
                        output.accept(ModItems.SKY_DOME_ANNIHILATION_PLANE.get());

                        // 单方块机器
                        output.accept(ModMachines.namingFactory().item());

                        // 多方块结构 - 主机
                        output.accept(ModMachines.crystalAssemblyLine().item());
                        output.accept(ModMachines.highReactivityLavaSmelter().item());
                        output.accept(ModMachines.matterPhaseTransformer().item());
                        output.accept(ModMachines.crystalReactionChamber().item());
                        output.accept(ModMachines.crystalWorkstation().item());
                        output.accept(ModMachines.jig().item());
                        output.accept(ModMachines.filterPress().item());
                        output.accept(ModMachines.multiFactory().item());
                        output.accept(ModMachines.integratedMassFabricator().item());
                        output.accept(ModMachines.crystalReactor().item());

                        // 多方块结构 - 机壳 / 玻璃 / 特色
                        acceptGroup(output, BlockGroup.CASING);
                        acceptGroup(output, BlockGroup.GLASS);
                        acceptGroup(output, BlockGroup.FEATURE);

                        // 多方块结构 - 仓室
                        acceptGroup(output, BlockGroup.MACHINE_PART);
                    })
                    .build());

    private static void acceptMaterials(CreativeModeTab.Output output, Form form) {
        for (Material material : ModMaterials.ALL) {
            material.item(form).ifPresent(item -> output.accept(item.get()));
        }
    }

    private static void acceptGroup(CreativeModeTab.Output output, BlockGroup group) {
        for (BlockDef<?> def : ModBlocks.group(group)) {
            if (def.hasItem()) {
                output.accept(def.item());
            }
        }
    }
}
