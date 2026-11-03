package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.api.multiblock.StructureHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;

final class MachinePartAppearance {

    private MachinePartAppearance() {
    }

    static void install() {
        StructureHooks.install(
                () -> ModBlocks.MACHINE_PARTS.stream().map(entry -> (Block) entry.get()).toList(),
                () -> ModBlocks.ME_NETWORK_ACCESS_HATCH.get(),
                MachinePartAppearance::apply);
    }

    private static void apply(Level level, BlockPos pos, boolean formed, int style, boolean active) {
        int targetStyle = formed ? style : MachinePartBlock.DEFAULT_STYLE;
        BlockState state = level.getBlockState(pos);
        BlockState updated = state;
        if (state.is(ModBlocks.ME_NETWORK_ACCESS_HATCH.get())) {
            if (state.getValue(MENetworkAccessHatchBlock.FORMED) != formed) {
                updated = updated.setValue(MENetworkAccessHatchBlock.FORMED, formed);
            }
            if (updated.getValue(MENetworkAccessHatchBlock.STYLE) != targetStyle) {
                updated = updated.setValue(MENetworkAccessHatchBlock.STYLE, targetStyle);
            }
        } else if (state.getBlock() instanceof MachinePartBlock) {
            if (state.getValue(MachinePartBlock.FORMED) != formed) {
                updated = updated.setValue(MachinePartBlock.FORMED, formed);
            }
            if (updated.getValue(MachinePartBlock.STYLE) != targetStyle) {
                updated = updated.setValue(MachinePartBlock.STYLE, targetStyle);
            }
        } else {
            return;
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }
}
