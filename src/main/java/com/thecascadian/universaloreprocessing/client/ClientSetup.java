package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.SlurryCauldronBlockEntity;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.network.Feedback;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = UniversalOreProcessing.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        Feedback.setParticleHandler(ClientFeedback::spawn);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RegistryHandler.CRUSHING_SLAB_BLOCK_ENTITY.get(), CrushingSlabRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // layer n of a ladder form model is tint index n: shadow, mid, light
        event.register((stack, tintIndex) -> MaterialTints.band(FormItem.materialId(stack), tintIndex),
                RegistryHandler.CLUMPS.get(),
                RegistryHandler.DUST.get(),
                RegistryHandler.SHARDS.get());
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // index 0 is the slurry surface (mid band), index 1 the crystals growing on it (light band)
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof SlurryCauldronBlockEntity cauldron))
                return -1;
            return MaterialTints.band(cauldron.material(), tintIndex == 1 ? MaterialTints.LIGHT : MaterialTints.MID);
        }, RegistryHandler.SLURRY_CAULDRON.get());
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        ResourceManagerReloadListener clearTints = resourceManager -> MaterialTints.clear();
        event.registerReloadListener(clearTints);
    }
}
