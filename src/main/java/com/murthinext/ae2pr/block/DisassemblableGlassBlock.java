package com.murthinext.ae2pr.block;

import net.minecraft.world.level.block.GlassBlock;

import com.murthinext.ae2pr.logic.wrench.Wrenchable;

/**
 * 可扳手拆除的玻璃方块，无朝向不可旋转，仅支持 Shift+右键拆除。
 */
public class DisassemblableGlassBlock extends GlassBlock implements Wrenchable {

    public DisassemblableGlassBlock(Properties properties) {
        super(properties);
    }
}
