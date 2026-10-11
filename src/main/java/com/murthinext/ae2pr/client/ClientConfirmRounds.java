package com.murthinext.ae2pr.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 确认界面的重复轮数缓存，按菜单 containerId 匹配。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientConfirmRounds {
    private static int containerId = -1;
    private static int rounds = 1;

    private ClientConfirmRounds() {
    }

    public static void update(int menuContainerId, int value) {
        containerId = menuContainerId;
        rounds = Math.max(1, value);
    }

    public static int getFor(int menuContainerId) {
        return menuContainerId == containerId ? rounds : 1;
    }

    public static void clear() {
        containerId = -1;
        rounds = 1;
    }
}
