package com.thecascadian.universaloreprocessing.process;

import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.item.MaterialItem.Stage;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.material.MaterialTrait;
import com.thecascadian.universaloreprocessing.material.MaterialTraits;
import com.thecascadian.universaloreprocessing.process.ProcessRule.Output;
import com.thecascadian.universaloreprocessing.recipe.Yields;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.thecascadian.universaloreprocessing.item.MaterialItem.Stage.*;
import static com.thecascadian.universaloreprocessing.material.MaterialTrait.*;
import static com.thecascadian.universaloreprocessing.process.Reagent.*;

/**
 * The refining tree. Tier I sorts crushed rock, tier II reduces it with heat,
 * tier III dissolves it, tier IV plates it with current, tier V removes gases
 * and inclusions, tier VI and VII work with vapor and crystals, tier VIII
 * sinters and splits isotopes. The base crusher, washer and smelter stay
 * dynamic recipes and machine logic, so only the additional stations live here.
 */
public final class ProcessRules {

    private static final Map<MachineKind, ProcessRule> RULES = build();

    private ProcessRules() {
    }

    private static Map<MachineKind, ProcessRule> build() {
        Map<MachineKind, ProcessRule> rules = new EnumMap<>(MachineKind.class);

        // tier I: mechanical beneficiation
        rules.put(MachineKind.DENSITY_CLASSIFIER, ProcessRule.builder(CRUSHED)
                .traits(DENSE).reagent(DENSE_MEDIUM, 1).water(250)
                .primary(Output.stage(CONCENTRATE, 1)).secondary(Output.stage(GANGUE, 1))
                .ticks(120).build());
        rules.put(MachineKind.FLOTATION_CELL, ProcessRule.builder(CRUSHED, DUST)
                .traits(SULFIDE).reagent(COLLECTOR, 1).water(250)
                .primary(Output.stage(FROTH, 1)).secondary(Output.stage(TAILINGS, 1))
                .ticks(140).hazard(Hazard.TOXIC).build());
        rules.put(MachineKind.MAGNETIC_SEPARATOR, ProcessRule.builder(CRUSHED, DUST)
                .traits(MaterialTrait.MAGNETIC)
                .primary(Output.stage(Stage.MAGNETIC, 1)).secondary(Output.stage(NONMAGNETIC, 1))
                .ticks(120).power(2.0D).build());

        // tier II: pyrometallurgy
        rules.put(MachineKind.BLAST_FURNACE, ProcessRule.builder(CONCENTRATE, FROTH, Stage.MAGNETIC, PURIFIED)
                .traits(OXIDE, SULFIDE).reagent(FLUX, 1)
                .primary(Output.stage(CRUDE, 1)).secondary(Output.stage(SLAG, 1))
                .ticks(200).power(2.0D).hazard(Hazard.HEAT).build());
        rules.put(MachineKind.OXIDATION_CONVERTER, ProcessRule.builder(CRUDE)
                .traits(OXIDE, SULFIDE).reagent(PROCESS_GAS, 1)
                .primary(Output.stage(BILLET, 1)).secondary(Output.stage(SLAG, 1))
                .ticks(200).power(2.0D).hazard(Hazard.TOXIC).build());
        rules.put(MachineKind.THERMAL_RETORT, ProcessRule.builder(PURIFIED, CONCENTRATE)
                .traits(REFRACTORY).reagent(REDUCER, 1)
                .primary(Output.stage(SPONGE, 1)).secondary(Output.stage(SALT, 1))
                .ticks(300).power(2.0D).hazard(Hazard.HEAT).build());

        // tier III: hydrometallurgy
        rules.put(MachineKind.PRESSURE_AUTOCLAVE, ProcessRule.builder(CRUSHED)
                .traits(LEACHABLE).reagent(ACID, 1).water(500)
                .primary(Output.stage(LEACH, 1)).secondary(Output.stage(FILTER_CAKE, 1))
                .ticks(240).power(2.0D).hazard(Hazard.TOXIC).build());
        rules.put(MachineKind.PHASE_EXTRACTOR, ProcessRule.builder(LEACH)
                .traits(EXTRACTABLE).reagent(SOLVENT, 1)
                .primary(Output.stage(EXTRACT, 1)).secondary(Output.stage(RAFFINATE, 1))
                .ticks(200).power(1.5D).hazard(Hazard.TOXIC).build());
        rules.put(MachineKind.PRECIPITATION_ARRAY, ProcessRule.builder(LEACH, EXTRACT)
                .traits(PRECIPITABLE).reagent(PRECIPITANT, 1)
                .primary(Output.stage(PRECIPITATE, 1))
                .ticks(160).hazard(Hazard.TOXIC).build());

        // tier IV: electrometallurgy
        rules.put(MachineKind.ELECTROREFINING_CELL, ProcessRule.builder(CRUDE, BILLET, SPONGE)
                .traits(ELECTRO).reagent(ACID, 1).reagentUse(0.5D).water(250)
                .primary(Output.stage(CATHODE, 1)).secondary(Output.noble(ANODE_SLIME, 1, 0.5D))
                .ticks(300).power(3.0D).hazard(Hazard.TOXIC).build());
        rules.put(MachineKind.MOLTEN_SALT_ELECTROLYZER, ProcessRule.builder(PURIFIED, PRECIPITATE, SALT)
                .traits(ELECTROLYSIS).reagent(FLUX, 1).reagentUse(0.5D)
                .primary(Output.stage(ELECTROLYTIC, 1))
                .ticks(400).power(4.0D).hazard(Hazard.HEAT).build());

        // tier V: vacuum and secondary refining
        rules.put(MachineKind.VACUUM_OUTGASSER, ProcessRule.builder(CRUDE, BILLET, CATHODE, ELECTROLYTIC)
                .traits(STRUCTURAL).reagent(INERT_GAS, 1).reagentUse(0.5D)
                .primary(Output.stage(DEGASSED, 1)).secondary(Output.stage(INCLUSIONS, 1))
                .ticks(300).power(3.0D).build());
        rules.put(MachineKind.ARC_REMELTER, ProcessRule.builder(DEGASSED, CATHODE, ELECTROLYTIC, BILLET)
                .traits(SUPERALLOY).reagent(INERT_GAS, 1).reagentUse(0.5D)
                .primary(Output.stage(ARC_INGOT, 1))
                .ticks(400).power(4.0D).hazard(Hazard.HEAT).build());

        // tier VI: vapor and fractionation
        rules.put(MachineKind.FRACTIONATION_COLUMN, ProcessRule.builder(DUST, CONCENTRATE)
                .traits(HYDROCARBON).water(250)
                .primary(Output.stage(DISTILLATE_LIGHT, 1)).secondary(Output.stage(DISTILLATE_HEAVY, 1))
                .ticks(200).power(1.5D).hazard(Hazard.HEAT).build());
        rules.put(MachineKind.VOLATILE_VAPORIZER, ProcessRule.builder(DUST, PURIFIED)
                .traits(CARBONYL).reagent(PROCESS_GAS, 1)
                .primary(Output.stage(VAPOR_METAL, 1)).secondary(Output.reagent(PROCESS_GAS, 1, 0.75D))
                .ticks(300).power(2.0D).hazard(Hazard.TOXIC).build());

        // tier VII: semiconductor and crystal technology
        rules.put(MachineKind.VAPOR_DEPOSITION_FURNACE, ProcessRule.builder(DUST, PURIFIED)
                .traits(SEMICONDUCTOR).reagent(PROCESS_GAS, 1)
                .primary(Output.stage(POLYCRYSTAL, 1)).secondary(Output.reagent(ACID, 1, 0.5D))
                .ticks(400).power(3.0D).hazard(Hazard.TOXIC).build());
        rules.put(MachineKind.CRYSTAL_PULLER, ProcessRule.builder(POLYCRYSTAL, VAPOR_METAL)
                .traits(CRYSTAL).reagent(SEED_CRYSTAL, 1).reagentUse(0.1D)
                .primary(Output.stage(MONOCRYSTAL, 1)).secondary(Output.stage(CROP_ENDS, 1))
                .ticks(600).power(3.0D).build());

        // tier VIII: advanced synthesis
        rules.put(MachineKind.GRAPHITIZER, ProcessRule.builder(DUST, POLYCRYSTAL)
                .traits(SINTERABLE).reagent(INERT_GAS, 1).reagentUse(0.5D)
                .primary(Output.stage(MONOLITH, 1))
                .ticks(600).power(5.0D).hazard(Hazard.HEAT).build());
        rules.put(MachineKind.CENTRIFUGE_CASCADE, ProcessRule.builder(PURIFIED, DUST, PRECIPITATE)
                .traits(ISOTOPIC).reagent(PROCESS_GAS, 1).reagentUse(0.5D)
                .primary(Output.stage(ENRICHED, 1)).secondary(Output.stage(DEPLETED, 1))
                .ticks(500).power(4.0D).hazard(Hazard.TOXIC).build());
        rules.put(MachineKind.HOT_CELL, ProcessRule.builder(ENRICHED, DEPLETED)
                .traits(RADIOACTIVE).reagent(ACID, 1)
                .primary(Output.stage(FISSILE, 1)).secondary(Output.stage(WASTE_GLASS, 1))
                .ticks(500).power(4.0D).hazard(Hazard.RADIATION).build());

        return Collections.unmodifiableMap(rules);
    }

