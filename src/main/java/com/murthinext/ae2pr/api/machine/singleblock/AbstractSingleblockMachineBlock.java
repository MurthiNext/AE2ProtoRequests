package com.murthinext.ae2pr.api.machine.singleblock;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.murthinext.ae2pr.api.wrench.Wrenchable;
import com.murthinext.ae2pr.api.wrench.WrenchTags;

public abstract class AbstractSingleblockMachineBlock<E extends AbstractSingleblockMachineBlockEntity>
        extends AEBaseEntityBlock<E> implements Wrenchable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final SingleblockMachineDefinition<?, E, ?> definition;

    protected AbstractSingleblockMachineBlock(SingleblockMachineDefinition<?, E, ?> definition) {
        super(definition.properties());
        this.definition = definition;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public SingleblockMachineDefinition<?, E, ?> definition() {
        return definition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
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

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AbstractSingleblockMachineBlockEntity machine) {
                machine.serverTick();
            }
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return definition.createBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        // 扳手交互交给 WrenchInteractions
        if (player.getItemInHand(hand).is(WrenchTags.WRENCH)) {
            return InteractionResult.PASS;
        }
        InteractionResult custom = useWithItem(level, pos, player, hand);
        if (custom != null) {
            return custom;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AbstractSingleblockMachineBlockEntity machine) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, p) -> definition.createMenu(id, inventory,
                            definition.controller(machine)), machine.getDisplayName()),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // 返回 null 表示走默认的机器界面；返回 PASS 表示交给手持物品处理
    @Nullable
    protected InteractionResult useWithItem(Level level, BlockPos pos, Player player, InteractionHand hand) {
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ae2pr." + definition.id() + ".desc"));
    }
}
