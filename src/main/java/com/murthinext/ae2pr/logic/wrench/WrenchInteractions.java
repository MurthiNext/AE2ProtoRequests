package com.murthinext.ae2pr.logic.wrench;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.blockentity.AEBaseBlockEntity;

import com.murthinext.ae2pr.ModTags;

/**
 * 扳手交互统一入口：右键旋转有朝向的方块，Shift+右键拆除机械方块。
 */
public final class WrenchInteractions {

    /** 旋转顺序 */
    private static final Direction[] ROTATION_ORDER = { Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST };

    private WrenchInteractions() {
    }

    /**
     * 扳手右键总路由：潜行拆除，否则旋转；仅接管 {@link Wrenchable} 方块的对应能力。
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() == Event.Result.DENY
                || event.getEntity().isSpectator() || !event.getItemStack().is(ModTags.WRENCHES)) {
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
        DirectionProperty facing = wrenchable.wrenchFacing();
        if (facing == null) {
            // 无朝向：扳手与其他物品一样交给方块自身处理（如打开界面）
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(wrenchable.canRotateWithWrench(state)
                ? rotate(event.getLevel(), event.getPos(), state, wrenchable, facing)
                : InteractionResult.PASS);
    }

    /** 旋转朝向：水平朝向顺时针，其余按固定顺序循环。 */
    private static InteractionResult rotate(Level level, BlockPos pos, BlockState state, Wrenchable wrenchable,
            DirectionProperty facing) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState rotated = state.setValue(facing, nextFacing(facing, state.getValue(facing)));
        level.setBlock(pos, rotated, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        wrenchable.onWrenchRotated(level, pos, rotated);
        return InteractionResult.SUCCESS;
    }

    /** 拆除：方块本体回收到玩家背包，内部物品由 {@code onRemove} 照常掉落。 */
    private static InteractionResult disassemble(Player player, Level level, BlockPos pos, BlockHitResult hit,
            ItemStack wrench) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        // AE2 机器沿用 AE2 的拆除流程：导出方块设置、把本体与内部内容放入玩家背包
        if (level.getBlockEntity(pos) instanceof AEBaseBlockEntity aeBlockEntity) {
            return aeBlockEntity.disassembleWithWrench(player, level, hit, wrench);
        }
        BlockState state = level.getBlockState(pos);
        level.removeBlock(pos, false);
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(state.getBlock()));
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.sidedSuccess(false);
    }

    private static Direction nextFacing(DirectionProperty facing, Direction current) {
        if (facing == BlockStateProperties.HORIZONTAL_FACING) {
            return current.getClockWise();
        }
        for (int i = 0; i < ROTATION_ORDER.length; i++) {
            if (ROTATION_ORDER[i] == current) {
                return ROTATION_ORDER[(i + 1) % ROTATION_ORDER.length];
            }
        }
        return Direction.NORTH;
    }
}
