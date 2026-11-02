package com.murthinext.ae2pr.machine.multi_factory;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.machine.ModeMachineBlockEntity;
import com.murthinext.ae2pr.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;
import com.murthinext.ae2pr.registry.ModMachines;

public class MultiFactoryControllerBlockEntity extends ModeMachineBlockEntity<MultiFactoryMode> {

    public MultiFactoryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.MULTI_FACTORY, pos, state, List.of(MultiFactoryMode.values()));
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<MultiblockRecipe> recipes() {
        if (level == null) {
            return List.of();
        }
        List<MultiblockRecipe> wrapped = new ArrayList<>();
        List<? extends Recipe<?>> all = (List<? extends Recipe<?>>) (List<?>) level.getRecipeManager()
                .getAllRecipesFor((RecipeType) getMode().recipeType());
        for (Recipe<?> recipe : all) {
            MultiFactoryRecipe adapter = MultiFactoryRecipe.wrap(recipe, getMode());
            if (adapter != null) {
                wrapped.add(adapter);
            }
        }
        wrapped.sort(AbstractMultiblockControllerBlockEntity::compareRecipePriority);
        return wrapped;
    }

    @Override
    @Nullable
    protected MultiblockRecipe resolveRecipe(ResourceLocation id) {
        if (level == null) {
            return null;
        }
        return MultiFactoryRecipe.wrap(level.getRecipeManager().byKey(id).orElse(null), getMode());
    }

    @Override
    protected boolean isRecipeApplicable(MultiblockRecipe recipe) {
        return recipe instanceof MultiFactoryRecipe wrapped && wrapped.mode() == getMode();
    }

    @Override
    public BlockPos getCorePos() {
        return MultiFactoryStructure.corePos(worldPosition, getFacing(), isMirrorFront());
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getCorePos()).inflate(2.0);
    }
}
