package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.CrushingSlabBlock;
import com.thecascadian.universaloreprocessing.block.SlurryCauldronBlock;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Blockstates and block models. Every texture is referenced at its standard
 * location under textures/block, and every overlay is its own model, so a
 * resource pack can replace any single piece.
 */
public class OreProcessingBlockStateProvider extends BlockStateProvider {

    public OreProcessingBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, UniversalOreProcessing.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        crushingSlab();
        sluice();
        slurryCauldron();
    }

    // -------------------------------------------------------------------------
    // Crushing Slab: a heavy basin, floor at 14, with a two pixel rim
    // -------------------------------------------------------------------------

    private void crushingSlab() {
        BlockModelBuilder base = models().getBuilder("block/crushing_slab")
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", modLoc("block/crushing_slab_side"))
                .texture("side", modLoc("block/crushing_slab_side"))
                .texture("rim", modLoc("block/crushing_slab_rim"))
                .texture("floor", modLoc("block/crushing_slab_floor"))
                .texture("bottom", modLoc("block/crushing_slab_bottom"));

        base.element().from(0, 0, 0).to(16, 14, 16)
                .face(Direction.DOWN).texture("#bottom").cullface(Direction.DOWN).end()
                .face(Direction.UP).texture("#floor").end()
                .face(Direction.NORTH).texture("#side").uvs(0, 2, 16, 16).cullface(Direction.NORTH).end()
                .face(Direction.SOUTH).texture("#side").uvs(0, 2, 16, 16).cullface(Direction.SOUTH).end()
                .face(Direction.WEST).texture("#side").uvs(0, 2, 16, 16).cullface(Direction.WEST).end()
                .face(Direction.EAST).texture("#side").uvs(0, 2, 16, 16).cullface(Direction.EAST).end()
                .end();
        rim(base, 0, 0, 16, 2);
        rim(base, 0, 14, 16, 16);
        rim(base, 0, 2, 2, 14);
        rim(base, 14, 2, 16, 14);

        MultiPartBlockStateBuilder states = getMultipartBuilder(RegistryHandler.CRUSHING_SLAB.get());
        states.part().modelFile(base).addModel().end();
        for (int stage = 1; stage <= CrushingSlabBlock.CRACK_STAGES; stage++) {
            BlockModelBuilder crack = models().getBuilder("block/crushing_slab_crack_" + stage)
                    .texture("particle", modLoc("block/crushing_slab_side"))
                    .texture("crack", modLoc("block/crushing_slab_crack_" + stage))
                    .renderType("cutout");
            crack.element().from(2, 14.01F, 2).to(14, 14.01F, 14)
                    .face(Direction.UP).texture("#crack").uvs(2, 2, 14, 14).end()
                    .end();
            states.part().modelFile(crack).addModel().condition(CrushingSlabBlock.CRACK, stage).end();
        }
    }

    private static void rim(BlockModelBuilder model, float x1, float z1, float x2, float z2) {
        model.element().from(x1, 14, z1).to(x2, 16, z2)
                .face(Direction.UP).texture("#rim").cullface(Direction.UP).end()
                .face(Direction.NORTH).texture("#side").uvs(x1, 0, x2, 2).end()
                .face(Direction.SOUTH).texture("#side").uvs(x1, 0, x2, 2).end()
                .face(Direction.WEST).texture("#side").uvs(z1, 0, z2, 2).end()
                .face(Direction.EAST).texture("#side").uvs(z1, 0, z2, 2).end()
                .end();
    }

    // -------------------------------------------------------------------------
    // Sluice: three wooden steps falling north, a riffle on each tread
    // -------------------------------------------------------------------------

    private void sluice() {
        BlockModelBuilder model = models().getBuilder("block/sluice")
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", modLoc("block/sluice_planks"))
                .texture("planks", modLoc("block/sluice_planks"))
                .texture("riffle", modLoc("block/sluice_riffle"));

        step(model, 0, 3);
        step(model, 6, 4.5F);
        step(model, 11, 6);
        riffle(model, 1, 3);
        riffle(model, 6, 4.5F);
        riffle(model, 11, 6);
        rail(model, 0, 1);
        rail(model, 15, 16);

        horizontalBlock(RegistryHandler.SLUICE.get(), model);
    }

    private static void step(BlockModelBuilder model, float zFrom, float height) {
        model.element().from(0, 0, zFrom).to(16, height, 16)
                .allFaces((direction, face) -> face.texture("#planks"))
                .end();
    }

    private static void riffle(BlockModelBuilder model, float z, float tread) {
        model.element().from(1, tread, z).to(15, tread + 1.5F, z + 1)
                .allFaces((direction, face) -> face.texture("#riffle"))
                .end();
    }

    private static void rail(BlockModelBuilder model, float x1, float x2) {
        model.element().from(x1, 3, 0).to(x2, 8, 16)
                .allFaces((direction, face) -> face.texture("#planks"))
                .end();
    }

    // -------------------------------------------------------------------------
    // Slurry cauldron: the vanilla cauldron model plus a tinted surface and crystals
    // -------------------------------------------------------------------------

    private void slurryCauldron() {
        ModelFile cauldron = new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/cauldron"));
        MultiPartBlockStateBuilder states = getMultipartBuilder(RegistryHandler.SLURRY_CAULDRON.get());
        states.part().modelFile(cauldron).addModel().end();

        states.part().modelFile(surface("slurry_cauldron_murky", "block/slurry_murky", 0, 15.0F)).addModel()
                .condition(SlurryCauldronBlock.PHASE, 0).end();
        states.part().modelFile(surface("slurry_cauldron_still", "block/slurry_still", 0, 15.0F)).addModel()
                .condition(SlurryCauldronBlock.PHASE, 1, 2, 3, 4).end();
        for (int stage = 1; stage <= 3; stage++) {
            states.part().modelFile(surface("slurry_cauldron_crystals_" + stage, "block/slurry_crystals_" + stage, 1, 15.02F))
                    .addModel().condition(SlurryCauldronBlock.PHASE, stage + 1).end();
        }
    }

    private BlockModelBuilder surface(String name, String texture, int tint, float height) {
        BlockModelBuilder model = models().getBuilder("block/" + name)
                .texture("particle", ResourceLocation.withDefaultNamespace("block/cauldron_side"))
                .texture("surface", modLoc(texture))
                .renderType("cutout");
        model.element().from(2, height, 2).to(14, height, 14)
                .face(Direction.UP).texture("#surface").uvs(2, 2, 14, 14).tintindex(tint).end()
                .end();
        return model;
    }
}
