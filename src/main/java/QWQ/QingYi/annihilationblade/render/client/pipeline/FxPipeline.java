package QWQ.QingYi.annihilationblade.render.client.pipeline;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.render.client.core.FxCompat;
import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxPass;
import QWQ.QingYi.annihilationblade.render.client.core.FxScreenState;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 专属渲染管线。每帧（仅在有活跃特效时）执行：
 * <ol>
 *   <li>深度捕获：在 Fabulous 透明度后处理覆盖主深度之前，把场景深度复制到 R32F 纹理（AFTER_WEATHER）。</li>
 *   <li>网格特效层：法阵、刀光、闪电、辉光等写入 HDR（RGBA16F）特效层，片元内做软深度遮挡。</li>
 *   <li>场景通道（B 档）：复制主画面后串行执行空间碎裂折射 / 黑洞透镜 / 冲击波扭曲。</li>
 *   <li>体积通道（B 档）：光线步进的体积裂隙辉光，低分辨率累积。</li>
 *   <li>泛光：仅对特效层 + 体积层做 6 级降采样/升采样（不会让整个场景发灰发白）。</li>
 *   <li>合成：场景 + 特效 + 泛光，叠加闪白、色差、径向模糊、暗角、去色，写回主画面。</li>
 * </ol>
 * 整个流程在 AFTER_LEVEL 执行：位于 LevelRenderer.renderLevel 返回之后（即光影包完成合成之后）、手部渲染之前，
 * 因此天然兼容延迟绘制，且不会影响第一人称手持物。
 * <p>
 * 架构参考：the-shooting-star-demo（仅参考思路，代码为自行实现）、ArcaneVortex / fantasy_ending（MIT，Luoee）。
 */
@OnlyIn(Dist.CLIENT)
public final class FxPipeline {
   public static final FxPipeline INSTANCE = new FxPipeline();
   private static final int BLOOM_LEVELS = 6;

   private final FxTarget depth = new FxTarget(FxTarget.Format.R32F);
   private final FxTarget fx = new FxTarget(FxTarget.Format.RGBA16F);
   private final FxTarget volume = new FxTarget(FxTarget.Format.RGBA16F);
   private final FxTarget sceneA = new FxTarget(FxTarget.Format.RGBA8);
   private final FxTarget sceneB = new FxTarget(FxTarget.Format.RGBA8);
   private final FxTarget[] bloomDown = new FxTarget[BLOOM_LEVELS];
   private final FxTarget[] bloomUp = new FxTarget[BLOOM_LEVELS];

   private boolean depthCaptured;
   private boolean failed;
   private boolean loggedFailure;

   private FxPipeline() {
      for (int i = 0; i < BLOOM_LEVELS; i++) {
         this.bloomDown[i] = new FxTarget(FxTarget.Format.RGBA16F);
         this.bloomUp[i] = new FxTarget(FxTarget.Format.RGBA16F);
      }
   }

   /** 渲染器当前是否可用（配置开启、着色器就绪、未发生运行时错误、光影包策略允许）。 */
   public boolean active() {
      ModConfig.ClientRenderer config = ModConfig.CLIENT.clientRenderer;
      if (this.failed || !config.enabled.getValue() || !FxShaders.coreReady()) {
         return false;
      }
      return !FxCompat.shaderPackActive() || config.withShaderPack.getValue();
   }

   public boolean volumetricEnabled() {
      return ModConfig.CLIENT.clientRenderer.volumetric.getValue();
   }

   /** 体积光线步进步数，由画质档位决定。 */
   public int volumeSteps() {
      return switch (ModConfig.CLIENT.clientRenderer.volumetricQuality.getValue()) {
         case 1 -> 12;
         case 3 -> 32;
         default -> 22;
      };
   }

   private float volumeScale() {
      return ModConfig.CLIENT.clientRenderer.volumetricQuality.getValue() >= 3 ? 1.0F : 0.5F;
   }

   void onShadersReloaded() {
      this.failed = false;
      this.loggedFailure = false;
   }

   public void beginFrame() {
      this.depthCaptured = false;
   }

