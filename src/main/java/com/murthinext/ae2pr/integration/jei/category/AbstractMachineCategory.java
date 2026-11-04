package com.murthinext.ae2pr.integration.jei.category;

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
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;

import com.murthinext.ae2pr.api.recipe.ChancedItemStack;
import com.murthinext.ae2pr.api.recipe.CountedIngredient;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;
import com.murthinext.ae2pr.integration.jei.JeiAmountFormat;
import com.murthinext.ae2pr.integration.jei.JeiRecipeInfo;
import com.murthinext.ae2pr.integration.jei.render.ChancedItemStackRenderer;
import com.murthinext.ae2pr.integration.jei.render.FluidSlotBackground;
import com.murthinext.ae2pr.integration.jei.render.FluidStackAmountRenderer;
import com.murthinext.ae2pr.integration.jei.render.ItemStackAmountRenderer;

/**
 * 按槽位区域声明布局的机器配方分类：子类只给出区域与贴图，槽位填充与信息行统一处理。
 */
public abstract class AbstractMachineCategory<T extends MultiblockRecipe> implements IRecipeCategory<T> {

    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_HINT = 0x808080;
    private static final int INFO_STEP = 10;
    private static final int HINT_HEIGHT = 10;

    private final IDrawable icon;
    private final IDrawable fluidSlot;
    private final int width;
    private final int height;

    protected AbstractMachineCategory(IGuiHelper guiHelper, ItemStack iconStack, int width, int height) {
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.fluidSlot = FluidSlotBackground.create(guiHelper);
        this.width = width;
        this.height = height;
    }

    protected SlotArea itemInputs() {
        return SlotArea.empty();
    }

    protected SlotArea itemOutputs() {
        return SlotArea.empty();
    }

    protected SlotArea fluidInputs() {
        return SlotArea.empty();
    }

    protected SlotArea fluidOutputs() {
        return SlotArea.empty();
    }

    protected int infoX() {
        return 4;
    }

    protected abstract int infoY();

    protected int infoStep() {
        return INFO_STEP;
    }

    protected void drawProcess(GuiGraphics graphics) {
    }

    // 关闭后既不绘制也不再响应“另有产物未显示”的提示
    protected boolean showsHiddenHint() {
        return true;
    }

    protected void configureItemInputSlot(IRecipeSlotBuilder slot, int index, T recipe) {
    }

    protected void configureFluidInputSlot(IRecipeSlotBuilder slot, int index, T recipe) {
    }

    // 布局之外的附加槽位，例如装配线的模块要求栏
    protected void addExtraSlots(IRecipeLayoutBuilder builder, T recipe) {
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public ResourceLocation getRegistryName(T recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        List<CountedIngredient> ingredients = recipe.getItemInputs();
        SlotArea ingredientArea = itemInputs();
        for (int index = 0; index < Math.max(ingredientArea.count(), ingredients.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < ingredientArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                        ingredientArea.slotX(index), ingredientArea.slotY(index));
                if (ingredientArea.background()) {
                    slot.setStandardSlotBackground();
                }
                slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
                configureItemInputSlot(slot, index, recipe);
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
            }
            if (index < ingredients.size()) {
                CountedIngredient ingredient = ingredients.get(index);
                acceptor.addItemStacks(Arrays.stream(ingredient.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(ingredient.count())).toList());
            }
        }

        List<FluidStack> incoming = recipe.getFluidInputs();
        SlotArea incomingArea = fluidInputs();
        for (int index = 0; index < Math.max(incomingArea.count(), incoming.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < incomingArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                        incomingArea.slotX(index), incomingArea.slotY(index));
                if (incomingArea.background()) {
                    slot.setBackground(fluidSlot, -1, -1);
                }
                slot.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
                configureFluidInputSlot(slot, index, recipe);
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
            }
            if (index < incoming.size()) {
                acceptor.addIngredient(ForgeTypes.FLUID_STACK, incoming.get(index));
            }
        }

