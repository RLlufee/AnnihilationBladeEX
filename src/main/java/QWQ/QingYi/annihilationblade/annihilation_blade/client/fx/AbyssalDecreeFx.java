package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SE「归墟天诏·星冕敕裁」专属渲染（持续特效）。
 * <p>
 * 意象「敕令」：施法者头顶戴上星冕，每一道敕令化作一枚盖向敌人的金印。
 * <pre>
 *  开场   头顶 3.2 格处升起星冕：8 枚金色冠棘 + 帝王紫冠环 + 金色光晕 + 冠心五角星，一道细光柱通向天空
 *  敕裁   一枚冠棘脱离星冕，沿抛物线飞至目标上空；展开成正对镜头的方形「敕印」（金框 + 逐格浮现的紫色篆文），
 *         随后加速盖下：地面拓出同纹金印、四角紫色封印光柱、冲击环、短促金色敕柱；空出的冠棘 3 tick 后重新长出
 *  诏成   （STOP）残余冠棘向中心收拢，星冕缩成一点，冠心之星爆闪并扩散一圈金环
 * </pre>
 * 星冕始终跟随施法者；盖印与印痕位置在敕裁事件到达时由目标解析。
 */
@OnlyIn(Dist.CLIENT)
public final class AbyssalDecreeFx extends FxInstance {
   private static final int SPIKES = 8;
   private static final float CROWN_UP = 3.2F;
   private static final float INTRO = 6.0F;
   private static final float FLIGHT = 1.6F;
   private static final float UNFOLD = 0.8F;
   private static final float HOVER_END = 3.0F;
   private static final float SLAM = 0.9F;
   private static final float IMPACT = HOVER_END + SLAM;
   private static final float REGROW_DELAY = 3.0F;
   private static final float REGROW = 3.0F;
   private static final float IMPRINT_HOLD = 14.0F;
   private static final float IMPRINT_FADE = 8.0F;
   private static final float FINALE_GATHER = 4.0F;
   private static final float FINALE_LIFE = 14.0F;

