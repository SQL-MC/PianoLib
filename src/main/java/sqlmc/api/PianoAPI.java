package sqlmc.api;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import sqlmc.PianoSounds;

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
        if (mc.level == null || mc.player == null) return;
        BlockPos pos = mc.player.blockPosition();
        mc.level.playLocalSound(pos, evt, SoundSource.RECORDS, volume, DEFAULT_PITCH, true);
    }

    public static void playPianoKey(int key) {
        playPianoKey(key, DEFAULT_VOLUME);
    }
}
