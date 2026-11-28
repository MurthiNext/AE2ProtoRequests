package com.murthinext.ae2pr.block.machine_part;

import static com.murthinext.ae2pr.block.machine_part.MachinePartBlock.DEFAULT_STYLE;
import static com.murthinext.ae2pr.block.machine_part.MachinePartBlock.FACING;
import static com.murthinext.ae2pr.block.machine_part.MachinePartBlock.FORMED;
import static com.murthinext.ae2pr.block.machine_part.MachinePartBlock.STYLE;

import com.murthinext.ae2pr.api.BlockProperties;
import com.murthinext.ae2pr.api.wrench.WrenchTags;
import com.murthinext.ae2pr.api.wrench.Wrenchable;
import com.murthinext.ae2pr.block.StabilityCasingBlock;
import java.util.List;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class MEPatternBufferBlock extends Block implements EntityBlock, Wrenchable {

    public MEPatternBufferBlock() {
        super(BlockProperties.metal(MapColor.METAL).lightLevel(StabilityCasingBlock::fieldLight));
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(StabilityCasingBlock.FIELD_LIT, false)
                .setValue(STYLE, DEFAULT_STYLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, STYLE, StabilityCasingBlock.FIELD_LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MEPatternBufferBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(WrenchTags.WRENCH)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MEPatternBufferBlockEntity buffer) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (id, inventory, owner) -> new MEPatternBufferMenu(id, inventory, pos, buffer), getName()),
                    pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MEPatternBufferBlockEntity buffer) {
            buffer.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2pr.me_pattern_buffer"));
    }
}
