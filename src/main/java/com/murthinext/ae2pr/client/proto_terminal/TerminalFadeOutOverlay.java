package com.murthinext.ae2pr.client.proto_terminal;

import com.murthinext.ae2pr.ae2pr;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 关闭屏幕后播放不接收输入的终端淡出画面。 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerminalFadeOutOverlay {

    private static ProtoTerminalScreen fadingScreen;

    private TerminalFadeOutOverlay() {
    }

    static void begin(ProtoTerminalScreen screen) {
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
