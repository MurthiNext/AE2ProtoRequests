package com.murthinext.ae2pr.block.crystal_assembly_line;

import com.murthinext.ae2pr.multiblock.module.ModuleDefinition;
import com.murthinext.ae2pr.multiblock.module.ModuleProvider;

/**
 * 水晶装配线替换模块方块。
 */
public class CrystalAssemblyLineModuleBlock extends CrystalAssemblyLineUnitBlock implements ModuleProvider {

    private final ModuleDefinition definition;

    public CrystalAssemblyLineModuleBlock(ModuleDefinition definition) {
        this.definition = definition;
    }

    @Override
    public ModuleDefinition moduleDefinition() {
        return definition;
    }
}
