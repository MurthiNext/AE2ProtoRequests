package com.murthinext.ae2pr.api.machine;

import com.murthinext.ae2pr.api.machine.part.ChannelRequest;
import java.util.List;
import net.minecraft.core.BlockPos;

public interface ControllerHost {

    boolean isFormed();

    boolean isControllerRemoved();

    ChannelRequest channelRequest();

    List<ChannelRequest> partChannelRequests();

    BlockPos getControllerPos();
}
