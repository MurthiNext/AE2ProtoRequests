package com.murthinext.ae2pr.client.ctm;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.ctm.CtmBakedModel;
import com.murthinext.ae2pr.api.ctm.CtmTextures;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientCtm {

    private ClientCtm() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MachineCtm.registerTextures();
    }

    @SubscribeEvent
    public static void onAtlasStitched(TextureStitchEvent.Post event) {
        if (!InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
            return;
        }
        CtmTextures.stitch(event.getAtlas());
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (var entry : event.getModels().entrySet()) {
            if (entry.getKey() instanceof ModelResourceLocation id
                    && ae2pr.MODID.equals(id.getNamespace())
                    && !(entry.getValue() instanceof CtmBakedModel)) {
                entry.setValue(new CtmBakedModel(entry.getValue(), MachineCtm.INSTANCE));
            }
        }
    }
}
