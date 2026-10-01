package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class OreProcessingItemTagsProvider extends ItemTagsProvider {

    public OreProcessingItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
            CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper files) {
        super(output, lookup, blockTags, UniversalOreProcessing.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(RegistryHandler.HAMMERS_TAG).add(RegistryHandler.HAMMER.get());
        // empty by default; packs list items here to keep them off the ladder
        tag(RegistryHandler.NON_PROCESSABLE_TAG);
    }
}
