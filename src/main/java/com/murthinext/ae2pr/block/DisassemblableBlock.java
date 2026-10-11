package com.murthinext.ae2pr.block;

import net.minecraft.world.level.block.Block;

import com.murthinext.ae2pr.logic.wrench.Wrenchable;

/**
 * 可扳手拆除的机械方块，仅支持 Shift+右键拆除。
 */
public class DisassemblableBlock extends Block implements Wrenchable {

    public DisassemblableBlock(Properties properties) {
        super(properties);
    }
}
