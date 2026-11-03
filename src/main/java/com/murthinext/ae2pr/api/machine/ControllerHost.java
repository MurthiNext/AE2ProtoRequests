package com.murthinext.ae2pr.api.machine;

import net.minecraft.core.BlockPos;

public interface ControllerHost {

    boolean isFormed();

    boolean isControllerRemoved();

    int getRequiredChannels();

    BlockPos getControllerPos();
}
