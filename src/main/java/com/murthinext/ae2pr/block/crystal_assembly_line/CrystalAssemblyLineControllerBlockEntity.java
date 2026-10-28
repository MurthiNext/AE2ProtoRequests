package com.murthinext.ae2pr.block.crystal_assembly_line;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModModules;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MachinePartFluidStorage;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.InputGroup;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.module.ModuleSnapshot;
import com.murthinext.ae2pr.multiblock.module.ModuleType;
import com.murthinext.ae2pr.multiblock.structure.CrystalAssemblyLineStructure;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

public class CrystalAssemblyLineControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    private static final BooleanProperty ACTIVE = CrystalAssemblyLineUnitBlock.ACTIVE;

    private int lastSlices;
    private int lastMismatches;
    @Nullable
    private BlockPos lastMismatchPos;
    private char lastExpected = ' ';
    @Nullable
    private Block lastFound;

    private ModuleSnapshot modules = ModuleSnapshot.EMPTY;
    private final List<UnitCell> unitCells = new ArrayList<>();

    public CrystalAssemblyLineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_ASSEMBLY_LINE.get(), pos, state, CrystalAssemblyLineStructure.INSTANCE,
                ModItems.CRYSTAL_ASSEMBLY_LINE.get());
    }

    @Override
    public String machineKey() {
        return "crystal_assembly_line";
    }

    @Override
    protected RecipeType<? extends MultiblockRecipe> recipeType() {
        return ModRecipes.CRYSTAL_ASSEMBLY_LINE_TYPE.get();
    }

    @Override
    protected int maxSlices() {
        return CrystalAssemblyLineStructure.INSTANCE.maxSlices();
    }

    @Override
    public int maxParallel() {
        int base = Config.assemblyBaseParallel()
                + Math.max(0, lastSlices - CrystalAssemblyLineStructure.MIN_SLICES) * Config.assemblyParallelPerSlice();
        return Math.min(base + moduleParallelBonus(), Config.assemblyMaxParallel());
    }

    private int moduleParallelBonus() {
        return modules.count(ModModules.PARALLEL) * Config.assemblyModuleParallelPerUnit();
    }

    @Override
    public double getSpeedMultiplier() {
        double multiplier = 1.0 + modules.count(ModModules.SPEED) * Config.assemblyModuleSpeedPerUnit();
        return Math.min(multiplier, Config.assemblyMaxSpeedMultiplier()) * speedCardMultiplier();
    }

    public double speedCardMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    public int getModuleLevel(ModuleType type) {
        return modules.level(type);
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
                List<ItemBusBlockEntity> buses = matchItems(lineRecipe, group);
                if (buses == null) {
                    continue;
                }
                List<MachinePartFluidStorage> hatches = matchFluids(lineRecipe, group);
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
    private List<ItemBusBlockEntity> matchItems(CrystalAssemblyLineRecipe recipe, InputGroup group) {
        List<CountedIngredient> inputs = recipe.getItemInputs();
        List<ItemBusBlockEntity> buses = group.buses();
        List<ItemBusBlockEntity> matched = new ArrayList<>(inputs.size());
        if (inputs.isEmpty()) {
            return matched;
        }
        if (Config.assemblyItemsOrdered()) {
            // 有序模式沿用绝对索引，并要求该部件属于候选组
            if (inputBuses.size() < inputs.size()) {
                return null;
            }
            for (int i = 0; i < inputs.size(); i++) {
                ItemBusBlockEntity bus = inputBuses.get(i);
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

    private static boolean hasItems(ItemBusBlockEntity bus, CountedIngredient input) {
        return input.test(bus.getStorage().getStackInSlot(0));
    }

    // 流体输入匹配，一仓一种原料，返回选中的仓
    @Nullable
    private List<MachinePartFluidStorage> matchFluids(CrystalAssemblyLineRecipe recipe, InputGroup group) {
        List<FluidStack> inputs = recipe.getFluidInputs();
        List<MachinePartFluidStorage> hatches = group.hatches();
        List<MachinePartFluidStorage> matched = new ArrayList<>(inputs.size());
        if (inputs.isEmpty()) {
            return matched;
        }
        if (Config.assemblyFluidsOrdered()) {
            if (inputHatches.size() < inputs.size()) {
                return null;
            }
            for (int i = 0; i < inputs.size(); i++) {
                MachinePartFluidStorage hatch = inputHatches.get(i);
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

    private static boolean hasFluid(MachinePartFluidStorage hatch, FluidStack input) {
        FluidStack stored = hatch.getTank().getFluid();
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
        lastMismatches = result.mismatches();
        lastMismatchPos = result.mismatchPos();
        lastExpected = result.expected();
        lastFound = result.found();

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

    // 状态查询

    public int getLastSlices() {
        return lastSlices;
    }

    public int getLastMismatches() {
        return lastMismatches;
    }

    @Nullable
    public BlockPos getLastMismatchPos() {
        return lastMismatchPos;
    }

    public char getLastExpected() {
        return lastExpected;
    }

    @Nullable
    public Block getLastFound() {
        return lastFound;
    }
}
