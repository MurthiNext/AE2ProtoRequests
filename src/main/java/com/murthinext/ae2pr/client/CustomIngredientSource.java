package com.murthinext.ae2pr.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * 界面中手绘的非槽位物品与流体，供配方查看器识别。
 */
public interface CustomIngredientSource {

    List<Area> getCustomAreas();

    record Area(@Nullable ItemStack item, @Nullable FluidStack fluid, Rect2i bounds) {

        public static Area ofItem(ItemStack item, Rect2i bounds) {
            return new Area(item, null, bounds);
        }

        public static Area ofFluid(FluidStack fluid, Rect2i bounds) {
            return new Area(null, fluid, bounds);
        }

        public boolean isEmpty() {
            return (item == null || item.isEmpty()) && (fluid == null || fluid.isEmpty());
        }
    }
}
