package sqlmc;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PianoLib implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PianoLib");
    public static final String MOD_ID = "pianolib";

    /**
     * 26.3 稳健方案：注册时顺手缓存，运行时直接从数组取，彻底不查注册表。
     * 规避 26.2 -> 26.3 注册表查询 API 的反复变更
     * （getEntry -> getOptional / getValueOrThrow 等），对任何版本都兼容。
     */
    public static final SoundEvent[] KEY_SOUNDS = new SoundEvent[88];

    @Override
    public void onInitialize() {
        int registered = 0;
        for (int i = 0; i < 88; i++) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "key_" + i);
            SoundEvent evt = SoundEvent.createVariableRangeEvent(id);
            Registry.register(BuiltInRegistries.SOUND_EVENT, id, evt);
            KEY_SOUNDS[i] = evt; // 缓存
            registered++;
        }
        LOGGER.info("PianoLib: registered {}/88 SoundEvents", registered);
    }
}
