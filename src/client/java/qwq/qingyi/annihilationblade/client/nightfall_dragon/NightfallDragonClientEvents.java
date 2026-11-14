package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import qwq.qingyi.annihilationblade.registry.ModEntities;

/**
 * 注册永夜魔龙相关实体客户端渲染器。
 */
public final class NightfallDragonClientEvents {
    private NightfallDragonClientEvents() {
    }

    public static void register() {
        EntityRendererRegistry.register(ModEntities.SCALE_GUARD_SWORD, ScaleGuardControllerRenderer::new);
        EntityRendererRegistry.register(ModEntities.NIGHTFALL_SCREEN_SHAKE, NightfallDragonScreenShakeRenderer::new);
    }
}
