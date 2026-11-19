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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/**
 * 专属渲染管线。每帧（仅在有可见特效时）执行：
 * <ol>
 *   <li>深度捕获：在 Fabulous 透明度后处理覆盖主深度之前，把场景深度复制到 R32F 纹理（AFTER_WEATHER）。</li>
 *   <li>预乘 Alpha 本体/暗部层（按需）：绘制暗黑空间切口、深色烟壳、半透明能量膜，不直接进入全屏泛光。</li>
 *   <li>网格 HDR 特效层：法阵、3D 刀光弧、3D 嵌套光柱、大气再入火壳、闪电等写入 RGBA16F 层。</li>
 *   <li>场景通道（B 档）：复制主画面后串行执行空间碎裂折射 / 黑洞透镜 / 冲击波扭曲。</li>
 *   <li>体积通道（B 档）：IGN 抖动光线步进的体积裂隙与雾效，低分辨率累积。</li>
 *   <li>泛光：仅对 HDR 特效层 + 体积层做带 Soft-Knee 阈值与 Karis Average 的 6 级降/升采样。</li>
 *   <li>合成：场景 + 本体暗部层 + 线性空间 HDR 发光与泛光，同步施加径向/速度线模糊、色差、过曝爆闪、冲击帧与 Bayer8 消色带。</li>
 * </ol>
 * 整个流程在 AFTER_LEVEL 执行，天然兼容光影包且不影响第一人称手持物。
 */
@OnlyIn(Dist.CLIENT)
public final class FxPipeline {
   public static final FxPipeline INSTANCE = new FxPipeline();
   private static final int BLOOM_LEVELS = 6;
   private static final Matrix4f IDENTITY = new Matrix4f();
   private static final ResourceLocation FLOW_NOISE = ResourceLocation.fromNamespaceAndPath(Annihilationblade.MODID, "textures/fx/fx_flow_noise.png");

   private final FxTarget depth = new FxTarget(FxTarget.Format.R32F);
   private final FxTarget body = new FxTarget(FxTarget.Format.RGBA16F);
   private final FxTarget fx = new FxTarget(FxTarget.Format.RGBA16F);
   private final FxTarget volume = new FxTarget(FxTarget.Format.RGBA16F);
   private final FxTarget sceneA = new FxTarget(FxTarget.Format.RGBA8);
   private final FxTarget sceneB = new FxTarget(FxTarget.Format.RGBA8);
   private final FxTarget[] bloomDown = new FxTarget[BLOOM_LEVELS];
   private final FxTarget[] bloomUp = new FxTarget[BLOOM_LEVELS];

