package com.murthinext.ae2pr.client.ctm;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.ctm.CTMBakedModel;
import com.murthinext.ae2pr.api.ctm.CTMTextures;
import com.murthinext.ae2pr.block.casing.EntangledCasingPulseRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientCTM {

    private ClientCTM() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MachineCTM.registerTextures();
    }

    @SubscribeEvent
    public static void onAtlasStitched(TextureStitchEvent.Post event) {
        if (!InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
            return;
        }
        CTMTextures.stitch(event.getAtlas());
        EntangledCasingPulseRenderer.stitch(event.getAtlas());
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (var entry : event.getModels().entrySet()) {
            if (entry.getKey() instanceof ModelResourceLocation id
                    && ae2pr.MODID.equals(id.getNamespace())
                    && !(entry.getValue() instanceof CTMBakedModel)) {
                entry.setValue(new CTMBakedModel(entry.getValue(), MachineCTM.INSTANCE));
            }
        }
    }
}
