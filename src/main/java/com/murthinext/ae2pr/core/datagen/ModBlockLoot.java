package com.murthinext.ae2pr.core.datagen;

import com.murthinext.ae2pr.core.registry.ModBlocks;
import com.murthinext.ae2pr.core.registry.ModItems;
import java.util.List;
import java.util.Set;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.RegistryObject;

/**
 * 掉落表：默认掉自己，矿表按时运/精准采集，ME 样板缓存保留共享流体。
 */
public final class ModBlockLoot extends BlockLootSubProvider {

    public ModBlockLoot() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    public static LootTableProvider provider(PackOutput output) {
        return new LootTableProvider(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)));
    }

    @Override
    protected void generate() {
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            Block block = entry.get();
            if (block.getLootTable() == BuiltInLootTables.EMPTY) {
                continue;
            }
            dropSelf(block);
        }

        add(
                ModBlocks.METEORITE_IRON_ORE.get(),
                createOreDrop(ModBlocks.METEORITE_IRON_ORE.get(), Items.RAW_IRON));
        add(
                ModBlocks.METEORITE_GOLD_ORE.get(),
                createOreDrop(ModBlocks.METEORITE_GOLD_ORE.get(), Items.RAW_GOLD));
        add(
                ModBlocks.METEORITE_DIAMOND_ORE.get(),
                createOreDrop(ModBlocks.METEORITE_DIAMOND_ORE.get(), Items.DIAMOND));
        add(
                ModBlocks.METEORITE_EMERALD_ORE.get(),
                createOreDrop(ModBlocks.METEORITE_EMERALD_ORE.get(), Items.EMERALD));
        add(
                ModBlocks.METEORITE_ZIRCON_ORE.get(),
                createOreDrop(ModBlocks.METEORITE_ZIRCON_ORE.get(), ModItems.ZIRCON.get()));
        add(ModBlocks.METEORITE_COPPER_ORE.get(), createCopperOreDrops(ModBlocks.METEORITE_COPPER_ORE.get()));
        add(ModBlocks.METEORITE_LAPIS_ORE.get(), createLapisOreDrops(ModBlocks.METEORITE_LAPIS_ORE.get()));

        Block patternBuffer = ModBlocks.ME_PATTERN_BUFFER.get();
        add(
                patternBuffer,
                LootTable.lootTable()
                        .withPool(applyExplosionCondition(
                                patternBuffer,
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))
                                        .add(LootItem.lootTableItem(patternBuffer)
                                                .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                                        .copy(
                                                                "shared_fluids",
                                                                "BlockEntityTag.shared_fluids"))))));
    }

    // 默认实现会遍历原版方块，为每个方块要求一张表
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
    }
}
