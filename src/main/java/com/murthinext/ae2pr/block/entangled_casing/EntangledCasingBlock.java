package com.murthinext.ae2pr.block.entangled_casing;

import com.murthinext.ae2pr.api.ClientHooks;
import com.murthinext.ae2pr.block.DisassemblableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class EntangledCasingBlock extends DisassemblableBlock {

    public EntangledCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        ClientHooks.run(() -> () -> EntangledCasingPulse.offer(level, pos));
    }
}
