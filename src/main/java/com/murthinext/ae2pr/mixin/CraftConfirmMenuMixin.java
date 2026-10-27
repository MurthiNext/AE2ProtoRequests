package com.murthinext.ae2pr.mixin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.repeat.GenericRepeatOrder;
import com.murthinext.ae2pr.logic.repeat.GenericRepeatOrders;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderBinding;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderNotifier;
import com.murthinext.ae2pr.logic.repeat.ServerRepeatRegistry;

import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.crafting.CraftConfirmMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

// 确认菜单：未命中普通 CPU 时兜底登记通用重复订单
@Mixin(CraftConfirmMenu.class)
public abstract class CraftConfirmMenuMixin {

    @Shadow(remap = false)
    @Nullable
    private ICraftingPlan result;

    @Shadow(remap = false)
    @Nullable
    private ICraftingCPU selectedCpu;

    @Shadow(remap = false)
    protected abstract IGrid getGrid();

    @Shadow(remap = false)
    protected abstract IActionSource getActionSrc();

    @Unique
    @Nullable
    private Set<ICraftingCPU> ae2pr$busyBefore;

    // startJob 为 AE2 自定义方法，不做映射
    @Inject(method = "startJob", at = @At("HEAD"), remap = false)
    private void ae2pr$bindRepeatRounds(CallbackInfo ci) {
        var menu = (CraftConfirmMenu) (Object) this;
        if (!(menu.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var rounds = ServerRepeatRegistry.take(serverPlayer.getUUID());
        if (rounds == null || rounds <= 1) {
            return;
        }
        RepeatOrderBinding.set(rounds);
        try {
            var grid = this.getGrid();
            if (grid != null) {
                this.ae2pr$busyBefore = ae2pr$busyCpus(grid);
            }
        } catch (Throwable t) {
            this.ae2pr$busyBefore = null;
            GenericRepeatOrders.logObserverFailure(t);
        }
    }

    @Inject(method = "startJob", at = @At("RETURN"), remap = false)
    private void ae2pr$clearRepeatRoundsBinding(CallbackInfo ci) {
        var leftover = RepeatOrderBinding.consume();
        var busyBefore = this.ae2pr$busyBefore;
        this.ae2pr$busyBefore = null;
        if (leftover == null || leftover <= 1 || busyBefore == null) {
            return;
        }
        try {
            ae2pr$registerGenericOrder(leftover, busyBefore);
        } catch (Throwable t) {
            GenericRepeatOrders.logObserverFailure(t);
        }
    }

    @Unique
    private void ae2pr$registerGenericOrder(int rounds, Set<ICraftingCPU> busyBefore) {
        var menu = (CraftConfirmMenu) (Object) this;
        if (!(menu.getPlayer() instanceof ServerPlayer serverPlayer) || this.result == null) {
            return;
        }
        var grid = this.getGrid();
        if (grid == null) {
            return;
        }

        ICraftingCPU newlyBusy = null;
        for (var cpu : grid.getCraftingService().getCpus()) {
            if (cpu.isBusy() && !busyBefore.contains(cpu)) {
                newlyBusy = cpu;
                break;
            }
        }
        var target = this.selectedCpu != null ? this.selectedCpu : newlyBusy;
        if (target == null) {
            return;
        }
        var finalOutput = this.result.finalOutput();
        if (newlyBusy == null && !ae2pr$isRunning(target, finalOutput)) {
            // 提交未成功（例如选中的 CPU 正在忙别的工作）
            return;
        }

        var order = new GenericRepeatOrder();
        order.grid = grid;
        order.level = serverPlayer.serverLevel();
        order.source = this.getActionSrc();
        order.ownerPlayerId = IPlayerRegistry.getPlayerId(serverPlayer);
        order.what = finalOutput.what();
        order.amountPerRound = Math.max(1, finalOutput.amount());
        order.pendingFinalAmount = order.amountPerRound;
        order.lastFinalAmount = order.amountPerRound;
        order.totalRounds = rounds;
        // 提交目标保留玩家选择/自动选择；展示目标取实际开始执行的 CPU
        order.cpu = this.selectedCpu;
        order.displayCpu = newlyBusy != null ? newlyBusy : target;
        GenericRepeatOrders.register(order);
        RepeatOrderNotifier.sendRound(order.ownerPlayerId, order.level, order.what, order.totalRounds,
                order.totalRounds);
        order.notifyCooldownTicks = 20;
    }

    @Unique
    private static boolean ae2pr$isRunning(ICraftingCPU cpu, GenericStack finalOutput) {
        if (!cpu.isBusy()) {
            return false;
        }
        var status = cpu.getJobStatus();
        return status != null && status.crafting() != null && status.crafting().what().equals(finalOutput.what());
    }

    @Unique
    private static Set<ICraftingCPU> ae2pr$busyCpus(IGrid grid) {
        Set<ICraftingCPU> busy = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var cpu : grid.getCraftingService().getCpus()) {
            if (cpu.isBusy()) {
                busy.add(cpu);
            }
        }
        return busy;
    }

    // removed(Player) 是原版菜单方法重写，必须保留重映射
    // 离开确认界面时转入恢复队列；GTLCore 会取消 {@code goBack} 主体，不能依赖其 RETURN
    @Inject(method = "removed", at = @At("HEAD"))
    private void ae2pr$pushRestoreRepeatRounds(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerRepeatRegistry.pushRestore(serverPlayer.getUUID());
        }
    }
}
