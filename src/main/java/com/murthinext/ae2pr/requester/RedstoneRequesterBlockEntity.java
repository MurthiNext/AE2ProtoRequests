package com.murthinext.ae2pr.requester;

import java.util.EnumSet;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.helpers.MachineSource;
import appeng.util.SettingsFrom;

import com.murthinext.ae2pr.requester.abstraction.RequestHost;
import com.murthinext.ae2pr.requester.crafting.RequestCrafting;
import com.murthinext.ae2pr.requester.platform.RequesterPlatform;

public class RedstoneRequesterBlockEntity extends AENetworkBlockEntity implements RequestHost, IGridTickable {

    private static final String REQUESTS_ID = "requests";
    private static final String REDSTONE_ID = "redstone";

    private final Requests requests;
    private final RequestCrafting crafting = new RequestCrafting(new MachineSource(this));

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

    // 由方块的红石信号变化回调。仅在上升沿（无信号 → 有信号）时触发一次下单
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
        crafting.submit(level, grid, requests);
        wakeDevice();
    }

    private void wakeDevice() {
        var grid = getMainNode().getGrid();
        var node = getMainNode().getNode();
        if (grid != null && node != null) {
            grid.getTickManager().wakeDevice(node);
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide || !getMainNode().isActive()) {
            return TickRateModulation.IDLE;
        }
        crafting.submitCompleted(getMainNode().getGrid());
        return crafting.hasPendingJobs() ? TickRateModulation.URGENT : TickRateModulation.IDLE;
    }

    @Override
    public void requestChanged(int index) {
        saveChanges();
    }

    @Override
    public Requests getRequests() {
        return requests;
    }

    @Override
    public Component getTerminalName() {
        return hasCustomName() ? Objects.requireNonNull(getCustomName())
                               : RequesterUtils.translate("block", RequesterConstants.REQUESTER_ID);
    }

    public long getSortValue() {
        return (long) worldPosition.getZ() << 24 ^ (long) worldPosition.getX() << 8 ^ worldPosition.getY();
    }

    public boolean isActive() {
        return crafting.hasPendingJobs();
    }
}
