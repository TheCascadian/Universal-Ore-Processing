package com.thecascadian.universaloreprocessing.item;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * One of the generic intermediate items. Every processing stage has exactly one
 * item, and the material is stored in the material data component, which drives
 * the display name, the client tint and the machine outputs.
 */
public class MaterialItem extends Item {

    /** What an item of a stage is good for once it leaves a machine. */
    public enum Role {
        /** Raw feed produced by the crusher. */
        FEED,
        /** A refined product that later stations or the smelter consume. */
        PRODUCT,
        /** A byproduct that can only be recycled into dust by the crusher. */
        WASTE
    }

    /**
     * The processing stage an item represents. The smelt count is how many of the
     * material's final items the smelter returns for one item of the stage, zero
     * meaning the smelter does not accept it.
     */
    public enum Stage {
        // tier I and the base chain
        CRUSHED("crushed_ore", "crushed", 0, Role.FEED),
        PURIFIED("purified_ore", "purified", 1, Role.PRODUCT),
        DUST("material_dust", "dust", 1, Role.PRODUCT),
        CONCENTRATE("heavy_concentrate", "concentrate", 1, Role.PRODUCT),
        GANGUE("light_gangue", "gangue", 0, Role.WASTE),
        FROTH("mineral_froth", "froth", 1, Role.PRODUCT),
        TAILINGS("spent_tailings", "tailings", 0, Role.WASTE),
        MAGNETIC("magnetic_fraction", "magnetic", 1, Role.PRODUCT),
        NONMAGNETIC("nonmagnetic_tailings", "nonmagnetic", 0, Role.WASTE),
        // tier II
        CRUDE("crude_metal", "crude", 1, Role.PRODUCT),
        SLAG("vitreous_slag", "slag", 0, Role.WASTE),
        BILLET("converted_billet", "billet", 2, Role.PRODUCT),
        SPONGE("metal_sponge", "sponge", 1, Role.PRODUCT),
        SALT("recovery_salt", "salt", 0, Role.WASTE),
        // tier III
        LEACH("leach_solution", "leach", 0, Role.PRODUCT),
        FILTER_CAKE("filter_cake", "filter_cake", 0, Role.WASTE),
        EXTRACT("organic_extract", "extract", 0, Role.PRODUCT),
        RAFFINATE("stripped_raffinate", "raffinate", 0, Role.WASTE),
        PRECIPITATE("solid_precipitate", "precipitate", 1, Role.PRODUCT),
        // tier IV
        CATHODE("cathode_plate", "cathode", 2, Role.PRODUCT),
        ANODE_SLIME("anode_slime", "slime", 1, Role.PRODUCT),
        ELECTROLYTIC("electrolytic_metal", "electrolytic", 2, Role.PRODUCT),
        // tier V
        DEGASSED("degassed_metal", "degassed", 2, Role.PRODUCT),
        INCLUSIONS("modified_inclusions", "inclusions", 0, Role.WASTE),
        ARC_INGOT("arc_remelted_ingot", "arc_ingot", 3, Role.PRODUCT),
        // tier VI
        DISTILLATE_LIGHT("light_distillate", "light_distillate", 0, Role.PRODUCT),
        DISTILLATE_HEAVY("heavy_distillate", "heavy_distillate", 0, Role.PRODUCT),
        VAPOR_METAL("vapor_refined_pellet", "vapor_metal", 3, Role.PRODUCT),
        // tier VII
        POLYCRYSTAL("polycrystal_cylinder", "polycrystal", 2, Role.PRODUCT),
        MONOCRYSTAL("monocrystal_boule", "monocrystal", 3, Role.PRODUCT),
        CROP_ENDS("crystal_crop_ends", "crop_ends", 0, Role.WASTE),
        // tier VIII
        MONOLITH("sintered_monolith", "monolith", 2, Role.PRODUCT),
        ENRICHED("enriched_fraction", "enriched", 2, Role.PRODUCT),
        DEPLETED("depleted_tails", "depleted", 0, Role.WASTE),
        FISSILE("fissile_stream", "fissile", 2, Role.PRODUCT),
        WASTE_GLASS("vitrified_waste", "waste_glass", 0, Role.WASTE);

        private final String itemId;
        private final String commandName;
        private final int smeltCount;
        private final Role role;

        Stage(String itemId, String commandName, int smeltCount, Role role) {
            this.itemId = itemId;
            this.commandName = commandName;
            this.smeltCount = smeltCount;
            this.role = role;
        }

        public String itemId() {
            return itemId;
        }

        public String commandName() {
            return commandName;
        }

        public int smeltCount() {
            return smeltCount;
        }

        public Role role() {
            return role;
        }

        public boolean isWaste() {
            return role == Role.WASTE;
        }

        public boolean isSmeltable() {
            return smeltCount > 0;
        }

        public static Optional<Stage> byCommandName(String name) {
            for (Stage stage : values()) {
                if (stage.commandName.equals(name.toLowerCase(Locale.ROOT)))
                    return Optional.of(stage);
            }
            return Optional.empty();
        }

        public Item item() {
            return RegistryHandler.STAGE_ITEMS.get(this).get();
        }
    }

    private final Stage stage;

    public MaterialItem(Stage stage, Item.Properties properties) {
        super(properties);
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }

    public static ItemStack create(Stage stage, String materialId, int count) {
        ItemStack stack = new ItemStack(stage.item(), count);
        stack.set(RegistryHandler.MATERIAL_COMPONENT.get(), materialId);
        return stack;
    }

    /** Returns the material id carried by the stack, or null if the stack has none. */
    public static String materialId(ItemStack stack) {
        return stack.get(RegistryHandler.MATERIAL_COMPONENT.get());
    }

    /** Returns the stage of a stack that holds a material item, or null for any other item. */
    public static Stage stageOf(ItemStack stack) {
        return stack.getItem() instanceof MaterialItem item ? item.stage : null;
    }

    public static Component materialName(String materialId) {
        return Component.translatableWithFallback(
                "material." + UniversalOreProcessing.MODID + "." + materialId, prettify(materialId));
    }

    @Override
    public Component getName(ItemStack stack) {
        String materialId = materialId(stack);
        if (materialId == null)
            return Component.translatable(getDescriptionId() + ".generic");
        return Component.translatable(getDescriptionId(), materialName(materialId));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        Tooltips.stage(stage, tooltip);
    }

    private static String prettify(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty())
                continue;
            if (out.length() > 0)
                out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
