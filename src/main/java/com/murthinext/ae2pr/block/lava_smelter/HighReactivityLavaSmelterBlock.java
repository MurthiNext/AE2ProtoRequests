package com.murthinext.ae2pr.block.lava_smelter;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.murthinext.ae2pr.ModTags;

/**
 * 高反应性熔岩冶炼炉主机方块。
 */
public class HighReactivityLavaSmelterBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** 是否正在运行（驱动工作态贴图）；框架已就绪，待加工逻辑接入。 */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    /** 暂停态（琥珀色暂停贴图）；框架已就绪，待加工逻辑接入。 */
    public static final BooleanProperty PAUSED = BooleanProperty.create("paused");

    public HighReactivityLavaSmelterBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(3.5F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(RUNNING, false)
                .setValue(PAUSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, RUNNING, PAUSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(RUNNING, false)
                .setValue(PAUSED, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LavaSmelterControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof LavaSmelterControllerBlockEntity controller) {
                controller.serverTick();
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModTags.WRENCHES)) {
            return rotate(level, pos, state, player);
        }
        // 非扳手：打开主机界面（含玩家背包，ESC/E 关闭）
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof LavaSmelterControllerBlockEntity controller) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new LavaSmelterMenu(id, inventory, controller),
                            Component.translatable("block.ae2pr.high_reactivity_lava_smelter")),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 扳手旋转：仅未成型时允许，避免误操作破坏已建成的机器。 */
    private static InteractionResult rotate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        if (level.getBlockEntity(pos) instanceof LavaSmelterControllerBlockEntity controller) {
            controller.validateStructure();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LavaSmelterControllerBlockEntity controller) {
            controller.validateStructure();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof LavaSmelterControllerBlockEntity controller) {
            controller.onControllerRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
