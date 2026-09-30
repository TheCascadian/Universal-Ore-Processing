package com.thecascadian.universaloreprocessing.material;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.thecascadian.universaloreprocessing.material.MaterialTrait.*;

/**
 * Resolves the traits of a material id. Built-in defaults cover the vanilla and
 * the common modded materials, pack authors can replace them per material with
 * the material_traits option, and every material that is in neither source gets
 * the default_traits so unfamiliar modded ores still enter the processing tree.
 */
public final class MaterialTraits {

    private static final Map<String, Set<MaterialTrait>> DEFAULTS = defaults();

    private record Resolved(List<String> overrideSource, List<String> fallbackSource,
            Map<String, Set<MaterialTrait>> overrides, Set<MaterialTrait> fallback) {
    }

    private static volatile Resolved resolved = new Resolved(null, null, Map.of(), Set.of());

    private MaterialTraits() {
    }

    private static Map<String, Set<MaterialTrait>> defaults() {
        Map<String, Set<MaterialTrait>> map = new HashMap<>();
        put(map, "iron", DENSE, MAGNETIC, OXIDE, STRUCTURAL);
        put(map, "copper", SULFIDE, ELECTRO, LEACHABLE, EXTRACTABLE, PRECIPITABLE);
        put(map, "gold", DENSE, NOBLE, ELECTRO, LEACHABLE, PRECIPITABLE);
        put(map, "coal", DENSE, HYDROCARBON);
        put(map, "diamond", DENSE, SEMICONDUCTOR, SINTERABLE);
        put(map, "emerald", LEACHABLE);
        put(map, "lapis", LEACHABLE);
        put(map, "redstone", LEACHABLE, PRECIPITABLE);
        put(map, "quartz", SEMICONDUCTOR, CRYSTAL);
        put(map, "netherite_scrap", DENSE, REFRACTORY, SUPERALLOY);
        put(map, "tin", DENSE, OXIDE, ELECTRO, LEACHABLE);
        put(map, "lead", DENSE, SULFIDE, OXIDE, ELECTRO);
        put(map, "zinc", SULFIDE, OXIDE, PRECIPITABLE);
        put(map, "nickel", SULFIDE, OXIDE, MAGNETIC, STRUCTURAL, SUPERALLOY, LEACHABLE, EXTRACTABLE, ELECTRO,
                CARBONYL);
        put(map, "cobalt", SULFIDE, SUPERALLOY, LEACHABLE, EXTRACTABLE, ELECTRO);
        put(map, "silver", DENSE, NOBLE, ELECTRO, LEACHABLE, PRECIPITABLE);
        put(map, "platinum", DENSE, NOBLE, ELECTRO, LEACHABLE);
        put(map, "osmium", DENSE, NOBLE, ELECTRO, LEACHABLE);
        put(map, "iridium", DENSE, NOBLE, ELECTRO, LEACHABLE);
        put(map, "aluminum", OXIDE, LEACHABLE, ELECTROLYSIS);
        put(map, "aluminium", OXIDE, LEACHABLE, ELECTROLYSIS);
        put(map, "bauxite", OXIDE, LEACHABLE, ELECTROLYSIS);
        put(map, "lithium", LEACHABLE, EXTRACTABLE, PRECIPITABLE, ELECTROLYSIS);
        put(map, "magnesium", REFRACTORY, ELECTROLYSIS);
        put(map, "titanium", REFRACTORY, MAGNETIC, ELECTROLYSIS, SUPERALLOY, CARBONYL);
        put(map, "zirconium", REFRACTORY, CARBONYL);
        put(map, "hafnium", REFRACTORY, CARBONYL);
        put(map, "tungsten", DENSE, REFRACTORY, SINTERABLE);
        put(map, "molybdenum", SULFIDE, REFRACTORY);
        put(map, "antimony", OXIDE, SULFIDE);
        put(map, "bismuth", DENSE, OXIDE);
        put(map, "silicon", SEMICONDUCTOR, CRYSTAL, SINTERABLE);
        put(map, "sapphire", CRYSTAL, SINTERABLE);
        put(map, "silicon_carbide", SEMICONDUCTOR, SINTERABLE);
        put(map, "uranium", DENSE, RADIOACTIVE, ISOTOPIC, LEACHABLE, PRECIPITABLE);
        put(map, "thorium", RADIOACTIVE, ISOTOPIC, LEACHABLE);
        put(map, "plutonium", RADIOACTIVE, ISOTOPIC);
        put(map, "monazite", MAGNETIC, LEACHABLE, EXTRACTABLE);
        put(map, "neodymium", MAGNETIC, LEACHABLE, EXTRACTABLE);
        put(map, "rare_earth", MAGNETIC, LEACHABLE, EXTRACTABLE);
        put(map, "ilmenite", MAGNETIC, REFRACTORY);
        put(map, "graphite", HYDROCARBON, SINTERABLE);
        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, Set<MaterialTrait>> map, String id, MaterialTrait first,
            MaterialTrait... rest) {
        map.put(id, Collections.unmodifiableSet(EnumSet.of(first, rest)));
    }

    /** The traits of a material, or every trait when strict_affinities is disabled. */
    public static Set<MaterialTrait> of(String materialId) {
        if (materialId == null)
            return Set.of();
        if (!OreProcessingConfig.get(OreProcessingConfig.COMMON.strictAffinities))
            return EnumSet.allOf(MaterialTrait.class);

        Resolved current = current();
        Set<MaterialTrait> traits = current.overrides().get(materialId);
        if (traits == null)
            traits = DEFAULTS.get(materialId);
        return traits != null ? traits : current.fallback();
    }

    /** True if the material has at least one of the required traits, or the requirement is empty. */
    public static boolean matches(String materialId, Set<MaterialTrait> required) {
        if (required.isEmpty())
            return true;
        Set<MaterialTrait> traits = of(materialId);
        for (MaterialTrait trait : required) {
            if (traits.contains(trait))
                return true;
        }
        return false;
    }

    private static Resolved current() {
        List<? extends String> overrideSource = OreProcessingConfig.get(OreProcessingConfig.COMMON.materialTraits);
        List<? extends String> fallbackSource = OreProcessingConfig.get(OreProcessingConfig.COMMON.defaultTraits);
        Resolved cached = resolved;
        if (overrideSource.equals(cached.overrideSource()) && fallbackSource.equals(cached.fallbackSource()))
            return cached;

        Map<String, Set<MaterialTrait>> overrides = new HashMap<>();
        for (String entry : overrideSource) {
            int split = entry.indexOf('=');
            if (split <= 0) {
                UniversalOreProcessing.LOGGER.warn("[UniversalOreProcessing] Ignoring malformed material_traits entry '{}'.",
                        entry);
                continue;
            }
            overrides.put(entry.substring(0, split).trim().toLowerCase(Locale.ROOT),
                    parse(entry.substring(split + 1)));
        }
        Resolved next = new Resolved(new ArrayList<>(overrideSource), new ArrayList<>(fallbackSource),
                Collections.unmodifiableMap(overrides), parse(String.join(",", fallbackSource)));
        resolved = next;
        return next;
    }

    private static Set<MaterialTrait> parse(String list) {
        EnumSet<MaterialTrait> traits = EnumSet.noneOf(MaterialTrait.class);
        for (String name : list.split(",")) {
            if (name.isBlank())
                continue;
            MaterialTrait.byName(name).ifPresentOrElse(traits::add, () -> UniversalOreProcessing.LOGGER
                    .warn("[UniversalOreProcessing] Unknown material trait '{}'.", name.trim()));
        }
        return Collections.unmodifiableSet(traits);
    }
}
