package com.murthinext.ae2pr.item.proto_terminal;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.murthinext.ae2pr.ae2pr;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerminalFadeOutRenderer {

    private static TerminalScreen fadingScreen;

    private TerminalFadeOutRenderer() {
    }

    static void begin(TerminalScreen screen) {
        fadingScreen = screen;
    }

    static void clear() {
        fadingScreen = null;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (fadingScreen == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.screen != null || fadingScreen.isFadeOutFinished()) {
            clear();
            return;
        }
        fadingScreen.renderFadeOut(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && fadingScreen != null && fadingScreen.isFadeOutFinished()) {
            clear();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
