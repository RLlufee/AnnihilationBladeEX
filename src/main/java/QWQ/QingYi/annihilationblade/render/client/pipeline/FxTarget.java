package QWQ.QingYi.annihilationblade.render.client.pipeline;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 不带深度附件的离屏颜色目标，可选 HDR（RGBA16F）或单通道浮点（R32F）格式。
 * <p>
 * 基于原版 {@link TextureTarget} 创建（保证帧缓冲句柄、清屏、绑定都走原版逻辑），
 * 创建后再把颜色纹理重新分配为浮点格式；帧缓冲附件引用的是纹理名，因此无需重新挂接。
 */
@OnlyIn(Dist.CLIENT)
final class FxTarget {
   enum Format {
      RGBA8(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE),
      RGBA16F(GL30.GL_RGBA16F, GL11.GL_RGBA, GL11.GL_FLOAT),
      R32F(GL30.GL_R32F, GL11.GL_RED, GL11.GL_FLOAT);

      final int internal;
      final int format;
      final int type;

      Format(int internal, int format, int type) {
         this.internal = internal;
         this.format = format;
         this.type = type;
      }
   }

   private final Format format;
   private TextureTarget target;

   FxTarget(Format format) {
      this.format = format;
   }

   /** 确保尺寸匹配；返回 true 表示发生了重建。 */
   boolean ensure(int width, int height) {
      width = Math.max(1, width);
      height = Math.max(1, height);
      if (this.target != null && this.target.width == width && this.target.height == height) {
         return false;
      }
      if (this.target == null) {
         this.target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
      } else {
         this.target.resize(width, height, Minecraft.ON_OSX);
      }
      this.reformat();
      this.target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.target.clear(Minecraft.ON_OSX);
      return true;
   }

   private void reformat() {
      int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      GlStateManager._bindTexture(this.target.getColorTextureId());
      GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, this.format.internal, this.target.width, this.target.height, 0, this.format.format, this.format.type, null);
      GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, this.format == Format.R32F ? GL11.GL_NEAREST : GL11.GL_LINEAR);
      GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, this.format == Format.R32F ? GL11.GL_NEAREST : GL11.GL_LINEAR);
      GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
      GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
      GlStateManager._bindTexture(previous);
   }

   void bindWrite() {
      this.target.bindWrite(true);
   }

   void clear() {
      this.target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.target.clear(Minecraft.ON_OSX);
   }

   int texture() {
      return this.target == null ? 0 : this.target.getColorTextureId();
   }

   int framebuffer() {
      return this.target == null ? 0 : this.target.frameBufferId;
   }

   int width() {
      return this.target == null ? 1 : this.target.width;
   }

   int height() {
      return this.target == null ? 1 : this.target.height;
   }

   boolean ready() {
      return this.target != null;
   }

   void destroy() {
      if (this.target != null) {
         RenderSystem.assertOnRenderThread();
         this.target.destroyBuffers();
         this.target = null;
      }
   }
}
