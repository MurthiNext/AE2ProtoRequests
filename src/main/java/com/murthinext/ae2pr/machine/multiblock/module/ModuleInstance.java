package com.murthinext.ae2pr.machine.multiblock.module;

import net.minecraft.core.BlockPos;

// 结构内一个已安装的模块：位置 + 定义
public record ModuleInstance(BlockPos pos, ModuleDefinition definition) {
}
