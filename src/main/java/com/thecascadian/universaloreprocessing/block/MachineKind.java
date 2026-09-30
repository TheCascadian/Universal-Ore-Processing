package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.Codec;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/** The three machine stages. One block entity type serves all of them and branches on this kind. */
public enum MachineKind implements StringRepresentable {
    CRUSHER("ore_crusher"),
    WASHER("ore_washer"),
    SMELTER("ore_smelter");

    public static final Codec<MachineKind> CODEC = StringRepresentable.fromEnum(MachineKind::values);

    private final String id;

    MachineKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Whether the stack may be placed in this machine's input slot. */
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return switch (this) {
            case CRUSHER -> MaterialRegistry.current().inputFor(stack.getItem()) != null;
            case WASHER -> stack.getItem() == MaterialItem.Stage.CRUSHED.item()
                    && MaterialItem.materialId(stack) != null;
            case SMELTER -> (stack.getItem() == MaterialItem.Stage.PURIFIED.item()
                    || stack.getItem() == MaterialItem.Stage.DUST.item())
                    && MaterialItem.materialId(stack) != null;
        };
    }
}
