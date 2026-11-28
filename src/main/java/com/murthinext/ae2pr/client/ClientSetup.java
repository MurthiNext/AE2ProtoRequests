package com.murthinext.ae2pr.client;

import appeng.init.client.InitScreens;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.material.MaterialItem;
import com.murthinext.ae2pr.block.level_emitter.MultiLevelEmitterScreen;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterScreen;
import com.murthinext.ae2pr.block.machine_part.FluidHatchScreen;
import com.murthinext.ae2pr.block.machine_part.ItemBusScreen;
import com.murthinext.ae2pr.block.machine_part.MEPatternBufferScreen;
import com.murthinext.ae2pr.block.molecular_assembly_core.MolecularAssemblyCoreRenderer;
import com.murthinext.ae2pr.client.tooltip.ColorEffectClientTooltip;
import com.murthinext.ae2pr.client.tooltip.ColorEffectLine;
import com.murthinext.ae2pr.client.tooltip.FormulaClientTooltip;
import com.murthinext.ae2pr.client.tooltip.FormulaTooltip;
import com.murthinext.ae2pr.item.matter_confinement_frame.MatterConfinementFrameModel;
import com.murthinext.ae2pr.item.sky_dome_annihilation_plane.SkyDomeAnnihilationPlaneModel;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryRenderer;
import com.murthinext.ae2pr.core.registry.ModBlockEntities;
import com.murthinext.ae2pr.core.registry.ModMaterials;
import com.murthinext.ae2pr.core.registry.ModMenus;
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
import net.minecraftforge.client.model.DynamicFluidContainerModel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private static final ResourceLocation COLLAPSED_ITEM_STORAGE_CELL_MODEL =
            Ids.of("block/drive/collapsed_item_storage_cell");

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            InitScreens.register(
                    ModMenus.MULTI_LEVEL_EMITTER.get(),
                    MultiLevelEmitterScreen::new,
                    "/screens/multi_level_emitter.json");
            InitScreens.register(
                    ModMenus.MULTI_THRESHOLD_LEVEL_EMITTER.get(),
                    MultiThresholdLevelEmitterScreen::new,
                    "/screens/multi_threshold_level_emitter.json");
            MachineClients.registerScreens();
            MenuScreens.register(ModMenus.MACHINE_PART_ITEM_BUS.get(), ItemBusScreen::new);
            MenuScreens.register(ModMenus.MACHINE_PART_FLUID_HATCH.get(), FluidHatchScreen::new);
            MenuScreens.register(ModMenus.ME_PATTERN_BUFFER.get(), MEPatternBufferScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ColorEffectLine.class, ColorEffectClientTooltip::new);
        event.register(FormulaTooltip.class, FormulaClientTooltip::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemLike[] items = ModMaterials.ALL.stream()
                .flatMap(material -> material.forms().stream())
                .flatMap(form -> form.ownItem().stream())
                .map(item -> (ItemLike) item.get())
                .toArray(ItemLike[]::new);
        event.register(
                (stack, layer) -> stack.getItem() instanceof MaterialItem item
                        ? item.material().argb(layer)
                        : -1,
                items);

        // 材质流体的桶共用灰度贴图，颜色来自流体的 tint
        ItemLike[] buckets = ModMaterials.ALL.stream()
                .flatMap(material -> material.fluid().stream())
                .map(set -> (ItemLike) set.bucket().get())
                .toArray(ItemLike[]::new);
        event.register(new DynamicFluidContainerModel.Colors(), buckets);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("sky_dome_annihilation_plane", SkyDomeAnnihilationPlaneModel.LOADER);
        event.register("matter_confinement_frame", MatterConfinementFrameModel.LOADER);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(NamingFactoryRenderer.FORGE_MODEL);
        event.register(COLLAPSED_ITEM_STORAGE_CELL_MODEL);
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        NamingFactoryRenderer.acceptModels(event.getModels());
        logNamingFactorySprites(event.getModelManager());
    }

    private static void logNamingFactorySprites(ModelManager modelManager) {
        var atlas = modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS);
        for (String path : new String[] {"naming_factory/machine", "naming_factory/rod", "naming_factory/glass"}) {
            var sprite = atlas.getSprite(Ids.of("block/" + path));
            ae2pr.LOGGER.info("atlas sprite {} -> {}", path, sprite.contents().name());
        }
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MachineClients.registerRenderers(event);
        event.registerBlockEntityRenderer(
                ModBlockEntities.MOLECULAR_ASSEMBLY_CORE.get(), MolecularAssemblyCoreRenderer::new);
    }
}
