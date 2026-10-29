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

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.multiblock.structure.CrystalReactorStructure;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.structure.CrystalAssemblyLineStructure;
import com.murthinext.ae2pr.multiblock.structure.CrystalReactionChamberStructure;
import com.murthinext.ae2pr.multiblock.structure.CrystalWorkstationStructure;
import com.murthinext.ae2pr.multiblock.structure.LavaSmelterStructure;
import com.murthinext.ae2pr.multiblock.structure.JigStructure;
import com.murthinext.ae2pr.multiblock.structure.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.multiblock.structure.MultiFactoryStructure;

public record MultiblockPreviewRecipe(ResourceLocation id, Component title, Block controller,
        MultiblockPattern pattern, IntSupplier maxSlices) {

    public static List<MultiblockPreviewRecipe> recipes() {
        return List.of(
                of("crystal_assembly_line", ModBlocks.CRYSTAL_ASSEMBLY_LINE.get(), CrystalAssemblyLineStructure.PATTERN,
                        CrystalAssemblyLineStructure.INSTANCE::maxSlices),
                of("crystal_reaction_chamber", ModBlocks.CRYSTAL_REACTION_CHAMBER.get(),
                        CrystalReactionChamberStructure.PATTERN, CrystalReactionChamberStructure.PATTERN::maxSlices),
                of("crystal_workstation", ModBlocks.CRYSTAL_WORKSTATION.get(), CrystalWorkstationStructure.PATTERN,
                        CrystalWorkstationStructure.PATTERN::maxSlices),
                of("jig", ModBlocks.JIG.get(), JigStructure.PATTERN, JigStructure.PATTERN::maxSlices),
                of("high_reactivity_lava_smelter", ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get(),
                        LavaSmelterStructure.PATTERN, LavaSmelterStructure.PATTERN::maxSlices),
                of("matter_phase_transformer", ModBlocks.MATTER_PHASE_TRANSFORMER.get(),
                        MatterPhaseTransformerStructure.PATTERN, MatterPhaseTransformerStructure.PATTERN::maxSlices),
                of("multi_factory", ModBlocks.MULTI_FACTORY.get(), MultiFactoryStructure.PATTERN,
                        MultiFactoryStructure.PATTERN::maxSlices),
                new MultiblockPreviewRecipe(id("crystal_reactor"),
                        Component.translatable("jei.ae2pr.multiblock.crystal_reactor"), ModBlocks.HIGH_ENERGY_CORE.get(),
                        CrystalReactorStructure.PATTERN, CrystalReactorStructure.PATTERN::maxSlices));
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

    private static MultiblockPreviewRecipe of(String name, Block controller, MultiblockPattern pattern,
            IntSupplier maxSlices) {
        return new MultiblockPreviewRecipe(id(name), controller.getName(), controller, pattern, maxSlices);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "multiblock/" + name);
    }
}
