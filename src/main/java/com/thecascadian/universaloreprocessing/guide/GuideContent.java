package com.thecascadian.universaloreprocessing.guide;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.SettlingTankBlock;
import com.thecascadian.universaloreprocessing.block.SlurryCauldronBlock;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.LadderTables;
import com.thecascadian.universaloreprocessing.ladder.Waste;
import com.thecascadian.universaloreprocessing.ladder.Yields;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The text and pictures of the in-game guide book. Prose is lang text; the
 * numbers in it (strikes, stirs, settling time, ratios) are read from the
 * config and datapack tables, so the book always matches the world it is
 * read in. Each step chapter ends with the mistakes its hints point to.
 */
public final class GuideContent {

    /** One piece of a chapter: wrapped text, a row of items, or a block layout. */
    public sealed interface Element permits Text, Flow, Diagram {
    }

    public record Text(Component text) implements Element {
    }

    /** Items read left to right; an empty stack is drawn as an arrow. */
    public record Flow(List<ItemStack> stacks) implements Element {
    }

    /** A side view, top row first; an empty stack is open air. The caption explains the layout. */
    public record Diagram(List<List<ItemStack>> rows, Component caption) implements Element {
    }

    public record Chapter(Component title, List<Element> elements) {
    }

    private static final String KEY = "guide." + UniversalOreProcessing.MODID + ".";
    private static final ItemStack ARROW = ItemStack.EMPTY;
    private static final ItemStack AIR = ItemStack.EMPTY;

    private GuideContent() {
    }

    public static List<Chapter> build() {
        String material = exampleMaterial();
        ItemStack raw = rawOf(material);
        ItemStack clumps = FormItem.create(Form.CLUMPS, material, 1);
        ItemStack dust = FormItem.create(Form.DUST, material, 1);
        ItemStack shards = FormItem.create(Form.SHARDS, material, 1);

        List<Chapter> chapters = new ArrayList<>();
        chapters.add(intro(raw, clumps, dust, shards));
        chapters.add(strike(raw, clumps));
        chapters.add(grind(clumps, dust));
        chapters.add(stir(dust));
        chapters.add(settle(shards));
        chapters.add(wash(clumps));
        chapters.add(smelt(raw, clumps, dust, shards));
        chapters.add(troubleshooting());
        return chapters;
    }

    // -------------------------------------------------------------------------
    // Chapters
    // -------------------------------------------------------------------------

    private static Chapter intro(ItemStack raw, ItemStack clumps, ItemStack dust, ItemStack shards) {
        return chapter("intro",
                text("intro.1"),
                flow(raw, ARROW, clumps, ARROW, dust, ARROW, stack(Items.CAULDRON), ARROW, shards),
                text("intro.2"),
                text("intro.3"),
                text("intro.4"));
    }

    private static Chapter strike(ItemStack raw, ItemStack clumps) {
        int strikes = OreProcessingConfig.get(OreProcessingConfig.COMMON.strikesPerItem);
        return chapter("strike",
                text("strike.1"),
                flow(raw, stack(RegistryHandler.CRUSHING_SLAB_ITEM.get()), stack(RegistryHandler.HAMMER.get()), ARROW, clumps),
                text("strike.2", strikes),
                waste("strike", Waste.GRAVEL),
                heading(RegistryHandler.TRIP_HAMMER_ITEM.get()),
                text("strike.3"),
                diagram("strike.diagram.trip_hammer",
                        row(Items.LEVER, RegistryHandler.TRIP_HAMMER_ITEM.get()),
                        row(null, RegistryHandler.CRUSHING_SLAB_ITEM.get())),
                text("strike.4"),
                diagram("strike.diagram.anvil",
                        row(Items.ANVIL),
                        row((ItemLike) null),
                        row(RegistryHandler.CRUSHING_SLAB_ITEM.get())),
                mistakes("strike", 3));
    }

