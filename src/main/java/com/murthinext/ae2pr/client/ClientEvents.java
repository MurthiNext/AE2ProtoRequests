package com.murthinext.ae2pr.client;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.gui.TooltipRenderer;
import com.murthinext.ae2pr.client.proto_terminal.ClientStoryProgress;
import com.murthinext.ae2pr.client.repeat.ClientRepeatOrderFailures;
import com.murthinext.ae2pr.client.repeat.ClientRepeatOrderRounds;
import com.murthinext.ae2pr.client.repeat.ClientRepeatOrderState;
import net.minecraft.client.Minecraft;
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
        if (stack.isEmpty()) {
            return;
        }
        var elements = event.getTooltipElements();
        // 本模组物品沿用折行处理，其余仅在有特效时接管
        boolean ours = ae2pr.MODID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
        if (!ours && !TooltipRenderer.containsEffect(elements)) {
            return;
        }
        // 特效行由自定义组件渲染，需在此处自行折行
        int maxWidth = event.getMaxWidth() > 0 ? event.getMaxWidth() : event.getScreenWidth() - 32;
        var prepared = TooltipRenderer.prepareElements(elements, Minecraft.getInstance().font, maxWidth);
        elements.clear();
        elements.addAll(prepared);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRepeatOrderState.clear();
        ClientRepeatOrderFailures.clear();
        ClientRepeatOrderRounds.clear();
        ClientStoryProgress.reset();
    }
}
