package com.thecascadian.universaloreprocessing.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.MachineBlock;
import com.thecascadian.universaloreprocessing.block.MachineBlockEntity;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.block.MachineMenu;
import com.thecascadian.universaloreprocessing.item.GuideBookItem;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.item.ReagentItem;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.material.OreProcessingGameTests;
import com.thecascadian.universaloreprocessing.process.Reagent;
import com.thecascadian.universaloreprocessing.recipe.CrushRecipe;
import com.thecascadian.universaloreprocessing.recipe.WashRecipe;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;


@EventBusSubscriber(modid = UniversalOreProcessing.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class RegistryHandler {

    private RegistryHandler() {
    }

    public static final TagKey<Item> NON_PROCESSABLE_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(UniversalOreProcessing.MODID, "non_processable"));

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

    // -------------------------------------------------------------------------
    // Blocks
    // -------------------------------------------------------------------------

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK,
            UniversalOreProcessing.MODID);

    public static final Map<MachineKind, DeferredHolder<Block, MachineBlock>> MACHINE_BLOCKS = new EnumMap<>(
            MachineKind.class);

    static {
        for (MachineKind kind : MachineKind.values()) {
            DeferredHolder<Block, MachineBlock> block = BLOCKS.register(kind.id(), () -> machineBlock(kind));
            MACHINE_BLOCKS.put(kind, block);
        }
    }

    public static final DeferredHolder<Block, MachineBlock> ORE_CRUSHER = MACHINE_BLOCKS.get(MachineKind.CRUSHER);
    public static final DeferredHolder<Block, MachineBlock> ORE_WASHER = MACHINE_BLOCKS.get(MachineKind.WASHER);
    public static final DeferredHolder<Block, MachineBlock> ORE_SMELTER = MACHINE_BLOCKS.get(MachineKind.SMELTER);

    // -------------------------------------------------------------------------
    // Items
    // -------------------------------------------------------------------------

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM,
            UniversalOreProcessing.MODID);

    public static final Map<MachineKind, DeferredHolder<Item, BlockItem>> MACHINE_ITEMS = new EnumMap<>(
            MachineKind.class);
    public static final Map<MaterialItem.Stage, DeferredHolder<Item, MaterialItem>> STAGE_ITEMS = new EnumMap<>(
            MaterialItem.Stage.class);
    public static final Map<Reagent, DeferredHolder<Item, ReagentItem>> REAGENT_ITEMS = new EnumMap<>(Reagent.class);

    static {
        for (MachineKind kind : MachineKind.values()) {
            DeferredHolder<Block, MachineBlock> block = MACHINE_BLOCKS.get(kind);
            MACHINE_ITEMS.put(kind, ITEMS.register(kind.id(), () -> new BlockItem(block.get(), new Item.Properties())));
        }
        for (MaterialItem.Stage stage : MaterialItem.Stage.values()) {
            STAGE_ITEMS.put(stage, ITEMS.register(stage.itemId(),
                    () -> new MaterialItem(stage, new Item.Properties())));
        }
        for (Reagent reagent : Reagent.values()) {
            REAGENT_ITEMS.put(reagent, ITEMS.register(reagent.itemId(),
                    () -> new ReagentItem(reagent, new Item.Properties())));
        }
    }

    public static final DeferredHolder<Item, GuideBookItem> GUIDE_BOOK = ITEMS.register("refinery_guide",
            () -> new GuideBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, BlockItem> ORE_CRUSHER_ITEM = MACHINE_ITEMS.get(MachineKind.CRUSHER);
    public static final DeferredHolder<Item, BlockItem> ORE_WASHER_ITEM = MACHINE_ITEMS.get(MachineKind.WASHER);
    public static final DeferredHolder<Item, BlockItem> ORE_SMELTER_ITEM = MACHINE_ITEMS.get(MachineKind.SMELTER);

    public static final DeferredHolder<Item, MaterialItem> CRUSHED_ORE = STAGE_ITEMS.get(MaterialItem.Stage.CRUSHED);
    public static final DeferredHolder<Item, MaterialItem> PURIFIED_ORE = STAGE_ITEMS.get(MaterialItem.Stage.PURIFIED);
    public static final DeferredHolder<Item, MaterialItem> MATERIAL_DUST = STAGE_ITEMS.get(MaterialItem.Stage.DUST);

    // -------------------------------------------------------------------------
    // Block entities and menus
    // -------------------------------------------------------------------------

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, UniversalOreProcessing.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("machine", () -> BlockEntityType.Builder
                    .of(MachineBlockEntity::new, MACHINE_BLOCKS.values().stream()
                            .map(DeferredHolder::get).toArray(Block[]::new))
                    .build(null));

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,
            UniversalOreProcessing.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU = MENUS
            .register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));

    // -------------------------------------------------------------------------
    // Creative tab
    // -------------------------------------------------------------------------

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            UniversalOreProcessing.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORE_PROCESSING_TAB = TABS
            .register("ore_processing", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + UniversalOreProcessing.MODID))
                    .icon(() -> new ItemStack(ORE_CRUSHER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(GUIDE_BOOK.get());
                        for (MachineKind kind : MachineKind.values()) {
                            if (OreProcessingConfig.tierEnabled(kind))
                                output.accept(MACHINE_ITEMS.get(kind).get());
                        }
                        for (Reagent reagent : Reagent.values()) {
                            output.accept(reagent.item());
                        }
                        for (String materialId : MaterialRegistry.current().materials().keySet()) {
                            output.accept(MaterialItem.create(MaterialItem.Stage.CRUSHED, materialId, 1));
                            output.accept(MaterialItem.create(MaterialItem.Stage.PURIFIED, materialId, 1));
                            output.accept(MaterialItem.create(MaterialItem.Stage.DUST, materialId, 1));
                        }
                    })
                    .build());

    // every other stage item, one generic stack each unless creative_all_stages lists them per material
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STAGES_TAB = TABS
            .register("refinery_stages", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + UniversalOreProcessing.MODID + ".stages"))
                    .icon(() -> new ItemStack(MaterialItem.Stage.CATHODE.item()))
                    .displayItems((params, output) -> {
                        boolean perMaterial = OreProcessingConfig.get(OreProcessingConfig.COMMON.creativeAllStages);
                        for (MaterialItem.Stage stage : MaterialItem.Stage.values()) {
                            if (stage == MaterialItem.Stage.CRUSHED || stage == MaterialItem.Stage.PURIFIED
                                    || stage == MaterialItem.Stage.DUST)
                                continue;
                            if (!perMaterial) {
                                output.accept(stage.item());
                                continue;
                            }
                            for (String materialId : MaterialRegistry.current().materials().keySet()) {
                                output.accept(MaterialItem.create(stage, materialId, 1));
                            }
                        }
                    })
                    .build());

    // -------------------------------------------------------------------------
    // Recipe types and serializers
    // -------------------------------------------------------------------------

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE,
            UniversalOreProcessing.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, UniversalOreProcessing.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushRecipe>> CRUSH_TYPE = RECIPE_TYPES.register(
            "crush",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(UniversalOreProcessing.MODID, "crush")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<WashRecipe>> WASH_TYPE = RECIPE_TYPES.register(
            "wash",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(UniversalOreProcessing.MODID, "wash")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushRecipe>> CRUSH_SERIALIZER = RECIPE_SERIALIZERS
            .register("crush", () -> new RecipeSerializer<CrushRecipe>() {
                @Override
                public MapCodec<CrushRecipe> codec() {
                    return CrushRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, CrushRecipe> streamCodec() {
                    return CrushRecipe.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WashRecipe>> WASH_SERIALIZER = RECIPE_SERIALIZERS
            .register("wash", () -> new RecipeSerializer<WashRecipe>() {
                @Override
                public MapCodec<WashRecipe> codec() {
                    return WashRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, WashRecipe> streamCodec() {
                    return WashRecipe.STREAM_CODEC;
                }
            });

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    public static void init(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        TABS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }

    // -------------------------------------------------------------------------
    // Capabilities and game tests
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        BlockEntityType<MachineBlockEntity> type = MACHINE_BLOCK_ENTITY.get();
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (machine, side) -> machine.itemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (machine, side) -> machine.fluidHandler());
        // the provider returns null unless use_energy is enabled, so the capability appears only when configured
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (machine, side) -> machine.energyStorage());
    }

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(OreProcessingGameTests.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static MachineBlock machineBlock(MachineKind kind) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0);
        return new MachineBlock(properties, kind);
    }
}
