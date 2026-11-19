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
 * 夜陨次元斩 · 单次切痕：在 SlashBlade 原生次元斩球体之上，叠加
 * <ul>
 *   <li>7 道以 0.5 tick 间隔依次划过的交错细斩线（白热核心 + 刀色外晕）；</li>
 *   <li>虚空光核脉冲与向外迸散的空间裂片；</li>
 *   <li>自天而降的紫电（取代原先纯视觉的伽马雷电实体）；</li>
 *   <li>暴击斩（每第 3 斩）额外的地面冲击环与色差。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class JudgementCutFx extends FxInstance {
   private static final int LINES = 7;
   private static final float[] WHITE = {1.0F, 0.95F, 1.0F};
   private static final float[] VOID = {0.69F, 0.15F, 1.0F};

   private final Vec3 ground;
   private final Vec3 center;
   private final float radius;
   private final boolean critical;
   private final float[] tint;
   private final Vec3[] lineStarts = new Vec3[LINES];
   private final Vec3[] lineEnds = new Vec3[LINES];
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
      for (int i = 0; i < (this.critical ? 18 : 10); i++) {
         Vector3f v = FxMath.randomUnit(this.random).mul(0.3F + this.random.nextFloat() * 0.5F);
         this.shards.add(new Shard(this.center, v, 8 + this.random.nextInt(8), i % 3 == 0 ? WHITE : this.tint));
      }
      this.bolt = new FxLightningBolt(this.ground.x, this.ground.z, this.ground.y + 26.0 + this.random.nextDouble() * 10.0, this.ground.y, this.random.nextLong(), 0, VOID);
   }

   @Override
   public Vec3 focus() {
      return this.center;
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

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float fade = 1.0F - FxMath.window(a, 10.0F, 18.0F);

      // 1. 交错细斩线：每道在 1 tick 内划过，依次相隔 0.5 tick
      if (fade > 0.0F) {
         g.begin(Style.RIBBON, 3.0F, 0.3F, 3.0F, 5.0F, 8.0F, 0.8F);
         this.drawLines(g, frame, a, fade, 0.22F, this.tint);
         g.begin(Style.RIBBON, 11.0F, 0.3F, 10.0F, 7.0F, 12.0F, 0.2F);
         this.drawLines(g, frame, a, fade, 0.055F, WHITE);
         g.end();
      }

      // 2. 虚空光核脉冲
      float pulse = 1.0F - FxMath.window(a, 0.0F, 8.0F);
      if (pulse > 0.0F) {
         g.begin(Style.GLOW, 4.0F + (this.critical ? 4.0F : 0.0F), 0.4F, 5.0F, 2.0F, this.critical ? 1.2F : 0.5F, 0.0F);
         g.billboard(frame.rel(this.center), this.radius * (0.6F + 0.5F * (1.0F - pulse)), a * 0.2F, this.tint[0], this.tint[1], this.tint[2], pulse * pulse);
         g.end();
      }

      // 3. 暴击：地面冲击环
      if (this.critical) {
         float t = FxMath.window(a, 0.0F, 10.0F);
         if (t < 1.0F) {
            float r = this.radius * 2.2F * FxMath.easeOutCubic(t);
            g.begin(Style.RING, 5.0F * (1.0F - t), 0.0F, 0.94F, 0.05F, 0.0F, 0.0F);
            g.quad(frame.rel(this.ground.add(0.0, 0.08, 0.0)), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), this.tint[0], this.tint[1], this.tint[2], 1.0F - t);
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

      // 5. 空间裂片
      g.begin(Style.RIBBON, 5.0F, 0.0F, 6.0F, 0.0F, 0.0F, 0.5F);
      for (Shard shard : this.shards) {
         if (shard.life <= 0) {
            continue;
         }
         Vec3 p = shard.position(frame.partialTick);
         g.streak(frame.rel(p), frame.rel(p.subtract(shard.vx * 2.2, shard.vy * 2.2, shard.vz * 2.2)), 0.05F, shard.color[0], shard.color[1], shard.color[2], (float)shard.life / shard.maxLife);
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
      if (a < 4.0F && distance < 32.0) {
         screen.addAberration(0.35F * (1.0F - a / 4.0F) * (float)(1.0 - distance / 32.0));
      }
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

      Shard(Vec3 origin, Vector3f velocity, int life, float[] color) {
         this.x = this.px = origin.x;
         this.y = this.py = origin.y;
         this.z = this.pz = origin.z;
         this.vx = velocity.x;
         this.vy = velocity.y;
         this.vz = velocity.z;
         this.life = life;
         this.maxLife = life;
         this.color = color;
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         this.vx *= 0.82;
         this.vy *= 0.82;
         this.vz *= 0.82;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
