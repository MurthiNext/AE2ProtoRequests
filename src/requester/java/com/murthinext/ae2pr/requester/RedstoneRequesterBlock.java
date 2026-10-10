/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
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

/**
 * ME 红石请求器方块。
 * <p>
 * 结构参考 ME Requester（https://github.com/AlmostReliable/merequester，LGPL-3.0）的 {@code RequesterBlock}。
 */
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
                    RequesterUtils.f("{}_desc", RequesterConstants.REQUESTER_ID))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            RequesterUtils.addShiftInfoTooltip(tooltip);
        }
    }
}
