package com.murthinext.ae2pr.logic.repeat;

import java.util.UUID;
import java.util.concurrent.Future;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import net.minecraft.server.level.ServerLevel;

/**
 * 通用重复订单上下文，适用于 VCPU 型 CPU。
 */
public final class GenericRepeatOrder {
    public IGrid grid;
    public ServerLevel level;
    public IActionSource source;
    @Nullable
    public Integer ownerPlayerId;
    @Nullable
    public AEKey what;
    public long amountPerRound;
    public long pendingFinalAmount;
    public long lastFinalAmount;
    public int totalRounds;
    public int completedRounds;
    public int retryCount;
    public int retryCooldownTicks;
    public int notifyCooldownTicks;
    @Nullable
    public Future<ICraftingPlan> recalc;
    @Nullable
    public ICraftingCPU cpu;
    @Nullable
    public ICraftingCPU displayCpu;
    @Nullable
    public UUID jobId;
    public RepeatOrderState state = RepeatOrderState.RUNNING;

    public int currentRound() {
        return Math.min(completedRounds + 1, totalRounds);
    }
}
