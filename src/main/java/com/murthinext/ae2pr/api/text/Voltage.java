package com.murthinext.ae2pr.api.text;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 电压字样统一格式：颜色表、字样组件与语言键注入。
 * <p>
 * 名称类文本会被同步到客户端，只能用逐字符着色的纯原版组件；
 * 物品 Tooltip 由客户端自行渲染，才可以用 {@link #circuit(int)} 拿到动态渐变。
 * 语言键最后一段以电压名开头即可自动注入，如 {@code block.ae2pr.aev_input_bus}。
 */
public enum Voltage {

    AEV(0x00FFFF),
    XPV(0xB8E4FF, 0x6FA2F2, 0x22409E);

    private static final float SPEED = 0.5F;
    private static final int DOWNGRADE_COLOR = 0x8A8A8A;
    private static final int XPV_ACCENT = 0x8FC4FF;

    private final int[] colors;

    Voltage(int... colors) {
        this.colors = colors;
    }

    public int[] colors() {
        return colors.clone();
    }

    public int accent() {
        return this == XPV ? XPV_ACCENT : colors[0];
    }

    // 逐字符写入颜色，可安全序列化
    public MutableComponent label() {
        MutableComponent label = Component.empty();
        String name = name();
        int count = Math.max(name.length() - 1, 1);
        for (int i = 0; i < name.length(); i++) {
            int color = GradientText.mix(colors, (float) i / count);
            label.append(Component.literal(String.valueOf(name.charAt(i))).withStyle(style -> style.withColor(color)));
        }
        return label;
    }

    // 电路 Tooltip 行
    public MutableComponent circuit(int downgrade) {
        MutableComponent label = Component.empty()
                .append(MutableComponent.create(GradientText.gradient(name(), SPEED, colors)));
        if (downgrade > 0) {
            label.append(Component.literal("-" + downgrade).withStyle(style -> style.withColor(DOWNGRADE_COLOR)));
        }
        int accent = accent();
        return label.append(Component.translatable("tooltip.ae2pr.circuit.suffix")
                .withStyle(style -> style.withColor(accent)));
    }

    public static MutableComponent name(String descriptionId) {
        Voltage voltage = of(descriptionId);
        return voltage == null ? Component.translatable(descriptionId)
                : Component.translatable(descriptionId, voltage.label());
    }

    public static MutableComponent circuit(String tier) {
        Voltage voltage = of(tier);
        if (voltage == null) {
            return Component.translatable("tooltip.ae2pr.circuit.suffix");
        }
        int separator = tier.indexOf('_');
        return voltage.circuit(separator < 0 ? 0 : Integer.parseInt(tier.substring(separator + 2)));
    }

    @Nullable
    public static Voltage of(String key) {
        String path = key.substring(key.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        for (Voltage voltage : values()) {
            String name = voltage.name().toLowerCase(Locale.ROOT);
            if (path.equals(name) || path.startsWith(name + "_")) {
                return voltage;
            }
        }
        return null;
    }
}
