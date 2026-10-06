package com.murthinext.ae2pr.block.meteor_steel_pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import com.murthinext.ae2pr.ModTags;

/**
 * 陨钢管道方块：两端为方形开口，四个侧面使用连接纹理，
 * 轴向相连的管道侧面包边在相接处连续，转角/T 字因轴向不同不连接。
 * <p>
 * 可用扳手旋转朝向，放置时开口默认朝向玩家。
 */
public class MeteorSteelPipeBlock extends Block {

    /** 扳手旋转顺序 */
    private static final Direction[] ROTATION_ORDER = { Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST };

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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModTags.WRENCHES)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(FACING, nextFacing(state.getValue(FACING))), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    private static Direction nextFacing(Direction current) {
        for (int i = 0; i < ROTATION_ORDER.length; i++) {
            if (ROTATION_ORDER[i] == current) {
                return ROTATION_ORDER[(i + 1) % ROTATION_ORDER.length];
            }
        }
        return Direction.NORTH;
    }
}
