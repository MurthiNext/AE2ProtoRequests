package com.murthinext.ae2pr.logic.wrench;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.blockentity.AEBaseBlockEntity;

import com.murthinext.ae2pr.registry.ModTags;

// 扳手交互统一入口
public final class WrenchInteractions {

    private WrenchInteractions() {
    }

    // 扳手右键路由
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() == Event.Result.DENY
                || event.getEntity().isSpectator() || !event.getItemStack().is(ModTags.WRENCH)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof Wrenchable wrenchable)) {
            return;
        }
        if (event.getEntity().isSecondaryUseActive()) {
            if (!wrenchable.canDisassembleWithWrench(state)) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(disassemble(event.getEntity(), event.getLevel(), event.getPos(),
                    event.getHitVec(), event.getItemStack()));
            return;
        }
        BlockState rotated = wrenchable.rotateWithWrench(state);
        if (rotated == state) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(wrenchable.canRotateWithWrench(state)
                ? rotate(event.getLevel(), event.getPos(), rotated, wrenchable)
                : InteractionResult.PASS);
    }

    private static InteractionResult rotate(Level level, BlockPos pos, BlockState rotated, Wrenchable wrenchable) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, rotated, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        wrenchable.onWrenchRotated(level, pos, rotated);
        return InteractionResult.SUCCESS;
    }

    // 拆除：本体回收到背包，内部物品照常掉落
    private static InteractionResult disassemble(Player player, Level level, BlockPos pos, BlockHitResult hit,
            ItemStack wrench) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        // AE2 机器沿用 AE2 拆除流程
        if (level.getBlockEntity(pos) instanceof AEBaseBlockEntity aeBlockEntity) {
            return aeBlockEntity.disassembleWithWrench(player, level, hit, wrench);
        }
        BlockState state = level.getBlockState(pos);
        level.removeBlock(pos, false);
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(state.getBlock()));
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.sidedSuccess(false);
    }
}
