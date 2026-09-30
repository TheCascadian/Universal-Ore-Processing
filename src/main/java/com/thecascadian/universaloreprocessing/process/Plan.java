package com.thecascadian.universaloreprocessing.process;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What one machine operation will do if it completes: the stacks it returns
 * (either may be empty) and what it draws from the tank and the reagent slot.
 */
public record Plan(ItemStack primary, ItemStack secondary, int water, @Nullable Reagent reagent, int reagentCount,
        double reagentUse) {

    public static Plan of(ItemStack primary) {
        return new Plan(primary, ItemStack.EMPTY, 0, null, 0, 1.0D);
    }

    public static Plan of(ItemStack primary, int water) {
        return new Plan(primary, ItemStack.EMPTY, water, null, 0, 1.0D);
    }
}
