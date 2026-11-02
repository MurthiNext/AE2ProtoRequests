package com.murthinext.ae2pr.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import appeng.init.client.InitScreens;

import com.murthinext.ae2pr.registry.ModBlockEntities;
import com.murthinext.ae2pr.registry.ModMenus;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.ctm.CtmBakedModel;
import com.murthinext.ae2pr.block.molecular_assembly_core.MolecularAssemblyCoreRenderer;
import com.murthinext.ae2pr.client.tooltip.ColorEffectClientTooltip;
import com.murthinext.ae2pr.client.tooltip.ColorEffectLine;
import com.murthinext.ae2pr.block.machine_part.FluidHatchScreen;
import com.murthinext.ae2pr.block.machine_part.ItemBusScreen;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterScreen;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterScreen;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryRenderer;
import com.murthinext.ae2pr.material.MaterialItem;
import com.murthinext.ae2pr.material.MaterialItems;

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
        event.enqueueWork(MachineClients::registerScreens);
        MenuScreens.register(ModMenus.CERTUS_QUARTZ_CRYSTAL_ITEM_BUS.get(), ItemBusScreen::new);
        MenuScreens.register(ModMenus.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), FluidHatchScreen::new);
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ColorEffectLine.class, ColorEffectClientTooltip::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemLike[] items = MaterialItems.all().stream().map(RegistryObject::get).toArray(ItemLike[]::new);
        event.register((stack, layer) -> stack.getItem() instanceof MaterialItem item
                ? item.material().argb(layer)
                : -1, items);
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
        MachineClients.registerRenderers(event);
        event.registerBlockEntityRenderer(ModBlockEntities.MOLECULAR_ASSEMBLY_CORE.get(),
                MolecularAssemblyCoreRenderer::new);
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
