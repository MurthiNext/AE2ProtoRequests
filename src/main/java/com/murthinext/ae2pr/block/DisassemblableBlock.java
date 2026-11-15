package com.murthinext.ae2pr.block;

import com.murthinext.ae2pr.api.text.Voltage;
import com.murthinext.ae2pr.api.wrench.Wrenchable;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;

// 可扳手拆除的机械方块
public class DisassemblableBlock extends Block implements Wrenchable {

    public DisassemblableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MutableComponent getName() {
        return Voltage.name(getDescriptionId());
    }
}
