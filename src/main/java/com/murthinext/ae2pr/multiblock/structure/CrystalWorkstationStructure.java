package com.murthinext.ae2pr.multiblock.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

/**
 * 晶能工作站结构定义。
 */
public final class CrystalWorkstationStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(0, 4)
            .slice("AAAAKAAAA", "B#######B", "B#######B", "B#######B", "AAAAAAAAA")
            .slice("AAAAAAAAA", "CDDDDDDDC", "CDDDDDDDC", "CDDDDDDDC", "AAAAAAAAA")
            .slice("AAAAAAAAA", "C#######C", "C###G###C", "C#######C", "AAAAAAAAA")
            .slice("AAAAAAAAA", "CDDDDDDDC", "CDDDDDDDC", "CDDDDDDDC", "AAAAAAAAA")
            .slice("AAAAAAAAA", "B#######B", "B#######B", "B#######B", "AAAAAAAAA")
            .where('K', StructurePredicate.blocks(ModBlocks.CRYSTAL_WORKSTATION.get()))
            .where('A', StructurePredicate.blocks(ModBlocks.REINFORCED_HIGH_POWER_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), ModBlocks.AEV_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), ModBlocks.AEV_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), ModBlocks.AEV_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(), ModBlocks.AEV_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).maxCount(1)))
            .where('B', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()))
            .where('C', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_STABILITY_CASING.get()))
            .where('D', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_CORE.get()))
            .where('#', StructurePredicate.blocks(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR))
            .build();

    public static final CrystalWorkstationStructure INSTANCE = new CrystalWorkstationStructure();

    private CrystalWorkstationStructure() {
        super(PATTERN, MachinePartFacade.HIGH_POWER);
    }

    public static BlockPos corePos(BlockPos controllerPos, Direction facing, boolean mirrorFront) {
        return controllerPos.relative(RelativeDirection.BACK.getFacing(facing, Direction.UP, false, mirrorFront), 2)
                .above(2);
    }
}
