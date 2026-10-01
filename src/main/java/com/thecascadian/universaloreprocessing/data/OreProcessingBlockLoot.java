package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class OreProcessingBlockLoot extends BlockLootSubProvider {

    public OreProcessingBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(RegistryHandler.CRUSHING_SLAB.get());
        dropSelf(RegistryHandler.SLUICE.get());
        // slurry is lost with the water; the vanilla cauldron itself always comes back
        dropOther(RegistryHandler.SLURRY_CAULDRON.get(), Items.CAULDRON);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return RegistryHandler.BLOCKS.getEntries().stream().<Block>map(holder -> holder.get()).toList();
    }
}
