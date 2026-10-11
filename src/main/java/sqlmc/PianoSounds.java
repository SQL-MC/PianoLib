package sqlmc;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * PianoLib 音效查询 —— NeoForge 26.3 版。
 *
 * 原版靠一堆反射去探测 Identifier 工厂和注册表返回值形态，是因为要同时兼容
 * 26.x 各版本不断变化的 API。现在既然已经确定运行在 26.3 上，可以直接使用
 * 已验证存在的方法，代码大幅简化：
 *   - Identifier.fromNamespaceAndPath(ns, path)   —— Disc Jockey 项目中已验证可用
 *   - 音效本体从 PianoLib.KEY_SUPPLIERS 数组直接取，完全不查注册表
 */
public final class PianoSounds {

    /** 必须与 PianoLib.MOD_ID 保持一致（本地常量，避免与 PianoLib 类初始化互相触发） */
    private static final String MOD_ID = "pianolib";

    /**
     * 键号偏移。PianoLib 的 sounds.json 中 key_0 = A0、key_39 = C4、key_87 = C8，
     * 与标准钢琴键号一致，故偏移为 0。若你的音效命名另有一套偏移，在此调整。
     */
    private static final int KEY_BASE_OFFSET = 0;

    private PianoSounds() {}

    /** 构造 PianoLib 域内的 Identifier。 */
    public static Identifier identifierOf(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    /**
     * 根据钢琴键号取 SoundEvent（0 = A0，39 = C4，87 = C8）。
     * 注册尚未完成时安全返回 null。
     */
    public static SoundEvent getKeySound(int key) {
        int k = key + KEY_BASE_OFFSET;
        Supplier<SoundEvent>[] arr = PianoLib.KEY_SUPPLIERS;
        if (arr == null || k < 0 || k >= arr.length) return null;

        Supplier<SoundEvent> supplier = arr[k];
        if (supplier == null) return null;

        try {
            return supplier.get();
        } catch (Throwable t) {
            // DeferredRegister 尚未完成注册时 get() 会抛异常，此处安全降级
            return null;
        }
    }
}
