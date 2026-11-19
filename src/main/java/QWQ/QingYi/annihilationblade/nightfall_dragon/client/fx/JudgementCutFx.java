package QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxScreenState;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxLightningBolt;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 夜陨次元斩 · 单次切痕：
 * <ul>
 *   <li>预乘 Alpha 暗部层切出 3D 实心黑紫空间断层刀弧（高对比鬼泣式暗芯）；</li>
 *   <li>HDR 层叠加 3D 定向曲面刀光弧、交错白热细斩线与 3D 立体空间碎晶；</li>
 *   <li>暴击斩（每第 3 斩）追加 3D 倾斜锁环、破空冲击锥与瞬时向心速度线。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class JudgementCutFx extends FxInstance {
   private static final int LINES = 7;
   private static final int ARCS = 4;
   private static final float[] WHITE = {1.0F, 0.95F, 1.0F};
   private static final float[] VOID = {0.69F, 0.15F, 1.0F};
   private static final float[] DARK_CUT = {0.06F, 0.01F, 0.14F};

   private final Vec3 ground;
   private final Vec3 center;
   private final float radius;
   private final boolean critical;
   private final float[] tint;
   private final Vec3[] lineStarts = new Vec3[LINES];
   private final Vec3[] lineEnds = new Vec3[LINES];
   private final SlashArcData[] arcs = new SlashArcData[ARCS];
   private final List<Shard> shards = new ArrayList<>();
   private final RandomSource random;
   private FxLightningBolt bolt;

   public JudgementCutFx(ClientLevel level, FxEventPacket packet) {
      super(22);
      this.ground = packet.origin();
      this.radius = Math.max(1.5F, packet.radius() * 0.55F);
      this.center = this.ground.add(0.0, this.radius * 0.6, 0.0);
      this.critical = packet.intParam() % 3 == 0;
      int rgb = (int)(packet.seed() & 0xFFFFFFL);
      this.tint = rgb == 0 ? VOID : new float[]{(rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F};
      this.random = RandomSource.create(packet.seed() >>> 32);
      for (int i = 0; i < LINES; i++) {
         Vector3f dir = FxMath.randomUnit(this.random);
         Vector3f offset = FxMath.randomUnit(this.random).mul(this.radius * 0.35F * this.random.nextFloat());
         Vec3 mid = this.center.add(offset.x, offset.y, offset.z);
         Vec3 half = new Vec3(dir.x, dir.y, dir.z).scale(this.radius * (0.9 + this.random.nextDouble() * 0.4));
         this.lineStarts[i] = mid.subtract(half);
         this.lineEnds[i] = mid.add(half);
      }
      for (int i = 0; i < ARCS; i++) {
         Vector3f normal = FxMath.randomUnit(this.random);
         Vector3f u = FxMath.perpendicular(normal).normalize();
         Vector3f v = new Vector3f(normal).cross(u).normalize();
         float startAngle = this.random.nextFloat() * FxMath.TAU;
         float span = (float)(Math.PI * (0.95 + this.random.nextDouble() * 0.55));
         float outerR = this.radius * (0.85F + this.random.nextFloat() * 0.45F) * (this.critical ? 1.25F : 1.0F);
         float innerR = outerR * (0.58F + this.random.nextFloat() * 0.14F);
         this.arcs[i] = new SlashArcData(u, v, innerR, outerR, startAngle, startAngle + span, i * 0.7F);
      }
      for (int i = 0; i < (this.critical ? 20 : 12); i++) {
         Vector3f v = FxMath.randomUnit(this.random).mul(0.28F + this.random.nextFloat() * 0.48F);
         this.shards.add(new Shard(this.center, v, 10 + this.random.nextInt(8), i % 3 == 0 ? WHITE : this.tint, this.random.nextFloat() * FxMath.TAU));
      }
      this.bolt = new FxLightningBolt(this.ground.x, this.ground.z, this.ground.y + 26.0 + this.random.nextDouble() * 10.0, this.ground.y, this.random.nextLong(), 0, VOID);
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public boolean hasBody() {
      return true;
   }

   @Override
   public void tick(ClientLevel level) {
      if (this.bolt != null) {
         this.bolt.tick(this.random);
         if (this.bolt.dead()) {
            this.bolt = null;
         }
      }
      for (Shard shard : this.shards) {
         shard.tick();
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      if (this.ageTicks() == 0) {
         for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, this.center.x, this.center.y, this.center.z, (this.random.nextDouble() - 0.5) * 0.6, (this.random.nextDouble() - 0.5) * 0.6, (this.random.nextDouble() - 0.5) * 0.6);
         }
      }
   }

   // ───────────────────────────── 暗黑空间切口层（预乘 Alpha）─────────────────────────────

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float fade = 1.0F - FxMath.window(a, 10.0F, 18.0F);
      if (fade <= 0.01F) {
         return;
      }
      Vector3f c = frame.rel(this.center);
      for (SlashArcData arc : this.arcs) {
         float t = a - arc.delay;
         if (t <= 0.0F) {
            continue;
         }
         float head = FxMath.easeOutCubic(Math.min(1.0F, t / 2.2F));
         float tail = FxMath.easeInCubic(FxMath.window(t, 4.5F, 14.0F));
         if (tail >= head) {
            continue;
         }
         b.begin(Style.SLASH_ARC, 0.35F, 0.25F, 7.0F, 0.85F, head, tail);
         b.slashArc(c, arc.u, arc.v, arc.innerR * 0.92F, arc.outerR * 1.04F, arc.startAngle, arc.endAngle, 16, DARK_CUT[0], DARK_CUT[1], DARK_CUT[2], fade * 0.92F);
      }
      b.end();
   }

   // ───────────────────────────── HDR 发光层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float fade = 1.0F - FxMath.window(a, 10.0F, 18.0F);
      Vector3f c = frame.rel(this.center);

      // 1. 3D 定向曲面刀光弧（贴在暗黑切口之上的锋利亮刃 + 能量残影）
      if (fade > 0.01F) {
         for (SlashArcData arc : this.arcs) {
            float t = a - arc.delay;
            if (t <= 0.0F) {
               continue;
            }
            float head = FxMath.easeOutCubic(Math.min(1.0F, t / 2.0F));
            float tail = FxMath.easeInCubic(FxMath.window(t, 4.0F, 13.5F));
            if (tail >= head) {
               continue;
            }
            g.begin(Style.SLASH_ARC, 5.2F + (this.critical ? 2.8F : 0.0F), 0.3F, 9.5F, 0.9F, head, tail);
            g.slashArc(c, arc.u, arc.v, arc.innerR, arc.outerR, arc.startAngle, arc.endAngle, 16, this.tint[0], this.tint[1], this.tint[2], fade);
         }
         // 交错白热细斩线：每道在 1 tick 内划过，营造高密次元斩切感
         g.begin(Style.RIBBON, 2.8F, 0.3F, 3.0F, 5.0F, 8.0F, 0.8F);
         this.drawLines(g, frame, a, fade, 0.20F, this.tint);
         g.begin(Style.RIBBON, 11.5F, 0.3F, 10.0F, 7.0F, 12.0F, 0.2F);
         this.drawLines(g, frame, a, fade, 0.055F, WHITE);
         g.end();
      }

      // 2. 虚空光核脉冲
      float pulse = 1.0F - FxMath.window(a, 0.0F, 8.0F);
      if (pulse > 0.0F) {
         g.begin(Style.GLOW, 4.2F + (this.critical ? 4.2F : 0.0F), 0.4F, 5.0F, 2.0F, this.critical ? 1.4F : 0.6F, 0.0F);
         g.billboard(c, this.radius * (0.65F + 0.55F * (1.0F - pulse)), a * 0.2F, this.tint[0], this.tint[1], this.tint[2], pulse * pulse);
         g.end();
      }

      // 3. 暴击斩专属：3D 倾斜锁环（torus）+ 3D 破空冲击锥（SHOCK_CONE）
      if (this.critical) {
         float t = FxMath.window(a, 0.0F, 11.0F);
         if (t < 1.0F) {
            float ease = FxMath.easeOutCubic(t);
            g.begin(Style.SHOCK_CONE, 5.5F * (1.0F - t), 0.4F, ease, 3.8F, 0.95F, 0.9F);
            g.cylinder(frame.rel(this.ground.add(0.0, 0.1, 0.0)), frame.rel(this.ground.add(0.0, 1.8 + 4.2 * ease, 0.0)), 0.45F + 0.8F * ease, this.radius * (1.2F + 1.6F * ease), 18, a * 0.2F, this.tint[0], this.tint[1], this.tint[2], 1.0F - t);
            g.begin(Style.RIBBON, 6.0F * (1.0F - t), 0.3F, 6.0F, 8.0F, 10.0F, 0.5F);
            g.torus(c, new Vector3f(0.35F, 0.9F, -0.25F), this.radius * (0.75F + 1.15F * ease), 0.14F * (1.0F - t * 0.6F), 24, 5, a * 0.25F, WHITE[0], WHITE[1], WHITE[2], 1.0F - t);
            g.end();
         }
      }

      // 4. 紫电
      if (this.bolt != null && this.bolt.visible()) {
         g.begin(Style.RIBBON, 2.5F, 0.4F, 2.0F, 9.0F, 18.0F, 0.9F);
         this.bolt.draw(g, frame, 3.0F, 0.6F, VOID);
         g.begin(Style.RIBBON, 9.0F, 0.4F, 9.0F, 12.0F, 24.0F, 0.2F);
         this.bolt.draw(g, frame, 0.8F, 1.0F, WHITE);
         g.end();
      }

      // 5. 3D 立体空间碎晶（棱镜菱片，随飞行旋转闪光）
      g.begin(Style.GLOW, 6.0F, 0.0F, 2.0F, 1.0F, 0.0F, 0.0F);
      for (Shard shard : this.shards) {
         if (shard.life <= 0) {
            continue;
         }
         Vec3 p = shard.position(frame.partialTick);
         Vector3f rel = frame.rel(p);
         float lifeAlpha = (float)shard.life / shard.maxLife;
         float spin = shard.phase + a * 0.5F;
         float cs = (float)Math.cos(spin);
         float sn = (float)Math.sin(spin);
         Vector3f u = new Vector3f(cs * 0.22F, sn * 0.14F, -sn * 0.18F);
         Vector3f v = new Vector3f(-sn * 0.12F, cs * 0.20F, cs * 0.14F);
         Vector3f n = new Vector3f(u).cross(v).normalize();
         g.shard(rel, u, v, n, 0.06F, shard.color[0], shard.color[1], shard.color[2], lifeAlpha);
      }
      g.end();
   }

   private void drawLines(FxGeometry g, FxFrame frame, float a, float fade, float width, float[] color) {
      for (int i = 0; i < LINES; i++) {
         float head = FxMath.easeOutCubic(FxMath.window(a, i * 0.5F, i * 0.5F + 1.0F));
         if (head <= 0.0F) {
            continue;
         }
         Vec3 end = this.lineStarts[i].lerp(this.lineEnds[i], head);
         List<Vector3f> line = List.of(frame.rel(this.lineStarts[i]), frame.rel(this.lineStarts[i].lerp(end, 0.5)), frame.rel(end));
         g.ribbon(line, new float[]{0.0F, width, width * 0.4F}, new float[]{fade, fade, fade}, color[0], color[1], color[2]);
      }
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      if (!this.critical) {
         return;
      }
      float a = this.age(frame.partialTick);
      double distance = frame.camera.distanceTo(this.center);
      if (a < 5.0F && distance < 32.0) {
         float k = (1.0F - a / 5.0F) * (float)(1.0 - distance / 32.0);
         screen.addAberration(0.4F * k);
         screen.addDashBlur(0.45F * k, frame.screenUv(this.center));
      }
   }

   private record SlashArcData(Vector3f u, Vector3f v, float innerR, float outerR, float startAngle, float endAngle, float delay) {
   }

   private static final class Shard {
      double x;
      double y;
      double z;
      double px;
      double py;
      double pz;
      double vx;
      double vy;
      double vz;
      int life;
      final int maxLife;
      final float[] color;
      final float phase;

      Shard(Vec3 origin, Vector3f velocity, int life, float[] color, float phase) {
         this.x = this.px = origin.x;
         this.y = this.py = origin.y;
         this.z = this.pz = origin.z;
         this.vx = velocity.x;
         this.vy = velocity.y;
         this.vz = velocity.z;
         this.life = life;
         this.maxLife = life;
         this.color = color;
         this.phase = phase;
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         this.vx *= 0.84;
         this.vy *= 0.84;
         this.vz *= 0.84;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
