package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.CrushingSlabBlock;
import com.thecascadian.universaloreprocessing.block.QuernBlock;
import com.thecascadian.universaloreprocessing.block.SettlingTankBlock;
import com.thecascadian.universaloreprocessing.block.StirringPaddleBlock;
import com.thecascadian.universaloreprocessing.block.TripHammerBlock;
import com.thecascadian.universaloreprocessing.block.SlurryCauldronBlock;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Blockstates and block models. Every block is layered from vanilla textures,
 * so it follows whatever resource pack is active, and every overlay is its
 * own model, so a pack can replace any single piece.
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
        tripHammer();
        quern();
        stirringPaddle();
        settlingTank();
    }

    private static int yRot(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace("block/" + path);
    }

    // -------------------------------------------------------------------------
    // Crushing Slab: a stone brick basin, floor at 14, with a smooth stone rim.
    // As strikes land the floor wears from smooth stone toward cobblestone.
    // -------------------------------------------------------------------------

    private static final String[] WEAR = {"stone", "andesite", "cracked_stone_bricks", "cobblestone"};

    private void crushingSlab() {
        BlockModelBuilder base = models().getBuilder("block/crushing_slab")
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", mc("stone_bricks"))
                .texture("side", mc("stone_bricks"))
                .texture("rim", mc("smooth_stone"))
                .texture("floor", mc("smooth_stone"))
                .texture("bottom", mc("smooth_stone"));

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
                    .texture("particle", mc("stone_bricks"))
                    .texture("crack", mc(WEAR[stage - 1]));
            crack.element().from(2, 14.01F, 2).to(14, 14.01F, 14)
                    .face(Direction.UP).texture("#crack").uvs(2, 2, 14, 14).end()
                    .end();
            states.part().modelFile(crack).addModel().condition(CrushingSlabBlock.CRACK, stage).end();
        }
    }

    private static void rim(BlockModelBuilder model, float x1, float z1, float x2, float z2) {
        model.element().from(x1, 14, z1).to(x2, 16, z2)
                .face(Direction.UP).texture("#rim").cullface(Direction.UP).end()
                .face(Direction.NORTH).texture("#rim").uvs(x1, 0, x2, 2).end()
                .face(Direction.SOUTH).texture("#rim").uvs(x1, 0, x2, 2).end()
                .face(Direction.WEST).texture("#rim").uvs(z1, 0, z2, 2).end()
                .face(Direction.EAST).texture("#rim").uvs(z1, 0, z2, 2).end()
                .end();
    }

    // -------------------------------------------------------------------------
    // Sluice: an oak plank trough running north between two stripped oak rails,
    // three dark oak riffles across the floor
    // -------------------------------------------------------------------------

    private void sluice() {
        BlockModelBuilder model = models().getBuilder("block/sluice")
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", mc("oak_planks"))
                .texture("planks", mc("oak_planks"))
                .texture("rail", mc("stripped_oak_log"))
                .texture("riffle", mc("stripped_dark_oak_log"));

        // the floor is level so rows join without a lip; SluiceBlock.RIFFLES mirrors the riffle positions
        step(model, 0, 3);
        riffle(model, 2, 3);
        riffle(model, 7, 3);
        riffle(model, 12, 3);
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
                .allFaces((direction, face) -> face.texture("#rail"))
                .end();
    }

    // -------------------------------------------------------------------------
    // Slurry cauldron: the vanilla cauldron model plus a tinted water surface and crystals.
    // Unstirred slurry churns (flowing water), stirred slurry lies still (still water),
    // and crystals are tinted calcite growing up out of the surface.
    // -------------------------------------------------------------------------

    private void slurryCauldron() {
        ModelFile cauldron = new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/cauldron"));
        MultiPartBlockStateBuilder states = getMultipartBuilder(RegistryHandler.SLURRY_CAULDRON.get());
        states.part().modelFile(cauldron).addModel().end();

        states.part().modelFile(surface("slurry_cauldron_murky", mc("water_flow"), 0, 15.0F)).addModel()
                .condition(SlurryCauldronBlock.PHASE, 0).end();
        states.part().modelFile(surface("slurry_cauldron_still", mc("water_still"), 0, 15.0F)).addModel()
                .condition(SlurryCauldronBlock.PHASE, 1, 2, 3, 4).end();
        for (int stage = 1; stage <= 3; stage++) {
            states.part().modelFile(crystals("slurry_cauldron_crystals_" + stage, stage, 15.0F))
                    .addModel().condition(SlurryCauldronBlock.PHASE, stage + 1).end();
        }
    }

    private BlockModelBuilder surface(String name, ResourceLocation texture, int tint, float height) {
        BlockModelBuilder model = models().getBuilder("block/" + name)
                .texture("particle", mc("cauldron_side"))
                .texture("surface", texture)
                .renderType("translucent");
        model.element().from(2, height, 2).to(14, height, 14)
                .face(Direction.UP).texture("#surface").uvs(2, 2, 14, 14).tintindex(tint).end()
                .end();
        return model;
    }

    // x, z, height at the last stage, and lean; later stages add crystals and height
    private static final float[][] CLUSTERS = {
            {4, 5, 4, -22.5F}, {10, 3.5F, 3, 22.5F}, {7.5F, 10, 5, 0}, {11, 11, 3, -22.5F}, {3.5F, 11, 2, 22.5F}};

    private BlockModelBuilder crystals(String name, int stage, float height) {
        BlockModelBuilder model = models().getBuilder("block/" + name)
                .texture("particle", mc("calcite"))
                .texture("crystal", mc("calcite"));
        int count = new int[] {2, 4, 5}[stage - 1];
        for (int i = 0; i < count; i++) {
            float[] c = CLUSTERS[i];
            float tall = Math.max(1, c[2] - (3 - stage));
            Direction.Axis axis = i % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z;
            model.element().from(c[0], height - 1, c[1]).to(c[0] + 1.5F, height + tall, c[1] + 1.5F)
                    .rotation().origin(c[0] + 0.75F, height, c[1] + 0.75F).axis(axis).angle(c[3]).end()
                    .allFaces((direction, face) -> face.texture("#crystal").tintindex(1))
                    .end();
        }
        return model;
    }

    private static void box(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2,
            String texture) {
        model.element().from(x1, y1, z1).to(x2, y2, z2)
                .allFaces((direction, face) -> face.texture(texture))
                .end();
    }

    // -------------------------------------------------------------------------
    // Trip Hammer: two posts and a beam, the head raised or dropped onto the slab below
    // -------------------------------------------------------------------------

    private void tripHammer() {
        ModelFile up = tripHammerModel("trip_hammer_up", 8);
        ModelFile down = tripHammerModel("trip_hammer_down", 0);
        getVariantBuilder(RegistryHandler.TRIP_HAMMER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(TripHammerBlock.DOWN) ? down : up)
                .rotationY(yRot(state.getValue(TripHammerBlock.FACING)))
                .build());
    }

    private BlockModelBuilder tripHammerModel(String name, float headBottom) {
        BlockModelBuilder model = models().getBuilder("block/" + name)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", mc("stripped_spruce_log"))
                .texture("frame", mc("stripped_spruce_log"))
                .texture("head", mc("stone"))
                .texture("shaft", mc("stripped_oak_log"));
        box(model, 0, 0, 6, 2, 16, 10, "#frame");
        box(model, 14, 0, 6, 16, 16, 10, "#frame");
        box(model, 2, 14, 6, 14, 16, 10, "#frame");
        box(model, 4, headBottom, 4, 12, headBottom + 5, 12, "#head");
        box(model, 7, headBottom + 5, 7, 9, 14, 9, "#shaft");
        return model;
    }

    // -------------------------------------------------------------------------
    // Quern: a stone base with a spout, and a top stone whose peg moves a quarter turn per grind
    // -------------------------------------------------------------------------

    private void quern() {
        BlockModelBuilder base = models().getBuilder("block/quern_base")
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", mc("smooth_stone"));
        quernBase(base);

        BlockModelBuilder top = models().getBuilder("block/quern_top")
                .texture("particle", mc("polished_andesite"));
        quernTop(top);

        // the whole quern in one model, for the item
        BlockModelBuilder whole = models().getBuilder("block/quern")
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", mc("smooth_stone"));
        quernBase(whole);
        quernTop(whole);

        MultiPartBlockStateBuilder states = getMultipartBuilder(RegistryHandler.QUERN.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            states.part().modelFile(base).rotationY(yRot(facing)).addModel()
                    .condition(QuernBlock.FACING, facing).end();
        }
        for (int turn = 0; turn < 4; turn++) {
            states.part().modelFile(top).rotationY(turn * 90).addModel()
                    .condition(QuernBlock.TURN, turn).end();
        }
    }

    // the bed stone: a smooth stone slab, its sides cut from the lower half of the slab side
    private void quernBase(BlockModelBuilder model) {
        model.texture("bed", mc("smooth_stone")).texture("bed_side", mc("smooth_stone_slab_side"));
        model.element().from(1, 0, 1).to(15, 8, 15)
                .allFaces((direction, face) -> {
                    if (direction.getAxis().isVertical())
                        face.texture("#bed");
                    else
                        face.texture("#bed_side").uvs(1, 8, 15, 16);
                })
                .end();
        box(model, 7, 4, -1, 9, 6, 1, "#bed");
    }

    // the runner stone: polished andesite, turned by a stripped oak peg
    private void quernTop(BlockModelBuilder model) {
        model.texture("runner", mc("polished_andesite")).texture("peg", mc("stripped_oak_log"));
        box(model, 2, 8, 2, 14, 13, 14, "#runner");
        box(model, 10, 13, 7, 12, 19, 9, "#peg");
    }

    // -------------------------------------------------------------------------
    // Stirring Paddle: a bar across the rim and a paddle hanging into the vessel, swung left or right
    // -------------------------------------------------------------------------

    private void stirringPaddle() {
        ModelFile left = paddleModel("stirring_paddle_a", -22.5F);
        ModelFile right = paddleModel("stirring_paddle_b", 22.5F);
        getVariantBuilder(RegistryHandler.STIRRING_PADDLE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StirringPaddleBlock.SWING) ? right : left)
                .rotationY(yRot(state.getValue(StirringPaddleBlock.FACING)))
                .build());
    }

    private BlockModelBuilder paddleModel(String name, float angle) {
        BlockModelBuilder model = models().getBuilder("block/" + name)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", mc("stripped_oak_log"))
                .texture("bar", mc("stripped_oak_log"))
                .texture("paddle", mc("oak_planks"));
        box(model, 0, 0, 6, 16, 4, 10, "#bar");
        box(model, 7, 4, 7, 9, 8, 9, "#bar");
        model.element().from(7.5F, -10, 7.5F).to(8.5F, 4, 8.5F)
                .rotation().origin(8, 4, 8).axis(Direction.Axis.Z).angle(angle).end()
                .allFaces((direction, face) -> face.texture("#paddle"))
                .end();
        model.element().from(5, -12, 7.5F).to(11, -5, 8.5F)
                .rotation().origin(8, 4, 8).axis(Direction.Axis.Z).angle(angle).end()
                .allFaces((direction, face) -> face.texture("#paddle"))
                .end();
        return model;
    }

    // -------------------------------------------------------------------------
    // Settling Tank: an open barrel-stave vat with a stripped spruce rim;
    // water, slurry and crystals show on its surface
    // -------------------------------------------------------------------------

    private void settlingTank() {
        BlockModelBuilder body = models().getBuilder("block/settling_tank")
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", mc("barrel_side"))
                .texture("planks", mc("barrel_side"))
                .texture("floor", mc("barrel_bottom"))
                .texture("rim", mc("stripped_spruce_log"));
        tankWall(body, 0, 0, 16, 2);
        tankWall(body, 0, 14, 16, 16);
        tankWall(body, 0, 2, 2, 14);
        tankWall(body, 14, 2, 16, 14);
        box(body, 2, 0, 2, 14, 2, 14, "#floor");

        MultiPartBlockStateBuilder states = getMultipartBuilder(RegistryHandler.SETTLING_TANK.get());
        states.part().modelFile(body).addModel().end();
        states.part().modelFile(surface("settling_tank_water", mc("water_still"), 2, 14.0F)).addModel()
                .condition(SettlingTankBlock.STAGE, SettlingTankBlock.WATER).end();
        states.part().modelFile(surface("settling_tank_murky", mc("water_flow"), 0, 14.0F)).addModel()
                .condition(SettlingTankBlock.STAGE, SettlingTankBlock.MURKY).end();
        states.part().modelFile(surface("settling_tank_still", mc("water_still"), 0, 14.0F)).addModel()
                .condition(SettlingTankBlock.STAGE, 3, 4, 5, 6).end();
        for (int stage = 1; stage <= 3; stage++) {
            states.part().modelFile(crystals("settling_tank_crystals_" + stage, stage, 14.0F))
                    .addModel().condition(SettlingTankBlock.STAGE, SettlingTankBlock.STIRRED + stage).end();
        }
    }

    private static void tankWall(BlockModelBuilder model, float x1, float z1, float x2, float z2) {
        model.element().from(x1, 0, z1).to(x2, 16, z2)
                .allFaces((direction, face) -> face.texture(direction == Direction.UP ? "#rim" : "#planks"))
                .end();
    }
}
