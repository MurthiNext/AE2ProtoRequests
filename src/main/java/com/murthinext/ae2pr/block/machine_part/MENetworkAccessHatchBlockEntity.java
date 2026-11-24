package com.murthinext.ae2pr.block.machine_part;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEKey;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.helpers.MachineSource;
import com.murthinext.ae2pr.api.EnergyAmount;
import com.murthinext.ae2pr.api.machine.ControllerHost;
import com.murthinext.ae2pr.api.machine.part.ChannelRequest;
import com.murthinext.ae2pr.api.machine.part.ChannelStatus;
import com.murthinext.ae2pr.api.machine.part.MachinePart;
import com.murthinext.ae2pr.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MENetworkAccessHatchBlockEntity extends AENetworkBlockEntity
        implements MachinePart, MachinePart.NetworkPart {

    private static final int MAX_CHANNELS = 8;

    private static final IGridNodeListener<BlockEntity> CHANNEL_LISTENER = (owner, node) -> owner.setChanged();

    private final List<IManagedGridNode> channelNodes = new ArrayList<>();
    private final Map<ChannelRequest, Claim> claims = new LinkedHashMap<>();
    private final MachineSource actionSource =
            new MachineSource(() -> getMainNode().getNode());
    private int requiredChannels;

    @Nullable
    private ControllerHost controller;

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

    @Override
    public void updateController(ControllerHost controller) {
        if (this.controller != null
                && this.controller != controller
                && !this.controller.isControllerRemoved()
                && this.controller.isFormed()) {
            return;
        }
        if (this.controller != controller) {
            clearChannelNodes();
            this.controller = controller;
        }
        updateChannels();
    }

    @Override
    public void detachController(ControllerHost controller) {
        if (this.controller == controller) {
            clearChannelNodes();
            this.controller = null;
        }
    }

    private void updateChannels() {
        var host = controller;
        if (host == null || !isServing(host) || level == null || level.isClientSide) {
            return;
        }
        var requests = new LinkedHashSet<ChannelRequest>();
        requests.add(host.channelRequest());
        requests.addAll(host.partChannelRequests());
        IGridNode mainNode = getMainNode().getNode();
        var retained = new ArrayList<ChannelRequest>(MAX_CHANNELS);
        long required = 0;
        // 主机和仓室都以累计需求取得同一种配额，主机排在首位。
        for (ChannelRequest request : requests) {
            int demand = request.demand();
            Claim claim = claims.get(request);
            if (demand == 0 || (claim == null && request.isBound())) {
                continue;
            }
            required += demand;
            if (mainNode == null || required > MAX_CHANNELS) {
                continue;
            }
            if (claim == null) {
                claim = new Claim(host, request);
                if (!request.tryBind(claim)) {
                    continue;
                }
                claims.put(request, claim);
            }
            claim.requiredThrough = (int) required;
            retained.add(request);
        }
        requiredChannels = (int) Math.min(Integer.MAX_VALUE, required);
        claims.entrySet().removeIf(entry -> {
            if (retained.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().close();
            return true;
        });
        if (mainNode != null) {
            resizePool(mainNode, Math.min(MAX_CHANNELS, requiredChannels));
            for (Claim claim : claims.values()) {
                claim.updateServices(mainNode);
            }
        }
    }

    private IManagedGridNode createInternalNode(BlockEntity owner, IGridNode mainNode) {
        var node = GridHelper.createManagedNode(owner, CHANNEL_LISTENER)
                .setInWorldNode(false)
                .setIdlePowerUsage(0);
        node.setOwningPlayerId(mainNode.getOwningPlayerId());
        return node;
    }

    // 只有接入仓的频道池向 AE2 申请频道。服务节点不另外占频道。
    private void resizePool(IGridNode mainNode, int requested) {
        while (channelNodes.size() > requested) {
            channelNodes.remove(channelNodes.size() - 1).destroy();
        }
        while (channelNodes.size() < requested) {
            IManagedGridNode node = createInternalNode(this, mainNode).setFlags(GridFlags.REQUIRE_CHANNEL);
            channelNodes.add(node);
            node.create(level, worldPosition);
            GridHelper.createConnection(mainNode, node.getNode());
        }
    }

    private boolean isServing(ControllerHost host) {
        return controller == host && !isRemoved() && !host.isControllerRemoved() && host.isFormed();
    }

    private int allocatedChannels() {
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

    @Override
    public ChannelStatus getChannelStatus(ControllerHost host) {
        return isServing(host)
                ? new ChannelStatus(requiredChannels, allocatedChannels(), MAX_CHANNELS)
                : ChannelStatus.NONE;
    }

    @Override
    public boolean isNetworkConnected(ControllerHost host) {
        return isServing(host) && host.channelRequest().isOnlineFor(host);
    }

    @Override
    public double extractAEPower(double amount, boolean simulate) {
        if (controller == null || !isNetworkConnected(controller)) {
            return 0;
        }
        double clamped = EnergyAmount.clamp(amount);
        if (clamped <= 0) {
            return 0;
        }
        return getMainNode()
                .getGrid()
                .getEnergyService()
                .extractAEPower(clamped, simulate ? Actionable.SIMULATE : Actionable.MODULATE, PowerMultiplier.CONFIG);
    }

    @Override
    public double getAvailableAEPower() {
        return controller != null && isNetworkConnected(controller)
                ? getMainNode().getGrid().getEnergyService().getStoredPower()
                : 0;
    }

    @Override
    public long insertProduct(AEKey key, long amount) {
        return controller != null && isNetworkConnected(controller)
                ? getMainNode()
                        .getGrid()
                        .getStorageService()
                        .getInventory()
                        .insert(key, amount, Actionable.MODULATE, actionSource)
                : 0;
    }

    @Override
    public boolean isHatchRemoved() {
        return isRemoved();
    }

    @Override
    public BlockPos getPartPos() {
        return worldPosition;
    }

    @Nullable
    @Override
    public ItemPart items() {
        return null;
    }

    @Nullable
    @Override
    public FluidPart fluids() {
        return null;
    }

    @Override
    public NetworkPart network() {
        return this;
    }

    @Override
    public boolean isRecipeIsolated() {
        return false;
    }

    @Override
    public void onReady() {
        super.onReady();
        updateChannels();
    }

    private void clearChannelNodes() {
        claims.values().forEach(Claim::close);
        claims.clear();
        requiredChannels = 0;
        for (IManagedGridNode node : channelNodes) {
            node.destroy();
        }
        channelNodes.clear();
    }

    @Override
    public void onChunkUnloaded() {
        clearChannelNodes();
        controller = null;
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        clearChannelNodes();
        controller = null;
        super.setRemoved();
    }

    private final class Claim implements ChannelRequest.Allocation {

        private final ControllerHost host;
        private final ChannelRequest request;

        @Nullable
        private IManagedGridNode serviceNode;

        private int requiredThrough;
        private boolean advertised;

        private Claim(ControllerHost host, ChannelRequest request) {
            this.host = host;
            this.request = request;
        }

        @Override
        public ControllerHost host() {
            return host;
        }

        @Override
        public boolean isOnline() {
            return requiredThrough > 0
                    && isServing(host)
                    && getMainNode().isActive()
                    && allocatedChannels() >= requiredThrough
                    && (request.services() == null
                            || (serviceNode != null
                                    && serviceNode.isActive()
                                    && serviceNode.getGrid() == getMainNode().getGrid()));
        }

        @Nullable
        @Override
        public IManagedGridNode serviceNode() {
            return serviceNode;
        }

        private void updateServices(IGridNode mainNode) {
            var services = request.services();
            if (services == null) {
                return;
            }
            if (serviceNode == null) {
                serviceNode = createInternalNode(request.owner(), mainNode);
                services.register(serviceNode);
                serviceNode.create(level, worldPosition);
                GridHelper.createConnection(mainNode, serviceNode.getNode());
            }
            boolean online = request.isOnline();
            if (online != advertised) {
                advertised = online;
                services.refresh(serviceNode);
            }
        }

        private void close() {
            request.unbind(this);
            if (serviceNode != null) {
                serviceNode.destroy();
                serviceNode = null;
            }
        }
    }
}
