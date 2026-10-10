package com.murthinext.ae2pr.block.meteor_steel_pipe;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.logic.wrench.Wrenchable;

/**
 * 陨钢管道方块：两端为方形开口，四个侧面使用连接纹理，
 * 轴向相连的管道侧面包边在相接处连续，转角/T 字因轴向不同不连接。
 */
public class MeteorSteelPipeBlock extends Block implements Wrenchable {

    /** 管道朝向（两端开口所在轴） */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MeteorSteelPipeBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }
}
