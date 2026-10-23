package com.murthinext.ae2pr.integration.jei;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.recipe.JigRecipe;

public class JigJeiCategory implements IRecipeCategory<JigRecipe> {

    public static final RecipeType<JigRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "jig", JigRecipe.class);

    private static final int WIDTH = 160;
    private static final int HEIGHT = 106;
    private static final int ITEM_INPUT_SLOTS = 1;
    private static final int FLUID_INPUT_SLOTS = 1;
    private static final int ITEM_OUTPUT_SLOTS = 4;
    private static final int FLUID_OUTPUT_SLOTS = 2;
    private static final int INFO_Y = 72;
    private static final int PROCESS_X = 30;
    private static final int PROCESS_Y = 2;
    private static final int PROCESS_W = 60;
    private static final int PROCESS_H = 64;
    private static final NumberFormat NUMBER = NumberFormat.getNumberInstance();
    private static final ResourceLocation PROCESS_TEXTURE = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/gui/jei/jig_process.png");

    private final IDrawable icon;
    private final IDrawable fluidSlot;
    private final IDrawable process;

    public JigJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.JIG.get()));
        fluidSlot = FluidSlotBackground.create(guiHelper);
        process = new AnimatedTextureDrawable(PROCESS_TEXTURE, PROCESS_W, PROCESS_H, 40, 50);
    }

    @Override
    public RecipeType<JigRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.jig");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, JigRecipe recipe, IFocusGroup focuses) {
        for (RecipeIngredientRole role : List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.OUTPUT)) {
            boolean input = role == RecipeIngredientRole.INPUT;
            int itemSlots = input ? ITEM_INPUT_SLOTS : ITEM_OUTPUT_SLOTS;
            int itemColumns = input ? 1 : 2;
            int itemX = input ? 4 : 100;
            int itemY = 17;
            int itemCount = input ? recipe.getItemInputs().size() : recipe.getItemOutputs().size();
            for (int index = 0; index < Math.max(itemSlots, itemCount); index++) {
                IIngredientAcceptor<?> acceptor;
                if (index < itemSlots) {
                    var slot = builder.addSlot(role, itemX + index % itemColumns * 18,
                            itemY + index / itemColumns * 18)
                            .setStandardSlotBackground();
                    slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
                    acceptor = slot;
                } else {
                    acceptor = builder.addInvisibleIngredients(role);
                }
                if (index < itemCount) {
                    if (input) {
                        var ingredient = recipe.getItemInputs().get(index);
                        acceptor.addItemStacks(Arrays.stream(ingredient.ingredient().getItems())
                                .map(stack -> stack.copyWithCount(ingredient.count())).toList());
                    } else {
                        acceptor.addItemStack(recipe.getItemOutputs().get(index));
                    }
                }
            }
            List<FluidStack> fluids = input ? recipe.getFluidInputs() : recipe.getFluidOutputs();
            int fluidSlots = input ? FLUID_INPUT_SLOTS : FLUID_OUTPUT_SLOTS;
            int fluidX = input ? itemX : itemX + itemColumns * 18;
            int fluidY = input ? itemY + 18 : itemY;
            for (int index = 0; index < Math.max(fluidSlots, fluids.size()); index++) {
                IIngredientAcceptor<?> acceptor;
                if (index < fluidSlots) {
                    var slot = builder.addSlot(role, fluidX, fluidY + index * 18)
                            .setBackground(fluidSlot, -1, -1);
                    slot.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
                    acceptor = slot;
                } else {
                    acceptor = builder.addInvisibleIngredients(role);
                }
                if (index < fluids.size()) {
                    acceptor.addIngredient(ForgeTypes.FLUID_STACK, fluids.get(index));
                }
            }
        }
    }

    @Override
    public void draw(JigRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        process.draw(graphics, PROCESS_X, PROCESS_Y);
        graphics.drawString(font, Component.translatable("jei.ae2pr.jig.duration", NUMBER.format(recipe.getDuration())),
                4, INFO_Y, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.ae2pr.jig.energy", NUMBER.format(recipe.getEnergyPerParallel())),
                4, INFO_Y + 11, 0x404040, false);
        int hiddenInputs = Math.max(0, recipe.getItemInputs().size() - ITEM_INPUT_SLOTS)
                + Math.max(0, recipe.getFluidInputs().size() - FLUID_INPUT_SLOTS);
        int hiddenOutputs = Math.max(0, recipe.getItemOutputs().size() - ITEM_OUTPUT_SLOTS)
                + Math.max(0, recipe.getFluidOutputs().size() - FLUID_OUTPUT_SLOTS);
        if (hiddenInputs > 0 || hiddenOutputs > 0) {
            graphics.drawString(font, Component.translatable("jei.ae2pr.jig.more_io", hiddenInputs, hiddenOutputs),
                    4, INFO_Y + 22, 0x808080, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, JigRecipe recipe, IRecipeSlotsView slots,
            double mouseX, double mouseY) {
        if (mouseX >= 4 && mouseX < WIDTH - 4 && mouseY >= INFO_Y + 22 && mouseY < INFO_Y + 32) {
            for (RecipeIngredientRole role : List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.OUTPUT)) {
                boolean input = role == RecipeIngredientRole.INPUT;
                int itemSlots = input ? ITEM_INPUT_SLOTS : ITEM_OUTPUT_SLOTS;
                int fluidSlots = input ? FLUID_INPUT_SLOTS : FLUID_OUTPUT_SLOTS;
                int itemCount = input ? recipe.getItemInputs().size() : recipe.getItemOutputs().size();
                List<FluidStack> fluids = input ? recipe.getFluidInputs() : recipe.getFluidOutputs();
                if (itemCount <= itemSlots && fluids.size() <= fluidSlots) {
                    continue;
                }
                tooltip.add(Component.translatable(input ? "jei.ae2pr.jig.extra_inputs" : "jei.ae2pr.jig.extra_outputs"));
                for (int index = itemSlots; index < itemCount; index++) {
                    ItemStack stack;
                    if (input) {
                        var ingredient = recipe.getItemInputs().get(index);
                        ItemStack[] alternatives = ingredient.ingredient().getItems();
                        if (alternatives.length == 0) {
                            continue;
                        }
                        stack = alternatives[0].copyWithCount(ingredient.count());
                    } else {
                        stack = recipe.getItemOutputs().get(index);
                    }
                    tooltip.add(Component.literal(NUMBER.format(stack.getCount()) + " × ").append(stack.getHoverName()));
                }
                for (int index = fluidSlots; index < fluids.size(); index++) {
                    FluidStack fluid = fluids.get(index);
                    tooltip.add(Component.literal(NUMBER.format(fluid.getAmount()) + " mB ").append(fluid.getDisplayName()));
                }
            }
        }
    }
}
