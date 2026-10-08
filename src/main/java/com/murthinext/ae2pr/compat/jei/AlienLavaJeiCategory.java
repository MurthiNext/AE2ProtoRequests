package com.murthinext.ae2pr.compat.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModTags;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.fluid.AlienLavaFluid;
import com.murthinext.ae2pr.recipe.AlienLavaRecipe;
import com.murthinext.ae2pr.recipe.CountedIngredient;

/**
 * 异星熔岩世界交互配方的 JEI 分类。
 */
public class AlienLavaJeiCategory implements IRecipeCategory<AlienLavaRecipe> {

    public static final RecipeType<AlienLavaRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "alien_lava",
            AlienLavaRecipe.class);

    private static final int WIDTH = 130;
    private static final int HEIGHT = 74;
    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 22;
    private static final int ARROW_HEIGHT = 16;
    private static final int ARROW_Y = 23;
    private static final int CATALYST_X = 55;
    private static final int OUTPUT_X = 105;
    private static final int COLOR_TEXT = 0x404040;
    private static final ResourceLocation ARROW = new ResourceLocation(ae2pr.MODID,
            "textures/gui/jei/recipe_arrow.png");

    private final IDrawable icon;

    public AlienLavaJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.ALIEN_LAVA_BUCKET.get()));
    }

    @Override
    public RecipeType<AlienLavaRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ae2pr.alien_lava");
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
    public ResourceLocation getRegistryName(AlienLavaRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlienLavaRecipe recipe, IFocusGroup focuses) {
        // 原料：单列排布，超过 3 个换到第二列，与 AE2 的物质转化分类一致
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

        // 催化剂流体
        IRecipeSlotBuilder catalystSlot = builder.addSlot(RecipeIngredientRole.CATALYST, CATALYST_X + 1, ARROW_Y + 1);
        List<Fluid> catalysts = sourceFluids(recipe.getCatalyst());
        for (Fluid fluid : catalysts) {
            catalystSlot.addFluidStack(fluid, FluidType.BUCKET_VOLUME);
        }
        catalystSlot.setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidBlockIngredientRenderer());

        // 产物：物品或流体
        IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, ARROW_Y + 1);
        outputSlot.setStandardSlotBackground();
        if (recipe.convertsCatalyst() && recipe.getResultFluid() != null) {
            outputSlot.addFluidStack(recipe.getResultFluid(), FluidType.BUCKET_VOLUME)
                    .setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16);
        } else {
            outputSlot.addItemStack(recipe.getResultItem());
        }
    }

    @Override
    public void draw(AlienLavaRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
            double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.blit(ARROW, 25, ARROW_Y, 0, 0, ARROW_WIDTH, ARROW_HEIGHT);
        graphics.blit(ARROW, 76, ARROW_Y, 0, 0, ARROW_WIDTH, ARROW_HEIGHT);
        drawCentered(graphics, font, Component.translatable("jei.ae2pr.alien_lava.submerge"),
                CATALYST_X + SLOT / 2, 11);
        // 只有以异星熔岩为催化剂的配方才会消耗转化次数
        if (isAlienLava(recipe.getCatalyst())) {
            drawCentered(graphics, font,
                    Component.translatable("jei.ae2pr.alien_lava.uses", AlienLavaFluid.MAX_CONVERSIONS),
                    WIDTH / 2, HEIGHT - 11);
        }
    }

    /** 无阴影居中绘制文本，避免深色文字出现重影。 */
    private static void drawCentered(GuiGraphics graphics, Font font, Component text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, COLOR_TEXT, false);
    }

    /** 取标签中可作为源方块的流体用于展示。 */
    private static List<Fluid> sourceFluids(TagKey<Fluid> tag) {
        return BuiltInRegistries.FLUID.getTag(tag)
                .map(holders -> holders.stream()
                        .map(Holder::value)
                        .filter(fluid -> fluid.isSource(fluid.defaultFluidState()))
                        .toList())
                .orElse(List.of());
    }

    private static boolean isAlienLava(TagKey<Fluid> tag) {
        return ModTags.ALIEN_LAVA.location().equals(tag.location());
    }
}
