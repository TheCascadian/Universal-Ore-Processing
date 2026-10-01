package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class OreProcessingBlockTagsProvider extends BlockTagsProvider {

    public OreProcessingBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
            ExistingFileHelper files) {
        super(output, lookup, UniversalOreProcessing.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(RegistryHandler.CRUSHING_SLAB.get(), RegistryHandler.SLURRY_CAULDRON.get(),
                        RegistryHandler.QUERN.get());
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(RegistryHandler.SLUICE.get(), RegistryHandler.TRIP_HAMMER.get(),
                        RegistryHandler.STIRRING_PADDLE.get(), RegistryHandler.SETTLING_TANK.get());
        // anvils are the vanilla heavy falling blocks; packs may add their own
        tag(RegistryHandler.HEAVY_TAG)
                .addTag(BlockTags.ANVIL);
    }
}
