package qwq.qingyi.annihilationblade.client.infinity_stellaris;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * 伽马天雷实体渲染状态快照，在提取阶段保存随机种子与固定色彩。
 */
public class GammaThunderboltRenderState extends EntityRenderState {
    public long seed;
    public int fixedColor = -1;
}
