package com.murthinext.ae2pr.integration.jei.recipe;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineStructure;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberStructure;
import com.murthinext.ae2pr.machine.crystal_reactor.CrystalReactorStructure;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationStructure;
import com.murthinext.ae2pr.machine.filter_press.FilterPressStructure;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterStructure;
import com.murthinext.ae2pr.machine.integrated_mass_fabricator.IntegratedMassFabricatorStructure;
import com.murthinext.ae2pr.machine.jig.JigStructure;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryStructure;
import com.murthinext.ae2pr.registry.ModMachines;
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

public record MultiblockPreviewRecipe(
        ResourceLocation id,
        Component title,
        Block controller,
        MultiblockPattern pattern,
        IntSupplier maxRepeatSlices) {

    public static List<MultiblockPreviewRecipe> recipes() {
        return List.of(
                of(
                        "crystal_assembly_line",
                        ModMachines.crystalAssemblyLine().block(),
                        CrystalAssemblyLineStructure.PATTERN,
                        CrystalAssemblyLineStructure.INSTANCE::maxRepeatSlices),
                of(
                        "crystal_reaction_chamber",
                        ModMachines.crystalReactionChamber().block(),
                        CrystalReactionChamberStructure.PATTERN),
                of(
                        "crystal_workstation",
                        ModMachines.crystalWorkstation().block(),
                        CrystalWorkstationStructure.PATTERN),
                of("jig", ModMachines.jig().block(), JigStructure.PATTERN),
                of("filter_press", ModMachines.filterPress().block(), FilterPressStructure.PATTERN),
                of(
                        "high_reactivity_lava_smelter",
                        ModMachines.highReactivityLavaSmelter().block(),
                        HighReactivityLavaSmelterStructure.PATTERN),
                of(
                        "matter_phase_transformer",
                        ModMachines.matterPhaseTransformer().block(),
                        MatterPhaseTransformerStructure.PATTERN),
                of("multi_factory", ModMachines.multiFactory().block(), MultiFactoryStructure.PATTERN),
                of(
                        "integrated_mass_fabricator",
                        ModMachines.integratedMassFabricator().block(),
                        IntegratedMassFabricatorStructure.PATTERN),
                of("crystal_reactor", ModMachines.crystalReactor().block(), CrystalReactorStructure.PATTERN));
    }

    public List<ItemStack> ingredients() {
        return pattern
                .patternCells(BlockPos.ZERO, Direction.NORTH, Direction.UP, false, false, pattern.minRepeatSlices())
                .stream()
                .map(cell -> pattern.predicate(cell.symbol()))
                .distinct()
                .flatMap(predicate -> predicate.previewBlocks().stream())
                .distinct()
                .filter(block -> block.asItem() != Items.AIR)
                .map(ItemStack::new)
                .toList();
    }

    public List<ItemStack> defaultMaterials() {
        Map<Block, Integer> totals = new LinkedHashMap<>();
        for (MultiblockPattern.PatternCell cell : pattern.patternCells(
                BlockPos.ZERO, Direction.NORTH, Direction.UP, false, false, pattern.minRepeatSlices())) {
            List<Block> candidates = pattern.predicate(cell.symbol()).previewBlocks();
            if (!candidates.isEmpty() && candidates.get(0).asItem() != Items.AIR) {
                totals.merge(candidates.get(0), 1, Integer::sum);
            }
        }
        return totals.entrySet().stream()
                .map(entry -> new ItemStack(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static MultiblockPreviewRecipe of(String name, Block controller, MultiblockPattern pattern) {
        return of(name, controller, pattern, pattern::maxRepeatSlices);
    }

    private static MultiblockPreviewRecipe of(
            String name, Block controller, MultiblockPattern pattern, IntSupplier maxRepeatSlices) {
        return new MultiblockPreviewRecipe(
                Ids.of("multiblock/" + name),
                Component.translatable("jei.ae2pr.multiblock.structure." + name),
                controller,
                pattern,
                maxRepeatSlices);
    }
}
