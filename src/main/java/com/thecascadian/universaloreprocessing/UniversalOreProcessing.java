package com.thecascadian.universaloreprocessing;

import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(UniversalOreProcessing.MODID)
public class UniversalOreProcessing {
    public static final String MODID = "universaloreprocessing";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public UniversalOreProcessing(IEventBus modEventBus, ModContainer modContainer) {
        OreProcessingConfig.register(modContainer);
        // inform pack makers / server admins where the generated config lives
        LOGGER.info("[UniversalOreProcessing] config available at config/{}-common.toml; " +
                "edit the exclusion lists and stage options there (see comments in file).",
                MODID);
        RegistryHandler.init(modEventBus);
    }
}
