package com.murthinext.ae2pr.block.crystal_reaction_chamber;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.structure.CrystalReactionChamberStructure;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

public class CrystalReactionChamberControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public CrystalReactionChamberControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_REACTION_CHAMBER.get(), pos, state, CrystalReactionChamberStructure.INSTANCE,
                ModItems.CRYSTAL_REACTION_CHAMBER.get());
    }

    @Override
    public String machineKey() {
        return "crystal_reaction_chamber";
    }

    @Override
    protected RecipeType<? extends MultiblockRecipe> recipeType() {
        return ModRecipes.CRYSTAL_REACTION_CHAMBER_TYPE.get();
    }

    @Override
    public int maxParallel() {
        return Math.max(1, Config.crystalReactionParallel());
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

    public BlockPos getBuddingQuartzPos() {
        return CrystalReactionChamberStructure.buddingQuartzPos(worldPosition, getFacing(), mirrorSide,
                mirrorFront);
    }

    @Override
    public BlockPos getCorePos() {
        return getBuddingQuartzPos().above(2);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBuddingQuartzPos()).inflate(2.0);
    }
}
