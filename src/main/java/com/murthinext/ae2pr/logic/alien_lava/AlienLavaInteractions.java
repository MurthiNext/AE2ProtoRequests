package com.murthinext.ae2pr.logic.alien_lava;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.block.alien_lava.AlienLavaBlock;
import com.murthinext.ae2pr.fluid.AlienLavaFluid;
import com.murthinext.ae2pr.recipe.AlienLavaRecipe;
import com.murthinext.ae2pr.recipe.CountedIngredient;

/**
 * 异星熔岩世界交互逻辑（服务端）：
 * <ul>
 * <li>物品浸入催化剂源方块一段时间后，一次性并行执行所有可完成的配方份数，按份数消耗附近物品并产出；</li>
 * <li>以异星熔岩为催化剂的配方每完成一份，该源方块的转化次数 +1，达到上限后变回普通熔岩；</li>
 * <li>以普通熔岩为催化剂的配方完成后，源方块转化为异星熔岩。</li>
 * </ul>
 */
public final class AlienLavaInteractions {

    /** 浸入流体后首次转化所需的等待时间（tick） */
    private static final int TRANSFORM_DELAY_TICKS = 60;

    private AlienLavaInteractions() {
    }

    /** 由 ItemEntity 的 tick 钩子调用（处理异星熔岩中的转化）。 */
    public static void onItemTick(ItemEntity item) {
        Level level = item.level();
        if (level.isClientSide || item.isRemoved() || item.getItem().isEmpty()
                || !(item instanceof AlienLavaTransformTimer timer)) {
            return;
        }
        BlockPos pos = BlockPos.containing(item.getX(),
                (item.getBoundingBox().minY + item.getBoundingBox().maxY) * 0.5D, item.getZ());
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(FluidTags.LAVA)) {
            // 普通岩浆会先销毁物品，改由 hurt 钩子处理“陨石粉充能”配方
            timer.ae2pr$setTransformTicks(0);
            return;
        }
        if (!fluid.isSource()) {
            timer.ae2pr$setTransformTicks(0);
            return;
        }
        AlienLavaRecipe recipe = findRecipe(level, fluid, item.getItem());
        if (recipe == null) {
            timer.ae2pr$setTransformTicks(0);
            return;
        }
        int ticks = timer.ae2pr$getTransformTicks() + 1;
        if (ticks < TRANSFORM_DELAY_TICKS) {
            timer.ae2pr$setTransformTicks(ticks);
            return;
        }
        if (apply(level, pos, recipe, item)) {
            timer.ae2pr$setTransformTicks(0);
        }
    }

    /**
     * 由 ItemEntity 的受伤钩子调用：普通岩浆会在数 tick 内销毁物品，
     * 因此在伤害生效前检查“把岩浆源转化为异星熔岩”的配方是否满足，满足则取消本次伤害。
     */
    public static boolean tryChargeLava(ItemEntity item) {
        Level level = item.level();
        if (level.isClientSide || item.isRemoved() || item.getItem().isEmpty()) {
            return false;
        }
        BlockPos pos = BlockPos.containing(item.getX(),
                (item.getBoundingBox().minY + item.getBoundingBox().maxY) * 0.5D, item.getZ());
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isSource() || !fluid.is(FluidTags.LAVA)) {
            return false;
        }
        AlienLavaRecipe recipe = findRecipe(level, fluid, item.getItem());
        return recipe != null && recipe.convertsCatalyst() && apply(level, pos, recipe, item);
    }

    /** 找到以触发物品为首个原料、且催化剂匹配当前流体状态的配方。 */
    private static AlienLavaRecipe findRecipe(Level level, FluidState fluid, ItemStack trigger) {
        for (AlienLavaRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.ALIEN_LAVA_TYPE.get())) {
            if (recipe.isCatalyst(fluid) && recipe.canTrigger(trigger)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * 校验并消耗附近物品实体中的原料，按可并行份数一次性产出；
     * 异星熔岩每并行一份扣除一点转化次数，次数用尽或材料不足时按实际份数执行。
     */
    private static boolean apply(Level level, BlockPos pos, AlienLavaRecipe recipe, ItemEntity source) {
        List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.0D),
                entity -> !entity.isRemoved() && entity.isAlive());
        if (nearby.isEmpty()) {
            return false;
        }

        BlockState catalystState = level.getBlockState(pos);
        boolean alienLava = catalystState.getBlock() instanceof AlienLavaBlock;

        // 并行上限：流体转化一次只能转化一个源方块；异星熔岩受剩余转化次数限制
        int maxParallel = Integer.MAX_VALUE;
        if (recipe.convertsCatalyst()) {
            maxParallel = 1;
        }
        if (alienLava) {
            maxParallel = Math.min(maxParallel,
                    AlienLavaFluid.MAX_CONVERSIONS - catalystState.getValue(AlienLavaFluid.CONVERSIONS));
        }
        if (maxParallel <= 0) {
            return false;
        }

        int parallel = maxBatches(recipe, nearby, maxParallel);
        if (parallel <= 0) {
            return false;
        }
        Map<ItemEntity, Integer> plan = planConsumption(recipe, nearby, parallel);
        if (plan == null) {
            return false;
        }

        // 执行消耗
        for (Map.Entry<ItemEntity, Integer> entry : plan.entrySet()) {
            int consumed = entry.getKey().getItem().getCount() - entry.getValue();
            if (consumed > 0) {
                entry.getKey().getItem().shrink(consumed);
                if (entry.getKey().getItem().isEmpty()) {
                    entry.getKey().discard();
                }
            }
        }

        if (recipe.convertsCatalyst()) {
            // 催化剂源方块转化为结果流体
            level.setBlockAndUpdate(pos, recipe.getResultFluid().defaultFluidState().createLegacyBlock());
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
        } else {
            // 生成产物并累计该源方块的转化次数
            spawnResults(level, source, recipe.getResultItem(), parallel);
            if (alienLava) {
                int conversions = catalystState.getValue(AlienLavaFluid.CONVERSIONS) + parallel;
                if (conversions >= AlienLavaFluid.MAX_CONVERSIONS) {
                    // 标记为已用尽，由方块的计划刻在物品离开流体后恢复为普通熔岩
                    level.setBlockAndUpdate(pos, catalystState.setValue(AlienLavaFluid.CONVERSIONS,
                            AlienLavaFluid.MAX_CONVERSIONS));
                    level.scheduleTick(pos, catalystState.getBlock(), 20);
                } else {
                    level.setBlockAndUpdate(pos, catalystState.setValue(AlienLavaFluid.CONVERSIONS, conversions));
                }
            }
        }
        return true;
    }

    /**
     * 按现有原料估算可并行的份数：对每种原料先计算它单独允许的份数，
     * 再从可用数量中预留该原料最终份数所需的数量，保证返回的份数一定可执行。
     */
    private static int maxBatches(AlienLavaRecipe recipe, List<ItemEntity> nearby, int cap) {
        Map<ItemEntity, Integer> available = new IdentityHashMap<>();
        for (ItemEntity entity : nearby) {
            available.put(entity, entity.getItem().getCount());
        }
        int parallel = cap;
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            int matched = 0;
            for (Map.Entry<ItemEntity, Integer> entry : available.entrySet()) {
                if (entry.getValue() > 0 && ingredient.ingredient().test(entry.getKey().getItem())) {
                    matched += entry.getValue();
                }
            }
            parallel = Math.min(parallel, matched / ingredient.count());
            if (parallel <= 0) {
                return 0;
            }

            int reserve = ingredient.count() * parallel;
            for (Map.Entry<ItemEntity, Integer> entry : available.entrySet()) {
                if (reserve <= 0) {
                    break;
                }
                if (entry.getValue() > 0 && ingredient.ingredient().test(entry.getKey().getItem())) {
                    int take = Math.min(reserve, entry.getValue());
                    entry.setValue(entry.getValue() - take);
                    reserve -= take;
                }
            }
        }
        return parallel;
    }

    /**
     * 尝试为 {@code parallel} 份配方规划消耗，原料不足返回 {@code null}；
     * 返回值为各物品实体剩余数量（未做实际扣除）。
     */
    @Nullable
    private static Map<ItemEntity, Integer> planConsumption(AlienLavaRecipe recipe, List<ItemEntity> nearby,
            int parallel) {
        Map<ItemEntity, Integer> remaining = new IdentityHashMap<>();
        for (ItemEntity entity : nearby) {
            remaining.put(entity, entity.getItem().getCount());
        }
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            int need = ingredient.count() * parallel;
            for (Map.Entry<ItemEntity, Integer> entry : remaining.entrySet()) {
                if (need <= 0) {
                    break;
                }
                if (entry.getValue() > 0 && ingredient.ingredient().test(entry.getKey().getItem())) {
                    int take = Math.min(need, entry.getValue());
                    entry.setValue(entry.getValue() - take);
                    need -= take;
                }
            }
            if (need > 0) {
                return null;
            }
        }
        return remaining;
    }

    /** 在触发物品位置生成并行的产物，超过堆叠上限时拆成多份。 */
    private static void spawnResults(Level level, ItemEntity source, ItemStack result, int parallel) {
        int total = result.getCount() * parallel;
        RandomSource random = level.getRandom();
        while (total > 0) {
            ItemStack stack = result.copyWithCount(Math.min(total, result.getMaxStackSize()));
            total -= stack.getCount();
            ItemEntity output = new ItemEntity(level, source.getX(), source.getY() + 0.25D, source.getZ(), stack);
            output.setDeltaMovement(random.nextDouble() * 0.25D - 0.125D,
                    random.nextDouble() * 0.25D - 0.125D,
                    random.nextDouble() * 0.25D - 0.125D);
            level.addFreshEntity(output);
        }
    }
}
