package com.murthinext.ae2pr.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.repeat.GenericRepeatOrders;

import appeng.api.stacks.AEKey;
import appeng.core.sync.packets.CraftingJobStatusPacket;

/**
 * 构造时捕获发送用状态包参数（构造器不写实例字段），供通用订单识别轮次。
 */
@Mixin(value = CraftingJobStatusPacket.class, remap = false)
public abstract class CraftingJobStatusPacketMixin {

    @Inject(method = "<init>(Ljava/util/UUID;Lappeng/api/stacks/AEKey;JJLappeng/core/sync/packets/CraftingJobStatusPacket$Status;)V",
            at = @At("TAIL"), remap = false)
    private void ae2pr$captureStatus(UUID jobId, AEKey what, long requestedAmount, long remainingAmount,
            CraftingJobStatusPacket.Status status, CallbackInfo ci) {
        GenericRepeatOrders.observeConstruction(jobId, what, requestedAmount, status);
    }
}
