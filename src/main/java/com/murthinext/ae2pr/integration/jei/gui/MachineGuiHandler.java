package com.murthinext.ae2pr.integration.jei.gui;

import com.murthinext.ae2pr.api.machine.MachineMenu;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.api.machine.client.CustomIngredientSource;
import com.murthinext.ae2pr.api.machine.client.GuiExtraAreas;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.integration.jei.plugin.AE2PRJeiPlugin;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class MachineGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    @Override
    public Collection<IGuiClickableArea> getGuiClickableAreas(
            AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!(screen instanceof AbstractMultiblockControllerScreen<?> machineScreen) || machineScreen.isDataPage()) {
            return List.of();
        }
        RecipeType<?> recipeType = recipeType(machineScreen.getMenu());
        if (recipeType == null) {
            return List.of();
        }
        Rect2i area = machineScreen.jobStatusArea();
        return List.of(
                IGuiClickableArea.createBasic(area.getX(), area.getY(), area.getWidth(), area.getHeight(), recipeType));
    }

    private static RecipeType<?> recipeType(AbstractMultiblockControllerMenu<?> menu) {
        if (!(menu.definition() instanceof MultiblockMachineDefinition<?, ?> definition)) {
            return null;
        }
        Mode mode = menu instanceof MachineMenu<?> machineMenu ? machineMenu.getMode() : null;
        return AE2PRJeiPlugin.recipeTypeFor(mode != null ? mode.recipeType() : definition.recipeType());
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
                return factory.createBuilder(ForgeTypes.FLUID_STACK, new FluidStack(fluid, 1))
                        .buildWithArea(bounds);
            }
        }
        return Optional.empty();
    }

    private static boolean contains(Rect2i bounds, double mouseX, double mouseY) {
        return mouseX >= bounds.getX()
                && mouseX < bounds.getX() + bounds.getWidth()
                && mouseY >= bounds.getY()
                && mouseY < bounds.getY() + bounds.getHeight();
    }
}
