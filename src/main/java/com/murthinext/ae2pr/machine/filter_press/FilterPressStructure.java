package com.murthinext.ae2pr.machine.filter_press;

import com.murthinext.ae2pr.registry.ModMachines;
import net.minecraft.world.level.block.Blocks;

import com.murthinext.ae2pr.registry.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.machine.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.machine.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.machine.multiblock.RelativeDirection;
import com.murthinext.ae2pr.machine.multiblock.StructurePredicate;

public final class FilterPressStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(1, 10)
            .slice("#SSSSSSSSSS#", "#SAAAAAAAAK#", "#SAAAAAAAAS#", "#SAAAAAAAAS#")
            .slice("SSSSSSSSSSSS", "SSSSSSSSSSSS", "SLAAAAAAAALS", "SSAAAAAAAASS")
            .slice("SSSSSSSSSSSS", "SLAAAAAAAALS", "SLPPPPPPPPLS", "SSPPPPPPPPSS")
            .slice("SSSSSSSSSSSS", "SSSSSSSSSSSS", "SLAAAAAAAALS", "SSAAAAAAAASS")
            .slice("#SSSSSSSSSS#", "#SAAAAAAAAS#", "#SAAAAAAAAS#", "#SAAAAAAAAS#")
            .where('K', StructurePredicate.blocks(ModMachines.FILTER_PRESS.block()))
            .where('S', StructurePredicate.casingWithParts(ModBlocks.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('P', StructurePredicate.blocks(ModBlocks.FILTER_PRESS_PLATE.get()))
            .where('A', StructurePredicate.blocks(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR))
            .where('#', StructurePredicate.any())
            .build();

    public static final FilterPressStructure INSTANCE = new FilterPressStructure();

    private FilterPressStructure() {
        super(PATTERN, MachinePartFacade.ZIRCONIUM_IRON);
    }
}
