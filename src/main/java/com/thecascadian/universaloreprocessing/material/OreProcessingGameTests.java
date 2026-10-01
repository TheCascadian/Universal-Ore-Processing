package com.thecascadian.universaloreprocessing.material;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.Yields;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * Game tests for discovery, the deterministic yield accumulator and the
 * ladder smelting recipe, run with the gameTestServer run configuration
 * against vanilla iron, copper and gold.
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
                "gold", Items.GOLD_INGOT);

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
    public static void accumulatorIsExactPerStack(GameTestHelper helper) {
        int sum = 0;
        for (int count = 64; count > 0; count--) {
            sum += Yields.forItem(1.25D, count);
        }
        helper.assertTrue(sum == 80, "A stack of 64 clumps should smelt to exactly 80, got " + sum);
        helper.assertTrue(Yields.total(1.5D, 3) == 4, "Three dust should smelt to exactly 4");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void shardsSmeltToTwoIngots(GameTestHelper helper) {
        ItemStack shards = FormItem.create(Form.SHARDS, "iron", 1);
        SingleRecipeInput input = new SingleRecipeInput(shards);
        ItemStack result = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(Items.IRON_INGOT) && result.getCount() == 2,
                "Iron shards should smelt to two iron ingots, got " + result);
        helper.succeed();
    }
}
