package com.murthinext.ae2pr.api.text;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

public final class AmountText {

    private static final long KILO = 1_000L;
    private static final long MEGA = 1_000_000L;
    private static final long GIGA = 1_000_000_000L;
    private static final long TERA = 1_000_000_000_000L;
    private static final long PETA = 1_000_000_000_000_000L;
    private static final long EXA = 1_000_000_000_000_000_000L;
    private static final long[] STEPS = {KILO, MEGA, GIGA, TERA, PETA, EXA};
    private static final String[] SUFFIXES = {"K", "M", "G", "T", "P", "E"};
    private static final long[] POWERS = {1L, 10L, 100L};

    private static final long BUCKET = 1_000L;
    private static final long KILOBUCKET = BUCKET * 1_000L;
    private static final long MEGABUCKET = KILOBUCKET * 1_000L;

    private static final char DECIMAL_SEPARATOR =
            DecimalFormatSymbols.getInstance().getDecimalSeparator();

    private static final ThreadLocal<NumberFormat> INTEGER_FORMAT =
            ThreadLocal.withInitial(NumberFormat::getIntegerInstance);
    private static final ThreadLocal<NumberFormat> NUMBER_FORMAT =
            ThreadLocal.withInitial(NumberFormat::getNumberInstance);
    private static final ThreadLocal<NumberFormat> PERCENT_FORMAT =
            ThreadLocal.withInitial(NumberFormat::getPercentInstance);
    private static final ThreadLocal<NumberFormat> STAT_FORMAT = ThreadLocal.withInitial(() -> {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(4);
        return format;
    });
    private static final ThreadLocal<DecimalFormat> SCIENTIFIC_FORMAT =
            ThreadLocal.withInitial(() -> new DecimalFormat("0.####E0"));

    private AmountText() {}

    public static boolean abbreviated(long value) {
        return value >= KILO;
    }

    public static String exact(long value) {
        return INTEGER_FORMAT.get().format(value);
    }

    public static String exact(double value, int maxDecimals) {
        NumberFormat format = NUMBER_FORMAT.get();
        format.setMaximumFractionDigits(Math.max(0, maxDecimals));
        return format.format(value);
    }

    public static String compact(long value, int maxChars) {
        if (value < KILO) {
            return Long.toString(value);
        }
        int unit = 0;
        while (unit + 1 < STEPS.length && value >= STEPS[unit + 1]) {
            unit++;
        }
        return scaled(value, STEPS[unit], SUFFIXES[unit], 1, maxChars);
    }

    public static String compactFluid(long milliBuckets, int maxChars) {
        if (milliBuckets < BUCKET) {
            return milliBuckets + "mB";
        }
        if (milliBuckets < KILOBUCKET) {
            return scaled(milliBuckets, BUCKET, "B", 2, maxChars);
        }
        if (milliBuckets < MEGABUCKET) {
            return scaled(milliBuckets, KILOBUCKET, "KB", 2, maxChars);
        }
        return scaled(milliBuckets, MEGABUCKET, "MB", 2, maxChars);
    }

    public static String percent(double ratio, int maxDecimals) {
        NumberFormat format = PERCENT_FORMAT.get();
        format.setMaximumFractionDigits(Math.max(0, maxDecimals));
        return format.format(ratio);
    }

    public static String stat(double value) {
        double magnitude = Math.abs(value);
        NumberFormat format = magnitude != 0 && (magnitude < 0.0001 || magnitude >= 1_000_000_000)
                ? SCIENTIFIC_FORMAT.get()
                : STAT_FORMAT.get();
        return format.format(value);
    }

    private static String scaled(long value, long step, String suffix, int maxDecimals, int maxChars) {
        long whole = value / step;
        long remainder = value % step;
        for (int decimals = Math.min(maxDecimals, POWERS.length - 1); decimals > 0; decimals--) {
            long divisor = POWERS[decimals];
            long fraction = remainder / (step / divisor);
            if (fraction == 0) {
                return whole + suffix;
            }
            String candidate = whole + String.valueOf(DECIMAL_SEPARATOR) + pad(fraction, decimals) + suffix;
            if (candidate.length() <= maxChars) {
                return candidate;
            }
        }
        return whole + suffix;
    }

    private static String pad(long value, int width) {
        String text = Long.toString(value);
        return text.length() >= width ? text : "0".repeat(width - text.length()) + text;
    }
}
