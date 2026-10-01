package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.ladder.LadderCookingRecipe;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class OreProcessingRecipeProvider extends RecipeProvider {

    public OreProcessingRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, RegistryHandler.HAMMER.get())
                .pattern("CCC")
                .pattern("CSC")
                .pattern(" S ")
                .define('C', Tags.Items.COBBLESTONES)
                .define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_cobblestone", has(Tags.Items.COBBLESTONES))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RegistryHandler.CRUSHING_SLAB_ITEM.get())
                .pattern("S S")
                .pattern("SSS")
                .define('S', Items.SMOOTH_STONE)
                .unlockedBy("has_smooth_stone", has(Items.SMOOTH_STONE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RegistryHandler.SLUICE_ITEM.get(), 3)
                .pattern("R  ")
                .pattern("PR ")
                .pattern("PPP")
                .define('P', net.minecraft.tags.ItemTags.PLANKS)
                .define('R', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_planks", has(net.minecraft.tags.ItemTags.PLANKS))
                .save(output);

        // one recipe per furnace type covers every form of every discovered material
        output.accept(RegistryHandler.id("ladder_smelting"),
                new LadderCookingRecipe(RegistryHandler.LADDER_SMELTING_SERIALIZER.get(), 0.1F, 200), null);
        output.accept(RegistryHandler.id("ladder_blasting"),
                new LadderCookingRecipe(RegistryHandler.LADDER_BLASTING_SERIALIZER.get(), 0.1F, 100), null);
    }
}
