package com.thecascadian.universaloreprocessing.ladder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datapack tables under {@code data/<namespace>/uop_ladder/}. The file
 * {@code ratios.json} holds the smelt ratio of each rung; every file under
 * {@code byproducts/} names a material (or {@code "*"} for all) and the items
 * the Sluice may wash out of it. Both reload with /reload.
 */
public final class LadderTables extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "uop_ladder";
    private static final Gson GSON = new GsonBuilder().create();

    /** Smelt ratios per rung; the defaults match the shipped table. */
    public record Ratios(double clumps, double dust, double shards, int oreClumps) {
        public static final Ratios DEFAULT = new Ratios(1.25D, 1.5D, 2.0D, 1);

        public double of(Form form) {
            return switch (form) {
                case RAW -> 1.0D;
                case CLUMPS -> clumps;
                case DUST -> dust;
                case SHARDS -> shards;
            };
        }
    }

    /** One Sluice byproduct: an item id or an item tag, with its base chance per washed item. */
    public record Byproduct(ResourceLocation id, boolean tag, double chance) {
    }

    private static volatile Ratios ratios = Ratios.DEFAULT;
    private static volatile Map<String, List<Byproduct>> byproducts = Map.of();

    public LadderTables() {
        super(GSON, DIRECTORY);
    }

    public static Ratios ratios() {
        return ratios;
    }

    /** Client side: replaces the ratios with the values the server synced. */
    public static void acceptSynced(Ratios synced) {
        ratios = synced;
    }

    public static List<Byproduct> byproductsFor(String material) {
        Map<String, List<Byproduct>> table = byproducts;
        List<Byproduct> result = new ArrayList<>(table.getOrDefault("*", List.of()));
        result.addAll(table.getOrDefault(material, List.of()));
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Ratios nextRatios = Ratios.DEFAULT;
        Map<String, List<Byproduct>> nextByproducts = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            String path = entry.getKey().getPath();
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), path);
                if (path.equals("ratios")) {
                    nextRatios = new Ratios(
                            GsonHelper.getAsDouble(json, "clumps", Ratios.DEFAULT.clumps()),
                            GsonHelper.getAsDouble(json, "dust", Ratios.DEFAULT.dust()),
                            GsonHelper.getAsDouble(json, "shards", Ratios.DEFAULT.shards()),
                            GsonHelper.getAsInt(json, "ore_clumps", Ratios.DEFAULT.oreClumps()));
                } else if (path.startsWith("byproducts/")) {
                    String material = GsonHelper.getAsString(json, "material");
                    List<Byproduct> list = nextByproducts.computeIfAbsent(material, k -> new ArrayList<>());
                    JsonArray array = GsonHelper.getAsJsonArray(json, "byproducts");
                    for (JsonElement element : array) {
                        JsonObject object = GsonHelper.convertToJsonObject(element, "byproduct");
                        double chance = GsonHelper.getAsDouble(object, "chance");
                        if (object.has("tag")) {
                            list.add(new Byproduct(ResourceLocation.parse(GsonHelper.getAsString(object, "tag")), true, chance));
                        } else {
                            list.add(new Byproduct(ResourceLocation.parse(GsonHelper.getAsString(object, "item")), false, chance));
                        }
                    }
                }
            } catch (RuntimeException e) {
                UniversalOreProcessing.LOGGER.error("[UniversalOreProcessing] Skipping ladder table {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }

        ratios = nextRatios;
        byproducts = Map.copyOf(nextByproducts);
        UniversalOreProcessing.LOGGER.info("[UniversalOreProcessing] Ladder ratios {} / {} / {}, byproducts for {} material key(s).",
                nextRatios.clumps(), nextRatios.dust(), nextRatios.shards(), nextByproducts.size());
    }
}
