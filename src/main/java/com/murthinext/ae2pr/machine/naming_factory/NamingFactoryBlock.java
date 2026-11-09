package com.murthinext.ae2pr.machine.naming_factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.murthinext.ae2pr.api.machine.singleblock.AbstractSingleblockMachineBlock;
import com.murthinext.ae2pr.api.machine.singleblock.SingleblockMachineDefinition;
import com.murthinext.ae2pr.api.wrench.WrenchTags;

// 名称压印工厂方块
public class NamingFactoryBlock extends AbstractSingleblockMachineBlock<NamingFactoryBlockEntity> {

    public NamingFactoryBlock(SingleblockMachineDefinition<NamingFactoryBlock, NamingFactoryBlockEntity,
            NamingFactoryMenu> definition) {
        super(definition);
    }

    // 手持切割刀时打开重命名界面，潜行时交给切割刀自身界面处理
    @Override
    protected InteractionResult useWithItem(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).is(WrenchTags.KNIFE)) {
            return null;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            openRenameScreen(level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openRenameScreen(Level level, BlockPos pos) {
        String currentName = level.getBlockEntity(pos) instanceof NamingFactoryBlockEntity factory
                && factory.getCustomName() != null ? factory.getCustomName().getString() : "";
        NamingFactoryRenameScreen.open(pos, currentName);
    }
}
