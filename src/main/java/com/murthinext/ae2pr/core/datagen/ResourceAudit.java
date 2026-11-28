package com.murthinext.ae2pr.core.datagen;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.core.registry.ModBlocks;
import com.murthinext.ae2pr.core.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegistryObject;

/**
 * 开发期资源校验：缺失的方块状态模型、物品模型与掉落表只记录错误，不中断启动。
 */
public final class ResourceAudit {

    @Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientAudit {

        @SubscribeEvent
        public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
            if (FMLEnvironment.production) {
                return;
            }
            ModelBakery bakery = event.getModelBakery();
            UnbakedModel missing = bakery.getModel(ModelBakery.MISSING_MODEL_LOCATION);
            List<String> missingResources = new ArrayList<>();
            for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
                Block block = entry.get();
                ModelResourceLocation stateModel =
                        BlockModelShaper.stateToModelLocation(block.defaultBlockState());
                if (isMissing(bakery, missing, event, stateModel)) {
                    missingResources.add("blockstate " + entry.getId());
                }
                if (block.asItem() != Items.AIR
                        && isMissing(bakery, missing, event, new ModelResourceLocation(entry.getId(), "inventory"))) {
                    missingResources.add("item model " + entry.getId());
                }
            }
            for (RegistryObject<Item> entry : ModItems.ITEMS.getEntries()) {
                if (entry.get() instanceof BlockItem) {
                    continue;
                }
                if (isMissing(bakery, missing, event, new ModelResourceLocation(entry.getId(), "inventory"))) {
                    missingResources.add("item model " + entry.getId());
                }
            }
            report(missingResources);
        }

        private static boolean isMissing(
                ModelBakery bakery, UnbakedModel missing, ModelEvent.BakingCompleted event, ResourceLocation location) {
            return !event.getModels().containsKey(location) || bakery.getModel(location) == missing;
        }
    }

    @Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ServerAudit {

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            if (FMLEnvironment.production) {
                return;
            }
            List<String> missing = new ArrayList<>();
            LootDataManager loot = event.getServer().getLootData();
            for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
                ResourceLocation table = entry.get().getLootTable();
                if (table != BuiltInLootTables.EMPTY && loot.getLootTable(table) == LootTable.EMPTY) {
                    missing.add("loot table " + table);
                }
            }
            report(missing);
        }
    }

    private static void report(List<String> missing) {
        if (missing.isEmpty()) {
            ae2pr.LOGGER.info("[resource-audit] all resources present");
            return;
        }
        ae2pr.LOGGER.error("[resource-audit] {} missing resources:", missing.size());
        missing.forEach(entry -> ae2pr.LOGGER.error("[resource-audit]   {}", entry));
    }

    private ResourceAudit() {}
}
