package com.thecascadian.universaloreprocessing.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration settings for Universal Ore Processing, written to
 * `config/universaloreprocessing-common.toml`. Ratios and byproducts are not
 * configured here; they live in the datapack tables and reload with /reload.
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
        public final ModConfigSpec.ConfigValue<List<? extends String>> excludedMaterials;
        public final ModConfigSpec.ConfigValue<List<? extends String>> excludedMods;
        public final ModConfigSpec.ConfigValue<List<? extends String>> outputPreference;
        public final ModConfigSpec.ConfigValue<List<? extends String>> namespacePriority;

        public final ModConfigSpec.BooleanValue ladderSmelting;
        public final ModConfigSpec.BooleanValue heavyBlockStrikes;
        public final ModConfigSpec.BooleanValue grindstoneHopperInput;

        public final ModConfigSpec.IntValue strikesPerItem;
        public final ModConfigSpec.IntValue stirsPerSlurry;
        public final ModConfigSpec.IntValue settleTicks;
        public final ModConfigSpec.IntValue sluiceMaxRow;
        public final ModConfigSpec.DoubleValue sluiceByproductCap;

        Common(ModConfigSpec.Builder builder) {
            builder.push("discovery");
            excludedMaterials = builder
                    .comment("Material ids that are never processed (e.g. 'iron', 'certus_quartz')")
                    .defineList("excluded_materials", new ArrayList<>(), o -> o instanceof String);
            excludedMods = builder
                    .comment("Mod IDs whose items are never used as ore inputs or as resolved outputs")
                    .defineList("excluded_mods", new ArrayList<>(), o -> o instanceof String);
            outputPreference = builder
                    .comment("Order in which output kinds are tried when resolving a material's smelting result. Valid entries: 'ingot', 'gem'.")
                    .defineList("output_preference", List.of("ingot", "gem"), o -> o instanceof String);
            namespacePriority = builder
                    .comment("Namespaces preferred when several items qualify as the output of one material.")
                    .defineList("namespace_priority", List.of("minecraft"), o -> o instanceof String);
            builder.pop();

            builder.push("ladder");
            ladderSmelting = builder
                    .comment("Allow clumps, dust and shards to smelt in the furnace and blast furnace.")
                    .define("ladder_smelting", true);
            heavyBlockStrikes = builder
                    .comment("A falling block in the universaloreprocessing:heavy tag strikes a Crushing Slab it lands on.")
                    .define("heavy_block_strikes", true);
            grindstoneHopperInput = builder
                    .comment("Hoppers may feed clumps into a vanilla grindstone.")
                    .define("grindstone_hopper_input", true);
            strikesPerItem = builder
                    .comment("Strikes needed before an item on a Crushing Slab breaks. The last four strikes show crack stages.")
                    .defineInRange("strikes_per_item", 5, 1, 16);
            stirsPerSlurry = builder
                    .comment("Stick stirs needed to turn dust in a water cauldron into slurry.")
                    .defineInRange("stirs_per_slurry", 3, 1, 16);
            settleTicks = builder
                    .comment("Ticks slurry needs to settle into shards. Split evenly across three visible stages.")
                    .defineInRange("settle_ticks", 2400, 60, 72000);
            sluiceMaxRow = builder
                    .comment("Longest Sluice row that still raises the byproduct chance.")
                    .defineInRange("sluice_max_row", 8, 1, 32);
            sluiceByproductCap = builder
                    .comment("Upper bound on any single byproduct chance, whatever the row length.")
                    .defineInRange("sluice_byproduct_cap", 0.5D, 0.0D, 1.0D);
            builder.pop();
        }
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
