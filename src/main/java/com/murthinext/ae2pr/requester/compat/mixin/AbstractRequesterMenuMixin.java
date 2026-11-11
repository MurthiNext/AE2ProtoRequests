package com.murthinext.ae2pr.requester.compat.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.almostreliable.merequester.requester.abstraction.AbstractRequesterMenu;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.abstraction.RequesterMenuActions;
import com.murthinext.ae2pr.requester.compat.IOurRequesterTerminalHost;

import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import net.minecraft.server.level.ServerPlayer;

/**
 * 让 ME Requester 终端的服务端菜单能够处理本模组红石请求器的条目修改请求
 * （标记物品、启用状态、数量），其余 id 交回 ME Requester 原生逻辑处理。
 */
@Mixin(value = AbstractRequesterMenu.class, remap = false)
public class AbstractRequesterMenuMixin {

    @Inject(method = "updateRequesterNumbers", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2pr$updateNumbers(long requesterId, int requestIndex, long amount, long ignoredBatch, CallbackInfo ci) {
        var be = ae2pr$our(this, requesterId);
        if (be == null) {
            return;
        }
        var list = be.getRequests();
        if (list.isValidIndex(requestIndex)) {
            // 本模组的请求器不再支持重复下单，ME Requester 终端的 batch 字段在此忽略
            list.get(requestIndex).updateAmount(amount);
        }
        ci.cancel();
    }

    @Inject(method = "updateRequesterState", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2pr$updateState(long requesterId, int requestIndex, boolean state, CallbackInfo ci) {
        var be = ae2pr$our(this, requesterId);
        if (be == null) {
            return;
        }
        var list = be.getRequests();
        if (list.isValidIndex(requestIndex)) {
            list.get(requestIndex).updateState(state);
        }
        ci.cancel();
    }

    @Inject(method = "doAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2pr$doAction(ServerPlayer player, InventoryAction action, int slot, long id,
            CallbackInfo ci) {
        var be = ae2pr$our(this, id);
        if (be == null) {
            return;
        }
        RequesterMenuActions.apply((AEBaseMenu) (Object) this, be.getRequests(), action, slot);
        ci.cancel();
    }

    @Nullable
    @Unique
    private static RedstoneRequesterBlockEntity ae2pr$our(Object self, long id) {
        if (self instanceof IOurRequesterTerminalHost host) {
            return host.ae2pr$getOurRequester(id);
        }
        return null;
    }
}