   private static final float[] GOLD = {1.0F, 0.76F, 0.32F};
   private static final float[] PALE_GOLD = {1.0F, 0.9F, 0.62F};
   private static final float[] PURPLE = {0.62F, 0.28F, 1.0F};
   private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);

   private final int casterId;
   private final float scale;
   private final boolean opened;
   private final List<Decree> decrees = new ArrayList<>();
   private Vec3 crownFallback;
   private int sustainDuration = Integer.MAX_VALUE;
   private int stopAt = -1;

   public AbyssalDecreeFx(ClientLevel level, FxEventPacket packet) {
      super(40);
      this.casterId = packet.casterId();
      this.scale = Math.max(0.4F, Math.min(3.0F, packet.scale()));
      this.crownFallback = packet.origin();
      this.opened = (packet.intParam() & 0xF) == AnnihilationFxEvents.DECREE_BEGIN;
      if (!this.opened) {
         this.addStrike(level, packet);
      }
   }

   // ───────────────────────────── 持续特效钩子 ─────────────────────────────

   @Override
   protected void onSustainedStart(int duration) {
      this.sustainDuration = duration;
      this.lifetime = Math.max(this.lifetime, duration + (int)(IMPACT + IMPRINT_HOLD + IMPRINT_FADE + FINALE_LIFE));
   }

   @Override
   public void onSustainedUpdate(ClientLevel level, FxEventPacket event) {
      if ((event.intParam() & 0xF) == AnnihilationFxEvents.DECREE_STRIKE) {
         this.addStrike(level, event);
      }
   }

   @Override
   public void onSustainedStop() {
      this.beginFinale();
   }

   private void beginFinale() {
      if (this.stopAt >= 0) {
         return;
      }
      this.stopAt = this.age;
      float lastImpact = 0.0F;
      for (Decree decree : this.decrees) {
         lastImpact = Math.max(lastImpact, decree.at + IMPACT + IMPRINT_HOLD + IMPRINT_FADE);
      }
      this.lifetime = (int)Math.ceil(Math.max(this.age + FINALE_LIFE, lastImpact) + 2.0F);
   }

   private void addStrike(ClientLevel level, FxEventPacket packet) {
      int index = packet.intParam() >> 4;
      int[] ids = packet.entityIds();
      Entity target = ids.length > 0 ? level.getEntity(ids[0]) : null;
      Vec3 feet;
      float height;
      if (target != null) {
         feet = target.position();
         height = target.getBbHeight();
      } else {
         feet = packet.origin().add(0.0, -CROWN_UP - 2.0, 0.0);
         height = Math.max(1.0F, packet.radius());
      }
      this.crownFallback = packet.origin();
      this.decrees.add(new Decree(index % SPIKES, feet, height, this.scale, this.age, packet.seed()));
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public Vec3 focus() {
      return this.crownFallback;
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }

   @Override
   public void tick(ClientLevel level) {
      if (this.stopAt < 0 && this.age >= this.sustainDuration) {
         this.beginFinale();
      }
      Entity caster = level.getEntity(this.casterId);
      if (caster != null) {
         this.crownFallback = caster.position().add(0.0, caster.getBbHeight() + CROWN_UP, 0.0);
      }
      int age = this.ageTicks();
      for (Decree decree : this.decrees) {
         if (decree.at == age) {
            play(level, this.crownFallback, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.7F);
         }
         if ((int)Math.floor(decree.at + IMPACT) == age) {
            play(level, decree.head(), SoundEvents.ANVIL_LAND, 0.3F, 1.7F);
         }
      }
      if (this.stopAt >= 0 && age == this.stopAt + (int)FINALE_GATHER) {
         play(level, this.crownFallback, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.4F);
      }
   }

   private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age % 10 == 0 && this.stopAt < 0) {
         Vec3 c = this.crownFallback;
         for (int i = 0; i < SPIKES; i++) {
            double angle = Math.PI * 2.0 * i / SPIKES + age * 0.05;
            level.addParticle(ParticleTypes.END_ROD, c.x + Math.cos(angle) * 1.1, c.y, c.z + Math.sin(angle) * 1.1, 0.0, 0.0, 0.0);
         }
      }
      for (Decree decree : this.decrees) {
         if ((int)Math.floor(decree.at + IMPACT) == age) {
            Vec3 head = decree.head();
            for (int i = 0; i < 5; i++) {
               level.addParticle(ParticleTypes.ENCHANTED_HIT, head.x, head.y, head.z, 0.0, 0.05, 0.0);
            }
         }
      }
   }

   // ───────────────────────────── 星冕几何 ─────────────────────────────

   /** 星冕中心（跟随施法者，带轻微上下起伏）。 */
   private Vec3 crownCenter(FxFrame frame, float t) {
      Entity caster = frame.level.getEntity(this.casterId);
      Vec3 base = caster != null ? caster.getPosition(frame.partialTick).add(0.0, caster.getBbHeight() + CROWN_UP, 0.0) : this.crownFallback;
      return base.add(0.0, Math.sin(t * 0.25) * 0.12, 0.0);
   }

   private float crownRadius() {
      return 1.15F * this.scale;
   }

   private float crownRotation(float t) {
      return t * 0.035F;
   }

   /** 星冕整体显现与诏成时的收拢。 */
   private float crownPresence(float t) {
      float in = this.opened ? FxMath.easeOutCubic(FxMath.window(t, 0.0F, INTRO)) : 1.0F;
      float gather = this.stopAt < 0 ? 0.0F : FxMath.easeInCubic(FxMath.window(t, this.stopAt, this.stopAt + FINALE_GATHER));
      return in * (1.0F - gather);
   }

   private Vec3 slotPosition(Vec3 crown, int slot, float t, float radiusScale) {
      double angle = Math.PI * 2.0 * slot / SPIKES + this.crownRotation(t);
      double r = this.crownRadius() * radiusScale;
      return crown.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
   }

   /** 冠棘在槽位上的缩放：被射出后空缺，3 tick 后重新长出。 */
   private float slotScale(int slot, float t) {
      float scale = 1.0F;
      for (Decree decree : this.decrees) {
         if (decree.slot != slot) {
            continue;
         }
         float e = t - decree.at;
         if (e >= 0.0F && e < REGROW_DELAY + REGROW) {
            scale = Math.min(scale, FxMath.window(e, REGROW_DELAY, REGROW_DELAY + REGROW));
         }
      }
      return scale;
   }

   private static float nearFade(Vector3f rel) {
      return FxMath.window(rel.length(), 1.2F, 3.5F);
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float t = this.age(frame.partialTick);
      Vec3 crown = this.crownCenter(frame, t);
      Vector3f c = frame.rel(crown);
      float presence = this.crownPresence(t);
      float radiusScale = this.stopAt < 0 ? 1.0F : 1.0F - FxMath.easeInCubic(FxMath.window(t, this.stopAt, this.stopAt + FINALE_GATHER));

      // 1. 冠环（帝王紫）与外层金环
      g.begin(Style.RING, 3.0F, 0.2F, 0.95F, 0.03F, 16.0F, 0.15F);
      if (presence > 0.01F) {
         float r = this.crownRadius() * 1.05F * radiusScale / 0.95F;
         g.quad(c, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), PURPLE[0], PURPLE[1], PURPLE[2], presence);
         float r2 = this.crownRadius() * 1.35F * radiusScale / 0.95F;
         g.quad(new Vector3f(c).add(0.0F, -0.08F, 0.0F), new Vector3f(r2, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r2), GOLD[0], GOLD[1], GOLD[2], presence * 0.6F);
      }
      this.renderImpactRings(g, frame, t);
      this.renderFinaleRing(g, frame, t);

      // 2. 金色光晕（水平光盘）与冠棘、飞行中的冠棘
      g.begin(Style.GLOW, 2.0F, 0.2F, 2.0F, 2.0F, 0.0F, 0.0F);
      if (presence > 0.01F) {
         float r = this.crownRadius() * 1.7F * radiusScale;
         g.quad(c, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), GOLD[0], GOLD[1], GOLD[2], presence * 0.35F);
      }
      g.begin(Style.GLOW, 6.0F, 0.0F, 2.0F, 1.0F, 0.0F, 0.0F);
      if (presence > 0.01F) {
         for (int slot = 0; slot < SPIKES; slot++) {
            float s = this.slotScale(slot, t) * presence;
            if (s <= 0.01F) {
               continue;
            }
            Vec3 base = this.slotPosition(crown, slot, t, radiusScale);
            Vec3 radial = base.subtract(crown);
            radial = radial.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : radial.normalize();
            // 冠棘略微外倾；诏成时向中心内倾
            float lean = this.stopAt < 0 ? 0.18F : 0.18F - 0.6F * FxMath.window(t, this.stopAt, this.stopAt + FINALE_GATHER);
            Vec3 axis = new Vec3(0.0, 1.0, 0.0).add(radial.scale(lean)).normalize();
            float length = (slot % 2 == 0 ? 0.55F : 0.4F) * this.scale * s;
            Vector3f center = frame.rel(base.add(axis.scale(length)));
            Vector3f u = toVector(axis).mul(length);
            Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x);
            Vector3f v = toVector(tangent).mul(0.12F * this.scale * s);
            g.shard(center, u, v, toVector(radial), 0.05F * this.scale * s, GOLD[0], GOLD[1], GOLD[2], FxMath.window(center.length(), 0.6F, 1.6F));
         }
      }
      for (Decree decree : this.decrees) {
         float e = t - decree.at;
         if (e < 0.0F || e >= FLIGHT) {
            continue;
         }
         Vec3 position = this.flightPosition(crown, decree, e, t);
         Vec3 ahead = this.flightPosition(crown, decree, Math.min(FLIGHT, e + 0.1F), t);
         Vec3 axis = ahead.subtract(position);
         axis = axis.lengthSqr() < 1.0E-8 ? new Vec3(0.0, -1.0, 0.0) : axis.normalize();
         Vector3f center = frame.rel(position);
         Vector3f u = toVector(axis).mul(0.5F * this.scale);
         Vector3f normal = toVector(axis.cross(new Vec3(0.0, 1.0, 0.0)).lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : axis.cross(new Vec3(0.0, 1.0, 0.0)).normalize());
         Vector3f v = new Vector3f(normal).cross(u).normalize().mul(0.12F * this.scale);
         g.shard(center, u, v, normal, 0.05F * this.scale, PALE_GOLD[0], PALE_GOLD[1], PALE_GOLD[2], 1.0F);
      }

      // 3. 飞行拖尾、天诏之柱、封印光柱、金色敕柱
      g.begin(Style.RIBBON, 5.0F, 0.2F, 6.0F, 0.0F, 0.0F, 0.4F);
      for (Decree decree : this.decrees) {
         float e = t - decree.at;
         if (e < 0.0F || e >= FLIGHT + 0.6F) {
            continue;
         }
         int n = 10;
         List<Vector3f> points = new ArrayList<>(n);
         float[] widths = new float[n];
         float[] alphas = new float[n];
         float head = Math.min(FLIGHT, e);
         float tail = Math.max(0.0F, head - 0.9F);
         float fade = 1.0F - FxMath.window(e, FLIGHT, FLIGHT + 0.6F);
         for (int i = 0; i < n; i++) {
            float k = (float)i / (n - 1);
            Vector3f p = frame.rel(this.flightPosition(crown, decree, FxMath.lerp(tail, head, k), t));
            points.add(p);
            widths[i] = 0.06F * this.scale * k;
            alphas[i] = fade * k * nearFade(p);
         }
         g.ribbon(points, widths, alphas, GOLD[0], GOLD[1], GOLD[2]);
      }
      if (presence > 0.01F) {
         Vector3f bottom = new Vector3f(c).add(0.0F, 0.4F, 0.0F);
         Vector3f top = new Vector3f(c).add(0.0F, 32.0F, 0.0F);
         g.ribbon(List.of(bottom, top), new float[]{0.05F * this.scale, Math.max(0.05F, top.length() * 0.002F)}, new float[]{presence * 0.5F, 0.0F}, PALE_GOLD[0], PALE_GOLD[1], PALE_GOLD[2]);
      }
      for (Decree decree : this.decrees) {
         float e = t - (decree.at + IMPACT);
         if (e < 0.0F || e >= 6.0F) {
            continue;
         }
         float k = 1.0F - e / 6.0F;
         float half = decree.plateHalf * 1.4F;
         for (int corner = 0; corner < 4; corner++) {
            double angle = decree.imprintRotation + Math.PI * 0.25 + corner * Math.PI * 0.5;
            double r = half * Math.sqrt(2.0) * 0.9;
            Vec3 foot = decree.feet.add(Math.cos(angle) * r, 0.05, Math.sin(angle) * r);
            Vector3f a = frame.rel(foot);
            g.streak(frame.rel(foot.add(0.0, 1.6 * this.scale * FxMath.easeOutCubic(FxMath.window(e, 0.0F, 1.0F)), 0.0)), a, 0.06F * this.scale, PURPLE[0], PURPLE[1], PURPLE[2], k * nearFade(a));
         }
         if (e < 2.0F) {
            Vector3f head = frame.rel(decree.head());
            Vector3f sky = frame.rel(decree.head().add(0.0, 10.0, 0.0));
            float pillar = 1.0F - e / 2.0F;
            g.ribbon(List.of(head, sky), new float[]{0.28F * this.scale * pillar, 0.05F}, new float[]{pillar, 0.0F}, PALE_GOLD[0], PALE_GOLD[1], PALE_GOLD[2]);
         }
      }

      // 4. 敕印：悬停展开 → 盖下；地面金印
      for (Decree decree : this.decrees) {
         float e = t - decree.at;
         if (e < FLIGHT || e >= IMPACT + 0.4F) {
            continue;
         }
         float unfold = FxMath.easeOutCubic(FxMath.window(e, FLIGHT, FLIGHT + UNFOLD));
         float reveal = FxMath.window(e, FLIGHT, HOVER_END + 0.2F);
         float slam = FxMath.easeInCubic(FxMath.window(e, HOVER_END, IMPACT));
         Vec3 position = decree.sky().lerp(decree.head(), slam);
         Vector3f p = frame.rel(position);
         float half = decree.plateHalf * unfold * (1.0F - 0.25F * slam);
         float fade = 1.0F - FxMath.window(e, IMPACT, IMPACT + 0.4F);
         g.begin(Style.RUNE, 4.5F + 4.0F * slam, 0.0F, 0.0F, decree.glyphSeed, 3.0F, reveal);
         g.quad(p, new Vector3f(frame.left).mul(half), new Vector3f(frame.up).mul(half), GOLD[0], GOLD[1], GOLD[2], fade * nearFade(p));
      }
      for (Decree decree : this.decrees) {
         float e = t - (decree.at + IMPACT);
         if (e < 0.0F || e >= IMPRINT_HOLD + IMPRINT_FADE) {
            continue;
         }
         float pop = 1.3F - 0.3F * FxMath.easeOutCubic(FxMath.window(e, 0.0F, 2.0F));
         float alpha = 1.0F - FxMath.window(e, IMPRINT_HOLD, IMPRINT_HOLD + IMPRINT_FADE);
         float half = decree.plateHalf * 1.4F * pop;
         float cos = (float)Math.cos(decree.imprintRotation);
         float sin = (float)Math.sin(decree.imprintRotation);
         Vector3f f = frame.rel(decree.feet.add(0.0, 0.05, 0.0));
         g.begin(Style.RUNE, 3.0F + 3.0F * (1.0F - FxMath.window(e, 0.0F, 3.0F)), 0.0F, 0.0F, decree.glyphSeed, 3.0F, 1.0F);
         g.quad(f, new Vector3f(cos * half, 0.0F, sin * half), new Vector3f(-sin * half, 0.0F, cos * half), GOLD[0], GOLD[1], GOLD[2], alpha * nearFade(f));
      }

      // 5. 公告板：冠心之星、盖印白闪、诏成星爆
      g.begin(Style.SPRITE, 6.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.0F);
      if (presence > 0.01F || (this.stopAt >= 0 && t < this.stopAt + FINALE_GATHER + 4.0F)) {
         float flare = this.stopAt < 0 ? 0.0F : FxMath.envelope(FxMath.window(t, this.stopAt + FINALE_GATHER - 0.5F, this.stopAt + FINALE_GATHER + 3.5F), 0.2F, 0.4F);
         Vector3f star = new Vector3f(c).add(0.0F, 0.55F * this.scale, 0.0F);
         float size = (0.42F + 1.2F * flare) * this.scale;
         float alpha = Math.max(presence, flare);
         g.billboard(star, size, t * 0.04F, GOLD[0], GOLD[1], GOLD[2], alpha * nearFade(star));
      }
      g.begin(Style.GLOW, 9.0F, 0.0F, 5.0F, 3.0F, 1.0F, 0.0F);
      for (Decree decree : this.decrees) {
         float e = t - (decree.at + IMPACT);
         if (e < 0.0F || e >= 3.0F) {
            continue;
         }
         float k = 1.0F - e / 3.0F;
         Vector3f head = frame.rel(decree.head());
         g.billboard(head, 1.4F * this.scale, 0.0F, WHITE[0], WHITE[1], WHITE[2], k * k * nearFade(head));
      }
      g.end();
   }

   /** 盖印瞬间：施法者附近轻微震动（取最近一次盖印）。 */
   @Override
   public float shake(FxFrame frame) {
      float t = this.age(frame.partialTick);
      if (frame.camera.distanceTo(this.crownFallback) > 24.0) {
         return 0.0F;
      }
      float strongest = 0.0F;
      for (Decree decree : this.decrees) {
         float e = t - (decree.at + IMPACT);
         if (e >= 0.0F && e < 3.0F) {
            strongest = Math.max(strongest, 0.18F * (1.0F - e / 3.0F));
         }
      }
      return strongest;
   }

   /** 冠棘飞行：从槽位沿抛物线飞到目标上空 8 格。 */
   private Vec3 flightPosition(Vec3 crown, Decree decree, float e, float t) {
      Vec3 from = this.slotPosition(crown, decree.slot, decree.at, 1.0F);
      Vec3 to = decree.sky();
      double k = FxMath.window(e, 0.0F, FLIGHT);
      double eased = 1.0 - (1.0 - k) * (1.0 - k);
      double arc = Math.sin(Math.PI * k) * (3.0 + from.distanceTo(to) * 0.12);
      return from.lerp(to, eased).add(0.0, arc, 0.0);
   }

   private void renderImpactRings(FxGeometry g, FxFrame frame, float t) {
      g.begin(Style.RING, 4.0F, 0.3F, 0.95F, 0.035F, 0.0F, 0.0F);
      for (Decree decree : this.decrees) {
         float e = t - (decree.at + IMPACT);
         if (e < 0.0F || e >= 5.0F) {
            continue;
         }
         float k = e / 5.0F;
         float radius = (0.3F + 3.0F * this.scale * FxMath.easeOutCubic(k)) / 0.95F;
         Vector3f f = frame.rel(decree.feet.add(0.0, 0.08, 0.0));
         g.quad(f, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), GOLD[0], GOLD[1], GOLD[2], (1.0F - k) * (1.0F - k) * nearFade(f));
      }
   }

   private void renderFinaleRing(FxGeometry g, FxFrame frame, float t) {
      if (this.stopAt < 0) {
         return;
      }
      float e = t - (this.stopAt + FINALE_GATHER);
      if (e < 0.0F || e >= 8.0F) {
         return;
      }
      float k = e / 8.0F;
      Vector3f c = frame.rel(this.crownCenter(frame, t));
      float radius = (0.3F + 6.0F * this.scale * FxMath.easeOutCubic(k)) / 0.95F;
      g.begin(Style.RING, 4.5F, 0.2F, 0.95F, 0.03F, 0.0F, 0.0F);
      g.quad(c, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), PALE_GOLD[0], PALE_GOLD[1], PALE_GOLD[2], (1.0F - k) * (1.0F - k));
   }

   private static Vector3f toVector(Vec3 v) {
      return new Vector3f((float)v.x, (float)v.y, (float)v.z);
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private static final class Decree {
      final int slot;
      final Vec3 feet;
      final float height;
      final int at;
      final float plateHalf;
      final float glyphSeed;
      final float imprintRotation;

      Decree(int slot, Vec3 feet, float height, float scale, int at, long seed) {
         this.slot = slot;
         this.feet = feet;
         this.height = height;
         this.at = at;
         this.plateHalf = Math.max(0.9F, height * 0.6F) * scale;
         RandomSource random = RandomSource.create(seed);
         this.glyphSeed = random.nextFloat() * 97.0F;
         this.imprintRotation = random.nextFloat() * (float)Math.PI * 2.0F;
      }

      Vec3 head() {
         return this.feet.add(0.0, this.height + 0.35, 0.0);
      }

      Vec3 sky() {
         return this.head().add(0.0, 8.0, 0.0);
      }
   }
}
