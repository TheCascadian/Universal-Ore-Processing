package com.thecascadian.universaloreprocessing.guide;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.MaterialItem.Stage;
import com.thecascadian.universaloreprocessing.item.Tooltips;
import com.thecascadian.universaloreprocessing.material.MaterialTrait;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import com.thecascadian.universaloreprocessing.process.Reagent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The text of the in-game guide book. The introduction is fixed lang text, and the
 * station chapters are assembled from the processing rules, so the book always
 * matches the machines and the configured max_tier.
 */
public final class GuideContent {

    /** One chapter: a title and paragraphs that the screen wraps and paginates. */
    public record Chapter(Component title, List<Component> paragraphs) {
    }

    private static final String KEY = "guide." + UniversalOreProcessing.MODID + ".";

    private GuideContent() {
    }

    private static Component text(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    private static Component heading(Component title) {
        return title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static Component labelled(String key, Component value) {
        return text(key).copy().withStyle(ChatFormatting.DARK_GREEN)
                .append(value.copy().withStyle(ChatFormatting.BLACK));
    }

    public static List<Chapter> build() {
        List<Chapter> chapters = new ArrayList<>();

        chapters.add(new Chapter(text("chapter.intro"),
                List.of(text("intro.1"), text("intro.2"), text("intro.3"))));
        chapters.add(new Chapter(text("chapter.basic"),
                List.of(text("basic.1"), text("basic.2"), text("basic.3"), text("basic.4"))));
        chapters.add(new Chapter(text("chapter.screen"),
                List.of(text("screen.1"), text("screen.2"), text("screen.3"), text("screen.4"))));
        chapters.add(new Chapter(text("chapter.materials"),
                List.of(text("materials.1"), text("materials.2"), text("materials.3"))));

        List<Component> reagents = new ArrayList<>();
        reagents.add(text("reagents.intro"));
        for (Reagent reagent : Reagent.values()) {
            reagents.add(heading(Tooltips.reagentName(reagent)));
            reagents.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".reagent."
                    + reagent.itemId()));
            List<MachineKind> users = Tooltips.reagentUsers(reagent);
            if (!users.isEmpty()) {
                List<Component> names = new ArrayList<>();
                for (MachineKind kind : users)
                    names.add(Tooltips.machineName(kind));
                reagents.add(labelled("used_by", Tooltips.join(names)));
            }
        }
        chapters.add(new Chapter(text("chapter.reagents"), reagents));

        int maxTier = OreProcessingConfig.get(OreProcessingConfig.COMMON.maxTier);
        for (int tier = 1; tier <= maxTier; tier++) {
            List<Component> paragraphs = new ArrayList<>();
            paragraphs.add(text("tier." + tier + ".text"));
            for (MachineKind kind : MachineKind.values()) {
                if (kind.tier() == tier && ProcessRules.get(kind) != null)
                    paragraphs.addAll(stationParagraphs(kind, ProcessRules.get(kind)));
            }
            chapters.add(new Chapter(text("chapter.tier", tier, text("tier." + tier + ".name")), paragraphs));
        }
        if (maxTier < 8)
            chapters.add(new Chapter(text("chapter.locked"), List.of(text("locked.1", maxTier), text("locked.2"))));
        return chapters;
    }

    private static List<Component> stationParagraphs(MachineKind kind, ProcessRule rule) {
        List<Component> out = new ArrayList<>();
        out.add(heading(Tooltips.machineName(kind)));
        out.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".machine." + kind.id()));

        List<Component> inputs = new ArrayList<>();
        for (Stage stage : rule.inputs())
            inputs.add(Tooltips.stageName(stage));
        out.add(labelled("takes", Tooltips.join(inputs)));

        List<Component> needs = new ArrayList<>();
        if (rule.reagent() != null)
            needs.add(Tooltips.reagentName(rule.reagent()));
        if (kind.usesWater())
            needs.add(Component.translatable("tooltip." + UniversalOreProcessing.MODID + ".water"));
        if (!needs.isEmpty())
            out.add(labelled("needs", Tooltips.join(needs)));

        List<Component> makes = new ArrayList<>();
        if (rule.primary().stage() != null)
            makes.add(Tooltips.stageName(rule.primary().stage()));
        if (rule.secondary() != null) {
            makes.add(rule.secondary().stage() != null ? Tooltips.stageName(rule.secondary().stage())
                    : Tooltips.reagentName(rule.secondary().reagent()));
        }
        out.add(labelled("makes", Tooltips.join(makes)));

        List<Component> traits = new ArrayList<>();
        for (MaterialTrait trait : rule.traits())
            traits.add(Tooltips.traitName(trait));
        if (!traits.isEmpty())
            out.add(labelled("works_on", Tooltips.join(traits)));
        return out;
    }
}
