package dev.mobrealms.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MobRealms implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mobrealms");
    @Override public void onInitialize() {
        LOGGER.info("Mob Realms simulation core initialized");
    }
}
