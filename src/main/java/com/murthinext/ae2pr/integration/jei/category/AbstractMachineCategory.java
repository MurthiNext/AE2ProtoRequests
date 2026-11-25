package com.murthinext.ae2pr.integration.jei.category;

import com.murthinext.ae2pr.api.recipe.ChancedOutput;
import com.murthinext.ae2pr.api.recipe.FluidInput;
import com.murthinext.ae2pr.api.recipe.FluidOutput;
import com.murthinext.ae2pr.api.recipe.ItemInput;
import com.murthinext.ae2pr.api.recipe.ItemOutput;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;
import com.murthinext.ae2pr.api.text.AmountText;
import com.murthinext.ae2pr.integration.jei.JeiRecipeInfo;
import com.murthinext.ae2pr.integration.jei.render.CatalystLabelDrawable;
import com.murthinext.ae2pr.integration.jei.render.ChancedRenderer;
import com.murthinext.ae2pr.integration.jei.render.FluidSlotBackground;
import com.murthinext.ae2pr.integration.jei.render.FluidStackAmountRenderer;
import com.murthinext.ae2pr.integration.jei.render.ItemStackAmountRenderer;
import java.util.List;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

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

    protected void drawProcess(GuiGraphics graphics) {}

    // 关闭后既不绘制也不再响应“另有产物未显示”的提示
    protected boolean showsHiddenHint() {
        return true;
    }

    protected void configureItemInputSlot(IRecipeSlotBuilder slot, int index, T recipe) {}

    protected void configureFluidInputSlot(IRecipeSlotBuilder slot, int index, T recipe) {}

    // 布局之外的附加槽位，例如装配线的模块要求栏
    protected void addExtraSlots(IRecipeLayoutBuilder builder, T recipe) {}

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
        List<ItemInput> itemInputs = recipe.getItemInputs();
        SlotArea itemInputArea = itemInputs();
        for (int index = 0; index < Math.max(itemInputArea.count(), itemInputs.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < itemInputArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(
                        RecipeIngredientRole.INPUT, itemInputArea.slotX(index), itemInputArea.slotY(index));
                if (itemInputArea.background()) {
                    slot.setStandardSlotBackground();
                }
                slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
                configureItemInputSlot(slot, index, recipe);
                if (index < itemInputs.size() && itemInputs.get(index).catalyst()) {
                    slot.setOverlay(CatalystLabelDrawable.INSTANCE, 0, 0);
                }
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
            }
            if (index < itemInputs.size()) {
                acceptor.addItemStacks(itemInputs.get(index).stacks());
            }
        }

        List<FluidInput> fluidInputs = recipe.getFluidInputs();
        SlotArea fluidInputArea = fluidInputs();
        for (int index = 0; index < Math.max(fluidInputArea.count(), fluidInputs.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < fluidInputArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(
                        RecipeIngredientRole.INPUT, fluidInputArea.slotX(index), fluidInputArea.slotY(index));
                if (fluidInputArea.background()) {
                    slot.setBackground(fluidSlot, -1, -1);
                }
                slot.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
                configureFluidInputSlot(slot, index, recipe);
                if (index < fluidInputs.size() && fluidInputs.get(index).catalyst()) {
                    slot.setOverlay(CatalystLabelDrawable.INSTANCE, 0, 0);
                }
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
            }
            if (index < fluidInputs.size()) {
                acceptor.addIngredient(
                        ForgeTypes.FLUID_STACK, fluidInputs.get(index).stack());
            }
        }

        List<ItemOutput> itemOutputs = recipe.getItemOutputs();
        SlotArea itemOutputArea = itemOutputs();
        for (int index = 0; index < Math.max(itemOutputArea.count(), itemOutputs.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < itemOutputArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(
                                RecipeIngredientRole.OUTPUT, itemOutputArea.slotX(index), itemOutputArea.slotY(index))
                        .setStandardSlotBackground();
                slot.setCustomRenderer(
                        VanillaTypes.ITEM_STACK,
                        new ChancedRenderer<>(ItemStackAmountRenderer.INSTANCE, chanceOf(itemOutputs, index)));
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
            }
            if (index < itemOutputs.size()) {
                acceptor.addItemStack(itemOutputs.get(index).stack());
            }
        }

        List<FluidOutput> fluidOutputs = recipe.getFluidOutputs();
        SlotArea fluidOutputArea = fluidOutputs();
        for (int index = 0; index < Math.max(fluidOutputArea.count(), fluidOutputs.size()); index++) {
            IIngredientAcceptor<?> acceptor;
            if (index < fluidOutputArea.count()) {
                IRecipeSlotBuilder slot = builder.addSlot(
                        RecipeIngredientRole.OUTPUT, fluidOutputArea.slotX(index), fluidOutputArea.slotY(index));
                if (fluidOutputArea.background()) {
                    slot.setBackground(fluidSlot, -1, -1);
                }
                slot.setCustomRenderer(
                        ForgeTypes.FLUID_STACK,
                        new ChancedRenderer<>(FluidStackAmountRenderer.INSTANCE, chanceOf(fluidOutputs, index)));
                acceptor = slot;
            } else {
                acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
            }
            if (index < fluidOutputs.size()) {
                acceptor.addIngredient(
                        ForgeTypes.FLUID_STACK, fluidOutputs.get(index).stack());
            }
        }

        addExtraSlots(builder, recipe);
    }

    private static float chanceOf(List<? extends ChancedOutput> outputs, int index) {
        return index < outputs.size() ? outputs.get(index).chance() : 1.0F;
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
        graphics.drawString(font, JeiRecipeInfo.energy(recipe.getEnergyPerParallel()), x, y + step, COLOR_TEXT, false);
        int hiddenInputs = hidden(recipe.getItemInputs().size(), itemInputs())
                + hidden(recipe.getFluidInputs().size(), fluidInputs());
        int hiddenOutputs = hidden(recipe.getItemOutputs().size(), itemOutputs())
                + hidden(recipe.getFluidOutputs().size(), fluidOutputs());
        if (showsHiddenHint() && (hiddenInputs > 0 || hiddenOutputs > 0)) {
            graphics.drawString(
                    font, JeiRecipeInfo.moreIo(hiddenInputs, hiddenOutputs), x, y + 2 * step, COLOR_HINT, false);
        }
    }

    private static int hidden(int size, SlotArea area) {
        return Math.max(0, size - area.count());
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
        addHiddenInputs(tooltip, recipe);
        addHiddenOutputs(tooltip, recipe);
    }

    private void addHiddenInputs(ITooltipBuilder tooltip, T recipe) {
        List<ItemInput> items = recipe.getItemInputs();
        List<FluidInput> fluids = recipe.getFluidInputs();
        SlotArea itemArea = itemInputs();
        SlotArea fluidArea = fluidInputs();
        int hiddenItems = hidden(items.size(), itemArea);
        int hiddenFluids = hidden(fluids.size(), fluidArea);
        if (hiddenItems <= 0 && hiddenFluids <= 0) {
            return;
        }
        tooltip.add(JeiRecipeInfo.extraInputs());
        for (int index = items.size() - hiddenItems; index < items.size(); index++) {
            List<ItemStack> candidates = items.get(index).stacks();
            if (!candidates.isEmpty()) {
                tooltip.add(itemLine(candidates.get(0)));
            }
        }
        for (int index = fluidArea.count(); index < fluids.size(); index++) {
            tooltip.add(fluidLine(fluids.get(index).stack()));
        }
    }

    private void addHiddenOutputs(ITooltipBuilder tooltip, T recipe) {
        List<ItemOutput> items = recipe.getItemOutputs();
        List<FluidOutput> fluids = recipe.getFluidOutputs();
        SlotArea itemArea = itemOutputs();
        SlotArea fluidArea = fluidOutputs();
        int hiddenItems = hidden(items.size(), itemArea);
        int hiddenFluids = hidden(fluids.size(), fluidArea);
        if (hiddenItems <= 0 && hiddenFluids <= 0) {
            return;
        }
        tooltip.add(JeiRecipeInfo.extraOutputs());
        for (int index = itemArea.count(); index < items.size(); index++) {
            ItemOutput output = items.get(index);
            tooltip.add(itemLine(output.stack()));
            if (!output.certain()) {
                tooltip.add(ChancedRenderer.chanceTooltip(output.chance()));
            }
        }
        for (int index = fluidArea.count(); index < fluids.size(); index++) {
            FluidOutput output = fluids.get(index);
            tooltip.add(fluidLine(output.stack()));
            if (!output.certain()) {
                tooltip.add(ChancedRenderer.chanceTooltip(output.chance()));
            }
        }
    }

    private static Component fluidLine(FluidStack stack) {
        return Component.translatable(
                "gui.ae2pr.amount.count_named",
                stack.getDisplayName(),
                Component.translatable("gui.ae2pr.unit.mb", AmountText.exact(stack.getAmount())));
    }

    private static Component itemLine(ItemStack stack) {
        return Component.translatable(
                "gui.ae2pr.amount.count_named", stack.getHoverName(), AmountText.exact(stack.getCount()));
    }
}
