package com.murthinext.ae2pr.compat.jei;

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

/**
 * 把多方块预览的所需方块写入处理样板。
 */
@SuppressWarnings("removal")
public class MultiblockPatternTransferHandler
        implements IRecipeTransferHandler<PatternEncodingTermMenu, MultiblockPreviewRecipe> {

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
        return MultiblockPreviewJeiCategory.RECIPE_TYPE;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, MultiblockPreviewRecipe recipe,
            IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) {
            return null;
        }
        List<List<GenericStack>> inputs = MultiblockPreviewWidget.materials(recipe).stream()
                .map(GenericStack::fromItemStack)
                .filter(Objects::nonNull)
                .map(List::of)
                .toList();
        GenericStack output = GenericStack.fromItemStack(label(recipe));
        if (output == null) {
            return null;
        }
        EncodingHelper.encodeProcessingRecipe(menu, inputs, List.of(output));
        return null;
    }

    private static ItemStack label(MultiblockPreviewRecipe recipe) {
        CompoundTag tag = new CompoundTag();
        tag.putString("title", recipe.title().getString());
        tag.putString("author", "AE2 Proto Requests");
        tag.putInt("generation", 0);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf("{\"text\":\"\"}"));
        tag.put("pages", pages);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.setTag(tag);
        return book;
    }
}
