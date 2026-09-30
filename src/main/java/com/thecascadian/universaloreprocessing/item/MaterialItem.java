package com.thecascadian.universaloreprocessing.item;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * One of the three generic intermediate items (crushed ore, purified ore,
 * material dust). The material is stored in the material data component, which
 * drives the display name, the client tint and the machine outputs.
 */
public class MaterialItem extends Item {

    /** The processing stage an item represents. */
    public enum Stage {
        CRUSHED("crushed"), PURIFIED("purified"), DUST("dust");

        private final String commandName;

        Stage(String commandName) {
            this.commandName = commandName;
        }

        public String commandName() {
            return commandName;
        }

        public static Optional<Stage> byCommandName(String name) {
            for (Stage stage : values()) {
                if (stage.commandName.equals(name.toLowerCase(Locale.ROOT)))
                    return Optional.of(stage);
            }
            return Optional.empty();
        }

        public Item item() {
            return switch (this) {
                case CRUSHED -> RegistryHandler.CRUSHED_ORE.get();
                case PURIFIED -> RegistryHandler.PURIFIED_ORE.get();
                case DUST -> RegistryHandler.MATERIAL_DUST.get();
            };
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
