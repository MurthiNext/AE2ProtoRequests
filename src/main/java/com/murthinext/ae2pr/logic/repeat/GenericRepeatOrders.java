package com.murthinext.ae2pr.logic.repeat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ae2pr;

import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.stacks.AEKey;
import appeng.core.sync.packets.CraftingJobStatusPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

// 通用重复订单管理器，接管 VCPU 型 CPU
public final class GenericRepeatOrders {
    private static final List<GenericRepeatOrder> ORDERS = new ArrayList<>();
    private static final Map<ICraftingCPU, GenericRepeatOrder> BY_CPU = new IdentityHashMap<>();
    private static final ThreadLocal<PendingStatus> PENDING_STATUS = new ThreadLocal<>();

    private static boolean observerFailureLogged;

    private GenericRepeatOrders() {
    }

    // 状态包构造时暂存参数（Mixin 调用）
    public static void observeConstruction(UUID jobId, AEKey what, long requestedAmount,
            CraftingJobStatusPacket.Status status) {
        PENDING_STATUS.set(new PendingStatus(jobId, what, requestedAmount, status));
    }

    @Nullable
    public static PendingStatus pollConstruction() {
        var pending = PENDING_STATUS.get();
        PENDING_STATUS.remove();
        return pending;
    }

    // 状态包观察失败的兜底日志（只记录一次）
    public static void logObserverFailure(Throwable t) {
        if (!observerFailureLogged) {
            observerFailureLogged = true;
            ae2pr.LOGGER.error("Failed to observe crafting job status packet; VCPU repeat orders may not work", t);
        }
    }

    public static void register(GenericRepeatOrder order) {
        ORDERS.add(order);
        if (order.displayCpu != null) {
            BY_CPU.put(order.displayCpu, order);
        }
    }

