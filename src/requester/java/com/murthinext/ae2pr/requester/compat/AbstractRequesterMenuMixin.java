/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * ME Requester（LGPL-3.0）兼容层，纳入本仓库 LGPL-3.0 声明范围。
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.almostreliable.merequester.requester.abstraction.AbstractRequesterMenu;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.stacks.GenericStack;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
        if (requestIndex >= 0 && requestIndex < list.size()) {
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
        if (requestIndex >= 0 && requestIndex < list.size()) {
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
        ae2pr$handleAction((AEBaseMenu) (Object) this, be, action, slot);
        ci.cancel();
    }

    @Unique
    private static RedstoneRequesterBlockEntity ae2pr$our(Object self, long id) {
        if (self instanceof IOurRequesterTerminalHost host) {
            return host.ae2pr$getOurRequester(id);
        }
        return null;
    }

    @Unique
    private static void ae2pr$handleAction(AEBaseMenu menu, RedstoneRequesterBlockEntity be,
            InventoryAction action, int slot) {
        var list = be.getRequests();
        if (slot < 0 || slot >= list.size()) {
            return;
        }
        var requestSlot = list.getSlotInv(slot);
        var carried = menu.getCarried();

        switch (action) {
            case PICKUP_OR_SET_DOWN ->
                requestSlot.setItemDirect(0, carried.isEmpty() ? ItemStack.EMPTY : carried.copy());
            case SPLIT_OR_PLACE_SINGLE -> {
                if (carried.isEmpty()) {
                    requestSlot.setItemDirect(0, ItemStack.EMPTY);
                } else {
                    var copy = carried.copy();
                    copy.setCount(1);
                    requestSlot.setItemDirect(0, copy);
                }
            }
            case SHIFT_CLICK -> requestSlot.setItemDirect(0, ItemStack.EMPTY);
            case EMPTY_ITEM -> {
                var emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
                if (emptyingAction != null) {
                    requestSlot.insertItem(0,
                            GenericStack.wrapInItemStack(emptyingAction.what(), emptyingAction.maxAmount()), false);
                }
            }
            case CREATIVE_DUPLICATE -> {
                if (menu.getPlayer().getAbilities().instabuild && carried.isEmpty()) {
                    var current = requestSlot.getStackInSlot(0);
                    if (current.isEmpty()) {
                        menu.setCarried(ItemStack.EMPTY);
                    } else {
                        var stack = current.copy();
                        stack.setCount(stack.getMaxStackSize());
                        menu.setCarried(stack);
                    }
                }
            }
            default -> {
            }
        }
    }
}
