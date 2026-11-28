package com.murthinext.ae2pr.core.datagen;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.material.Form;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialForm;
import com.murthinext.ae2pr.core.registry.ModBlocks;
import com.murthinext.ae2pr.core.registry.ModMaterials;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

/**
 * 工具标签：全部方块可被镐挖，工具等级集中在此表。
 */
public final class ModBlockTagsProvider extends BlockTagsProvider {

    private enum ToolTier {
        NONE,
        STONE,
        IRON
    }

    private static final Map<String, ToolTier> TOOL_TIERS = Map.ofEntries(
            Map.entry("meteor_steel_block", ToolTier.STONE),
            Map.entry("meteor_steel_pipe_block", ToolTier.STONE),
            Map.entry("filter_press_plate", ToolTier.STONE),
            Map.entry("zircon_block", ToolTier.STONE),
            Map.entry("zirconia_corundum_bricks", ToolTier.STONE),
            Map.entry("high_reactivity_lava_smelter", ToolTier.STONE),
            Map.entry("matter_phase_transformer", ToolTier.STONE),
            Map.entry("crystal_reaction_chamber", ToolTier.STONE),
            Map.entry("crystal_workstation", ToolTier.STONE),
            Map.entry("jig", ToolTier.STONE),
            Map.entry("filter_press", ToolTier.STONE),
            Map.entry("molecular_assembly_core", ToolTier.STONE),
            Map.entry("meteorite_iron_ore", ToolTier.IRON),
            Map.entry("meteorite_copper_ore", ToolTier.IRON),
            Map.entry("meteorite_gold_ore", ToolTier.IRON),
            Map.entry("meteorite_lapis_ore", ToolTier.IRON),
            Map.entry("meteorite_diamond_ore", ToolTier.IRON),
            Map.entry("meteorite_emerald_ore", ToolTier.IRON),
            Map.entry("meteorite_zircon_ore", ToolTier.IRON),
            Map.entry("dense_sky_stone_block", ToolTier.IRON));

    public ModBlockTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ae2pr.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            if (entry.get() == ModBlocks.ALIEN_LAVA.get()) {
                continue;
            }
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(entry.get());
            switch (TOOL_TIERS.getOrDefault(entry.getId().getPath(), ToolTier.NONE)) {
                case STONE -> tag(BlockTags.NEEDS_STONE_TOOL).add(entry.get());
                case IRON -> tag(BlockTags.NEEDS_IRON_TOOL).add(entry.get());
                case NONE -> {}
            }
        }

        for (Material material : ModMaterials.ALL) {
            material.form(Form.BLOCK)
                    .flatMap(MaterialForm::ownItem)
                    .ifPresent(holder -> {
                        if (holder.get() instanceof BlockItem blockItem) {
                            Block block = blockItem.getBlock();
                            var storageBlockTag = BlockTags.create(ResourceLocation.fromNamespaceAndPath(
                                    "forge", "storage_blocks/" + material.id()));
                            tag(storageBlockTag).add(block);
                            tag(Tags.Blocks.STORAGE_BLOCKS).addTag(storageBlockTag);
                        }
                    });
        }
    }
}
