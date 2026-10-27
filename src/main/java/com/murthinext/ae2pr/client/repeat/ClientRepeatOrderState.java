package com.murthinext.ae2pr.client.repeat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.murthinext.ae2pr.network.CpuRoundInfo;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientRepeatOrderState {
    private static Map<Integer, CpuRoundInfo> bySerial = Map.of();

    private ClientRepeatOrderState() {
    }

    public static void update(int containerId, List<CpuRoundInfo> entries) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.containerMenu == null || player.containerMenu.containerId != containerId) {
            return;
        }
        var map = new HashMap<Integer, CpuRoundInfo>();
        for (var entry : entries) {
            map.put(entry.serial(), entry);
        }
        bySerial = map;
    }

    @Nullable
    public static CpuRoundInfo get(int serial) {
        return bySerial.get(serial);
    }

    public static void clear() {
        bySerial = Map.of();
    }
}
