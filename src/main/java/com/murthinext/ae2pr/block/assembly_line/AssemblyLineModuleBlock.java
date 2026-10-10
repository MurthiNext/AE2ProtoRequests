package com.murthinext.ae2pr.block.assembly_line;

import com.murthinext.ae2pr.multiblock.module.ModuleDefinition;
import com.murthinext.ae2pr.multiblock.module.ModuleProvider;

/**
 * 水晶装配线替换模块方块。
 */
public class AssemblyLineModuleBlock extends AssemblyLineUnitBlock implements ModuleProvider {

    private final ModuleDefinition definition;

    public AssemblyLineModuleBlock(ModuleDefinition definition) {
        this.definition = definition;
    }

    @Override
    public ModuleDefinition moduleDefinition() {
        return definition;
    }
}
