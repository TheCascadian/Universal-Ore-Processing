package com.thecascadian.universaloreprocessing.block;

/** Why a machine is working or idle, synced to the screen so it can explain itself. */
public enum Status {
    DISABLED,
    NO_INPUT,
    REJECTED,
    OUTPUT_FULL,
    NEEDS_WATER,
    NEEDS_REAGENT,
    NEEDS_FUEL,
    NEEDS_POWER,
    WORKING;

    private static final Status[] VALUES = values();

    public static Status byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NO_INPUT;
    }
}
