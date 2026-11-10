/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * ME Requester（LGPL-3.0）兼容层，纳入本仓库 LGPL-3.0 声明范围。
 * See licenses/me-requester in this repository.
 */
package com.murthinext.ae2pr.requester.compat;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;

/**
 * 仅当安装了 ME Requester 时才应用本配置下的兼容 mixin，
 * 避免在未安装时加载引用了其类的 mixin 而导致 NoClassDefFoundError。
 */
public class MERequesterCompatPlugin implements IMixinConfigPlugin {

    private static final String TARGET_MOD_ID = "merequester";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var modList = ModList.get();
        if (modList != null) {
            return modList.isLoaded(TARGET_MOD_ID);
        }
        // mixin 的筛选早于 ModList 构建，此时只能查加载阶段的模组文件列表
        var loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getModFileById(TARGET_MOD_ID) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
