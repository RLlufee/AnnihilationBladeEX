package QWQ.QingYi.annihilationblade.nightfall_dragon.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.DragonHeadChargeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class DragonHeadChargeRenderer extends EntityRenderer<DragonHeadChargeEntity> {
   private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/dragon_head.obj");
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/dragon_head.png");
   private static final String MODEL_PART = "base";
   private static final float SCALE = 5.0F;
   private static boolean loggedFirstRender;

   public DragonHeadChargeRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(DragonHeadChargeEntity head, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
      float yaw = Mth.rotLerp(partialTicks, head.yRotO, head.getYRot());
      float pitch = Mth.lerp(partialTicks, head.xRotO, head.getXRot());
      if (!loggedFirstRender) {
         loggedFirstRender = true;
         Annihilationblade.LOGGER.info("Rendering dragon_head_charge with model={}, texture={}, part={}", MODEL, TEXTURE, MODEL_PART);
      }

      poseStack.pushPose();
      try {
         poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
         poseStack.scale(SCALE, SCALE, SCALE);
         poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
         BladeRenderState.setCol(0xFFFFFFFF, true);
         BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, MODEL_PART, TEXTURE, poseStack, bufferSource, BladeRenderState.MAX_LIGHT);
         BladeRenderState.renderOverridedReverseLuminous(ItemStack.EMPTY, model, MODEL_PART, TEXTURE, poseStack, bufferSource, BladeRenderState.MAX_LIGHT);
         BladeRenderState.resetCol();
      } finally {
         poseStack.popPose();
      }

      super.render(head, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(DragonHeadChargeEntity head) {
      return TEXTURE;
   }
}