    @Nullable
    public static ProcessRule get(MachineKind kind) {
        return RULES.get(kind);
    }

    public static Map<MachineKind, ProcessRule> all() {
        return RULES;
    }

    // -------------------------------------------------------------------------
    // Per-kind settings
    // -------------------------------------------------------------------------

    /** Energy draw and fuel burn rate relative to the base machines. */
    public static double powerFactor(MachineKind kind) {
        ProcessRule rule = RULES.get(kind);
        if (rule == null || !OreProcessingConfig.get(OreProcessingConfig.COMMON.scalePowerByTier))
            return 1.0D;
        return rule.power();
    }

    /** Whether the station owns a water tank, a structural property independent of the configured cost. */
    public static boolean usesWater(MachineKind kind) {
        ProcessRule rule = RULES.get(kind);
        return kind == MachineKind.WASHER || (rule != null && rule.water() > 0);
    }

    public static int waterCost(MachineKind kind) {
        if (kind == MachineKind.WASHER)
            return OreProcessingConfig.get(OreProcessingConfig.COMMON.washerWaterPerOperation);
        ProcessRule rule = RULES.get(kind);
        if (rule == null)
            return 0;
        double multiplier = OreProcessingConfig.get(OreProcessingConfig.COMMON.stationWaterMultiplier);
        return (int) Math.round(rule.water() * multiplier);
    }

