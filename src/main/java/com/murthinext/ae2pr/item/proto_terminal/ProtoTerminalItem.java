package com.murthinext.ae2pr.item.proto_terminal;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.api.ClientHooks;
import com.murthinext.ae2pr.api.machine.MachineBlock;

public class ProtoTerminalItem extends Item {

    public ProtoTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            ClientHooks.run(() -> TerminalScreen::open);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    // Shift 右键多方块主机时优先补齐结构，需早于主机方块自身的交互
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (player == null || !player.isShiftKeyDown() || !(state.getBlock() instanceof MachineBlock<?> machine)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MultiblockAssembler.assemble(serverPlayer, context.getClickedPos(), machine, context.getHand());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            ClientHooks.run(() -> TerminalScreen::open);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
