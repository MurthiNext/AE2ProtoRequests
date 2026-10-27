package com.murthinext.ae2pr.multiblock;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public interface MultiblockControllerBlock {

    DirectionProperty facingProperty();

    BooleanProperty formedProperty();

    BooleanProperty runningProperty();

    BooleanProperty pausedProperty();
}
