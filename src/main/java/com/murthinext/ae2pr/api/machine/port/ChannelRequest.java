package com.murthinext.ae2pr.api.machine.port;

import appeng.api.networking.IManagedGridNode;
import com.murthinext.ae2pr.api.machine.ControllerHost;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class ChannelRequest {

    private final BlockEntity owner;
    private final int demand;

    @Nullable
    private final Services services;

    @Nullable
    private Allocation allocation;

    public ChannelRequest(BlockEntity owner, int demand) {
        this(owner, demand, null);
    }

    public ChannelRequest(BlockEntity owner, int demand, @Nullable Services services) {
        if (demand <= 0) {
            throw new IllegalArgumentException("Channel demand must be positive");
        }
        this.owner = owner;
        this.demand = demand;
        this.services = services;
    }

    public BlockEntity owner() {
        return owner;
    }

    public int demand() {
        return owner.isRemoved() ? 0 : demand;
    }

    @Nullable
    public Services services() {
        return services;
    }

    public boolean isBound() {
        return allocation != null;
    }

    public boolean tryBind(Allocation allocation) {
        if (this.allocation != null) {
            return false;
        }
        this.allocation = allocation;
        return true;
    }

    public void unbind(Allocation allocation) {
        if (this.allocation == allocation) {
            this.allocation = null;
        }
    }

    public boolean isOnline() {
        return demand() > 0 && allocation != null && allocation.isOnline();
    }

    public boolean isOnlineFor(ControllerHost host) {
        return allocation != null && allocation.host() == host && isOnline();
    }

    public void refreshServices() {
        if (allocation != null && allocation.serviceNode() != null && services != null) {
            services.refresh(allocation.serviceNode());
        }
    }

    public interface Allocation {

        ControllerHost host();

        boolean isOnline();

        @Nullable
        IManagedGridNode serviceNode();
    }

    public interface Services {

        void register(IManagedGridNode node);

        default void refresh(IManagedGridNode node) {}
    }
}
