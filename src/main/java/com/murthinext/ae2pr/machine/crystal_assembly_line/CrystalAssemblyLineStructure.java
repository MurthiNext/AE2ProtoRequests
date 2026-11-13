package com.murthinext.ae2pr.machine.crystal_assembly_line;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ServerConfig;
import com.murthinext.ae2pr.api.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.api.multiblock.MachinePartFacade;
import com.murthinext.ae2pr.api.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.api.multiblock.RelativeDirection;
import com.murthinext.ae2pr.api.multiblock.StructurePredicate;
import com.murthinext.ae2pr.registry.ModBlocks;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.registry.ModModules;

// 水晶装配线结构定义与外观同步
public final class CrystalAssemblyLineStructure extends AbstractStructureFacade {

    public static final MultiblockPattern
            PATTERN = MultiblockPattern.builder(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
                              .origin(2, 0)
                              .slice("FIF", "RTR", "SAG", "#Y#")
                              .sliceRepeatable(3, Integer.MAX_VALUE, "FIF", "RTR", "DAG", "#Y#")
                              .slice("FOF", "RTR", "DAG", "#Y#")
                              .where('S', StructurePredicate.blocks(ModMachines.CRYSTAL_ASSEMBLY_LINE.block()))
                              .where('F',
                                     StructurePredicate
                                             .blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get())
                                             .or(StructurePredicate.blocks(
                                                     ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()))
                                             .or(StructurePredicate.blocks(ModBlocks.ME_NETWORK_ACCESS_HATCH.get())
                                                         .maxCount(1)))
                              .where('Y',
                                     StructurePredicate.blocks(
                                             ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()))
                              .where('I', StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()))
                              .where('O', StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()))
                              .where('A', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CASING.get()))
                              .where('G', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()))
                              .where('D', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()))
                              .where('R', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
                              .where('T',
                                     StructurePredicate.moduleSlot(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT.get(),
                                                                   ModModules.CHARGING, ModModules.PARALLEL,
                                                                   ModModules.SPEED))
                              .where('#', StructurePredicate.any())
                              .build();

    public static final CrystalAssemblyLineStructure INSTANCE = new CrystalAssemblyLineStructure();

    public static final int MIN_SLICES = PATTERN.minSlices();

    private CrystalAssemblyLineStructure() {
        super(PATTERN, MachinePartFacade.CRYSTAL);
    }

    @Override
    public int maxSlices() {
        return Math.max(MIN_SLICES, ServerConfig.crystalAssemblyLineMaxSlices());
    }

    @Override
    protected void onPartCell(Level level, BlockPos pos, boolean active) {
        updateUnitActive(level, pos, active);
    }

    // 控制外壳与替换模块的工作外观
    public static void updateUnitActive(Level level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CrystalAssemblyLineUnitBlock &&
            state.getValue(CrystalAssemblyLineUnitBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(CrystalAssemblyLineUnitBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }
}
