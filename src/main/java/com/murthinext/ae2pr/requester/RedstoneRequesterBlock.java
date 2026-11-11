package com.murthinext.ae2pr.requester;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;

public class RedstoneRequesterBlock extends AEBaseEntityBlock<RedstoneRequesterBlockEntity> {

    public RedstoneRequesterBlock() {
        super(metalProps());
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player, InteractionHand hand,
            @Nullable ItemStack stack, BlockHitResult hit) {
        var entity = getBlockEntity(level, pos);
        if (entity == null || InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            MenuOpener.open(RedstoneRequesterMenu.TYPE, player, MenuLocators.forBlockEntity(entity));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        var entity = getBlockEntity(level, pos);
        if (entity != null) {
            entity.onRedstoneChanged(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.literal(" "));
            tooltip.add(RequesterUtils.translate("tooltip",
                    RequesterUtils.f("{}_desc", RequesterConstants.REQUESTER_ID)));
        } else {
            RequesterUtils.addShiftInfoTooltip(tooltip);
        }
    }
}
