package com.murthinext.ae2pr.integration.jei;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.machine.crystal_reactor.CrystalReactorStructure;
import com.murthinext.ae2pr.api.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineStructure;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberStructure;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationStructure;
import com.murthinext.ae2pr.machine.filter_press.FilterPressStructure;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterStructure;
import com.murthinext.ae2pr.machine.jig.JigStructure;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryStructure;

public record MultiblockPreviewRecipe(ResourceLocation id, Component title, Block controller,
        MultiblockPattern pattern, IntSupplier maxSlices) {

    public static List<MultiblockPreviewRecipe> recipes() {
        return List.of(
                of("crystal_assembly_line", ModMachines.CRYSTAL_ASSEMBLY_LINE.block(), CrystalAssemblyLineStructure.PATTERN,
                        CrystalAssemblyLineStructure.INSTANCE::maxSlices),
                of("crystal_reaction_chamber", ModMachines.CRYSTAL_REACTION_CHAMBER.block(),
                        CrystalReactionChamberStructure.PATTERN, CrystalReactionChamberStructure.PATTERN::maxSlices),
                of("crystal_workstation", ModMachines.CRYSTAL_WORKSTATION.block(), CrystalWorkstationStructure.PATTERN,
                        CrystalWorkstationStructure.PATTERN::maxSlices),
                of("jig", ModMachines.JIG.block(), JigStructure.PATTERN, JigStructure.PATTERN::maxSlices),
                of("filter_press", ModMachines.FILTER_PRESS.block(), FilterPressStructure.PATTERN,
                        FilterPressStructure.PATTERN::maxSlices),
                of("high_reactivity_lava_smelter", ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.block(),
                        HighReactivityLavaSmelterStructure.PATTERN, HighReactivityLavaSmelterStructure.PATTERN::maxSlices),
                of("matter_phase_transformer", ModMachines.MATTER_PHASE_TRANSFORMER.block(),
                        MatterPhaseTransformerStructure.PATTERN, MatterPhaseTransformerStructure.PATTERN::maxSlices),
                of("multi_factory", ModMachines.MULTI_FACTORY.block(), MultiFactoryStructure.PATTERN,
                        MultiFactoryStructure.PATTERN::maxSlices),
                of("crystal_reactor", ModMachines.CRYSTAL_REACTOR.block(), CrystalReactorStructure.PATTERN,
                        CrystalReactorStructure.PATTERN::maxSlices));
    }

    public List<ItemStack> ingredients() {
        return pattern.patternCells(BlockPos.ZERO, Direction.NORTH, Direction.UP, false, false, pattern.minSlices())
                .stream().map(cell -> pattern.predicate(cell.symbol())).distinct()
                .flatMap(predicate -> predicate.previewBlocks().stream()).distinct()
                .filter(block -> block.asItem() != Items.AIR).map(ItemStack::new).toList();
    }

    public List<ItemStack> defaultMaterials() {
        Map<Block, Integer> totals = new LinkedHashMap<>();
        for (MultiblockPattern.PatternCell cell : pattern.patternCells(BlockPos.ZERO, Direction.NORTH, Direction.UP,
                false, false, pattern.minSlices())) {
            List<Block> candidates = pattern.predicate(cell.symbol()).previewBlocks();
            if (!candidates.isEmpty() && candidates.get(0).asItem() != Items.AIR) {
                totals.merge(candidates.get(0), 1, Integer::sum);
            }
        }
        return totals.entrySet().stream().map(entry -> new ItemStack(entry.getKey(), entry.getValue())).toList();
    }

    // 结构预览统一使用结构名，key 与结构 id 一一对应
    private static MultiblockPreviewRecipe of(String name, Block controller, MultiblockPattern pattern,
            IntSupplier maxSlices) {
        return new MultiblockPreviewRecipe(Ids.of("multiblock/" + name),
                Component.translatable("jei.ae2pr.multiblock.structure." + name), controller, pattern, maxSlices);
    }
}
