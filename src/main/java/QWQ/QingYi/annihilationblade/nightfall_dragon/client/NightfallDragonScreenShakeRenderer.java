package QWQ.QingYi.annihilationblade.nightfall_dragon.client;

import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class NightfallDragonScreenShakeRenderer extends EntityRenderer<NightfallDragonScreenShakeEntity> {
   public NightfallDragonScreenShakeRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(NightfallDragonScreenShakeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
   }

   @Override
   public ResourceLocation getTextureLocation(NightfallDragonScreenShakeEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
