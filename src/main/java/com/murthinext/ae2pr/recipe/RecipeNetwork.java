package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/**
 * 配方网络包的通用读写。
 */
public final class RecipeNetwork {

    private RecipeNetwork() {
    }

    public static void writeDraft(FriendlyByteBuf buffer, RecipeDraft draft) {
        writeItemInputs(buffer, draft.itemInputs());
        writeFluidStacks(buffer, draft.fluidInputs());
        writeItemOutputs(buffer, draft.itemOutputs());
        writeFluidStacks(buffer, draft.fluidOutputs());
        buffer.writeVarInt(draft.duration());
        writeEnergy(buffer, draft.energy());
    }

    public static RecipeDraft readDraft(FriendlyByteBuf buffer) {
        return new RecipeDraft(readItemInputs(buffer), readFluidStacks(buffer), readItemOutputs(buffer),
                readFluidStacks(buffer), buffer.readVarInt(), readEnergy(buffer));
    }

    public static void writeItemInputs(FriendlyByteBuf buffer, List<CountedIngredient> inputs) {
        buffer.writeVarInt(inputs.size());
        for (CountedIngredient input : inputs) {
            input.ingredient().toNetwork(buffer);
            buffer.writeVarInt(input.count());
        }
    }

    public static List<CountedIngredient> readItemInputs(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<CountedIngredient> inputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            inputs.add(new CountedIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
        }
        return inputs;
    }

    public static void writeItemOutputs(FriendlyByteBuf buffer, List<ChancedItemStack> outputs) {
        buffer.writeVarInt(outputs.size());
        for (ChancedItemStack output : outputs) {
            ItemStack stack = output.stack();
            buffer.writeItem(stack.copyWithCount(1));
            buffer.writeVarInt(stack.getCount());
            buffer.writeFloat(output.chance());
        }
    }

    public static List<ChancedItemStack> readItemOutputs(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ChancedItemStack> outputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            stack.setCount(buffer.readVarInt());
            outputs.add(new ChancedItemStack(stack, buffer.readFloat()));
        }
        return outputs;
    }

    public static void writeItemStacks(FriendlyByteBuf buffer, List<ItemStack> stacks) {
        buffer.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            buffer.writeItem(stack.copyWithCount(1));
            buffer.writeVarInt(stack.getCount());
        }
    }

    public static List<ItemStack> readItemStacks(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            stack.setCount(buffer.readVarInt());
            stacks.add(stack);
        }
        return stacks;
    }

    public static void writeFluidStacks(FriendlyByteBuf buffer, List<FluidStack> stacks) {
        buffer.writeVarInt(stacks.size());
        for (FluidStack stack : stacks) {
            buffer.writeFluidStack(stack);
        }
    }

    public static List<FluidStack> readFluidStacks(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<FluidStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(buffer.readFluidStack());
        }
        return stacks;
    }

    public static void writeEnergy(FriendlyByteBuf buffer, @Nullable Double energy) {
        buffer.writeBoolean(energy != null);
        if (energy != null) {
            buffer.writeDouble(energy);
        }
    }

    @Nullable
    public static Double readEnergy(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readDouble() : null;
    }
}
