package sqlmc.client;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import sqlmc.api.PianoAPI;

public final class PianoLibClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PianoLibClient() {}

    /** 由 PianoLib 主类在 mod 总线上注册，不再使用 @EventBusSubscriber */
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 此阶段 DeferredRegister 已完成注册，88 个 SoundEvent 可用
        PianoAPI.setAvailable(true);
        LOGGER.info("PianoLib: client setup done, available=true");
    }
}