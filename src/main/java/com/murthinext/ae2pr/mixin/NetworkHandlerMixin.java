package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.repeat.GenericRepeatOrders;

import appeng.core.sync.BasePacket;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.CraftingJobStatusPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * 观察发往玩家的合成状态包，推进通用（VCPU）重复订单轮次（钩子需完全防御式）。
 */
@Mixin(value = NetworkHandler.class, remap = false)
public abstract class NetworkHandlerMixin {

    @Inject(method = "sendTo", at = @At("HEAD"), remap = false)
    private void ae2pr$observeCraftingJobStatus(BasePacket message, ServerPlayer player, CallbackInfo ci) {
        if (!(message instanceof CraftingJobStatusPacket)) {
            return;
        }
        var pending = GenericRepeatOrders.pollConstruction();
        if (pending == null) {
            return;
        }
        try {
            GenericRepeatOrders.onJobStatus(player, pending.jobId(), pending.what(), pending.requestedAmount(),
                    pending.status());
        } catch (Throwable t) {
            GenericRepeatOrders.logObserverFailure(t);
        }
    }
}
