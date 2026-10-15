package com.murthinext.ae2pr.client;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.gui.TooltipRenderer;
import com.murthinext.ae2pr.client.proto_terminal.ClientStoryProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端事件。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        var stack = event.getItemStack();
        if (stack.isEmpty() || !ae2pr.MODID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            return;
        }
        var elements = event.getTooltipElements();
        var prepared = TooltipRenderer.prepareElements(elements);
        elements.clear();
        elements.addAll(prepared);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRepeatOrderState.clear();
        ClientRepeatOrderFailures.clear();
        ClientRepeatOrderRounds.clear();
        ClientConfirmRounds.clear();
        ClientStoryProgress.reset();
    }
}
