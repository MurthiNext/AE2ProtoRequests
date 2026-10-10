package com.murthinext.ae2pr.requester.setup;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.init.client.InitScreens;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.requester.client.RedstoneRequesterScreen;

/**
 * 独立源码集内红石请求器的客户端初始化：注册请求器界面。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RequesterClientSetup {

    private RequesterClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        InitScreens.register(RequesterRegistration.REDSTONE_REQUESTER_MENU.get(),
                RedstoneRequesterScreen::new,
                "/screens/redstone_requester.json");
    }
}
