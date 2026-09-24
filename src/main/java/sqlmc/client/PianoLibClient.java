package sqlmc.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sqlmc.api.PianoAPI;

public class PianoLibClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PianoLib");

    @Override
    public void onInitializeClient() {
        LOGGER.info("PianoLib: client init, available={}", PianoAPI.isAvailable());
        ClientLifecycleEvents.CLIENT_STARTED.register(mc ->
            PianoAPI.playPianoKey(39, 1.0f)
        );
    }
}