        List<ChancedItemStack> results = recipe.getChancedItemOutputs();
        SlotArea resultArea = itemOutputs();
        for (int index = 0; index < Math.max(resultArea.count(), results.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < resultArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT,
                        resultArea.slotX(index), resultArea.slotY(index)).setStandardSlotBackground();
                slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ChancedItemStackRenderer.of(results, index));
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
            }
            if (index < results.size()) {
                acceptor.addItemStack(results.get(index).stack());
            }
        }

        List<FluidStack> outgoing = recipe.getFluidOutputs();
        SlotArea outgoingArea = fluidOutputs();
        for (int index = 0; index < Math.max(outgoingArea.count(), outgoing.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < outgoingArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT,
                        outgoingArea.slotX(index), outgoingArea.slotY(index));
                if (outgoingArea.background()) {
                    slot.setBackground(fluidSlot, -1, -1);
                }
                slot.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
            }
            if (index < outgoing.size()) {
                acceptor.addIngredient(ForgeTypes.FLUID_STACK, outgoing.get(index));
            }
        }

        addExtraSlots(builder, recipe);
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        drawProcess(graphics);
        drawInfo(graphics, recipe);
    }

    // 默认三行：时长、能耗、超出槽位提示
    protected void drawInfo(GuiGraphics graphics, T recipe) {
        var font = Minecraft.getInstance().font;
        int x = infoX();
        int y = infoY();
        int step = infoStep();
        graphics.drawString(font, JeiRecipeInfo.duration(recipe.getDuration()), x, y, COLOR_TEXT, false);
        graphics.drawString(font, JeiRecipeInfo.energy(recipe.getEnergyPerParallel()), x, y + step,
                COLOR_TEXT, false);
        int hiddenInputs = hiddenItemInputs(recipe) + hiddenFluids(recipe.getFluidInputs(), fluidInputs());
        int hiddenOutputs = hiddenItemOutputs(recipe) + hiddenFluids(recipe.getFluidOutputs(), fluidOutputs());
        if (showsHiddenHint() && (hiddenInputs > 0 || hiddenOutputs > 0)) {
            graphics.drawString(font, JeiRecipeInfo.moreIo(hiddenInputs, hiddenOutputs), x, y + 2 * step,
                    COLOR_HINT, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (!showsHiddenHint()) {
            return;
        }
        int hintY = infoY() + 2 * infoStep();
        if (mouseX < infoX() || mouseX >= width - 4 || mouseY < hintY || mouseY >= hintY + HINT_HEIGHT) {
            return;
        }
        addHiddenRole(tooltip, recipe, true);
        addHiddenRole(tooltip, recipe, false);
    }

    private void addHiddenRole(ITooltipBuilder tooltip, T recipe, boolean input) {
        List<CountedIngredient> ingredients = recipe.getItemInputs();
        List<ChancedItemStack> results = recipe.getChancedItemOutputs();
        List<FluidStack> fluids = input ? recipe.getFluidInputs() : recipe.getFluidOutputs();
        SlotArea fluidArea = input ? fluidInputs() : fluidOutputs();
        int hiddenItems = input ? hiddenItemInputs(recipe) : hiddenItemOutputs(recipe);
        int hiddenFluids = hiddenFluids(fluids, fluidArea);
        if (hiddenItems <= 0 && hiddenFluids <= 0) {
            return;
        }
        tooltip.add(input ? JeiRecipeInfo.extraInputs() : JeiRecipeInfo.extraOutputs());
        if (input) {
            addHiddenItemInputs(tooltip, ingredients, hiddenItems);
        } else {
            addHiddenItemOutputs(tooltip, results, hiddenItems);
        }
        addHiddenFluids(tooltip, fluids, fluidArea);
    }

    private static void addHiddenItemInputs(ITooltipBuilder tooltip, List<CountedIngredient> ingredients, int hidden) {
        for (int index = ingredients.size() - hidden; index < ingredients.size(); index++) {
            ItemStack[] alternatives = ingredients.get(index).ingredient().getItems();
            if (alternatives.length == 0) {
                continue;
            }
            tooltip.add(itemLine(alternatives[0].copyWithCount(ingredients.get(index).count())));
        }
    }

    private static void addHiddenItemOutputs(ITooltipBuilder tooltip, List<ChancedItemStack> results, int hidden) {
        for (int index = results.size() - hidden; index < results.size(); index++) {
            ChancedItemStack result = results.get(index);
            tooltip.add(itemLine(result.stack()));
            if (!result.certain()) {
                tooltip.add(ChancedItemStackRenderer.chanceTooltip(result.chance()));
            }
        }
    }

    private static void addHiddenFluids(ITooltipBuilder tooltip, List<FluidStack> fluids, SlotArea area) {
        for (int index = area.count(); index < fluids.size(); index++) {
            FluidStack fluid = fluids.get(index);
            tooltip.add(Component.literal(JeiAmountFormat.exact(fluid.getAmount()) + " mB ")
                    .append(fluid.getDisplayName()));
        }
    }

    private static Component itemLine(ItemStack stack) {
        return Component.literal(JeiAmountFormat.exact(stack.getCount()) + " × ").append(stack.getHoverName());
    }

    private int hiddenItemInputs(T recipe) {
        return Math.max(0, recipe.getItemInputs().size() - itemInputs().count());
    }

    private int hiddenItemOutputs(T recipe) {
        return Math.max(0, recipe.getChancedItemOutputs().size() - itemOutputs().count());
    }

    private static int hiddenFluids(List<FluidStack> fluids, SlotArea area) {
        return Math.max(0, fluids.size() - area.count());
    }
}
