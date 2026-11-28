package com.murthinext.ae2pr;

import appeng.api.client.StorageCellModels;
import appeng.api.parts.PartModels;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModelsHelper;
import com.murthinext.ae2pr.api.ClientHooks;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.Log;
import com.murthinext.ae2pr.api.machine.CollapseSink;
import com.murthinext.ae2pr.api.machine.MachineRegistries;
import com.murthinext.ae2pr.api.machine.MachineTuning;
import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.api.upgrade.UpgradeModifier;
import com.murthinext.ae2pr.cable_part.level_emitter.MultiLevelEmitterPart;
import com.murthinext.ae2pr.cable_part.level_emitter.MultiThresholdLevelEmitterPart;
import com.murthinext.ae2pr.block.ore.MeteoriteOres;
import com.murthinext.ae2pr.cable_part.sky_dome.SkyDomeAnnihilationPlanePart;
import com.murthinext.ae2pr.block.alien_lava.AlienLavaInteractions;
import com.murthinext.ae2pr.item.cell.collapsed.CollapseEvents;
import com.murthinext.ae2pr.item.cell.collapsed.CollapseHandler;
import com.murthinext.ae2pr.item.cell.collapsed.CollapsedCellGuiHandler;
import com.murthinext.ae2pr.item.cell.collapsed.CollapsedCellHandler;
import com.murthinext.ae2pr.integration.ae2.repeat_order.GeneralRepeatOrders;
import com.murthinext.ae2pr.item.upgrade.MachineUpgrades;
import com.murthinext.ae2pr.item.upgrade.SuperStackSupport;
import com.murthinext.ae2pr.api.WrenchInteractions;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlockEntity;
import com.murthinext.ae2pr.core.config.ServerConfig;
import com.murthinext.ae2pr.core.network.ModNetwork;
import com.murthinext.ae2pr.core.registry.*;
import java.util.Objects;
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

    public static final String MODID = Ids.MODID;
    public static final Logger LOGGER = Log.LOGGER;

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
        CollapseSink.install(CollapseHandler::dropBufferedContents);
        UpgradeModifier.install(MachineUpgrades::apply);
        ModBlocks.installStructureHooks();
        ModRegistries.registerAll(modEventBus);

        context.registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER, ServerConfig.SPEC, ServerConfig.FILE_NAME);

        // AE2 在 ModelEvent.RegisterAdditional 时冻结 part models，注册时机必须留在构造期
        ClientHooks.run(() -> () -> {
            PartModels.registerModels(PartModelsHelper.createModels(MultiLevelEmitterPart.class));
            PartModels.registerModels(PartModelsHelper.createModels(MultiThresholdLevelEmitterPart.class));
            PartModels.registerModels(PartModelsHelper.createModels(SkyDomeAnnihilationPlanePart.class));
        });
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
            var namingFactoryType = ModMachines.namingFactory().blockEntityType();

            Upgrades.add(AEItems.FUZZY_CARD, ModItems.FILTER_CELL.get(), 1);
            Upgrades.add(AEItems.FUZZY_CARD, multiLevelEmitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, multiLevelEmitterItem, 1);
            Upgrades.add(AEItems.FUZZY_CARD, multiThresholdLevelEmitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, multiThresholdLevelEmitterItem, 1);
            for (var machine : Objects.requireNonNull(ModMachines.all(), "ModMachines.register() not called")) {
                if (machine.speedUpgrades() > 0) {
                    Upgrades.add(AEItems.SPEED_CARD, machine.block(), machine.speedUpgrades());
                }
                if (machine instanceof MultiblockMachineDefinition<?>) {
                    Upgrades.add(ModItems.AEV_SPEED_CARD.get(), machine.block(), machine.speedUpgrades());
                    Upgrades.add(ModItems.AEV_PARALLEL_CARD.get(), machine.block(), machine.speedUpgrades());
                    Upgrades.add(ModItems.AEV_ENERGY_CARD.get(), machine.block(), machine.speedUpgrades());
                }
            }

            SuperStackSupport.registerUpgrades();

            StorageCells.addCellHandler(new CollapsedCellHandler());
            StorageCells.addCellGuiHandler(new CollapsedCellGuiHandler());
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
                    networkAccessHatchType, ModBlocks.ME_NETWORK_ACCESS_HATCH.item());
            ModMachines.namingFactory()
                    .block()
                    .setBlockEntity(NamingFactoryBlockEntity.class, namingFactoryType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(
                    namingFactoryType, ModMachines.namingFactory().item());

            ModSelfCheck.run();
        });
    }
}
