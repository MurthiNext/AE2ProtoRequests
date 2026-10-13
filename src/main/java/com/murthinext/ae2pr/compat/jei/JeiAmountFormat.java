package com.murthinext.ae2pr.compat.jei;

import java.text.NumberFormat;

/**
 * JEI 配方界面的数量缩写与精确数量格式。
 */
public final class JeiAmountFormat {

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();
    private static final long KILO = 1_000L;
    private static final long MEGA = 1_000_000L;
    private static final long GIGA = 1_000_000_000L;

    private JeiAmountFormat() {
    }

    public static boolean abbreviated(int count) {
        return count >= KILO;
    }

    public static String item(int count) {
        if (count >= MEGA) {
            return oneDecimal(count, MEGA) + "M";
        }
        if (count >= KILO) {
            return oneDecimal(count, KILO) + "K";
        }
        return Integer.toString(count);
    }

    public static String fluid(int milliBuckets) {
        if (milliBuckets >= GIGA) {
            return twoDecimals(milliBuckets, GIGA) + "MB";
        }
        if (milliBuckets >= MEGA) {
            return twoDecimals(milliBuckets, MEGA) + "KB";
        }
        if (milliBuckets >= KILO) {
            return twoDecimals(milliBuckets, KILO) + "B";
        }
        return milliBuckets + "mB";
    }

    public static String exact(int value) {
        return NUMBER.format(value);
    }

    private static String oneDecimal(long value, long step) {
        long fraction = value % step / (step / 10);
        return fraction == 0 ? Long.toString(value / step) : value / step + "." + fraction;
    }

    private static String twoDecimals(long value, long step) {
        long fraction = value % step / (step / 100);
        if (fraction == 0) {
            return Long.toString(value / step);
        }
        if (fraction % 10 == 0) {
            return value / step + "." + fraction / 10;
        }
        return fraction < 10 ? value / step + ".0" + fraction : value / step + "." + fraction;
    }
}
