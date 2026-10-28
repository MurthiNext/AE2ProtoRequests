package com.murthinext.ae2pr.block.matter_phase_transformer;

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
import com.murthinext.ae2pr.multiblock.structure.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

public class MatterPhaseTransformerControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    private final MatterPhaseTransformerAnimation animation = new MatterPhaseTransformerAnimation();

    public MatterPhaseTransformerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_PHASE_TRANSFORMER.get(), pos, state, MatterPhaseTransformerStructure.INSTANCE,
                ModItems.MATTER_PHASE_TRANSFORMER.get());
    }

    @Override
    public String machineKey() {
        return "matter_phase_transformer";
    }

    @Override
    protected RecipeType<? extends MultiblockRecipe> recipeType() {
        return ModRecipes.MATTER_PHASE_TRANSFORMER_TYPE.get();
    }

    @Override
    public int maxParallel() {
        return Math.max(1, Config.matterPhaseTransformerParallel());
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

    @Override
    public BlockPos getCorePos() {
        return MatterPhaseTransformerStructure.corePos(worldPosition, getFacing(), mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos core = getCorePos();
        return new AABB(core.getX(), core.getY() - 3, core.getZ(),
                core.getX() + 1, core.getY() + 4, core.getZ() + 1);
    }

    // 客户端动画

    public void clientTick() {
        animation.tick(isFormed(), isRunning());
    }

    public double getParticleTime(float partialTick) {
        return animation.getParticleTime(partialTick);
    }

    public float getEffectActivity(float partialTick) {
        return animation.getActivity(partialTick);
    }

    public float getBeamStrength(float partialTick) {
        return animation.getBeamStrength(partialTick);
    }
}
