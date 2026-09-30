package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.material.MaterialDiscovery;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.process.Plan;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Optional JEI integration. JEI discovers this class through its annotation
 * scan, so nothing in the mod references it and it is never loaded when JEI is
 * absent. It shows every machine conversion for every material: the dynamic
 * crush, wash and smelt results and each refining station with its reagent and
 * byproduct.
 */
@JeiPlugin
public class OreProcessingJeiPlugin implements IModPlugin {

    /** One displayed conversion: the input, the reagent it needs, the main output and the byproduct. */
    public record MaterialRecipeView(ItemStack input, ItemStack reagent, ItemStack output, ItemStack secondary) {

        static MaterialRecipeView of(ItemStack input, ItemStack output) {
            return new MaterialRecipeView(input, ItemStack.EMPTY, output, ItemStack.EMPTY);
        }
    }

    private static final Map<MachineKind, RecipeType<MaterialRecipeView>> TYPES = new EnumMap<>(MachineKind.class);

    static {
        for (MachineKind kind : MachineKind.values()) {
            TYPES.put(kind, RecipeType.create(UniversalOreProcessing.MODID, typeName(kind), MaterialRecipeView.class));
        }
    }

    /** The three base machines keep the names their recipe types had before the refinery stations existed. */
    private static String typeName(MachineKind kind) {
        return switch (kind) {
            case CRUSHER -> "crushing";
            case WASHER -> "washing";
            case SMELTER -> "smelting";
            default -> kind.id();
        };
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(UniversalOreProcessing.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (MachineKind kind : MachineKind.values()) {
            if (OreProcessingConfig.tierEnabled(kind))
                registration.addRecipeCategories(new Category(gui, kind));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // discovery may not have run yet when JEI loads, and the tags are already bound by then
        MaterialRegistry registry = MaterialRegistry.current();
        if (registry.isEmpty())
            registry = MaterialDiscovery.discover();

        Map<MachineKind, List<MaterialRecipeView>> views = new EnumMap<>(MachineKind.class);
        for (MachineKind kind : MachineKind.values()) {
            views.put(kind, new ArrayList<>());
        }

        for (MaterialRegistry.Material material : registry.materials().values()) {
            for (Item item : material.oreItems()) {
                addCrush(views.get(MachineKind.CRUSHER), new ItemStack(item));
            }
            for (Item item : material.rawItems()) {
                addCrush(views.get(MachineKind.CRUSHER), new ItemStack(item));
            }
            ItemStack crushed = MaterialItem.create(MaterialItem.Stage.CRUSHED, material.id(), 1);
            views.get(MachineKind.WASHER).add(MaterialRecipeView.of(crushed, WashRecipe.craft(crushed)));

            for (MaterialItem.Stage stage : MaterialItem.Stage.values()) {
                ItemStack input = MaterialItem.create(stage, material.id(), 1);
                if (stage.isSmeltable()) {
                    views.get(MachineKind.SMELTER).add(MaterialRecipeView.of(input,
                            new ItemStack(material.output(), stage.smeltCount())));
                }
                if (stage.isWaste()) {
                    views.get(MachineKind.CRUSHER).add(MaterialRecipeView.of(input,
                            MaterialItem.create(MaterialItem.Stage.DUST, material.id(), 1)));
                }
            }

            for (Map.Entry<MachineKind, ProcessRule> entry : ProcessRules.all().entrySet()) {
                for (MaterialItem.Stage stage : entry.getValue().inputs()) {
                    Plan plan = ProcessRules.preview(entry.getKey(), stage, material.id());
                    if (plan == null)
                        continue;
                    ItemStack reagent = plan.reagent() == null ? ItemStack.EMPTY
                            : new ItemStack(plan.reagent().item(), plan.reagentCount());
                    views.get(entry.getKey()).add(new MaterialRecipeView(
                            MaterialItem.create(stage, material.id(), 1), reagent, plan.primary(), plan.secondary()));
                }
            }
        }

        for (MachineKind kind : MachineKind.values()) {
            if (OreProcessingConfig.tierEnabled(kind))
                registration.addRecipes(TYPES.get(kind), views.get(kind));
        }
    }

    private static void addCrush(List<MaterialRecipeView> views, ItemStack input) {
        ItemStack output = CrushRecipe.craft(input);
        if (!output.isEmpty())
            views.add(MaterialRecipeView.of(input, output));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineKind kind : MachineKind.values()) {
            if (!OreProcessingConfig.tierEnabled(kind))
                continue;
            registration.addRecipeCatalyst(new ItemStack(RegistryHandler.MACHINE_ITEMS.get(kind).get()),
                    TYPES.get(kind));
        }
    }

    private static final class Category extends AbstractRecipeCategory<MaterialRecipeView> {
        Category(IGuiHelper gui, MachineKind kind) {
            super(TYPES.get(kind),
                    Component.translatable("block." + UniversalOreProcessing.MODID + "." + kind.id()),
                    gui.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                            new ItemStack(RegistryHandler.MACHINE_ITEMS.get(kind).get())),
                    112, 26);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MaterialRecipeView recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 5).addItemStack(recipe.input());
            if (!recipe.reagent().isEmpty())
                builder.addSlot(RecipeIngredientRole.INPUT, 27, 5).addItemStack(recipe.reagent());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 67, 5).addItemStack(recipe.output());
            if (!recipe.secondary().isEmpty())
                builder.addSlot(RecipeIngredientRole.OUTPUT, 89, 5).addItemStack(recipe.secondary());
        }
    }
}
