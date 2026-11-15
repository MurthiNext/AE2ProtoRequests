package com.murthinext.ae2pr.block;

import com.murthinext.ae2pr.api.wrench.Wrenchable;
import net.minecraft.world.level.block.GlassBlock;

// 扳手拆除的玻璃方块
public class DisassemblableGlassBlock extends GlassBlock implements Wrenchable {

    public DisassemblableGlassBlock(Properties properties) {
        super(properties);
    }
}
