package com.murthinext.ae2pr.block.jig;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.structure.JigStructure;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

public class JigControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public JigControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JIG.get(), pos, state, JigStructure.INSTANCE, ModItems.JIG.get());
    }

    @Override
    public String machineKey() {
        return JigBlock.MACHINE_KEY;
    }

    @Override
    protected RecipeType<? extends MultiblockRecipe> recipeType() {
        return ModRecipes.JIG_TYPE.get();
    }

    @Override
    public int maxParallel() {
        return Config.jigParallel();
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

    public Direction getFlowDirection() {
        return JigStructure.flowDirection(getFacing(), mirrorFront);
    }

    public Direction getSideDirection() {
        return JigStructure.sideDirection(getFacing(), mirrorSide);
    }

    public List<BlockPos> getWaterCells() {
        return JigStructure.waterCells(worldPosition, getFacing(), mirrorSide, mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        List<BlockPos> cells = getWaterCells();
        return new AABB(cells.get(0)).minmax(new AABB(cells.get(cells.size() - 1)))
                .minmax(new AABB(worldPosition));
    }
}
