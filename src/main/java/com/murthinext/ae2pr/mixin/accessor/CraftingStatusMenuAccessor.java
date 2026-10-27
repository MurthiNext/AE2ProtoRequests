package com.murthinext.ae2pr.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.google.common.collect.ImmutableSet;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingStatusMenu;

// 暴露 CPU 列表与序号分配，用于发送重复订单轮次状态
@Mixin(value = CraftingStatusMenu.class, remap = false)
public interface CraftingStatusMenuAccessor {

    @Accessor("lastCpuSet")
    ImmutableSet<ICraftingCPU> ae2pr$getLastCpuSet();

    @Invoker("getOrAssignCpuSerial")
    int ae2pr$getOrAssignCpuSerial(ICraftingCPU cpu);
}
