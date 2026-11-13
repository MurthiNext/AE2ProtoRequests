package com.murthinext.ae2pr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class FilterPressPlateBlock extends DisassemblableBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public FilterPressPlateBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any()
                                     .setValue(AXIS, Direction.Axis.Z)
                                     .setValue(LEFT, false)
                                     .setValue(RIGHT, false)
                                     .setValue(UP, false)
                                     .setValue(DOWN, false));
    }

    private boolean connectsTo(BlockState state, BlockState neighbor) {
        return neighbor.is(this) && neighbor.getValue(AXIS) == state.getValue(AXIS);
    }

    private BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction left = state.getValue(AXIS) == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        return state.setValue(LEFT, connectsTo(state, level.getBlockState(pos.relative(left))))
                .setValue(RIGHT, connectsTo(state, level.getBlockState(pos.relative(left.getOpposite()))))
                .setValue(UP, connectsTo(state, level.getBlockState(pos.above())))
                .setValue(DOWN, connectsTo(state, level.getBlockState(pos.below())));
    }

    // 按给定方块视图推导连接状态
    public static BlockState connectedShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof FilterPressPlateBlock plate ? plate.updateConnections(state, level, pos)
                                                                       : state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, LEFT, RIGHT, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        return updateConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() == state.getValue(AXIS)) {
            return state;
        }
        Direction left = state.getValue(AXIS) == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        BooleanProperty connection = direction == Direction.UP ? UP
                : direction == Direction.DOWN                  ? DOWN
                : direction == left                            ? LEFT
                                                               : RIGHT;
        return state.setValue(connection, connectsTo(state, neighbor));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction left = state.getValue(AXIS) == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        Direction rotatedLeft = rotation.rotate(left);
        Direction.Axis axis = rotatedLeft.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction newLeft = axis == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        BlockState rotated = state.setValue(AXIS, axis);
        return rotatedLeft == newLeft
                ? rotated
                : rotated.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction left = state.getValue(AXIS) == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        return mirror.mirror(left) == left
                ? state
                : state.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override
    public BlockState rotateWithWrench(BlockState state) {
        return rotate(state, Rotation.CLOCKWISE_90);
    }

    @Override
    public void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
        BlockState connected = updateConnections(state, level, pos);
        if (connected != state) {
            level.setBlock(pos, connected, Block.UPDATE_ALL);
        }
    }
}
