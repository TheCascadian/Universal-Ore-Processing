package com.thecascadian.universaloreprocessing.guide;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Short action bar notes shown when an interaction does not do what the player
 * hoped. Each one says what went wrong and what to do instead, and the guide
 * book's troubleshooting chapter lists the same notes.
 */
public final class Hints {

    public static final String PREFIX = "hint." + UniversalOreProcessing.MODID + ".";

    private Hints() {
    }

    /** Server side only, so a hint is shown once rather than once per side. */
    public static void tell(Player player, String key, Object... args) {
        if (player instanceof ServerPlayer)
            player.displayClientMessage(Component.translatable(PREFIX + key, args).withStyle(ChatFormatting.GOLD), true);
    }
}
