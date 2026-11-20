package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxPass;
import QWQ.QingYi.annihilationblade.render.client.core.FxScreenState;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.client.pipeline.FxShaders;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SE「星寂裁决·断天终判」专属渲染（一次性）。
 * <p>
 * 意象「断天」：一刀把天幕从上到下劈开，判定带上空的群星与带中的每个敌人一同熄灭，天地归于寂静。
 * <pre>
 *  0 ─ 2     断天之刃：白色光刃自高空劈落到判定带尽头；贴地裂线沿地形从脚下冲向远方，两侧烧出焦痕
 *  2         天幕裂开：落点处竖起数十格高的直刃虚空裂缝（回弹张开），两侧画面被推开，全屏天空开始熄灭
 *  2 ─ 8     星针审判：每个目标按远近依次被一根星针自天而降刺中，身上亮起白星随即向内熄灭成黑点；
 *            上空悬着的百余颗星辰也由近到远逐颗熄灭
 *  5 ─ 16    裂缝两侧冷青电弧噼啪闪动
 * 16 ─ 26    裂缝由上下两端向中点合拢，接缝闪光，天光恢复
 * 26 ─ 40    焦痕淡去
 * </pre>
 * 以施法者视角设计：裂缝始终正对相机，判定带被看成一道把天空左右分开的竖直裂口。
 */
@OnlyIn(Dist.CLIENT)
public final class StarlessJudgementFx extends FxInstance {
   private static final int LIFETIME = 40;
   private static final float DESCEND = 2.0F;
   private static final float OPEN_END = 5.0F;
   private static final float NEEDLE_SPREAD = 4.0F;
   private static final float NEEDLE_FALL = 1.2F;
   private static final float CLOSE_START = 16.0F;
   private static final float CLOSE_END = 26.0F;
   private static final int STARS = 120;
   private static final int GROUND_POINTS = 24;
   private static final int CRACKLES = 4;
   private static final int MAX_NEEDLES = 80;
   private static final int SOUND_SAMPLES = 5;