   private VertexBuffer fullscreenVbo;
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
    * 复制当前主深度。会完整恢复调用前的帧缓冲绑定、混合因子与视口（Fabulous 下此时绑定的是天气目标）。
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
         this.fullscreen(shader);
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
      int[] savedTextures = new int[6];
      for (int i = 0; i < savedTextures.length; i++) {
         savedTextures[i] = RenderSystem.getShaderTexture(i);
      }
      RenderSystem.backupProjectionMatrix();
      PoseStack modelView = RenderSystem.getModelViewStack();
      modelView.pushPose();
      try {
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.setShaderTexture(0, this.depth.texture());
         RenderSystem.setShaderTexture(1, FLOW_NOISE);
         RenderSystem.setProjectionMatrix(frame.projection, VertexSorting.DISTANCE_TO_ORIGIN);
         modelView.setIdentity();
         modelView.mulPoseMatrix(frame.view);
         RenderSystem.applyModelViewMatrix();

         // 0. 预乘 Alpha 本体/暗部层（按需启用：暗芯刀弧、深色烟壳、半透明护盾膜）
         boolean anyBodyRequested = false;
         for (FxInstance instance : instances) {
            if (instance.hasBody()) {
               anyBodyRequested = true;
               break;
            }
         }
         boolean hasBody = false;
         if (anyBodyRequested) {
            this.body.ensure(width, height);
            this.body.clear();
            this.body.bindWrite();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
            FxGeometry bodyGeometry = new FxGeometry(frame, true);
            for (FxInstance instance : instances) {
               if (instance.hasBody()) {
                  instance.renderBody(bodyGeometry, frame);
                  bodyGeometry.clearOccluder();
                  bodyGeometry.clearGeoCenter();
               }
            }
            hasBody = bodyGeometry.drawCalls() > 0;
         }

         // 1. 网格 HDR 特效层
         this.fx.ensure(width, height);
         this.fx.clear();
         this.fx.bindWrite();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
         FxGeometry geometry = new FxGeometry(frame, false);
         for (FxInstance instance : instances) {
            instance.renderGeometry(geometry, frame);
            geometry.clearOccluder();
            geometry.clearGeoCenter();
         }
         boolean hasGeometry = geometry.drawCalls() > 0;
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

         // 若所有层与屏幕效果都为空，直接退出，避免无谓的全屏 Blit 与合成
         if (!hasBody && !hasGeometry && scenePasses.isEmpty() && volumePasses.isEmpty() && screen.idle()) {
            return;
         }

         // 3. 场景通道：复制主画面后串行处理
         this.sceneA.ensure(width, height);
         GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
         GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.sceneA.framebuffer());
         GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
         int sceneTexture = this.sceneA.texture();
         if (!scenePasses.isEmpty()) {
            this.sceneB.ensure(width, height);
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
               this.fullscreen(shader);
               sceneTexture = out.texture();
               writeB = !writeB;
            }
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
               this.fullscreen(shader);
            }
            RenderSystem.disableBlend();
         }

         // 5. 泛光（仅当本帧存在 HDR 网格或体积发光时才执行，空层跳过整条泛光链）
         float bloomStrength = config.bloom.getValue() ? (float)(config.bloomStrength.getValue() * (1.0 + screen.bloomBoost)) : 0.0F;
         int bloomTexture = 0;
         if ((hasGeometry || hasVolume) && bloomStrength > 0.001F && FxShaders.bloomDown != null && FxShaders.bloomUp != null) {
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
         RenderSystem.setShaderTexture(4, hasBody ? this.body.texture() : this.fx.texture());
         this.commonUniforms(composite, frame, width, height);
         float flash = (float)(screen.flash * config.screenFlash.getValue());
         composite.safeGetUniform("BodyWeight").set(hasBody ? 1.0F : 0.0F);
         composite.safeGetUniform("VolumeWeight").set(hasVolume ? 1.0F : 0.0F);
         composite.safeGetUniform("BloomStrength").set(bloomTexture != 0 ? bloomStrength : 0.0F);
         composite.safeGetUniform("Flash").set(screen.flashR, screen.flashG, screen.flashB, Math.min(1.0F, flash));
         composite.safeGetUniform("FlashExtra").set(screen.flashBoost, 0.0F);
         composite.safeGetUniform("Aberration").set((float)(screen.aberration * config.chromaticAberration.getValue()));
         composite.safeGetUniform("ZoomBlur").set(screen.zoomBlur);
         composite.safeGetUniform("DashBlur").set(screen.dashBlur);
         composite.safeGetUniform("Center").set(screen.centerX, screen.centerY);
         composite.safeGetUniform("Vignette").set(screen.vignetteR, screen.vignetteG, screen.vignetteB, screen.vignette);
         composite.safeGetUniform("Desaturate").set(screen.desaturate);
         // 冲击帧属于强闪烁画面，受「闪白强度」配置约束（设为 0 即关闭）
         float impact = (float)(screen.impactFrame * config.screenFlash.getValue());
         composite.safeGetUniform("ImpactFrame").set(Math.min(1.0F, impact), screen.impactInvert ? 1.0F : 0.0F);
         this.fullscreen(composite);
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
         this.fullscreen(down);
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
         this.fullscreen(up);
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

   private void ensureFullscreenVbo() {
      if (this.fullscreenVbo != null) {
         return;
      }
      VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
      BufferBuilder builder = Tesselator.getInstance().getBuilder();
      builder.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION);
      // 单个全屏大三角形覆盖 [-1,1]x[-1,1]，零对角线辅助像素浪费且避免每 Pass 重新上传 VBO
      builder.vertex(-1.0, -1.0, 0.0).endVertex();
      builder.vertex(3.0, -1.0, 0.0).endVertex();
      builder.vertex(-1.0, 3.0, 0.0).endVertex();
      vbo.bind();
      vbo.upload(builder.end());
      VertexBuffer.unbind();
      this.fullscreenVbo = vbo;
   }

   private void fullscreen(ShaderInstance shader) {
      this.ensureFullscreenVbo();
      this.fullscreenVbo.bind();
      this.fullscreenVbo.drawWithShader(IDENTITY, IDENTITY, shader);
      VertexBuffer.unbind();
   }

   private void fail(String stage, Throwable throwable) {
      this.failed = true;
      if (!this.loggedFailure) {
         this.loggedFailure = true;
         Annihilationblade.LOGGER.error("[AnnihilationBlade FX] Renderer disabled after an error during {}; falling back to particles until resources reload.", stage, throwable);
      }
   }

   /** 释放全部离屏目标与静态全屏 VBO（断开世界时调用，节省显存）。 */
   public void release() {
      this.depth.destroy();
      this.body.destroy();
      this.fx.destroy();
      this.volume.destroy();
      this.sceneA.destroy();
      this.sceneB.destroy();
      for (int i = 0; i < BLOOM_LEVELS; i++) {
         this.bloomDown[i].destroy();
         this.bloomUp[i].destroy();
      }
      if (this.fullscreenVbo != null) {
         RenderSystem.assertOnRenderThread();
         this.fullscreenVbo.close();
         this.fullscreenVbo = null;
      }
   }

   /**
    * 读取真实 GL 状态（含分离混合因子）并在结束时通过 RenderSystem 恢复，保证原版状态缓存与真实状态一致。
    */
   private record StateGuard(boolean blend, boolean depthTest, boolean cull, boolean depthMask, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
      static StateGuard capture() {
         return new StateGuard(
            GL11.glIsEnabled(GL11.GL_BLEND),
            GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
            GL11.glIsEnabled(GL11.GL_CULL_FACE),
            GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
            GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
            GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
            GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
            GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
         );
      }

      void restore() {
         RenderSystem.blendFuncSeparate(this.srcRgb, this.dstRgb, this.srcAlpha, this.dstAlpha);
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
