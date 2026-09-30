package com.thecascadian.universaloreprocessing.item;

import com.thecascadian.universaloreprocessing.process.Reagent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A plain refinery support item that explains itself in its tooltip. */
public class ReagentItem extends Item {

    private final Reagent reagent;

    public ReagentItem(Reagent reagent, Item.Properties properties) {
        super(properties);
        this.reagent = reagent;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        Tooltips.reagent(reagent, tooltip);
    }
}