    public static Hazard hazard(MachineKind kind) {
        ProcessRule rule = RULES.get(kind);
        return rule == null ? Hazard.NONE : rule.hazard();
    }

    // -------------------------------------------------------------------------
    // Acceptance
    // -------------------------------------------------------------------------

    /** Whether the stack may enter the input slot of a refining station. */
    public static boolean accepts(MachineKind kind, ItemStack stack) {
        ProcessRule rule = RULES.get(kind);
        if (rule == null || stack.isEmpty())
            return false;
        Stage stage = MaterialItem.stageOf(stack);
        String materialId = MaterialItem.materialId(stack);
        return stage != null && materialId != null && accepts(rule, stage, materialId);
    }

    public static boolean accepts(ProcessRule rule, Stage stage, String materialId) {
        return rule.inputs().contains(stage) && MaterialTraits.matches(materialId, rule.traits());
    }

    // -------------------------------------------------------------------------
    // Plans
    // -------------------------------------------------------------------------

    /** Rolls one operation of a refining station, or returns null if the station cannot process the stack. */
    @Nullable
    public static Plan plan(MachineKind kind, ItemStack input) {
        ProcessRule rule = RULES.get(kind);
        if (rule == null || !OreProcessingConfig.enabled(kind) || !accepts(kind, input))
            return null;
        String materialId = MaterialItem.materialId(input);
        if (MaterialRegistry.current().get(materialId).isEmpty())
            return null;
        return assemble(kind, rule, materialId, false);
    }