    private static Chapter grind(ItemStack clumps, ItemStack dust) {
        return chapter("grind",
                text("grind.1"),
                flow(clumps, stack(Items.GRINDSTONE), ARROW, dust),
                text("grind.2"),
                waste("grind", Waste.SAND),
                heading(RegistryHandler.QUERN_ITEM.get()),
                text("grind.3"),
                diagram("grind.diagram.quern",
                        row(Items.HOPPER),
                        row(RegistryHandler.QUERN_ITEM.get(), Items.LEVER),
                        row(Items.HOPPER)),
                text("grind.4"),
                mistakes("grind", 3));
    }

    private static Chapter stir(ItemStack dust) {
        int stirs = OreProcessingConfig.get(OreProcessingConfig.COMMON.stirsPerSlurry);
        return chapter("stir",
                text("stir.1"),
                flow(stack(Items.WATER_BUCKET), stack(Items.CAULDRON), dust, stack(Items.STICK)),
                text("stir.2", SlurryCauldronBlock.CAPACITY, stirs),
                heading(RegistryHandler.STIRRING_PADDLE_ITEM.get()),
                text("stir.3"),
                diagram("stir.diagram.paddle",
                        row(Items.LEVER, RegistryHandler.STIRRING_PADDLE_ITEM.get()),
                        row(null, Items.CAULDRON)),
                mistakes("stir", 3));
    }

    private static Chapter settle(ItemStack shards) {
        int seconds = OreProcessingConfig.get(OreProcessingConfig.COMMON.settleTicks) / 20;
        return chapter("settle",
                text("settle.1", seconds),
                flow(stack(Items.CAULDRON), stack(Items.CLOCK), ARROW, shards),
                text("settle.2"),
                waste("settle", Waste.CLAY),
                heading(RegistryHandler.SETTLING_TANK_ITEM.get()),
                text("settle.3", SettlingTankBlock.CAPACITY),
                diagram("settle.diagram.tank",
                        row(Items.HOPPER, RegistryHandler.STIRRING_PADDLE_ITEM.get()),
                        row(null, RegistryHandler.SETTLING_TANK_ITEM.get()),
                        row(null, Items.HOPPER)),
                text("settle.4"),
                mistakes("settle", 3));
    }

    private static Chapter wash(ItemStack clumps) {
        int maxRow = OreProcessingConfig.get(OreProcessingConfig.COMMON.sluiceMaxRow);
        List<Element> elements = new ArrayList<>();
        elements.add(text("wash.1"));
        ItemLike sluice = RegistryHandler.SLUICE_ITEM.get();
        elements.add(diagram("wash.diagram.row",
                row(Items.WATER_BUCKET, Items.WATER_BUCKET, Items.WATER_BUCKET, Items.WATER_BUCKET),
                row(sluice, sluice, sluice, sluice)));
        elements.add(text("wash.2", maxRow));
        elements.add(byproducts());
        elements.add(heading(RegistryHandler.PANNING_TRAY.get()));
        elements.add(text("wash.3"));
        elements.add(flow(stack(RegistryHandler.PANNING_TRAY.get()), clumps, stack(Items.WATER_BUCKET), ARROW,
                stack(Items.FLINT)));
        elements.add(mistakes("wash", 3));
        return new Chapter(text("chapter.wash").text(), elements);
    }

    private static Chapter smelt(ItemStack raw, ItemStack clumps, ItemStack dust, ItemStack shards) {
        LadderTables.Ratios ratios = LadderTables.ratios();
        return chapter("smelt",
                text("smelt.1"),
                flow(raw, stack(Items.FURNACE), ARROW, outputOf(exampleMaterial())),
                text("smelt.ratio", raw.getHoverName(), Yields.format(ratios.of(Form.RAW))),
                text("smelt.ratio", clumps.getHoverName(), Yields.format(ratios.of(Form.CLUMPS))),
                text("smelt.ratio", dust.getHoverName(), Yields.format(ratios.of(Form.DUST))),
                text("smelt.ratio", shards.getHoverName(), Yields.format(ratios.of(Form.SHARDS))),
                text("smelt.2"),
                text("smelt.3"));
    }

