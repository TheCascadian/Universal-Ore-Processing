package com.thecascadian.universaloreprocessing.item;

import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.LadderTables;
import com.thecascadian.universaloreprocessing.ladder.Yields;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * One item per ladder form. The material travels in a data component, so a
 * single registered item covers every discovered material without needing
 * registration at tag reload time.
 */
public class FormItem extends Item {

    private final Form form;

    public FormItem(Form form, Properties properties) {
        super(properties);
        this.form = form;
    }

    public Form form() {
        return form;
    }

    public static ItemStack create(Form form, String materialId, int count) {
        Item item = switch (form) {
            case CLUMPS -> RegistryHandler.CLUMPS.get();
            case DUST -> RegistryHandler.DUST.get();
            case SHARDS -> RegistryHandler.SHARDS.get();
            case RAW -> throw new IllegalArgumentException("raw ore is a vanilla item");
        };
        ItemStack stack = new ItemStack(item, count);
        stack.set(RegistryHandler.MATERIAL_COMPONENT.get(), materialId);
        return stack;
    }

    public static String materialId(ItemStack stack) {
        return stack.get(RegistryHandler.MATERIAL_COMPONENT.get());
    }

    /** Returns the form of the stack, or null when it is not a ladder item carrying a known material. */
    public static Form formOf(ItemStack stack) {
        if (stack.getItem() instanceof FormItem formItem && materialId(stack) != null)
            return formItem.form();
        return null;
    }

    public static boolean isWashed(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(RegistryHandler.WASHED_COMPONENT.get()));
    }

    public static Component materialName(String materialId) {
        String[] words = materialId.split("_");
        StringBuilder fallback = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty())
                continue;
            if (!fallback.isEmpty())
                fallback.append(' ');
            fallback.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return Component.translatableWithFallback("material.universaloreprocessing." + materialId, fallback.toString());
    }

    @Override
    public Component getName(ItemStack stack) {
        String materialId = materialId(stack);
        if (materialId == null)
            return super.getName(stack);
        return Component.translatable(getDescriptionId() + ".named", materialName(materialId));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String materialId = materialId(stack);
        if (materialId == null)
            return;
        appendLadderLines(form, materialId, tooltip);
        if (isWashed(stack))
            tooltip.add(Component.translatable("tooltip.universaloreprocessing.washed").withStyle(ChatFormatting.DARK_AQUA));
    }

    /** Shared by ladder items and the raw ore tooltip hook: smelt ratio, then the next verb. */
    public static void appendLadderLines(Form form, String materialId, List<Component> tooltip) {
        double ratio = LadderTables.ratios().of(form);
        MaterialRegistry.current().get(materialId).ifPresent(material -> tooltip.add(Component.translatable(
                "tooltip.universaloreprocessing.smelts", Yields.format(ratio), material.output().getDescription())
                .withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.translatable(form.nextKey()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
