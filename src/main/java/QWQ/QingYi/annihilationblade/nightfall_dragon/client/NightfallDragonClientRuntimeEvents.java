package QWQ.QingYi.annihilationblade.nightfall_dragon.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Annihilationblade.MODID, value = Dist.CLIENT)
public final class NightfallDragonClientRuntimeEvents {
   private NightfallDragonClientRuntimeEvents() {
   }

   @SubscribeEvent
   public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
      Minecraft minecraft = Minecraft.getInstance();
      Player player = minecraft.player;
      if (player == null || minecraft.isPaused()) {
         return;
      }

      float partialTick = minecraft.getFrameTime();
      float age = player.tickCount + partialTick;
      float shakeAmplitude = 0.0F;
      for (NightfallDragonScreenShakeEntity shake : player.level().getEntitiesOfClass(NightfallDragonScreenShakeEntity.class, player.getBoundingBox().inflate(24.0D))) {
         if (shake.distanceTo(player) < shake.getRadius()) {
            shakeAmplitude += shake.getShakeAmount(player, partialTick);
         }
      }

      if (shakeAmplitude <= 0.0F) {
         return;
      }

      shakeAmplitude = Math.min(shakeAmplitude, 1.0F);
      event.setPitch((float)(event.getPitch() + shakeAmplitude * Math.cos(age * 3.0F + 2.0F) * 25.0F));
      event.setYaw((float)(event.getYaw() + shakeAmplitude * Math.cos(age * 5.0F + 1.0F) * 25.0F));
      event.setRoll((float)(event.getRoll() + shakeAmplitude * Math.cos(age * 4.0F) * 25.0F));
   }
}
