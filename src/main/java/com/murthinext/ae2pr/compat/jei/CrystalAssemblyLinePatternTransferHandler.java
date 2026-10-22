package com.murthinext.ae2pr.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;

import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;

/**
 * 把水晶装配线配方写入处理样板，隔离模块需求。
 */
@SuppressWarnings("removal")
public class CrystalAssemblyLinePatternTransferHandler
        implements IRecipeTransferHandler<PatternEncodingTermMenu, CrystalAssemblyLineRecipe> {

    @Override
    public Class<? extends PatternEncodingTermMenu> getContainerClass() {
        return PatternEncodingTermMenu.class;
    }

    @Override
    public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() {
        return Optional.of(PatternEncodingTermMenu.TYPE);
    }

    @Override
    public RecipeType<CrystalAssemblyLineRecipe> getRecipeType() {
        return CrystalAssemblyLineJeiCategory.RECIPE_TYPE;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, CrystalAssemblyLineRecipe recipe,
            IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) {
            return null;
        }
        EncodingHelper.encodeProcessingRecipe(menu, inputs(recipe), outputs(recipe));
        return null;
    }

    private static List<List<GenericStack>> inputs(CrystalAssemblyLineRecipe recipe) {
        List<List<GenericStack>> inputs = new ArrayList<>();
        for (CountedIngredient input : recipe.getItemInputs()) {
            List<GenericStack> candidates = new ArrayList<>();
            for (ItemStack stack : input.ingredient().getItems()) {
                GenericStack candidate = GenericStack.fromItemStack(stack.copyWithCount(input.count()));
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
            if (!candidates.isEmpty()) {
                inputs.add(candidates);
            }
        }
        for (FluidStack fluid : recipe.getFluidInputs()) {
            inputs.add(List.of(GenericStack.fromFluidStack(fluid)));
        }
        return inputs;
    }

    private static List<GenericStack> outputs(CrystalAssemblyLineRecipe recipe) {
        return recipe.getItemOutputs().stream()
                .map(GenericStack::fromItemStack)
                .filter(Objects::nonNull)
                .toList();
    }
}
