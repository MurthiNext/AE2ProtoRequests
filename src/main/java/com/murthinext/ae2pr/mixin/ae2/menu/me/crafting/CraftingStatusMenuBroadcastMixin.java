package com.murthinext.ae2pr.mixin.ae2.menu.me.crafting;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.me.crafting.CraftingStatusMenu;

import com.murthinext.ae2pr.logic.repeat.GeneralRepeatOrders;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderHost;
import com.murthinext.ae2pr.mixin.ae2.accessor.CraftingStatusMenuAccessor;
import com.murthinext.ae2pr.network.CpuRoundInfo;
import com.murthinext.ae2pr.network.RepeatOrderStatusPacket;
import com.murthinext.ae2pr.registry.ModNetwork;

// 合成状态菜单：列表刷新时把重复订单轮次同步给查看该菜单的玩家
@Mixin(CraftingStatusMenu.class)
public abstract class CraftingStatusMenuBroadcastMixin {

    @Unique
    private List<CpuRoundInfo> ae2pr$lastSentRounds;

    // broadcastChanges 是原版菜单方法重写，必须保留重映射
    @Inject(method = "broadcastChanges", at = @At("RETURN"))
    private void ae2pr$sendRepeatRounds(CallbackInfo ci) {
        var menu = (CraftingStatusMenu) (Object) this;
        if (!(menu.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var accessor = (CraftingStatusMenuAccessor) this;
        List<CpuRoundInfo> rounds = new ArrayList<>();
        for (var cpu : accessor.ae2pr$getLastCpuSet()) {
            Integer round = null;
            Integer total = null;
            if (cpu instanceof CraftingCPUCluster cluster) {
                var context = ((RepeatOrderHost) cluster.craftingLogic).ae2pr$getContext();
                if (context != null) {
                    round = context.currentRound();
                    total = context.totalRounds;
                }
            } else {
                // 通用（VCPU）订单按 CPU 实例查询轮次
                var info = GeneralRepeatOrders.getRoundInfo(cpu);
                if (info != null) {
                    round = info.round();
                    total = info.total();
                }
            }
            if (total != null) {
                rounds.add(new CpuRoundInfo(accessor.ae2pr$getOrAssignCpuSerial(cpu), round, total));
            }
        }

        if (rounds.equals(this.ae2pr$lastSentRounds)) {
            return;
        }
        this.ae2pr$lastSentRounds = rounds;
        ModNetwork.sendToPlayer(serverPlayer, new RepeatOrderStatusPacket(menu.containerId, rounds));
    }
}
