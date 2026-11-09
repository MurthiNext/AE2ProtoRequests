package com.murthinext.ae2pr.api.machine.client;

import net.minecraft.resources.ResourceLocation;

public interface MachineStyle {

    ResourceLocation guidePage();

    int titleColor();

    int accent();

    int accentDark();

    // 模式按钮图标，按模式索引
    java.util.Map<com.murthinext.ae2pr.api.machine.Mode, MachineModeButton.Icon> modeIcons();
}
