package com.thecascadian.universaloreprocessing.ladder;

import java.util.Locale;

/**
 * The rungs of the processing ladder. Each form is made by exactly one verb
 * and leads to exactly one next verb; slurry is a cauldron state, not an item,
 * so it has no entry here.
 */
public enum Form {
    RAW(0, "strike"),
    CLUMPS(1, "grind"),
    DUST(2, "stir"),
    SHARDS(4, "none");

    private final int rung;
    private final String nextVerb;

    Form(int rung, String nextVerb) {
        this.rung = rung;
        this.nextVerb = nextVerb;
    }

    public int rung() {
        return rung;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Translation key of the tooltip line naming the next verb. */
    public String nextKey() {
        return "tooltip.universaloreprocessing.next." + nextVerb;
    }

    public boolean washable() {
        return this == CLUMPS || this == DUST;
    }
}
