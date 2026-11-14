package qwq.qingyi.annihilationblade.client.infinity_stellaris;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import qwq.qingyi.annihilationblade.registry.ModEntities;

/**
 * 注册无尽星空相关实体客户端渲染器。
 */
public final class InfinityStellarisClientEvents {
    private InfinityStellarisClientEvents() {
    }

    public static void register() {
        EntityRendererRegistry.register(ModEntities.GAMMA_THUNDERBOLT, GammaThunderboltRenderer::new);
    }
}
