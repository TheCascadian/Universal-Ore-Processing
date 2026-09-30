package com.thecascadian.universaloreprocessing.material;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.recipe.CrushRecipe;
import com.thecascadian.universaloreprocessing.recipe.WashRecipe;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * Game tests for discovery and the dynamic crush and wash recipes, run with the
 * gameTestServer run configuration against vanilla iron, copper, gold and coal.
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
}
