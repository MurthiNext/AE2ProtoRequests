package com.murthinext.ae2pr.block.machine_part;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

import appeng.block.AEBaseEntityBlock;

import com.murthinext.ae2pr.logic.wrench.Wrenchable;

/**
 * ME 网络接入仓方块。
 */
public class MENetworkAccessHatchBlock extends AEBaseEntityBlock<MENetworkAccessHatchBlockEntity>
        implements Wrenchable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty STYLE = MachinePartBlock.STYLE;

    public MENetworkAccessHatchBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(STYLE, MachinePartBlock.DEFAULT_STYLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, STYLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(STYLE, MachinePartBlock.DEFAULT_STYLE);
    }

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }

    /**
     * 仅未成型时允许旋转。
     */
    @Override
    public boolean canRotateWithWrench(BlockState state) {
        return !state.getValue(FORMED);
    }

    @Override
    public void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof MENetworkAccessHatchBlockEntity hatch) {
            hatch.updateGridConnectableSides();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ae2pr.me_network_access_hatch"));
    }
}
