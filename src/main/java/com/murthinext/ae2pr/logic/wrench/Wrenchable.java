package com.murthinext.ae2pr.logic.wrench;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 可使用扳手交互的机械方块：有朝向的支持右键旋转，默认支持 Shift+右键拆除。
 * <p>
 * 交互统一由 {@link WrenchInteractions} 处理。
 */
public interface Wrenchable {

    /** 扳手旋转使用的朝向属性；无朝向的方块返回 null（仅支持拆除）。 */
    @Nullable
    default DirectionProperty wrenchFacing() {
        return null;
    }

    /** 当前状态是否允许扳手旋转（例如已成型的主机返回 false）。 */
    default boolean canRotateWithWrench(BlockState state) {
        return true;
    }

    /** 当前状态是否允许 Shift+右键拆除。 */
    default boolean canDisassembleWithWrench(BlockState state) {
        return true;
    }

    /** 扳手旋转成功后的服务端回调：刷新结构校验、ME 连接面等。 */
    default void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
    }
}
