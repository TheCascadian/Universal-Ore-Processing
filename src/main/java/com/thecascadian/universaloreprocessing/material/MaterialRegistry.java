package com.thecascadian.universaloreprocessing.material;

import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Immutable snapshot of every processable material. A new snapshot is built by
 * {@link MaterialDiscovery} after each tag load and swapped in atomically, so
 * machines and recipes always see a consistent view without locking.
 */
public final class MaterialRegistry {

    /** A discovered material: its id, resolved final item and the input items that feed the crusher. */
    public record Material(String id, Item output, List<Item> oreItems, List<Item> rawItems) {
    }

    /** Lookup entry for one crusher input item. */
    public record InputEntry(String materialId, boolean raw) {
    }

    private static final MaterialRegistry EMPTY = new MaterialRegistry(Map.of());
    private static final AtomicReference<MaterialRegistry> CURRENT = new AtomicReference<>(EMPTY);

    private final Map<String, Material> materials;
    private final Map<Item, InputEntry> inputs;

    MaterialRegistry(Map<String, Material> materials) {
        this.materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
        Map<Item, InputEntry> index = new LinkedHashMap<>();
        for (Material material : this.materials.values()) {
            // materials arrive sorted by id, so on a clash the alphabetically first material wins
            for (Item item : material.oreItems()) {
                index.putIfAbsent(item, new InputEntry(material.id(), false));
            }
            for (Item item : material.rawItems()) {
                index.putIfAbsent(item, new InputEntry(material.id(), true));
            }
        }
        this.inputs = Collections.unmodifiableMap(index);
    }

    public static MaterialRegistry current() {
        return CURRENT.get();
    }

    static void swap(MaterialRegistry next) {
        CURRENT.set(next);
    }

    public Map<String, Material> materials() {
        return materials;
    }

    public Optional<Material> get(String id) {
        return Optional.ofNullable(materials.get(id));
    }

    public InputEntry inputFor(Item item) {
        return inputs.get(item);
    }

    public boolean isEmpty() {
        return materials.isEmpty();
    }

    /** Number of dynamic recipes the stage evaluates: crush per input item, wash and smelt per material. */
    public int crushRecipeCount() {
        return inputs.size();
    }

    public int washRecipeCount() {
        return materials.size();
    }

    public int smeltRecipeCount() {
        return materials.size();
    }
}
