package qwq.qingyi.annihilationblade.client.blood_prison;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;

/**
 * 注册血狱领域的屏幕覆层 HUD 元素。
 */
public final class BloodPrisonOverlay {
    private BloodPrisonOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(
                AnnihilationBladeex.prefix("blood_prison_domain"),
                BloodPrisonClientEffects::renderOverlay
        );
    }
}
