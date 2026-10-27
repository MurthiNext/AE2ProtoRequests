package com.murthinext.ae2pr.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.client.Point;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;

// 暴露 CPU 命中的内部方法，用于 tooltip 扩展
@Mixin(value = CPUSelectionList.class, remap = false)
public interface CPUSelectionListAccessor {

    @Invoker("hitTestCpu")
    CraftingStatusMenu.CraftingCpuListEntry ae2pr$hitTestCpu(Point mousePos);
}
