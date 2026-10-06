package com.murthinext.ae2pr;

import appeng.api.parts.PartModels;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModelsHelper;
import com.mojang.logging.LogUtils;
import com.murthinext.ae2pr.block.assembly_line.CertusQuartzCrystalMachinePartBlock;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterPart;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterPart;
import com.murthinext.ae2pr.block.meteorite.MeteoriteOres;
import com.murthinext.ae2pr.logic.repeat.GenericRepeatOrders;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
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

    public ae2pr() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // 部件模型必须在预初始化阶段注册（AE2 在模型加载时会冻结注册表，资源重载时不能再注册）
        PartModels.registerModels(PartModelsHelper.createModels(MultiLevelEmitterPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(MultiThresholdLevelEmitterPart.class));
        ModNetwork.register();
        com.murthinext.ae2pr.block.redstone_requester.network.RequesterNetwork.init();
        // 通用（VCPU）重复订单：由服务端 tick 驱动，状态包钩子负责轮次推进
        MinecraftForge.EVENT_BUS.addListener(GenericRepeatOrders::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(GenericRepeatOrders::onServerStopped);
        // 水晶机器部件：潜行时原版会跳过 Block#use，用事件放行以实现扳手 Shift+右键拆卸
        MinecraftForge.EVENT_BUS.addListener(CertusQuartzCrystalMachinePartBlock::onRightClickBlock);
        // 陨石矿石：铁镐以上挖掘时速度与天空石一致
        MinecraftForge.EVENT_BUS.addListener(MeteoriteOres::onBreakSpeed);

        // 客户端：构建并注册 GuideME 指南
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.murthinext.ae2pr.client.ModGuide.init();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AE2 Proto Req initialized");
        event.enqueueWork(() -> {
            // 过滤元件的可用升级：仅模糊卡（1 张），与元件工作台的模糊模式开关联动
            Upgrades.add(AEItems.FUZZY_CARD, ModItems.FILTER_CELL.get(), 1);
            // 通式标准发信器的可用升级：模糊卡 + 合成卡（过滤元件走专用槽，不作为升级卡）
            var emitterItem = ModItems.MULTI_LEVEL_EMITTER.get();
            Upgrades.add(AEItems.FUZZY_CARD, emitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, emitterItem, 1);
            // 通式阈值发信器同上
            var thresholdEmitterItem = ModItems.MULTI_THRESHOLD_LEVEL_EMITTER.get();
            Upgrades.add(AEItems.FUZZY_CARD, thresholdEmitterItem, 1);
            Upgrades.add(AEItems.CRAFTING_CARD, thresholdEmitterItem, 1);

            // ME 红石请求器：绑定方块实体类型并登记代表物品
            var requesterType = ModBlockEntities.REDSTONE_REQUESTER.get();
            ModBlocks.REDSTONE_REQUESTER.get().setBlockEntity(
                    com.murthinext.ae2pr.block.redstone_requester.RedstoneRequesterBlockEntity.class, requesterType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(requesterType,
                    ModItems.REDSTONE_REQUESTER.get());

            // 福鲁伊克斯水晶能源仓：绑定方块实体类型并登记代表物品
            var energyHatchType = ModBlockEntities.FLUIX_CRYSTAL_ENERGY_HATCH.get();
            ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get().setBlockEntity(
                    com.murthinext.ae2pr.block.assembly_line.FluixCrystalEnergyHatchBlockEntity.class,
                    energyHatchType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(energyHatchType,
                    ModItems.FLUIX_CRYSTAL_ENERGY_HATCH.get());

            // 名称压印工厂：绑定方块实体类型并登记代表物品
            var namingFactoryType = ModBlockEntities.NAMING_FACTORY.get();
            ModBlocks.NAMING_FACTORY.get().setBlockEntity(
                    com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity.class,
                    namingFactoryType, null, null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(namingFactoryType,
                    ModItems.NAMING_FACTORY.get());
        });
    }
}
