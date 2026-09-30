package com.thecascadian.universaloreprocessing.item;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Hands each player the guide book once, the first time they join a world. */
@EventBusSubscriber(modid = UniversalOreProcessing.MODID)
public final class GuideGiver {

    private static final String PERSISTED = "PlayerPersisted";
    private static final String FLAG = UniversalOreProcessing.MODID + "_guide_given";

    private GuideGiver() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !OreProcessingConfig.get(OreProcessingConfig.COMMON.giveGuide))
            return;

        // the persisted sub-tag survives death and dimension changes
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(PERSISTED);
        if (persisted.getBoolean(FLAG))
            return;

        persisted.putBoolean(FLAG, true);
        persistent.put(PERSISTED, persisted);
        ItemStack book = new ItemStack(RegistryHandler.GUIDE_BOOK.get());
        if (!player.getInventory().add(book))
            player.drop(book, false);
    }
}
