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
 * 网格特效基元构建器，所有基元使用 {@code fx_world} 着色器按 {@link Style} 程序化/流场贴图着色。
 * <p>
 * 支持双渲染层：
 * <ul>
 *   <li>HDR 发光层（{@code bodyPass = false}，默认）：加法混合，进入泛光金字塔。</li>
 *   <li>预乘 Alpha 本体/暗部层（{@code bodyPass = true}）：可绘制暗黑空间切口、深色翻滚烟壳、半透明能量膜。</li>
 * </ul>
 * 用法：{@code begin(style, emission, softness, p0, p1, p2, p3)} → 若干基元 → {@code end()}。
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
      /** 程序化法阵，params: x=旋转(弧度), y=随机种子, z=变体(0 六芒星 / 1 主法阵 / 2 爱心 / 3 方形敕印), w=展开进度（敕印为篆文浮现进度）。 */
      RUNE(2),
      /** 发光环带，params: x=环半径(0..1), y=环宽, z=虚线段数(0 为实线), w=虚线流动速度。 */
      RING(3),
      /** 湍流能量光束（ribbon），params: x=沿线噪声频率, y=流动速度, z=湍流强度, w=边缘光。 */
      BEAM(4),
      /** 极光帷幕墙（quad），params: x=种子, y=网格/扫描线强度, z=升起进度, w=崩解进度。 */
      CURTAIN(5),
      /** 吸积盘（quad），params: x=内径(0..1), y=旋转速度, z=种子, w=多普勒强度。 */
      DISK(6),
      /** 龙鳞能量屏障（sphere + geoCenter 解析法线），params: x=鳞片/丝线行数, y=龟裂比例, z=闪烁相位, w=边缘光强度。 */
      SHIELD(7),
      /** 精灵图形公告板，params: x=形状(0 爱心 / 1 四芒闪光 / 2 五角星)。 */
      SPRITE(8),
      /** 3D 圆柱/锥台等离子光柱（cylinder），params: x=纹理缩放, y=流动速度, z=坍缩进度, w=冲击波前位置(0..1)。 */
      PLASMA_BEAM(9),
      /** 3D 大气再入燃烧锥壳/沸腾火球（cylinder 锥台或 sphere），params: x=前端聚焦锐度, y=噪声密度, z=流动速度, w=激波环脉动。 */
      REENTRY(10),
      /** 3D 定向曲面刀光弧（slashArc），params: x=锋刃锐度, y=流场噪声强度, z=推进进度(0..1), w=尾部裁剪(0..1)。 */
      SLASH_ARC(11),
      /** 3D 喇叭形多层冲击波锥（cylinder 锥台），params: x=扩散进度(0..1), y=波速, z=边缘撕裂强度, w=喉部核心亮核。 */
      SHOCK_CONE(12),
      /** 360° 三平面无缝沸腾日冕/日蚀球壳（配合 deformedSphere + geoCenter），params: x=边缘日冕锐度, y=湍流对比度, z=流动速度, w=脉动强度。 */
      CORONA(13),
      /** 全息残影（rawQuad 逐面绘制模型），params: x=棱边线宽倍率, y=面内填充, z=扫描线密度, w=溶解进度(0..1)。 */
      AFTERIMAGE(14),
      /** 虚空裂口（quad，uv.x 横跨 / uv.y 沿裂口），params: x=张开度, y=边缘撕裂强度, z=变体(0 眼形伤口 / 1 直刃裂缝), w=内部细节。 */
      VOID_SLIT(15);

      final float id;

      Style(int id) {
         this.id = id;
      }
   }

   private final FxFrame frame;
   private final boolean bodyPass;
   private BufferBuilder builder;
   private Style style;
   private float emission;
   private float softness;
   private final float[] params = new float[4];
   private final float[] occluder = new float[4];
   private final float[] geoCenter = new float[4];
   private int batchVertices;
   private int drawCalls;

   // 复用临时向量以降低高峰帧的短命对象分配与 Minor GC
   private final Vector3f tmpA = new Vector3f();
   private final Vector3f tmpB = new Vector3f();
   private final Vector3f tmpC = new Vector3f();
   private final Vector3f tmpD = new Vector3f();
   private final Vector3f tmpE = new Vector3f();

   public FxGeometry(FxFrame frame) {
      this(frame, false);
   }

   public FxGeometry(FxFrame frame, boolean bodyPass) {
      this.frame = frame;
      this.bodyPass = bodyPass;
   }

   public FxFrame frame() {
      return this.frame;
   }

   public boolean isBodyPass() {
      return this.bodyPass;
   }

   public int drawCalls() {
      return this.drawCalls;
   }

   /**
    * 设置遮挡球（例如黑洞事件视界）：之后的批次中位于球体背后的片元会被平滑剔除。
    * 每个特效实例绘制结束后管线会自动清除。
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
    * 指定球面/几何解析中心，启用平滑球面解析法线（消除球体的 Flat Shading 多边形棱面）。
    */
   public void geoCenter(Vector3f center) {
      this.end();
      this.geoCenter[0] = center.x;
      this.geoCenter[1] = center.y;
      this.geoCenter[2] = center.z;
      this.geoCenter[3] = 1.0F;
   }

   public void clearGeoCenter() {
      this.end();
      this.geoCenter[3] = 0.0F;
   }

   /**
    * @param emission HDR 发光倍率（Body 层则为本体亮度缩放，暗黑切口可设为 0.0..1.0）
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
      this.batchVertices = 0;
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
      if (this.batchVertices == 0 || shader == null) {
         rendered.release();
         return;
      }
      shader.safeGetUniform("FxStyle").set(this.style.id);
      shader.safeGetUniform("FxEmission").set(this.emission);
      shader.safeGetUniform("FxSoft").set(this.softness);
      shader.safeGetUniform("FxParams").set(this.params[0], this.params[1], this.params[2], this.params[3]);
      shader.safeGetUniform("FxTime").set(this.frame.time);
      shader.safeGetUniform("FxOccluder").set(this.occluder[0], this.occluder[1], this.occluder[2], this.occluder[3]);
      shader.safeGetUniform("FxGeoCenter").set(this.geoCenter[0], this.geoCenter[1], this.geoCenter[2], this.geoCenter[3]);
      shader.safeGetUniform("FxBlendMode").set(this.bodyPass ? 1.0F : 0.0F);
      RenderSystem.setShader(() -> shader);
      BufferUploader.drawWithShader(rendered);
      this.drawCalls++;
   }

   private void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v) {
      this.builder.vertex(x, y, z).color(r, g, b, a).uv(u, v).endVertex();
      this.batchVertices++;
   }

   /** 面向相机的公告板。 */
   public void billboard(Vector3f center, float halfSize, float rotation, float r, float g, float b, float a) {
      if (this.builder == null || halfSize <= 0.0F || a <= 0.0F) {
         return;
      }
      float cos = (float)Math.cos(rotation);
      float sin = (float)Math.sin(rotation);
      Vector3f left = this.tmpA.set(this.frame.left).mul(halfSize);
      Vector3f up = this.tmpB.set(this.frame.up).mul(halfSize);
      Vector3f axisU = this.tmpC.set(left).mul(cos).fma(sin, up);
      Vector3f axisV = this.tmpD.set(up).mul(cos).fma(-sin, left);
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
      Vector3f prevSide = null;
      for (int i = 0; i < count; i++) {
         Vector3f prev = points.get(Math.max(0, i - 1));
         Vector3f next = points.get(Math.min(count - 1, i + 1));
         Vector3f tangent = this.tmpA.set(next).sub(prev);
         Vector3f toCamera = this.tmpB.set(points.get(i)).negate();
         Vector3f side = new Vector3f(tangent).cross(toCamera);
         if (side.lengthSquared() < 1.0E-8F) {
            if (prevSide != null && prevSide.lengthSquared() > 1.0E-8F) {
               side.set(prevSide);
            } else {
               side.set(this.frame.up);
            }
         } else {
            side.normalize();
            if (prevSide != null && side.dot(prevSide) < 0.0F) {
               side.negate();
            }
         }
         prevSide = side;
         sides[i] = new Vector3f(side).mul(widths[i]);
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

   /**
    * 经纬球面网格，uv.x = 经度 0..1，uv.y = 纬度 0..1。
    */
   public void sphere(Vector3f center, float radius, int rings, int segments, float spin, float r, float g, float b, float a) {
      this.deformedSphere(center, radius, rings, segments, spin, 0.0F, 0.0F, 0.0F, r, g, b, a);
   }

   /**
    * 带三维波浪顶点位移的立体球面网格（用于沸腾恒星核心、呼吸波浪龙鳞护盾、相变真空泡）。
    * 建议在调用前通过 {@link #geoCenter} 传入球心以启用平滑解析法线。
    */
   public void deformedSphere(Vector3f center, float radius, int rings, int segments, float spin, float waveAmp, float waveFreq, float wavePhase, float r, float g, float b, float a) {
      if (this.builder == null || radius <= 0.0F || a <= 0.0F) {
         return;
      }
      rings = Math.max(4, rings);
      segments = Math.max(6, segments);
      for (int i = 0; i < rings; i++) {
         float v0 = (float)i / rings;
         float v1 = (float)(i + 1) / rings;
         float p0 = (float)Math.PI * v0;
         float p1 = (float)Math.PI * v1;
         for (int j = 0; j < segments; j++) {
            float u0 = (float)j / segments;
            float u1 = (float)(j + 1) / segments;
            float t0 = FxMath.TAU * u0 + spin;
            float t1 = FxMath.TAU * u1 + spin;
            this.deformedSphereVertex(center, radius, p0, t0, waveAmp, waveFreq, wavePhase, r, g, b, a, u0, v0);
            this.deformedSphereVertex(center, radius, p0, t1, waveAmp, waveFreq, wavePhase, r, g, b, a, u1, v0);
            this.deformedSphereVertex(center, radius, p1, t1, waveAmp, waveFreq, wavePhase, r, g, b, a, u1, v1);
            this.deformedSphereVertex(center, radius, p1, t0, waveAmp, waveFreq, wavePhase, r, g, b, a, u0, v1);
         }
      }
   }

   private void deformedSphereVertex(Vector3f center, float radius, float polar, float azimuth, float waveAmp, float waveFreq, float wavePhase, float r, float g, float b, float a, float u, float v) {
      float sinPolar = (float)Math.sin(polar);
      float dx = sinPolar * (float)Math.cos(azimuth);
      float dy = (float)Math.cos(polar);
      float dz = sinPolar * (float)Math.sin(azimuth);
      float scale = radius;
      if (waveAmp > 1.0E-5F) {
         float w1 = (float)Math.sin(dx * waveFreq * 4.5F + dy * waveFreq * 2.6F + wavePhase);
         float w2 = (float)Math.sin(dz * waveFreq * 5.6F - dx * waveFreq * 3.2F - wavePhase * 0.82F);
         float w3 = (float)Math.sin((dx - dy + dz) * waveFreq * 8.4F + wavePhase * 1.35F);
         scale = radius * (1.0F + waveAmp * (w1 * 0.45F + w2 * 0.35F + w3 * 0.20F));
      }
      this.vertex(center.x + dx * scale, center.y + dy * scale, center.z + dz * scale, r, g, b, a, u, v);
   }

   /**
    * 3D 任意朝向圆柱 / 圆锥 / 喇叭锥台侧面网格（无端盖，适合多层嵌套光柱、大气再入燃烧锥壳、3D 冲击波锥）。
    * <p>
    * UV 约定：{@code uv.x} 沿圆周 0..1，{@code uv.y = 0} 位于 {@code base}，{@code uv.y = 1} 位于 {@code top}。
    */
   public void cylinder(Vector3f base, Vector3f top, float baseRadius, float topRadius, int segments, float spin, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F || (baseRadius <= 0.0F && topRadius <= 0.0F)) {
         return;
      }
      Vector3f axis = this.tmpA.set(top).sub(base);
      if (axis.lengthSquared() < 1.0E-6F) {
         return;
      }
      axis.normalize();
      Vector3f right = this.tmpB.set(FxMath.perpendicular(axis)).normalize();
      this.orientedCylinderInternal(base, top, axis, right, baseRadius, baseRadius, topRadius, topRadius, segments, spin, r, g, b, a);
   }

   /**
    * 3D 扁平棱面/椭圆截面柱体（沿指定横向基准 {@code rightDir} 展开，适合宽刃巨剑剑身与十字护手）。
    * 当 {@code segments = 4} 且 {@code rx > rz} 时，会生成锋利的四棱菱形剑刃截面。
    */
   public void orientedCylinder(Vector3f base, Vector3f top, Vector3f rightDir, float baseRx, float baseRz, float topRx, float topRz, int segments, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F || (baseRx <= 0.0F && topRx <= 0.0F)) {
         return;
      }
      Vector3f axis = this.tmpA.set(top).sub(base);
      if (axis.lengthSquared() < 1.0E-6F) {
         return;
      }
      axis.normalize();
      // 正交化 rightDir 确保截面与轴向严格垂直
      Vector3f right = this.tmpB.set(rightDir).fma(-rightDir.dot(axis), axis);
      if (right.lengthSquared() < 1.0E-6F) {
         right.set(FxMath.perpendicular(axis));
      }
      right.normalize();
      this.orientedCylinderInternal(base, top, axis, right, baseRx, baseRz, topRx, topRz, segments, 0.0F, r, g, b, a);
   }

   private void orientedCylinderInternal(Vector3f base, Vector3f top, Vector3f axis, Vector3f right, float baseRx, float baseRz, float topRx, float topRz, int segments, float spin, float r, float g, float b, float a) {
      Vector3f forward = this.tmpC.set(axis).cross(right).normalize();
      segments = Math.max(4, segments);
      for (int i = 0; i < segments; i++) {
         float u0 = (float)i / segments;
         float u1 = (float)(i + 1) / segments;
         float a0 = FxMath.TAU * u0 + spin;
         float a1 = FxMath.TAU * u1 + spin;
         float c0 = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         float c1 = (float)Math.cos(a1);
         float s1 = (float)Math.sin(a1);
         float b0x = right.x * c0 * baseRx + forward.x * s0 * baseRz;
         float b0y = right.y * c0 * baseRx + forward.y * s0 * baseRz;
         float b0z = right.z * c0 * baseRx + forward.z * s0 * baseRz;
         float b1x = right.x * c1 * baseRx + forward.x * s1 * baseRz;
         float b1y = right.y * c1 * baseRx + forward.y * s1 * baseRz;
         float b1z = right.z * c1 * baseRx + forward.z * s1 * baseRz;
         float t0x = right.x * c0 * topRx + forward.x * s0 * topRz;
         float t0y = right.y * c0 * topRx + forward.y * s0 * topRz;
         float t0z = right.z * c0 * topRx + forward.z * s0 * topRz;
         float t1x = right.x * c1 * topRx + forward.x * s1 * topRz;
         float t1y = right.y * c1 * topRx + forward.y * s1 * topRz;
         float t1z = right.z * c1 * topRx + forward.z * s1 * topRz;
         this.vertex(base.x + b0x, base.y + b0y, base.z + b0z, r, g, b, a, u0, 0.0F);
         this.vertex(base.x + b1x, base.y + b1y, base.z + b1z, r, g, b, a, u1, 0.0F);
         this.vertex(top.x + t1x, top.y + t1y, top.z + t1z, r, g, b, a, u1, 1.0F);
         this.vertex(top.x + t0x, top.y + t0y, top.z + t0z, r, g, b, a, u0, 1.0F);
      }
   }

   /**
    * 3D 定向曲面刀光弧带（在 {@code axisU / axisV} 张成的真实 3D 挥砍平面内展开，不再强行朝向相机）。
    * <p>
    * UV 约定：{@code uv.x} 沿弧线从 {@code startAngle}(0) 到 {@code endAngle}(1)，{@code uv.y} 从内缘(0)到外刃(1)。
    */
   public void slashArc(Vector3f center, Vector3f axisU, Vector3f axisV, float innerRadius, float outerRadius, float startAngle, float endAngle, int segments, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F || outerRadius <= innerRadius) {
         return;
      }
      segments = Math.max(6, segments);
      for (int i = 0; i < segments; i++) {
         float u0 = (float)i / segments;
         float u1 = (float)(i + 1) / segments;
         float ang0 = FxMath.lerp(startAngle, endAngle, u0);
         float ang1 = FxMath.lerp(startAngle, endAngle, u1);
         float c0 = (float)Math.cos(ang0);
         float s0 = (float)Math.sin(ang0);
         float c1 = (float)Math.cos(ang1);
         float s1 = (float)Math.sin(ang1);
         // 弧尾与弧尖自然收窄，拉出凌厉的月牙斩击剪影
         float taper0 = (float)Math.sin(Math.PI * Math.pow(u0, 0.72));
         float taper1 = (float)Math.sin(Math.PI * Math.pow(u1, 0.72));
         float in0 = FxMath.lerp(outerRadius, innerRadius, taper0);
         float in1 = FxMath.lerp(outerRadius, innerRadius, taper1);

         float d0x = axisU.x * c0 + axisV.x * s0;
         float d0y = axisU.y * c0 + axisV.y * s0;
         float d0z = axisU.z * c0 + axisV.z * s0;
         float d1x = axisU.x * c1 + axisV.x * s1;
         float d1y = axisU.y * c1 + axisV.y * s1;
         float d1z = axisU.z * c1 + axisV.z * s1;

         this.vertex(center.x + d0x * in0, center.y + d0y * in0, center.z + d0z * in0, r, g, b, a, u0, 0.0F);
         this.vertex(center.x + d1x * in1, center.y + d1y * in1, center.z + d1z * in1, r, g, b, a, u1, 0.0F);
         this.vertex(center.x + d1x * outerRadius, center.y + d1y * outerRadius, center.z + d1z * outerRadius, r, g, b, a, u1, 1.0F);
         this.vertex(center.x + d0x * outerRadius, center.y + d0y * outerRadius, center.z + d0z * outerRadius, r, g, b, a, u0, 1.0F);
      }
   }

   /**
    * 3D 立体圆环体（能量星轨、压环、激波环），{@code uv.x} 沿主环 0..1，{@code uv.y} 沿管截面 -1..1（配合 {@link Style#RIBBON} 或 {@link Style#PLASMA_BEAM}）。
    */
   public void torus(Vector3f center, Vector3f normal, float majorRadius, float minorRadius, int majorSegs, int minorSegs, float spin, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F || majorRadius <= 0.0F || minorRadius <= 0.0F) {
         return;
      }
      Vector3f axis = this.tmpA.set(normal).normalize();
      Vector3f right = this.tmpB.set(FxMath.perpendicular(axis)).normalize();
      Vector3f forward = this.tmpC.set(axis).cross(right).normalize();
      majorSegs = Math.max(10, majorSegs);
      minorSegs = Math.max(4, minorSegs);
      for (int i = 0; i < majorSegs; i++) {
         float u0 = (float)i / majorSegs;
         float u1 = (float)(i + 1) / majorSegs;
         float t0 = FxMath.TAU * u0 + spin;
         float t1 = FxMath.TAU * u1 + spin;
         float ct0 = (float)Math.cos(t0);
         float st0 = (float)Math.sin(t0);
         float ct1 = (float)Math.cos(t1);
         float st1 = (float)Math.sin(t1);
         float r0x = right.x * ct0 + forward.x * st0;
         float r0y = right.y * ct0 + forward.y * st0;
         float r0z = right.z * ct0 + forward.z * st0;
         float r1x = right.x * ct1 + forward.x * st1;
         float r1y = right.y * ct1 + forward.y * st1;
         float r1z = right.z * ct1 + forward.z * st1;
         for (int j = 0; j < minorSegs; j++) {
            float v0 = (float)j / minorSegs;
            float v1 = (float)(j + 1) / minorSegs;
            float p0 = FxMath.TAU * v0;
            float p1 = FxMath.TAU * v1;
            float cp0 = (float)Math.cos(p0);
            float sp0 = (float)Math.sin(p0);
            float cp1 = (float)Math.cos(p1);
            float sp1 = (float)Math.sin(p1);
            float vy0 = v0 * 2.0F - 1.0F;
            float vy1 = v1 * 2.0F - 1.0F;
            this.torusVertex(center, axis, r0x, r0y, r0z, majorRadius, minorRadius, cp0, sp0, r, g, b, a, u0, vy0);
            this.torusVertex(center, axis, r1x, r1y, r1z, majorRadius, minorRadius, cp0, sp0, r, g, b, a, u1, vy0);
            this.torusVertex(center, axis, r1x, r1y, r1z, majorRadius, minorRadius, cp1, sp1, r, g, b, a, u1, vy1);
            this.torusVertex(center, axis, r0x, r0y, r0z, majorRadius, minorRadius, cp1, sp1, r, g, b, a, u0, vy1);
         }
      }
   }

   private void torusVertex(Vector3f c, Vector3f axis, float rx, float ry, float rz, float majorR, float minorR, float cp, float sp, float r, float g, float b, float a, float u, float v) {
      float rad = majorR + minorR * cp;
      float x = c.x + rx * rad + axis.x * (minorR * sp);
      float y = c.y + ry * rad + axis.y * (minorR * sp);
      float z = c.z + rz * rad + axis.z * (minorR * sp);
      this.vertex(x, y, z, r, g, b, a, u, v);
   }

   /**
    * 3D 立体悬浮晶格/碎鳞片（菱形双锥棱面，用于在任意角度呈现立体高光反射与实体厚度）。
    */
   public void shard(Vector3f center, Vector3f axisU, Vector3f axisV, Vector3f normal, float thickness, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F) {
         return;
      }
      Vector3f top = this.tmpA.set(center).fma(thickness, normal);
      Vector3f bot = this.tmpB.set(center).fma(-thickness, normal);
      Vector3f p0 = this.tmpC.set(center).add(axisU);
      Vector3f p1 = this.tmpD.set(center).add(axisV);
      Vector3f p2 = this.tmpE.set(center).sub(axisU);
      float p3x = center.x - axisV.x;
      float p3y = center.y - axisV.y;
      float p3z = center.z - axisV.z;
      // 用 4 个退化四边形组成双面菱形晶片，保持 QUADS 批次零切换
      this.vertex(top.x, top.y, top.z, r, g, b, a, 0.0F, 0.0F);
      this.vertex(p0.x, p0.y, p0.z, r, g, b, a, 1.0F, 0.0F);
      this.vertex(p1.x, p1.y, p1.z, r, g, b, a, 0.0F, 1.0F);
      this.vertex(bot.x, bot.y, bot.z, r, g, b, a, 0.0F, 0.0F);

      this.vertex(top.x, top.y, top.z, r, g, b, a, 0.0F, 0.0F);
      this.vertex(p1.x, p1.y, p1.z, r, g, b, a, 0.0F, 1.0F);
      this.vertex(p2.x, p2.y, p2.z, r, g, b, a, -1.0F, 0.0F);
      this.vertex(bot.x, bot.y, bot.z, r, g, b, a, 0.0F, 0.0F);

      this.vertex(top.x, top.y, top.z, r, g, b, a, 0.0F, 0.0F);
      this.vertex(p2.x, p2.y, p2.z, r, g, b, a, -1.0F, 0.0F);
      this.vertex(p3x, p3y, p3z, r, g, b, a, 0.0F, -1.0F);
      this.vertex(bot.x, bot.y, bot.z, r, g, b, a, 0.0F, 0.0F);

      this.vertex(top.x, top.y, top.z, r, g, b, a, 0.0F, 0.0F);
      this.vertex(p3x, p3y, p3z, r, g, b, a, 0.0F, -1.0F);
      this.vertex(p0.x, p0.y, p0.z, r, g, b, a, 1.0F, 0.0F);
      this.vertex(bot.x, bot.y, bot.z, r, g, b, a, 0.0F, 0.0F);
   }

   /**
    * 任意四边形（按环绕顺序给出四个顶点），uv 依次为 (-1,-1) (1,-1) (1,1) (-1,1)。
    * 用于把截取到的模型面原样画进特效层（例如断空的全息残影）。
    */
   public void rawQuad(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F) {
         return;
      }
      this.vertex(v0.x, v0.y, v0.z, r, g, b, a, -1.0F, -1.0F);
      this.vertex(v1.x, v1.y, v1.z, r, g, b, a, 1.0F, -1.0F);
      this.vertex(v2.x, v2.y, v2.z, r, g, b, a, 1.0F, 1.0F);
      this.vertex(v3.x, v3.y, v3.z, r, g, b, a, -1.0F, 1.0F);
   }

   /** 两点间的直线拖尾（火花、流星），头部宽、尾部尖。 */
   public void streak(Vector3f head, Vector3f tail, float halfWidth, float r, float g, float b, float a) {
      if (this.builder == null || a <= 0.0F) {
         return;
      }
      Vector3f tangent = this.tmpA.set(head).sub(tail);
      Vector3f side = this.tmpB.set(head).negate();
      side.set(tangent.y * side.z - tangent.z * side.y, tangent.z * side.x - tangent.x * side.z, tangent.x * side.y - tangent.y * side.x);
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
