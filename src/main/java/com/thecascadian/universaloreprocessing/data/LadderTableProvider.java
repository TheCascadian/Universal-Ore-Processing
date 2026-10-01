package com.thecascadian.universaloreprocessing.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.ladder.LadderTables;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Writes the default ladder tables. Byproducts name items by tag wherever a
 * common tag exists, so the defaults follow whichever mod provides them.
 */
public class LadderTableProvider implements DataProvider {

    private final PackOutput.PathProvider paths;

    public LadderTableProvider(PackOutput output) {
        this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, LadderTables.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();

        LadderTables.Ratios defaults = LadderTables.Ratios.DEFAULT;
        JsonObject ratios = new JsonObject();
        ratios.addProperty("clumps", defaults.clumps());
        ratios.addProperty("dust", defaults.dust());
        ratios.addProperty("shards", defaults.shards());
        ratios.addProperty("ore_clumps", defaults.oreClumps());
        writes.add(DataProvider.saveStable(cache, ratios, path("ratios")));

        writes.add(byproducts(cache, "default", "*", entry("item", "minecraft:flint", 0.02D)));
        writes.add(byproducts(cache, "iron", "iron", entry("tag", "c:nuggets/gold", 0.02D)));
        writes.add(byproducts(cache, "gold", "gold", entry("tag", "c:nuggets/iron", 0.03D)));
        writes.add(byproducts(cache, "copper", "copper", entry("tag", "c:nuggets/gold", 0.015D)));

        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> byproducts(CachedOutput cache, String file, String material, JsonObject... entries) {
        JsonObject json = new JsonObject();
        json.addProperty("material", material);
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        json.add("byproducts", array);
        return DataProvider.saveStable(cache, json, path("byproducts/" + file));
    }

    private static JsonObject entry(String key, String id, double chance) {
        JsonObject entry = new JsonObject();
        entry.addProperty(key, id);
        entry.addProperty("chance", chance);
        return entry;
    }

    private Path path(String name) {
        return paths.json(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(UniversalOreProcessing.MODID, name));
    }

    @Override
    public String getName() {
        return "Universal Ore Processing ladder tables";
    }
}