   private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
   private static final float[] ICE = {0.6F, 0.9F, 1.0F};
   private static final float[] STAR = {0.85F, 0.92F, 1.0F};
   private static final float[] DARK = {0.0F, 0.0F, 0.02F};
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);

   private final Vec3 start;
   private final Vec3 dir;
   private final Vec3 right;
   private final double range;
   private final double width;
   private final float scale;
   private final float slitHeight;
   private final float slitHalfWidth;
   private final Vec3[] ground = new Vec3[GROUND_POINTS + 1];
   private final Vec3 far;
   private final List<Needle> needles = new ArrayList<>();
   private final Vec3[] starPos = new Vec3[STARS];
   private final float[] starAppear = new float[STARS];
   private final float[] starOut = new float[STARS];
   private final float[] starPhase = new float[STARS];
   private final long seed;

   public StarlessJudgementFx(ClientLevel level, FxEventPacket packet) {
      super(LIFETIME);
      this.start = packet.origin();
      Vec3 direction = packet.direction();
      this.dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
      Vec3 horizontal = new Vec3(this.dir.x, 0.0, this.dir.z);
      horizontal = horizontal.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
      this.right = horizontal.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      this.range = Math.max(4.0, packet.radius());
      this.width = Math.max(1.0, packet.scale());
      this.scale = Math.max(0.4F, Math.min(3.0F, packet.intParam() / 10.0F));
      this.slitHeight = 46.0F * this.scale;
      this.slitHalfWidth = 7.0F * this.scale;
      this.seed = packet.seed();
      RandomSource random = RandomSource.create(packet.seed());

      // 贴地裂线：沿判定带水平投影逐点取地表高度
      for (int i = 0; i <= GROUND_POINTS; i++) {
         Vec3 p = this.start.add(horizontal.scale(this.range * i / GROUND_POINTS));
         int x = Mth.floor(p.x);
         int z = Mth.floor(p.z);
         double surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
         double y = surface > this.start.y + 1.0 ? this.start.y - 1.6 : surface + 0.05;
         this.ground[i] = new Vec3(p.x, y, p.z);
      }
      this.far = this.ground[GROUND_POINTS];

      int[] ids = packet.entityIds();
      for (int i = 0; i < ids.length && this.needles.size() < MAX_NEEDLES; i++) {
         Entity entity = level.getEntity(ids[i]);
         if (entity == null) {
            continue;
         }
         Vec3 center = entity.getBoundingBox().getCenter();
         double projection = Math.max(0.0, Math.min(this.range, center.subtract(this.start).dot(this.dir)));
         float impact = DESCEND + (float)(projection / this.range) * NEEDLE_SPREAD;
         this.needles.add(new Needle(center, entity.position(), Math.max(0.6F, entity.getBbHeight() * 0.5F), impact));
      }

      for (int i = 0; i < STARS; i++) {
         double x = random.nextDouble() * this.range;
         double lateral = (random.nextDouble() - 0.5) * this.width * 3.0;
         double height = 12.0 + random.nextDouble() * 26.0;
         Vec3 base = this.start.add(horizontal.scale(x)).add(this.right.scale(lateral));
         this.starPos[i] = new Vec3(base.x, this.start.y + height, base.z);
         this.starAppear[i] = 0.3F + random.nextFloat() * 2.0F;
         this.starOut[i] = DESCEND + (float)(x / this.range) * NEEDLE_SPREAD + random.nextFloat() * 6.0F;
         this.starPhase[i] = random.nextFloat() * 6.28F;
      }
   }

   @Override
   public Vec3 focus() {
      return this.start.add(this.dir.scale(this.range * 0.5));
   }

   @Override
   public double cullDistance() {
      return this.range + 160.0;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      Vec3 near = this.start.add(this.dir.scale(8.0));
      if (age == (int)DESCEND) {
         play(level, near, SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 1.3F);
      }
      if (age == (int)CLOSE_END - 2) {
         play(level, near, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.9F, 0.6F);
      }
      int stride = Math.max(1, this.needles.size() / SOUND_SAMPLES);
      for (int i = 0; i < this.needles.size(); i += stride) {
         Needle needle = this.needles.get(i);
         if ((int)Math.floor(needle.impact + NEEDLE_FALL) == age) {
            play(level, needle.center, SoundEvents.AMETHYST_BLOCK_BREAK, 0.4F, 1.8F);
         }
      }
   }

   private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == (int)DESCEND) {
         for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.END_ROD, this.far.x, this.far.y + i * 2.5, this.far.z, 0.0, 0.0, 0.0);
         }
      }
      for (Needle needle : this.needles) {
         if ((int)Math.floor(needle.impact + NEEDLE_FALL) == age) {
            for (int i = 0; i < 3; i++) {
               level.addParticle(ParticleTypes.END_ROD, needle.center.x, needle.center.y, needle.center.z, 0.0, 0.08, 0.0);
            }
         }
      }
   }

   // ───────────────────────────── 时间轴 ─────────────────────────────

   private static float backOut(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float m = x - 1.0F;
      return 1.0F + c3 * m * m * m + c1 * m * m;
   }

   private static float open(float t) {
      if (t < DESCEND) {
         return 0.0F;
      }
      return Math.max(0.0F, backOut(FxMath.window(t, DESCEND, OPEN_END))) * (1.0F - 0.6F * FxMath.easeInCubic(FxMath.window(t, CLOSE_START, CLOSE_END)));
   }

   /** 合拢：裂缝的竖向半高从上下两端向中点收缩。 */
   private static float closeHeight(float t) {
      return 1.0F - FxMath.easeInCubic(FxMath.window(t, CLOSE_START, CLOSE_END));
   }

   private float skyDarkness(float t) {
      return FxMath.easeOutCubic(FxMath.window(t, DESCEND, DESCEND + 4.0F)) * (1.0F - FxMath.window(t, CLOSE_END - 4.0F, CLOSE_END + 6.0F));
   }

   private Vec3 slitCenter() {
      return this.far.add(0.0, this.slitHeight * 0.5, 0.0);
   }

   private static Vector3f across(Vector3f rel, FxFrame frame) {
      Vector3f u = new Vector3f(UP).cross(new Vector3f(rel).negate());
      return u.lengthSquared() < 1.0E-6F ? new Vector3f(frame.left) : u.normalize();
   }

   /** 远处的线条按距离加粗，避免细到闪烁。 */
   private static float distanceWidth(Vector3f rel, float base, float perBlock) {
      return Math.max(base, rel.length() * perBlock);
   }

   private static float nearFade(Vector3f rel) {
      return FxMath.window(rel.length(), 1.5F, 4.0F);
   }

   // ───────────────────────────── 本体层 ─────────────────────────────

   @Override
   public boolean hasBody() {
      return true;
   }

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float t = this.age(frame.partialTick);
      // 天幕裂缝暗腔
      float open = open(t);
      float height = closeHeight(t);
      if (open > 0.01F && height > 0.01F) {
         Vector3f c = frame.rel(this.slitCenter());
         Vector3f u = across(c, frame).mul(this.slitHalfWidth);
         Vector3f v = new Vector3f(UP).mul(this.slitHeight * 0.5F * height);
         b.begin(Style.VOID_SLIT, 1.0F, 0.0F, open, 0.6F, 1.0F, 0.0F);
         b.quad(c, u, v, 1.0F, 1.0F, 1.0F, 1.0F);
      }
      // 贴地焦痕
      float scorch = 0.75F * (1.0F - FxMath.window(t, 28.0F, LIFETIME));
      if (scorch > 0.0F) {
         b.begin(Style.RIBBON, 0.8F, 0.3F, 2.0F, 0.0F, 0.0F, 0.6F);
         this.groundLine(b, frame, t, 0.45F * this.scale, scorch, DARK, false);
      }
      // 熄灭的星：中针目标身上的暗点
      b.begin(Style.GLOW, 0.9F, 0.0F, 3.0F, 2.5F, 0.0F, 0.0F);
      for (Needle needle : this.needles) {
         float e = t - (needle.impact + NEEDLE_FALL);
         if (e < 1.0F || e >= 7.0F) {
            continue;
         }
         float grow = FxMath.easeOutCubic(FxMath.window(e, 1.0F, 2.5F));
         float fade = 1.0F - FxMath.window(e, 4.5F, 7.0F);
         Vector3f c = frame.rel(needle.center);
         b.billboard(c, 0.55F * needle.size * grow, 0.0F, DARK[0], DARK[1], DARK[2], 0.9F * fade * nearFade(c));
      }
      b.end();
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float t = this.age(frame.partialTick);
      Vec3 slit = this.slitCenter();
      Vector3f c = frame.rel(slit);
      Vector3f acrossUnit = across(c, frame);
      float open = open(t);
      float height = closeHeight(t);

      // 1. 天幕裂缝刃口
      if (open > 0.01F && height > 0.01F) {
         g.begin(Style.VOID_SLIT, 6.0F, 0.0F, open, 0.6F, 1.0F, 0.1F);
         g.quad(c, new Vector3f(acrossUnit).mul(this.slitHalfWidth), new Vector3f(UP).mul(this.slitHeight * 0.5F * height), ICE[0], ICE[1], ICE[2], 1.0F);
      }

      // 2. 断天之刃、贴地裂线、合拢接缝
      g.begin(Style.RIBBON, 10.0F, 0.2F, 8.0F, 0.0F, 0.0F, 0.3F);
      if (t < DESCEND + 1.5F) {
         float fall = FxMath.easeInCubic(FxMath.window(t, 0.0F, DESCEND));
         float fade = 1.0F - FxMath.window(t, DESCEND + 0.3F, DESCEND + 1.5F);
         Vec3 top = this.far.add(0.0, this.slitHeight + 6.0, 0.0);
         Vec3 head = top.add(0.0, -(this.slitHeight + 6.0) * fall, 0.0);
         this.verticalBeam(g, frame, top, head, 0.06F, 0.004F, fade, WHITE);
      }
      float groundCore = (1.0F - 0.8F * FxMath.window(t, 8.0F, 20.0F)) * (1.0F - FxMath.window(t, 30.0F, LIFETIME));
      this.groundLine(g, frame, t, 0.07F * this.scale, groundCore, WHITE, true);
      if (t >= CLOSE_END - 1.5F && t < CLOSE_END + 1.5F) {
         float k = FxMath.envelope(FxMath.window(t, CLOSE_END - 1.5F, CLOSE_END + 1.5F), 0.3F, 0.5F);
         float half = this.slitHeight * 0.5F * Math.max(0.12F, height);
         this.verticalBeam(g, frame, slit.add(0.0, half, 0.0), slit.add(0.0, -half, 0.0), 0.05F, 0.003F, k, WHITE);
      }

      // 3. 外层冷光：断天之刃与裂线的柔光
      g.begin(Style.RIBBON, 3.0F, 0.2F, 3.0F, 0.0F, 0.0F, 0.8F);
      if (t < DESCEND + 1.5F) {
         float fall = FxMath.easeInCubic(FxMath.window(t, 0.0F, DESCEND));
         float fade = 1.0F - FxMath.window(t, DESCEND + 0.3F, DESCEND + 1.5F);
         Vec3 top = this.far.add(0.0, this.slitHeight + 6.0, 0.0);
         this.verticalBeam(g, frame, top, top.add(0.0, -(this.slitHeight + 6.0) * fall, 0.0), 0.3F, 0.014F, fade * 0.5F, ICE);
      }
      this.groundLine(g, frame, t, 0.3F * this.scale, groundCore * 0.35F, ICE, true);

      // 4. 星针与裂缝电弧
      g.begin(Style.RIBBON, 9.0F, 0.2F, 8.0F, 0.0F, 0.0F, 0.4F);
      for (Needle needle : this.needles) {
         float e = t - needle.impact;
         if (e < 0.0F || e >= NEEDLE_FALL + 1.0F) {
            continue;
         }
         float fall = FxMath.easeInCubic(FxMath.window(e, 0.0F, NEEDLE_FALL));
         float fade = 1.0F - FxMath.window(e, NEEDLE_FALL, NEEDLE_FALL + 1.0F);
         Vec3 top = needle.center.add(0.0, 26.0, 0.0);
         Vec3 head = top.lerp(needle.center, fall);
         Vec3 tail = head.add(0.0, 4.0 * this.scale, 0.0);
         Vector3f rel = frame.rel(head);
         g.streak(rel, frame.rel(tail), distanceWidth(rel, 0.05F, 0.003F), WHITE[0], WHITE[1], WHITE[2], fade * nearFade(rel));
      }
      if (t >= OPEN_END && t < CLOSE_START && open > 0.05F) {
         int tick = (int)t;
         if (t - tick < 0.6F) {
            RandomSource random = RandomSource.create(this.seed ^ (tick * 341873128712L));
            float halfWorld = 0.5F * open * this.slitHalfWidth;
            for (int j = 0; j < CRACKLES; j++) {
               float side = random.nextBoolean() ? 1.0F : -1.0F;
               float y = (random.nextFloat() * 1.6F - 0.8F) * this.slitHeight * 0.5F;
               Vector3f p = new Vector3f(c).add(new Vector3f(acrossUnit).mul(halfWorld * side)).add(0.0F, y, 0.0F);
               List<Vector3f> bolt = new ArrayList<>(5);
               float length = (1.5F + random.nextFloat() * 1.5F) * this.scale;
               for (int k = 0; k < 5; k++) {
                  float f = k / 4.0F;
                  bolt.add(new Vector3f(p)
                     .add(new Vector3f(acrossUnit).mul(side * length * f))
                     .add(0.0F, (random.nextFloat() - 0.5F) * 0.8F * this.scale + f * length * 0.3F, 0.0F));
               }
               float w = distanceWidth(p, 0.04F, 0.0025F);
               g.ribbon(bolt, new float[]{w, w, w * 0.8F, w * 0.6F, w * 0.3F}, new float[]{1.0F, 0.9F, 0.7F, 0.5F, 0.0F}, ICE[0], ICE[1], ICE[2]);
            }
         }
      }

      // 5. 落点冲击环与中针目标脚下的环
      g.begin(Style.RING, 4.0F, 0.3F, 0.95F, 0.03F, 0.0F, 0.0F);
      if (t >= DESCEND && t < DESCEND + 6.0F) {
         float k = (t - DESCEND) / 6.0F;
         float radius = (0.5F + 10.0F * this.scale * FxMath.easeOutCubic(k)) / 0.95F;
         Vector3f f = frame.rel(this.far.add(0.0, 0.1, 0.0));
         g.quad(f, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), ICE[0], ICE[1], ICE[2], (1.0F - k) * (1.0F - k));
      }
      for (Needle needle : this.needles) {
         float e = t - (needle.impact + NEEDLE_FALL);
         if (e < 0.0F || e >= 4.0F) {
            continue;
         }
         float k = e / 4.0F;
         float radius = (0.3F + 1.3F * needle.size * FxMath.easeOutCubic(k)) / 0.95F;
         Vector3f f = frame.rel(needle.feet.add(0.0, 0.06, 0.0));
         g.quad(f, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), ICE[0], ICE[1], ICE[2], (1.0F - k) * nearFade(f));
      }

      // 6. 公告板：群星、断天之刃锋尖、落点白闪、中针白星
      g.begin(Style.GLOW, 8.0F, 0.0F, 6.0F, 3.0F, 0.6F, 0.0F);
      for (int i = 0; i < STARS; i++) {
         float alpha = this.starAlpha(i, t, frame.time);
         if (alpha <= 0.0F) {
            continue;
         }
         Vector3f p = frame.rel(this.starPos[i]);
         float flare = 1.0F + 1.2F * FxMath.envelope(FxMath.window(t, this.starOut[i] - 0.6F, this.starOut[i] + 0.4F), 0.5F, 0.5F);
         g.billboard(p, distanceWidth(p, 0.15F, 0.006F) * flare, 0.0F, STAR[0], STAR[1], STAR[2], alpha);
      }
      if (t < DESCEND + 0.5F) {
         float fall = FxMath.easeInCubic(FxMath.window(t, 0.0F, DESCEND));
         Vec3 top = this.far.add(0.0, this.slitHeight + 6.0, 0.0);
         Vector3f head = frame.rel(top.add(0.0, -(this.slitHeight + 6.0) * fall, 0.0));
         g.billboard(head, distanceWidth(head, 1.0F, 0.05F), 0.0F, WHITE[0], WHITE[1], WHITE[2], 1.0F);
      }
      if (t >= DESCEND && t < DESCEND + 4.0F) {
         float k = 1.0F - (t - DESCEND) / 4.0F;
         Vector3f f = frame.rel(this.far.add(0.0, 2.0, 0.0));
         g.billboard(f, 6.0F * this.scale, 0.0F, WHITE[0], WHITE[1], WHITE[2], k * k);
      }
      for (Needle needle : this.needles) {
         float e = t - (needle.impact + NEEDLE_FALL);
         if (e < 0.0F || e >= 3.0F) {
            continue;
         }
         float k = 1.0F - FxMath.window(e, 0.5F, 3.0F);
         float shrink = 1.0F - 0.7F * FxMath.easeInCubic(FxMath.window(e, 0.8F, 3.0F));
         Vector3f p = frame.rel(needle.center);
         g.billboard(p, 1.2F * needle.size * shrink, 0.0F, WHITE[0], WHITE[1], WHITE[2], k * nearFade(p));
         g.billboard(p, 1.2F * needle.size * shrink, (float)(Math.PI * 0.5), ICE[0], ICE[1], ICE[2], k * 0.7F * nearFade(p));
      }
      g.end();
   }

   /** 星辰：渐次浮现、闪烁，在各自的熄灭时刻先亮一下再缩成虚无。 */
   private float starAlpha(int i, float t, float time) {
      float appear = FxMath.window(t, this.starAppear[i], this.starAppear[i] + 1.5F);
      float out = 1.0F - FxMath.window(t, this.starOut[i], this.starOut[i] + 1.5F);
      float twinkle = 0.55F + 0.45F * (float)Math.sin(time * 7.0F + this.starPhase[i]);
      return appear * out * twinkle;
   }

   /** 贴地裂线：沿地形的折线，0 – 2 tick 从脚下冲向远方。 */
   private void groundLine(FxGeometry g, FxFrame frame, float t, float width, float alpha, float[] color, boolean scaleByDistance) {
      if (alpha <= 0.0F) {
         return;
      }
      float reveal = FxMath.easeOutCubic(FxMath.window(t, 0.0F, DESCEND));
      int count = Math.max(2, (int)Math.ceil(GROUND_POINTS * reveal) + 1);
      List<Vector3f> points = new ArrayList<>(count);
      float[] widths = new float[count];
      float[] alphas = new float[count];
      for (int i = 0; i < count; i++) {
         Vector3f p = frame.rel(this.ground[Math.min(GROUND_POINTS, i)]);
         points.add(p);
         widths[i] = scaleByDistance ? distanceWidth(p, width, width * 0.02F) : width;
         alphas[i] = alpha * nearFade(p) * (i == count - 1 && reveal < 1.0F ? 0.3F : 1.0F);
      }
      g.ribbon(points, widths, alphas, color[0], color[1], color[2]);
   }

   private void verticalBeam(FxGeometry g, FxFrame frame, Vec3 top, Vec3 bottom, float baseWidth, float perBlock, float alpha, float[] color) {
      if (alpha <= 0.0F) {
         return;
      }
      int n = 9;
      List<Vector3f> points = new ArrayList<>(n);
      float[] widths = new float[n];
      float[] alphas = new float[n];
      for (int i = 0; i < n; i++) {
         float k = (float)i / (n - 1);
         Vector3f p = frame.rel(top.lerp(bottom, k));
         points.add(p);
         widths[i] = distanceWidth(p, baseWidth * this.scale, perBlock);
         alphas[i] = alpha * (float)Math.pow(Math.sin(Math.PI * Math.max(0.05, k)), 0.3);
      }
      g.ribbon(points, widths, alphas, color[0], color[1], color[2]);
   }

   // ───────────────────────────── 场景通道与屏幕 ─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      if (FxShaders.starlessScene == null) {
         return;
      }
      float t = this.age(frame.partialTick);
      float sky = this.skyDarkness(t);
      float open = open(t) * closeHeight(t);
      if (sky <= 0.001F && open <= 0.001F) {
         return;
      }
      Vec3 slit = this.slitCenter();
      Vector3f c = frame.rel(slit);
      Vector3f acrossUnit = across(c, frame);
      float[] center = frame.screenUv(slit);
      float[] bottom = frame.screenUv(this.far);
      float[] top = frame.screenUv(this.far.add(0.0, this.slitHeight, 0.0));
      float halfWorld = 0.5F * open * this.slitHalfWidth;
      float[] side = frame.screenUv(slit.add(acrossUnit.x * halfWorld, acrossUnit.y * halfWorld, acrossUnit.z * halfWorld));
      float cu = 0.5F;
      float bv = 0.0F;
      float tv = 1.0F;
      float hw = 0.0F;
      float strength = 0.0F;
      if (center != null && bottom != null && top != null && side != null) {
         cu = center[0];
         bv = Math.min(bottom[1], top[1]);
         tv = Math.max(bottom[1], top[1]);
         hw = Math.abs(side[0] - center[0]);
         strength = 1.6F * open;
      }
      float sliceU = cu;
      float sliceB = bv;
      float sliceT = tv;
      float sliceW = hw;
      float push = strength;
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.starlessScene, shader -> {
         shader.safeGetUniform("Slit").set(sliceU, sliceB, sliceT, sliceW);
         shader.safeGetUniform("Params").set(push, sky, sky * 0.35F, 0.0F);
      }));
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float t = this.age(frame.partialTick);
      double distance = frame.camera.distanceTo(this.start);
      if (distance > 48.0) {
         return;
      }
      float near = 1.0F - (float)(distance / 48.0);
      if (t >= DESCEND && t < DESCEND + 4.0F) {
         screen.addAberration(0.5F * (1.0F - (t - DESCEND) / 4.0F) * near);
      }
      float sky = this.skyDarkness(t);
      if (sky > 0.0F) {
         screen.addVignette(0x02040A, 0.35F * sky * near);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float t = this.age(frame.partialTick);
      if (t < DESCEND || t > DESCEND + 4.0F) {
         return 0.0F;
      }
      double distance = frame.camera.distanceTo(this.start);
      return distance > 48.0 ? 0.0F : 0.3F * (1.0F - (t - DESCEND) / 4.0F) * (1.0F - (float)(distance / 48.0));
   }

   private record Needle(Vec3 center, Vec3 feet, float size, float impact) {
   }
}
