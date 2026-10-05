package com.murthinext.ae2pr.block.naming_factory;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.block.AEBaseEntityBlock;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.murthinext.ae2pr.ModTags;
import com.murthinext.ae2pr.client.naming_factory.NamingFactoryRenameScreen;

/**
 * 名称压印工厂方块：使用 AE2 名称压印模板批量命名物品。
 */
public class NamingFactoryBlock extends AEBaseEntityBlock<NamingFactoryBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public NamingFactoryBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof NamingFactoryBlockEntity factory) {
                factory.serverTick();
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
        if (stack.is(ModTags.KNIVES)) {
            if (player.isShiftKeyDown()) {
                // 潜行时交给切割刀自身的界面（制作名称压印模板）
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                openRenameScreen(level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof NamingFactoryBlockEntity factory) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new NamingFactoryMenu(id, inventory, factory),
                            factory.getDisplayName()),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 客户端：打开重命名界面，并带入机器当前的自定义名称。 */
    private static void openRenameScreen(Level level, BlockPos pos) {
        String currentName = level.getBlockEntity(pos) instanceof NamingFactoryBlockEntity factory
                && factory.getCustomName() != null ? factory.getCustomName().getString() : "";
        NamingFactoryRenameScreen.open(pos, currentName);
    }

    private static InteractionResult rotate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof NamingFactoryBlockEntity factory) {
            for (int slot = 0; slot < NamingFactoryBlockEntity.SLOT_COUNT; slot++) {
                ItemStack stack = factory.getInventory().getItem(slot);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
