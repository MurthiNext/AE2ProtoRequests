/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * ME Requester（LGPL-3.0）兼容层，纳入本仓库 LGPL-3.0 声明范围。
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.compat;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.almostreliable.merequester.platform.Platform;
import com.almostreliable.merequester.requester.Requests;
import com.almostreliable.merequester.requester.abstraction.AbstractRequesterMenu;
import com.almostreliable.merequester.requester.abstraction.RequestHost;
import com.almostreliable.merequester.terminal.RequesterTerminalMenu;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;

/**
 * 让本模组的 ME 红石请求器也出现在 ME Requester 的请求器终端中：
 * 在终端每次广播变更时，扫描网络中的 {@link RedstoneRequesterBlockEntity}，
 * 并以 ME Requester 自身的数据格式把它们的条目同步给客户端终端界面。
 * <p>
 * 客户端无需 mixin：ME Requester 的终端界面本身就是通用的（按 requesterId + 名称 + NBT 展示）。
 */
@Mixin(value = RequesterTerminalMenu.class, remap = false)
public abstract class RequesterTerminalMenuMixin implements IOurRequesterTerminalHost {

    @Unique
    private final Map<Long, RedstoneRequesterBlockEntity> ae2pr$ourById = new HashMap<>();

    @Unique
    private final Map<Long, CompoundTag> ae2pr$lastSent = new HashMap<>();

    @Override
    public RedstoneRequesterBlockEntity ae2pr$getOurRequester(long id) {
        return ae2pr$ourById.get(id);
    }

    @Inject(method = "broadcastChanges", at = @At("TAIL"), remap = false)
    private void ae2pr$syncOurRequesters(CallbackInfo ci) {
        var self = (RequesterTerminalMenu) (Object) this;
        if (self.isClientSide()) {
            return;
        }

        var grid = ae2pr$getGrid(self);
        if (grid == null) {
            return;
        }

        ae2pr$ourById.clear();
        for (var be : grid.getActiveMachines(RedstoneRequesterBlockEntity.class)) {
            long id = ae2pr$idFor(be);
            ae2pr$ourById.put(id, be);
            ae2pr$send(self, be, id);
        }
    }

    /** 由方块位置导出的稳定正数 id（ME Requester 自身的 id 从 Long.MIN_VALUE 递增，均为负数，不会冲突）。 */
    @Unique
    private static long ae2pr$idFor(RedstoneRequesterBlockEntity be) {
        long id = be.getBlockPos().asLong() & 0x7FFFFFFFFFFFFFFFL;
        return id == 0 ? 1 : id;
    }

    @Unique
    private static IGrid ae2pr$getGrid(RequesterTerminalMenu menu) {
        if (menu.getTarget() instanceof IActionHost host) {
            var node = host.getActionableNode();
            if (node != null && node.isActive()) {
                return node.getGrid();
            }
        }
        return null;
    }

    @Unique
    private void ae2pr$send(RequesterTerminalMenu menu, RedstoneRequesterBlockEntity be, long id) {
        // 用 ME Requester 的 Requests 结构把本机条目转成其 NBT 格式
        var mirror = new Requests((RequestHost) null);
        int count = Math.min(mirror.size(), be.getRequests().size());
        for (int i = 0; i < count; i++) {
            var entry = be.getRequests().get(i);
            if (entry.getKey() != null) {
                mirror.setStack(i, new GenericStack(entry.getKey(), Math.max(1, entry.getAmount())));
                mirror.get(i).updateState(entry.getState());
            }
        }

        var tag = mirror.serialize();
        tag.putString(AbstractRequesterMenu.UNIQUE_NAME_ID, be.getTerminalName().getString());
        tag.putLong(AbstractRequesterMenu.SORT_BY_ID, be.getSortValue());

        if (tag.equals(ae2pr$lastSent.get(id))) {
            return;
        }
        ae2pr$lastSent.put(id, tag);
        Platform.sendInventoryData(menu.getPlayer(), id, tag);
    }
}
