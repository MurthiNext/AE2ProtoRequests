package com.murthinext.ae2pr.machine.crystal_assembly_line;

import com.murthinext.ae2pr.api.multiblock.module.ModuleDefinition;
import com.murthinext.ae2pr.api.multiblock.module.ModuleProvider;

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
