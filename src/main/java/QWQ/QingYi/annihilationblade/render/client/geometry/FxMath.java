package QWQ.QingYi.annihilationblade.render.client.geometry;

import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 特效常用的缓动、插值与向量工具。
 */
@OnlyIn(Dist.CLIENT)
public final class FxMath {
   public static final float TAU = (float)(Math.PI * 2.0);

   private FxMath() {
   }

   public static float clamp01(float value) {
      return value < 0.0F ? 0.0F : Math.min(1.0F, value);
   }

   /** 将 value 从 [from, to] 线性映射到 [0,1] 并截断。 */
   public static float window(float value, float from, float to) {
      return clamp01((value - from) / Math.max(1.0E-4F, to - from));
   }

   public static float easeOutCubic(float t) {
      float inv = 1.0F - clamp01(t);
      return 1.0F - inv * inv * inv;
   }

   public static float easeInCubic(float t) {
      float c = clamp01(t);
      return c * c * c;
   }

   public static float easeInOutSine(float t) {
      return (float)(-(Math.cos(Math.PI * clamp01(t)) - 1.0) * 0.5);
   }

   /** 先升后降的包络：[0,in] 淡入，[out,1] 淡出。 */
   public static float envelope(float t, float in, float out) {
      if (t <= 0.0F || t >= 1.0F) {
         return 0.0F;
      }
      float a = in <= 0.0F ? 1.0F : clamp01(t / in);
      float b = out >= 1.0F ? 1.0F : clamp01((1.0F - t) / (1.0F - out));
      return Math.min(a, b);
   }

   public static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   public static Vector3f randomUnit(RandomSource random) {
      double yaw = random.nextDouble() * Math.PI * 2.0;
      double pitch = Math.asin(random.nextDouble() * 2.0 - 1.0);
      double horizontal = Math.cos(pitch);
      return new Vector3f((float)(Math.cos(yaw) * horizontal), (float)Math.sin(pitch), (float)(Math.sin(yaw) * horizontal));
   }

   /** 求一个与 axis 垂直的单位向量。 */
   public static Vector3f perpendicular(Vector3f axis) {
      Vector3f reference = Math.abs(axis.y) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F);
      return new Vector3f(axis).cross(reference).normalize();
   }
}
