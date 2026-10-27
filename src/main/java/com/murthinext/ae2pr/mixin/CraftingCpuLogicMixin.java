package com.murthinext.ae2pr.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.logic.repeat.FailureReason;
import com.murthinext.ae2pr.logic.repeat.IRepeatOrderHost;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderBinding;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderContext;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderNotifier;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderState;

import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

// 重复订单核心状态机：轮次推进、异步重算与失败重试
@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class CraftingCpuLogicMixin implements IRepeatOrderHost {

    @Shadow
    @Final
    CraftingCPUCluster cluster;

    @Shadow
    @Nullable
    private ExecutingCraftingJob job;

    @Unique
    @Nullable
    private RepeatOrderContext ae2pr$context;

    @Override
    @Nullable
    public RepeatOrderContext ae2pr$getContext() {
        return this.ae2pr$context;
    }

    @Override
    public void ae2pr$setContext(@Nullable RepeatOrderContext context) {
        this.ae2pr$context = context;
    }

    @Inject(method = "trySubmitJob", at = @At("RETURN"))
    private void ae2pr$bindRepeatOrder(IGrid grid, ICraftingPlan plan, IActionSource src,
            ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        var rounds = RepeatOrderBinding.consume();
        if (rounds == null || rounds <= 1) {
            return;
        }
        var result = cir.getReturnValue();
        if (result == null || !result.successful() || this.ae2pr$context != null) {
            return;
        }

        var context = new RepeatOrderContext();
        context.what = plan.finalOutput().what();
        context.amountPerRound = plan.finalOutput().amount();
        context.source = src;
        context.ownerPlayerId = ae2pr$getPlayerId(src);
        context.totalRounds = rounds;
        context.currentJob = this.job;
        context.state = RepeatOrderState.RUNNING;
        this.ae2pr$context = context;
        this.ae2pr$sendRoundNotify(context, context.totalRounds);
        context.notifyCooldownTicks = 20;
    }

    // 任务结束：仅处理本订单的任务；成功推进轮次，取消清空
    @Inject(method = "finishJob", at = @At("HEAD"))
    private void ae2pr$onFinishJob(boolean success, CallbackInfo ci) {
        var context = this.ae2pr$context;
        if (context == null || context.currentJob != this.job) {
            return;
        }
        context.currentJob = null;

        if (!success) {
            context.state = RepeatOrderState.CANCELLED;
            this.ae2pr$context = null;
            return;
        }

        context.completedRounds++;
        context.retryCount = 0;
        if (context.completedRounds >= context.totalRounds) {
            // 延迟到下一 tick 通知：确保 AE2 的 FINISHED 包先到并被客户端静默
            context.state = RepeatOrderState.DONE;
        } else {
            context.state = RepeatOrderState.PENDING_NEXT;
        }
    }

    @Inject(method = "tickCraftingLogic", at = @At("HEAD"))
    private void ae2pr$tickRepeatOrder(IEnergyService energyService, CraftingService craftingService,
            CallbackInfo ci) {
        var context = this.ae2pr$context;
        if (context == null) {
            return;
        }

        // 全部轮次完成：延迟一 tick 通知并清理
        if (context.state == RepeatOrderState.DONE) {
            this.ae2pr$sendFinishedNotify(context);
            this.ae2pr$context = null;
            return;
        }

        // 定期补发剩余轮数
        if (--context.notifyCooldownTicks <= 0) {
            context.notifyCooldownTicks = 20;
            this.ae2pr$sendRoundNotify(context, context.totalRounds - context.completedRounds);
        }

        if (this.job != null) {
            if (context.state == RepeatOrderState.RUNNING && context.currentJob == this.job) {
                return;
            }
            if (context.state == RepeatOrderState.PENDING_NEXT || context.state == RepeatOrderState.PLANNING
                    || context.state == RepeatOrderState.RUNNING) {
                // 两轮之间 CPU 被其他任务占用（或本任务异常消失）
                context.recalc = null;
                this.ae2pr$retryOrFail(context, FailureReason.CPU_BUSY);
            }
            return;
        }

        switch (context.state) {
            case PENDING_NEXT -> this.ae2pr$beginRecalc(context);
            case PLANNING -> this.ae2pr$pollRecalc(context);
            case RETRY_WAIT -> {
                if (--context.retryCooldownTicks <= 0) {
                    this.ae2pr$beginRecalc(context);
                }
            }
            case RUNNING -> this.ae2pr$retryOrFail(context, FailureReason.UNKNOWN);
            default -> {
            }
        }
    }

    @Inject(method = "cancel", at = @At("HEAD"))
    private void ae2pr$onCancel(CallbackInfo ci) {
        var context = this.ae2pr$context;
        if (context != null) {
            if (context.state == RepeatOrderState.DONE) {
                return; // 已完成，等待下一 tick 通知
            }
            this.ae2pr$sendRoundNotify(context, 0);
            context.recalc = null;
            context.state = RepeatOrderState.CANCELLED;
            this.ae2pr$context = null;
        }
    }

    // 持久化订单；计划不可直接序列化，恢复后重新模拟
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void ae2pr$writeRepeatOrder(CompoundTag data, CallbackInfo ci) {
        var context = this.ae2pr$context;
        if (context == null || context.what == null) {
            return;
        }
        var tag = new CompoundTag();
        tag.put("what", GenericStack.writeTag(new GenericStack(context.what, context.amountPerRound)));
        tag.putInt("total", context.totalRounds);
        tag.putInt("completed", context.completedRounds);
        if (context.ownerPlayerId != null) {
            tag.putInt("owner", context.ownerPlayerId);
        }
        data.put("ae2pr:repeat_order", tag);
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void ae2pr$readRepeatOrder(CompoundTag data, CallbackInfo ci) {
        var key = "ae2pr:repeat_order";
        if (!data.contains(key)) {
            return;
        }
        var tag = data.getCompound(key);
        var stack = GenericStack.readTag(tag.getCompound("what"));
        if (stack == null || stack.amount() <= 0) {
            return;
        }
        var total = Math.max(1, tag.getInt("total"));
        var completed = Math.max(0, Math.min(tag.getInt("completed"), total));
        if (completed >= total) {
            return;
        }

        var context = new RepeatOrderContext();
        context.what = stack.what();
        context.amountPerRound = stack.amount();
        context.totalRounds = total;
        context.completedRounds = completed;
        context.ownerPlayerId = tag.contains("owner") ? tag.getInt("owner") : null;
        context.source = null; // 恢复后改用 CPU 自身的 action source
        if (this.job != null) {
            // 存档时该轮仍在执行，AE2 已恢复任务，直接绑定以推进轮次
            context.currentJob = this.job;
            context.state = RepeatOrderState.RUNNING;
        } else {
            // recalc 为 null 时先转 PENDING_NEXT，再异步重算
            context.state = RepeatOrderState.PLANNING;
        }
        this.ae2pr$context = context;
    }

    @Unique
    private void ae2pr$beginRecalc(RepeatOrderContext context) {        var grid = this.cluster.getGrid();
        var level = this.cluster.getLevel();
        if (grid == null || level == null) {
            this.ae2pr$retryOrFail(context, FailureReason.UNKNOWN);
            return;
        }
        try {
            context.recalc = grid.getCraftingService().beginCraftingCalculation(level,
                    () -> ae2pr$contextSource(context), context.what, context.amountPerRound,
                    CalculationStrategy.CRAFT_LESS);
            context.state = RepeatOrderState.PLANNING;
        } catch (Throwable t) {
            ae2pr.LOGGER.warn("Failed to recalculate repeat order plan", t);
            this.ae2pr$retryOrFail(context, FailureReason.UNKNOWN);
        }
    }

    @Unique
    private void ae2pr$pollRecalc(RepeatOrderContext context) {
        var future = context.recalc;
        if (future == null) {
            context.state = RepeatOrderState.PENDING_NEXT;
            return;
        }
        if (!future.isDone()) {
            return;
        }
        context.recalc = null;

        ICraftingPlan plan;
        try {
            plan = future.get();
        } catch (Throwable t) {
            this.ae2pr$retryOrFail(context, FailureReason.UNKNOWN);
            return;
        }
        if (plan == null || plan.simulation()) {
            this.ae2pr$retryOrFail(context, FailureReason.MISSING_MATERIAL);
            return;
        }

        var grid = this.cluster.getGrid();
        if (grid == null) {
            this.ae2pr$retryOrFail(context, FailureReason.UNKNOWN);
            return;
        }

        ICraftingSubmitResult result;
        try {
            result = this.cluster.submitJob(grid, plan, ae2pr$contextSource(context), null);
        } catch (Throwable t) {
            ae2pr.LOGGER.warn("Failed to submit next repeat order round", t);
            this.ae2pr$retryOrFail(context, FailureReason.UNKNOWN);
            return;
        }

        if (result != null && result.successful()) {
            context.currentPlan = plan;
            context.currentJob = this.job;
            context.retryCount = 0;
            context.state = RepeatOrderState.RUNNING;
            this.ae2pr$sendRoundNotify(context, context.totalRounds - context.completedRounds);
            context.notifyCooldownTicks = 20;
        } else {
            this.ae2pr$retryOrFail(context, FailureReason.fromCode(result != null ? result.errorCode() : null));
        }
    }

    @Unique
    private void ae2pr$retryOrFail(RepeatOrderContext context, FailureReason reason) {
        context.recalc = null;
        if (++context.retryCount > Config.retryCount()) {
            ae2pr.LOGGER.info("Repeat order failed: {}x{} after {} retries (reason: {})",
                    context.amountPerRound, context.what, context.retryCount - 1, reason);
            context.state = RepeatOrderState.FAILED;
            this.ae2pr$sendRoundNotify(context, 0);
            this.ae2pr$notifyFailure(context, reason);
            this.ae2pr$context = null;
            return;
        }
        context.retryCooldownTicks = Config.retryIntervalTicks();
        context.state = RepeatOrderState.RETRY_WAIT;
    }

    @Unique
    private IActionSource ae2pr$contextSource(RepeatOrderContext context) {
        return context.source != null ? context.source : this.cluster.getSrc();
    }

    // 通知所有者轮次开始（remainingRounds 含当前轮）；&lt;= 1 表示清除中间轮标记
    @Unique
    private void ae2pr$sendRoundNotify(RepeatOrderContext context, int remainingRounds) {
        RepeatOrderNotifier.sendRound(context.ownerPlayerId, this.cluster.getLevel(), context.what,
                context.totalRounds, remainingRounds);
    }

    @Unique
    private void ae2pr$sendFinishedNotify(RepeatOrderContext context) {
        RepeatOrderNotifier.sendFinished(context.ownerPlayerId, this.cluster.getLevel(), context.what,
                context.amountPerRound, context.totalRounds);
    }

    @Unique
    private void ae2pr$notifyFailure(RepeatOrderContext context, FailureReason reason) {
        RepeatOrderNotifier.sendFailure(context.ownerPlayerId, this.cluster.getLevel(), context.what,
                context.amountPerRound, context.totalRounds, context.completedRounds, reason);
    }

    @Unique
    @Nullable
    private static Integer ae2pr$getPlayerId(IActionSource source) {
        var player = source.player().orElse(null);
        if (player instanceof ServerPlayer serverPlayer) {
            return IPlayerRegistry.getPlayerId(serverPlayer);
        }
        return null;
    }
}
