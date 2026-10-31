package com.murthinext.ae2pr.integration.jei;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.runtime.IClickableIngredient;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.client.CustomIngredientSource;
import com.murthinext.ae2pr.client.GuiExtraAreas;
import com.murthinext.ae2pr.machine.MachineDefinition;
import com.murthinext.ae2pr.machine.ModeMachineMenu;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerScreen;

public class MachineGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    @Override
    public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> screen,
            double mouseX, double mouseY) {
        if (!(screen instanceof AbstractMultiblockControllerScreen<?> machineScreen)) {
            return List.of();
        }
        MachineDefinition<?, ?, ?> definition = machineScreen.getMenu().definition();
        Rect2i area = machineScreen.jobStatusArea();
        if (definition == ModMachines.FILTER_PRESS) {
            return List.of(IGuiClickableArea.createBasic(area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                    FilterPressJeiCategory.RECIPE_TYPE));
        }
        if (definition == ModMachines.JIG) {
            return List.of(IGuiClickableArea.createBasic(area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                    JigJeiCategory.RECIPE_TYPE));
        }
        if (definition == ModMachines.MULTI_FACTORY && machineScreen.getMenu() instanceof ModeMachineMenu<?, ?> modeMenu
                && modeMenu.getMode() instanceof MultiFactoryMode mode) {
            return List.of(IGuiClickableArea.createBasic(area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                    MultiFactoryJeiPlugin.recipeType(mode)));
        }
        return List.of();
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
            if (mouseX < bounds.getX() || mouseX >= bounds.getX() + bounds.getWidth()
                    || mouseY < bounds.getY() || mouseY >= bounds.getY() + bounds.getHeight()) {
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
}
