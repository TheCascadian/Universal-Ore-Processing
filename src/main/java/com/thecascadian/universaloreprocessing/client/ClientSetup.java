package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(modid = UniversalOreProcessing.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(RegistryHandler.MACHINE_MENU.get(), MachineScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? MaterialTints.colorFor(MaterialItem.materialId(stack)) : -1,
                RegistryHandler.STAGE_ITEMS.values().stream().map(DeferredHolder::get).toArray(Item[]::new));
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        ResourceManagerReloadListener clearTints = resourceManager -> MaterialTints.clear();
        event.registerReloadListener(clearTints);
    }
}
