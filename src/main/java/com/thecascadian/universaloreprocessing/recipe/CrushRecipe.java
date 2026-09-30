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
 * A single recipe instance that matches any ore or raw material item known to
 * the {@link MaterialRegistry} and yields Crushed Ore carrying that material.
 * Replaces one recipe JSON per material with one dynamic recipe evaluated at
 * craft time.
 */
public class CrushRecipe implements Recipe<SingleRecipeInput> {

    public static final MapCodec<CrushRecipe> CODEC = MapCodec.unit(new CrushRecipe());
    public static final StreamCodec<RegistryFriendlyByteBuf, CrushRecipe> STREAM_CODEC =
            StreamCodec.unit(new CrushRecipe());

    /** Shared by the recipe, the machine tests and the JEI view; empty when the input is not crushable. */
    public static ItemStack craft(ItemStack input) {
        if (input.isEmpty() || !OreProcessingConfig.get(OreProcessingConfig.COMMON.crushEnabled))
            return ItemStack.EMPTY;

        MaterialRegistry.InputEntry entry = MaterialRegistry.current().inputFor(input.getItem());
        if (entry == null)
            return ItemStack.EMPTY;

        OreProcessingConfig.Common config = OreProcessingConfig.COMMON;
        int base = OreProcessingConfig.get(entry.raw() ? config.crushedPerRaw : config.crushedPerOre);
        int count = Yields.scale(base, OreProcessingConfig.get(config.crusherYieldMultiplier));
        return MaterialItem.create(MaterialItem.Stage.CRUSHED, entry.materialId(), count);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        if (input.isEmpty())
            return false;
        MaterialRegistry.InputEntry entry = MaterialRegistry.current().inputFor(input.item().getItem());
        return entry != null && OreProcessingConfig.get(OreProcessingConfig.COMMON.crushEnabled);
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
        return RegistryHandler.CRUSH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        // must return our custom recipe type so that the machine lookup and
        // recipe synchronization use the matching serializer
        return RegistryHandler.CRUSH_TYPE.get();
    }
}
