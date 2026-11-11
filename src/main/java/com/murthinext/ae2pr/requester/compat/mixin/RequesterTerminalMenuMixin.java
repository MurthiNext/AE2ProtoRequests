package com.murthinext.ae2pr.requester.compat.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.almostreliable.merequester.platform.Platform;
import com.almostreliable.merequester.requester.Requests;
import com.almostreliable.merequester.requester.abstraction.AbstractRequesterMenu;
import com.almostreliable.merequester.terminal.RequesterTerminalMenu;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.compat.IOurRequesterTerminalHost;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;

/**
 * 让本模组的 ME 红石请求器也出现在 ME Requester 的请求器终端中：
 * 在终端每次刷新时，扫描网络中的 {@link RedstoneRequesterBlockEntity}，
 * 并以 ME Requester 自身的数据格式把它们的条目同步给客户端终端界面。
 * <p>
 * 客户端无需 mixin：ME Requester 的终端界面本身就是通用的（按 requesterId + 名称 + NBT 展示）。
 * <p>
 * 注入点选在 ME Requester 自己声明的 {@code sendFullUpdate} / {@code sendPartialUpdate} 上，
 * 而非它们所重写的原版 {@code broadcastChanges}，这样在混淆环境下无需额外的映射。
 */
@Mixin(value = RequesterTerminalMenu.class, remap = false)
public abstract class RequesterTerminalMenuMixin implements IOurRequesterTerminalHost {

    @Shadow(remap = false)
    protected abstract void sendFullUpdate(IGrid grid);

    @Unique
    private Map<Long, RedstoneRequesterBlockEntity> ae2pr$ourById;

    @Unique
    private Map<Long, CompoundTag> ae2pr$lastSent;

    // 上一次同步给客户端的“id → 名称”总览：变化时说明增删或重命名，客户端旧数据无法增量修正
    @Unique
    private Map<Long, String> ae2pr$lastOverview;

    /**
     * 本 mixin 有多个字段初始化器时，Mixin 只会把其中一个注入目标构造器，其余被静默丢弃，
     * 首次同步便会读到 null 映射。因此改在用到之前显式初始化。
     */
    @Unique
    private void ae2pr$ensureState() {
        if (ae2pr$ourById != null) {
            return;
        }
        ae2pr$ourById = new HashMap<>();
        ae2pr$lastSent = new HashMap<>();
        ae2pr$lastOverview = Map.of();
    }

    @Override
    @Nullable
    public RedstoneRequesterBlockEntity ae2pr$getOurRequester(long id) {
        return ae2pr$ourById == null ? null : ae2pr$ourById.get(id);
    }

    // 整体刷新已经清空并重建了客户端数据，本模组的缓存随之失效，重新全量发送
    @Inject(method = "sendFullUpdate", at = @At("TAIL"), remap = false)
    private void ae2pr$syncAfterFullUpdate(IGrid grid, CallbackInfo ci) {
        if (((RequesterTerminalMenu) (Object) this).isClientSide()) {
            return;
        }
        ae2pr$ensureState();
        ae2pr$lastSent.clear();
        ae2pr$sendAll(grid);
    }

    @Inject(method = "sendPartialUpdate", at = @At("TAIL"), remap = false)
    private void ae2pr$syncAfterPartialUpdate(CallbackInfo ci) {
        var menu = (RequesterTerminalMenu) (Object) this;
        if (menu.isClientSide()) {
            return;
        }
        ae2pr$ensureState();
        var grid = ae2pr$getGrid(menu);
        if (grid == null) {
            return;
        }

        // 请求器增删或改名时，客户端旧数据无法按 id 增量修正，退化为一次整体刷新
        if (!ae2pr$overview(grid).equals(ae2pr$lastOverview)) {
            sendFullUpdate(grid);
            return;
        }
        ae2pr$sendAll(grid);
    }

    @Unique
    private void ae2pr$sendAll(IGrid grid) {
        var menu = (RequesterTerminalMenu) (Object) this;
        if (menu.isClientSide()) {
            return;
        }
        ae2pr$ensureState();

        var machines = grid.getActiveMachines(RedstoneRequesterBlockEntity.class);
        ae2pr$lastOverview = ae2pr$overview(grid);
        ae2pr$ourById.clear();
        for (var be : machines) {
            var id = ae2pr$idFor(be);
            ae2pr$ourById.put(id, be);
            ae2pr$send(menu, be, id);
        }
    }

    @Unique
    private static Map<Long, String> ae2pr$overview(IGrid grid) {
        var overview = new TreeMap<Long, String>();
        for (var be : grid.getActiveMachines(RedstoneRequesterBlockEntity.class)) {
            overview.put(ae2pr$idFor(be), be.getTerminalName().getString());
        }
        return overview;
    }

    // 取方块打包坐标的绝对值位：ME Requester 自身的 id 从 Long.MIN_VALUE 递增，均为负数，不会冲突
    @Unique
    private static long ae2pr$idFor(RedstoneRequesterBlockEntity be) {
        return be.getBlockPos().asLong() & Long.MAX_VALUE;
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
        var mirror = new Requests(null);
        var count = Math.min(mirror.size(), be.getRequests().size());
        for (var i = 0; i < count; i++) {
            var entry = be.getRequests().get(i);
            if (entry.getKey() == null) {
                continue;
            }
            mirror.setStack(i, new GenericStack(entry.getKey(), Math.max(1, entry.getAmount())));
            mirror.get(i).updateState(entry.getState());
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
