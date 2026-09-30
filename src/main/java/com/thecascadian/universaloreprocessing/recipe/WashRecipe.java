package com.thecascadian.universaloreprocessing.recipe;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A single recipe instance that matches any Crushed Ore carrying a known
 * material and yields Purified Ore of the same material. The water cost is
 * enforced by the Ore Washer, which owns the tank.
 */
public class WashRecipe implements Recipe<SingleRecipeInput> {

    // StreamCodec.unit only encodes an object equal to its instance, so both codecs must share one
    private static final WashRecipe INSTANCE = new WashRecipe();

    public static final MapCodec<WashRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, WashRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    /** Shared by the recipe, the machine tests and the JEI view; empty when the input is not washable. */
    public static ItemStack craft(ItemStack input) {
        if (input.isEmpty() || !OreProcessingConfig.get(OreProcessingConfig.COMMON.washEnabled))
            return ItemStack.EMPTY;
        if (input.getItem() != MaterialItem.Stage.CRUSHED.item())
            return ItemStack.EMPTY;

        String materialId = MaterialItem.materialId(input);
        if (materialId == null || MaterialRegistry.current().get(materialId).isEmpty())
            return ItemStack.EMPTY;

        double multiplier = OreProcessingConfig.get(OreProcessingConfig.COMMON.washerYieldMultiplier);
        return MaterialItem.create(MaterialItem.Stage.PURIFIED, materialId, Yields.scale(1, multiplier));
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !craft(input.item()).isEmpty();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return craft(input.item());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RegistryHandler.WASH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        // must return our custom recipe type so that the machine lookup and
        // recipe synchronization use the matching serializer
        return RegistryHandler.WASH_TYPE.get();
    }
}
