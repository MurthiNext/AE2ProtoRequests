package com.murthinext.ae2pr.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.init.client.InitScreens;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModMenus;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.ctm.CtmBakedModel;
import com.murthinext.ae2pr.client.assembly_line.AssemblyLineScreen;
import com.murthinext.ae2pr.client.assembly_line.FluidHatchScreen;
import com.murthinext.ae2pr.client.assembly_line.ItemBusScreen;
import com.murthinext.ae2pr.client.emitter.MultiLevelEmitterScreen;
import com.murthinext.ae2pr.client.emitter.MultiThresholdLevelEmitterScreen;
import com.murthinext.ae2pr.client.lava_smelter.LavaSmelterRenderer;
import com.murthinext.ae2pr.client.lava_smelter.LavaSmelterScreen;
import com.murthinext.ae2pr.client.naming_factory.NamingFactoryRenderer;
import com.murthinext.ae2pr.client.naming_factory.NamingFactoryScreen;
import com.murthinext.ae2pr.client.requester.RedstoneRequesterScreen;

/**
 * 客户端初始化：注册界面。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        InitScreens.register(ModMenus.MULTI_LEVEL_EMITTER.get(),
                MultiLevelEmitterScreen::new,
                "/screens/multi_level_emitter.json");
        InitScreens.register(ModMenus.MULTI_THRESHOLD_LEVEL_EMITTER.get(),
                MultiThresholdLevelEmitterScreen::new,
                "/screens/multi_threshold_level_emitter.json");
        InitScreens.register(ModMenus.REDSTONE_REQUESTER.get(),
                RedstoneRequesterScreen::new,
                "/screens/redstone_requester.json");
        MenuScreens.register(ModMenus.CRYSTAL_ASSEMBLY_LINE.get(), AssemblyLineScreen::new);
        MenuScreens.register(ModMenus.HIGH_REACTIVITY_LAVA_SMELTER.get(), LavaSmelterScreen::new);
        MenuScreens.register(ModMenus.CERTUS_QUARTZ_CRYSTAL_ITEM_BUS.get(), ItemBusScreen::new);
        MenuScreens.register(ModMenus.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), FluidHatchScreen::new);
        MenuScreens.register(ModMenus.NAMING_FACTORY.get(), NamingFactoryScreen::new);
    }

    /** 名称压印工厂：forge 使用附加模型渲染（连杆手工绘制）。 */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(NamingFactoryRenderer.FORGE_MODEL);
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        NamingFactoryRenderer.acceptModels(event.getModels());
        logNamingFactorySprites(event.getModelManager());
    }

    /** 诊断：确认名称压印工厂相关贴图确实进入方块图集（缺失时会显示 minecraft:missingno）。 */
    private static void logNamingFactorySprites(ModelManager modelManager) {
        var atlas = modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS);
        for (String path : new String[] { "naming_factory", "naming_factory_rod", "naming_factory_glass" }) {
            var sprite = atlas.getSprite(new ResourceLocation(ae2pr.MODID, "block/" + path));
            ae2pr.LOGGER.info("atlas sprite {} -> {}", path, sprite.contents().name());
        }
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NAMING_FACTORY.get(), NamingFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HIGH_REACTIVITY_LAVA_SMELTER.get(),
                LavaSmelterRenderer::new);
    }

    /** 为 ae2pr 的方块模型套上连接纹理包装（按世界邻居重写 UV）。 */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (var entry : event.getModels().entrySet()) {
            if (entry.getKey() instanceof net.minecraft.client.resources.model.ModelResourceLocation mrl
                    && ae2pr.MODID.equals(mrl.getNamespace())
                    && !(entry.getValue() instanceof CtmBakedModel)) {
                entry.setValue(new CtmBakedModel(entry.getValue()));
            }
        }
    }

    /** 水晶装配线：透明/发光覆盖层需要 cutout 渲染类型。 */
    @SubscribeEvent
    public static void registerRenderLayers(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CRYSTAL_GLASS.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AEV_INPUT_BUS.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AEV_INPUT_HATCH.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AEV_OUTPUT_BUS.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AEV_OUTPUT_HATCH.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NAMING_FACTORY.get(), RenderType.cutoutMipped());
        });
    }
}
