package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = UniversalOreProcessing.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class DataGenerators {

    private DataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        BlockTagsProvider blockTags = new OreProcessingBlockTagsProvider(output, lookup, files);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(),
                new OreProcessingItemTagsProvider(output, lookup, blockTags.contentsGetter(), files));
        generator.addProvider(event.includeServer(), new OreProcessingRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(OreProcessingBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
        generator.addProvider(event.includeServer(), new LadderTableProvider(output));

        generator.addProvider(event.includeClient(), new OreProcessingBlockStateProvider(output, files));
        generator.addProvider(event.includeClient(), new OreProcessingItemModelProvider(output, files));
        generator.addProvider(event.includeClient(), new OreProcessingLanguageProvider(output));
    }
}
