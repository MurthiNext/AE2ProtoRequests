/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEKey;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.helpers.MachineSource;
import appeng.util.SettingsFrom;

import com.murthinext.ae2pr.requester.abstraction.RequestHost;
import com.murthinext.ae2pr.requester.platform.RequesterPlatform;

/**
 * ME 红石请求器方块实体。
 * <p>
 * 结构参考 ME Requester（https://github.com/AlmostReliable/merequester，LGPL-3.0）的 {@code RequesterBlockEntity}，
 * 红石上升沿一次下单。
 */
public class RedstoneRequesterBlockEntity extends AENetworkBlockEntity
        implements RequestHost, IGridTickable {

    private static final String REQUESTS_ID = "requests";
    private static final String REDSTONE_ID = "redstone";

    private final Requests requests;
    private final IActionSource actionSource = new MachineSource(this);
    /** 供合成模拟使用的来源（携带本机的 action source）。 */
    private final ICraftingSimulationRequester simulationRequester = () -> actionSource;
    /** 在途的合成计划计算（红石触发后异步模拟，完成后提交）。 */
    private final List<PendingCraft> pending = new ArrayList<>();

    private boolean lastRedstone;

    public RedstoneRequesterBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
        requests = new Requests(this);
        getMainNode()
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .addService(IGridTickable.class, this)
                .setIdlePowerUsage(RequesterPlatform.getIdleEnergy());
        if (RequesterPlatform.requireChannel()) {
            getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
        }
    }

    // ------------------------------------------------------------------ 网络列表

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains(REQUESTS_ID)) {
            requests.deserialize(tag.getCompound(REQUESTS_ID));
        }
        this.lastRedstone = tag.getBoolean(REDSTONE_ID);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(REQUESTS_ID, requests.serialize());
        tag.putBoolean(REDSTONE_ID, this.lastRedstone);
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.MEMORY_CARD && input.contains(REQUESTS_ID)) {
            requests.deserialize(input.getCompound(REQUESTS_ID));
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            output.put(REQUESTS_ID, requests.serialize());
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        if (level != null && !level.isClientSide) {
            this.lastRedstone = level.hasNeighborSignal(worldPosition);
        }
    }

    @Override
    public void onOrientationChanged(BlockOrientation orientation) {
        super.onOrientationChanged(orientation);
        getMainNode().setExposedOnSides(EnumSet.allOf(Direction.class));
    }

    // ------------------------------------------------------------------ 红石触发

    /**
     * 由方块的红石信号变化回调。仅在上升沿（无信号 → 有信号）时触发一次下单。
     */
    public void onRedstoneChanged(boolean powered) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (powered && !this.lastRedstone) {
            triggerRequests();
        }
        this.lastRedstone = powered;
    }

    private void triggerRequests() {
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) {
            return;
        }
        var service = grid.getCraftingService();
        for (var i = 0; i < requests.size(); i++) {
            var request = requests.get(i);
            if (!request.isRequesting() || request.getAmount() <= 0) {
                continue;
            }
            var what = request.getKey();
            long amount = request.getAmount();
            try {
                Future<ICraftingPlan> future = service.beginCraftingCalculation(
                        level, simulationRequester, what, amount, CalculationStrategy.CRAFT_LESS);
                pending.add(new PendingCraft(future));
            } catch (Throwable t) {
                com.murthinext.ae2pr.ae2pr.LOGGER.warn("Failed to start crafting calculation for {}x{}", amount, what, t);
            }
        }
        wakeDevice();
    }

    private void wakeDevice() {
        var grid = getMainNode().getGrid();
        var node = getMainNode().getNode();
        if (grid != null && node != null) {
            grid.getTickManager().wakeDevice(node);
        }
    }

    // ------------------------------------------------------------------ tick

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide || !getMainNode().isActive()) {
            return TickRateModulation.IDLE;
        }
        pollPending();
        return pending.isEmpty() ? TickRateModulation.IDLE : TickRateModulation.URGENT;
    }

    /** 轮询在途的合成计划，完成后提交任务（结果直接进入 ME 网络存储）。 */
    private void pollPending() {
        if (pending.isEmpty()) {
            return;
        }
        var grid = getMainNode().getGrid();
        if (grid == null) {
            pending.clear();
            return;
        }
        var service = grid.getCraftingService();
        var iterator = pending.iterator();
        while (iterator.hasNext()) {
            var craft = iterator.next();
            if (!craft.future().isDone()) {
                continue;
            }
            iterator.remove();
            try {
                var plan = craft.future().get();
                if (plan != null && !plan.simulation()) {
                    service.submitJob(plan, null, null, true, actionSource);
                }
            } catch (Throwable t) {
                com.murthinext.ae2pr.ae2pr.LOGGER.warn("Failed to submit redstone requester crafting job", t);
            }
        }
    }

    // ------------------------------------------------------------------ RequestHost

    @Override
    public void requestChanged(int index) {
        saveChanges();
    }

    @Override
    public Requests getRequests() {
        return requests;
    }

    @Override
    public boolean isClientSide() {
        return super.isClientSide();
    }

    @Override
    public Component getTerminalName() {
        return hasCustomName()
                ? Objects.requireNonNull(getCustomName())
                : RequesterUtils.translate("block", RequesterConstants.REQUESTER_ID);
    }

    // ------------------------------------------------------------------ crafting simulation source

    public IActionSource getActionSource() {
        return actionSource;
    }

    public IGrid getMainNodeGrid() {
        var grid = getMainNode().getGrid();
        Objects.requireNonNull(grid, "RedstoneRequesterBlockEntity was not fully initialized - Grid is null");
        return grid;
    }

    public long getSortValue() {
        return (long) worldPosition.getZ() << 24 ^ (long) worldPosition.getX() << 8 ^ worldPosition.getY();
    }

    public boolean isActive() {
        return !pending.isEmpty();
    }

    private record PendingCraft(Future<ICraftingPlan> future) {
    }
}
