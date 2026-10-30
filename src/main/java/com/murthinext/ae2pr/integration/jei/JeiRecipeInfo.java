package com.murthinext.ae2pr.integration.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// JEI 配方信息行的统一构建：时间、能耗与未显示槽位的提示
public final class JeiRecipeInfo {

    private JeiRecipeInfo() {
    }

    public static Component duration(int ticks) {
        return Component.translatable("jei.ae2pr.recipe.duration", ticks);
    }

    public static Component energy(double energy) {
        return Component.translatable("jei.ae2pr.recipe.energy", JeiAmountFormat.exact((int) Math.round(energy)));
    }

    public static MutableComponent chance(float chance) {
        return Component.translatable("jei.ae2pr.recipe.chance", ChancedItemStackRenderer.percent(chance));
    }

    // 同时有输入与产物超出可见槽位
    public static Component moreIo(int hiddenInputs, int hiddenOutputs) {
        if (hiddenInputs > 0 && hiddenOutputs > 0) {
            return Component.translatable("jei.ae2pr.recipe.more_io", hiddenInputs, hiddenOutputs);
        }
        if (hiddenInputs > 0) {
            return moreInputs(hiddenInputs);
        }
        return hiddenOutputs > 0 ? moreOutputs(hiddenOutputs) : Component.empty();
    }

    public static Component moreInputs(int hidden) {
        return Component.translatable("jei.ae2pr.recipe.more_inputs", hidden);
    }

    public static Component moreOutputs(int hidden) {
        return Component.translatable("jei.ae2pr.recipe.more_outputs", hidden);
    }

    public static Component extraInputs() {
        return Component.translatable("jei.ae2pr.recipe.extra_inputs");
    }

    public static Component extraOutputs() {
        return Component.translatable("jei.ae2pr.recipe.extra_outputs");
    }

    // 结构预览分类的标题：方块名 + 模式名
    public static Component titleWithMode(Component block, Component mode) {
        return Component.translatable("jei.ae2pr.recipe.title_with_mode", block, mode);
    }
}
