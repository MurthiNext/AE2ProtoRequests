/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester;

import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.platform.InputConstants;

import com.murthinext.ae2pr.ae2pr;

/**
 * 红石请求器相关工具方法。
 * <p>
 * 移植自 ME Requester（https://github.com/AlmostReliable/merequester，LGPL-3.0）的 {@code Utils}。
 */
public final class RequesterUtils {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{}");

    private RequesterUtils() {
    }

    public static ResourceLocation getRL(String path) {
        return new ResourceLocation(ae2pr.MODID, path);
    }

    public static String f(String input, Object... args) {
        for (var arg : args) {
            input = PLACEHOLDER.matcher(input).replaceFirst(arg.toString());
        }
        for (var i = 0; i < args.length; i++) {
            input = input.replace("{" + i + "}", args[i].toString());
        }
        return input;
    }

    public static int fillColorAlpha(ChatFormatting color) {
        // noinspection ConstantConditions
        return 0xFF << 3 * 8 | color.getColor();
    }

    public static MutableComponent translate(String type, String key, Object... args) {
        return Component.translatable(getTranslationKey(type, key), args);
    }

    public static String translateAsString(String type, String key) {
        return translate(type, key).getString();
    }

    public static void addShiftInfoTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal("» ").withStyle(ChatFormatting.AQUA)
                .append(translate(
                        "tooltip",
                        "shift_for_more",
                        InputConstants.getKey("key.keyboard.left.shift").getDisplayName())
                        .withStyle(ChatFormatting.GRAY)));
    }

    public static <T> T cast(Object o, Class<T> clazz) {
        return clazz.cast(o);
    }

    private static String getTranslationKey(String type, String key) {
        return f("{}.{}.{}", type, ae2pr.MODID, key);
    }
}
