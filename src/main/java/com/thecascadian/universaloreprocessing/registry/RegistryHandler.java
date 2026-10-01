package com.thecascadian.universaloreprocessing.registry;

import com.mojang.serialization.Codec;
import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.CrushingSlabBlock;
import com.thecascadian.universaloreprocessing.block.CrushingSlabBlockEntity;
import com.thecascadian.universaloreprocessing.block.GrindstoneFeed;
import com.thecascadian.universaloreprocessing.block.QuernBlock;
import com.thecascadian.universaloreprocessing.block.QuernBlockEntity;
import com.thecascadian.universaloreprocessing.block.SettlingTankBlock;
import com.thecascadian.universaloreprocessing.block.SettlingTankBlockEntity;
import com.thecascadian.universaloreprocessing.block.SluiceBlock;
import com.thecascadian.universaloreprocessing.block.SlurryCauldronBlock;
import com.thecascadian.universaloreprocessing.block.SlurryCauldronBlockEntity;
import com.thecascadian.universaloreprocessing.block.StirringPaddleBlock;
import com.thecascadian.universaloreprocessing.block.TripHammerBlock;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.item.GuideBookItem;
import com.thecascadian.universaloreprocessing.item.PanningTrayItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.LadderCookingRecipe;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.material.OreProcessingGameTests;
import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = UniversalOreProcessing.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class RegistryHandler {

    private RegistryHandler() {
    }

    // -------------------------------------------------------------------------
    // Tags
    // -------------------------------------------------------------------------

    public static final TagKey<Item> NON_PROCESSABLE_TAG = TagKey.create(Registries.ITEM, id("non_processable"));

    public static final TagKey<Item> HAMMERS_TAG = TagKey.create(Registries.ITEM, id("hammers"));

    public static final TagKey<Block> HEAVY_TAG = TagKey.create(Registries.BLOCK, id("heavy"));

    // -------------------------------------------------------------------------
    // Data components
    // -------------------------------------------------------------------------

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister
            .create(Registries.DATA_COMPONENT_TYPE, UniversalOreProcessing.MODID);

    // the material id, for example "iron" or "certus_quartz"
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MATERIAL_COMPONENT = DATA_COMPONENTS
            .register("material", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    // set once a stack has passed a Sluice row, so byproducts are rolled only once
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WASHED_COMPONENT = DATA_COMPONENTS
            .register("washed", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // -------------------------------------------------------------------------
    // Blocks
    // -------------------------------------------------------------------------

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK,
            UniversalOreProcessing.MODID);

    public static final DeferredHolder<Block, CrushingSlabBlock> CRUSHING_SLAB = BLOCKS.register(
            "crushing_slab", () -> new CrushingSlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredHolder<Block, SluiceBlock> SLUICE = BLOCKS.register(
            "sluice", () -> new SluiceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .noOcclusion()));

    public static final DeferredHolder<Block, SlurryCauldronBlock> SLURRY_CAULDRON = BLOCKS.register(
            "slurry_cauldron", () -> new SlurryCauldronBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)));

    public static final DeferredHolder<Block, TripHammerBlock> TRIP_HAMMER = BLOCKS.register(
            "trip_hammer", () -> new TripHammerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .noOcclusion()));

    public static final DeferredHolder<Block, QuernBlock> QUERN = BLOCKS.register(
            "quern", () -> new QuernBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredHolder<Block, StirringPaddleBlock> STIRRING_PADDLE = BLOCKS.register(
            "stirring_paddle", () -> new StirringPaddleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .noOcclusion()));

    public static final DeferredHolder<Block, SettlingTankBlock> SETTLING_TANK = BLOCKS.register(
            "settling_tank", () -> new SettlingTankBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .noOcclusion()));

    // -------------------------------------------------------------------------
    // Items
    // -------------------------------------------------------------------------

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM,
            UniversalOreProcessing.MODID);

    public static final DeferredHolder<Item, BlockItem> CRUSHING_SLAB_ITEM = ITEMS.register(
            "crushing_slab", () -> new BlockItem(CRUSHING_SLAB.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> SLUICE_ITEM = ITEMS.register(
            "sluice", () -> new BlockItem(SLUICE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> TRIP_HAMMER_ITEM = ITEMS.register(
            "trip_hammer", () -> new BlockItem(TRIP_HAMMER.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> QUERN_ITEM = ITEMS.register(
            "quern", () -> new BlockItem(QUERN.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> STIRRING_PADDLE_ITEM = ITEMS.register(
            "stirring_paddle", () -> new BlockItem(STIRRING_PADDLE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> SETTLING_TANK_ITEM = ITEMS.register(
            "settling_tank", () -> new BlockItem(SETTLING_TANK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, PanningTrayItem> PANNING_TRAY = ITEMS.register(
            "panning_tray", () -> new PanningTrayItem(new Item.Properties().durability(128)));

    public static final DeferredHolder<Item, GuideBookItem> GUIDE = ITEMS.register(
            "guide", () -> new GuideBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> HAMMER = ITEMS.register(
            "hammer", () -> new Item(new Item.Properties().durability(192)));

    public static final DeferredHolder<Item, FormItem> CLUMPS = ITEMS.register(
            "clumps", () -> new FormItem(Form.CLUMPS, new Item.Properties()));

    public static final DeferredHolder<Item, FormItem> DUST = ITEMS.register(
            "dust", () -> new FormItem(Form.DUST, new Item.Properties()));

    public static final DeferredHolder<Item, FormItem> SHARDS = ITEMS.register(
            "shards", () -> new FormItem(Form.SHARDS, new Item.Properties()));

    // -------------------------------------------------------------------------
    // Block entities
    // -------------------------------------------------------------------------

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, UniversalOreProcessing.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrushingSlabBlockEntity>> CRUSHING_SLAB_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("crushing_slab", () -> BlockEntityType.Builder
                    .of(CrushingSlabBlockEntity::new, CRUSHING_SLAB.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SlurryCauldronBlockEntity>> SLURRY_CAULDRON_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("slurry_cauldron", () -> BlockEntityType.Builder
                    .of(SlurryCauldronBlockEntity::new, SLURRY_CAULDRON.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuernBlockEntity>> QUERN_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("quern", () -> BlockEntityType.Builder
                    .of(QuernBlockEntity::new, QUERN.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SettlingTankBlockEntity>> SETTLING_TANK_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("settling_tank", () -> BlockEntityType.Builder
                    .of(SettlingTankBlockEntity::new, SETTLING_TANK.get())
                    .build(null));

    // -------------------------------------------------------------------------
    // Creative tab
    // -------------------------------------------------------------------------

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            UniversalOreProcessing.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORE_PROCESSING_TAB = TABS
            .register("ore_processing", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + UniversalOreProcessing.MODID))
                    .icon(() -> new ItemStack(CRUSHING_SLAB_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(GUIDE.get());
                        output.accept(HAMMER.get());
                        output.accept(CRUSHING_SLAB_ITEM.get());
                        output.accept(TRIP_HAMMER_ITEM.get());
                        output.accept(QUERN_ITEM.get());
                        output.accept(STIRRING_PADDLE_ITEM.get());
                        output.accept(SETTLING_TANK_ITEM.get());
                        output.accept(SLUICE_ITEM.get());
                        output.accept(PANNING_TRAY.get());
                        for (String materialId : MaterialRegistry.current().materials().keySet()) {
                            output.accept(FormItem.create(Form.CLUMPS, materialId, 1));
                            output.accept(FormItem.create(Form.DUST, materialId, 1));
                            output.accept(FormItem.create(Form.SHARDS, materialId, 1));
                        }
                    })
                    .build());

    // -------------------------------------------------------------------------
    // Recipe serializers
    // -------------------------------------------------------------------------

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, UniversalOreProcessing.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, LadderCookingRecipe.Serializer> LADDER_SMELTING_SERIALIZER = RECIPE_SERIALIZERS
            .register("ladder_smelting", () -> new LadderCookingRecipe.Serializer(() -> RecipeType.SMELTING, 200));

    public static final DeferredHolder<RecipeSerializer<?>, LadderCookingRecipe.Serializer> LADDER_BLASTING_SERIALIZER = RECIPE_SERIALIZERS
            .register("ladder_blasting", () -> new LadderCookingRecipe.Serializer(() -> RecipeType.BLASTING, 100));

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    public static void init(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }

    // -------------------------------------------------------------------------
    // Capabilities, payloads and game tests
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        // the vanilla grindstone gains an insert-only handler so hoppers can feed it; nothing is stored
        event.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> GrindstoneFeed.handlerFor(level, pos),
                Blocks.GRINDSTONE);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QUERN_BLOCK_ENTITY.get(),
                (quern, side) -> quern.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SETTLING_TANK_BLOCK_ENTITY.get(),
                (tank, side) -> tank.handlerFor(side));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        Feedback.register(event.registrar("1"));
    }

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(OreProcessingGameTests.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(UniversalOreProcessing.MODID, path);
    }
}
