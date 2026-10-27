package com.murthinext.ae2pr.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.menu.AEBaseMenu;

// 暴露 AE2 菜单的 client action 发送方法（泛型参数按擦除后签名匹配）
@Mixin(value = AEBaseMenu.class, remap = false)
public interface AEBaseMenuInvoker {
    @Invoker("sendClientAction")
    void ae2pr$sendClientAction(String action, Object arg);
}
