package QWQ.QingYi.annihilationblade.render.client.core;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import java.lang.reflect.Method;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 光影包（Iris / Oculus）兼容探测。
 * <p>
 * 思路参考 ArcaneVortex（MIT，Luoee）：光影包启用时不关闭特效，而是把自定义渲染延后到光影合成完成之后，
 * 并跳过阴影 pass。本类只通过 Iris 公开 API {@code net.irisshaders.iris.api.v0.IrisApi} 反射探测，
 * 不依赖其内部实现；探测失败一律视为「未安装光影」。
 */
@OnlyIn(Dist.CLIENT)
public final class FxCompat {
   private static boolean resolved;
   private static Object irisApi;
   private static Method shaderPackInUse;
   private static Method renderingShadowPass;

   private FxCompat() {
   }

   private static void resolve() {
      if (resolved) {
         return;
      }
      resolved = true;
      try {
         Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         irisApi = apiClass.getMethod("getInstance").invoke(null);
         shaderPackInUse = apiClass.getMethod("isShaderPackInUse");
         try {
            renderingShadowPass = apiClass.getMethod("isRenderingShadowPass");
         } catch (NoSuchMethodException ignored) {
            renderingShadowPass = null;
         }
         Annihilationblade.LOGGER.info("[AnnihilationBlade FX] Iris/Oculus API detected, shader-pack compatibility path enabled.");
      } catch (Throwable ignored) {
         irisApi = null;
      }
   }

   public static boolean shaderPackActive() {
      resolve();
      if (irisApi == null || shaderPackInUse == null) {
         return false;
      }
      try {
         return Boolean.TRUE.equals(shaderPackInUse.invoke(irisApi));
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean shadowPass() {
      resolve();
      if (irisApi == null || renderingShadowPass == null) {
         return false;
      }
      try {
         return Boolean.TRUE.equals(renderingShadowPass.invoke(irisApi));
      } catch (Throwable ignored) {
         return false;
      }
   }
}
