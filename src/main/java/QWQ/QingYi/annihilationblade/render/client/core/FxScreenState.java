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
   public float aberration;
   public float zoomBlur;
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
      this.flash = this.aberration = this.zoomBlur = this.vignette = this.desaturate = this.bloomBoost = 0.0F;
      this.impactFrame = 0.0F;
      this.impactInvert = false;
      this.fovKick = 0.0F;
      this.flashR = this.flashG = this.flashB = 1.0F;
      this.vignetteR = this.vignetteG = this.vignetteB = 0.0F;
      this.centerX = this.centerY = 0.5F;
   }

   public boolean idle() {
      return this.flash < 1.0E-3F && this.aberration < 1.0E-3F && this.zoomBlur < 1.0E-3F && this.vignette < 1.0E-3F && this.desaturate < 1.0E-3F
         && this.impactFrame < 1.0E-3F;
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
      if (strength > this.flash) {
         this.flash = strength;
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

   /** 径向模糊/色差中心以最近一次更强的效果为准。 */
   public void addZoomBlur(float strength, float[] centerUv) {
      if (strength > this.zoomBlur) {
         this.zoomBlur = strength;
         if (centerUv != null) {
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
