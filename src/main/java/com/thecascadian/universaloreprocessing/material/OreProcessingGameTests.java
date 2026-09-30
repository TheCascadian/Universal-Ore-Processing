package com.thecascadian.universaloreprocessing.material;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.MachineBlockEntity;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.item.MaterialItem.Stage;
import com.thecascadian.universaloreprocessing.process.Plan;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import com.thecascadian.universaloreprocessing.process.Reagent;
import com.thecascadian.universaloreprocessing.recipe.CrushRecipe;
import com.thecascadian.universaloreprocessing.recipe.WashRecipe;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Game tests for discovery, the dynamic crush and wash recipes and the refining
 * tree, run with the gameTestServer run configuration against vanilla iron,
 * copper, gold and coal.
 */
@GameTestHolder(UniversalOreProcessing.MODID)
@PrefixGameTestTemplate(false)
public final class OreProcessingGameTests {

    private static final String EMPTY_TEMPLATE = UniversalOreProcessing.MODID + ":empty";

    private OreProcessingGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void discoversVanillaMaterials(GameTestHelper helper) {
        Map<String, Item> expected = Map.of(
                "iron", Items.IRON_INGOT,
                "copper", Items.COPPER_INGOT,
                "gold", Items.GOLD_INGOT,
                "coal", Items.COAL);

        MaterialRegistry registry = MaterialRegistry.current();
        for (Map.Entry<String, Item> entry : expected.entrySet()) {
            MaterialRegistry.Material material = registry.get(entry.getKey()).orElse(null);
            if (material == null) {
                helper.fail("Material not discovered: " + entry.getKey());
                return;
            }
            if (material.output() != entry.getValue()) {
                helper.fail("Wrong output for " + entry.getKey() + ": " + BuiltInRegistries.ITEM.getKey(material.output()));
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void crusherOutputCarriesComponent(GameTestHelper helper) {
        ItemStack input = new ItemStack(Items.RAW_IRON);
        ItemStack result = helper.getLevel().getRecipeManager()
                .getRecipeFor(RegistryHandler.CRUSH_TYPE.get(), new SingleRecipeInput(input), helper.getLevel())
                .map(holder -> holder.value().assemble(new SingleRecipeInput(input), helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);

        if (result.isEmpty() || result.getItem() != RegistryHandler.CRUSHED_ORE.get()) {
            helper.fail("Crusher did not produce Crushed Ore from raw iron");
            return;
        }
        if (!"iron".equals(MaterialItem.materialId(result))) {
            helper.fail("Crushed Ore carries the wrong material: " + MaterialItem.materialId(result));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void washerOutputCarriesComponent(GameTestHelper helper) {
        ItemStack crushed = CrushRecipe.craft(new ItemStack(Items.COPPER_ORE));
        ItemStack result = WashRecipe.craft(crushed);

        if (result.isEmpty() || result.getItem() != RegistryHandler.PURIFIED_ORE.get()) {
            helper.fail("Washer did not produce Purified Ore from Crushed Ore");
            return;
        }
        if (!"copper".equals(MaterialItem.materialId(result))) {
            helper.fail("Purified Ore carries the wrong material: " + MaterialItem.materialId(result));
            return;
        }
        helper.succeed();
    }
    // -------------------------------------------------------------------------
    // Refinery tree
    // -------------------------------------------------------------------------

    @GameTest(template = EMPTY_TEMPLATE)
    public static void traitsResolve(GameTestHelper helper) {
        if (!MaterialTraits.of("iron").contains(MaterialTrait.MAGNETIC)) {
            helper.fail("Iron should be magnetic");
            return;
        }
        if (!MaterialTraits.of("copper").contains(MaterialTrait.SULFIDE)) {
            helper.fail("Copper should be a sulfide");
            return;
        }
        if (!MaterialTraits.of("coal").contains(MaterialTrait.HYDROCARBON)) {
            helper.fail("Coal should be a hydrocarbon");
            return;
        }
        if (MaterialTraits.of("some_unknown_modded_ore").isEmpty()) {
            helper.fail("Unknown materials should receive the default traits");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void everyStationHasARule(GameTestHelper helper) {
        for (MachineKind kind : MachineKind.values()) {
            if (!kind.isBase() && ProcessRules.get(kind) == null) {
                helper.fail("Station without a rule: " + kind.id());
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void everyInputStageIsProduced(GameTestHelper helper) {
        // the base machines produce crushed ore, purified ore and, by recycling, dust
        Set<Stage> produced = EnumSet.of(Stage.CRUSHED, Stage.PURIFIED, Stage.DUST);
        for (ProcessRule rule : ProcessRules.all().values()) {
            if (rule.primary().stage() != null)
                produced.add(rule.primary().stage());
            if (rule.secondary() != null && rule.secondary().stage() != null)
                produced.add(rule.secondary().stage());
        }
        for (Map.Entry<MachineKind, ProcessRule> entry : ProcessRules.all().entrySet()) {
            for (Stage input : entry.getValue().inputs()) {
                if (!produced.contains(input)) {
                    helper.fail(entry.getKey().id() + " takes " + input + " but nothing produces it");
                    return;
                }
            }
        }
        for (Stage stage : Stage.values()) {
            if (!produced.contains(stage)) {
                helper.fail("No station produces " + stage);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void ironSteelChain(GameTestHelper helper) {
        ItemStack stack = MaterialItem.create(Stage.CRUSHED, "iron", 1);
        Stage[] expected = {Stage.MAGNETIC, Stage.CRUDE, Stage.BILLET, Stage.DEGASSED};
        MachineKind[] stations = {MachineKind.MAGNETIC_SEPARATOR, MachineKind.BLAST_FURNACE,
                MachineKind.OXIDATION_CONVERTER, MachineKind.VACUUM_OUTGASSER};

        for (int i = 0; i < stations.length; i++) {
            Plan plan = ProcessRules.plan(stations[i], stack);
            if (plan == null) {
                helper.fail(stations[i].id() + " rejected " + MaterialItem.stageOf(stack));
                return;
            }
            if (MaterialItem.stageOf(plan.primary()) != expected[i] || !"iron".equals(MaterialItem.materialId(plan.primary()))) {
                helper.fail(stations[i].id() + " produced the wrong output for iron");
                return;
            }
            stack = plan.primary();
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void copperFlotationAndRefining(GameTestHelper helper) {
        Plan flotation = ProcessRules.plan(MachineKind.FLOTATION_CELL, MaterialItem.create(Stage.CRUSHED, "copper", 1));
        if (flotation == null || MaterialItem.stageOf(flotation.primary()) != Stage.FROTH
                || MaterialItem.stageOf(flotation.secondary()) != Stage.TAILINGS) {
            helper.fail("Flotation should split copper into froth and tailings");
            return;
        }
        Plan refining = ProcessRules.plan(MachineKind.ELECTROREFINING_CELL, MaterialItem.create(Stage.CRUDE, "copper", 1));
        if (refining == null || MaterialItem.stageOf(refining.primary()) != Stage.CATHODE) {
            helper.fail("Electrorefining should plate copper onto a cathode");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void affinityRejectsMismatchedMaterials(GameTestHelper helper) {
        if (ProcessRules.accepts(MachineKind.FLOTATION_CELL, MaterialItem.create(Stage.CRUSHED, "gold", 1))) {
            helper.fail("Flotation should not take gold, which is not a sulfide");
            return;
        }
        if (ProcessRules.accepts(MachineKind.FRACTIONATION_COLUMN, MaterialItem.create(Stage.DUST, "iron", 1))) {
            helper.fail("The fractionation column should only take hydrocarbons");
            return;
        }
        if (!ProcessRules.accepts(MachineKind.FRACTIONATION_COLUMN, MaterialItem.create(Stage.DUST, "coal", 1))) {
            helper.fail("The fractionation column should take coal dust");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void smelterScalesWithStage(GameTestHelper helper) {
        ItemStack cathode = ProcessRules.smelt(MaterialItem.create(Stage.CATHODE, "copper", 1));
        if (cathode.getItem() != Items.COPPER_INGOT || cathode.getCount() != Stage.CATHODE.smeltCount()) {
            helper.fail("A copper cathode should smelt to " + Stage.CATHODE.smeltCount() + " copper ingots");
            return;
        }
        if (!ProcessRules.smelt(MaterialItem.create(Stage.SLAG, "copper", 1)).isEmpty()) {
            helper.fail("Slag must not be smeltable");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void wasteRecyclesToDustOrNothing(GameTestHelper helper) {
        Plan plan = ProcessRules.recycle(MaterialItem.create(Stage.GANGUE, "iron", 1));
        if (plan == null) {
            helper.fail("The crusher should accept gangue");
            return;
        }
        ItemStack result = plan.primary();
        if (!result.isEmpty() && MaterialItem.stageOf(result) != Stage.DUST) {
            helper.fail("Recycling should only ever return dust");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void everyReagentHasAnItem(GameTestHelper helper) {
        for (Reagent reagent : Reagent.values()) {
            if (reagent.item() == Items.AIR) {
                helper.fail("Reagent without an item: " + reagent);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 400)
    public static void densityClassifierWorks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, RegistryHandler.MACHINE_BLOCKS.get(MachineKind.DENSITY_CLASSIFIER).get());
        if (!(helper.getBlockEntity(pos) instanceof MachineBlockEntity machine)) {
            helper.fail("The density classifier has no block entity");
            return;
        }
        machine.items().setStackInSlot(MachineBlockEntity.SLOT_INPUT, MaterialItem.create(Stage.CRUSHED, "iron", 1));
        machine.items().setStackInSlot(MachineBlockEntity.SLOT_REAGENT, new ItemStack(Reagent.DENSE_MEDIUM.item(), 4));
        machine.items().setStackInSlot(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
        machine.fillWater(1000);

        helper.succeedWhen(() -> {
            ItemStack primary = machine.items().getStackInSlot(MachineBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(MaterialItem.stageOf(primary) == Stage.CONCENTRATE, "No heavy concentrate produced yet");
            ItemStack secondary = machine.items().getStackInSlot(MachineBlockEntity.SLOT_BYPRODUCT);
            helper.assertTrue(MaterialItem.stageOf(secondary) == Stage.GANGUE, "No light gangue produced yet");
        });
    }
}
