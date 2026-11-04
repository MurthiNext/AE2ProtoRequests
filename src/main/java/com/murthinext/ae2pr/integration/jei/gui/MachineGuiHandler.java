package com.murthinext.ae2pr.integration.jei.gui;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IClickableIngredient;

import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.MachineMenu;
import com.murthinext.ae2pr.api.machine.client.CustomIngredientSource;
import com.murthinext.ae2pr.api.machine.client.GuiExtraAreas;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.integration.jei.category.FilterPressCategory;
import com.murthinext.ae2pr.integration.jei.category.JigCategory;
import com.murthinext.ae2pr.integration.jei.plugin.MultiFactoryPlugin;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.registry.ModMachines;

public class MachineGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    @Override
    public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> screen,
            double mouseX, double mouseY) {
        if (!(screen instanceof AbstractMultiblockControllerScreen<?> machineScreen)) {
            return List.of();
        }
        RecipeType<?> recipeType = recipeType(machineScreen.getMenu());
        if (recipeType == null) {
            return List.of();
        }
        Rect2i area = machineScreen.jobStatusArea();
        return List.of(IGuiClickableArea.createBasic(area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                recipeType));
    }

    private static RecipeType<?> recipeType(AbstractMultiblockControllerMenu<?> menu) {
        MachineDefinition<?, ?, ?> definition = menu.definition();
        if (definition == ModMachines.FILTER_PRESS) {
            return FilterPressCategory.RECIPE_TYPE;
        }
        if (definition == ModMachines.JIG) {
            return JigCategory.RECIPE_TYPE;
        }
        if (definition == ModMachines.MULTI_FACTORY && menu instanceof MachineMenu<?> machineMenu
                && machineMenu.getMode() instanceof MultiFactoryMode mode) {
            return MultiFactoryPlugin.recipeType(mode);
        }
        return null;
    }

    @Override
    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen) {
        return screen instanceof GuiExtraAreas areas ? areas.getGuiExtraAreas() : List.of();
    }

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            IClickableIngredientFactory factory, AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!(screen instanceof CustomIngredientSource source)) {
            return Optional.empty();
        }
        for (CustomIngredientSource.Area area : source.getCustomAreas()) {
            Rect2i bounds = area.bounds();
            if (!contains(bounds, mouseX, mouseY)) {
                continue;
            }
            ItemStack item = area.item();
            if (item != null && !item.isEmpty()) {
                return factory.createBuilder(item.copyWithCount(1)).buildWithArea(bounds);
            }
            FluidStack fluid = area.fluid();
            if (fluid != null && !fluid.isEmpty()) {
                // 数量固定为 1，避免配方查找被储量影响
                return factory.createBuilder(ForgeTypes.FLUID_STACK, new FluidStack(fluid, 1)).buildWithArea(bounds);
            }
        }
        return Optional.empty();
    }

    private static boolean contains(Rect2i bounds, double mouseX, double mouseY) {
        return mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
                && mouseY >= bounds.getY() && mouseY < bounds.getY() + bounds.getHeight();
    }
}
