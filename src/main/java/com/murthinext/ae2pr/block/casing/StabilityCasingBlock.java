package com.murthinext.ae2pr.block.casing;

import com.murthinext.ae2pr.block.DisassemblableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class StabilityCasingBlock extends DisassemblableBlock {

    public static final BooleanProperty FIELD_LIT = BooleanProperty.create("field_lit");

    public StabilityCasingBlock(Properties properties) {
        super(properties.lightLevel(StabilityCasingBlock::fieldLight));
        registerDefaultState(defaultBlockState().setValue(FIELD_LIT, false));
    }

    public static int fieldLight(BlockState state) {
        return state.getValue(FIELD_LIT) ? 15 : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FIELD_LIT);
    }
}
