package com.murthinext.ae2pr.multiblock;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 多方块控制器方块暴露的通用方块状态属性。
 */
public interface MultiblockControllerBlock {

    DirectionProperty facingProperty();

    BooleanProperty formedProperty();

    BooleanProperty runningProperty();

    BooleanProperty pausedProperty();
}