   /**
    * 复制当前主深度。会恢复调用前的帧缓冲绑定与视口（Fabulous 下此时绑定的是天气目标）。
    */
   public void captureDepth() {
      ShaderInstance shader = FxShaders.depthCopy;
      if (shader == null || this.failed) {
         return;
      }
      RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
      if (main.getDepthTextureId() <= 0) {
         return;
      }
      int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
      int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
      int[] viewport = new int[4];
      GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
      StateGuard guard = StateGuard.capture();
      int savedTexture = RenderSystem.getShaderTexture(0);
      try {
         this.depth.ensure(main.width, main.height);
         this.depth.bindWrite();
         RenderSystem.disableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.setShaderTexture(0, main.getDepthTextureId());
         fullscreen(shader);
         this.depthCaptured = true;
      } catch (Throwable throwable) {
         this.fail("depth capture", throwable);
      } finally {
         RenderSystem.setShaderTexture(0, savedTexture);
         guard.restore();
         GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
         GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
         GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
      }
   }

   /**
    * 执行整帧特效渲染并合成回主画面。
    */
   public void render(FxFrame frame, List<FxInstance> instances, FxScreenState screen) {
      if (this.failed || FxShaders.world == null || FxShaders.composite == null) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      RenderTarget main = minecraft.getMainRenderTarget();
      int width = main.width;
      int height = main.height;
      if (!this.depthCaptured) {
         this.captureDepth();
         if (!this.depthCaptured) {
            return;
         }
      }

      ModConfig.ClientRenderer config = ModConfig.CLIENT.clientRenderer;
      StateGuard guard = StateGuard.capture();
      int[] savedTextures = new int[4];
      for (int i = 0; i < savedTextures.length; i++) {
         savedTextures[i] = RenderSystem.getShaderTexture(i);
      }
      RenderSystem.backupProjectionMatrix();
      PoseStack modelView = RenderSystem.getModelViewStack();
      modelView.pushPose();
      try {
         // 1. 网格特效层
         this.fx.ensure(width, height);
         this.fx.clear();
         this.fx.bindWrite();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.setShaderTexture(0, this.depth.texture());
         RenderSystem.setProjectionMatrix(frame.projection, VertexSorting.DISTANCE_TO_ORIGIN);
         modelView.setIdentity();
         modelView.mulPoseMatrix(frame.view);
         RenderSystem.applyModelViewMatrix();
         FxGeometry geometry = new FxGeometry(frame);
         for (FxInstance instance : instances) {
            instance.renderGeometry(geometry, frame);
            geometry.clearOccluder();
         }
         modelView.setIdentity();
         RenderSystem.applyModelViewMatrix();

         // 2. 收集 B 档通道
         List<FxPass> scenePasses = new ArrayList<>();
         List<FxPass> volumePasses = new ArrayList<>();
         if (this.volumetricEnabled()) {
            List<FxPass> passes = new ArrayList<>();
            for (FxInstance instance : instances) {
               instance.collectPasses(frame, passes);
            }
            for (FxPass pass : passes) {
               if (pass.shader().get() != null) {
                  (pass.kind() == FxPass.Kind.SCENE ? scenePasses : volumePasses).add(pass);
               }
            }
         }

         // 3. 场景通道：复制主画面后串行处理
         this.sceneA.ensure(width, height);
         this.sceneB.ensure(width, height);
         GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
         GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.sceneA.framebuffer());
         GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
         int sceneTexture = this.sceneA.texture();
         boolean writeB = true;
         RenderSystem.disableBlend();
         for (FxPass pass : scenePasses) {
            FxTarget out = writeB ? this.sceneB : this.sceneA;
            out.bindWrite();
            ShaderInstance shader = pass.shader().get();
            RenderSystem.setShaderTexture(0, sceneTexture);
            RenderSystem.setShaderTexture(1, this.depth.texture());
            this.commonUniforms(shader, frame, out.width(), out.height());
            pass.uniforms().accept(shader);
            fullscreen(shader);
            sceneTexture = out.texture();
            writeB = !writeB;
         }

         // 4. 体积通道：加法累积
         boolean hasVolume = !volumePasses.isEmpty();
         if (hasVolume) {
            float scale = this.volumeScale();
            this.volume.ensure(Math.max(1, (int)(width * scale)), Math.max(1, (int)(height * scale)));
            this.volume.clear();
            this.volume.bindWrite();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
            for (FxPass pass : volumePasses) {
               ShaderInstance shader = pass.shader().get();
               RenderSystem.setShaderTexture(0, this.depth.texture());
               RenderSystem.setShaderTexture(1, this.depth.texture());
               this.commonUniforms(shader, frame, this.volume.width(), this.volume.height());
               pass.uniforms().accept(shader);
               fullscreen(shader);
            }
            RenderSystem.disableBlend();
         }

         // 5. 泛光
         float bloomStrength = config.bloom.getValue() ? (float)(config.bloomStrength.getValue() * (1.0 + screen.bloomBoost)) : 0.0F;
         int bloomTexture = 0;
         if (bloomStrength > 0.001F && FxShaders.bloomDown != null && FxShaders.bloomUp != null) {
            bloomTexture = this.bloom(width, height, hasVolume);
         }

         // 6. 合成回主画面
         main.bindWrite(true);
         RenderSystem.disableBlend();
         ShaderInstance composite = FxShaders.composite;
         RenderSystem.setShaderTexture(0, sceneTexture);
         RenderSystem.setShaderTexture(1, this.fx.texture());
         RenderSystem.setShaderTexture(2, hasVolume ? this.volume.texture() : this.fx.texture());
         RenderSystem.setShaderTexture(3, bloomTexture != 0 ? bloomTexture : this.fx.texture());
         this.commonUniforms(composite, frame, width, height);
         float flash = (float)(screen.flash * config.screenFlash.getValue());
         composite.safeGetUniform("VolumeWeight").set(hasVolume ? 1.0F : 0.0F);
         composite.safeGetUniform("BloomStrength").set(bloomTexture != 0 ? bloomStrength : 0.0F);
         composite.safeGetUniform("Flash").set(screen.flashR, screen.flashG, screen.flashB, Math.min(1.0F, flash));
         composite.safeGetUniform("Aberration").set((float)(screen.aberration * config.chromaticAberration.getValue()));
         composite.safeGetUniform("ZoomBlur").set(screen.zoomBlur);
         composite.safeGetUniform("Center").set(screen.centerX, screen.centerY);
         composite.safeGetUniform("Vignette").set(screen.vignetteR, screen.vignetteG, screen.vignetteB, screen.vignette);
         composite.safeGetUniform("Desaturate").set(screen.desaturate);
         fullscreen(composite);
      } catch (Throwable throwable) {
         this.fail("frame", throwable);
      } finally {
         modelView.popPose();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.restoreProjectionMatrix();
         for (int i = 0; i < savedTextures.length; i++) {
            RenderSystem.setShaderTexture(i, savedTextures[i]);
         }
         guard.restore();
         main.bindWrite(true);
         this.depthCaptured = false;
      }
   }

   private int bloom(int width, int height, boolean hasVolume) {
      ShaderInstance down = FxShaders.bloomDown;
      ShaderInstance up = FxShaders.bloomUp;
      int levels = 0;
      int sourceWidth = width;
      int sourceHeight = height;
      int source = this.fx.texture();
      RenderSystem.disableBlend();
      for (int i = 0; i < BLOOM_LEVELS; i++) {
         int w = width >> (i + 1);
         int h = height >> (i + 1);
         if (w < 2 || h < 2) {
            break;
         }
         FxTarget target = this.bloomDown[i];
         target.ensure(w, h);
         target.bindWrite();
         RenderSystem.setShaderTexture(0, source);
         RenderSystem.setShaderTexture(1, i == 0 && hasVolume ? this.volume.texture() : source);
         down.safeGetUniform("TexelSize").set(1.0F / sourceWidth, 1.0F / sourceHeight);
         down.safeGetUniform("Prefilter").set(i == 0 ? 1.0F : 0.0F);
         down.safeGetUniform("VolumeWeight").set(i == 0 && hasVolume ? 1.0F : 0.0F);
         fullscreen(down);
         source = target.texture();
         sourceWidth = w;
         sourceHeight = h;
         levels++;
      }
      if (levels == 0) {
         return 0;
      }
      int lower = this.bloomDown[levels - 1].texture();
      int lowerWidth = this.bloomDown[levels - 1].width();
      int lowerHeight = this.bloomDown[levels - 1].height();
      for (int i = levels - 2; i >= 0; i--) {
         FxTarget target = this.bloomUp[i];
         target.ensure(this.bloomDown[i].width(), this.bloomDown[i].height());
         target.bindWrite();
         RenderSystem.setShaderTexture(0, lower);
         RenderSystem.setShaderTexture(1, this.bloomDown[i].texture());
         up.safeGetUniform("TexelSize").set(1.0F / lowerWidth, 1.0F / lowerHeight);
         up.safeGetUniform("Radius").set(1.0F);
         fullscreen(up);
         lower = target.texture();
         lowerWidth = target.width();
         lowerHeight = target.height();
      }
      return lower;
   }

   private void commonUniforms(ShaderInstance shader, FxFrame frame, int outWidth, int outHeight) {
      shader.safeGetUniform("InvProj").set(frame.inverseProjection);
      shader.safeGetUniform("InvView").set(frame.inverseView);
      shader.safeGetUniform("ViewProj").set(frame.viewProjection);
      shader.safeGetUniform("FxTime").set(frame.time);
      shader.safeGetUniform("OutSize").set((float)outWidth, (float)outHeight);
      shader.safeGetUniform("Steps").set((float)this.volumeSteps());
   }

   static void fullscreen(ShaderInstance shader) {
      RenderSystem.setShader(() -> shader);
      BufferBuilder builder = Tesselator.getInstance().getBuilder();
      builder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      builder.vertex(-1.0, -1.0, 0.0).endVertex();
      builder.vertex(1.0, -1.0, 0.0).endVertex();
      builder.vertex(1.0, 1.0, 0.0).endVertex();
      builder.vertex(-1.0, 1.0, 0.0).endVertex();
      BufferUploader.drawWithShader(builder.end());
   }

   private void fail(String stage, Throwable throwable) {
      this.failed = true;
      if (!this.loggedFailure) {
         this.loggedFailure = true;
         Annihilationblade.LOGGER.error("[AnnihilationBlade FX] Renderer disabled after an error during {}; falling back to particles until resources reload.", stage, throwable);
      }
   }

   /** 释放全部离屏目标（断开世界时调用，节省显存）。 */
   public void release() {
      this.depth.destroy();
      this.fx.destroy();
      this.volume.destroy();
      this.sceneA.destroy();
      this.sceneB.destroy();
      for (int i = 0; i < BLOOM_LEVELS; i++) {
         this.bloomDown[i].destroy();
         this.bloomUp[i].destroy();
      }
   }

   /**
    * 读取真实 GL 状态并在结束时通过 RenderSystem 恢复，保证原版状态缓存与真实状态一致。
    */
   private record StateGuard(boolean blend, boolean depthTest, boolean cull, boolean depthMask) {
      static StateGuard capture() {
         return new StateGuard(GL11.glIsEnabled(GL11.GL_BLEND), GL11.glIsEnabled(GL11.GL_DEPTH_TEST), GL11.glIsEnabled(GL11.GL_CULL_FACE), GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
      }

      void restore() {
         RenderSystem.defaultBlendFunc();
         if (this.blend) {
            RenderSystem.enableBlend();
         } else {
            RenderSystem.disableBlend();
         }
         if (this.depthTest) {
            RenderSystem.enableDepthTest();
         } else {
            RenderSystem.disableDepthTest();
         }
         if (this.cull) {
            RenderSystem.enableCull();
         } else {
            RenderSystem.disableCull();
         }
         RenderSystem.depthMask(this.depthMask);
      }
   }
}
