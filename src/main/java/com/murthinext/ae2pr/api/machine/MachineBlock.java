package com.murthinext.ae2pr.api.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.wrench.WrenchTags;
import com.murthinext.ae2pr.api.wrench.Wrenchable;

public final class MachineBlock<E extends AbstractMultiblockControllerBlockEntity>
        extends Block implements EntityBlock, Wrenchable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    public static final BooleanProperty PAUSED = BooleanProperty.create("paused");

    private final MultiblockMachineDefinition<E, ?> definition;

    public MachineBlock(MultiblockMachineDefinition<E, ?> definition, Properties properties) {
        super(properties);
        this.definition = definition;
        registerDefaultState(stateDefinition.any()
                                     .setValue(FACING, Direction.NORTH)
                                     .setValue(FORMED, false)
                                     .setValue(RUNNING, false)
                                     .setValue(PAUSED, false));
    }

    public MultiblockMachineDefinition<E, ?> definition() {
        return definition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, RUNNING, PAUSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return definition.createBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type != definition.blockEntityType()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            E controller = definition.controller(entity);
            if (controller == null) {
                return;
            }
            if (tickLevel.isClientSide) {
                definition.clientTick(controller);
            } else {
                controller.serverTick();
            }
        };
    }

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }

    @Override
    public boolean canRotateWithWrench(BlockState state) {
        return !state.getValue(FORMED);
    }

    @Override
    public void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof AbstractMultiblockControllerBlockEntity controller) {
            controller.validateStructure();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2pr." + definition.id() + ".desc"));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (player.getItemInHand(hand).is(WrenchTags.WRENCH)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer &&
            level.getBlockEntity(pos) instanceof AbstractMultiblockControllerBlockEntity controller) {
            NetworkHooks.openScreen(serverPlayer, menuProvider(definition.controller(controller)),
                                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private MenuProvider menuProvider(@Nullable E controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> definition.createMenu(id, inventory, controller), getName());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide &&
            level.getBlockEntity(pos) instanceof AbstractMultiblockControllerBlockEntity controller) {
            controller.validateStructure();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide &&
            level.getBlockEntity(pos) instanceof AbstractMultiblockControllerBlockEntity controller) {
            controller.onControllerRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
