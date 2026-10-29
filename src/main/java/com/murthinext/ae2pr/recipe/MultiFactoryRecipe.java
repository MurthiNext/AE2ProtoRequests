package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fluids.FluidStack;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryMode;

// 通式加工厂配方包装器：直接适配 AE2 原生压印与充能配方
public final class MultiFactoryRecipe implements MultiblockRecipe {

    private final ResourceLocation id;
    private final MultiFactoryMode mode;
    private final List<CountedIngredient> itemInputs;
    private final List<CountedIngredient> itemCatalysts;
    private final List<ItemStack> itemOutputs;
    private final List<ChancedItemStack> chancedItemOutputs;

    private MultiFactoryRecipe(ResourceLocation id, MultiFactoryMode mode, List<CountedIngredient> itemInputs,
            List<CountedIngredient> itemCatalysts, ItemStack output) {
        this.id = id;
        this.mode = mode;
        this.itemInputs = List.copyOf(itemInputs);
        this.itemCatalysts = List.copyOf(itemCatalysts);
        this.itemOutputs = List.of(output);
        this.chancedItemOutputs = List.of(ChancedItemStack.certain(output));
    }

    // 不匹配当前模式或非 AE2 配方时返回 null
    @Nullable
    public static MultiFactoryRecipe wrap(@Nullable Recipe<?> recipe, MultiFactoryMode mode) {
        if (recipe instanceof InscriberRecipe inscriber) {
            if (mode != MultiFactoryMode.INSCRIBING) {
                return null;
            }
            List<CountedIngredient> inputs = new ArrayList<>(3);
            inputs.add(new CountedIngredient(inscriber.getMiddleInput(), 1));
            List<CountedIngredient> catalysts = new ArrayList<>(2);
            // PRESS 的模具随并行消耗，直接作为原料；INSCRIBE 仅需模具存在
            boolean press = inscriber.getProcessType() == InscriberProcessType.PRESS;
            addOptional(press ? inputs : catalysts, inscriber.getTopOptional());
            addOptional(press ? inputs : catalysts, inscriber.getBottomOptional());
            return new MultiFactoryRecipe(inscriber.getId(), mode, inputs, catalysts,
                    inscriber.getResultItem().copy());
        }
        if (recipe instanceof ChargerRecipe charger) {
            if (mode != MultiFactoryMode.CHARGING) {
                return null;
            }
            return new MultiFactoryRecipe(charger.getId(), mode,
                    List.of(new CountedIngredient(charger.getIngredient(), 1)), List.of(),
                    charger.getResultItem());
        }
        return null;
    }

    private static void addOptional(List<CountedIngredient> target, Ingredient ingredient) {
        if (!ingredient.isEmpty()) {
            target.add(new CountedIngredient(ingredient, 1));
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
    public List<CountedIngredient> getItemInputs() {
        return itemInputs;
    }

    @Override
    public List<CountedIngredient> getItemCatalysts() {
        return itemCatalysts;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return List.of();
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    @Override
    public List<ChancedItemStack> getChancedItemOutputs() {
        return chancedItemOutputs;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
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
