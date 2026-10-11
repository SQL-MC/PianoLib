package sqlmc.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import sqlmc.PianoSounds;

import java.lang.reflect.Method;

/**
 * 对外 API —— Disc Jockey 通过反射调用本类（Class.forName("sqlmc.api.PianoAPI")），
 * 因此方法签名必须与 Fabric 时期保持一致：
 *   - public static boolean isAvailable()
 *   - public static void    playPianoKey(int key, float volume)
 */
public final class PianoAPI {
    private static final float DEFAULT_VOLUME = 2.0f;
    private static final float DEFAULT_PITCH = 1.0f;

    private PianoAPI() {}

    public static boolean isAvailable() {
        return PianoSounds.getKeySound(0) != null;
    }

    public static Identifier getSoundId(int key) {
        return PianoSounds.identifierOf("key_" + key);
    }

    public static void playPianoKey(int key, float volume) {
        SoundEvent evt = PianoSounds.getKeySound(key);
        if (evt == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        // 世界内：优先走 Level 播放，保留空间定位（会随距离衰减）
        if (mc.level != null && mc.player != null) {
            if (playLocalSound(mc, evt, volume, DEFAULT_PITCH)) return;
        }

        // 回退：主菜单 / playLocalSound 签名不匹配 → 走 SoundManager 的 UI 播放
        // （不依赖 level 与 player，主菜单也能发声）
        try {
            float vol = Math.max(0.0f, Math.min(1.0f, volume));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(evt, DEFAULT_PITCH, vol));
        } catch (Throwable ignored) {}
    }

    public static void playPianoKey(int key) {
        playPianoKey(key, DEFAULT_VOLUME);
    }

    // ================= 反射调用 Level#playLocalSound =================
    // 26.x 期间该方法签名多次变动，这里用反射探测，失败即回退到 UI 播放，
    // 避免直调导致编译失败。
    private static volatile Method cachedMethod = null;
    private static volatile boolean probed = false;

    private static boolean playLocalSound(Minecraft mc, SoundEvent evt, float volume, float pitch) {
        try {
            Object level = mc.level;
            if (level == null) return false;

            if (!probed) {
                probed = true;
                cachedMethod = find(level.getClass(), "playLocalSound",
                        BlockPos.class, SoundEvent.class, SoundSource.class,
                        float.class, float.class, boolean.class);
            }

            Method m = cachedMethod;
            if (m == null) return false;

            m.invoke(level, mc.player.blockPosition(), evt,
                    SoundSource.RECORDS, volume, pitch, true);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Method find(Class<?> c, String name, Class<?>... params) {
        try {
            return c.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    private static volatile boolean available = false;

    public static void setAvailable(boolean value) {
        available = value;
    }
}
