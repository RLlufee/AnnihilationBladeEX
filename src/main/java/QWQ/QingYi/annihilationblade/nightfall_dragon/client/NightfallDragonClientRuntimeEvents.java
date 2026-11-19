package QWQ.QingYi.annihilationblade.nightfall_dragon.client;

import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import QWQ.QingYi.annihilationblade.render.client.core.FxCameraShake;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 魔龙夜陨震屏实体的镜头震动来源。
 * <p>
 * 原先直接在相机角度事件里叠加（最大 ±25°）；现改为 {@link FxCameraShake} 的一个来源，
 * 与其他特效的震动合并后统一乘「镜头震动强度」倍率并截断到「镜头震动上限」。
 * 由 {@code AnnihilationBladeClientHandler} 在客户端初始化时注册。
 */
@OnlyIn(Dist.CLIENT)
public final class NightfallDragonClientRuntimeEvents {
   private static final float MAX_DEGREES = 25.0F;

   private NightfallDragonClientRuntimeEvents() {
   }

   public static void contributeShake(float partialTick, float[] out) {
      Minecraft minecraft = Minecraft.getInstance();
      Player player = minecraft.player;
      if (player == null) {
         return;
      }

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
      out[0] += (float)(shakeAmplitude * Math.cos(age * 5.0F + 1.0F) * MAX_DEGREES);
      out[1] += (float)(shakeAmplitude * Math.cos(age * 3.0F + 2.0F) * MAX_DEGREES);
      out[2] += (float)(shakeAmplitude * Math.cos(age * 4.0F) * MAX_DEGREES);
   }
}
