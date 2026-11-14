package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import qwq.qingyi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;

/**
 * 屏幕震动逻辑实体的空渲染器。
 */
public class NightfallDragonScreenShakeRenderer extends EntityRenderer<NightfallDragonScreenShakeEntity, EntityRenderState> {
    public NightfallDragonScreenShakeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
