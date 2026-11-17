package com.murthinext.ae2pr.machine.multi_factory;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import com.murthinext.ae2pr.api.recipe.FluidInput;
import com.murthinext.ae2pr.api.recipe.FluidOutput;
import com.murthinext.ae2pr.api.recipe.ItemInput;
import com.murthinext.ae2pr.api.recipe.ItemOutput;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

// 通式加工厂配方包装器：直接适配 AE2 原生压印与充能配方
public final class MultiFactoryRecipe implements MultiblockRecipe {

    private final ResourceLocation id;
    private final MultiFactoryMode mode;
    private final List<ItemInput> itemInputs;
    private final List<ItemOutput> itemOutputs;

    private MultiFactoryRecipe(
            ResourceLocation id, MultiFactoryMode mode, List<ItemInput> itemInputs, ItemStack output) {
        this.id = id;
        this.mode = mode;
        this.itemInputs = List.copyOf(itemInputs);
        this.itemOutputs = List.of(ItemOutput.certain(output));
    }

    // 不匹配当前模式或非 AE2 配方时返回 null
    @Nullable
    public static MultiFactoryRecipe wrap(@Nullable Recipe<?> recipe, MultiFactoryMode mode) {
        if (recipe instanceof InscriberRecipe inscriber) {
            if (mode != MultiFactoryMode.INSCRIBING) {
                return null;
            }
            List<ItemInput> inputs = new ArrayList<>(3);
            inputs.add(new ItemInput(inscriber.getMiddleInput(), 1, false));
            // PRESS 的模具随并行消耗，直接作为原料；INSCRIBE 仅需模具存在
            boolean press = inscriber.getProcessType() == InscriberProcessType.PRESS;
            addOptional(inputs, inscriber.getTopOptional(), press);
            addOptional(inputs, inscriber.getBottomOptional(), press);
            return new MultiFactoryRecipe(
                    inscriber.getId(), mode, inputs, inscriber.getResultItem().copy());
        }
        if (recipe instanceof ChargerRecipe charger) {
            if (mode != MultiFactoryMode.CHARGING) {
                return null;
            }
            return new MultiFactoryRecipe(
                    charger.getId(),
                    mode,
                    List.of(new ItemInput(charger.getIngredient(), 1, false)),
                    charger.getResultItem());
        }
        return null;
    }

    private static void addOptional(List<ItemInput> target, Ingredient ingredient, boolean press) {
        if (!ingredient.isEmpty()) {
            target.add(new ItemInput(ingredient, 1, !press));
        }
    }

    public MultiFactoryMode mode() {
        return mode;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<ItemInput> getItemInputs() {
        return itemInputs;
    }

    @Override
    public List<FluidInput> getFluidInputs() {
        return List.of();
    }

    @Override
    public List<ItemOutput> getItemOutputs() {
        return itemOutputs;
    }

    @Override
    public List<FluidOutput> getFluidOutputs() {
        return List.of();
    }

    @Override
    public int getDuration() {
        return mode.durationTicks();
    }

    @Override
    public double getEnergyPerParallel() {
        return mode.energyPerParallel();
    }
}
