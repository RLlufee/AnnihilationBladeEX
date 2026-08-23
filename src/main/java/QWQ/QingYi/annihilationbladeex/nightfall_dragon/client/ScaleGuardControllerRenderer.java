package QWQ.QingYi.annihilationbladeex.nightfall_dragon.client;

import QWQ.QingYi.annihilationbladeex.nightfall_dragon.entity.ScaleGuardSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class ScaleGuardControllerRenderer extends EntityRenderer<ScaleGuardSwordEntity> {
   public ScaleGuardControllerRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(ScaleGuardSwordEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
   }

   @Override
   public ResourceLocation getTextureLocation(ScaleGuardSwordEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
