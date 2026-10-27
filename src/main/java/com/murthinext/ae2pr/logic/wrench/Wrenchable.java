package com.murthinext.ae2pr.logic.wrench;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

// 可使用扳手交互的机械方块
public interface Wrenchable {

    // 扳手旋转使用的朝向属性；无朝向返回 null
    @Nullable
    default DirectionProperty wrenchFacing() {
        return null;
    }

    default boolean canRotateWithWrench(BlockState state) {
        return true;
    }

    default boolean canDisassembleWithWrench(BlockState state) {
        return true;
    }

    // 扳手旋转成功后的服务端回调
    default void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
    }
}
