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
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * 湮灭之刃 SA「空间破碎」专属渲染（渲染器试点）。
 * <p>
 * 时间轴（tick）：
 * <pre>
 *  0 ─ 6   裂界：施法者→中心的贯穿光束、中心爆闪、色差冲击、镜头震动
 *  0 ─ 12  碎裂展开：体积裂隙网络自中心向外扩张，空间像玻璃一样碎成折射碎片，部分碎片露出虚空星海
 *  2 ─ 26  刃风暴：弧形刀光逐条掠过碎裂球体，伴随裂隙闪电
 *  4 ─ 30  处决：每个目标脚下展开六芒星处决阵与交叉斩痕
 * 30 ─ 44  坍缩：碎片大量化为虚空，中心诞生黑洞并扭曲光线，法阵与光环被吸向中心
 * 44       湮灭：冲击波球壳扩散、全屏闪白 + 径向模糊 + 强泛光、火花四溅（爆炸音效在此刻于客户端播放）
 * 44 ─ 72  余烬消散
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public final class SpatialFractureFx extends FxInstance {
   private static final int LIFETIME = 72;
   private static final int REVEAL_END = 12;
   private static final int STORM_START = 2;
   private static final int STORM_END = 26;
   private static final int COLLAPSE_START = 30;
   private static final int DETONATE = 44;
   private static final int SHOCK_DURATION = 16;
   private static final float MAX_RENDER_RADIUS = 48.0F;

   private static final float[] VOID = {0.62F, 0.28F, 1.0F};
   private static final float[] CYAN = {0.36F, 0.86F, 1.0F};
   private static final float[] GOLD = {1.0F, 0.76F, 0.36F};
   private static final float[] WHITE = {1.0F, 0.97F, 1.0F};

   private final Vec3 center;
   private final float radius;
   private final float scale;
   private final long seed;
   private final float seedFloat;
   private final int casterId;
   private final Vec3 casterOrigin;
   private final double groundY;
   private final int[] targetIds;
   private final Vec3[] targetPositions;
   private final float[] targetSizes;
   private final List<Slash> slashes = new ArrayList<>();
   private final List<List<Vector3f>> bolts = new ArrayList<>();
   private final List<Spark> sparks = new ArrayList<>();
   private final RandomSource random;

   public SpatialFractureFx(ClientLevel level, FxEventPacket packet) {
      super(LIFETIME);
      this.center = packet.origin();
      this.radius = Math.max(3.0F, Math.min(MAX_RENDER_RADIUS, packet.radius()));
      this.scale = Math.max(0.25F, packet.scale());
      this.seed = packet.seed();
      this.seedFloat = (float)((packet.seed() & 0xFFFFL) / 65535.0 * 97.0);
      this.casterId = packet.casterId();
      this.random = RandomSource.create(packet.seed());
      Entity caster = level.getEntity(this.casterId);
      this.casterOrigin = caster != null ? caster.getEyePosition().add(0.0, -0.35, 0.0) : this.center.add(packet.direction().scale(-8.0));
      this.groundY = findGround(level, this.center, caster);
      this.targetIds = packet.entityIds();
      this.targetPositions = new Vec3[this.targetIds.length];
      this.targetSizes = new float[this.targetIds.length];
      for (int i = 0; i < this.targetIds.length; i++) {
         Entity target = level.getEntity(this.targetIds[i]);
         this.targetPositions[i] = target != null ? target.position() : this.center;
         this.targetSizes[i] = target != null ? Math.max(0.9F, target.getBbWidth() * 1.6F) : 1.2F;
      }
      this.buildSlashes(Math.max(0, packet.intParam()));
      this.regenerateBolts();
   }

   private static double findGround(ClientLevel level, Vec3 from, Entity caster) {
      BlockHitResult hit = level.clip(new ClipContext(from, from.subtract(0.0, 24.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
      return hit.getType() == HitResult.Type.MISS ? from.y - 1.0 : hit.getLocation().y;
   }

   private void buildSlashes(int count) {
      int total = Math.min(48, count);
      for (int i = 0; i < total; i++) {
         Vector3f direction = FxMath.randomUnit(this.random);
         Vector3f offset = FxMath.randomUnit(this.random).mul((float)(this.random.nextDouble() * this.radius * 0.6));
         float length = Math.min(this.radius * 1.8F, (float)((12.0 + this.random.nextDouble() * 28.0) * this.scale));
         Vec3 middle = this.center.add(offset.x, offset.y + (1.0 + this.random.nextDouble() * 4.0) * this.scale, offset.z);
         Vec3 half = new Vec3(direction.x, direction.y, direction.z).scale(length * 0.5);
         Vector3f bulgeAxis = FxMath.perpendicular(direction);
         float twist = (float)(this.random.nextDouble() * FxMath.TAU);
         Vector3f bulge = bulgeAxis.rotateAxis(twist, direction.x, direction.y, direction.z).mul(length * 0.17F);
         float spawn = STORM_START + (STORM_END - STORM_START) * (i + (float)this.random.nextDouble() * 0.6F) / Math.max(1, total);
         float width = (float)((0.16 + this.random.nextDouble() * 0.12) * this.scale);
         this.slashes.add(new Slash(middle.subtract(half), middle.add(half), new Vec3(bulge.x, bulge.y, bulge.z), spawn, width, i % 5 == 0));
      }
   }

   private void regenerateBolts() {
      this.bolts.clear();
      int count = 7;
      for (int i = 0; i < count; i++) {
         Vector3f end = FxMath.randomUnit(this.random).mul(this.radius * (0.55F + this.random.nextFloat() * 0.45F));
         end.y = Math.abs(end.y) * 0.6F + 0.5F;
         this.bolts.add(lightning(new Vector3f(0.0F, 1.0F, 0.0F), end, 4, this.random));
      }
   }

   /** 中点位移法生成折线闪电（相对中心的坐标）。 */
   private static List<Vector3f> lightning(Vector3f from, Vector3f to, int depth, RandomSource random) {
      List<Vector3f> points = new ArrayList<>();
      points.add(new Vector3f(from));
      points.add(new Vector3f(to));
      float amplitude = from.distance(to) * 0.16F;
      for (int level = 0; level < depth; level++) {
         List<Vector3f> next = new ArrayList<>(points.size() * 2);
         for (int i = 0; i < points.size() - 1; i++) {
            Vector3f a = points.get(i);
            Vector3f b = points.get(i + 1);
            Vector3f mid = new Vector3f(a).add(b).mul(0.5F);
            Vector3f jitter = FxMath.randomUnit(random).mul(amplitude * (0.5F + random.nextFloat() * 0.5F));
            next.add(a);
            next.add(mid.add(jitter));
         }
         next.add(points.get(points.size() - 1));
         points = next;
         amplitude *= 0.5F;
      }
      return points;
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public double cullDistance() {
      return 160.0 + this.radius * 3.0;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      if (age % 2 == 0 && age < DETONATE) {
         this.regenerateBolts();
      }
      for (Slash slash : this.slashes) {
         if (!slash.sparked && age >= slash.spawn) {
            slash.sparked = true;
            Vec3 mid = slash.point(0.5);
            for (int i = 0; i < 4; i++) {
               this.sparks.add(Spark.burst(mid, this.random, 0.25F * this.scale, 10 + this.random.nextInt(8), CYAN));
            }
         }
      }
      if (age == DETONATE) {
         this.detonate(level);
      }
      Iterator<Spark> iterator = this.sparks.iterator();
      while (iterator.hasNext()) {
         Spark spark = iterator.next();
         spark.tick();
         if (spark.life <= 0) {
            iterator.remove();
         }
      }
   }

   private void detonate(ClientLevel level) {
      float volume = Math.min(4.0F, 1.0F + this.targetIds.length / 20.0F);
      level.playLocalSound(this.center.x, this.center.y, this.center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, volume, 0.35F, false);
      level.playLocalSound(this.center.x, this.center.y, this.center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, volume, 0.65F, false);
      level.playLocalSound(this.center.x, this.center.y, this.center.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, Math.min(2.5F, volume), 1.7F, false);
      level.playLocalSound(this.center.x, this.center.y, this.center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F, false);
      int count = Math.min(160, (int)(90 * this.scale));
      Vec3 core = this.center.add(0.0, 1.0, 0.0);
      for (int i = 0; i < count; i++) {
         float[] color = i % 3 == 0 ? GOLD : (i % 3 == 1 ? CYAN : VOID);
         this.sparks.add(Spark.burst(core, this.random, (0.8F + this.random.nextFloat() * 1.6F) * this.scale, 18 + this.random.nextInt(20), color));
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         level.addParticle(ParticleTypes.FLASH, this.center.x, this.center.y + 1.0, this.center.z, 0.0, 0.0, 0.0);
         ring(level, ParticleTypes.END_ROD, this.center.add(0.0, 0.3, 0.0), this.radius, 48);
         ring(level, ParticleTypes.REVERSE_PORTAL, this.center.add(0.0, 2.4, 0.0), this.radius * 0.6F, 32);
         for (int i = 0; i < 40; i++) {
            Vector3f dir = FxMath.randomUnit(this.random).mul(0.6F);
            level.addParticle(ParticleTypes.PORTAL, this.center.x, this.center.y + 1.0, this.center.z, dir.x, dir.y, dir.z);
         }
      }
      for (Slash slash : this.slashes) {
         if (age == (int)slash.spawn) {
            for (int i = 0; i <= 8; i++) {
               Vec3 p = slash.point(i / 8.0);
               level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0.0, 0.0, 0.0);
            }
         }
      }
      for (int i = 0; i < this.targetIds.length; i++) {
         if (age == this.markStart(i)) {
            ring(level, ParticleTypes.END_ROD, this.targetPos(level, i, 1.0F).add(0.0, 0.1, 0.0), this.targetSizes[i], 16);
         }
      }
      if (age == DETONATE) {
         level.addParticle(ParticleTypes.SONIC_BOOM, this.center.x, this.center.y + 1.0, this.center.z, 0.0, 0.0, 0.0);
         ring(level, ParticleTypes.DRAGON_BREATH, this.center.add(0.0, 1.0, 0.0), this.radius * 0.5F, 60);
      }
   }

   private static void ring(ClientLevel level, ParticleOptions particle, Vec3 center, float radius, int points) {
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         level.addParticle(particle, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius, 0.0, 0.0, 0.0);
      }
   }

   // ───────────────────────────── 时间轴参数 ─────────────────────────────

   private float reveal(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 0.0F, REVEAL_END));
   }

   private float collapse(float a) {
      return FxMath.easeInCubic(FxMath.window(a, COLLAPSE_START, DETONATE));
   }

   /** 整体淡出（碎裂场 / 法阵）。 */
   private float fade(float a) {
      return 1.0F - FxMath.window(a, DETONATE + 2.0F, DETONATE + 12.0F);
   }

   private float shockProgress(float a) {
      return a < DETONATE ? -1.0F : FxMath.window(a, DETONATE, DETONATE + SHOCK_DURATION);
   }

   private float coreRadius(float a) {
      if (a < COLLAPSE_START) {
         return 0.0F;
      }
      if (a < DETONATE) {
         return this.radius * 0.13F * this.collapse(a);
      }
      return this.radius * 0.13F * (1.0F - FxMath.window(a, DETONATE, DETONATE + 2.5F));
   }

   private int markStart(int index) {
      return 4 + (int)(Math.min(index, 24) * 0.75F);
   }

   private Vec3 targetPos(ClientLevel level, int index, float partialTick) {
      Entity entity = level.getEntity(this.targetIds[index]);
      if (entity != null && entity.isAlive()) {
         this.targetPositions[index] = entity.getPosition(partialTick);
      }
      return this.targetPositions[index];
   }

   private Vec3 casterPos(ClientLevel level, float partialTick) {
      Entity caster = level.getEntity(this.casterId);
      return caster != null ? caster.getPosition(partialTick).add(0.0, caster.getEyeHeight() - 0.35, 0.0) : this.casterOrigin;
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float collapse = this.collapse(a);
      float fade = this.fade(a);
      float shrink = 1.0F - 0.65F * collapse;
      Vector3f c = frame.rel(this.center);

      // 1. 施法者 → 中心的裂界贯穿光束
      if (a < 10.0F) {
         float life = 1.0F - a / 10.0F;
         Vector3f from = frame.rel(this.casterPos(frame.level, frame.partialTick));
         Vector3f to = new Vector3f(c).add(0.0F, 1.0F, 0.0F);
         List<Vector3f> line = new ArrayList<>();
         int n = 16;
         float[] widths = new float[n + 1];
         float[] alphas = new float[n + 1];
         for (int i = 0; i <= n; i++) {
            float u = (float)i / n;
            line.add(new Vector3f(from).lerp(to, u));
            widths[i] = (0.25F + 0.55F * (float)Math.sin(Math.PI * u)) * life * this.scale;
            alphas[i] = life;
         }
         g.begin(Style.RIBBON, 3.0F, 0.0F, 3.0F, 4.0F, 6.0F, 0.6F);
         g.ribbon(line, scaled(widths, 2.6F), alphas, VOID[0], VOID[1], VOID[2]);
         g.begin(Style.RIBBON, 9.0F, 0.0F, 9.0F, 7.0F, 10.0F, 0.25F);
         g.ribbon(line, widths, alphas, WHITE[0], WHITE[1], WHITE[2]);
      }

      // 2. 中心辉光（裂界爆闪 / 坍缩核心光晕 / 湮灭白闪）
      Vector3f core = new Vector3f(c).add(0.0F, 1.0F, 0.0F);
      if (a < 8.0F) {
         float t = a / 8.0F;
         g.begin(Style.GLOW, 9.0F, 0.6F, 6.0F, 2.6F, 1.2F, 0.0F);
         g.billboard(core, this.radius * 0.45F * FxMath.easeOutCubic(Math.min(1.0F, a / 2.0F)), 0.0F, WHITE[0], WHITE[1], WHITE[2], 1.0F - t);
         g.begin(Style.GLOW, 2.0F, 0.6F, 3.0F, 2.0F, 0.0F, 0.0F);
         g.billboard(core, this.radius * 0.9F, 0.0F, VOID[0], VOID[1], VOID[2], (1.0F - t) * 0.6F);
      }
      if (a >= COLLAPSE_START && a < DETONATE) {
         float k = this.collapse(a);
         g.begin(Style.GLOW, 2.0F + 5.0F * k, 0.6F, 4.0F, 2.0F, 0.6F, 0.0F);
         g.billboard(core, this.radius * (0.25F + 0.2F * k), a * 0.1F, VOID[0], VOID[1], VOID[2], 0.35F + 0.65F * k);
      }
      if (a >= DETONATE && a < DETONATE + 10.0F) {
         float t = (a - DETONATE) / 10.0F;
         float k = (1.0F - t) * (1.0F - t);
         g.begin(Style.GLOW, 14.0F, 0.6F, 5.0F, 2.4F, 1.5F, 0.0F);
         g.billboard(core, this.radius * (0.6F + 1.2F * t), 0.0F, WHITE[0], WHITE[1], WHITE[2], k);
         g.begin(Style.GLOW, 4.0F, 0.6F, 3.0F, 1.8F, 0.0F, 0.0F);
         g.billboard(core, this.radius * (1.0F + 1.6F * t), 0.0F, GOLD[0], GOLD[1], GOLD[2], (1.0F - t) * 0.8F);
      }
      g.end();

      // 3. 地面主法阵 + 内层处决阵
      float circleIn = FxMath.easeOutCubic(FxMath.window(a, 1.0F, 14.0F));
      float circleAlpha = circleIn * fade;
      if (circleAlpha > 0.0F) {
         Vector3f ground = frame.rel(new Vec3(this.center.x, this.groundY + 0.06, this.center.z));
         float r = this.radius * shrink;
         g.begin(Style.RUNE, 3.4F + 4.0F * collapse, 0.0F, a * 0.035F, this.seedFloat, 1.0F, circleIn);
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), VOID[0], VOID[1], VOID[2], circleAlpha);
         g.begin(Style.RUNE, 4.5F + 4.0F * collapse, 0.0F, -a * 0.06F, this.seedFloat + 3.0F, 0.0F, circleIn);
         g.quad(new Vector3f(ground).add(0.0F, 0.04F, 0.0F), new Vector3f(r * 0.52F, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r * 0.52F), CYAN[0], CYAN[1], CYAN[2], circleAlpha);
         g.end();
      }

      // 4. 悬浮光环（三层水平虚线环）与陀螺仪竖环
      float haloAlpha = FxMath.window(a, 3.0F, 10.0F) * fade;
      if (haloAlpha > 0.0F) {
         float[] heights = {1.2F, 2.6F, 4.2F};
         float[] radii = {0.7F, 0.46F, 0.28F};
         float[][] colors = {VOID, CYAN, GOLD};
         for (int i = 0; i < 3; i++) {
            float r = this.radius * radii[i] * shrink;
            float y = FxMath.lerp(heights[i] * this.scale, 1.0F, collapse);
            float rot = a * (0.05F + 0.03F * i) * (i % 2 == 0 ? 1.0F : -1.0F);
            Vector3f u = new Vector3f((float)Math.cos(rot) * r, 0.0F, (float)Math.sin(rot) * r);
            Vector3f v = new Vector3f(-(float)Math.sin(rot) * r, 0.0F, (float)Math.cos(rot) * r);
            g.begin(Style.RING, 3.0F + 3.0F * collapse, 0.4F, 0.95F, 0.018F, 40.0F + 12.0F * i, 0.4F);
            g.quad(new Vector3f(c).add(0.0F, y, 0.0F), u, v, colors[i][0], colors[i][1], colors[i][2], haloAlpha);
         }
         float gyro = this.radius * 0.85F * shrink;
         for (int i = 0; i < 2; i++) {
            float theta = a * (0.09F + 0.05F * i) + i * 1.5708F;
            Vector3f u = new Vector3f((float)Math.cos(theta) * gyro, 0.0F, (float)Math.sin(theta) * gyro);
            Vector3f v = new Vector3f(0.0F, gyro, 0.0F);
            g.begin(Style.RING, 2.4F + 4.0F * collapse, 0.4F, 0.97F, 0.01F, 0.0F, 0.0F);
            g.quad(new Vector3f(c).add(0.0F, 1.0F, 0.0F), u, v, (i == 0 ? CYAN : VOID)[0], (i == 0 ? CYAN : VOID)[1], (i == 0 ? CYAN : VOID)[2], haloAlpha * 0.85F);
         }
         g.end();
      }

      // 5. 刃风暴：弧形刀光
      for (Slash slash : this.slashes) {
         float t = a - slash.spawn;
         if (t < 0.0F || t > 7.0F) {
            continue;
         }
         float head = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 2.2F));
         float tail = FxMath.easeInCubic(FxMath.window(t, 1.2F, 6.5F));
         if (head - tail < 0.01F) {
            continue;
         }
         float life = 1.0F - FxMath.window(t, 4.0F, 7.0F);
         int n = 24;
         List<Vector3f> points = new ArrayList<>(n + 1);
         float[] widths = new float[n + 1];
         float[] alphas = new float[n + 1];
         for (int i = 0; i <= n; i++) {
            float k = (float)i / n;
            float u = FxMath.lerp(tail, head, k);
            points.add(frame.rel(slash.point(u)));
            float profile = (float)Math.pow(Math.sin(Math.PI * k), 0.75);
            widths[i] = slash.width * profile;
            alphas[i] = life * (0.35F + 0.65F * k);
         }
         float[] tint = slash.golden ? GOLD : CYAN;
         g.begin(Style.RIBBON, 2.2F, 0.3F, 2.5F, 3.0F, 5.0F, 0.8F);
         g.ribbon(points, scaled(widths, 3.2F), alphas, VOID[0], VOID[1], VOID[2]);
         g.begin(Style.RIBBON, 8.0F, 0.3F, 7.0F, 6.0F, 9.0F, 0.3F);
         g.ribbon(points, widths, alphas, tint[0], tint[1], tint[2]);
      }
      g.end();

      // 6. 裂隙闪电
      float boltAlpha = FxMath.window(a, 3.0F, 6.0F) * (1.0F - FxMath.window(a, DETONATE - 2.0F, DETONATE));
      if (boltAlpha > 0.0F) {
         float flicker = 0.55F + 0.45F * (float)Math.abs(Math.sin(a * 5.3F + this.seedFloat));
         for (List<Vector3f> bolt : this.bolts) {
            List<Vector3f> points = new ArrayList<>(bolt.size());
            float[] widths = new float[bolt.size()];
            float[] alphas = new float[bolt.size()];
            for (int i = 0; i < bolt.size(); i++) {
               Vector3f p = bolt.get(i);
               points.add(new Vector3f(p).mul(shrink).add(c));
               float k = (float)i / (bolt.size() - 1);
               widths[i] = (0.09F * (1.0F - k * 0.7F)) * this.scale;
               alphas[i] = boltAlpha * flicker * (1.0F - k * 0.5F);
            }
            g.begin(Style.RIBBON, 2.0F, 0.2F, 2.0F, 9.0F, 14.0F, 0.9F);
            g.ribbon(points, scaled(widths, 4.0F), alphas, VOID[0], VOID[1], VOID[2]);
            g.begin(Style.RIBBON, 7.0F, 0.2F, 9.0F, 12.0F, 20.0F, 0.2F);
            g.ribbon(points, widths, alphas, CYAN[0], CYAN[1], CYAN[2]);
         }
         g.end();
      }

      // 7. 目标处决阵与交叉斩痕
      for (int i = 0; i < this.targetIds.length; i++) {
         float t = a - this.markStart(i);
         if (t < 0.0F || t > 24.0F) {
            continue;
         }
         Vec3 pos = this.targetPos(frame.level, i, frame.partialTick);
         float size = this.targetSizes[i];
         float open = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 4.0F));
         float life = 1.0F - FxMath.window(t, 16.0F, 24.0F);
         Vector3f foot = frame.rel(pos.add(0.0, 0.05, 0.0));
         float rr = size * 1.5F * (0.6F + 0.4F * open);
         g.begin(Style.RUNE, 5.0F, 0.0F, t * 0.12F, this.seedFloat + i * 1.7F, 0.0F, open);
         g.quad(foot, new Vector3f(rr, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, rr), WHITE[0], WHITE[1], WHITE[2], life);
         g.end();
         Vector3f mid = frame.rel(pos.add(0.0, size * 0.7, 0.0));
         float cut = FxMath.easeOutCubic(FxMath.window(t, 0.5F, 3.0F));
         float cutLife = 1.0F - FxMath.window(t, 4.0F, 11.0F);
         if (cutLife > 0.0F) {
            float span = size * 1.6F;
            g.begin(Style.RIBBON, 9.0F, 0.2F, 8.0F, 5.0F, 8.0F, 0.35F);
            for (int k = 0; k < 2; k++) {
               Vector3f dir = k == 0 ? new Vector3f(1.0F, 0.75F, 0.6F) : new Vector3f(-0.6F, 0.75F, 1.0F);
               dir.normalize().mul(span);
               Vector3f from = new Vector3f(mid).sub(dir);
               Vector3f to = new Vector3f(from).lerp(new Vector3f(mid).add(dir), cut);
               List<Vector3f> line = List.of(from, new Vector3f(from).lerp(to, 0.5F), to);
               float w = 0.12F * size;
               g.ribbon(line, new float[]{0.0F, w, w * 0.4F}, new float[]{cutLife, cutLife, cutLife}, WHITE[0], WHITE[1], WHITE[2]);
            }
            g.end();
            g.begin(Style.GLOW, 4.0F, 0.3F, 6.0F, 2.5F, 0.8F, 0.0F);
            g.billboard(mid, size * 1.4F, 0.0F, CYAN[0], CYAN[1], CYAN[2], cutLife * (1.0F - cut * 0.5F));
            g.end();
         }
      }

      // 8. 湮灭冲击波地面环
      float shock = this.shockProgress(a);
      if (shock >= 0.0F && shock < 1.0F) {
         float r = this.radius * 1.8F * FxMath.easeOutCubic(shock);
         Vector3f ground = frame.rel(new Vec3(this.center.x, this.groundY + 0.08, this.center.z));
         g.begin(Style.RING, 6.0F * (1.0F - shock), 0.0F, 0.94F, 0.05F, 0.0F, 0.0F);
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), GOLD[0], GOLD[1], GOLD[2], 1.0F - shock);
         g.end();
      }

      // 9. 火花
      if (!this.sparks.isEmpty()) {
         g.begin(Style.RIBBON, 6.0F, 0.0F, 6.0F, 0.0F, 0.0F, 0.5F);
         for (Spark spark : this.sparks) {
            Vec3 p = spark.position(frame.partialTick);
            Vec3 tail = p.subtract(spark.vx * 2.5, spark.vy * 2.5, spark.vz * 2.5);
            float life = (float)spark.life / spark.maxLife;
            g.streak(frame.rel(p), frame.rel(tail), 0.05F * this.scale, spark.color[0], spark.color[1], spark.color[2], life);
         }
         g.end();
      }
   }

   private static float[] scaled(float[] values, float factor) {
      float[] out = new float[values.length];
      for (int i = 0; i < values.length; i++) {
         out[i] = values[i] * factor;
      }
      return out;
   }

   // ───────────────────────────── B 档全屏通道 ─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      float a = this.age(frame.partialTick);
      float fade = this.fade(a);
      float shock = this.shockProgress(a);
      boolean fieldVisible = fade > 0.001F;
      boolean shockVisible = shock >= 0.0F && shock < 1.0F;
      if (!fieldVisible && !shockVisible) {
         return;
      }
      Vector3f c = frame.rel(this.center.add(0.0, 1.0, 0.0));
      float reveal = this.reveal(a);
      float collapse = this.collapse(a);
      float shockRadius = shockVisible ? this.radius * 1.8F * FxMath.easeOutCubic(shock) : 0.0F;
      float shockStrength = shockVisible ? (1.0F - shock) * (1.0F - shock) : 0.0F;
      float coreRadius = this.coreRadius(a);
      float flash = Math.max(1.0F - a / 6.0F, a >= DETONATE ? 1.0F - (a - DETONATE) / 6.0F : 0.0F);
      float voidRatio = 0.06F + collapse * 0.6F;
      float cellSize = this.radius * 0.19F;
      float fieldFade = fieldVisible ? fade : 0.0F;
      java.util.function.Consumer<ShaderInstance> uniforms = shader -> {
         shader.safeGetUniform("Sphere").set(c.x, c.y, c.z, this.radius);
         shader.safeGetUniform("Phase").set(reveal, collapse, fieldFade, this.seedFloat);
         shader.safeGetUniform("Shock").set(shockRadius, shockStrength, Math.max(0.8F, this.radius * 0.08F), coreRadius);
         shader.safeGetUniform("Extra").set(Math.max(0.0F, flash), voidRatio, cellSize, 0.0F);
      };
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.fractureScene, uniforms));
      out.add(new FxPass(FxPass.Kind.VOLUME, () -> FxShaders.fractureVolume, uniforms));
   }

   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   private float attenuation(FxFrame frame) {
      double distance = frame.camera.distanceTo(this.center);
      return (float)Math.max(0.0, 1.0 - Math.max(0.0, distance - this.radius) / (this.radius * 3.0 + 64.0));
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      float att = this.attenuation(frame);
      if (att <= 0.0F) {
         return;
      }
      float[] uv = frame.screenUv(this.center.add(0.0, 1.0, 0.0));
      if (a < 6.0F) {
         float t = 1.0F - a / 6.0F;
         screen.addFlash(0xD8C4FF, 0.32F * t * t * att);
         screen.addAberration(0.7F * t * att);
         screen.addZoomBlur(0.25F * t * att, uv);
      }
      float collapse = this.collapse(a);
      if (collapse > 0.0F && a < DETONATE + 4.0F) {
         float k = a < DETONATE ? collapse : 1.0F - FxMath.window(a, DETONATE, DETONATE + 4.0F);
         screen.addVignette(0x1A0633, 0.55F * k * att);
         screen.addDesaturate(0.35F * k * att);
         screen.addZoomBlur(0.12F * k * att, uv);
         screen.addAberration(0.35F * k * att);
      }
      if (a >= DETONATE) {
         float t = a - DETONATE;
         float flash = Math.max(0.0F, 1.0F - t / 7.0F);
         screen.addFlash(0xFFF4FF, 0.8F * flash * flash * att);
         screen.addAberration(1.3F * Math.max(0.0F, 1.0F - t / 14.0F) * att);
         screen.addZoomBlur(0.85F * Math.max(0.0F, 1.0F - t / 12.0F) * att, uv);
         screen.addBloomBoost(1.2F * Math.max(0.0F, 1.0F - t / 10.0F) * att);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      float att = this.attenuation(frame);
      float value = 0.0F;
      if (a < 8.0F) {
         value = 0.5F * (1.0F - a / 8.0F);
      }
      if (a >= COLLAPSE_START && a < DETONATE) {
         value = Math.max(value, 0.18F * this.collapse(a));
      }
      if (a >= DETONATE) {
         value = Math.max(value, 1.0F - FxMath.window(a, DETONATE, DETONATE + SHOCK_DURATION));
      }
      return value * att;
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private static final class Slash {
      final Vec3 start;
      final Vec3 end;
      final Vec3 bulge;
      final float spawn;
      final float width;
      final boolean golden;
      boolean sparked;

      Slash(Vec3 start, Vec3 end, Vec3 bulge, float spawn, float width, boolean golden) {
         this.start = start;
         this.end = end;
         this.bulge = bulge;
         this.spawn = spawn;
         this.width = width;
         this.golden = golden;
      }

      /** 二次贝塞尔弧线：中点向 bulge 方向鼓出，形成新月形刀光。 */
      Vec3 point(double u) {
         Vec3 control = this.start.add(this.end).scale(0.5).add(this.bulge.scale(2.0));
         double inv = 1.0 - u;
         return this.start.scale(inv * inv).add(control.scale(2.0 * inv * u)).add(this.end.scale(u * u));
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
      final float[] color;

      private Spark(Vec3 origin, Vector3f velocity, int life, float[] color) {
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

      static Spark burst(Vec3 origin, RandomSource random, float speed, int life, float[] color) {
         return new Spark(origin, FxMath.randomUnit(random).mul(speed), life, color);
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         this.vx *= 0.88;
         this.vy = this.vy * 0.88 - 0.012;
         this.vz *= 0.88;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
