package qwq.qingyi.annihilationblade.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;

/**
 * 模组自定义音效注册表。
 */
public final class ModSounds {
    private static final Identifier LOLI_SUCCESS_ID = AnnihilationBladeex.prefix("loli_success");
    public static final SoundEvent LOLI_SUCCESS = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            LOLI_SUCCESS_ID,
            SoundEvent.createVariableRangeEvent(LOLI_SUCCESS_ID)
    );

    private ModSounds() {
    }

    public static void register() {
        // 触发静态初始化完成注册
    }
}
