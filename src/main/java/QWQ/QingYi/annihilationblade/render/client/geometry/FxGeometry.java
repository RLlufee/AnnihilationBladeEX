package QWQ.QingYi.annihilationblade.render.client.geometry;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.pipeline.FxShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.List;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 网格特效基元构建器，所有基元写入 HDR 特效层，使用 {@code fx_world} 着色器按 {@link Style} 程序化着色。
 * <p>
 * 使用方式：{@code begin(style, emission, params...)} → 若干基元 → {@code end()}。同一批次共享一组 uniform。
 * 坐标一律为相对相机的世界坐标（见 {@link FxFrame#rel}）。
 */
@OnlyIn(Dist.CLIENT)
public final class FxGeometry {
   /** 片元着色风格，数值与 fx_world.fsh 中的分支一一对应。 */
   public enum Style {
      /** 径向辉光公告板，params: x=核心锐度, y=光晕衰减, z=横向星芒强度。 */
      GLOW(0),
      /** 能量条带（刀光/闪电/火花拖尾），params: x=中心线锐度, y=噪声频率, z=噪声速度, w=边缘辉光。 */
      RIBBON(1),
      /** 程序化法阵，params: x=旋转(弧度), y=随机种子, z=变体(0 处决六芒星 / 1 主法阵), w=展开进度。 */
      RUNE(2),
      /** 发光环带，params: x=环半径(0..1), y=环宽, z=虚线段数(0 为实线), w=虚线流动速度。 */
      RING(3),
      /**
       * 湍流能量光束（用 ribbon 绘制），白热核心 + 噪声扰动的烟壳 + 边缘光。
       * params: x=沿光束噪声频率, y=流动速度, z=湍流强度, w=边缘光强度。
       */
      BEAM(4),
      /**
       * 极光帷幕墙（用 quad 绘制，uv.x 沿墙，uv.y 竖直）：上涌的光带 + 扫描线 + 网格。
       * params: x=随机种子, y=网格/扫描线强度, z=升起进度(0..1), w=崩解进度(0..1)。
       */
      CURTAIN(5),
      /**
       * 吸积盘（用 quad 绘制）：差速旋转的噪声光带、内缘白热、多普勒增亮。
       * params: x=内径(0..1), y=旋转速度, z=随机种子, w=多普勒强度。
       */
      DISK(6);

      final float id;

      Style(int id) {
         this.id = id;
      }
   }

   private final FxFrame frame;
   private BufferBuilder builder;
   private Style style;
   private float emission;
   private float softness;
   private final float[] params = new float[4];
   private final float[] occluder = new float[4];
   private int drawCalls;

   public FxGeometry(FxFrame frame) {
      this.frame = frame;
   }

   public FxFrame frame() {
      return this.frame;
   }

   public int drawCalls() {
      return this.drawCalls;
   }

   /**
    * 设置遮挡球（例如黑洞事件视界）：之后的批次中位于球体背后的片元会被平滑剔除。
    * 对后续所有批次生效，直到 {@link #clearOccluder()}；每个特效实例绘制结束后管线会自动清除。
    */
   public void occluder(Vector3f center, float radius) {
      this.end();
      this.occluder[0] = center.x;
      this.occluder[1] = center.y;
      this.occluder[2] = center.z;
      this.occluder[3] = Math.max(0.0F, radius);
   }

   public void clearOccluder() {
      this.end();
      this.occluder[3] = 0.0F;
   }

   /**
    * @param emission HDR 发光倍率（1 = 普通亮度，>1 会产生明显泛光）
    * @param softness 软深度距离（格）：与场景几何相交处的淡出宽度，0 表示硬裁剪
    */
   public FxGeometry begin(Style style, float emission, float softness, float p0, float p1, float p2, float p3) {
      if (this.builder != null) {
         this.end();
      }
      this.style = style;
      this.emission = emission;
      this.softness = softness;
      this.params[0] = p0;
      this.params[1] = p1;
      this.params[2] = p2;
      this.params[3] = p3;
      this.builder = Tesselator.getInstance().getBuilder();
      this.builder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
      return this;
   }

   public void end() {
      if (this.builder == null) {
         return;
      }
      BufferBuilder current = this.builder;
      this.builder = null;
      BufferBuilder.RenderedBuffer rendered = current.end();
      ShaderInstance shader = FxShaders.world;
      if (shader == null) {
         rendered.release();
         return;
      }
      shader.safeGetUniform("FxStyle").set(this.style.id);
      shader.safeGetUniform("FxEmission").set(this.emission);
      shader.safeGetUniform("FxSoft").set(this.softness);
      shader.safeGetUniform("FxParams").set(this.params[0], this.params[1], this.params[2], this.params[3]);
      shader.safeGetUniform("FxTime").set(this.frame.time);
      shader.safeGetUniform("FxOccluder").set(this.occluder[0], this.occluder[1], this.occluder[2], this.occluder[3]);
      RenderSystem.setShader(() -> shader);
      BufferUploader.drawWithShader(rendered);
      this.drawCalls++;
   }

   private void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v) {
      this.builder.vertex(x, y, z).color(r, g, b, a).uv(u, v).endVertex();
   }

   /** 面向相机的公告板。 */
   public void billboard(Vector3f center, float halfSize, float rotation, float r, float g, float b, float a) {
      if (this.builder == null || halfSize <= 0.0F || a <= 0.0F) {
         return;
      }
      float cos = (float)Math.cos(rotation);
      float sin = (float)Math.sin(rotation);
      Vector3f left = new Vector3f(this.frame.left).mul(halfSize);
      Vector3f up = new Vector3f(this.frame.up).mul(halfSize);
      Vector3f axisU = new Vector3f(left).mul(cos).add(new Vector3f(up).mul(sin));
      Vector3f axisV = new Vector3f(up).mul(cos).sub(new Vector3f(left).mul(sin));
      this.quad(center, axisU, axisV, r, g, b, a);
   }

   /** 任意朝向的平面方片，uv 范围 [-1,1]，常用于法阵与环带。axisU/axisV 为半边长向量。 */
   public void quad(Vector3f center, Vector3f axisU, Vector3f axisV, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F) {
         return;
      }
      this.vertex(center.x - axisU.x - axisV.x, center.y - axisU.y - axisV.y, center.z - axisU.z - axisV.z, r, g, b, a, -1.0F, -1.0F);
      this.vertex(center.x + axisU.x - axisV.x, center.y + axisU.y - axisV.y, center.z + axisU.z - axisV.z, r, g, b, a, 1.0F, -1.0F);
      this.vertex(center.x + axisU.x + axisV.x, center.y + axisU.y + axisV.y, center.z + axisU.z + axisV.z, r, g, b, a, 1.0F, 1.0F);
      this.vertex(center.x - axisU.x + axisV.x, center.y - axisU.y + axisV.y, center.z - axisU.z + axisV.z, r, g, b, a, -1.0F, 1.0F);
   }

   /**
    * 面向相机的折线条带。uv.x 沿条带 0..1，uv.y 横跨 -1..1。
    *
    * @param widths 每个点的半宽（格）
    * @param alphas 每个点的透明度/强度
    */
   public void ribbon(List<Vector3f> points, float[] widths, float[] alphas, float r, float g, float b) {
      int count = points.size();
      if (this.builder == null || count < 2) {
         return;
      }
      Vector3f[] sides = new Vector3f[count];
      for (int i = 0; i < count; i++) {
         Vector3f prev = points.get(Math.max(0, i - 1));
         Vector3f next = points.get(Math.min(count - 1, i + 1));
         Vector3f tangent = new Vector3f(next).sub(prev);
         Vector3f toCamera = new Vector3f(points.get(i)).negate();
         Vector3f side = tangent.cross(toCamera);
         if (side.lengthSquared() < 1.0E-10F) {
            side.set(this.frame.up);
         }
         sides[i] = side.normalize().mul(widths[i]);
      }
      for (int i = 0; i < count - 1; i++) {
         Vector3f a = points.get(i);
         Vector3f c = points.get(i + 1);
         float u0 = (float)i / (count - 1);
         float u1 = (float)(i + 1) / (count - 1);
         Vector3f sa = sides[i];
         Vector3f sc = sides[i + 1];
         float aa = alphas[i];
         float ac = alphas[i + 1];
         if (aa <= 0.0F && ac <= 0.0F) {
            continue;
         }
         this.vertex(a.x - sa.x, a.y - sa.y, a.z - sa.z, r, g, b, aa, u0, -1.0F);
         this.vertex(c.x - sc.x, c.y - sc.y, c.z - sc.z, r, g, b, ac, u1, -1.0F);
         this.vertex(c.x + sc.x, c.y + sc.y, c.z + sc.z, r, g, b, ac, u1, 1.0F);
         this.vertex(a.x + sa.x, a.y + sa.y, a.z + sa.z, r, g, b, aa, u0, 1.0F);
      }
   }

   /** 两点间的直线拖尾（火花、流星），头部宽、尾部尖。 */
   public void streak(Vector3f head, Vector3f tail, float halfWidth, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F) {
         return;
      }
      Vector3f tangent = new Vector3f(head).sub(tail);
      Vector3f side = tangent.cross(new Vector3f(head).negate());
      if (side.lengthSquared() < 1.0E-10F) {
         return;
      }
      side.normalize().mul(halfWidth);
      this.vertex(tail.x, tail.y, tail.z, r, g, b, 0.0F, 0.0F, -1.0F);
      this.vertex(head.x - side.x, head.y - side.y, head.z - side.z, r, g, b, a, 1.0F, -1.0F);
      this.vertex(head.x + side.x, head.y + side.y, head.z + side.z, r, g, b, a, 1.0F, 1.0F);
      this.vertex(tail.x, tail.y, tail.z, r, g, b, 0.0F, 0.0F, 1.0F);
   }
}
