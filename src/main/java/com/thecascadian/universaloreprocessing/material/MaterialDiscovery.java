package com.thecascadian.universaloreprocessing.material;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Scans the common item tags (`c:ores/*`, `c:raw_materials/*`, `c:ingots/*`,
 * `c:gems/*`, `c:dusts/*`) and builds a {@link MaterialRegistry}. Discovery runs
 * after every tag load on both logical sides, so clients derive the same
 * registry from the synced tags instead of needing a dedicated payload.
 */
@EventBusSubscriber(modid = UniversalOreProcessing.MODID)
public final class MaterialDiscovery {

    private MaterialDiscovery() {
    }

    private enum Category {
        ORE("ores/"), RAW("raw_materials/"), INGOT("ingots/"), GEM("gems/"), DUST("dusts/");

        private final String prefix;

        Category(String prefix) {
            this.prefix = prefix;
        }
    }

    /** Config snapshot taken once per discovery so a scan sees consistent filters. */
    private record Filters(List<String> materials, List<String> mods, List<String> nsExact, List<String> nsContains,
            List<String> nsStarts, List<String> pathContains, List<String> outputPreference,
            List<String> namespacePriority) {

        static Filters read() {
            OreProcessingConfig.Common c = OreProcessingConfig.COMMON;
            return new Filters(
                    List.copyOf(OreProcessingConfig.get(c.excludedMaterials)),
                    List.copyOf(OreProcessingConfig.get(c.excludedMods)),
                    List.copyOf(OreProcessingConfig.get(c.nsExact)),
                    List.copyOf(OreProcessingConfig.get(c.nsContains)),
                    List.copyOf(OreProcessingConfig.get(c.nsStarts)),
                    List.copyOf(OreProcessingConfig.get(c.pathContains)),
                    List.copyOf(OreProcessingConfig.get(c.outputPreference)),
                    List.copyOf(OreProcessingConfig.get(c.namespacePriority)));
        }

        boolean excludesItem(Item item) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String ns = id.getNamespace();
            String path = id.getPath();
            if (item == Items.AIR || item.builtInRegistryHolder().is(RegistryHandler.NON_PROCESSABLE_TAG))
                return true;
            if (mods.contains(ns) || nsExact.contains(ns))
                return true;
            for (String partial : nsContains) {
                if (ns.contains(partial))
                    return true;
            }
            for (String prefix : nsStarts) {
                if (ns.startsWith(prefix))
                    return true;
            }
            for (String substr : pathContains) {
                if (path.contains(substr))
                    return true;
            }
            return false;
        }
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        MaterialRegistry next = discover();
        MaterialRegistry.swap(next);
        UniversalOreProcessing.LOGGER.info("[UniversalOreProcessing] Discovered {} processable material(s).",
                next.materials().size());
    }

    public static MaterialRegistry discover() {
        Filters filters = Filters.read();
        Map<String, Map<Category, List<Item>>> found = new TreeMap<>();

        BuiltInRegistries.ITEM.getTagNames().forEach(tag -> collect(tag, found));

        Map<String, MaterialRegistry.Material> accepted = new TreeMap<>();
        for (Map.Entry<String, Map<Category, List<Item>>> entry : found.entrySet()) {
            String id = entry.getKey();
            if (filters.materials().contains(id))
                continue;

            Map<Category, List<Item>> byCategory = entry.getValue();
            List<Item> ores = allowed(byCategory, Category.ORE, filters);
            List<Item> raws = allowed(byCategory, Category.RAW, filters);
            if (ores.isEmpty() && raws.isEmpty())
                continue;

            Optional<Item> output = resolveOutput(id, byCategory, filters);
            if (output.isEmpty())
                continue;

            accepted.put(id, new MaterialRegistry.Material(id, output.get(), List.copyOf(ores), List.copyOf(raws)));
        }
        return new MaterialRegistry(accepted);
    }

    private static void collect(TagKey<Item> tag, Map<String, Map<Category, List<Item>>> found) {
        ResourceLocation location = tag.location();
        if (!location.getNamespace().equals("c"))
            return;

        String path = location.getPath();
        for (Category category : Category.values()) {
            if (!path.startsWith(category.prefix))
                continue;
            String material = path.substring(category.prefix.length());
            // nested tags such as c:ores/nether/gold are not materials
            if (material.isEmpty() || material.contains("/"))
                return;

            Optional<HolderSet.Named<Item>> holders = BuiltInRegistries.ITEM.getTag(tag);
            if (holders.isEmpty())
                return;

            List<Item> items = found
                    .computeIfAbsent(material, k -> new EnumMap<>(Category.class))
                    .computeIfAbsent(category, k -> new ArrayList<>());
            for (Holder<Item> holder : holders.get()) {
                items.add(holder.value());
            }
            return;
        }
    }

    private static List<Item> allowed(Map<Category, List<Item>> byCategory, Category category, Filters filters) {
        List<Item> result = new ArrayList<>();
        for (Item item : byCategory.getOrDefault(category, List.of())) {
            if (!filters.excludesItem(item) && !result.contains(item))
                result.add(item);
        }
        return result;
    }

    private static Optional<Item> resolveOutput(String material, Map<Category, List<Item>> byCategory,
            Filters filters) {
        Comparator<Item> order = namespaceOrder(filters.namespacePriority());

        for (String kind : filters.outputPreference()) {
            Category category = switch (kind.toLowerCase(Locale.ROOT)) {
                case "ingot" -> Category.INGOT;
                case "gem" -> Category.GEM;
                case "dust" -> Category.DUST;
                default -> null;
            };
            if (category == null)
                continue;
            Optional<Item> best = allowed(byCategory, category, filters).stream().min(order);
            if (best.isPresent())
                return best;
        }

        // materials such as coal ship no ingot, gem or dust tag; accept a registry item named after the material
        List<Item> named = new ArrayList<>();
        for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getKey().location().getPath().equals(material) && !filters.excludesItem(entry.getValue()))
                named.add(entry.getValue());
        }
        return named.stream().min(order);
    }

    private static Comparator<Item> namespaceOrder(List<String> priority) {
        return Comparator
                .<Item>comparingInt(item -> {
                    int index = priority.indexOf(BuiltInRegistries.ITEM.getKey(item).getNamespace());
                    return index < 0 ? priority.size() : index;
                })
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString());
    }
}
