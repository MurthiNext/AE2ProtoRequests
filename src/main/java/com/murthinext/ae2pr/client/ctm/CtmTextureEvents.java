package com.murthinext.ae2pr.client.ctm;

import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.murthinext.ae2pr.ae2pr;

// 连接纹理的图集事件：拼接完成后缓存 CTM sprite
@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CtmTextureEvents {

    private CtmTextureEvents() {
    }

    @SubscribeEvent
    public static void onAtlasStitched(TextureStitchEvent.Post event) {
        if (!InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
            return;
        }
        CtmSprites.stitch(event.getAtlas());
    }
}
