package com.murthinext.ae2pr.machine.crystal_assembly_line;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.registry.ModModules;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.machine.part.MachinePart;
import com.murthinext.ae2pr.api.machine.part.InputGroup;
import com.murthinext.ae2pr.api.multiblock.StructureResult;
import com.murthinext.ae2pr.api.multiblock.module.ModuleSnapshot;
import com.murthinext.ae2pr.api.recipe.CountedIngredient;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;

public class CrystalAssemblyLineControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    private static final BooleanProperty ACTIVE = CrystalAssemblyLineUnitBlock.ACTIVE;

    private int lastSlices;

    private ModuleSnapshot modules = ModuleSnapshot.EMPTY;
    private final List<UnitCell> unitCells = new ArrayList<>();

    public CrystalAssemblyLineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.CRYSTAL_ASSEMBLY_LINE, pos, state);
    }

    @Override
    protected int maxSlices() {
        return CrystalAssemblyLineStructure.INSTANCE.maxSlices();
    }

    @Override
    protected long parallelBonus() {
        return (long) Math.max(0, lastSlices - CrystalAssemblyLineStructure.MIN_SLICES) * ModConfig.assemblyParallelPerSlice()
                + (long) modules.count(ModModules.PARALLEL) * ModConfig.assemblyModuleParallelPerUnit();
    }

    @Override
    public double getSpeedMultiplier() {
        return (1.0 + modules.count(ModModules.SPEED) * ModConfig.assemblyModuleSpeedPerUnit()) * super.getSpeedMultiplier();
    }

    // 配方匹配

    @Nullable
    @Override
    protected Match findMatch() {
        for (MultiblockRecipe recipe : recipes()) {
            if (!(recipe instanceof CrystalAssemblyLineRecipe lineRecipe)) {
                continue;
            }
            if (!modules.satisfies(lineRecipe.getModuleRequirements())) {
                continue;
            }
            InputGroup best = null;
            int bestParallel = 0;
            // 隔离件各自成组，同一配方只能取自完全匹配的单个组
            for (InputGroup group : inputGroups()) {
                List<MachinePart.ItemPart> buses = matchItems(lineRecipe, group);
                if (buses == null) {
                    continue;
                }
                List<MachinePart.FluidPart> hatches = matchFluids(lineRecipe, group);
                if (hatches == null) {
                    continue;
                }
                InputGroup selected = new InputGroup();
                buses.forEach(selected::addBus);
                hatches.forEach(selected::addHatch);
                int parallel = minSlotParallel(lineRecipe, selected);
                if (parallel > bestParallel) {
                    best = selected;
                    bestParallel = parallel;
                }
            }
            if (best != null) {
                return new Match(lineRecipe, bestParallel, best);
            }
        }
        return null;
    }

    // 物品输入匹配，一仓一种原料，返回选中的总线
    @Nullable
    private List<MachinePart.ItemPart> matchItems(CrystalAssemblyLineRecipe recipe, InputGroup group) {
        List<CountedIngredient> inputs = recipe.getItemInputs();
        List<MachinePart.ItemPart> buses = group.buses();
        List<MachinePart.ItemPart> matched = new ArrayList<>(inputs.size());
        if (inputs.isEmpty()) {
            return matched;
        }
        if (ModConfig.assemblyItemsOrdered()) {
            // 有序模式沿用绝对索引，并要求该部件属于候选组
            if (inputBuses.size() < inputs.size()) {
                return null;
            }
            for (int i = 0; i < inputs.size(); i++) {
                MachinePart.ItemPart bus = inputBuses.get(i);
                if (!buses.contains(bus) || !hasItems(bus, inputs.get(i))) {
                    return null;
                }
                matched.add(bus);
            }
            return matched;
        }
        boolean[] used = new boolean[buses.size()];
        for (CountedIngredient input : inputs) {
            int found = -1;
            for (int i = 0; i < buses.size(); i++) {
                if (!used[i] && hasItems(buses.get(i), input)) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                return null;
            }
            used[found] = true;
            matched.add(buses.get(found));
        }
        return matched;
    }

    private static boolean hasItems(MachinePart.ItemPart bus, CountedIngredient input) {
        return input.test(bus.storage().getStackInSlot(0));
    }

    // 流体输入匹配，一仓一种原料，返回选中的仓
    @Nullable
    private List<MachinePart.FluidPart> matchFluids(CrystalAssemblyLineRecipe recipe, InputGroup group) {
        List<FluidStack> inputs = recipe.getFluidInputs();
        List<MachinePart.FluidPart> hatches = group.hatches();
        List<MachinePart.FluidPart> matched = new ArrayList<>(inputs.size());
        if (inputs.isEmpty()) {
            return matched;
        }
        if (ModConfig.assemblyFluidsOrdered()) {
            if (inputHatches.size() < inputs.size()) {
                return null;
            }
            for (int i = 0; i < inputs.size(); i++) {
                MachinePart.FluidPart hatch = inputHatches.get(i);
                if (!hatches.contains(hatch) || !hasFluid(hatch, inputs.get(i))) {
                    return null;
                }
                matched.add(hatch);
            }
            return matched;
        }
        boolean[] used = new boolean[hatches.size()];
        for (FluidStack input : inputs) {
            int found = -1;
            for (int i = 0; i < hatches.size(); i++) {
                if (!used[i] && hasFluid(hatches.get(i), input)) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                return null;
            }
            used[found] = true;
            matched.add(hatches.get(found));
        }
        return matched;
    }

    private static boolean hasFluid(MachinePart.FluidPart hatch, FluidStack input) {
        FluidStack stored = hatch.tank(0).getFluid();
        return stored.isFluidEqual(input) && stored.getAmount() > 0;
    }

    // 组内可执行的份数，产物容量交给基类统一检查
    private int minSlotParallel(CrystalAssemblyLineRecipe recipe, InputGroup group) {
        int low = 0;
        int high = maxParallel();
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (group.inventory().consume(recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    // 结构联动

    @Override
    protected void onStructureChanged(StructureResult result) {
        lastSlices = result.slices();

        ModuleSnapshot snapshot = result.formed() ? result.modules() : ModuleSnapshot.EMPTY;
        MultiblockRecipe recipe = currentRecipe();
        if (recipe instanceof CrystalAssemblyLineRecipe lineRecipe
                && !snapshot.satisfies(lineRecipe.getModuleRequirements())) {
            cancelJob();
        }
        modules = snapshot;
    }

    @Override
    protected void onPartsCleared() {
        unitCells.clear();
    }

    @Override
    protected void collectExtraPart(BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (level != null && level.getBlockState(pos).getBlock() instanceof CrystalAssemblyLineUnitBlock) {
            unitCells.add(new UnitCell(pos, level.getBlockState(pos).getBlock()));
        }
    }

    @Override
    protected void onControllerStateChanged(boolean formed, boolean running) {
        for (UnitCell cell : unitCells) {
            if (level != null && level.getBlockState(cell.pos()).hasProperty(ACTIVE)) {
                CrystalAssemblyLineStructure.updateUnitActive(level, cell.pos(), running);
            }
        }
    }

    @Override
    public void validateStructure() {
        // 控制外壳被替换时强制重新匹配结构
        if (jobTicksLeft > 0 && unitCellsChanged()) {
            structure.invalidate();
        }
        super.validateStructure();
    }

    private boolean unitCellsChanged() {
        Level level = this.level;
        if (level == null) {
            return false;
        }
        for (UnitCell cell : unitCells) {
            if (level.getBlockState(cell.pos()).getBlock() != cell.block()) {
                return true;
            }
        }
        return false;
    }

    private record UnitCell(BlockPos pos, Block block) {
    }
}
