package com.murthinext.ae2pr.machine.integrated_mass_fabricator;

import com.murthinext.ae2pr.api.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.api.multiblock.MachinePartFacade;
import com.murthinext.ae2pr.api.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.api.multiblock.RelativeDirection;
import com.murthinext.ae2pr.api.multiblock.StructurePredicate;
import com.murthinext.ae2pr.registry.ModBlocks;
import com.murthinext.ae2pr.registry.ModMachines;

public final class IntegratedMassFabricatorStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern.builder(
                    RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
            .sliceOrigin(8)
            .origin(1, 6)
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".SSS.....SSS.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "..SEE...EES..",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    "...EEEEEEE...",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".....SSS.....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....E...E....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    ".....SSS.....",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".....SSS.....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....E...E....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    ".....SSS.....",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".....SSS.....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....E...E....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    ".....SSS.....",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    "......K......",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    "...EEEEEEE...",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "..SEE...EES..",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".SSS.....SSS.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .where('.', StructurePredicate.any())
            .where('K', StructurePredicate.blocks(ModMachines.INTEGRATED_MASS_FABRICATOR.block()))
            .where('S', StructurePredicate.casingWithParts(ModBlocks.STABILITY_CASING.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            .where('E', StructurePredicate.blocks(ModBlocks.ENTANGLED_CASING.get()))
            .build();

    public static final IntegratedMassFabricatorStructure INSTANCE = new IntegratedMassFabricatorStructure();

    private IntegratedMassFabricatorStructure() {
        super(PATTERN, MachinePartFacade.STABILITY);
    }
}
