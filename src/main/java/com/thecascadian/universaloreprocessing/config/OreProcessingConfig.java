package com.thecascadian.universaloreprocessing.config;

import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration settings for Universal Ore Processing.  The values here are
 * written to a TOML file on disk (`config/universaloreprocessing-common.toml`)
 * which pack authors and end users can edit directly.  Every option is
 * commented in the generated file; list options filter which materials are
 * discovered and the numeric options tune each processing stage.
 */
public class OreProcessingConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static class Common {
        // filtering model shared with Universal Compression
        public final ModConfigSpec.ConfigValue<List<? extends String>> excludedMaterials;
        public final ModConfigSpec.ConfigValue<List<? extends String>> excludedMods;
        public final ModConfigSpec.ConfigValue<List<? extends String>> nsExact;
        public final ModConfigSpec.ConfigValue<List<? extends String>> nsContains;
        public final ModConfigSpec.ConfigValue<List<? extends String>> nsStarts;
        public final ModConfigSpec.ConfigValue<List<? extends String>> pathContains;

        public final ModConfigSpec.ConfigValue<List<? extends String>> outputPreference;
        public final ModConfigSpec.ConfigValue<List<? extends String>> namespacePriority;

        public final ModConfigSpec.BooleanValue crushEnabled;
        public final ModConfigSpec.BooleanValue washEnabled;
        public final ModConfigSpec.BooleanValue smeltEnabled;

        public final ModConfigSpec.IntValue crushedPerOre;
        public final ModConfigSpec.IntValue crushedPerRaw;
        public final ModConfigSpec.DoubleValue crusherYieldMultiplier;
        public final ModConfigSpec.DoubleValue washerYieldMultiplier;
        public final ModConfigSpec.DoubleValue smelterYieldMultiplier;

        public final ModConfigSpec.IntValue crusherTicks;
        public final ModConfigSpec.IntValue washerTicks;
        public final ModConfigSpec.IntValue smelterTicks;
        public final ModConfigSpec.IntValue washerWaterPerOperation;

        public final ModConfigSpec.BooleanValue useEnergy;
        public final ModConfigSpec.IntValue energyPerTick;
        public final ModConfigSpec.IntValue energyCapacity;

        // refinery stations
        public final ModConfigSpec.BooleanValue strictAffinities;
        public final ModConfigSpec.ConfigValue<List<? extends String>> materialTraits;
        public final ModConfigSpec.ConfigValue<List<? extends String>> defaultTraits;
        public final ModConfigSpec.BooleanValue scalePowerByTier;
        public final ModConfigSpec.DoubleValue stationWaterMultiplier;
        public final ModConfigSpec.DoubleValue wasteRecycleChance;
        public final ModConfigSpec.ConfigValue<List<? extends String>> slimeMaterials;
        public final ModConfigSpec.BooleanValue consumeReagents;
        public final ModConfigSpec.BooleanValue hazardsEnabled;
        public final ModConfigSpec.IntValue hazardRadius;
        public final ModConfigSpec.BooleanValue creativeAllStages;

        public final Map<MachineKind, ModConfigSpec.BooleanValue> stationEnabled = new EnumMap<>(MachineKind.class);
        public final Map<MachineKind, ModConfigSpec.IntValue> stationTicks = new EnumMap<>(MachineKind.class);
        public final Map<MachineKind, ModConfigSpec.DoubleValue> stationYield = new EnumMap<>(MachineKind.class);
        public final Map<MachineKind, ModConfigSpec.DoubleValue> stationByproduct = new EnumMap<>(MachineKind.class);

