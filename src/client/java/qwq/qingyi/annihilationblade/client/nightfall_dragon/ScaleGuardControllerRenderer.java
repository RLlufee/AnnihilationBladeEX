package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import qwq.qingyi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity;

/**
 * 鳞卫护体控制实体的空渲染器。
 */
public class ScaleGuardControllerRenderer extends EntityRenderer<ScaleGuardSwordEntity, EntityRenderState> {
    public ScaleGuardControllerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
