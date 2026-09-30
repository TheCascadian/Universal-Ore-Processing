package com.thecascadian.universaloreprocessing.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.material.MaterialTraits;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import com.thecascadian.universaloreprocessing.process.Reagent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The /uop command tree: `dump` lists every discovered material with its
 * resolved output and writes the same report to logs/uop_dump.txt, `give`
 * hands out a stage item for a material so the machines can be tested quickly,
 * `reagent` hands out a support item, `stations` lists which refining stations
 * accept a material and why.
 */
@EventBusSubscriber(modid = UniversalOreProcessing.MODID)
public final class UopCommand {

    private static final int CHAT_LINE_LIMIT = 20;
    private static final SimpleCommandExceptionType UNKNOWN_MATERIAL = new SimpleCommandExceptionType(
            Component.literal("Unknown material. Run /uop dump to list discovered materials."));
    private static final SimpleCommandExceptionType UNKNOWN_STAGE = new SimpleCommandExceptionType(
            Component.literal("Unknown stage. Use one of the suggested stage names."));
    private static final SimpleCommandExceptionType UNKNOWN_REAGENT = new SimpleCommandExceptionType(
            Component.literal("Unknown reagent. Use one of the suggested reagent names."));

    private UopCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("uop")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dump").executes(UopCommand::dump))
                .then(Commands.literal("give")
                        .then(Commands.argument("material", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        MaterialRegistry.current().materials().keySet(), builder))
                                .then(Commands.argument("stage", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(MaterialItem.Stage.values())
                                                        .map(MaterialItem.Stage::commandName), builder))
                                        .executes(UopCommand::give))))
                .then(Commands.literal("reagent")
                        .then(Commands.argument("reagent", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Reagent.values()).map(Reagent::itemId), builder))
                                .executes(context -> giveReagent(context, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveReagent(context,
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("stations")
                        .then(Commands.argument("material", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        MaterialRegistry.current().materials().keySet(), builder))
                                .executes(UopCommand::stations))));
    }

    private static int dump(CommandContext<CommandSourceStack> context) {
        MaterialRegistry registry = MaterialRegistry.current();
        List<String> lines = new ArrayList<>();
        lines.add("Discovered materials: " + registry.materials().size());
        lines.add("Dynamic recipes: crush=" + registry.crushRecipeCount()
                + " wash=" + registry.washRecipeCount()
                + " smelt=" + registry.smeltRecipeCount());
        for (MaterialRegistry.Material material : registry.materials().values()) {
            lines.add(material.id() + " -> " + BuiltInRegistries.ITEM.getKey(material.output())
                    + " (ores=" + material.oreItems().size() + ", raw=" + material.rawItems().size()
                    + ", traits=" + traitList(material.id()) + ")");
        }

        Path file = FMLPaths.GAMEDIR.get().resolve("logs").resolve("uop_dump.txt");
        String fileNote;
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
            fileNote = "Full report written to logs/uop_dump.txt";
        } catch (IOException e) {
            UniversalOreProcessing.LOGGER.error("[UniversalOreProcessing] Could not write {}", file, e);
            fileNote = "Could not write logs/uop_dump.txt, see the server log for details";
        }

        int shown = Math.min(lines.size(), CHAT_LINE_LIMIT);
        for (int i = 0; i < shown; i++) {
            String line = lines.get(i);
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        if (lines.size() > shown) {
            int hidden = lines.size() - shown;
            context.getSource().sendSuccess(() -> Component.literal("... " + hidden + " more line(s)"), false);
        }
        String note = fileNote;
        context.getSource().sendSuccess(() -> Component.literal(note), false);
        return registry.materials().size();
    }

    private static int give(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String materialId = StringArgumentType.getString(context, "material");
        if (MaterialRegistry.current().get(materialId).isEmpty())
            throw UNKNOWN_MATERIAL.create();

        MaterialItem.Stage stage = MaterialItem.Stage.byCommandName(StringArgumentType.getString(context, "stage"))
                .orElseThrow(UNKNOWN_STAGE::create);

        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = MaterialItem.create(stage, materialId, 1);
        if (!player.getInventory().add(stack.copy()))
            player.drop(stack, false);

        context.getSource().sendSuccess(() -> Component.literal("Gave 1 " + stage.commandName() + " " + materialId),
                true);
        return 1;
    }

    private static int giveReagent(CommandContext<CommandSourceStack> context, int count)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "reagent");
        Reagent reagent = Arrays.stream(Reagent.values()).filter(r -> r.itemId().equals(name)).findFirst()
                .orElseThrow(UNKNOWN_REAGENT::create);

        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(reagent.item(), count);
        if (!player.getInventory().add(stack.copy()))
            player.drop(stack, false);

        context.getSource().sendSuccess(() -> Component.literal("Gave " + count + " " + name), true);
        return count;
    }

    private static int stations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String materialId = StringArgumentType.getString(context, "material");
        if (MaterialRegistry.current().get(materialId).isEmpty())
            throw UNKNOWN_MATERIAL.create();

        context.getSource().sendSuccess(() -> Component.literal(materialId + " traits: " + traitList(materialId)), false);
        int count = 0;
        for (Map.Entry<MachineKind, ProcessRule> entry : ProcessRules.all().entrySet()) {
            ProcessRule rule = entry.getValue();
            if (!MaterialTraits.matches(materialId, rule.traits()))
                continue;
            count++;
            String inputs = rule.inputs().stream().map(MaterialItem.Stage::commandName)
                    .collect(Collectors.joining(", "));
            String line = "tier " + entry.getKey().tier() + " " + entry.getKey().id() + " accepts " + inputs;
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return count;
    }

    private static String traitList(String materialId) {
        return MaterialTraits.of(materialId).stream().map(trait -> trait.name().toLowerCase(Locale.ROOT))
                .sorted().collect(Collectors.joining(","));
    }
}
