package com.thecascadian.universaloreprocessing.config;

import com.thecascadian.universaloreprocessing.block.MachineKind;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * Client side display options, written to `config/universaloreprocessing-client.toml`.
 * Nothing here affects gameplay, only which explanations are shown and how quickly.
 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final Client CLIENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CLIENT = new Client(builder);
        SPEC = builder.build();
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue tooltips;
        public final ModConfigSpec.BooleanValue itemTooltips;
        public final ModConfigSpec.BooleanValue guiHints;
        public final ModConfigSpec.IntValue hintDelayMs;
        public final Map<MachineKind, ModConfigSpec.BooleanValue> machineTooltips = new EnumMap<>(MachineKind.class);

        Client(ModConfigSpec.Builder builder) {
            builder.push("tooltips");
            tooltips = builder
                    .comment("Master switch. If false this mod shows no explanatory tooltips or screen hints at all.")
                    .define("enabled", true);
            itemTooltips = builder
                    .comment("Show explanatory tooltips on this mod's items and blocks.")
                    .define("item_tooltips", true);
            guiHints = builder
                    .comment("Show explanatory hints when hovering slots, bars and the status icon in machine screens.")
                    .define("gui_hints", true);
            hintDelayMs = builder
                    .comment("Milliseconds the pointer must rest on a machine screen element before its hint appears. 0 shows hints immediately.")
                    .defineInRange("hint_delay_ms", 500, 0, 5000);
            builder.push("per_machine");
            for (MachineKind kind : MachineKind.values()) {
                machineTooltips.put(kind, builder
                        .comment("Show tooltips and screen hints for the " + kind.id() + ".")
                        .define(kind.id(), true));
            }
            builder.pop(2);
        }
    }

    private ClientConfig() {
    }

    public static boolean itemTooltips() {
        return get(CLIENT.tooltips, true) && get(CLIENT.itemTooltips, true);
    }

    public static boolean machineItemTooltips(MachineKind kind) {
        return itemTooltips() && get(CLIENT.machineTooltips.get(kind), true);
    }

    public static boolean guiHints(MachineKind kind) {
        return get(CLIENT.tooltips, true) && get(CLIENT.guiHints, true)
                && get(CLIENT.machineTooltips.get(kind), true);
    }

    public static int hintDelayMs() {
        try {
            return CLIENT.hintDelayMs.get();
        } catch (IllegalStateException e) {
            return CLIENT.hintDelayMs.getDefault();
        }
    }

    private static boolean get(ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
