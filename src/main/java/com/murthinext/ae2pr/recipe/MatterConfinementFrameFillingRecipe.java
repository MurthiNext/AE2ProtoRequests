package com.murthinext.ae2pr.recipe;

import appeng.api.util.AEColor;
import appeng.items.misc.PaintBallItem;
import com.murthinext.ae2pr.item.matter_confinement_frame.MatterConfinementFrameItem;
import com.murthinext.ae2pr.registry.ModItems;
import com.murthinext.ae2pr.registry.ModRecipes;
import java.util.EnumMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class MatterConfinementFrameFillingRecipe extends CustomRecipe {

    public MatterConfinementFrameFillingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !fill(container).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return fill(container);
    }

    private ItemStack fill(CraftingContainer container) {
        ItemStack frame = ItemStack.EMPTY;
        var additions = new EnumMap<AEColor, Integer>(AEColor.class);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.MATTER_CONFINEMENT_FRAME.get())) {
                if (!frame.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                frame = stack;
            } else if (stack.getItem() instanceof PaintBallItem ball
                    && ball.isLumen()
                    && AEColor.VALID_COLORS.contains(ball.getColor())) {
                additions.merge(ball.getColor(), 1, Integer::sum);
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (frame.isEmpty() || additions.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var contents = new EnumMap<AEColor, Integer>(AEColor.class);
        contents.putAll(MatterConfinementFrameItem.getContents(frame));
        for (var entry : additions.entrySet()) {
            int current = contents.getOrDefault(entry.getKey(), 0);
            if (current > Integer.MAX_VALUE - entry.getValue()) {
                return ItemStack.EMPTY;
            }
            contents.put(entry.getKey(), current + entry.getValue());
        }
        var result = frame.copyWithCount(1);
        MatterConfinementFrameItem.setContents(result, contents);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MATTER_CONFINEMENT_FRAME_FILLING.get();
    }
}
