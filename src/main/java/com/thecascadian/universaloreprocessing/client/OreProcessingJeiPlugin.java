package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.material.MaterialDiscovery;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.recipe.CrushRecipe;
import com.thecascadian.universaloreprocessing.recipe.WashRecipe;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional JEI integration. JEI discovers this class through its annotation
 * scan, so nothing in the mod references it and it is never loaded when JEI is
 * absent. It shows the dynamic crush, wash and smelt results for every material.
 */
@JeiPlugin
public class OreProcessingJeiPlugin implements IModPlugin {

    /** One displayed conversion: a single input stack and the stack the machine produces from it. */
    public record MaterialRecipeView(ItemStack input, ItemStack output) {
    }

    private static final RecipeType<MaterialRecipeView> CRUSHING = create("crushing");
    private static final RecipeType<MaterialRecipeView> WASHING = create("washing");
    private static final RecipeType<MaterialRecipeView> SMELTING = create("smelting");

    private static RecipeType<MaterialRecipeView> create(String name) {
        return RecipeType.create(UniversalOreProcessing.MODID, name, MaterialRecipeView.class);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(UniversalOreProcessing.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new Category(gui, CRUSHING, "ore_crusher", new ItemStack(RegistryHandler.ORE_CRUSHER_ITEM.get())),
                new Category(gui, WASHING, "ore_washer", new ItemStack(RegistryHandler.ORE_WASHER_ITEM.get())),
                new Category(gui, SMELTING, "ore_smelter", new ItemStack(RegistryHandler.ORE_SMELTER_ITEM.get())));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // discovery may not have run yet when JEI loads, and the tags are already bound by then
        MaterialRegistry registry = MaterialRegistry.current();
        if (registry.isEmpty())
            registry = MaterialDiscovery.discover();

        List<MaterialRecipeView> crushing = new ArrayList<>();
        List<MaterialRecipeView> washing = new ArrayList<>();
        List<MaterialRecipeView> smelting = new ArrayList<>();

        for (MaterialRegistry.Material material : registry.materials().values()) {
            for (Item item : material.oreItems()) {
                addCrush(crushing, new ItemStack(item));
            }
            for (Item item : material.rawItems()) {
                addCrush(crushing, new ItemStack(item));
            }
            ItemStack crushed = MaterialItem.create(MaterialItem.Stage.CRUSHED, material.id(), 1);
            washing.add(new MaterialRecipeView(crushed, WashRecipe.craft(crushed)));

            ItemStack purified = MaterialItem.create(MaterialItem.Stage.PURIFIED, material.id(), 1);
            smelting.add(new MaterialRecipeView(purified, new ItemStack(material.output())));
        }

        registration.addRecipes(CRUSHING, crushing);
        registration.addRecipes(WASHING, washing);
        registration.addRecipes(SMELTING, smelting);
    }

    private static void addCrush(List<MaterialRecipeView> views, ItemStack input) {
        ItemStack output = CrushRecipe.craft(input);
        if (!output.isEmpty())
            views.add(new MaterialRecipeView(input, output));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(RegistryHandler.ORE_CRUSHER_ITEM.get()), CRUSHING);
        registration.addRecipeCatalyst(new ItemStack(RegistryHandler.ORE_WASHER_ITEM.get()), WASHING);
        registration.addRecipeCatalyst(new ItemStack(RegistryHandler.ORE_SMELTER_ITEM.get()), SMELTING);
    }

    private static final class Category extends AbstractRecipeCategory<MaterialRecipeView> {
        Category(IGuiHelper gui, RecipeType<MaterialRecipeView> type, String blockName, ItemStack icon) {
            super(type,
                    Component.translatable("block." + UniversalOreProcessing.MODID + "." + blockName),
                    gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon),
                    90, 26);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MaterialRecipeView recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 5).addItemStack(recipe.input());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 67, 5).addItemStack(recipe.output());
        }
    }
}
