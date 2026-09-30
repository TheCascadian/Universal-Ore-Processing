package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.Codec;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * Every machine. One block entity type serves all of them and branches on this kind.
 * Tier zero is the base chain, tiers one to eight follow the refinery tech tree.
 */
public enum MachineKind implements StringRepresentable {
    CRUSHER("ore_crusher", 0),
    WASHER("ore_washer", 0),
    SMELTER("ore_smelter", 0),

    DENSITY_CLASSIFIER("density_classifier", 1),
    FLOTATION_CELL("flotation_cell", 1),
    MAGNETIC_SEPARATOR("magnetic_separator", 1),

    BLAST_FURNACE("blast_furnace", 2),
    OXIDATION_CONVERTER("oxidation_converter", 2),
    THERMAL_RETORT("thermal_retort", 2),

    PRESSURE_AUTOCLAVE("pressure_autoclave", 3),
    PHASE_EXTRACTOR("phase_extractor", 3),
    PRECIPITATION_ARRAY("precipitation_array", 3),

    ELECTROREFINING_CELL("electrorefining_cell", 4),
    MOLTEN_SALT_ELECTROLYZER("molten_salt_electrolyzer", 4),

    VACUUM_OUTGASSER("vacuum_outgasser", 5),
    ARC_REMELTER("arc_remelter", 5),

    FRACTIONATION_COLUMN("fractionation_column", 6),
    VOLATILE_VAPORIZER("volatile_vaporizer", 6),

    VAPOR_DEPOSITION_FURNACE("vapor_deposition_furnace", 7),
    CRYSTAL_PULLER("crystal_puller", 7),

    GRAPHITIZER("graphitizer", 8),
    CENTRIFUGE_CASCADE("centrifuge_cascade", 8),
    HOT_CELL("hot_cell", 8);

    public static final Codec<MachineKind> CODEC = StringRepresentable.fromEnum(MachineKind::values);

    private final String id;
    private final int tier;

    MachineKind(String id, int tier) {
        this.id = id;
        this.tier = tier;
    }

    public String id() {
        return id;
    }

    public int tier() {
        return tier;
    }

    /** The crusher, washer and smelter, which use the dynamic recipes and the smelter logic. */
    public boolean isBase() {
        return tier == 0;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Whether the station owns a water tank. */
    public boolean usesWater() {
        return ProcessRules.usesWater(this);
    }

    /** Whether the station takes a reagent item. */
    public boolean hasReagentSlot() {
        ProcessRule rule = ProcessRules.get(this);
        return rule != null && rule.reagent() != null;
    }

    /** Whether the station can return a second output. */
    public boolean hasByproductSlot() {
        ProcessRule rule = ProcessRules.get(this);
        return rule != null && rule.secondary() != null;
    }

    /** Whether the stack may be placed in this machine's reagent slot. */
    public boolean acceptsReagent(ItemStack stack) {
        ProcessRule rule = ProcessRules.get(this);
        return rule != null && rule.reagent() != null && !stack.isEmpty() && stack.is(rule.reagent().item());
    }

    /** Whether the stack may be placed in this machine's input slot. */
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return switch (this) {
            case CRUSHER -> MaterialRegistry.current().inputFor(stack.getItem()) != null || isWaste(stack);
            case WASHER -> MaterialItem.stageOf(stack) == MaterialItem.Stage.CRUSHED
                    && MaterialItem.materialId(stack) != null;
            case SMELTER -> {
                MaterialItem.Stage stage = MaterialItem.stageOf(stack);
                yield stage != null && stage.isSmeltable() && MaterialItem.materialId(stack) != null;
            }
            default -> ProcessRules.accepts(this, stack);
        };
    }

    private static boolean isWaste(ItemStack stack) {
        MaterialItem.Stage stage = MaterialItem.stageOf(stack);
        return stage != null && stage.isWaste() && MaterialItem.materialId(stack) != null;
    }
}
