package com.murthinext.ae2pr.block.machine_part;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;

import appeng.block.AEBaseEntityBlock;

import com.murthinext.ae2pr.logic.wrench.Wrenchable;

/**
 * 福鲁伊克斯水晶能源仓方块：从 ME 网络直接为多方块机器供电。
 */
public class FluixCrystalEnergyHatchBlock extends AEBaseEntityBlock<FluixCrystalEnergyHatchBlockEntity>
        implements Wrenchable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public FluixCrystalEnergyHatchBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(FORMED, false);
    }

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }

    /** 仅未成型时允许旋转，避免误操作破坏已建成的机器。 */
    @Override
    public boolean canRotateWithWrench(BlockState state) {
        return !state.getValue(FORMED);
    }

    /** 朝向变化后刷新 ME 连接面。 */
    @Override
    public void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof FluixCrystalEnergyHatchBlockEntity hatch) {
            hatch.updateGridConnectableSides();
        }
    }
}
