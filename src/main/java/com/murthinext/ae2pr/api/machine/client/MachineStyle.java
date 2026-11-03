package com.murthinext.ae2pr.api.machine.client;

import net.minecraft.resources.ResourceLocation;

public interface MachineStyle {

    int ACCENT_CRYSTAL = 0xFF8ABBEF;
    int ACCENT_CRYSTAL_DARK = 0x80294057;
    int ACCENT_EMBER = 0xFFE6AF50;
    int ACCENT_EMBER_DARK = 0x808A5A20;

    ResourceLocation guidePage();

    int titleColor();

    int accent();

    int accentDark();

    // 模式按钮图标，按模式索引
    java.util.Map<com.murthinext.ae2pr.api.machine.Mode, MachineModeButton.Icon> modeIcons();
}
