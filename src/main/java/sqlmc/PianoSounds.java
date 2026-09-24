package sqlmc;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.lang.reflect.Method;

/**
 * PianoLib 音效查询工具 —— 26.3 兼容版（无硬编码映射 API）。
 *
 * 核心思路：不在源码里写死 "Identifier.of / RegistryEntry / Reference" 的具体签名，
 * 全部通过反射探测，适配任意 26.x 映射。这样无论你的映射里方法是 of(String)、
 * of(String,String)、fromNamespaceAndPath 还是 parse，都能编译并运行。
 */
public class PianoSounds {

    /** ★ 必须与 PianoLib.MOD_ID 保持一致（本地常量，避免触发 PianoLib 类初始化引发循环依赖） */
    private static final String MOD_ID = "pianolib";

    /**
     * ★ 若你的 PianoAPI.getKeyIdentifier 里曾有「音高偏移」，在此调整（默认 0 = 无偏移）。
     *   例如原逻辑是 key+12，就把这里改成 12。
     */
    private static final int KEY_BASE_OFFSET = 0;

    // ================= Identifier 工厂（反射探测，类加载时执行一次） =================
    private interface IdFactory { Identifier apply(String namespace, String path); }
    private static volatile IdFactory ID_FACTORY;

    static {
        try {
            Class<?> idClass = Identifier.class;
            // 优先用双参 of(ns, path)
            Method of2 = findStatic(idClass, "of", String.class, String.class);
            if (of2 != null) {
                ID_FACTORY = (ns, p) -> invoke(of2, ns, p);
            } else {
                // 退化探测：of(String "ns:path") / fromNamespaceAndPath / parse / 构造器
                final Method of1   = findStatic(idClass, "of", String.class);
                final Method fnp   = findStatic(idClass, "fromNamespaceAndPath", String.class, String.class);
                final Method parse = findStatic(idClass, "parse", String.class);
                ID_FACTORY = (ns, p) -> {
                    String joined = ns + ":" + p;
                    if (of1 != null)   return invoke(of1, joined);
                    if (fnp != null)   return invoke(fnp, ns, p);
                    if (parse != null) return invoke(parse, joined);
                    try { return (Identifier) idClass.getConstructor(String.class, String.class).newInstance(ns, p); }
                    catch (Exception e) { throw new RuntimeException("[PianoLib] 无法构造 Identifier: " + joined, e); }
                };
            }
            System.out.println("[PianoLib] Identifier 工厂就绪: " + describe(idClass));
        } catch (Exception e) {
            System.err.println("[PianoLib] Identifier 工厂探测失败: " + e);
        }
    }

    private static Identifier invoke(Method m, Object... args) {
        try { return (Identifier) m.invoke(null, args); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private static Method findStatic(Class<?> c, String name, Class<?>... params) {
        try { return c.getMethod(name, params); } catch (NoSuchMethodException e) { return null; }
    }
    private static String describe(Class<?> c) {
        StringBuilder sb = new StringBuilder(c.getName()).append(" [");
        for (Method m : c.getMethods()) sb.append(m.getName()).append(' ');
        return sb.append(']').toString();
    }

    // ================= 对外 API =================

    /** 由名字构造 PianoLib 域内的 Identifier（供 PianoAPI / 内部调用）。 */
    public static Identifier identifierOf(String name) {
        if (ID_FACTORY == null) return null;
        return ID_FACTORY.apply(MOD_ID, name);
    }

    /**
     * 根据钢琴键号取对应的 SoundEvent。
     * ★ 26.3 关键改动：直接构造 identifier（打破与 PianoAPI 的循环依赖），
     *   并通过反射解包注册表返回值（不引用 RegistryEntry 类型）。
     */
    public static SoundEvent getKeySound(int key) {
        Identifier id = identifierOf("key_" + (key + KEY_BASE_OFFSET));
        if (id == null) return null;
        Object raw = BuiltInRegistries.SOUND_EVENT.get(id);
        return unwrap(raw);
    }

    /**
     * 把注册表返回值统一转为 SoundEvent。
     * 兼容三种形态：SoundEvent / Optional<SoundEvent> / Optional<Reference<SoundEvent>> / Reference<SoundEvent>。
     */
    private static SoundEvent unwrap(Object raw) {
        if (raw == null) return null;
        if (raw instanceof SoundEvent) return (SoundEvent) raw;       // 形态A
        Class<?> c = raw.getClass();
        // 形如 Optional<...>：调 orElse(null) 取内层
        Method orElse = findMethod(c, "orElse", Object.class);
        if (orElse != null) {
            try { return unwrap(orElse.invoke(raw, (Object) null)); } // 递归：内层可能是 Reference 或 SoundEvent
            catch (ReflectiveOperationException ignored) { /* fall through */ }
        }
        // 形如 RegistryEntry.Reference：调 value()
        Method value = findMethod(c, "value");
        if (value != null) {
            try { return (SoundEvent) value.invoke(raw); }
            catch (ReflectiveOperationException ignored) { return null; }
        }
        return null;
    }
    private static Method findMethod(Class<?> c, String name, Class<?>... params) {
        try { return c.getMethod(name, params); } catch (NoSuchMethodException e) { return null; }
    }
}