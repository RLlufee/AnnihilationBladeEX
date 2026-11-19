package QWQ.QingYi.annihilationblade.loli_blade.client.fx;

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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 萝莉刀审判专属渲染（Ciallo～(∠・ω&lt; )⌒☆）。
 * <ul>
 *   <li>范围审判（SA）：脚下爱心法阵弹出，一道粉色审判波横扫全域；波前扫到哪个目标，
 *       就有一颗粉色坠星砸中它，炸开爱心、星芒与闪光碎片，几颗小爱心缓缓飘起。</li>
 *   <li>锥形审判（SE）：沿视线扇形射出星光射线，射线触及的目标同样炸开爱心。</li>
 * </ul>
 * 目标位置在收到事件时（处决之前）解析并记录。
 */
@OnlyIn(Dist.CLIENT)
public final class LoliJudgementFx extends FxInstance {
   private static final int WAVE_TICKS = 14;
   private static final int RAY_TICKS = 6;
   private static final int FALL_TICKS = 5;
   private static final int RAYS = 7;
   private static final int WAVE_SPARKLES = 40;

   private static final float[] PINK = {1.0F, 0.41F, 0.71F};
   private static final float[] LIGHT = {1.0F, 0.78F, 0.9F};
   private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
   private static final float[] GOLD = {1.0F, 0.9F, 0.55F};

   private final boolean area;
   private final Vec3 origin;
   private final Vec3 look;
   private final float range;
   private final float seedFloat;
   private final List<Target> targets = new ArrayList<>();
   private int lastSoundTick = -1;

   public LoliJudgementFx(ClientLevel level, FxEventPacket packet) {
      super(30);
      this.area = packet.intParam() == 1;
      this.origin = packet.origin();
      Vec3 dir = packet.direction();
      this.look = dir.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
      this.range = Math.max(4.0F, packet.radius());
      this.seedFloat = (float)((packet.seed() & 0xFFFFL) / 65535.0);
      float latest = 0.0F;
      int[] ids = packet.entityIds();
      for (int i = 0; i < ids.length; i++) {
         Entity entity = level.getEntity(ids[i]);
         if (entity == null) {
            continue;
         }
         Vec3 center = entity.getBoundingBox().getCenter();
         double distance = this.area ? Math.sqrt(sq(center.x - this.origin.x) + sq(center.z - this.origin.z)) : center.distanceTo(this.origin);
         float duration = this.area ? WAVE_TICKS : RAY_TICKS;
         float hit = Math.max(this.area ? FALL_TICKS : 1.0F, duration * invEaseOut((float)Math.min(1.0, distance / this.range)));
         float size = Math.max(0.8F, Math.min(3.0F, entity.getBbHeight() * 0.6F));
         this.targets.add(new Target(center, hit, size, hash(i * 7 + 3)));
         latest = Math.max(latest, hit);
      }
      this.lifetime = (int)Math.ceil(Math.max(this.area ? WAVE_TICKS + 14 : RAY_TICKS + 12, latest + 24.0F));
   }

   private static double sq(double v) {
      return v * v;
   }

   /** easeOutCubic 的反函数：已知进度求时间比例。 */
   private static float invEaseOut(float y) {
      return 1.0F - (float)Math.cbrt(1.0F - Math.min(1.0F, Math.max(0.0F, y)));
   }

   private float hash(int n) {
      int x = n * 374761393 + (int)(this.seedFloat * 65535.0F) * 668265263;
      x = (x ^ (x >>> 13)) * 1274126177;
      return ((x ^ (x >>> 16)) & 0xFFFFFF) / (float)0xFFFFFF;
   }

   @Override
   public Vec3 focus() {
      return this.origin;
   }

   @Override
   public double cullDistance() {
      return this.range + 128.0;
   }

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      for (Target target : this.targets) {
         if ((int)Math.floor(target.hit) == age && this.lastSoundTick != age) {
            this.lastSoundTick = age;
            level.playLocalSound(target.position.x, target.position.y, target.position.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F + target.random * 0.5F, false);
         }
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      for (Target target : this.targets) {
         if ((int)Math.floor(target.hit) == age) {
            for (int k = 0; k < 3; k++) {
               level.addParticle(ParticleTypes.HEART, target.position.x + (k - 1) * 0.3, target.position.y + 0.5, target.position.z, 0.0, 0.1, 0.0);
            }
         }
      }
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      if (this.area) {
         this.renderArea(g, frame, a);
      } else {
         this.renderCone(g, frame, a);
      }
      this.renderTargets(g, frame, a);
   }

