package com.murthinext.ae2pr.block.feature.filter_press;

import com.murthinext.ae2pr.api.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.api.multiblock.MachinePartFacade;
import com.murthinext.ae2pr.api.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.api.multiblock.RelativeDirection;
import com.murthinext.ae2pr.api.multiblock.StructurePredicate;
import com.murthinext.ae2pr.core.registry.ModBlocks;
import com.murthinext.ae2pr.core.registry.ModMachines;

public final class FilterPressStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern.builder(
                    RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.BACK)
            .sliceOrigin(1)
            .origin(1, 0)
            .slice("#SSS#", "#SSS#", "#SSS#", "#SSS#")
            .slice("SSSSS", "KSLSS", "SLLLS", "SSSSS")
            .sliceRepeatable(5, 32, "SSSSS", "ASASA", "AAPAA", "AAPAA")
            .slice("SSSSS", "SSLSS", "SLLLS", "SSSSS")
            .slice("#SSS#", "#SSS#", "#SSS#", "#SSS#")
            .where('K', StructurePredicate.blocks(ModMachines.filterPress().block()))
            .where('S', StructurePredicate.casingWithParts(ModBlocks.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where(
                    'P',
                    StructurePredicate.blocks(ModBlocks.FILTER_PRESS_PLATE.get())
                            .axis(RelativeDirection.RIGHT))
            .where('A', StructurePredicate.any())
            .where('#', StructurePredicate.any())
            .build();

    public static final FilterPressStructure INSTANCE = new FilterPressStructure();

    private FilterPressStructure() {
        super(PATTERN, MachinePartFacade.ZIRCONIUM_IRON);
    }
}