    private static Chapter troubleshooting() {
        List<Element> elements = new ArrayList<>();
        elements.add(text("trouble.intro"));
        for (int i = 1; i <= 8; i++) {
            elements.add(new Text(text("trouble." + i + ".problem").text().copy().withStyle(ChatFormatting.DARK_RED)));
            elements.add(text("trouble." + i + ".fix"));
        }
        return new Chapter(text("chapter.trouble").text(), elements);
    }

    // -------------------------------------------------------------------------
    // Pieces
    // -------------------------------------------------------------------------

    private static Chapter chapter(String id, Element... elements) {
        // null elements are pieces switched off by the tables, such as a waste rate of zero
        return new Chapter(text("chapter." + id).text(), Arrays.stream(elements).filter(Objects::nonNull).toList());
    }

    /** The rock a step leaves behind, or null when the tables turn that waste off. */
    private static Element waste(String chapter, Waste waste) {
        int every = waste.every();
        return every > 0 ? text(chapter + ".waste", every, new ItemStack(waste.item()).getHoverName()) : null;
    }

    private static Text text(String key, Object... args) {
        return new Text(Component.translatable(KEY + key, args));
    }

    private static Text heading(Item item) {
        return new Text(item.getDescription().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /** The red "Common mistakes" block that closes a step chapter. */
    private static Element mistakes(String chapter, int count) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "mistakes").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        for (int i = 1; i <= count; i++)
            lines.add(Component.literal("- ").append(Component.translatable(KEY + chapter + ".mistake." + i)));
        Component joined = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0)
                joined = joined.copy().append("\n");
            joined = joined.copy().append(lines.get(i));
        }
        return new Text(joined);
    }

    private static Element byproducts() {
        String material = exampleMaterial();
        List<LadderTables.Byproduct> list = LadderTables.byproductsFor(material);
        if (list.isEmpty())
            return text("wash.byproducts.none", FormItem.materialName(material));
        StringBuilder entries = new StringBuilder();
        for (LadderTables.Byproduct byproduct : list) {
            if (!entries.isEmpty())
                entries.append(", ");
            entries.append(byproduct.id().getPath()).append(' ')
                    .append(Yields.format(byproduct.chance() * 100.0D)).append('%');
        }
        return text("wash.byproducts", FormItem.materialName(material), entries.toString());
    }

    private static Flow flow(ItemStack... stacks) {
        return new Flow(Arrays.asList(stacks));
    }

    @SafeVarargs
    private static Diagram diagram(String captionKey, List<ItemStack>... rows) {
        return new Diagram(List.of(rows), Component.translatable(KEY + captionKey));
    }

    private static List<ItemStack> row(ItemLike... items) {
        List<ItemStack> row = new ArrayList<>();
        for (ItemLike item : items)
            row.add(item == null ? AIR : new ItemStack(item));
        return row;
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    private static ItemStack outputOf(String material) {
        return MaterialRegistry.current().get(material)
                .map(m -> new ItemStack(m.output()))
                .orElse(new ItemStack(Items.IRON_INGOT));
    }

    /** Iron when it is processed, else the first discovered material, so the pictures show real items. */
    private static String exampleMaterial() {
        MaterialRegistry registry = MaterialRegistry.current();
        if (registry.get("iron").isPresent() || registry.isEmpty())
            return "iron";
        return registry.materials().keySet().iterator().next();
    }

    private static ItemStack rawOf(String material) {
        return MaterialRegistry.current().get(material)
                .filter(m -> !m.rawItems().isEmpty())
                .map(m -> new ItemStack(m.rawItems().get(0)))
                .orElse(new ItemStack(Items.RAW_IRON));
    }
}
