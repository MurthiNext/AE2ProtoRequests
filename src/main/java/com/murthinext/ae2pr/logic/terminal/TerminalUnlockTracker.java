package com.murthinext.ae2pr.logic.terminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.network.TerminalStoryUnlockPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 记录打开终端的玩家关心的解锁条件，每秒重新判定一次并在结果变化时推送。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerminalUnlockTracker {

    private static final int INTERVAL_TICKS = 20;

    private static final class Watch {

        private final List<String> conditions;
        private Set<String> satisfied;

        private Watch(List<String> conditions, Set<String> satisfied) {
            this.conditions = conditions;
            this.satisfied = satisfied;
        }
    }

    private static final Map<UUID, Watch> watches = new HashMap<>();
    private static int tick;

    private TerminalUnlockTracker() {
    }

    public static void watch(ServerPlayer player, List<String> keys) {
        if (keys.isEmpty()) {
            watches.remove(player.getUUID());
            return;
        }
        List<String> satisfied = TerminalUnlockCondition.satisfied(player, keys);
        watches.put(player.getUUID(), new Watch(List.copyOf(keys), new HashSet<>(satisfied)));
        ModNetwork.sendToPlayer(player, new TerminalStoryUnlockPacket(satisfied));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || watches.isEmpty() || ++tick % INTERVAL_TICKS != 0) {
            return;
        }
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Watch> entry : watches.entrySet()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                stale.add(entry.getKey());
                continue;
            }
            Watch watch = entry.getValue();
            List<String> satisfied = TerminalUnlockCondition.satisfied(player, watch.conditions);
            if (watch.satisfied.equals(new HashSet<>(satisfied))) {
                continue;
            }
            watch.satisfied = new HashSet<>(satisfied);
            ModNetwork.sendToPlayer(player, new TerminalStoryUnlockPacket(satisfied));
        }
        stale.forEach(watches::remove);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        watches.clear();
        tick = 0;
    }
}