    @Nullable
    public static RoundInfo getRoundInfo(ICraftingCPU cpu) {
        var order = BY_CPU.get(cpu);
        return order == null ? null : new RoundInfo(order.currentRound(), order.totalRounds);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (var it = ORDERS.iterator(); it.hasNext();) {
            var order = it.next();
            boolean keep;
            try {
                keep = tick(order);
            } catch (Throwable t) {
                ae2pr.LOGGER.warn("Generic repeat order tick failed", t);
                keep = false;
            }
            if (!keep) {
                detachDisplayCpu(order);
                it.remove();
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ORDERS.clear();
        BY_CPU.clear();
    }

    public static void onJobStatus(ServerPlayer player, UUID jobId, AEKey what, long requestedAmount,
            CraftingJobStatusPacket.Status status) {
        if (status == null) {
            return;
        }
        var playerId = IPlayerRegistry.getPlayerId(player);
        switch (status) {
            case STARTED -> onStarted(playerId, jobId, what, requestedAmount);
            case FINISHED -> onFinished(playerId, jobId, what, requestedAmount);
            case CANCELLED -> onCancelled(playerId, jobId, what, requestedAmount);
        }
    }

    private static void onStarted(int playerId, UUID jobId, AEKey what, long requestedAmount) {
        for (var order : ORDERS) {
            if (order.jobId != null || order.state != RepeatOrderState.PLANNING) {
                continue;
            }
            if (order.ownerPlayerId == null || order.ownerPlayerId != playerId) {
                continue;
            }
            if (!matches(order.what, what) || order.pendingFinalAmount != requestedAmount) {
                continue;
            }
            order.jobId = jobId;
            return;
        }
    }

    private static void onFinished(int playerId, UUID jobId, AEKey what, long requestedAmount) {
        var order = findRunning(playerId, jobId, what, requestedAmount);
        if (order == null) {
            return;
        }
        order.jobId = null;
        order.completedRounds++;
        order.retryCount = 0;
        order.state = order.completedRounds >= order.totalRounds
                ? RepeatOrderState.DONE
                : RepeatOrderState.PENDING_NEXT;
    }

    private static void onCancelled(int playerId, UUID jobId, AEKey what, long requestedAmount) {
        var order = findRunning(playerId, jobId, what, requestedAmount);
        if (order == null) {
            return;
        }
        order.state = RepeatOrderState.CANCELLED;
        RepeatOrderNotifier.sendRound(order.ownerPlayerId, order.level, order.what, order.totalRounds, 0);
    }

    private static boolean tick(GenericRepeatOrder order) {
        if (order.level.getServer() == null || !order.level.getServer().isRunning() || order.what == null) {
            return false;
        }

        if (order.state == RepeatOrderState.DONE) {
            // 延迟到 tick 末尾发送，原生完成包先到
            RepeatOrderNotifier.sendFinished(order.ownerPlayerId, order.level, order.what, order.amountPerRound,
                    order.totalRounds);
            return false;
        }
        if (order.state == RepeatOrderState.CANCELLED || order.state == RepeatOrderState.FAILED) {
            return false;
        }

        if (--order.notifyCooldownTicks <= 0) {
            order.notifyCooldownTicks = 20;
            if (order.state == RepeatOrderState.RUNNING || order.state == RepeatOrderState.PENDING_NEXT) {
                RepeatOrderNotifier.sendRound(order.ownerPlayerId, order.level, order.what, order.totalRounds,
                        order.totalRounds - order.completedRounds);
            }
        }

        switch (order.state) {
            case PENDING_NEXT -> beginRecalc(order);
            case PLANNING -> pollRecalc(order);
            case RETRY_WAIT -> {
                if (--order.retryCooldownTicks <= 0) {
                    beginRecalc(order);
                }
            }
            default -> {
            }
        }
        return true;
    }

    private static void beginRecalc(GenericRepeatOrder order) {
        if (order.source == null) {
            retryOrFail(order, FailureReason.UNKNOWN);
            return;
        }
        order.jobId = null;
        try {
            order.recalc = order.grid.getCraftingService().beginCraftingCalculation(order.level,
                    () -> order.source, order.what, order.amountPerRound, CalculationStrategy.CRAFT_LESS);
            order.state = RepeatOrderState.PLANNING;
        } catch (Throwable t) {
            ae2pr.LOGGER.warn("Failed to recalculate generic repeat order plan", t);
            retryOrFail(order, FailureReason.UNKNOWN);
        }
    }

    private static void pollRecalc(GenericRepeatOrder order) {
        var future = order.recalc;
        if (future == null) {
            order.state = RepeatOrderState.PENDING_NEXT;
            return;
        }
        if (!future.isDone()) {
            return;
        }
        order.recalc = null;

        ICraftingPlan plan;
        try {
            plan = future.get();
        } catch (Throwable t) {
            retryOrFail(order, FailureReason.UNKNOWN);
            return;
        }
        if (plan == null || plan.simulation()) {
            retryOrFail(order, FailureReason.MISSING_MATERIAL);
            return;
        }
        order.pendingFinalAmount = plan.finalOutput().amount();
        submit(order, plan);
    }

    private static void submit(GenericRepeatOrder order, ICraftingPlan plan) {
        var service = order.grid.getCraftingService();
        var busyBefore = busyCpus(order.grid);
        ICraftingSubmitResult result;
        try {
            result = service.submitJob(plan, null, order.cpu, true, order.source);
            if (result == null || !result.successful()) {
                // 部分 VCPU 不接受直接提交，退回自动选择
                result = service.submitJob(plan, null, null, true, order.source);
            }
        } catch (Throwable t) {
            ae2pr.LOGGER.warn("Failed to submit next generic repeat order round", t);
            retryOrFail(order, FailureReason.UNKNOWN);
            return;
        }

        if (result != null && result.successful()) {
            order.lastFinalAmount = plan.finalOutput().amount();
            order.retryCount = 0;
            order.state = RepeatOrderState.RUNNING;
            updateDisplayCpu(order, busyBefore);
            RepeatOrderNotifier.sendRound(order.ownerPlayerId, order.level, order.what, order.totalRounds,
                    order.totalRounds - order.completedRounds);
            order.notifyCooldownTicks = 20;
        } else {
            retryOrFail(order, FailureReason.fromCode(result != null ? result.errorCode() : null));
        }
    }

    private static void retryOrFail(GenericRepeatOrder order, FailureReason reason) {
        order.recalc = null;
        if (++order.retryCount > Config.retryCount()) {
            ae2pr.LOGGER.info("Generic repeat order failed: {}x{} after {} retries (reason: {})",
                    order.amountPerRound, order.what, order.retryCount - 1, reason);
            order.state = RepeatOrderState.FAILED;
            RepeatOrderNotifier.sendRound(order.ownerPlayerId, order.level, order.what, order.totalRounds, 0);
            RepeatOrderNotifier.sendFailure(order.ownerPlayerId, order.level, order.what, order.amountPerRound,
                    order.totalRounds, order.completedRounds, reason);
            return;
        }
        order.retryCooldownTicks = Config.retryIntervalTicks();
        order.state = RepeatOrderState.RETRY_WAIT;
    }

    @Nullable
    private static GenericRepeatOrder findRunning(int playerId, UUID jobId, AEKey what, long requestedAmount) {
        GenericRepeatOrder fallback = null;
        for (var order : ORDERS) {
            if (order.state != RepeatOrderState.RUNNING
                    || order.ownerPlayerId == null || order.ownerPlayerId != playerId) {
                continue;
            }
            if (jobId != null && jobId.equals(order.jobId)) {
                return order;
            }
            if (order.jobId == null && matches(order.what, what) && order.lastFinalAmount == requestedAmount) {
                if (fallback == null) {
                    fallback = order;
                }
            }
        }
        return fallback;
    }

    private static boolean matches(@Nullable AEKey orderKey, AEKey packetKey) {
        return orderKey != null && orderKey.equals(packetKey);
    }

    private static Set<ICraftingCPU> busyCpus(IGrid grid) {
        Set<ICraftingCPU> busy = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var cpu : grid.getCraftingService().getCpus()) {
            if (cpu.isBusy()) {
                busy.add(cpu);
            }
        }
        return busy;
    }

    // 刷新展示用 CPU，取新进入忙碌者；提交目标不变
    private static void updateDisplayCpu(GenericRepeatOrder order, Set<ICraftingCPU> busyBefore) {
        ICraftingCPU newlyBusy = null;
        for (var cpu : order.grid.getCraftingService().getCpus()) {
            if (cpu.isBusy() && !busyBefore.contains(cpu)) {
                newlyBusy = cpu;
                break;
            }
        }
        if (newlyBusy != null) {
            attachDisplayCpu(order, newlyBusy);
        }
    }

    private static void attachDisplayCpu(GenericRepeatOrder order, @Nullable ICraftingCPU cpu) {
        if (order.displayCpu == cpu) {
            return;
        }
        detachDisplayCpu(order);
        order.displayCpu = cpu;
        if (cpu != null) {
            BY_CPU.put(cpu, order);
        }
    }

    private static void detachDisplayCpu(GenericRepeatOrder order) {
        if (order.displayCpu != null && BY_CPU.get(order.displayCpu) == order) {
            BY_CPU.remove(order.displayCpu);
        }
        order.displayCpu = null;
    }

    // 状态界面展示信息
    public record RoundInfo(int round, int total) {
    }

    // 发送用状态包构造时暂存的参数
    public record PendingStatus(UUID jobId, @Nullable AEKey what, long requestedAmount,
            CraftingJobStatusPacket.Status status) {
    }
}
