package com.murthinext.ae2pr.integration.jei.category;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.integration.jei.render.AnimatedTextureDrawable;

// 单一动画贴图 + 固定尺寸的机器分类，子类只描述槽位布局
public abstract class AbstractSimpleMachineCategory extends AbstractMachineCategory<MachineRecipe> {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 106;
    private static final int INFO_Y = 72;
    private static final int PROCESS_W = 60;
    private static final int PROCESS_H = 64;

    private final RecipeType<MachineRecipe> recipeType;
    private final String titleKey;
    private final IDrawable process;

    protected AbstractSimpleMachineCategory(IGuiHelper guiHelper, RecipeType<MachineRecipe> recipeType, ItemStack icon,
                                            String titleKey, String processTexture) {
        super(guiHelper, icon, WIDTH, HEIGHT);
        this.recipeType = recipeType;
        this.titleKey = titleKey;
        this.process = new AnimatedTextureDrawable(Ids.of(processTexture), PROCESS_W, PROCESS_H, 40, 50);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(titleKey);
    }

    @Override
    protected int infoY() {
        return INFO_Y;
    }

    @Override
    protected int infoStep() {
        return 11;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        process.draw(graphics, 30, 2);
    }
}