   private void renderArea(FxGeometry g, FxFrame frame, float a) {
      // 爱心法阵：弹出 → 旋转 → 淡出
      float pop = FxMath.window(a, 0.0F, 5.0F);
      float circleAlpha = pop * (1.0F - FxMath.window(a, WAVE_TICKS + 4.0F, WAVE_TICKS + 12.0F));
      if (circleAlpha > 0.0F) {
         float bounce = 1.0F + 0.25F * (float)Math.sin(Math.PI * Math.min(1.0F, a / 6.0F)) * (1.0F - pop * 0.5F);
         float r = 5.0F * bounce * FxMath.easeOutCubic(pop);
         g.begin(Style.RUNE, 3.5F, 0.0F, a * 0.08F, this.seedFloat * 50.0F, 2.0F, 1.0F);
         g.quad(frame.rel(this.origin.add(0.0, 0.06, 0.0)), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), PINK[0], PINK[1], PINK[2], circleAlpha);
      }

      // 审判波：主波前 + 内侧余波 + 波前闪光
      float t = a / WAVE_TICKS;
      if (t < 1.0F) {
         float wave = this.range * FxMath.easeOutCubic(t);
         Vector3f ground = frame.rel(this.origin.add(0.0, 0.1, 0.0));
         g.begin(Style.RING, 5.0F * (1.0F - t * 0.6F), 0.0F, 0.97F, 0.012F, 0.0F, 0.0F);
         g.quad(ground, new Vector3f(wave, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, wave), PINK[0], PINK[1], PINK[2], 1.0F - t);
         float inner = wave * 0.82F;
         g.begin(Style.RING, 2.5F, 0.0F, 0.96F, 0.02F, 48.0F, 0.5F);
         g.quad(new Vector3f(ground).add(0.0F, 0.04F, 0.0F), new Vector3f(inner, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, inner), LIGHT[0], LIGHT[1], LIGHT[2], (1.0F - t) * 0.8F);
         g.begin(Style.SPRITE, 5.0F, 0.2F, 1.0F, 0.0F, 0.0F, 0.0F);
         for (int i = 0; i < WAVE_SPARKLES; i++) {
            double angle = Math.PI * 2.0 * i / WAVE_SPARKLES + this.hash(i) * 0.4;
            double height = 0.4 + this.hash(i + 101) * 2.8;
            float flicker = 0.6F + 0.4F * (float)Math.sin(a * 2.2F + i * 1.7F);
            Vec3 p = this.origin.add(Math.cos(angle) * wave, height, Math.sin(angle) * wave);
            g.billboard(frame.rel(p), 0.45F + 0.35F * this.hash(i + 47), a * 0.3F, i % 4 == 0 ? GOLD[0] : WHITE[0], i % 4 == 0 ? GOLD[1] : WHITE[1], i % 4 == 0 ? GOLD[2] : WHITE[2], (1.0F - t) * flicker);
         }
      }
   }

   private void renderCone(FxGeometry g, FxFrame frame, float a) {
      Vec3 up = Math.abs(this.look.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 side = this.look.cross(up).normalize();
      Vec3 lift = side.cross(this.look).normalize();
      float t = a / RAY_TICKS;
      float fade = 1.0F - FxMath.window(a, RAY_TICKS, RAY_TICKS + 10.0F);
      if (fade <= 0.0F) {
         return;
      }
      float reach = this.range * FxMath.easeOutCubic(Math.min(1.0F, t));
      Vec3 start = this.origin.add(this.look.scale(1.0));
      g.begin(Style.RIBBON, 6.0F, 0.2F, 8.0F, 6.0F, 10.0F, 0.6F);
      Vec3[] heads = new Vec3[RAYS];
      for (int i = 0; i < RAYS; i++) {
         double spread = Math.toRadians(-30.0 + 60.0 * i / (RAYS - 1));
         double tilt = Math.toRadians((this.hash(i) - 0.5) * 16.0);
         Vec3 dir = this.look.scale(Math.cos(spread)).add(side.scale(Math.sin(spread))).add(lift.scale(Math.sin(tilt))).normalize();
         heads[i] = start.add(dir.scale(reach));
         g.ribbon(List.of(frame.rel(start), frame.rel(start.lerp(heads[i], 0.5)), frame.rel(heads[i])), new float[]{0.02F, 0.08F, 0.03F}, new float[]{fade * 0.4F, fade, fade}, PINK[0], PINK[1], PINK[2]);
      }
      g.begin(Style.SPRITE, 6.0F, 0.2F, 1.0F, 0.0F, 0.0F, 0.0F);
      for (int i = 0; i < RAYS; i++) {
         for (int k = 1; k <= 4; k++) {
            Vec3 p = start.lerp(heads[i], k / 4.0);
            g.billboard(frame.rel(p), 0.35F + 0.15F * k, a * 0.4F + i, WHITE[0], WHITE[1], WHITE[2], fade * (0.5F + 0.5F * (float)Math.sin(a * 2.0F + i + k)));
         }
      }
      // 施法者面前的爱心
      float pop = FxMath.easeOutCubic(FxMath.window(a, 0.0F, 3.0F));
      float heartAlpha = pop * (1.0F - FxMath.window(a, 6.0F, 14.0F));
      if (heartAlpha > 0.0F) {
         g.begin(Style.SPRITE, 5.0F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F);
         g.billboard(frame.rel(this.origin.add(this.look.scale(2.2))), 0.7F * pop + 0.2F * (float)Math.sin(a), 0.0F, PINK[0], PINK[1], PINK[2], heartAlpha);
      }
   }

   /**
    * 被审判目标：坠星（范围审判）→ 爱心爆发 + 星芒碎片 + 冲击环 + 飘起的小爱心。
    * 按图元种类跨目标合批（共 7 个批次），目标再多也不会增加绘制调用次数。
    */
   private void renderTargets(FxGeometry g, FxFrame frame, float a) {
      if (this.targets.isEmpty()) {
         return;
      }
      // 1. 坠星拖尾 + 星形头部（仅范围审判）
      if (this.area) {
         g.begin(Style.RIBBON, 6.0F, 0.2F, 6.0F, 5.0F, 8.0F, 0.6F);
         for (Target target : this.targets) {
            Vec3[] comet = this.comet(target, a);
            if (comet != null) {
               g.ribbon(List.of(frame.rel(comet[0]), frame.rel(comet[0].lerp(comet[1], 0.5)), frame.rel(comet[1])), new float[]{0.35F, 0.2F, 0.02F}, new float[]{1.0F, 0.6F, 0.0F}, PINK[0], PINK[1], PINK[2]);
            }
         }
         g.begin(Style.SPRITE, 7.0F, 0.2F, 2.0F, 0.0F, 0.0F, 0.0F);
         for (Target target : this.targets) {
            Vec3[] comet = this.comet(target, a);
            if (comet != null) {
               g.billboard(frame.rel(comet[0]), 0.8F, a * 0.6F, GOLD[0], GOLD[1], GOLD[2], 1.0F);
            }
         }
      }
      // 2. 爱心爆发
      g.begin(Style.SPRITE, 6.0F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F);
      for (Target target : this.targets) {
         float t = a - target.hit;
         float fade = 1.0F - FxMath.window(t, 8.0F, 16.0F);
         if (t >= 0.0F && fade > 0.0F) {
            float pop = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 4.0F));
            g.billboard(frame.rel(target.position), target.size * (0.5F + 0.9F * pop), (float)Math.sin(t * 0.5F) * 0.15F, PINK[0], PINK[1], PINK[2], fade);
         }
      }
      // 3. 爆发辉光
      g.begin(Style.GLOW, 4.0F, 0.2F, 5.0F, 2.0F, 0.6F, 0.0F);
      for (Target target : this.targets) {
         float t = a - target.hit;
         float fade = 1.0F - FxMath.window(t, 8.0F, 16.0F);
         if (t >= 0.0F && fade > 0.0F) {
            float pop = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 4.0F));
            g.billboard(frame.rel(target.position), target.size * 1.8F, 0.0F, PINK[0], PINK[1], PINK[2], fade * (1.0F - pop * 0.6F));
         }
      }
      // 4. 星芒碎片
      g.begin(Style.SPRITE, 6.0F, 0.2F, 1.0F, 0.0F, 0.0F, 0.0F);
      for (Target target : this.targets) {
         float t = a - target.hit;
         float fade = 1.0F - FxMath.window(t, 4.0F, 12.0F);
         if (t < 0.0F || fade <= 0.0F) {
            continue;
         }
         float burst = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 10.0F));
         for (int k = 0; k < 6; k++) {
            double angle = Math.PI * 2.0 * k / 6.0 + target.random * 3.0;
            Vec3 dir = new Vec3(Math.cos(angle), 0.35 + 0.3 * Math.sin(k * 1.7), Math.sin(angle));
            float[] c = k % 2 == 0 ? WHITE : GOLD;
            g.billboard(frame.rel(target.position.add(dir.scale(target.size * 1.6 * burst))), 0.4F, t * 0.5F + k, c[0], c[1], c[2], fade);
         }
      }
      // 5. 冲击环
      g.begin(Style.RING, 4.0F, 0.2F, 0.94F, 0.05F, 0.0F, 0.0F);
      for (Target target : this.targets) {
         float t = a - target.hit;
         float fade = 1.0F - FxMath.window(t, 4.0F, 12.0F);
         if (t < 0.0F || fade <= 0.0F) {
            continue;
         }
         float r = target.size * 2.2F * FxMath.easeOutCubic(FxMath.window(t, 0.0F, 10.0F));
         g.quad(frame.rel(target.position.subtract(0.0, target.size * 0.4, 0.0)), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), PINK[0], PINK[1], PINK[2], fade);
      }
      // 6. 飘起的小爱心
      g.begin(Style.SPRITE, 4.0F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F);
      for (Target target : this.targets) {
         float t = a - target.hit;
         if (t < 0.0F || t > 22.0F) {
            continue;
         }
         for (int k = 0; k < 3; k++) {
            float rise = FxMath.window(t, 2.0F + k * 1.5F, 20.0F + k);
            if (rise <= 0.0F || rise >= 1.0F) {
               continue;
            }
            double sway = Math.sin(t * 0.35 + k * 2.0) * 0.35;
            Vec3 q = target.position.add((k - 1) * 0.45 + sway, target.size * 0.4 + rise * 2.4, sway * 0.5);
            g.billboard(frame.rel(q), 0.28F, (float)sway * 0.5F, LIGHT[0], LIGHT[1], LIGHT[2], (float)Math.sin(Math.PI * rise));
         }
      }
      g.end();
   }

   /** 坠星当前的 {头部, 尾部}；不在下落阶段时返回 null。 */
   private Vec3[] comet(Target target, float a) {
      float start = target.hit - FALL_TICKS;
      if (a < start || a >= target.hit) {
         return null;
      }
      float fall = FxMath.easeInCubic(FxMath.window(a, start, target.hit));
      double angle = target.random * Math.PI * 2.0;
      Vec3 sky = target.position.add(Math.cos(angle) * 4.0, 24.0, Math.sin(angle) * 4.0);
      Vec3 head = sky.lerp(target.position, fall);
      return new Vec3[]{head, head.add(sky.subtract(target.position).normalize().scale(5.0))};
   }
   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      double distance = frame.camera.distanceTo(this.origin);
      if (distance < 24.0 && a < 5.0F) {
         float k = (1.0F - a / 5.0F) * (float)(1.0 - distance / 24.0);
         screen.addFlash(0xFF9AD0, (this.area ? 0.15F : 0.06F) * k * k);
         screen.addAberration((this.area ? 0.3F : 0.15F) * k);
      }
   }

   private record Target(Vec3 position, float hit, float size, float random) {
   }
}
