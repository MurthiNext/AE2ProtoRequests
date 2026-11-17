package com.murthinext.ae2pr.integration.jei.transfer;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.murthinext.ae2pr.api.recipe.FluidInput;
import com.murthinext.ae2pr.api.recipe.ItemInput;
import com.murthinext.ae2pr.api.recipe.ItemOutput;
import com.murthinext.ae2pr.integration.jei.category.CrystalAssemblyLineCategory;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

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
        return CrystalAssemblyLineCategory.RECIPE_TYPE;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(
            PatternEncodingTermMenu menu,
            CrystalAssemblyLineRecipe recipe,
            IRecipeSlotsView slotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        if (!doTransfer) {
            return null;
        }
        EncodingHelper.encodeProcessingRecipe(menu, inputs(recipe), outputs(recipe));
        return null;
    }

    private static List<List<GenericStack>> inputs(CrystalAssemblyLineRecipe recipe) {
        List<List<GenericStack>> inputs = new ArrayList<>();
        for (ItemInput input : recipe.getItemInputs()) {
            if (input.catalyst()) {
                continue;
            }
            List<GenericStack> candidates = input.stacks().stream()
                    .map(GenericStack::fromItemStack)
                    .filter(Objects::nonNull)
                    .toList();
            if (!candidates.isEmpty()) {
                inputs.add(candidates);
            }
        }
        for (FluidInput fluid : recipe.getFluidInputs()) {
            if (fluid.catalyst()) {
                continue;
            }
            inputs.add(List.of(GenericStack.fromFluidStack(fluid.stack())));
        }
        return inputs;
    }

    private static List<GenericStack> outputs(CrystalAssemblyLineRecipe recipe) {
        return recipe.getItemOutputs().stream()
                .map(ItemOutput::stack)
                .map(GenericStack::fromItemStack)
                .filter(Objects::nonNull)
                .toList();
    }
}
