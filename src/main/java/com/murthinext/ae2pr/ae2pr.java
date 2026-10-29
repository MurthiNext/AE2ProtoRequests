package com.murthinext.ae2pr;

import appeng.api.parts.PartModels;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModelsHelper;
import com.mojang.logging.LogUtils;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterPart;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterPart;
import com.murthinext.ae2pr.block.meteorite.MeteoriteOres;
import com.murthinext.ae2pr.logic.repeat.GenericRepeatOrders;
import com.murthinext.ae2pr.logic.wrench.WrenchInteractions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // 部件模型必须在预初始化阶段注册
        PartModels.registerModels(PartModelsHelper.createModels(MultiLevelEmitterPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(MultiThresholdLevelEmitterPart.class));
        ModNetwork.register();
        // 通用重复订单：由服务端 tick 驱动
        MinecraftForge.EVENT_BUS.addListener(GenericRepeatOrders::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(GenericRepeatOrders::onServerStopped);
        // 扳手交互：右键旋转，Shift+右键拆除
        MinecraftForge.EVENT_BUS.addListener(WrenchInteractions::onRightClickBlock);
        // 陨石矿石：铁镐以上挖掘速度同天空石
        MinecraftForge.EVENT_BUS.addListener(MeteoriteOres::onBreakSpeed);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.murthinext.ae2pr.client.ModGuide.init();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AE2 Proto Requests initialized");
        event.enqueueWork(() -> {
            // 过滤元件升级
            Upgrades.add(AEItems.FUZZY_CARD, ModItems.FILTER_CELL.get(), 1);

            // 通式标准发信器升级
            var emitterItem = ModItems.MULTI_LEVEL_EMITTER.get();
            Upgrades.add(AEItems.FUZZY_CARD, emitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, emitterItem, 1);
            var thresholdEmitterItem = ModItems.MULTI_THRESHOLD_LEVEL_EMITTER.get();
            Upgrades.add(AEItems.FUZZY_CARD, thresholdEmitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, thresholdEmitterItem, 1);

            // 多方块机器升级
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.NAMING_FACTORY.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.CRYSTAL_ASSEMBLY_LINE.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.MATTER_PHASE_TRANSFORMER.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.CRYSTAL_REACTION_CHAMBER.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.CRYSTAL_WORKSTATION.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.JIG.get(), 4);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.MULTI_FACTORY.get(), 4);

            // 绑定方块实体类型并登记代表物品
            var networkAccessHatchType = ModBlockEntities.ME_NETWORK_ACCESS_HATCH.get();
            ModBlocks.ME_NETWORK_ACCESS_HATCH.get().setBlockEntity(
                    com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlockEntity.class,
                    networkAccessHatchType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(networkAccessHatchType,
                    ModItems.ME_NETWORK_ACCESS_HATCH.get());

            var namingFactoryType = ModBlockEntities.NAMING_FACTORY.get();
            ModBlocks.NAMING_FACTORY.get().setBlockEntity(
                    com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity.class,
                    namingFactoryType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(namingFactoryType,
                    ModItems.NAMING_FACTORY.get());
        });
    }
}
