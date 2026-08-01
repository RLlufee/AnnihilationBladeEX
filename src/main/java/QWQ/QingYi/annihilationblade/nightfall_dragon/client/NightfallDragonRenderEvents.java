package QWQ.QingYi.annihilationblade.nightfall_dragon.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Annihilationblade.MODID, value = Dist.CLIENT)
public final class NightfallDragonRenderEvents {
   private static final int FLOW_FRAMES = 32;
   private static final String ITEM_HALO_TARGET = "item_dragon_halo_luminous";
   private static final ThreadLocal<Boolean> RENDERING_EXTRA_LAYER = ThreadLocal.withInitial(() -> false);

   private NightfallDragonRenderEvents() {
   }

   @SubscribeEvent
   public static void onSlashBladeRenderOverride(RenderOverrideEvent event) {
      if (!NightfallDragonDefinitions.isNightfallDragon(event.getStack())) {
         return;
      }

      String target = event.getTarget();
      if (target == null || !target.contains("luminous")) {
         return;
      }

      if (RENDERING_EXTRA_LAYER.get()) {
         return;
      }

      int frame = (int) ((Util.getMillis() / 80L) % FLOW_FRAMES);
      try {
         RENDERING_EXTRA_LAYER.set(true);
         renderRotatingItemHalo(event);
         BladeRenderState.setCol(0xFFFF4DFF, true);
         BladeRenderState.renderChargeEffect(
               event.getStack(),
               frame,
               event.getModel(),
               target,
               event.getTexture(),
               event.getPoseStack(),
               event.getBuffer(),
               BladeRenderState.MAX_LIGHT);
      } finally {
         RENDERING_EXTRA_LAYER.set(false);
      }
   }

   private static void renderRotatingItemHalo(RenderOverrideEvent event) {
      String target = event.getTarget();
      if (target == null || !target.startsWith("item_")) {
         return;
      }

      long time = Util.getMillis();
      float rotation = (time % 12000L) * 0.03F;

      event.getPoseStack().pushPose();
      try {
         event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(rotation));
         BladeRenderState.setCol(0xF4E84DFF, true);
         BladeRenderState.renderOverridedLuminous(
               event.getStack(),
               event.getModel(),
               ITEM_HALO_TARGET,
               NightfallDragonDefinitions.HALO_TEXTURE,
               event.getPoseStack(),
               event.getBuffer(),
               BladeRenderState.MAX_LIGHT);
      } finally {
         event.getPoseStack().popPose();
      }
   }
}
