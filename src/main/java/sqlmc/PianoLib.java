package sqlmc;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import sqlmc.client.PianoLibClient;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * PianoLib —— NeoForge 26.3 版主类。
 *
 * 原 Fabric 版用 ModInitializer + Registry.register，在 NeoForge 下 onInitialize()
 * 不会被调用，导致 88 个 SoundEvent 从未注册 → PianoAPI.isAvailable() 恒为 false。
 *
 * 本版改用 NeoForge 的 DeferredRegister；同时保留「注册时顺手缓存、运行时直接从数组取」
 * 的设计，彻底不查注册表 —— 规避 26.2 -> 26.3 注册表查询 API 的反复变更
 * （getEntry -> getOptional / getValueOrThrow 等）。
 */
@Mod(PianoLib.MOD_ID)
public class PianoLib {
    public static final String MOD_ID = "pianolib";
    private static final Logger LOGGER = LoggerFactory.getLogger("PianoLib");

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    /**
     * 88 键对应 SoundEvent 的 Supplier 缓存（A0=0 ... C4=39 ... C8=87）。
     * 运行时经 PianoSounds.getKeySound() 取用，不查询注册表。
     */
    public static final Supplier<SoundEvent>[] KEY_SUPPLIERS = newSupplierArray(88);

    @SuppressWarnings("unchecked")
    private static Supplier<SoundEvent>[] newSupplierArray(int n) {
        return new Supplier[n];
    }

    public PianoLib(IEventBus bus) {
        SOUND_EVENTS.register(bus);                       // 你原有的注册，保留

        // 客户端装配：FMLClientSetupEvent 属于 mod 总线
        bus.addListener(PianoLibClient::onClientSetup);
    }
}
