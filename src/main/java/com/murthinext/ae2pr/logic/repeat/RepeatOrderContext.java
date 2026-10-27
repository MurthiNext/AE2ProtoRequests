package com.murthinext.ae2pr.logic.repeat;

import java.util.concurrent.Future;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.crafting.execution.ExecutingCraftingJob;

public final class RepeatOrderContext {
    public AEKey what;
    public long amountPerRound;
    @Nullable
    public ICraftingPlan currentPlan;
    @Nullable
    public Future<ICraftingPlan> recalc;
    public IActionSource source;
    @Nullable
    public Integer ownerPlayerId;
    public int totalRounds;
    public int completedRounds;
    public int retryCount;
    public int retryCooldownTicks;
    public int notifyCooldownTicks;
    @Nullable
    public ExecutingCraftingJob currentJob;
    public RepeatOrderState state = RepeatOrderState.RUNNING;

    public int currentRound() {
        return Math.min(completedRounds + 1, totalRounds);
    }
}
