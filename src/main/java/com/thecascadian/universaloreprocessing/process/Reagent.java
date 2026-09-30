package com.thecascadian.universaloreprocessing.process;

import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.world.item.Item;

/**
 * Consumable support items that the refining stations take in the reagent slot
 * (or, for coke, burn as fuel). They are plain items with no material.
 */
public enum Reagent {
    DENSE_MEDIUM("dense_medium"),
    COLLECTOR("flotation_collector"),
    FLUX("smelting_flux"),
    COKE("coke"),
    ACID("leach_acid"),
    SOLVENT("organic_solvent"),
    PRECIPITANT("precipitant"),
    REDUCER("reducing_agent"),
    INERT_GAS("inert_gas"),
    PROCESS_GAS("process_gas"),
    SEED_CRYSTAL("seed_crystal");

    private final String itemId;

    Reagent(String itemId) {
        this.itemId = itemId;
    }

    public String itemId() {
        return itemId;
    }

    public Item item() {
        return RegistryHandler.REAGENT_ITEMS.get(this).get();
    }
}
