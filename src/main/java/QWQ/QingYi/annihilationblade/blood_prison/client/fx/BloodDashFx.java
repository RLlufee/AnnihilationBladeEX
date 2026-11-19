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
 * 血狱领域闪击：瞬移路径上的血色斩痕与残影，目标处炸开交叉斩痕与血花。
 */
@OnlyIn(Dist.CLIENT)
public final class BloodDashFx extends FxInstance {
   private static final float[] CRIMSON = {1.0F, 0.1F, 0.14F};
   private static final float[] HOT = {1.0F, 0.75F, 0.72F};

   private final Vec3 from;
   private final Vec3 to;
   private final Vec3 bulge;
   private final List<Spark> sparks = new ArrayList<>();
   private final RandomSource random;

   public BloodDashFx(ClientLevel level, FxEventPacket packet) {
      super(16);
      this.from = packet.origin();
      this.to = packet.origin().add(packet.direction());
      this.random = RandomSource.create(packet.seed());
      Vec3 dir = packet.direction();
      Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
      this.bulge = side.lengthSqr() < 1.0E-6 ? Vec3.ZERO : side.normalize().scale(dir.length() * 0.12 * (this.random.nextBoolean() ? 1.0 : -1.0));
      for (int i = 0; i < 16; i++) {
         Vector3f v = FxMath.randomUnit(this.random).mul(0.25F + this.random.nextFloat() * 0.35F);
         this.sparks.add(new Spark(this.to, v.x, Math.abs(v.y) + 0.1F, v.z, 8 + this.random.nextInt(8)));
      }
   }

   private Vec3 point(double u) {
      Vec3 control = this.from.add(this.to).scale(0.5).add(this.bulge.scale(2.0));
      double inv = 1.0 - u;
      return this.from.scale(inv * inv).add(control.scale(2.0 * inv * u)).add(this.to.scale(u * u));
   }

   @Override
   public Vec3 focus() {
      return this.to;
   }

   @Override
   public void tick(ClientLevel level) {
      for (Spark spark : this.sparks) {
         spark.tick();
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      if (this.ageTicks() == 0) {
         level.addParticle(ParticleTypes.SWEEP_ATTACK, this.to.x, this.to.y, this.to.z, 0.0, 0.0, 0.0);
         for (int i = 0; i < 12; i++) {
            level.addParticle(ParticleTypes.CRIT, this.to.x, this.to.y, this.to.z, (this.random.nextDouble() - 0.5) * 0.6, this.random.nextDouble() * 0.4, (this.random.nextDouble() - 0.5) * 0.6);
         }
      }
   }

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float head = FxMath.easeOutCubic(FxMath.window(a, 0.0F, 1.5F));
      float tail = FxMath.easeInCubic(FxMath.window(a, 1.0F, 9.0F));
      if (head - tail > 0.01F) {
         int n = 20;
         List<Vector3f> points = new ArrayList<>(n + 1);
         float[] widths = new float[n + 1];
         float[] alphas = new float[n + 1];
         for (int i = 0; i <= n; i++) {
            float k = (float)i / n;
            points.add(frame.rel(this.point(FxMath.lerp(tail, head, k))));
            widths[i] = 0.32F * (float)Math.pow(Math.sin(Math.PI * k), 0.7);
            alphas[i] = 0.4F + 0.6F * k;
         }
         g.begin(Style.RIBBON, 2.6F, 0.2F, 2.5F, 3.0F, 6.0F, 0.8F);
         float[] wide = new float[widths.length];
         for (int i = 0; i < wide.length; i++) {
            wide[i] = widths[i] * 3.0F;
         }
         g.ribbon(points, wide, alphas, CRIMSON[0], CRIMSON[1], CRIMSON[2]);
         g.begin(Style.RIBBON, 9.0F, 0.2F, 8.0F, 6.0F, 10.0F, 0.3F);
         g.ribbon(points, widths, alphas, HOT[0], HOT[1], HOT[2]);
         g.end();
      }

      // 残影：沿路径留下逐渐熄灭的血光
      g.begin(Style.GLOW, 3.0F, 0.4F, 4.0F, 2.0F, 0.0F, 0.0F);
      for (int i = 0; i < 5; i++) {
         float k = (i + 0.5F) / 5.0F;
         float life = 1.0F - FxMath.window(a, k * 3.0F, k * 3.0F + 7.0F);
         if (life > 0.0F && a >= k * 1.5F) {
            g.billboard(frame.rel(this.point(k)), 0.9F, 0.0F, CRIMSON[0], CRIMSON[1], CRIMSON[2], life * 0.7F);
         }
      }

      // 目标处：交叉斩痕 + 血光
      float cut = FxMath.easeOutCubic(FxMath.window(a, 1.0F, 3.0F));
      float cutLife = 1.0F - FxMath.window(a, 4.0F, 12.0F);
      if (cut > 0.0F && cutLife > 0.0F) {
         Vector3f center = frame.rel(this.to);
         g.billboard(center, 2.2F, 0.0F, CRIMSON[0], CRIMSON[1], CRIMSON[2], cutLife);
         g.begin(Style.RIBBON, 10.0F, 0.2F, 8.0F, 5.0F, 8.0F, 0.35F);
         for (int k = 0; k < 2; k++) {
            Vector3f dir = k == 0 ? new Vector3f(1.0F, 0.8F, 0.4F) : new Vector3f(-0.4F, 0.8F, 1.0F);
            dir.normalize().mul(1.6F);
            Vector3f start = new Vector3f(center).sub(dir);
            Vector3f end = new Vector3f(start).lerp(new Vector3f(center).add(dir), cut);
            g.ribbon(List.of(start, new Vector3f(start).lerp(end, 0.5F), end), new float[]{0.0F, 0.14F, 0.05F}, new float[]{cutLife, cutLife, cutLife}, HOT[0], HOT[1], HOT[2]);
         }
      }

      // 血花
      g.begin(Style.RIBBON, 4.0F, 0.0F, 6.0F, 0.0F, 0.0F, 0.5F);
      for (Spark spark : this.sparks) {
         if (spark.life <= 0) {
            continue;
         }
         Vec3 p = spark.position(frame.partialTick);
         g.streak(frame.rel(p), frame.rel(p.subtract(spark.vx * 2.0, spark.vy * 2.0, spark.vz * 2.0)), 0.04F, CRIMSON[0], CRIMSON[1], CRIMSON[2], (float)spark.life / spark.maxLife);
      }
      g.end();
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      double distance = frame.camera.distanceTo(this.to);
      float a = this.age(frame.partialTick);
      if (distance < 12.0 && a < 4.0F) {
         screen.addAberration(0.35F * (1.0F - a / 4.0F) * (float)(1.0 - distance / 12.0));
      }
   }

   private static final class Spark {
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

      Spark(Vec3 origin, double vx, double vy, double vz, int life) {
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
         this.vx *= 0.85;
         this.vy = this.vy * 0.85 - 0.04;
         this.vz *= 0.85;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
