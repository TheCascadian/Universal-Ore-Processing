package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Item models. Ladder forms use three grayscale layers, one per value band
 * (shadow, mid, light), and each layer has its own tint index so the client
 * can apply a hue-shifted ramp instead of one flat multiply.
 */
public class OreProcessingItemModelProvider extends ItemModelProvider {

    public OreProcessingItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, UniversalOreProcessing.MODID, files);
    }

    @Override
    protected void registerModels() {
        layered("clumps");
        layered("dust");
        layered("shards");

        withExistingParent("hammer", ResourceLocation.withDefaultNamespace("item/handheld"))
                .texture("layer0", modLoc("item/hammer"));
        // block models come from the blockstate provider in the same run, so they are not checked on disk
        getBuilder("crushing_slab").parent(new ModelFile.UncheckedModelFile(modLoc("block/crushing_slab")));
        getBuilder("sluice").parent(new ModelFile.UncheckedModelFile(modLoc("block/sluice")));
        getBuilder("trip_hammer").parent(new ModelFile.UncheckedModelFile(modLoc("block/trip_hammer_up")));
        getBuilder("quern").parent(new ModelFile.UncheckedModelFile(modLoc("block/quern")));
        getBuilder("stirring_paddle").parent(new ModelFile.UncheckedModelFile(modLoc("block/stirring_paddle_a")));
        getBuilder("settling_tank").parent(new ModelFile.UncheckedModelFile(modLoc("block/settling_tank")));
        basicItem(RegistryHandler.PANNING_TRAY.get());
        basicItem(RegistryHandler.GUIDE.get());
    }

    private void layered(String name) {
        ItemModelBuilder builder = withExistingParent(name, ResourceLocation.withDefaultNamespace("item/generated"));
        for (int layer = 0; layer < 3; layer++) {
            builder.texture("layer" + layer, modLoc("item/" + name + "_" + layer));
        }
    }
}