        Common(ModConfigSpec.Builder builder) {
            builder.push("general");
            excludedMaterials = builder
                    .comment("Material ids that are never processed (e.g. 'iron', 'certus_quartz')")
                    .defineList("excluded_materials", new ArrayList<>(), o -> o instanceof String);
            excludedMods = builder
                    .comment("Mod IDs whose items are never used as ore inputs or as resolved outputs (e.g. 'create')")
                    .defineList("excluded_mods", new ArrayList<>(), o -> o instanceof String);
            nsExact = builder
                    .comment("Additional namespaces to blacklist exactly (e.g. 'mymod' will exclude all items from that mod)")
                    .defineList("blacklist_namespaces_exact", new ArrayList<>(), o -> o instanceof String);
            nsContains = builder
                    .comment("Namespaces which if they contain the supplied string will be blacklisted (case-sensitive). Useful for catching family mods.")
                    .defineList("blacklist_namespaces_contains", new ArrayList<>(), o -> o instanceof String);
            nsStarts = builder
                    .comment("Namespaces which if they start with the supplied prefix will be blacklisted.")
                    .defineList("blacklist_namespaces_starts", new ArrayList<>(), o -> o instanceof String);
            pathContains = builder
                    .comment("Item ID path substrings that trigger a blacklist (e.g. 'deepslate' will skip 'mod:deepslate_tin_ore').")
                    .defineList("blacklist_paths_contains", new ArrayList<>(), o -> o instanceof String);

            outputPreference = builder
                    .comment("Order in which output kinds are tried when resolving a material's final item. Valid entries: 'ingot', 'gem', 'dust'.")
                    .defineList("output_preference", List.of("ingot", "gem", "dust"), o -> o instanceof String);
            namespacePriority = builder
                    .comment("Namespaces preferred when several items qualify as the output of one material. Listed namespaces win in order, the rest are sorted alphabetically.")
                    .defineList("namespace_priority", List.of("minecraft"), o -> o instanceof String);

            crushEnabled = builder
                    .comment("Enable the Ore Crusher stage.")
                    .define("crush_enabled", true);
            washEnabled = builder
                    .comment("Enable the Ore Washer stage.")
                    .define("wash_enabled", true);
            smeltEnabled = builder
                    .comment("Enable the Ore Smelter stage.")
                    .define("smelt_enabled", true);

            crushedPerOre = builder
                    .comment("Crushed Ore produced from one ore block item.")
                    .defineInRange("crushed_per_ore", 2, 1, 64);
            crushedPerRaw = builder
                    .comment("Crushed Ore produced from one raw material item.")
                    .defineInRange("crushed_per_raw", 1, 1, 64);
            crusherYieldMultiplier = builder
                    .comment("Multiplier applied to the Ore Crusher output count (the fractional part is a chance of one extra item, minimum 1).")
                    .defineInRange("crusher_yield_multiplier", 1.0D, 0.0D, 16.0D);
            washerYieldMultiplier = builder
                    .comment("Multiplier applied to the Ore Washer output count (the fractional part is a chance of one extra item, minimum 1).")
                    .defineInRange("washer_yield_multiplier", 1.0D, 0.0D, 16.0D);
            smelterYieldMultiplier = builder
                    .comment("Yield bonus for the Ore Smelter output count (the fractional part is a chance of one extra item, minimum 1). 1.5 averages 1.5 items per purified ore.")
                    .defineInRange("smelter_yield_multiplier", 1.0D, 0.0D, 16.0D);

            crusherTicks = builder
                    .comment("Ticks the Ore Crusher needs per operation.")
                    .defineInRange("crusher_ticks", 100, 1, 72000);
            washerTicks = builder
                    .comment("Ticks the Ore Washer needs per operation.")
                    .defineInRange("washer_ticks", 100, 1, 72000);
            smelterTicks = builder
                    .comment("Ticks the Ore Smelter needs per operation. 100 matches a blast furnace, 200 matches a furnace.")
                    .defineInRange("smelter_ticks", 100, 1, 72000);
            washerWaterPerOperation = builder
                    .comment("Millibuckets of water consumed by the Ore Washer per operation.")
                    .defineInRange("washer_water_per_operation", 250, 0, 4000);

            useEnergy = builder
                    .comment("If true machines consume FE instead of fuel. If false machines burn fuel items.")
                    .define("use_energy", false);
            energyPerTick = builder
                    .comment("FE consumed per tick of work when use_energy is enabled.")
                    .defineInRange("energy_per_tick", 20, 1, 100000);
            energyCapacity = builder
                    .comment("FE buffer of each machine when use_energy is enabled.")
                    .defineInRange("energy_capacity", 20000, 1, 10000000);
            builder.pop();

            builder.push("refinery");
            strictAffinities = builder
                    .comment("If true each refining station only accepts materials that have a matching trait (for example flotation only takes sulfides). If false every station accepts every material.")
                    .define("strict_affinities", true);
            materialTraits = builder
                    .comment("Per-material trait overrides as 'material=trait,trait', replacing the built-in traits of that material (e.g. 'tin=dense,oxide,electro'). Traits: dense, sulfide, magnetic, oxide, refractory, leachable, extractable, precipitable, electro, electrolysis, structural, superalloy, hydrocarbon, carbonyl, semiconductor, crystal, sinterable, isotopic, radioactive, noble.")
                    .defineList("material_traits", new ArrayList<>(), o -> o instanceof String);
            defaultTraits = builder
                    .comment("Traits given to every material that has no built-in or overridden traits, so unfamiliar modded ores still enter the refining tree.")
                    .defineList("default_traits", List.of("dense", "oxide", "leachable", "electro", "precipitable"),
                            o -> o instanceof String);
            scalePowerByTier = builder
                    .comment("If true higher tier stations draw proportionally more FE or burn fuel faster than the base machines.")
                    .define("scale_power_by_tier", true);
            stationWaterMultiplier = builder
                    .comment("Multiplier on the water every refining station consumes per operation.")
                    .defineInRange("station_water_multiplier", 1.0D, 0.0D, 16.0D);
            wasteRecycleChance = builder
                    .comment("Chance that the Ore Crusher turns one byproduct item (gangue, tailings, slag and so on) into one dust.")
                    .defineInRange("waste_recycle_chance", 0.25D, 0.0D, 1.0D);
            slimeMaterials = builder
                    .comment("Precious materials that anode slime can carry, tried in order. The first one present in the pack is used.")
                    .defineList("slime_materials", List.of("gold", "silver"), o -> o instanceof String);
            consumeReagents = builder
                    .comment("If false reagents (acid, flux, gases and so on) are required but never used up.")
                    .define("consume_reagents", true);
            hazardsEnabled = builder
                    .comment("If true running stations harm players standing close to them: toxic stations poison, hot stations set fire and radioactive stations wither.")
                    .define("hazards_enabled", false);
            hazardRadius = builder
                    .comment("Radius in blocks of the station hazards.")
                    .defineInRange("hazard_radius", 4, 1, 16);
            creativeAllStages = builder
                    .comment("If true the refinery creative tab lists every stage item for every material. If false it lists one generic item per stage.")
                    .define("creative_all_stages", false);

            builder.push("stations");
            for (MachineKind kind : MachineKind.values()) {
                ProcessRule rule = ProcessRules.get(kind);
                if (rule == null)
                    continue;
                builder.push(kind.id());
                stationEnabled.put(kind, builder
                        .comment("Enable this station.")
                        .define("enabled", true));
                stationTicks.put(kind, builder
                        .comment("Ticks this station needs per operation.")
                        .defineInRange("ticks", rule.ticks(), 1, 72000));
                stationYield.put(kind, builder
                        .comment("Multiplier on the main output count (the fractional part is a chance of one extra item, minimum 1).")
                        .defineInRange("yield_multiplier", 1.0D, 0.0D, 16.0D));
                if (rule.secondary() != null) {
                    stationByproduct.put(kind, builder
                            .comment("Chance per operation that the byproduct appears.")
                            .defineInRange("byproduct_chance", rule.secondary().chance(), 0.0D, 1.0D));
                }
                builder.pop();
            }
            builder.pop(2);
        }
    }

    // -------------------------------------------------------------------------
    // Per-station settings. The three base machines keep their original option names.
    // -------------------------------------------------------------------------

    public static boolean enabled(MachineKind kind) {
        Common config = COMMON;
        return switch (kind) {
            case CRUSHER -> get(config.crushEnabled);
            case WASHER -> get(config.washEnabled);
            case SMELTER -> get(config.smeltEnabled);
            default -> get(config.stationEnabled.get(kind));
        };
    }

    public static int ticks(MachineKind kind) {
        Common config = COMMON;
        return switch (kind) {
            case CRUSHER -> get(config.crusherTicks);
            case WASHER -> get(config.washerTicks);
            case SMELTER -> get(config.smelterTicks);
            default -> get(config.stationTicks.get(kind));
        };
    }

    public static double yieldMultiplier(MachineKind kind) {
        Common config = COMMON;
        return switch (kind) {
            case CRUSHER -> get(config.crusherYieldMultiplier);
            case WASHER -> get(config.washerYieldMultiplier);
            case SMELTER -> get(config.smelterYieldMultiplier);
            default -> get(config.stationYield.get(kind));
        };
    }

    public static double byproductChance(MachineKind kind) {
        ModConfigSpec.DoubleValue value = COMMON.stationByproduct.get(kind);
        return value == null ? 0.0D : get(value);
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    /** Reads a value, falling back to its default while the config file has not been loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            // config not yet loaded; the declared default is the right answer
            return value.getDefault();
        }
    }
}
