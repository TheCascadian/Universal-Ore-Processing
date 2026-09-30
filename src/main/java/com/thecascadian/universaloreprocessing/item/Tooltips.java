package com.thecascadian.universaloreprocessing.item;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.MaterialItem.Stage;
import com.thecascadian.universaloreprocessing.material.MaterialTrait;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import com.thecascadian.universaloreprocessing.process.Reagent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plain-language names and tooltips shared by the items, the machines and the guide
 * book. Every "made in" and "used in" line is computed from the processing rules, so
 * the text can never disagree with what the machines actually do.
 */
public final class Tooltips {

    private Tooltips() {
    }

    // -------------------------------------------------------------------------
    // Names
    // -------------------------------------------------------------------------

    public static Component stageName(Stage stage) {
        return Component.translatable("item." + UniversalOreProcessing.MODID + "." + stage.itemId() + ".generic");
    }

    public static Component reagentName(Reagent reagent) {
        return Component.translatable("item." + UniversalOreProcessing.MODID + "." + reagent.itemId());
    }

    public static Component machineName(MachineKind kind) {
        return Component.translatable("block." + UniversalOreProcessing.MODID + "." + kind.id());
    }

    public static Component traitName(MaterialTrait trait) {
        return Component.translatable("trait." + UniversalOreProcessing.MODID + "."
                + trait.name().toLowerCase(Locale.ROOT));
    }

    public static Component join(List<? extends Component> parts) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0)
                out.append(Component.literal(", "));
            out.append(parts.get(i));
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Relations computed from the rules
    // -------------------------------------------------------------------------

    /** Active stations that return the stage, including the base machines. */
    public static List<MachineKind> producers(Stage stage) {
        List<MachineKind> out = new ArrayList<>();
        if (stage == Stage.CRUSHED)
            out.add(MachineKind.CRUSHER);
        if (stage == Stage.PURIFIED)
            out.add(MachineKind.WASHER);
        if (stage == Stage.DUST)
            out.add(MachineKind.CRUSHER);
        for (Map.Entry<MachineKind, ProcessRule> entry : ProcessRules.all().entrySet()) {
            ProcessRule rule = entry.getValue();
            boolean makes = rule.primary().stage() == stage
                    || (rule.secondary() != null && rule.secondary().stage() == stage);
            if (makes && OreProcessingConfig.tierEnabled(entry.getKey()))
                out.add(entry.getKey());
        }
        return out;
    }

    /** Active stations that accept the stage as input, including the base machines. */
    public static List<MachineKind> consumers(Stage stage) {
        List<MachineKind> out = new ArrayList<>();
        if (stage == Stage.CRUSHED)
            out.add(MachineKind.WASHER);
        if (stage.isWaste())
            out.add(MachineKind.CRUSHER);
        if (stage.isSmeltable())
            out.add(MachineKind.SMELTER);
        for (Map.Entry<MachineKind, ProcessRule> entry : ProcessRules.all().entrySet()) {
            if (entry.getValue().inputs().contains(stage) && OreProcessingConfig.tierEnabled(entry.getKey()))
                out.add(entry.getKey());
        }
        return out;
    }

    public static List<MachineKind> reagentUsers(Reagent reagent) {
        List<MachineKind> out = new ArrayList<>();
        for (Map.Entry<MachineKind, ProcessRule> entry : ProcessRules.all().entrySet()) {
            ProcessRule rule = entry.getValue();
            boolean uses = rule.reagent() == reagent
                    || (rule.secondary() != null && rule.secondary().reagent() == reagent);
            if (uses && OreProcessingConfig.tierEnabled(entry.getKey()))
                out.add(entry.getKey());
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Tooltips
    // -------------------------------------------------------------------------

    private static boolean shift() {
        return Screen.hasShiftDown();
    }

    private static void hint(List<Component> out) {
        out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".shift")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static void line(List<Component> out, String key, Component value) {
        out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + "." + key)
                .withStyle(ChatFormatting.GOLD).append(value.copy().withStyle(ChatFormatting.GRAY)));
    }

    private static List<Component> names(List<MachineKind> kinds) {
        List<Component> out = new ArrayList<>();
        for (MachineKind kind : kinds)
            out.add(machineName(kind));
        return out;
    }

    public static void stage(Stage stage, List<Component> out) {
        out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".stage." + stage.itemId())
                .withStyle(ChatFormatting.GRAY));
        if (!shift()) {
            hint(out);
            return;
        }
        List<MachineKind> made = producers(stage);
        if (!made.isEmpty())
            line(out, "made_in", join(names(made)));
        List<MachineKind> used = consumers(stage);
        if (!used.isEmpty())
            line(out, "goes_into", join(names(used)));
        if (stage.isSmeltable())
            line(out, "smelts_to", Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".ingots",
                    stage.smeltCount()));
        if (stage.isWaste())
            out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".waste")
                    .withStyle(ChatFormatting.DARK_AQUA));
    }

    public static void reagent(Reagent reagent, List<Component> out) {
        out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".reagent." + reagent.itemId())
                .withStyle(ChatFormatting.GRAY));
        if (!shift()) {
            hint(out);
            return;
        }
        List<MachineKind> users = reagentUsers(reagent);
        if (!users.isEmpty())
            line(out, "used_by", join(names(users)));
        if (reagent == Reagent.COKE)
            out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".fuel")
                    .withStyle(ChatFormatting.DARK_AQUA));
    }

    public static void machine(MachineKind kind, List<Component> out) {
        out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".machine." + kind.id())
                .withStyle(ChatFormatting.GRAY));
        if (!shift()) {
            hint(out);
            return;
        }
        line(out, "tier", Component.literal(kind.tier() == 0 ? "-" : String.valueOf(kind.tier())));
        ProcessRule rule = ProcessRules.get(kind);
        if (rule == null)
            return;

        List<Component> inputs = new ArrayList<>();
        for (Stage stage : rule.inputs())
            inputs.add(stageName(stage));
        line(out, "takes", join(inputs));
        if (rule.reagent() != null)
            line(out, "needs", reagentName(rule.reagent()));
        if (kind.usesWater())
            line(out, "needs", Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".water"));
        if (rule.primary().stage() != null) {
            List<Component> makes = new ArrayList<>();
            makes.add(stageName(rule.primary().stage()));
            if (rule.secondary() != null) {
                makes.add(rule.secondary().stage() != null ? stageName(rule.secondary().stage())
                        : reagentName(rule.secondary().reagent()));
            }
            line(out, "makes", join(makes));
        }
        if (!rule.traits().isEmpty()) {
            List<Component> traits = new ArrayList<>();
            for (MaterialTrait trait : rule.traits())
                traits.add(traitName(trait));
            line(out, "works_on", join(traits));
        }
    }
}
