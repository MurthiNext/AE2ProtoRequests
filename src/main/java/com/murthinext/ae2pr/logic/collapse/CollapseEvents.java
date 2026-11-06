package com.murthinext.ae2pr.logic.collapse;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import appeng.blockentity.AEBaseBlockEntity;

import com.murthinext.ae2pr.Config;

/**
 * 第三方机器的通用坍缩入口：玩家破坏方块时，若该方块在 {@code collapse.extraEnableCollapse} 内
 * 且物品总量达到阈值，就把内容压成元件就地掉落并清空库存，后续原版掉落自然为空。
 * <p>
 * AE 生态的方块由 {@code AEBaseBlockEntityDropsMixin} 走掉落列表处理，这里跳过以免重复。
 */
public final class CollapseEvents {

    private CollapseEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!Config.collapseEnabled() || event.isCanceled()) {
            return;
        }
        var player = event.getPlayer();
        if (player == null || player.isCreative()) {
            // 创造模式破坏不掉落任何东西，这里保持一致
            return;
        }
        var level = event.getLevel();
        if (level.isClientSide() || !(level instanceof net.minecraft.world.level.Level world)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (blockEntity == null || blockEntity instanceof AEBaseBlockEntity) {
            return;
        }
        ItemStack cell;
        try {
            cell = CollapseHandler.tryCollapse(blockEntity);
        } catch (RuntimeException e) {
            // 第三方库存实现异常不应打断正常破坏流程
            return;
        }
        if (cell != null) {
            Block.popResource(world, event.getPos(), cell);
        }
    }

    public static boolean collapseInto(BlockEntity blockEntity, java.util.List<ItemStack> drops) {
        return CollapseHandler.collapseInto(blockEntity, drops);
    }
}
