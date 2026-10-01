package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class OreProcessingLanguageProvider extends LanguageProvider {

    public OreProcessingLanguageProvider(PackOutput output) {
        super(output, UniversalOreProcessing.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + UniversalOreProcessing.MODID, "Universal Ore Processing");

        add(RegistryHandler.CRUSHING_SLAB.get(), "Crushing Slab");
        add(RegistryHandler.SLUICE.get(), "Sluice");
        add(RegistryHandler.SLURRY_CAULDRON.get(), "Slurry Cauldron");
        add(RegistryHandler.HAMMER.get(), "Hammer");

        add(RegistryHandler.CLUMPS.get(), "Clumps");
        add(RegistryHandler.DUST.get(), "Dust");
        add(RegistryHandler.SHARDS.get(), "Shards");
        add(RegistryHandler.CLUMPS.get().getDescriptionId() + ".named", "%s Clumps");
        add(RegistryHandler.DUST.get().getDescriptionId() + ".named", "%s Dust");
        add(RegistryHandler.SHARDS.get().getDescriptionId() + ".named", "%s Shards");

        add("tooltip.universaloreprocessing.smelts", "Smelts into %s %s");
        add("tooltip.universaloreprocessing.next.strike", "Next: strike on a Crushing Slab");
        add("tooltip.universaloreprocessing.next.grind", "Next: grind on a grindstone");
        add("tooltip.universaloreprocessing.next.stir", "Next: stir in a water cauldron");
        add("tooltip.universaloreprocessing.next.none", "Final form: smelt it");
        add("tooltip.universaloreprocessing.washed", "Washed");
    }
}
