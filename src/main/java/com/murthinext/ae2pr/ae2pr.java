package com.murthinext.ae2pr;

import appeng.api.client.StorageCellModels;
import appeng.api.parts.PartModels;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModelsHelper;
import com.mojang.logging.LogUtils;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineRegistries;
import com.murthinext.ae2pr.api.machine.MachineTuning;
import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterPart;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterPart;
import com.murthinext.ae2pr.block.meteorite.MeteoriteOres;
import com.murthinext.ae2pr.block.sky_dome_annihilation_plane.SkyDomeAnnihilationPlanePart;
import com.murthinext.ae2pr.fluid.AlienLavaInteractions;
import com.murthinext.ae2pr.item.collapsed_storage_cell.CollapsedItemStorageCellGuiHandler;
import com.murthinext.ae2pr.item.collapsed_storage_cell.CollapsedItemStorageCellHandler;
import com.murthinext.ae2pr.logic.collapse.CollapseEvents;
import com.murthinext.ae2pr.logic.repeat.GeneralRepeatOrders;
import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;
import com.murthinext.ae2pr.logic.wrench.WrenchInteractions;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlockEntity;
import com.murthinext.ae2pr.registry.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * AE2 Proto Requests
 */
@Mod(ae2pr.MODID)
public class ae2pr {

    public static final String MODID = "ae2pr";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ae2pr(FMLJavaModLoadingContext context) {
        var modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ServerConfig::onReload);

        MachineRegistries.install(
                ModBlocks.BLOCKS, ModItems.ITEMS,
                ModBlockEntities.BLOCK_ENTITIES, ModMenus.MENUS,
                ModRecipes.RECIPE_TYPES, ModRecipes.SERIALIZERS);
        MachineTuning.install(ServerConfig::lossySpeedPerCard, ServerConfig::lossyEnergyMultiplierPerCard);

        ModMaterials.register();
        ModMachines.register();
        ModBlocks.installStructureHooks();
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        context.registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER, ServerConfig.SPEC, ServerConfig.FILE_NAME);

        PartModels.registerModels(PartModelsHelper.createModels(MultiLevelEmitterPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(MultiThresholdLevelEmitterPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(SkyDomeAnnihilationPlanePart.class));
        ModNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(GeneralRepeatOrders::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(GeneralRepeatOrders::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(WrenchInteractions::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(MeteoriteOres::onBreakSpeed);
        MinecraftForge.EVENT_BUS.addListener(CollapseEvents::onBlockBreak);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModGuide.init();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AE2 Proto Requests initialized");
        event.enqueueWork(() -> {
            var multiLevelEmitterItem = ModItems.MULTI_LEVEL_EMITTER.get();
            var multiThresholdLevelEmitterItem = ModItems.MULTI_THRESHOLD_LEVEL_EMITTER.get();
            var networkAccessHatchType = ModBlockEntities.ME_NETWORK_ACCESS_HATCH.get();
            var namingFactoryType = ModMachines.NAMING_FACTORY.blockEntityType();

            Upgrades.add(AEItems.FUZZY_CARD, ModItems.FILTER_CELL.get(), 1);
            Upgrades.add(AEItems.FUZZY_CARD, multiLevelEmitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, multiLevelEmitterItem, 1);
            Upgrades.add(AEItems.FUZZY_CARD, multiThresholdLevelEmitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, multiThresholdLevelEmitterItem, 1);
            for (var machine : ModMachines.ALL) {
                if (machine.speedUpgrades() > 0) {
                    Upgrades.add(AEItems.SPEED_CARD, machine.block(), machine.speedUpgrades());
                }
                if (machine instanceof MultiblockMachineDefinition<?, ?>) {
                    Upgrades.add(ModItems.AEV_SPEED_CARD.get(), machine.block(), machine.speedUpgrades());
                    Upgrades.add(ModItems.AEV_PARALLEL_CARD.get(), machine.block(), machine.speedUpgrades());
                    Upgrades.add(ModItems.AEV_ENERGY_CARD.get(), machine.block(), machine.speedUpgrades());
                }
            }

            SuperStackSupport.registerUpgrades();

            StorageCells.addCellHandler(new CollapsedItemStorageCellHandler());
            StorageCells.addCellGuiHandler(new CollapsedItemStorageCellGuiHandler());
            StorageCellModels.registerModel(
                    ModItems.COLLAPSED_ITEM_STORAGE_CELL.get(), Ids.of("block/drive/collapsed_item_storage_cell"));

            AlienLavaInteractions.register();

            ModBlocks.ME_NETWORK_ACCESS_HATCH
                    .get()
                    .setBlockEntity(
                            com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlockEntity.class,
                            networkAccessHatchType,
                            null,
                            null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(
                    networkAccessHatchType, ModItems.ME_NETWORK_ACCESS_HATCH.get());
            ModMachines.NAMING_FACTORY
                    .block()
                    .setBlockEntity(NamingFactoryBlockEntity.class, namingFactoryType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(
                    namingFactoryType, ModMachines.NAMING_FACTORY.item());
        });
    }
}
