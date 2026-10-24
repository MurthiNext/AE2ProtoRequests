package com.murthinext.ae2pr.item.crystaline;

import java.util.List;

import net.minecraft.network.chat.Component;

import com.murthinext.ae2pr.client.tooltip.TooltipRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CrystalineTooltip {

    // 青蓝色色表，逐字符顺序取色并随时间流动
    private static final int[] GROWTH_COLORS = {
            0x0055FF, 0x0077FF, 0x0099FF, 0x00BBFF, 0x00DDFF, 0x00FFFF
    };

    private CrystalineTooltip() {
    }

    public static void append(List<Component> tooltip) {
        tooltip.add(TooltipRenderer.flow(
                Component.translatable("tooltip.ae2pr.crystaline.desc").getString(), GROWTH_COLORS));
    }
}
