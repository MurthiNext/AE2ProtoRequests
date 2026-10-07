package com.murthinext.ae2pr.compat.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.LavaSmelterRecipe;

/**
 * 熔岩冶炼炉配方的 JEI 分类：左侧原料、中间配方耐久来源（陨石粉）、右侧产物，
 * 底部标注加工时长与配方耐久消耗。
 */
public class LavaSmelterJeiCategory implements IRecipeCategory<LavaSmelterRecipe> {

    public static final RecipeType<LavaSmelterRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "lava_smelter",
            LavaSmelterRecipe.class);

    private static final int WIDTH = 130;
    private static final int HEIGHT = 84;
    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 22;
    private static final int ARROW_HEIGHT = 16;
    private static final int ARROW_Y = 23;
    private static final int CATALYST_X = 55;
    private static final int OUTPUT_X = 105;
    private static final int COLOR_TEXT = 0x404040;
    private static final ResourceLocation ARROW = new ResourceLocation(ae2pr.MODID,
            "textures/gui/jei/recipe_arrow.png");
    private static final ResourceLocation SKY_DUST_ID = new ResourceLocation("ae2", "sky_dust");

    private final IDrawable icon;

    public LavaSmelterJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()));
    }

    @Override
    public RecipeType<LavaSmelterRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.high_reactivity_lava_smelter");
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
    public ResourceLocation getRegistryName(LavaSmelterRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LavaSmelterRecipe recipe, IFocusGroup focuses) {
        // 原料：单列排布，超过 3 个换到第二列，与异星熔岩分类一致
        int x = 5;
        int y = 5;
        int index = 0;
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x + 1, y + 1);
            slot.setStandardSlotBackground();
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : ingredient.ingredient().getItems()) {
                stacks.add(stack.copyWithCount(ingredient.count()));
            }
            slot.addItemStacks(stacks);
            index++;
            y += SLOT;
            if (y >= 54 && index < recipe.getCountedIngredients().size()) {
                y -= 54;
                x += SLOT;
            }
        }

        // 配方耐久来源：陨石粉
        IRecipeSlotBuilder durabilitySlot = builder.addSlot(RecipeIngredientRole.CATALYST, CATALYST_X + 1,
                ARROW_Y + 1);
        durabilitySlot.setStandardSlotBackground();
        Item skyDust = ForgeRegistries.ITEMS.getValue(SKY_DUST_ID);
        if (skyDust != null) {
            durabilitySlot.addItemStack(new ItemStack(skyDust));
        }
        durabilitySlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(
                Component.translatable("jei.ae2pr.lava_smelter.dust",
                        LavaSmelterControllerBlockEntity.DURABILITY_PER_DUST)));

        // 产物
        IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, ARROW_Y + 1);
        outputSlot.setStandardSlotBackground();
        outputSlot.addItemStack(recipe.getResultItem());
    }

    @Override
    public void draw(LavaSmelterRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
            double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.blit(ARROW, 25, ARROW_Y, 0, 0, ARROW_WIDTH, ARROW_HEIGHT);
        graphics.blit(ARROW, 76, ARROW_Y, 0, 0, ARROW_WIDTH, ARROW_HEIGHT);
        drawCentered(graphics, font, Component.translatable("jei.ae2pr.lava_smelter.durability"),
                CATALYST_X + SLOT / 2, 11);
        drawCentered(graphics, font, Component.translatable("jei.ae2pr.lava_smelter.catalyst"),
                WIDTH / 2, HEIGHT - 21);
        drawCentered(graphics, font, Component.translatable("jei.ae2pr.lava_smelter.duration",
                recipe.getDuration()), WIDTH / 2, HEIGHT - 10);
    }

    /** 无阴影居中绘制文本，避免深色文字出现重影。 */
    private static void drawCentered(GuiGraphics graphics, Font font, Component text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, COLOR_TEXT, false);
    }
}
