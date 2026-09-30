package com.thecascadian.universaloreprocessing.recipe;

import net.minecraft.util.RandomSource;

/** Applies the per-stage yield multipliers from the config to a base output count. */
public final class Yields {

    private static final RandomSource RANDOM = RandomSource.create();

    private Yields() {
    }

    /** The whole part of base * multiplier is guaranteed, the fractional part is the chance of one extra item. */
    public static int scale(int base, double multiplier) {
        double scaled = base * multiplier;
        int whole = (int) Math.floor(scaled);
        double fraction = scaled - whole;
        if (fraction > 0.0D && RANDOM.nextDouble() < fraction)
            whole++;
        return Math.max(1, whole);
    }

    /** True with the given probability; values at or above one always succeed, values at or below zero never do. */
    public static boolean chance(double probability) {
        if (probability >= 1.0D)
            return true;
        return probability > 0.0D && RANDOM.nextDouble() < probability;
    }
}
