package QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx;

import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.EXPAND_TICKS;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.HOLD_TICKS;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.IMPACT_RADIUS;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.ORBIT_RADIUS;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.ORBIT_SPEED_ACCEL_DEGREES;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.ORBIT_SPEED_START_DEGREES;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.ORBIT_TICKS;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.OUTWARD_DISTANCE;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.RISE_HEIGHT;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.RISE_TICKS;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.SLAM_TICKS;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.SWORD_HEIGHT;
import static QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity.SWORD_SIZE_SCALE;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxScreenState;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 魔龙夜陨 SA「鳞之卫」专属渲染。
 * <p>
 * 剑的可见本体由服务端驱动的 SlashBlade 原生召唤剑承担；本特效用与 {@code ScaleGuardSwordEntity}
 * 完全相同的轨迹公式与调参常量在客户端复现每柄剑的位置，从而让光效与剑严丝合缝。
 * <pre>
 *  绕旋（0-40）  龙鳞护盾包裹施法者，16 柄剑拖出加速旋转的光尾，脚下龙纹法阵
 *  展开（40-50） 护盾随剑阵一同扩张
 *  悬停（50-60） 每柄剑向中心牵出能量丝，护盾鳞片逐片龟裂点亮
 *  升空（60-68） 护盾炸裂为鳞片碎屑，剑拖着竖直光柱升空
 *  砸地（68-73） 16 道下劈光痕；第 73 tick 落地：闪光、冲击环、龙纹印、碎屑，中心总冲击波
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public final class ScaleGuardFx extends FxInstance {
   private static final int EXPAND_END = ORBIT_TICKS + EXPAND_TICKS;
   private static final int HOLD_END = EXPAND_END + HOLD_TICKS;
   private static final int RISE_END = HOLD_END + RISE_TICKS;
   private static final int IMPACT = RISE_END + SLAM_TICKS;
   private static final double ORBIT_R = ORBIT_RADIUS * SWORD_SIZE_SCALE;
   private static final double EXPANDED_R = (ORBIT_RADIUS + OUTWARD_DISTANCE) * SWORD_SIZE_SCALE;
   private static final double HEIGHT = SWORD_HEIGHT * SWORD_SIZE_SCALE;
   private static final double RISE = RISE_HEIGHT * SWORD_SIZE_SCALE;

   private static final float[] SWORD = {0.855F, 0.263F, 1.0F};
   private static final float[] PINK = {1.0F, 0.55F, 0.95F};
   private static final float[] WHITE = {1.0F, 0.95F, 1.0F};

   private final int casterId;
   private final int swords;
   private final RandomSource random;
   private final List<Shard> shards = new ArrayList<>();
   private Vec3 casterFeet;
   private Vec3 lockedCenter;
   private double[] impactGround;

   public ScaleGuardFx(ClientLevel level, FxEventPacket packet) {
      super(IMPACT + 22);
      this.casterId = packet.casterId();
      this.swords = Math.max(1, packet.intParam());
      this.random = RandomSource.create(packet.seed());
      this.casterFeet = packet.origin();
   }

   @Override
   public Vec3 focus() {
      return this.lockedCenter != null ? this.lockedCenter : this.casterFeet;
   }

   // ───────────────────────────── 轨迹（与服务端公式一致）─────────────────────────────

   private static double smooth(double progress) {
      double c = Math.max(0.0, Math.min(1.0, progress));
      return c * c * (3.0 - 2.0 * c);
   }

   private double baseAngle(int index) {
      return Math.PI * 2.0 * index / this.swords;
   }

   private Vec3 casterFeet(FxFrame frame) {
      Entity caster = frame.level.getEntity(this.casterId);
      if (caster != null && this.lockedCenter == null) {
         this.casterFeet = caster.getPosition(frame.partialTick);
      }
      return this.casterFeet;
   }

   private Vec3 center() {
      return this.lockedCenter != null ? this.lockedCenter : this.casterFeet.add(0.0, HEIGHT, 0.0);
   }

   /** 第 index 柄剑在时刻 a 的位置。 */
   private Vec3 swordPos(int index, float a, Vec3 feet) {
      if (a < ORBIT_TICKS || this.lockedCenter == null) {
         double acceleration = a / Math.max(1.0, ORBIT_TICKS);
         double angle = this.baseAngle(index) + Math.toRadians(a * (ORBIT_SPEED_START_DEGREES + ORBIT_SPEED_ACCEL_DEGREES * acceleration));
         return feet.add(Math.cos(angle) * ORBIT_R, HEIGHT, Math.sin(angle) * ORBIT_R);
      }
      double base = this.baseAngle(index);
      Vec3 radial = new Vec3(Math.cos(base), 0.0, Math.sin(base));
      Vec3 orbitPoint = this.lockedCenter.add(radial.scale(ORBIT_R));
      Vec3 expanded = this.lockedCenter.add(radial.scale(EXPANDED_R));
      if (a < EXPAND_END) {
         return orbitPoint.lerp(expanded, smooth((a - ORBIT_TICKS + 1.0) / EXPAND_TICKS));
      }
      if (a < HOLD_END) {
         return expanded;
      }
      if (a < RISE_END) {
         return expanded.add(0.0, RISE * smooth((a - HOLD_END + 1.0) / RISE_TICKS), 0.0);
      }
      double p = smooth((a - RISE_END + 1.0) / SLAM_TICKS);
      return expanded.add(0.0, RISE * (1.0 - p) - 0.75 * p, 0.0);
   }

   private Vec3 impactPoint(int index) {
      double base = this.baseAngle(index);
      Vec3 expanded = this.lockedCenter.add(Math.cos(base) * EXPANDED_R, 0.0, Math.sin(base) * EXPANDED_R);
      return new Vec3(expanded.x, this.impactGround != null ? this.impactGround[index] : expanded.y - 0.75, expanded.z);
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      Entity caster = level.getEntity(this.casterId);
      if (age == ORBIT_TICKS && this.lockedCenter == null) {
         Vec3 feet = caster != null ? caster.position() : this.casterFeet;
         this.casterFeet = feet;
         this.lockedCenter = feet.add(0.0, HEIGHT, 0.0);
         this.impactGround = new double[this.swords];
         for (int i = 0; i < this.swords; i++) {
            double base = this.baseAngle(i);
            Vec3 top = this.lockedCenter.add(Math.cos(base) * EXPANDED_R, 0.0, Math.sin(base) * EXPANDED_R);
            BlockHitResult hit = level.clip(new ClipContext(top, top.subtract(0.0, 12.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            this.impactGround[i] = hit.getType() == HitResult.Type.MISS ? top.y - 0.75 : hit.getLocation().y;
         }
      }
      if (age == HOLD_END && this.lockedCenter != null) {
         // 护盾炸裂：鳞片碎屑自球面向外迸散
         Vec3 c = this.lockedCenter.subtract(0.0, HEIGHT - 1.0, 0.0);
         double r = EXPANDED_R + 0.5;
         for (int i = 0; i < 90; i++) {
            Vector3f dir = FxMath.randomUnit(this.random);
            dir.y = Math.abs(dir.y);
            Vec3 start = c.add(dir.x * r, dir.y * r, dir.z * r);
            float speed = 0.5F + this.random.nextFloat() * 0.7F;
            this.shards.add(new Shard(start, dir.x * speed, dir.y * speed + 0.15, dir.z * speed, 12 + this.random.nextInt(10), i % 3 == 0 ? WHITE : (i % 3 == 1 ? PINK : SWORD), true));
         }
      }
      if (age == IMPACT && this.lockedCenter != null) {
         for (int i = 0; i < this.swords; i++) {
            Vec3 p = this.impactPoint(i);
            for (int k = 0; k < 6; k++) {
               Vector3f dir = FxMath.randomUnit(this.random);
               float speed = 0.3F + this.random.nextFloat() * 0.5F;
               this.shards.add(new Shard(p.add(0.0, 0.2, 0.0), dir.x * speed, Math.abs(dir.y) * speed + 0.25, dir.z * speed, 10 + this.random.nextInt(10), k % 2 == 0 ? PINK : SWORD, true));
            }
         }
      }
      Iterator<Shard> iterator = this.shards.iterator();
      while (iterator.hasNext()) {
         Shard shard = iterator.next();
         shard.tick();
         if (shard.life <= 0) {
            iterator.remove();
         }
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      if (this.ageTicks() == IMPACT && this.lockedCenter != null) {
         for (int i = 0; i < this.swords; i++) {
            Vec3 p = this.impactPoint(i);
            level.addParticle(ParticleTypes.EXPLOSION, p.x, p.y + 0.3, p.z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.DRAGON_BREATH, p.x, p.y + 0.3, p.z, 0.0, 0.05, 0.0);
         }
      }
   }

   // ───────────────────────────── 本体/暗部层（预乘 Alpha）─────────────────────────────

   @Override
   public boolean hasBody() {
      return true;
   }

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float a = this.age(frame.partialTick);
      Vec3 feet = this.casterFeet(frame);
      Vec3 center = this.center();

      // 1. 龙鳞能量护盾半透明本体膜（平滑解析法线 + 三维呼吸波浪形变，盾内自动减透防挡视线）
      float shieldAlpha = FxMath.window(a, 2.0F, 12.0F) * (1.0F - FxMath.window(a, HOLD_END, HOLD_END + 2.0F));
      if (shieldAlpha > 0.01F) {
         float expand = this.lockedCenter != null ? (float)smooth((a - ORBIT_TICKS + 1.0) / EXPAND_TICKS) : 0.0F;
         float radius = FxMath.lerp((float)ORBIT_R + 0.5F, (float)EXPANDED_R + 0.5F, expand);
         Vec3 shieldCenter = center.subtract(0.0, HEIGHT - 1.0, 0.0);
         Vector3f sc = frame.rel(shieldCenter);
         float crack = FxMath.window(a, EXPAND_END, HOLD_END) * 0.75F;
         float spin = a * (0.02F + 0.04F * (float)Math.min(1.0, a / ORBIT_TICKS));
         boolean inside = frame.camera.distanceTo(shieldCenter) < radius;
         b.geoCenter(sc);
         b.begin(Style.SHIELD, 0.9F, 0.6F, 10.0F, crack, a * 0.5F, 0.9F);
         b.deformedSphere(sc, radius, 22, 34, spin, 0.018F + 0.028F * crack, 1.3F, a * 0.22F, SWORD[0], SWORD[1], SWORD[2], shieldAlpha * (inside ? 0.34F : 0.72F));
         b.clearGeoCenter();
      }

      // 2. 16 剑升空与重砸的 3D 暗紫翻滚烟柱外壳
      if (a >= HOLD_END && a < IMPACT && this.lockedCenter != null) {
         boolean slam = a >= RISE_END;
         b.begin(Style.PLASMA_BEAM, 0.85F, 0.4F, 1.5F, slam ? 4.5F : -2.6F, 0.0F, 0.4F);
         for (int i = 0; i < this.swords; i++) {
            Vec3 s = this.swordPos(i, a, feet);
            Vec3 tail = slam ? s.add(0.0, 7.5, 0.0) : this.swordPos(i, HOLD_END, feet);
            if (tail.distanceToSqr(s) < 0.04) {
               continue;
            }
            float r = slam ? 0.65F : 0.42F;
            b.cylinder(frame.rel(s), frame.rel(tail), r, r * 0.45F, 8, a * 0.2F + i, SWORD[0], SWORD[1], SWORD[2], 0.68F);
         }
         b.end();
      }
   }

   // ───────────────────────────── 网格 HDR 发光层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      Vec3 feet = this.casterFeet(frame);
      Vec3 center = this.center();

      // 1. 脚下龙纹法阵：随剑阵展开而扩大，升空后淡出
      float circleAlpha = FxMath.window(a, 0.0F, 8.0F) * (1.0F - FxMath.window(a, RISE_END - 2.0F, IMPACT + 6.0F));
      if (circleAlpha > 0.0F) {
         float expand = (float)smooth((a - ORBIT_TICKS) / EXPAND_TICKS);
         float r = (float)FxMath.lerp((float)ORBIT_R + 1.2F, (float)EXPANDED_R + 1.2F, this.lockedCenter != null ? expand : 0.0F);
         Vec3 groundAt = (this.lockedCenter != null ? this.lockedCenter.subtract(0.0, HEIGHT, 0.0) : feet).add(0.0, 0.06, 0.0);
         g.begin(Style.RUNE, 2.4F, 0.0F, a * 0.06F, 9.0F, 1.0F, FxMath.easeOutCubic(FxMath.window(a, 0.0F, 10.0F)));
         g.quad(frame.rel(groundAt), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), SWORD[0], SWORD[1], SWORD[2], circleAlpha);
         g.end();
      }

      // 2. 龙鳞能量护盾高光外层（平滑解析法线 + 波浪形变 + 交织能量丝线 + 双重菲涅尔硬边）
      float shieldAlpha = FxMath.window(a, 2.0F, 12.0F) * (1.0F - FxMath.window(a, HOLD_END, HOLD_END + 2.0F));
      if (shieldAlpha > 0.0F) {
         float expand = this.lockedCenter != null ? (float)smooth((a - ORBIT_TICKS + 1.0) / EXPAND_TICKS) : 0.0F;
         float radius = FxMath.lerp((float)ORBIT_R + 0.5F, (float)EXPANDED_R + 0.5F, expand);
         Vec3 shieldCenter = center.subtract(0.0, HEIGHT - 1.0, 0.0);
         Vector3f sc = frame.rel(shieldCenter);
         float crack = FxMath.window(a, EXPAND_END, HOLD_END) * 0.75F;
         float spin = a * (0.02F + 0.04F * (float)Math.min(1.0, a / ORBIT_TICKS));
         boolean inside = frame.camera.distanceTo(shieldCenter) < radius;
         g.geoCenter(sc);
         g.begin(Style.SHIELD, 1.6F + 2.6F * crack, 0.6F, 10.0F, crack, a * 0.5F, 1.05F);
         g.deformedSphere(sc, radius * 1.012F, 22, 34, spin, 0.018F + 0.028F * crack, 1.3F, a * 0.22F, PINK[0], PINK[1], PINK[2], shieldAlpha * (inside ? 0.45F : 0.95F));
         g.clearGeoCenter();
      }

      // 3. 绕旋期 3D 定向水平刀弧尾流 + 16 剑流光拖尾
      if (a < ORBIT_TICKS) {
         float spinProgress = Math.min(1.0F, a / ORBIT_TICKS);
         float arcAlpha = FxMath.window(a, 2.0F, 10.0F) * (1.0F - FxMath.window(a, ORBIT_TICKS - 4.0F, ORBIT_TICKS));
         if (arcAlpha > 0.01F) {
            Vector3f orbitCenter = frame.rel(feet.add(0.0, HEIGHT, 0.0));
            Vector3f u = new Vector3f(1.0F, 0.0F, 0.0F);
            Vector3f v = new Vector3f(0.0F, 0.0F, 1.0F);
            float sweepAngle = (float)Math.toRadians(a * (ORBIT_SPEED_START_DEGREES + ORBIT_SPEED_ACCEL_DEGREES * spinProgress));
            g.begin(Style.SLASH_ARC, 3.2F + 2.0F * spinProgress, 0.3F, 6.0F, 0.8F, 1.0F, 0.0F);
            for (int k = 0; k < 4; k++) {
               float ang = sweepAngle + k * (float)(Math.PI * 0.5);
               g.slashArc(orbitCenter, u, v, (float)ORBIT_R - 0.85F, (float)ORBIT_R + 0.35F, ang - 1.25F, ang, 14, SWORD[0], SWORD[1], SWORD[2], arcAlpha * 0.72F);
            }
            g.end();
         }
      }
      if (a < IMPACT) {
         g.begin(Style.RIBBON, 4.0F, 0.3F, 6.0F, 4.0F, 8.0F, 0.6F);
         for (int i = 0; i < this.swords; i++) {
            int n = 10;
            List<Vector3f> points = new ArrayList<>(n + 1);
            float[] widths = new float[n + 1];
            float[] alphas = new float[n + 1];
            boolean afterLock = a >= ORBIT_TICKS && this.lockedCenter != null;
            for (int k = n; k >= 0; k--) {
               float sample = a - k * 0.6F;
               if (afterLock) {
                  sample = Math.max(ORBIT_TICKS, sample);
               } else {
                  sample = Math.max(0.0F, sample);
               }
               points.add(frame.rel(this.swordPos(i, sample, feet)));
               float kk = 1.0F - (float)k / n;
               widths[n - k] = 0.12F + 0.25F * kk;
               alphas[n - k] = kk;
            }
            g.ribbon(points, widths, alphas, SWORD[0], SWORD[1], SWORD[2]);
         }
         g.end();
      }

      // 4. 悬停能量丝 + 立体聚焦星环（torus）
      float threads = FxMath.window(a, EXPAND_END, EXPAND_END + 3.0F) * (1.0F - FxMath.window(a, HOLD_END, HOLD_END + 3.0F));
      if (threads > 0.0F && this.lockedCenter != null) {
         Vector3f c = frame.rel(this.lockedCenter);
         g.begin(Style.RIBBON, 4.2F, 0.3F, 7.0F, 10.0F, 14.0F, 0.45F);
         for (int i = 0; i < this.swords; i++) {
            Vector3f s = frame.rel(this.swordPos(i, a, feet));
            float pulse = 0.6F + 0.4F * (float)Math.sin(a * 1.3F + i);
            g.ribbon(List.of(s, new Vector3f(s).lerp(c, 0.5F), c), new float[]{0.06F, 0.04F, 0.02F}, new float[]{threads * pulse, threads * pulse, threads * 0.4F}, PINK[0], PINK[1], PINK[2]);
         }
         g.torus(c, new Vector3f(0.0F, 1.0F, 0.0F), (float)EXPANDED_R, 0.16F, 32, 5, a * 0.15F, PINK[0], PINK[1], PINK[2], threads * 0.85F);
         g.end();
      }

      // 5. 升空 3D 圆柱光柱 / 下劈 3D 嵌套冲柱
      if (a >= HOLD_END && a < IMPACT && this.lockedCenter != null) {
         boolean slam = a >= RISE_END;
         g.begin(Style.PLASMA_BEAM, slam ? 8.5F : 4.5F, 0.3F, 1.6F, slam ? 5.2F : -3.0F, 0.0F, 0.3F);
         for (int i = 0; i < this.swords; i++) {
            Vec3 s = this.swordPos(i, a, feet);
            Vec3 tail = slam ? s.add(0.0, 7.5, 0.0) : this.swordPos(i, HOLD_END, feet);
            if (tail.distanceToSqr(s) < 0.04) {
               continue;
            }
            float r = slam ? 0.46F : 0.30F;
            g.cylinder(frame.rel(s), frame.rel(tail), r, r * 0.25F, 8, -a * 0.25F + i, slam ? WHITE[0] : SWORD[0], slam ? WHITE[1] : SWORD[1], slam ? WHITE[2] : SWORD[2], 1.0F);
         }
         g.end();
      }

      // 6. 落地：16 道 3D 喇叭形粉紫气浪冲击锥 + 中心 3D 立体总环浪 + 龙纹印 + 闪光
      float t = (a - IMPACT) / 12.0F;
      if (t >= 0.0F && t < 1.0F && this.lockedCenter != null) {
         float fade = (1.0F - t) * (1.0F - t);
         float ease = FxMath.easeOutCubic(t);
         g.begin(Style.SHOCK_CONE, 5.2F, 0.4F, ease, 3.6F, 0.9F, 0.95F);
         for (int i = 0; i < this.swords; i++) {
            Vec3 p = this.impactPoint(i);
            float baseR = 0.35F + 0.9F * ease;
            float topR = (float)(IMPACT_RADIUS * SWORD_SIZE_SCALE) * (0.45F + 0.75F * ease);
            g.cylinder(frame.rel(p.add(0.0, 0.05, 0.0)), frame.rel(p.add(0.0, 1.5 + 4.5 * ease, 0.0)), baseR, topR, 12, a * 0.2F + i, SWORD[0], SWORD[1], SWORD[2], 1.0F - t);
         }
         g.begin(Style.GLOW, 8.0F, 0.4F, 5.0F, 2.2F, 1.0F, 0.0F);
         for (int i = 0; i < this.swords; i++) {
            g.billboard(frame.rel(this.impactPoint(i).add(0.0, 0.8, 0.0)), 2.8F + 3.2F * t, 0.0F, PINK[0], PINK[1], PINK[2], fade);
         }
         g.begin(Style.RUNE, 3.8F, 0.0F, a * 0.1F, 3.0F, 0.0F, FxMath.easeOutCubic(Math.min(1.0F, t * 5.0F)));
         for (int i = 0; i < this.swords; i++) {
            g.quad(frame.rel(this.impactPoint(i).add(0.0, 0.1, 0.0)), new Vector3f(2.6F, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, 2.6F), PINK[0], PINK[1], PINK[2], 1.0F - t);
         }
         Vec3 groundCenter = this.lockedCenter.subtract(0.0, HEIGHT - 0.35, 0.0);
         float big = (float)(EXPANDED_R * 2.1) * ease;
         g.begin(Style.RIBBON, 6.2F * (1.0F - t), 0.4F, 5.5F, 6.0F, 8.0F, 0.5F);
         g.torus(frame.rel(groundCenter), new Vector3f(0.0F, 1.0F, 0.0F), big, 0.32F * (1.0F - t * 0.5F), 32, 6, a * 0.15F, WHITE[0], WHITE[1], WHITE[2], 1.0F - t);
         g.end();
      }

      // 7. 3D 立体旋转紫晶鳞片碎屑（取代平面细线）
      if (!this.shards.isEmpty()) {
         g.begin(Style.GLOW, 5.5F, 0.0F, 1.8F, 1.0F, 0.0F, 0.0F);
         for (Shard shard : this.shards) {
            Vec3 p = shard.position(frame.partialTick);
            Vector3f rel = frame.rel(p);
            float lifeAlpha = (float)shard.life / shard.maxLife;
            float spin = a * 0.45F + shard.maxLife * 1.7F;
            float c = (float)Math.cos(spin);
            float s = (float)Math.sin(spin);
            Vector3f u = new Vector3f(c * 0.30F, s * 0.18F, -s * 0.24F);
            Vector3f v = new Vector3f(-s * 0.16F, c * 0.24F, c * 0.18F);
            Vector3f n = new Vector3f(u).cross(v).normalize();
            g.shard(rel, u, v, n, 0.08F, shard.color[0], shard.color[1], shard.color[2], lifeAlpha);
         }
         g.end();
      }
   }

   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   private float attenuation(FxFrame frame) {
      double distance = frame.camera.distanceTo(this.focus());
      return (float)Math.max(0.0, 1.0 - Math.max(0.0, distance - EXPANDED_R) / 48.0);
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      float att = this.attenuation(frame);
      if (att <= 0.0F) {
         return;
      }
      if (a >= HOLD_END && a < HOLD_END + 4.0F) {
         screen.addAberration(0.5F * (1.0F - (a - HOLD_END) / 4.0F) * att);
      }
      if (a >= IMPACT && this.lockedCenter != null) {
         float t = a - IMPACT;
         float flash = Math.max(0.0F, 1.0F - t / 6.0F);
         screen.addFlash(0xF0B8FF, 0.35F * flash * flash * att, 0.65F);
         screen.addAberration(0.9F * Math.max(0.0F, 1.0F - t / 10.0F) * att);
         screen.addDashBlur(0.55F * Math.max(0.0F, 1.0F - t / 8.0F) * att, frame.screenUv(this.lockedCenter));
         float kick = t < 2.0F ? t / 2.0F : 1.0F - FxMath.easeInOutSine(FxMath.window(t, 2.0F, 10.0F));
         screen.addFovKick(0.06F * kick * att);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      if (a < IMPACT) {
         return 0.0F;
      }
      return 0.6F * (1.0F - FxMath.window(a, IMPACT, IMPACT + 12.0F)) * this.attenuation(frame);
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
      final boolean gravity;

      Shard(Vec3 origin, double vx, double vy, double vz, int life, float[] color, boolean gravity) {
         this.x = this.px = origin.x;
         this.y = this.py = origin.y;
         this.z = this.pz = origin.z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.life = life;
         this.maxLife = life;
         this.color = color;
         this.gravity = gravity;
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         this.vx *= 0.88;
         this.vy = this.vy * 0.88 - (this.gravity ? 0.04 : 0.0);
         this.vz *= 0.88;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
