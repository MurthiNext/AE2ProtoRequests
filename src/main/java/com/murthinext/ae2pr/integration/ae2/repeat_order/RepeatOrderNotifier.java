package com.murthinext.ae2pr.integration.ae2.repeat_order;

import appeng.api.features.IPlayerRegistry;
import appeng.api.stacks.AEKey;
import com.murthinext.ae2pr.integration.ae2.repeat_order.network.RepeatOrderFailedPacket;
import com.murthinext.ae2pr.integration.ae2.repeat_order.network.RepeatOrderFinishedPacket;
import com.murthinext.ae2pr.integration.ae2.repeat_order.network.RepeatOrderRoundPacket;
import com.murthinext.ae2pr.core.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// 重复订单通知（普通与 VCPU 路径共用）
public final class RepeatOrderNotifier {

    private RepeatOrderNotifier() {}

    // 通知某轮开始；remainingRounds 小于等于 0 时清除标记
    public static void sendRound(
            @Nullable Integer ownerPlayerId,
            @Nullable Level level,
            @Nullable AEKey what,
            int totalRounds,
            int remainingRounds) {
        var player = resolvePlayer(ownerPlayerId, level);
        if (player == null || what == null) {
            return;
        }
        ModNetwork.sendToPlayer(player, new RepeatOrderRoundPacket(what, remainingRounds, totalRounds));
    }

    public static void sendFinished(
            @Nullable Integer ownerPlayerId,
            @Nullable Level level,
            @Nullable AEKey what,
            long amountPerRound,
            int totalRounds) {
        var player = resolvePlayer(ownerPlayerId, level);
        if (player == null || what == null) {
            return;
        }
        ModNetwork.sendToPlayer(player, new RepeatOrderFinishedPacket(what, amountPerRound, totalRounds));
    }

    public static void sendFailure(
            @Nullable Integer ownerPlayerId,
            @Nullable Level level,
            @Nullable AEKey what,
            long amountPerRound,
            int totalRounds,
            int completedRounds,
            FailureReason reason) {
        var player = resolvePlayer(ownerPlayerId, level);
        if (player == null || what == null) {
            return;
        }
        ModNetwork.sendToPlayer(
                player, new RepeatOrderFailedPacket(what, amountPerRound, totalRounds, completedRounds, reason));
    }

    @Nullable
    private static ServerPlayer resolvePlayer(@Nullable Integer ownerPlayerId, @Nullable Level level) {
        if (ownerPlayerId == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return IPlayerRegistry.getConnected(serverLevel.getServer(), ownerPlayerId);
    }
}
