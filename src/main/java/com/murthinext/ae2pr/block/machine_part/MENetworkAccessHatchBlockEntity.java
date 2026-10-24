package com.murthinext.ae2pr.block.machine_part;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.grid.AENetworkBlockEntity;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlockEntity;

/**
 * 多方块的 ME 网络入口，按需申请频道并直接抽取 AE 能量。
 */
public class MENetworkAccessHatchBlockEntity extends AENetworkBlockEntity {

    public static final int MAX_CHANNELS = 8;

    private static final IGridNodeListener<MENetworkAccessHatchBlockEntity> CHANNEL_LISTENER =
            (hatch, node) -> hatch.setChanged();

    private final List<IManagedGridNode> channelNodes = new ArrayList<>();
    @Nullable
    private AbstractMultiblockControllerBlockEntity controller;

    public MENetworkAccessHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_NETWORK_ACCESS_HATCH.get(), pos, state);
        getMainNode().setIdlePowerUsage(0);
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(getBlockState().getValue(MENetworkAccessHatchBlock.FACING));
    }

    public void updateGridConnectableSides() {
        onGridConnectableSidesChanged();
    }

    public void updateController(AbstractMultiblockControllerBlockEntity controller) {
        if (this.controller != null && this.controller != controller && !this.controller.isRemoved()
                && this.controller.isFormed()) {
            return;
        }
        this.controller = controller;
        updateChannelNodes();
    }

    public void detachController(AbstractMultiblockControllerBlockEntity controller) {
        if (this.controller == controller) {
            this.controller = null;
            clearChannelNodes();
        }
    }

    private void updateChannelNodes() {
        IGridNode mainNode = getMainNode().getNode();
        if (mainNode == null || level == null || level.isClientSide) {
            return;
        }
        int requested = Math.min(MAX_CHANNELS, getRequiredChannels());
        while (channelNodes.size() > requested) {
            channelNodes.remove(channelNodes.size() - 1).destroy();
        }
        // 每个内部节点向 AE2 实际申请一个频道。
        while (channelNodes.size() < requested) {
            IManagedGridNode node = GridHelper.createManagedNode(this, CHANNEL_LISTENER)
                    .setInWorldNode(false)
                    .setFlags(GridFlags.REQUIRE_CHANNEL)
                    .setIdlePowerUsage(0);
            node.setOwningPlayerId(mainNode.getOwningPlayerId());
            channelNodes.add(node);
            node.create(level, worldPosition);
            GridHelper.createConnection(mainNode, node.getNode());
        }
    }

    public int getRequiredChannels() {
        return controller != null && !controller.isRemoved() && controller.isFormed()
                ? controller.getRequiredChannels() : 0;
    }

    public int getAllocatedChannels() {
        IGridNode mainNode = getMainNode().getNode();
        if (mainNode == null || mainNode.getInWorldConnections().isEmpty() || !mainNode.hasGridBooted()) {
            return 0;
        }
        int allocated = 0;
        for (IManagedGridNode node : channelNodes) {
            if (node.getNode() != null && node.getNode().meetsChannelRequirements()) {
                allocated++;
            }
        }
        return allocated;
    }

    public boolean isNetworkConnected(AbstractMultiblockControllerBlockEntity controller) {
        int required = getRequiredChannels();
        return this.controller == controller && getMainNode().isActive()
                && required > 0 && required <= MAX_CHANNELS
                && getAllocatedChannels() >= required;
    }

    public double extractAEPower(double amount, boolean simulate) {
        if (amount <= 0 || controller == null || !isNetworkConnected(controller)) {
            return 0;
        }
        return getMainNode().getGrid().getEnergyService().extractAEPower(amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, PowerMultiplier.CONFIG);
    }

    public double getAvailableAEPower() {
        return controller != null && isNetworkConnected(controller)
                ? getMainNode().getGrid().getEnergyService().getStoredPower() : 0;
    }

    @Override
    public void onReady() {
        super.onReady();
        updateChannelNodes();
    }

    private void clearChannelNodes() {
        for (IManagedGridNode node : channelNodes) {
            node.destroy();
        }
        channelNodes.clear();
    }

    @Override
    public void onChunkUnloaded() {
        controller = null;
        clearChannelNodes();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        controller = null;
        clearChannelNodes();
        super.setRemoved();
    }
}
