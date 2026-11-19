package QWQ.QingYi.annihilationblade.render.client.core;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 本模组唯一的镜头震动出口。
 * <p>
 * 所有震动来源（专属渲染器特效、魔龙夜陨震屏实体等）只负责累加各自的偏转角度，
 * 由这里统一乘以「镜头震动强度」倍率，再把每个轴截断到「镜头震动上限」以内。
 * 这样多个技能同时释放时也不会叠出失控的晃动。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Annihilationblade.MODID, value = Dist.CLIENT)
public final class FxCameraShake {
   /** 震动来源：把本帧的偏转角度（度）累加到 out = {yaw, pitch, roll}。 */
   @FunctionalInterface
   public interface Source {
      void contribute(float partialTick, float[] out);
   }

   private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

   static {
      SOURCES.add(FxManager::contributeShake);
   }

   private FxCameraShake() {
   }

   public static void addSource(Source source) {
      if (!SOURCES.contains(source)) {
         SOURCES.add(source);
      }
   }

   @SubscribeEvent
   public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null || minecraft.isPaused()) {
         return;
      }
      float[] offsets = new float[3];
      float partialTick = (float)event.getPartialTick();
      for (Source source : SOURCES) {
         source.contribute(partialTick, offsets);
      }
      if (offsets[0] == 0.0F && offsets[1] == 0.0F && offsets[2] == 0.0F) {
         return;
      }
      ModConfig.ClientRenderer config = ModConfig.CLIENT.clientRenderer;
      float multiplier = (float)(double)config.screenShake.getValue();
      float cap = (float)(double)config.screenShakeMax.getValue();
      event.setYaw(event.getYaw() + clamp(offsets[0] * multiplier, cap));
      event.setPitch(event.getPitch() + clamp(offsets[1] * multiplier, cap));
      event.setRoll(event.getRoll() + clamp(offsets[2] * multiplier, cap));
   }

   /** 视野冲击（FOV 扩张/收缩），力度同样受震动倍率约束。 */
   @SubscribeEvent
   public static void onComputeFov(ViewportEvent.ComputeFov event) {
      float kick = FxManager.fovKick();
      if (Math.abs(kick) < 1.0E-4F || !event.usedConfiguredFov()) {
         return;
      }
      float multiplier = (float)Math.min(1.0, ModConfig.CLIENT.clientRenderer.screenShake.getValue());
      event.setFOV(event.getFOV() * (1.0 + kick * multiplier));
   }

   private static float clamp(float value, float cap) {
      return Math.max(-cap, Math.min(cap, value));
   }
}
