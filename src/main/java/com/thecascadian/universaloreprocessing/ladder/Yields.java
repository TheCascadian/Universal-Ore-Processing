package com.thecascadian.universaloreprocessing.ladder;

/**
 * Deterministic resolution of fractional yields. A stack is treated as an
 * accumulator over its item count: the n-th item of a stack yields the
 * difference between floor(ratio * n) and floor(ratio * (n - 1)), so a full
 * stack always produces exactly floor(ratio * count) with no random rolls.
 */
public final class Yields {

    // absorbs binary rounding of ratios such as 1.1 so whole products stay whole
    private static final double EPSILON = 1.0E-6D;

    private Yields() {
    }

    /** Total output for a whole stack of the given size. */
    public static int total(double ratio, int count) {
        if (count <= 0 || ratio <= 0.0D)
            return 0;
        return (int) Math.floor(ratio * count + EPSILON);
    }

    /** Output of the item consumed while the stack holds {@code count} items. */
    public static int forItem(double ratio, int count) {
        return total(ratio, count) - total(ratio, count - 1);
    }

    /** Formats a ratio for tooltips: 1, 1.25, 1.5, 2. */
    public static String format(double ratio) {
        String text = String.format(java.util.Locale.ROOT, "%.2f", ratio);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }
}
