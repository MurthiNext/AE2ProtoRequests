package com.murthinext.ae2pr.block.crystal_workstation;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.structure.CrystalWorkstationStructure;
import com.murthinext.ae2pr.recipe.CrystalWorkstationRecipe;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

/**
 * 晶能工作站主机方块实体。
 */
public class CrystalWorkstationControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    private CrystalWorkstationMode mode = CrystalWorkstationMode.ETCHING;

    public CrystalWorkstationControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_WORKSTATION.get(), pos, state, CrystalWorkstationStructure.INSTANCE,
                ModItems.CRYSTAL_WORKSTATION.get());
    }

    @Override
    public String machineKey() {
        return "crystal_workstation";
    }

    @Override
    protected RecipeType<? extends MultiblockRecipe> recipeType() {
        return mode.recipeType();
    }

    public CrystalWorkstationMode getMode() {
        return mode;
    }

    public boolean switchMode() {
        if (level == null || level.isClientSide || isBusy()) {
            return false;
        }
        mode = mode.next();
        clearPauseState();
        markStateChanged();
        return true;
    }

    @Override
    public int maxParallel() {
        return Math.max(1, Config.crystalWorkstationParallel());
    }

    @Nullable
    @Override
    protected Match findMatch() {
        for (MultiblockRecipe recipe : recipes()) {
            int parallel = inputParallel(recipe, maxParallel());
            if (parallel > 0) {
                return new Match(recipe, parallel);
            }
        }
        return null;
    }

    @Override
    protected boolean isRecipeApplicable(MultiblockRecipe recipe) {
        return recipe instanceof CrystalWorkstationRecipe workstation && workstation.getMode() == mode;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("mode", mode.id());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = CrystalWorkstationMode.byId(tag.getString("mode"));
    }

    @Override
    public BlockPos getCorePos() {
        return CrystalWorkstationStructure.corePos(worldPosition, getFacing(), mirrorFront);
    }
}
