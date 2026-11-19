package QWQ.QingYi.annihilationblade.render.client.core;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 每帧由所有活跃特效共同写入的屏幕级效果。多个特效同时存在时取最强值，而不是叠加，避免画面失控。
 */
@OnlyIn(Dist.CLIENT)
public final class FxScreenState {
   public float flash;
   public float flashR = 1.0F;
   public float flashG = 1.0F;
   public float flashB = 1.0F;
   /** 爆闪过曝与胶片颗粒增强系数（0 = 柔和闪白，1 = 强烈焰过曝感）。 */
   public float flashBoost;
   public float aberration;
   public float zoomBlur;
   /** 边缘向心速度线模糊强度（中心保持清晰，四周拉丝 + 轻微压暗）。 */
   public float dashBlur;
   /** 径向效果中心（屏幕 UV）。 */
   public float centerX = 0.5F;
   public float centerY = 0.5F;
   public float vignette;
   public float vignetteR;
   public float vignetteG;
   public float vignetteB;
   public float desaturate;
   /** 泛光额外增益（叠加到配置的泛光强度上）。 */
   public float bloomBoost;
   /** 冲击帧（动画式黑白定格）强度与是否反相。 */
   public float impactFrame;
   public boolean impactInvert;
   /** 视野冲击：>0 视野扩张，<0 视野收缩（比例）。 */
   public float fovKick;

   void reset() {
      this.flash = this.flashBoost = this.aberration = this.zoomBlur = this.dashBlur = this.vignette = this.desaturate = this.bloomBoost = 0.0F;
      this.impactFrame = 0.0F;
      this.impactInvert = false;
      this.fovKick = 0.0F;
      this.flashR = this.flashG = this.flashB = 1.0F;
      this.vignetteR = this.vignetteG = this.vignetteB = 0.0F;
      this.centerX = this.centerY = 0.5F;
   }

   public boolean idle() {
      return this.flash < 1.0E-3F && this.aberration < 1.0E-3F && this.zoomBlur < 1.0E-3F && this.dashBlur < 1.0E-3F
         && this.vignette < 1.0E-3F && this.desaturate < 1.0E-3F && this.impactFrame < 1.0E-3F;
   }

   /** 冲击帧：画面瞬间变为高对比黑白（发光处为白），invert 为 true 时黑白反相。 */
   public void addImpactFrame(float strength, boolean invert) {
      if (strength > this.impactFrame) {
         this.impactFrame = strength;
         this.impactInvert = invert;
      }
   }

   public void addFovKick(float amount) {
      if (Math.abs(amount) > Math.abs(this.fovKick)) {
         this.fovKick = amount;
      }
   }

   public void addFlash(int rgb, float strength) {
      this.addFlash(rgb, strength, 0.35F);
   }

   /** 带过曝与细腻胶片颗粒控制的爆闪（适合大招坍缩、天柱引爆等强冲击瞬间）。 */
   public void addFlash(int rgb, float strength, float boost) {
      if (strength > this.flash) {
         this.flash = strength;
         this.flashBoost = Math.max(0.0F, Math.min(1.5F, boost));
         this.flashR = (rgb >> 16 & 0xFF) / 255.0F;
         this.flashG = (rgb >> 8 & 0xFF) / 255.0F;
         this.flashB = (rgb & 0xFF) / 255.0F;
      }
   }

   public void addVignette(int rgb, float strength) {
      if (strength > this.vignette) {
         this.vignette = strength;
         this.vignetteR = (rgb >> 16 & 0xFF) / 255.0F;
         this.vignetteG = (rgb >> 8 & 0xFF) / 255.0F;
         this.vignetteB = (rgb & 0xFF) / 255.0F;
      }
   }

   public void addAberration(float strength) {
      this.aberration = Math.max(this.aberration, strength);
   }

   /** 径向模糊中心以最近一次更强的效果为准。 */
   public void addZoomBlur(float strength, float[] centerUv) {
      if (strength > this.zoomBlur) {
         this.zoomBlur = strength;
         if (centerUv != null) {
            this.centerX = centerUv[0];
            this.centerY = centerUv[1];
         }
      }
   }

   /** 边缘向心速度线模糊：中心区域保持清晰，四周拉丝与微压暗（适合高速突进、次元斩瞬断、天柱爆发）。 */
   public void addDashBlur(float strength, float[] centerUv) {
      if (strength > this.dashBlur) {
         this.dashBlur = strength;
         if (centerUv != null && this.zoomBlur <= strength) {
            this.centerX = centerUv[0];
            this.centerY = centerUv[1];
         }
      }
   }

   public void addDesaturate(float strength) {
      this.desaturate = Math.max(this.desaturate, strength);
   }

   public void addBloomBoost(float strength) {
      this.bloomBoost = Math.max(this.bloomBoost, strength);
   }
}
