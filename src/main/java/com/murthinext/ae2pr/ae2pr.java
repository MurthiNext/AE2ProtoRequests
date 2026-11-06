package com.murthinext.ae2pr;

import appeng.api.parts.PartModels;
import appeng.api.client.StorageCellModels;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModelsHelper;
import com.mojang.logging.LogUtils;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterPart;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterPart;
import com.murthinext.ae2pr.block.meteorite.MeteoriteOres;
import com.murthinext.ae2pr.item.collapsed_cell.CollapsedItemCellGuiHandler;
import com.murthinext.ae2pr.item.collapsed_cell.CollapsedItemCellHandler;
import com.murthinext.ae2pr.logic.collapse.CollapseEvents;
import com.murthinext.ae2pr.logic.repeat.GeneralRepeatOrders;
import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;
import com.murthinext.ae2pr.logic.wrench.WrenchInteractions;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlockEntity;
import com.murthinext.ae2pr.api.machine.MachineRegistries;
import com.murthinext.ae2pr.api.machine.MachineTuning;
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
        // 机器模板的注册表需在 ModMachines 静态初始化前注入
        MachineRegistries.install(ModBlocks.BLOCKS, ModItems.ITEMS, ModBlockEntities.BLOCK_ENTITIES,
                ModMenus.MENUS, ModRecipes.RECIPE_TYPES, ModRecipes.SERIALIZERS);
        MachineTuning.install(ModConfig::lossySpeedPerCard, ModConfig::lossyEnergyMultiplierPerCard);
        ModMaterials.init();
        ModMachines.init();
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
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
        // 部件模型必须在预初始化阶段注册
        PartModels.registerModels(PartModelsHelper.createModels(MultiLevelEmitterPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(MultiThresholdLevelEmitterPart.class));
        ModNetwork.register();
        // 通用重复订单：由服务端 tick 驱动
        MinecraftForge.EVENT_BUS.addListener(GeneralRepeatOrders::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(GeneralRepeatOrders::onServerStopped);
        // 扳手交互：右键旋转，Shift+右键拆除
        MinecraftForge.EVENT_BUS.addListener(WrenchInteractions::onRightClickBlock);
        // 陨石矿石：铁镐以上挖掘速度同天空石
        MinecraftForge.EVENT_BUS.addListener(MeteoriteOres::onBreakSpeed);
        // 坍缩物品元件：第三方机器的通用破坏入口
        MinecraftForge.EVENT_BUS.addListener(CollapseEvents::onBlockBreak);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModGuide.init();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AE2 Proto Requests initialized");
        event.enqueueWork(() -> {
            // 过滤元件升级
            Upgrades.add(AEItems.FUZZY_CARD, ModItems.FILTER_CELL.get(), 1);

            // 超级堆叠卡：接口与样板供应器
            SuperStackSupport.registerUpgrades();

            // 坍缩物品元件：作为只读存储元件接入 ME 网络
            StorageCells.addCellHandler(new CollapsedItemCellHandler());
            StorageCells.addCellGuiHandler(new CollapsedItemCellGuiHandler());
            StorageCellModels.registerModel(ModItems.COLLAPSED_ITEM_CELL.get(),
                    Ids.of("block/drive/collapsed_item_cell"));

            // 通式标准发信器升级
            var emitterItem = ModItems.MULTI_LEVEL_EMITTER.get();
            Upgrades.add(AEItems.FUZZY_CARD, emitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, emitterItem, 1);
            var thresholdEmitterItem = ModItems.MULTI_THRESHOLD_LEVEL_EMITTER.get();
            Upgrades.add(AEItems.FUZZY_CARD, thresholdEmitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, thresholdEmitterItem, 1);

            // 多方块机器升级
            for (var machine : ModMachines.ALL) {
                if (machine.speedUpgrades() > 0) {
                    Upgrades.add(AEItems.SPEED_CARD, machine.block(), machine.speedUpgrades());
                }
            }

            // 绑定方块实体类型并登记代表物品
            var networkAccessHatchType = ModBlockEntities.ME_NETWORK_ACCESS_HATCH.get();
            ModBlocks.ME_NETWORK_ACCESS_HATCH.get().setBlockEntity(
                    com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlockEntity.class,
                    networkAccessHatchType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(networkAccessHatchType,
                    ModItems.ME_NETWORK_ACCESS_HATCH.get());

            var namingFactoryType = ModMachines.NAMING_FACTORY.blockEntityType();
            ModMachines.NAMING_FACTORY.block().setBlockEntity(
                    NamingFactoryBlockEntity.class,
                    namingFactoryType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(namingFactoryType,
                    ModMachines.NAMING_FACTORY.item());
        });
    }
}
