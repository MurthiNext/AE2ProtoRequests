package com.murthinext.ae2pr.integration.jei.transfer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;

import com.murthinext.ae2pr.integration.jei.category.MultiblockPreviewCategory;
import com.murthinext.ae2pr.integration.jei.recipe.MultiblockPreviewRecipe;
import com.murthinext.ae2pr.integration.jei.widget.MultiblockPreviewWidget;

@SuppressWarnings("removal")
public class MultiblockPatternTransferHandler
        implements IRecipeTransferHandler<PatternEncodingTermMenu, MultiblockPreviewRecipe> {

    private static final String AUTHOR = "AE2 Proto Requests";

    @Override
    public Class<? extends PatternEncodingTermMenu> getContainerClass() {
        return PatternEncodingTermMenu.class;
    }

    @Override
    public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() {
        return Optional.of(PatternEncodingTermMenu.TYPE);
    }

    @Override
    public RecipeType<MultiblockPreviewRecipe> getRecipeType() {
        return MultiblockPreviewCategory.RECIPE_TYPE;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, MultiblockPreviewRecipe recipe,
            IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) {
            return null;
        }
        GenericStack output = GenericStack.fromItemStack(label(recipe));
        if (output == null) {
            return null;
        }
        List<List<GenericStack>> inputs = MultiblockPreviewWidget.materials(recipe).stream()
                .map(GenericStack::fromItemStack)
                .filter(Objects::nonNull)
                .map(List::of)
                .toList();
        EncodingHelper.encodeProcessingRecipe(menu, inputs, List.of(output));
        return null;
    }

    private static ItemStack label(MultiblockPreviewRecipe recipe) {
        CompoundTag tag = new CompoundTag();
        tag.putString("title", recipe.title().getString());
        tag.putString("author", AUTHOR);
        tag.putInt("generation", 0);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf("{\"text\":\"\"}"));
        tag.put("pages", pages);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.setTag(tag);
        return book;
    }
}
