package QWQ.QingYi.annihilationblade.blood_prison.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxScreenState;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
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
 * 血狱十印幻影剑爆破：环形剑阵各自拉出一道蓄力血光指向目标，幻影剑命中瞬间血色爆发（冲击环 + 封印阵 + 血花）。
 * 剑阵起点与服务端 {@code spawnPhantomSwordBurst} 的计算方式完全一致。
 */
@OnlyIn(Dist.CLIENT)
public final class BloodBurstFx extends FxInstance {
   private static final float[] CRIMSON = {1.0F, 0.1F, 0.14F};
   private static final float[] HOT = {1.0F, 0.75F, 0.72F};

   private final Vec3 center;
   private final float radius;
   private final int delay;
   private final List<Vec3> starts = new ArrayList<>();
   private final List<Petal> petals = new ArrayList<>();
   private final RandomSource random;

   public BloodBurstFx(ClientLevel level, FxEventPacket packet) {
      super((packet.intParam() >> 8 & 0xFF) + 24);
      this.center = packet.origin();
      this.radius = Math.max(0.6F, packet.radius());
      this.delay = Math.max(1, packet.intParam() >> 8 & 0xFF);
      this.random = RandomSource.create(packet.seed());
      int swords = Math.max(1, packet.intParam() & 0xFF);
      double scale = Math.max(0.25, packet.scale());
      for (int i = 0; i < swords; i++) {
         double angle = Math.PI * 2.0 * i / swords;
         this.starts.add(this.center.add(Math.cos(angle) * 4.0 * scale, (3.0 + i % 3) * scale, Math.sin(angle) * 4.0 * scale));
      }
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public void tick(ClientLevel level) {
      if (this.ageTicks() == this.delay) {
         for (int i = 0; i < 36; i++) {
            Vector3f v = FxMath.randomUnit(this.random).mul(0.35F + this.random.nextFloat() * 0.5F);
            this.petals.add(new Petal(this.center, v.x, v.y + 0.15F, v.z, 10 + this.random.nextInt(12)));
         }
      }
      for (Petal petal : this.petals) {
         petal.tick();
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      if (this.ageTicks() == this.delay) {
         level.addParticle(ParticleTypes.FLASH, this.center.x, this.center.y, this.center.z, 0.0, 0.0, 0.0);
         for (int i = 0; i < 16; i++) {
            level.addParticle(ParticleTypes.DAMAGE_INDICATOR, this.center.x, this.center.y, this.center.z, (this.random.nextDouble() - 0.5) * 0.5, this.random.nextDouble() * 0.3, (this.random.nextDouble() - 0.5) * 0.5);
         }
      }
   }

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      Vector3f c = frame.rel(this.center);

      // 1. 蓄力：剑阵起点 → 目标的血光引线，越接近命中越亮
      if (a < this.delay + 1.0F) {
         float charge = FxMath.easeInCubic(FxMath.window(a, 0.0F, this.delay));
         float alpha = Math.min(1.0F, a / 3.0F) * (1.0F - FxMath.window(a, this.delay, this.delay + 1.0F));
         g.begin(Style.RIBBON, 2.0F + 6.0F * charge, 0.2F, 6.0F, 5.0F, 9.0F, 0.6F);
         for (Vec3 start : this.starts) {
            Vector3f s = frame.rel(start);
            g.ribbon(List.of(s, new Vector3f(s).lerp(c, 0.5F), c), new float[]{0.03F, 0.05F + 0.06F * charge, 0.02F}, new float[]{alpha, alpha, alpha * 0.5F}, CRIMSON[0], CRIMSON[1], CRIMSON[2]);
         }
         g.begin(Style.GLOW, 3.0F + 4.0F * charge, 0.3F, 5.0F, 2.0F, 0.0F, 0.0F);
         for (Vec3 start : this.starts) {
            g.billboard(frame.rel(start), 0.6F + 0.4F * charge, 0.0F, CRIMSON[0], CRIMSON[1], CRIMSON[2], alpha);
         }
         g.end();
      }

      // 2. 命中爆发
      float t = (a - this.delay) / 12.0F;
      if (t >= 0.0F && t < 1.0F) {
         float fade = (1.0F - t) * (1.0F - t);
         g.begin(Style.GLOW, 10.0F, 0.3F, 5.0F, 2.2F, 1.2F, 0.0F);
         g.billboard(c, this.radius * (1.2F + 1.4F * t), 0.0F, HOT[0], HOT[1], HOT[2], fade);
         g.begin(Style.GLOW, 3.0F, 0.3F, 3.0F, 1.6F, 0.0F, 0.0F);
         g.billboard(c, this.radius * (2.0F + 2.5F * t), 0.0F, CRIMSON[0], CRIMSON[1], CRIMSON[2], 1.0F - t);
         float r = this.radius * (0.6F + 2.4F * FxMath.easeOutCubic(t));
         g.begin(Style.RING, 5.0F, 0.2F, 0.94F, 0.05F, 0.0F, 0.0F);
         g.quad(c, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), CRIMSON[0], CRIMSON[1], CRIMSON[2], 1.0F - t);
         float seal = this.radius * 1.7F;
         g.begin(Style.RUNE, 4.0F, 0.2F, a * 0.15F, 21.0F, 0.0F, FxMath.easeOutCubic(Math.min(1.0F, t * 4.0F)));
         g.quad(c, new Vector3f(seal, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, seal), CRIMSON[0], CRIMSON[1], CRIMSON[2], 1.0F - t);
         g.end();
      }

      // 3. 血花
      if (!this.petals.isEmpty()) {
         g.begin(Style.RIBBON, 4.0F, 0.0F, 6.0F, 0.0F, 0.0F, 0.5F);
         for (Petal petal : this.petals) {
            if (petal.life <= 0) {
               continue;
            }
            Vec3 p = petal.position(frame.partialTick);
            g.streak(frame.rel(p), frame.rel(p.subtract(petal.vx * 2.0, petal.vy * 2.0, petal.vz * 2.0)), 0.05F, CRIMSON[0], CRIMSON[1], CRIMSON[2], (float)petal.life / petal.maxLife);
         }
         g.end();
      }
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      double distance = frame.camera.distanceTo(this.center);
      if (distance < 16.0 && a >= this.delay && a < this.delay + 5.0F) {
         float k = (1.0F - (a - this.delay) / 5.0F) * (float)(1.0 - distance / 16.0);
         screen.addAberration(0.5F * k);
         screen.addFlash(0xFF3030, 0.12F * k * k);
      }
   }

   private static final class Petal {
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

      Petal(Vec3 origin, double vx, double vy, double vz, int life) {
         this.x = this.px = origin.x;
         this.y = this.py = origin.y;
         this.z = this.pz = origin.z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.life = life;
         this.maxLife = life;
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         this.vx *= 0.86;
         this.vy = this.vy * 0.86 - 0.035;
         this.vz *= 0.86;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
