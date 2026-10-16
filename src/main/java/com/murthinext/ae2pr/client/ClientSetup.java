package com.murthinext.ae2pr.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.init.client.InitScreens;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModMenus;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.ctm.CtmBakedModel;
import com.murthinext.ae2pr.client.gui.tooltip.ColorEffectClientTooltip;
import com.murthinext.ae2pr.client.gui.tooltip.ColorEffectLine;
import com.murthinext.ae2pr.client.gui.assembly_line.AssemblyLineScreen;
import com.murthinext.ae2pr.client.gui.crystal_reaction_chamber.CrystalReactionChamberRenderer;
import com.murthinext.ae2pr.client.gui.crystal_reaction_chamber.CrystalReactionChamberScreen;
import com.murthinext.ae2pr.client.gui.machine_part.FluidHatchScreen;
import com.murthinext.ae2pr.client.gui.machine_part.ItemBusScreen;
import com.murthinext.ae2pr.client.gui.level_emitter.MultiLevelEmitterScreen;
import com.murthinext.ae2pr.client.gui.level_emitter.MultiThresholdLevelEmitterScreen;
import com.murthinext.ae2pr.client.gui.lava_smelter.LavaSmelterRenderer;
import com.murthinext.ae2pr.client.gui.lava_smelter.LavaSmelterScreen;
import com.murthinext.ae2pr.client.gui.matter_phase_transformer.MatterPhaseTransformerRenderer;
import com.murthinext.ae2pr.client.gui.matter_phase_transformer.MatterPhaseTransformerScreen;
import com.murthinext.ae2pr.client.gui.naming_factory.NamingFactoryRenderer;
import com.murthinext.ae2pr.client.gui.naming_factory.NamingFactoryScreen;

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
        MenuScreens.register(ModMenus.CRYSTAL_ASSEMBLY_LINE.get(), AssemblyLineScreen::new);
        MenuScreens.register(ModMenus.HIGH_REACTIVITY_LAVA_SMELTER.get(), LavaSmelterScreen::new);
        MenuScreens.register(ModMenus.MATTER_PHASE_TRANSFORMER.get(), MatterPhaseTransformerScreen::new);
        MenuScreens.register(ModMenus.CRYSTAL_REACTION_CHAMBER.get(), CrystalReactionChamberScreen::new);
        MenuScreens.register(ModMenus.CERTUS_QUARTZ_CRYSTAL_ITEM_BUS.get(), ItemBusScreen::new);
        MenuScreens.register(ModMenus.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), FluidHatchScreen::new);
        MenuScreens.register(ModMenus.NAMING_FACTORY.get(), NamingFactoryScreen::new);
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ColorEffectLine.class, ColorEffectClientTooltip::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(NamingFactoryRenderer.FORGE_MODEL);
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        NamingFactoryRenderer.acceptModels(event.getModels());
        logNamingFactorySprites(event.getModelManager());
    }

    private static void logNamingFactorySprites(ModelManager modelManager) {
        var atlas = modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS);
        for (String path : new String[] { "naming_factory", "naming_factory_rod", "naming_factory_glass" }) {
            var sprite = atlas.getSprite(ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "block/" + path));
            ae2pr.LOGGER.info("atlas sprite {} -> {}", path, sprite.contents().name());
        }
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NAMING_FACTORY.get(), NamingFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HIGH_REACTIVITY_LAVA_SMELTER.get(),
                LavaSmelterRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_PHASE_TRANSFORMER.get(),
                MatterPhaseTransformerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CRYSTAL_REACTION_CHAMBER.get(),
                CrystalReactionChamberRenderer::new);
    }

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
}
