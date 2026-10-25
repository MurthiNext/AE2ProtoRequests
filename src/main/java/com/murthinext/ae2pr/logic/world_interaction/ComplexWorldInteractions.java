package com.murthinext.ae2pr.logic.world_interaction;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.recipe.ComplexWorldInteractionRecipe;
import com.murthinext.ae2pr.recipe.CountedIngredient;

/**
 * 服务端的复杂流体与爆炸世界交互。
 */
public final class ComplexWorldInteractions {

    private ComplexWorldInteractions() {
    }

    public static void onItemTick(ItemEntity item) {
        Level level = item.level();
        if (level.isClientSide || item.isRemoved() || item.getItem().isEmpty()
                || !(item instanceof WorldInteractionEntity interaction)) {
            return;
        }
        WorldInteractionProgress progress = interaction.ae2pr$getWorldInteractionProgress();
        BlockPos pos = fluidPosition(item);
        FluidState fluid = level.getFluidState(pos);
        progress.isProtectedInFluid(fluid);
        if (!fluid.isEmpty()) {
            List<ItemEntity> nearby = null;
            for (ComplexWorldInteractionRecipe recipe : level.getRecipeManager()
                    .getAllRecipesFor(ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get())) {
                if (!recipe.getCircumstance().matches(fluid) || !recipe.canTrigger(item.getItem())) {
                    continue;
                }
                if (nearby == null) {
                    nearby = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos),
                            entity -> entity.isAlive() && !entity.getItem().isEmpty()
                                    && fluidPosition(entity).equals(pos));
                }
                BlockState catalyst = level.getBlockState(pos);
                if (recipe.convertsFluid() && !(catalyst.getBlock() instanceof LiquidBlock)) {
                    continue;
                }
                int limit = recipe.convertsFluid() ? 1 : Integer.MAX_VALUE;
                if (catalyst.getBlock() instanceof WorldInteractionCatalyst limited) {
                    limit = Math.min(limit, limited.getRemainingInteractions(catalyst));
                }
                List<ItemEntity> inputs = nearby.stream().filter(entity -> recipe.canTrigger(entity.getItem())).toList();
                WorldInteractionInputPlan plan = plan(recipe, inputs, limit);
                if (plan.batches() <= 0) {
                    continue;
                }
                if (progress.advance(recipe, pos)) {
                    apply(level, pos, recipe, item, inputs, plan);
                    progress.reset();
                }
                return;
            }
        }
        progress.reset();
    }

    public static boolean protectFromFire(ItemEntity item) {
        Level level = item.level();
        if (level.isClientSide || item.isRemoved() || item.getItem().isEmpty()) {
            return false;
        }
        BlockPos pos = fluidPosition(item);
        FluidState fluid = level.getFluidState(pos);
        if (item instanceof WorldInteractionEntity interaction
                && interaction.ae2pr$getWorldInteractionProgress().isProtectedInFluid(fluid)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WorldInteractionCatalyst catalyst
                && catalyst.getRemainingInteractions(state) <= 0) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get()).stream()
                .anyMatch(recipe -> recipe.getCircumstance().matches(fluid) && recipe.canTrigger(item.getItem()));
    }

    public static boolean tryExplosion(ItemEntity item) {
        Level level = item.level();
        if (level.isClientSide || item.isRemoved() || item.getItem().isEmpty()) {
            return false;
        }
        if (item instanceof WorldInteractionEntity interaction
                && interaction.ae2pr$getWorldInteractionProgress().isProtectedFromExplosion(level.getGameTime())) {
            return true;
        }
        List<ItemEntity> nearby = null;
        for (ComplexWorldInteractionRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get())) {
            if (!recipe.getCircumstance().isExplosion() || !recipe.canTrigger(item.getItem())) {
                continue;
            }
            if (nearby == null) {
                nearby = level.getEntitiesOfClass(ItemEntity.class, new AABB(item.position(), item.position()).inflate(1),
                        entity -> entity.isAlive() && !entity.getItem().isEmpty());
            }
            List<ItemEntity> inputs = nearby.stream().filter(entity -> recipe.canTrigger(entity.getItem())).toList();
            WorldInteractionInputPlan plan = plan(recipe, inputs, Integer.MAX_VALUE);
            if (plan.batches() > 0) {
                apply(level, fluidPosition(item), recipe, item, inputs, plan);
                return true;
            }
        }
        return false;
    }

    private static BlockPos fluidPosition(ItemEntity item) {
        return BlockPos.containing(item.getX(), (item.getBoundingBox().minY + item.getBoundingBox().maxY) * 0.5D,
                item.getZ());
    }

    private static WorldInteractionInputPlan plan(ComplexWorldInteractionRecipe recipe, List<ItemEntity> nearby,
            int limit) {
        List<CountedIngredient> ingredients = recipe.getItemInputs();
        int[] available = new int[nearby.size()];
        int[] required = ingredients.stream().mapToInt(CountedIngredient::count).toArray();
        boolean[][] matches = new boolean[nearby.size()][ingredients.size()];
        for (int item = 0; item < nearby.size(); item++) {
            ItemStack stack = nearby.get(item).getItem();
            available[item] = stack.getCount();
            for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
                matches[item][ingredient] = ingredients.get(ingredient).test(stack);
            }
        }
        return WorldInteractionInputPlan.find(available, required, matches, limit);
    }

    private static void apply(Level level, BlockPos pos, ComplexWorldInteractionRecipe recipe, ItemEntity source,
            List<ItemEntity> nearby, WorldInteractionInputPlan plan) {
        for (int index = 0; index < nearby.size(); index++) {
            int consumed = plan.consumed()[index];
            if (consumed == 0) {
                continue;
            }
            ItemEntity entity = nearby.get(index);
            if (recipe.getCircumstance().isExplosion() && entity instanceof WorldInteractionEntity interaction) {
                interaction.ae2pr$getWorldInteractionProgress().protectFromExplosion(level.getGameTime());
            }
            ItemStack remaining = entity.getItem().copy();
            remaining.shrink(consumed);
            if (remaining.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(remaining);
            }
        }
        if (!recipe.getCircumstance().isExplosion()) {
            BlockState state = level.getBlockState(pos);
            if (recipe.convertsFluid()) {
                level.setBlockAndUpdate(pos, recipe.getResultFluid().defaultFluidState().createLegacyBlock());
                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
            } else if (state.getBlock() instanceof WorldInteractionCatalyst catalyst) {
                catalyst.consumeInteractions(level, pos, state, plan.batches());
            }
        }
        for (ItemStack result : recipe.getItemOutputs()) {
            long total = (long) result.getCount() * plan.batches();
            while (total > 0) {
                ItemStack stack = result.copyWithCount((int) Math.min(total, result.getMaxStackSize()));
                total -= stack.getCount();
                ItemEntity output = new ItemEntity(level, source.getX(), source.getY() + 0.25D, source.getZ(), stack);
                output.setDeltaMovement(level.random.nextDouble() * 0.25D - 0.125D,
                        level.random.nextDouble() * 0.25D - 0.125D, level.random.nextDouble() * 0.25D - 0.125D);
                if (output instanceof WorldInteractionEntity interaction) {
                    WorldInteractionProgress progress = interaction.ae2pr$getWorldInteractionProgress();
                    if (recipe.getCircumstance().isExplosion()) {
                        progress.protectFromExplosion(level.getGameTime());
                    } else {
                        progress.protectInFluid(level.getFluidState(pos).getType());
                    }
                }
                level.addFreshEntity(output);
            }
        }
    }
}