    /** The same plan without random rolls, used by the JEI views. */
    @Nullable
    public static Plan preview(MachineKind kind, Stage stage, String materialId) {
        ProcessRule rule = RULES.get(kind);
        if (rule == null || !accepts(rule, stage, materialId))
            return null;
        return assemble(kind, rule, materialId, true);
    }

    private static Plan assemble(MachineKind kind, ProcessRule rule, String materialId, boolean preview) {
        int primaryCount = preview ? rule.primary().count()
                : Yields.scale(rule.primary().count(), OreProcessingConfig.yieldMultiplier(kind));
        ItemStack primary = create(rule.primary(), materialId, primaryCount);

        ItemStack secondary = ItemStack.EMPTY;
        Output byproduct = rule.secondary();
        if (byproduct != null && (preview || Yields.chance(OreProcessingConfig.byproductChance(kind))))
            secondary = create(byproduct, materialId, byproduct.count());

        return new Plan(primary, secondary, waterCost(kind), rule.reagent(), rule.reagentCount(), rule.reagentUse());
    }

    private static ItemStack create(Output output, String materialId, int count) {
        if (output.stage() != null) {
            String carried = output.noble() ? nobleMaterial(materialId) : materialId;
            return MaterialItem.create(output.stage(), carried, count);
        }
        return new ItemStack(output.reagent().item(), count);
    }

    /** The precious metal that anode slime carries: the material itself if noble, else the first configured metal the pack has. */
    private static String nobleMaterial(String materialId) {
        if (MaterialTraits.of(materialId).contains(NOBLE))
            return materialId;
        MaterialRegistry registry = MaterialRegistry.current();
        for (String candidate : OreProcessingConfig.get(OreProcessingConfig.COMMON.slimeMaterials)) {
            if (registry.get(candidate).isPresent())
                return candidate;
        }
        return materialId;
    }

    // -------------------------------------------------------------------------
    // Base machines
    // -------------------------------------------------------------------------

    /** The crusher reclaims a byproduct stage as dust with the configured chance. */
    @Nullable
    public static Plan recycle(ItemStack input) {
        if (input.isEmpty() || !OreProcessingConfig.get(OreProcessingConfig.COMMON.crushEnabled))
            return null;
        Stage stage = MaterialItem.stageOf(input);
        String materialId = MaterialItem.materialId(input);
        if (stage == null || !stage.isWaste() || materialId == null
                || MaterialRegistry.current().get(materialId).isEmpty())
            return null;

        boolean success = Yields.chance(OreProcessingConfig.get(OreProcessingConfig.COMMON.wasteRecycleChance));
        return Plan.of(success ? MaterialItem.create(DUST, materialId, 1) : ItemStack.EMPTY);
    }

    /** The smelter turns any smeltable stage into the material's final item. */
    public static ItemStack smelt(ItemStack input) {
        if (input.isEmpty() || !OreProcessingConfig.get(OreProcessingConfig.COMMON.smeltEnabled))
            return ItemStack.EMPTY;
        Stage stage = MaterialItem.stageOf(input);
        String materialId = MaterialItem.materialId(input);
        if (stage == null || !stage.isSmeltable() || materialId == null)
            return ItemStack.EMPTY;

        Optional<MaterialRegistry.Material> material = MaterialRegistry.current().get(materialId);
        if (material.isEmpty())
            return ItemStack.EMPTY;

        double multiplier = OreProcessingConfig.get(OreProcessingConfig.COMMON.smelterYieldMultiplier);
        return new ItemStack(material.get().output(), Yields.scale(stage.smeltCount(), multiplier));
    }

    /** Every material trait a station looks for, for display in the debug command. */
    public static List<MaterialTrait> traitsOf(MachineKind kind) {
        ProcessRule rule = RULES.get(kind);
        return rule == null ? List.of() : List.copyOf(rule.traits());
    }
}
