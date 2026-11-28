package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.api.multiblock.StructureHooks;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class MachinePartAppearance {

    private MachinePartAppearance() {}

    static void install() {
        StructureHooks.install(
                () -> Stream.concat(
                                ModBlocks.machineParts().stream().map(BlockDef::get),
                                Stream.<Block>of(ModBlocks.ME_PATTERN_BUFFER.get()))
                        .toList(),
                ModBlocks.ME_NETWORK_ACCESS_HATCH::get,
                MachinePartAppearance::apply);
    }

    private static void apply(Level level, BlockPos pos, boolean formed, int style, boolean active) {
        int targetStyle = formed ? style : MachinePartBlock.DEFAULT_STYLE;
        BlockState state = level.getBlockState(pos);
        BlockState updated = state;
        if (state.getBlock() instanceof MachinePartBlock
                || state.is(ModBlocks.ME_NETWORK_ACCESS_HATCH.get())
                || state.is(ModBlocks.ME_PATTERN_BUFFER.get())) {
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
