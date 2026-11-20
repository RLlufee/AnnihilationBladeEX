package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxPass;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.client.pipeline.FxShaders;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SE「裂界·因果伤痕」专属渲染（一次性，连锁时每层一个实例）。
 * <p>
 * 意象「伤口」：空间被撕开一道会呼吸、会进食、会注视的伤口，吞下因果后被缝合成疤。
 * <pre>
 *  0 ─ 1     撕裂：竖直品红发丝一闪，横向迸出撕裂火花
 *  1 ─ 5     张开：杏仁形伤口回弹式张开（锯齿刃口 + 暗腔暗星），周围空间被吸向伤口（场景通道）
 *  1 ─ 4     因果丝：品红丝线鞭射向每个被拖入的目标，抖动后绷紧
 *  3 ─ 10    吞噬：目标的魂光沿丝线被吸入，伴随吸附光尘；每吞一颗，伤口脉动一次
 *  4 ─ 13    因果之眼：伤口内睁开金色竖瞳，吞噬时瞳孔放大
 *  2 ─ 16    下缘不断滴落品红光液
 * 12 ─ 16    闭眼、合拢，接缝闪光
 * 14 ─ 19    （仍有下一层连锁时）伤痕沿品红电弧蔓延到下一处伤口
 * 16 ─ 34    缝合疤痕：细线上逐针浮现交叉缝线，随后淡去
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public final class WorldRiftFx extends FxInstance {
   private static final int LIFETIME = 36;
   private static final float TEAR_END = 0.6F;
   private static final float OPEN_END = 3.0F;
   private static final float CLOSE_START = 12.0F;
   private static final float CLOSE_END = 16.0F;
   private static final float ARC_START = 14.0F;
   private static final float SCAR_START = 16.0F;
   private static final int MAX_THREADS = 24;
   private static final int MAX_ARCS = 8;
   private static final int MOTES = 3;
   private static final int DRIPS = 10;
   private static final int TEAR_SPARKS = 12;
   private static final int STITCHES = 7;
   private static final int ARC_POINTS = 17;

   private static final float[] MAGENTA = {0.85F, 0.22F, 1.0F};
   private static final float[] PINK = {1.0F, 0.62F, 0.95F};
   private static final float[] VIOLET = {0.55F, 0.2F, 1.0F};
   private static final float[] GOLD = {1.0F, 0.76F, 0.36F};
   private static final float[] WHITE = {1.0F, 0.96F, 1.0F};
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);

   private final Vec3 center;
   private final float scale;
   private final float halfHeight;
   private final boolean spreads;
   private final List<Tether> threads = new ArrayList<>();
   private final List<Arc> arcs = new ArrayList<>();
   private final float[] dripStart = new float[DRIPS];
   private final float[] dripY = new float[DRIPS];
   private final float[] dripSide = new float[DRIPS];
   private final Vec3[] sparkVelocity = new Vec3[TEAR_SPARKS];
   private final float[] sparkY = new float[TEAR_SPARKS];
   private final float[] stitchTilt = new float[STITCHES];

   public WorldRiftFx(ClientLevel level, FxEventPacket packet) {
      super(LIFETIME);
      this.center = packet.origin();
      this.scale = Math.max(0.4F, Math.min(3.0F, packet.scale()));
      int depth = packet.intParam() & 0xF;
      int maxDepth = (packet.intParam() >> 4) & 0xF;
      this.spreads = depth < maxDepth;
      float radius = Math.max(2.0F, packet.radius());
      RandomSource random = RandomSource.create(packet.seed());

      int[] ids = packet.entityIds();
      for (int i = 0; i < ids.length && this.threads.size() < MAX_THREADS; i++) {
         Entity entity = level.getEntity(ids[i]);
         if (entity == null) {
            continue;
         }
         Vec3 target = entity.getBoundingBox().getCenter();
         float distance = (float)target.distanceTo(this.center);
         if (distance < 0.4F) {
            continue;
         }
         this.threads.add(new Tether(target, 0.8F + Math.min(1.0F, distance / radius) * 2.0F, random));
      }
      this.halfHeight = (1.25F + Math.min(12, this.threads.size()) * 0.13F + radius * 0.04F) * this.scale;

      if (this.spreads) {
         for (int i = 0; i < this.threads.size() && i < MAX_ARCS; i++) {
            this.arcs.add(new Arc(this.center, this.threads.get(i).target, random));
         }
      }
      for (int i = 0; i < DRIPS; i++) {
         this.dripStart[i] = 2.0F + random.nextFloat() * 12.0F;
         this.dripY[i] = -0.85F + random.nextFloat() * 0.6F;
         this.dripSide[i] = random.nextBoolean() ? 1.0F : -1.0F;
      }
      for (int i = 0; i < TEAR_SPARKS; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double speed = 0.18 + random.nextDouble() * 0.22;
         this.sparkVelocity[i] = new Vec3(Math.cos(angle) * speed, (random.nextDouble() - 0.3) * 0.12, Math.sin(angle) * speed);
         this.sparkY[i] = (random.nextFloat() * 2.0F - 1.0F) * 0.8F;
      }
      for (int i = 0; i < STITCHES; i++) {
         this.stitchTilt[i] = (random.nextFloat() - 0.5F) * 0.7F;
      }
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      if (!this.threads.isEmpty() && age == (int)Math.ceil(this.threads.get(0).absorbAt)) {
         level.playLocalSound(this.center.x, this.center.y, this.center.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.45F, 0.6F, false);
      }
      if (age == (int)CLOSE_END - 1) {
         level.playLocalSound(this.center.x, this.center.y, this.center.z, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.5F, 1.5F, false);
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.PORTAL, this.center.x, this.center.y + (i - 5) * 0.2, this.center.z, 0.0, 0.0, 0.0);
         }
      } else if (age == (int)CLOSE_END) {
         for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, this.center.x, this.center.y, this.center.z, 0.0, 0.05, 0.0);
         }
      }
   }

   // ───────────────────────────── 时间轴 ─────────────────────────────

   /** 吞噬脉动：每吞下一颗魂光，伤口短暂张大。 */
   private float pulse(float t) {
      float sum = 0.0F;
      for (Tether thread : this.threads) {
         float e = t - thread.absorbAt;
         if (e >= 0.0F && e < 4.0F) {
            sum += (float)Math.exp(-e * 1.4F);
         }
      }
      return Math.min(1.5F, sum);
   }

   private float open(float t) {
      float open;
      if (t < TEAR_END) {
         open = 0.03F;
      } else if (t < OPEN_END) {
         open = backOut(FxMath.window(t, TEAR_END, OPEN_END));
      } else {
         open = 1.0F;
      }
      open += 0.14F * this.pulse(t);
      if (t >= CLOSE_START) {
         open *= 1.0F - FxMath.easeInCubic(FxMath.window(t, CLOSE_START, CLOSE_END));
      }
      return Math.max(0.0F, open);
   }

   private static float backOut(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float m = x - 1.0F;
      return 1.0F + c3 * m * m * m + c1 * m * m;
   }

   private static float jag(float t) {
      float settle = FxMath.lerp(1.0F, 0.45F, FxMath.window(t, 0.0F, 5.0F));
      return settle + 0.35F * FxMath.window(t, CLOSE_START, CLOSE_END);
   }

   private float eye(float t) {
      float eye = FxMath.easeOutCubic(FxMath.window(t, 4.0F, 6.0F)) * (1.0F - FxMath.window(t, 11.0F, 13.0F));
      return eye * (1.0F + 0.35F * this.pulse(t));
   }

   /** 伤口所在平面：竖直方向为世界向上，横向始终正对相机。 */
   private static Vector3f across(Vector3f rel, FxFrame frame) {
      Vector3f toCamera = new Vector3f(rel).negate();
      Vector3f u = new Vector3f(UP).cross(toCamera);
      if (u.lengthSquared() < 1.0E-6F) {
         return new Vector3f(frame.left);
      }
      return u.normalize();
   }

   /** 近处淡化：近战时伤口就开在眼前，不压暗的话会直接糊满画面。 */
   private static float nearFade(Vector3f rel, float from, float to, float floor) {
      return floor + (1.0F - floor) * FxMath.window(rel.length(), from, to);
   }

   // ───────────────────────────── 本体层 ─────────────────────────────

   @Override
   public boolean hasBody() {
      return true;
   }

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float t = this.age(frame.partialTick);
      float open = this.open(t);
      if (open <= 0.01F) {
         return;
      }
      Vector3f c = frame.rel(this.center);
      float fade = nearFade(c, 2.0F, 5.0F, 0.0F);
      if (fade <= 0.0F) {
         return;
      }
      Vector3f u = across(c, frame).mul(this.halfHeight * 0.7F);
      Vector3f v = new Vector3f(UP).mul(this.halfHeight);
      b.begin(Style.VOID_SLIT, 1.0F, 0.2F, open, jag(t), 0.0F, 0.0F);
      b.quad(c, u, v, 1.0F, 1.0F, 1.0F, fade);
      b.end();
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float t = this.age(frame.partialTick);
      Vector3f c = frame.rel(this.center);
      Vector3f acrossUnit = across(c, frame);
      float fade = nearFade(c, 2.5F, 6.0F, 0.25F);
      float open = this.open(t);
      float pulse = this.pulse(t);

      // 1. 伤口本体：锯齿刃口、暗星、因果之眼
      if (open > 0.01F) {
         Vector3f u = new Vector3f(acrossUnit).mul(this.halfHeight * 0.7F);
         Vector3f v = new Vector3f(UP).mul(this.halfHeight);
         g.begin(Style.VOID_SLIT, 4.5F + 2.5F * pulse, 0.2F, open, jag(t), 0.0F, this.eye(t));
         g.quad(c, u, v, MAGENTA[0], MAGENTA[1], MAGENTA[2], fade);
      }

      // 2. 中心辉光与撕裂瞬间的白闪
      g.begin(Style.GLOW, 5.0F, 0.0F, 3.0F, 2.0F, 0.6F, 0.0F);
      if (open > 0.01F) {
         g.billboard(c, this.halfHeight * 0.55F, 0.0F, MAGENTA[0], MAGENTA[1], MAGENTA[2], (0.3F * Math.min(1.0F, open) + 0.5F * pulse) * fade);
      }
      if (t < 2.0F) {
         float k = 1.0F - t / 2.0F;
         g.billboard(c, this.halfHeight * 0.9F, (float)(Math.PI * 0.5), WHITE[0], WHITE[1], WHITE[2], k * k * fade);
      }

      // 3. 撕裂发丝、合拢接缝闪光、缝合疤痕
      g.begin(Style.RIBBON, 8.0F, 0.2F, 8.0F, 0.0F, 0.0F, 0.35F);
      if (t < 1.4F) {
         float k = 1.0F - FxMath.window(t, 0.4F, 1.4F);
         this.axisLine(g, c, 1.0F, 0.035F * this.scale, k * fade, WHITE);
      }
      if (t >= CLOSE_END - 1.0F && t < CLOSE_END + 1.5F) {
         float k = FxMath.envelope(FxMath.window(t, CLOSE_END - 1.0F, CLOSE_END + 1.5F), 0.3F, 0.5F);
         this.axisLine(g, c, 0.95F, 0.06F * this.scale, k * fade, PINK);
      }
      if (t >= SCAR_START) {
         float scarFade = 1.0F - FxMath.window(t, 26.0F, LIFETIME);
         this.axisLine(g, c, 0.9F, 0.02F * this.scale, 0.55F * scarFade * fade, PINK);
         for (int i = 0; i < STITCHES; i++) {
            float appear = FxMath.window(t, SCAR_START + i * 0.45F, SCAR_START + i * 0.45F + 0.5F);
            if (appear <= 0.0F) {
               continue;
            }
            float y = -0.8F + 1.6F * i / (STITCHES - 1);
            float tilt = this.stitchTilt[i];
            Vector3f p = new Vector3f(c).add(0.0F, y * this.halfHeight * 0.9F, 0.0F);
            Vector3f d = new Vector3f(acrossUnit).mul((float)Math.cos(tilt)).add(new Vector3f(UP).mul((float)Math.sin(tilt)))
               .mul(0.2F * this.scale * appear);
            List<Vector3f> stitch = List.of(new Vector3f(p).sub(d), new Vector3f(p).add(d));
            float[] tint = i % 2 == 0 ? WHITE : PINK;
            g.ribbon(stitch, new float[]{0.022F * this.scale, 0.022F * this.scale}, new float[]{scarFade * fade, scarFade * fade}, tint[0], tint[1], tint[2]);
         }
      }

      // 4. 因果丝：品红外丝 + 白热内芯
      for (int pass = 0; pass < 2; pass++) {
         if (pass == 0) {
            g.begin(Style.RIBBON, 4.5F, 0.2F, 5.0F, 6.0F, 10.0F, 0.6F);
         } else {
            g.begin(Style.RIBBON, 9.0F, 0.2F, 9.0F, 0.0F, 0.0F, 0.2F);
         }
         for (Tether thread : this.threads) {
            this.drawTether(g, frame, thread, t, pass == 0 ? 0.05F : 0.014F, pass == 0 ? MAGENTA : WHITE, frame.time);
         }
      }

      // 5. 魂光与吸附光尘、撕裂火花、下缘滴液
      g.begin(Style.RIBBON, 5.0F, 0.2F, 6.0F, 0.0F, 0.0F, 0.4F);
      for (Tether thread : this.threads) {
         for (int i = 0; i < MOTES; i++) {
            float start = thread.delay + 2.0F + i * 0.6F;
            float m = FxMath.window(t, start, start + 3.5F);
            if (m <= 0.0F || m >= 1.0F) {
               continue;
            }
            Vec3 from = thread.target.add(thread.moteOffset[i]);
            Vec3 head = from.lerp(this.center, FxMath.easeInCubic(m));
            Vec3 tail = head.add(from.subtract(head).normalize().scale(0.5 * this.scale));
            Vector3f rel = frame.rel(head);
            float[] tint = i == 1 ? VIOLET : PINK;
            g.streak(rel, frame.rel(tail), 0.03F * this.scale, tint[0], tint[1], tint[2], (float)Math.sin(Math.PI * m) * nearFade(rel, 1.5F, 4.0F, 0.2F));
         }
      }
      if (t < 5.0F) {
         float k = 1.0F - t / 5.0F;
         for (int i = 0; i < TEAR_SPARKS; i++) {
            Vec3 origin = this.center.add(0.0, this.sparkY[i] * this.halfHeight, 0.0);
            Vec3 head = origin.add(this.sparkVelocity[i].scale(t * (1.0 - t / 12.0)));
            Vec3 tail = head.subtract(this.sparkVelocity[i].scale(0.9));
            Vector3f rel = frame.rel(head);
            float[] tint = i % 3 == 0 ? WHITE : MAGENTA;
            g.streak(rel, frame.rel(tail), 0.025F * this.scale, tint[0], tint[1], tint[2], k * k * nearFade(rel, 1.5F, 4.0F, 0.2F));
         }
      }
      for (int i = 0; i < DRIPS; i++) {
         float e = t - this.dripStart[i];
         if (e < 0.0F || e >= 10.0F || this.dripStart[i] > CLOSE_START) {
            continue;
         }
         float profile = (float)Math.pow(Math.max(0.0, 1.0 - this.dripY[i] * this.dripY[i]), 0.75);
         float rim = 0.35F * this.halfHeight * profile * 0.9F;
         Vector3f head = new Vector3f(c)
            .add(new Vector3f(acrossUnit).mul(rim * this.dripSide[i]))
            .add(0.0F, this.dripY[i] * this.halfHeight - 0.02F * e * e, 0.0F);
         Vector3f tail = new Vector3f(head).add(0.0F, 0.18F + 0.03F * e, 0.0F);
         g.streak(head, tail, 0.028F * this.scale, MAGENTA[0], MAGENTA[1], MAGENTA[2], (1.0F - e / 10.0F) * fade);
      }

      // 6. 魂光本体
      g.begin(Style.GLOW, 7.0F, 0.0F, 4.0F, 2.6F, 0.0F, 0.0F);
      for (Tether thread : this.threads) {
         float start = thread.delay + 3.0F;
         float m = FxMath.window(t, start, thread.absorbAt);
         if (m <= 0.0F || m >= 1.0F) {
            continue;
         }
         Vec3 position = thread.target.lerp(this.center, FxMath.easeInCubic(m));
         Vector3f rel = frame.rel(position);
         float size = 0.42F * this.scale * (1.0F - 0.5F * m);
         float alpha = FxMath.window(t, start, start + 0.6F) * nearFade(rel, 1.5F, 4.0F, 0.2F);
         g.billboard(rel, size, 0.0F, PINK[0], PINK[1], PINK[2], alpha);
         g.billboard(rel, size * 0.4F, 0.0F, WHITE[0], WHITE[1], WHITE[2], alpha);
      }

      // 7. 伤痕蔓延：向下一层伤口张开处的品红电弧
      if (!this.arcs.isEmpty() && t >= ARC_START && t < ARC_START + 5.0F) {
         float e = t - ARC_START;
         float reveal = FxMath.easeOutCubic(FxMath.window(e, 0.0F, 1.5F));
         float alpha = 1.0F - FxMath.window(e, 3.0F, 5.0F);
         int shape = ((int)e) & 1;
         g.begin(Style.RIBBON, 7.0F, 0.2F, 6.0F, 8.0F, 16.0F, 0.6F);
         for (Arc arc : this.arcs) {
            this.drawArc(g, frame, arc, shape, reveal, 0.06F * this.scale, alpha, VIOLET);
         }
         g.begin(Style.RIBBON, 10.0F, 0.2F, 10.0F, 0.0F, 0.0F, 0.2F);
         for (Arc arc : this.arcs) {
            this.drawArc(g, frame, arc, shape, reveal, 0.016F * this.scale, alpha, WHITE);
         }
      }
      g.end();
   }

   /** 沿伤口竖直轴的一条线（长度为伤口半高的 lengthScale 倍，两端收尖）。 */
   private void axisLine(FxGeometry g, Vector3f c, float lengthScale, float width, float alpha, float[] color) {
      if (alpha <= 0.0F) {
         return;
      }
      float half = this.halfHeight * lengthScale;
      int n = 7;
      List<Vector3f> points = new ArrayList<>(n);
      float[] widths = new float[n];
      float[] alphas = new float[n];
      for (int i = 0; i < n; i++) {
         float k = (float)i / (n - 1);
         points.add(new Vector3f(c).add(0.0F, (k * 2.0F - 1.0F) * half, 0.0F));
         float shape = (float)Math.pow(Math.sin(Math.PI * k), 0.6);
         widths[i] = width * Math.max(0.1F, shape);
         alphas[i] = alpha * shape;
      }
      g.ribbon(points, widths, alphas, color[0], color[1], color[2]);
   }

   private void drawTether(FxGeometry g, FxFrame frame, Tether thread, float t, float width, float[] color, float time) {
      float tt = t - thread.delay;
      if (tt < 0.0F || t >= thread.absorbAt + 2.0F) {
         return;
      }
      float reveal = FxMath.easeOutCubic(FxMath.window(tt, 0.0F, 1.5F));
      float retract = FxMath.easeInCubic(FxMath.window(t, thread.absorbAt, thread.absorbAt + 2.0F));
      Vec3 start = this.center;
      Vec3 end = this.center.lerp(thread.target, reveal * (1.0F - retract));
      float alpha = FxMath.window(tt, 0.0F, 0.5F);
      float tighten = FxMath.window(tt, 1.5F, 4.0F);
      float amplitude = (0.35F * (1.0F - tighten) + 0.04F) * this.scale;
      Vector3f startRel = frame.rel(start);
      Vector3f endRel = frame.rel(end);
      Vector3f side = new Vector3f(endRel).sub(startRel).cross(startRel);
      if (side.lengthSquared() < 1.0E-6F) {
         side.set(UP);
      }
      side.normalize();
      int n = 14;
      List<Vector3f> points = new ArrayList<>(n);
      float[] widths = new float[n];
      float[] alphas = new float[n];
      for (int i = 0; i < n; i++) {
         float k = (float)i / (n - 1);
         float wave = (float)(Math.sin(Math.PI * k) * Math.sin(k * 9.0F + time * 14.0F + thread.phase)) * amplitude;
         Vector3f p = new Vector3f(startRel).lerp(endRel, k).add(new Vector3f(side).mul(wave));
         points.add(p);
         widths[i] = width * (1.0F - 0.4F * k);
         alphas[i] = alpha * nearFade(p, 1.2F, 3.5F, 0.2F) * (i == n - 1 ? 0.3F : 1.0F);
      }
      g.ribbon(points, widths, alphas, color[0], color[1], color[2]);
   }

   private void drawArc(FxGeometry g, FxFrame frame, Arc arc, int shape, float reveal, float width, float alpha, float[] color) {
      Vec3[] points = arc.points[shape];
      int count = Math.max(2, (int)Math.ceil(points.length * reveal));
      List<Vector3f> rel = new ArrayList<>(count);
      float[] widths = new float[count];
      float[] alphas = new float[count];
      for (int i = 0; i < count; i++) {
         Vector3f p = frame.rel(points[i]);
         rel.add(p);
         widths[i] = width;
         alphas[i] = alpha * nearFade(p, 1.2F, 3.5F, 0.2F) * (i == count - 1 ? 0.4F : 1.0F);
      }
      g.ribbon(rel, widths, alphas, color[0], color[1], color[2]);
   }

   // ───────────────────────────── 场景通道：空间被吸入伤口 ─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      if (FxShaders.woundScene == null) {
         return;
      }
      float t = this.age(frame.partialTick);
      float open = this.open(t);
      if (open <= 0.02F) {
         return;
      }
      Vector3f c = frame.rel(this.center);
      float near = FxMath.window(c.length(), 3.0F, 6.0F);
      if (near <= 0.0F) {
         return;
      }
      float[] uv = frame.screenUv(this.center);
      float[] top = frame.screenUv(this.center.add(0.0, this.halfHeight * 1.5, 0.0));
      if (uv == null || top == null) {
         return;
      }
      float radius = Math.abs(top[1] - uv[1]) / 1.4F;
      if (radius < 0.005F) {
         return;
      }
      float strength = 0.18F * Math.min(1.2F, open) * near;
      float swirl = 0.35F * Math.min(1.0F, open);
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.woundScene, shader -> {
         shader.safeGetUniform("Wound").set(uv[0], uv[1], radius, strength);
         shader.safeGetUniform("Shape").set(1.4F, swirl, 0.06F, 0.25F);
      }));
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private static final class Tether {
      final Vec3 target;
      final float delay;
      final float absorbAt;
      final float phase;
      final Vec3[] moteOffset = new Vec3[MOTES];

      Tether(Vec3 target, float delay, RandomSource random) {
         this.target = target;
         this.delay = delay;
         this.absorbAt = delay + 6.0F;
         this.phase = random.nextFloat() * 6.28F;
         for (int i = 0; i < MOTES; i++) {
            this.moteOffset[i] = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(0.8);
         }
      }
   }

   /** 伤痕蔓延电弧：两套中点位移形状逐 tick 交替，产生噼啪闪烁。 */
   private static final class Arc {
      final Vec3 target;
      final Vec3[][] points = new Vec3[2][];

      Arc(Vec3 from, Vec3 to, RandomSource random) {
         this.target = to;
         for (int s = 0; s < 2; s++) {
            this.points[s] = displace(from, to, random);
         }
      }

      private static Vec3[] displace(Vec3 from, Vec3 to, RandomSource random) {
         Vec3[] points = new Vec3[ARC_POINTS];
         points[0] = from;
         points[ARC_POINTS - 1] = to;
         double amplitude = from.distanceTo(to) * 0.12;
         subdivide(points, 0, ARC_POINTS - 1, amplitude, random);
         return points;
      }

      private static void subdivide(Vec3[] points, int a, int b, double amplitude, RandomSource random) {
         if (b - a < 2) {
            return;
         }
         int mid = (a + b) / 2;
         Vec3 offset = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(amplitude * 2.0);
         points[mid] = points[a].lerp(points[b], 0.5).add(offset);
         subdivide(points, a, mid, amplitude * 0.55, random);
         subdivide(points, mid, b, amplitude * 0.55, random);
      }
   }
}
