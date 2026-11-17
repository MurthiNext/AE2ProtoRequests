package com.murthinext.ae2pr.api.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class RecipeNetwork {

    private RecipeNetwork() {}

    public static void writeDraft(FriendlyByteBuf buffer, RecipeDraft draft) {
        writeItemInputs(buffer, draft.itemInputs());
        writeFluidInputs(buffer, draft.fluidInputs());
        writeItemOutputs(buffer, draft.itemOutputs());
        writeFluidOutputs(buffer, draft.fluidOutputs());
        buffer.writeVarInt(draft.duration());
        writeEnergy(buffer, draft.energy());
    }

    public static RecipeDraft readDraft(FriendlyByteBuf buffer) {
        return new RecipeDraft(
                readItemInputs(buffer),
                readFluidInputs(buffer),
                readItemOutputs(buffer),
                readFluidOutputs(buffer),
                buffer.readVarInt(),
                readEnergy(buffer));
    }

    public static void writeItemInputs(FriendlyByteBuf buffer, List<ItemInput> inputs) {
        buffer.writeVarInt(inputs.size());
        for (ItemInput input : inputs) {
            input.ingredient().toNetwork(buffer);
            buffer.writeVarInt(input.count());
            buffer.writeBoolean(input.catalyst());
        }
    }

    public static List<ItemInput> readItemInputs(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ItemInput> inputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int count = buffer.readVarInt();
            inputs.add(new ItemInput(ingredient, count, buffer.readBoolean()));
        }
        return inputs;
    }

    public static void writeFluidInputs(FriendlyByteBuf buffer, List<FluidInput> inputs) {
        buffer.writeVarInt(inputs.size());
        for (FluidInput input : inputs) {
            buffer.writeFluidStack(input.stack());
            buffer.writeBoolean(input.catalyst());
        }
    }

    public static List<FluidInput> readFluidInputs(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<FluidInput> inputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            inputs.add(new FluidInput(buffer.readFluidStack(), buffer.readBoolean()));
        }
        return inputs;
    }

    public static void writeItemOutputs(FriendlyByteBuf buffer, List<ItemOutput> outputs) {
        buffer.writeVarInt(outputs.size());
        for (ItemOutput output : outputs) {
            ItemStack stack = output.stack();
            buffer.writeItem(stack.copyWithCount(1));
            buffer.writeVarInt(stack.getCount());
            buffer.writeFloat(output.chance());
        }
    }

    public static List<ItemOutput> readItemOutputs(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ItemOutput> outputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            stack.setCount(buffer.readVarInt());
            outputs.add(new ItemOutput(stack, buffer.readFloat()));
        }
        return outputs;
    }

    public static void writeFluidOutputs(FriendlyByteBuf buffer, List<FluidOutput> outputs) {
        buffer.writeVarInt(outputs.size());
        for (FluidOutput output : outputs) {
            buffer.writeFluidStack(output.stack());
            buffer.writeFloat(output.chance());
        }
    }

    public static List<FluidOutput> readFluidOutputs(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<FluidOutput> outputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            FluidStack stack = buffer.readFluidStack();
            outputs.add(new FluidOutput(stack, buffer.readFloat()));
        }
        return outputs;
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
