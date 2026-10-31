package com.murthinext.ae2pr.block.filter_press;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.structure.FilterPressStructure;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

public class FilterPressControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public FilterPressControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_PRESS.get(), pos, state, FilterPressStructure.INSTANCE,
                ModItems.FILTER_PRESS.get());
    }

    @Override
    public String machineKey() {
        return FilterPressBlock.MACHINE_KEY;
    }

    @Override
    protected RecipeType<? extends MultiblockRecipe> recipeType() {
        return ModRecipes.FILTER_PRESS_TYPE.get();
    }

    @Override
    public int maxParallel() {
        return Config.filterPressParallel();
    }

    @Nullable
    @Override
    protected Match findMatch() {
        for (MultiblockRecipe recipe : recipes()) {
            Match match = matchGroups(recipe);
            if (match != null) {
                return match;
            }
        }
        return null;
    }
}
