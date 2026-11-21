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
import java.util.Comparator;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SA「空间破碎」专属渲染（v2 全面重构）。
 * <p>
 * 时间轴（tick）：
 * <pre>
 *  0 ─ 3.5  极粗虚空光柱自施法者身前射向终点并冲击展开双重法阵
 * 2.5 ─ 20  地面水平主阵 + UsefulMagic 风格竖直六芒星阻挡阵（含六星卫星阵与同轴前级光环）同步展开
 *  8 ─ 36   阵内高频交叉斩击与刀痕悬停、局部空间切碎错位、棱镜玻璃碎片浮现
 * 30 ─ 44   坍缩聚能：碎片化作虚空星海，主切痕蓄力切屏、中心诞生理论黑洞并吸引法阵
 * 44        终结裂屏碎裂爆轰：全屏斜切错位弹开 + 3D 棱镜碎片四散飞溅 + 强声光震荡
 * 44 ─ 76   碎片溶解与余烬消散
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public final class SpatialFractureFx extends FxInstance {
   private static final int LIFETIME = 76;
   private static final float BEAM_TRAVEL_END = 3.5F;
   private static final float BEAM_HOLD_END = 20.0F;
   private static final float GATE_OPEN_START = 2.5F;
   private static final float GATE_OPEN_END = 12.0F;
   private static final int REVEAL_END = 14;
   private static final int STORM_START = 8;
   private static final int STORM_END = 35;
   private static final int COLLAPSE_START = 30;
   private static final int DETONATE = 44;
   private static final int SHOCK_DURATION = 16;
   private static final float MAX_RENDER_RADIUS = 48.0F;
   private static final int MAX_SLICES = 8;

   private static final float[] VOID = {0.62F, 0.28F, 1.0F};
   private static final float[] DEEP_VIOLET = {0.26F, 0.08F, 0.48F};
   private static final float[] CYAN = {0.36F, 0.88F, 1.0F};
   private static final float[] GOLD = {1.0F, 0.78F, 0.36F};
   private static final float[] WHITE = {1.0F, 0.97F, 1.0F};

   private final Vec3 center;
   private final float radius;
   private final float scale;
   private final float seedFloat;
   private final int casterId;
   private final Vec3 casterOrigin;
   private final double groundY;
   private final Vec3 beamDir;
   private final Vec3 gateCenter;
   private final float gateRadius;
   private final Vector3f gateRight;
   private final Vector3f gateUp;
   private final Vector3f gateForward;

   private final int[] targetIds;
   private final Vec3[] targetPositions;
   private final float[] targetSizes;

   private final List<Slash> slashes = new ArrayList<>();
   private final List<CutScar> cutScars = new ArrayList<>();
   private final List<ShardFragment> shards = new ArrayList<>();
   private final List<List<Vector3f>> bolts = new ArrayList<>();
   private final List<Spark> sparks = new ArrayList<>();
   private final RandomSource random;

   public SpatialFractureFx(ClientLevel level, FxEventPacket packet) {
      super(LIFETIME);
      this.center = packet.origin();
      this.radius = Math.max(3.0F, Math.min(MAX_RENDER_RADIUS, packet.radius()));
      this.scale = Math.max(0.25F, packet.scale());
      this.seedFloat = (float)((packet.seed() & 0xFFFFL) / 65535.0 * 97.0);
      this.casterId = packet.casterId();
      this.random = RandomSource.create(packet.seed());

      Entity caster = level.getEntity(this.casterId);
      Vec3 rawLook = packet.direction().lengthSqr() > 1.0E-6 ? packet.direction().normalize() : new Vec3(0.0, 0.0, 1.0);
      this.casterOrigin = caster != null ? caster.getEyePosition().add(0.0, -0.35, 0.0) : this.center.add(rawLook.scale(-8.0));
      this.groundY = findGround(level, this.center, caster);

      Vec3 targetCore = this.center.add(0.0, 1.0, 0.0);
      Vec3 ray = targetCore.subtract(this.casterOrigin);
      this.beamDir = ray.lengthSqr() > 0.25 ? ray.normalize() : rawLook;

      // 竖直阻挡阵尺寸与中心高度：确保下半圆不被地面过度掩埋，并略迎向光柱来向
      this.gateRadius = Math.min(this.radius * 0.68F, 12.5F * this.scale);
      double gateY = Math.max(this.center.y + 1.0, this.groundY + this.gateRadius * 0.62);
      double leadBack = Math.min(2.2 * this.scale, Math.max(0.0, ray.length() * 0.16));
      this.gateCenter = new Vec3(this.center.x, gateY, this.center.z).subtract(this.beamDir.scale(leadBack));

      this.gateForward = new Vector3f((float)this.beamDir.x, (float)this.beamDir.y, (float)this.beamDir.z).normalize();
      Vector3f worldUp = Math.abs(this.gateForward.y) > 0.92F ? new Vector3f(0.0F, 0.0F, 1.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
      this.gateRight = new Vector3f(this.gateForward).cross(worldUp).normalize();
      this.gateUp = new Vector3f(this.gateRight).cross(this.gateForward).normalize();

      this.targetIds = packet.entityIds();
      this.targetPositions = new Vec3[this.targetIds.length];
      this.targetSizes = new float[this.targetIds.length];
      for (int i = 0; i < this.targetIds.length; i++) {
         Entity target = level.getEntity(this.targetIds[i]);
         this.targetPositions[i] = target != null ? target.position() : this.center;
         this.targetSizes[i] = target != null ? Math.max(0.9F, target.getBbWidth() * 1.6F) : 1.2F;
      }

      this.buildSlashes(Math.max(0, packet.intParam()));
      this.buildCutScars();
      this.buildShards();
      this.regenerateBolts();
   }

   private static double findGround(ClientLevel level, Vec3 from, Entity caster) {
      BlockHitResult hit = level.clip(new ClipContext(from, from.subtract(0.0, 24.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
      return hit.getType() == HitResult.Type.MISS ? from.y - 1.0 : hit.getLocation().y;
   }

   private void buildSlashes(int count) {
      int total = Math.min(48, Math.max(12, count));
      for (int i = 0; i < total; i++) {
         Vector3f direction = FxMath.randomUnit(this.random);
         Vector3f offset = FxMath.randomUnit(this.random).mul((float)(this.random.nextDouble() * this.radius * 0.55));
         float length = Math.min(this.radius * 1.75F, (float)((10.0 + this.random.nextDouble() * 24.0) * this.scale));
         Vec3 middle = this.center.add(offset.x, offset.y + (1.0 + this.random.nextDouble() * 3.8) * this.scale, offset.z);
         Vec3 half = new Vec3(direction.x, direction.y, direction.z).scale(length * 0.5);
         Vector3f bulgeAxis = FxMath.perpendicular(direction);
         float twist = (float)(this.random.nextDouble() * FxMath.TAU);
         Vector3f bulge = bulgeAxis.rotateAxis(twist, direction.x, direction.y, direction.z).mul(length * 0.16F);
         float spawn = STORM_START + (STORM_END - STORM_START - 6.0F) * (i + (float)this.random.nextDouble() * 0.5F) / Math.max(1, total);
         float width = (float)((0.16 + this.random.nextDouble() * 0.13) * this.scale);
         this.slashes.add(new Slash(middle.subtract(half), middle.add(half), new Vec3(bulge.x, bulge.y, bulge.z), spawn, width, i % 5 == 0));
      }
   }

   /** 生成法阵内的空间斩痕：前 3 条为贯穿域内的主斜劈（终段升级全屏错位切屏）。 */
   private void buildCutScars() {
      Vec3 core = this.center.add(0.0, 1.3 * this.scale, 0.0);
      for (int i = 0; i < 14; i++) {
         boolean major = i < 3;
         Vector3f offset = FxMath.randomUnit(this.random).mul(this.radius * (major ? 0.18F : 0.48F * this.random.nextFloat()));
         Vec3 mid = core.add(offset.x, Math.max(-0.5, offset.y * 0.55), offset.z);
         Vector3f dir;
         if (major) {
            double angle = (i - 1) * 0.72 + (this.random.nextDouble() - 0.5) * 0.25;
            dir = new Vector3f(this.gateRight).mul((float)Math.cos(angle)).add(new Vector3f(this.gateUp).mul((float)Math.sin(angle))).normalize();
         } else {
            dir = FxMath.randomUnit(this.random);
         }
         float halfSpan = this.radius * (major ? 0.92F : 0.42F + 0.35F * this.random.nextFloat());
         Vec3 span = new Vec3(dir.x, dir.y, dir.z).scale(halfSpan);
         float spawn = major ? 18.0F + i * 4.5F : 8.0F + (STORM_END - 12.0F) * (i / 14.0F);
         float slipSign = i % 2 == 0 ? 1.0F : -1.0F;
         this.cutScars.add(new CutScar(mid.subtract(span), mid.add(span), spawn, major, slipSign, major ? 1.35F : 0.75F));
      }
   }

   /** 生成 3D 立体玻璃棱镜碎片（在域内悬停震颤，爆破时四散飞出）。 */
   private void buildShards() {
      Vec3 core = this.center.add(0.0, 1.2 * this.scale, 0.0);
      for (int i = 0; i < 36; i++) {
         double theta = this.random.nextDouble() * FxMath.TAU;
         double elev = (this.random.nextDouble() - 0.28) * 1.4;
         float dist = this.radius * (0.22F + 0.65F * (float)Math.sqrt(this.random.nextDouble()));
         Vec3 pos = core.add(Math.cos(theta) * dist, elev * Math.min(this.radius * 0.5F, 5.0F * this.scale), Math.sin(theta) * dist);
         Vector3f outward = new Vector3f((float)(pos.x - core.x), (float)(pos.y - core.y + 0.6), (float)(pos.z - core.z));
         if (outward.lengthSquared() < 1.0E-4F) {
            outward.set(0.0F, 1.0F, 0.0F);
         } else {
            outward.normalize();
         }
         Vector3f u = FxMath.randomUnit(this.random);
         Vector3f v = FxMath.perpendicular(u).rotateAxis((float)(this.random.nextDouble() * 1.8 - 0.9), u.x, u.y, u.z).normalize();
         float spawn = 8.5F + 24.0F * (i / 36.0F);
         float size = (0.42F + 0.68F * this.random.nextFloat()) * this.scale;
         float speed = (0.28F + 0.55F * this.random.nextFloat()) * this.scale;
         float spin = (this.random.nextFloat() - 0.5F) * 0.35F;
         float[] color = i % 4 == 0 ? WHITE : (i % 2 == 0 ? CYAN : VOID);
         this.shards.add(new ShardFragment(pos, outward, u, v, spawn, size, speed, spin, color));
      }
   }

   private void regenerateBolts() {
      this.bolts.clear();
      for (int i = 0; i < 7; i++) {
         Vector3f end = FxMath.randomUnit(this.random).mul(this.radius * (0.55F + this.random.nextFloat() * 0.45F));
         end.y = Math.abs(end.y) * 0.6F + 0.5F;
         this.bolts.add(lightning(new Vector3f(0.0F, 1.0F, 0.0F), end, 4, this.random));
      }
   }

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
      return 160.0 + this.radius * 3.2;
   }

   @Override
   public boolean hasBody() {
      return true;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 3) {
         // 光柱抵达终点：触发法阵展开声与玻璃共鸣
         level.playLocalSound(this.gateCenter.x, this.gateCenter.y, this.gateCenter.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6F, 0.72F, false);
         level.playLocalSound(this.gateCenter.x, this.gateCenter.y, this.gateCenter.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.1F, 1.35F, false);
      }
      if (age == 10 || age == 20 || age == 30) {
         level.playLocalSound(this.center.x, this.center.y + 1.0, this.center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.35F + age * 0.015F, false);
      }
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
      level.playLocalSound(this.center.x, this.center.y, this.center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.2F, 0.48F, false);
      int count = Math.min(160, (int)(96 * this.scale));
      Vec3 core = this.center.add(0.0, 1.0, 0.0);
      for (int i = 0; i < count; i++) {
         float[] color = i % 3 == 0 ? GOLD : (i % 3 == 1 ? CYAN : VOID);
         this.sparks.add(Spark.burst(core, this.random, (0.85F + this.random.nextFloat() * 1.7F) * this.scale, 18 + this.random.nextInt(20), color));
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         level.addParticle(ParticleTypes.FLASH, this.center.x, this.center.y + 1.0, this.center.z, 0.0, 0.0, 0.0);
         ring(level, ParticleTypes.END_ROD, this.center.add(0.0, 0.3, 0.0), this.radius, 48);
         ring(level, ParticleTypes.REVERSE_PORTAL, this.gateCenter, this.gateRadius * 0.65F, 32);
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

   // ───────────────────────────── 时间轴辅助 ─────────────────────────────

   private float reveal(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 0.0F, REVEAL_END));
   }

   private float collapse(float a) {
      return FxMath.easeInCubic(FxMath.window(a, COLLAPSE_START, DETONATE));
   }

   private float fade(float a) {
      return 1.0F - FxMath.window(a, DETONATE + 2.0F, DETONATE + 13.0F);
   }

   private float shockProgress(float a) {
      return a < DETONATE ? -1.0F : FxMath.window(a, DETONATE, DETONATE + SHOCK_DURATION);
   }

   private float coreRadius(float a) {
      if (a < COLLAPSE_START) {
         return 0.0F;
      }
      if (a < DETONATE) {
         return this.radius * 0.135F * this.collapse(a);
      }
      return this.radius * 0.135F * (1.0F - FxMath.window(a, DETONATE, DETONATE + 2.8F));
   }

   /** 供碎裂全屏着色器驱动棱镜缩放与高光裂边的瞬时切碎强度。 */
   private float shatterCutIntensity(float a) {
      float slashPhase = FxMath.window(a, 7.0F, 13.0F) * (1.0F - FxMath.window(a, DETONATE + 4.0F, DETONATE + 12.0F));
      float peak = a >= 32.0F && a < DETONATE + 6.0F
         ? FxMath.window(a, 32.0F, 38.0F) * (1.0F - FxMath.window(a, DETONATE + 1.0F, DETONATE + 6.0F))
         : 0.0F;
      return Math.min(1.45F, slashPhase * 0.78F + peak * 0.65F);
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

   private Vec3 beamStartWorld(ClientLevel level, float partialTick) {
      Entity caster = level.getEntity(this.casterId);
      Vec3 base = caster != null ? caster.getPosition(partialTick).add(0.0, caster.getEyeHeight() - 0.35, 0.0) : this.casterOrigin;
      Vec3 shifted = base.add(this.beamDir.scale(1.15 * Math.min(this.scale, 1.5F)));
      // 保证起点始终位于终点之前，避免超近距离贴脸施法时向量反转
      double proj = this.gateCenter.subtract(shifted).dot(this.beamDir);
      if (proj < 1.2) {
         return this.gateCenter.subtract(this.beamDir.scale(Math.max(2.0, this.gateCenter.distanceTo(this.casterOrigin))));
      }
      return shifted;
   }

   // ───────────────────────────── Body 暗部实体层 ─────────────────────────────

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float a = this.age(frame.partialTick);

      // 1. 光柱深紫烟雾暗壳（在第一帧光柱到达前后保持清晰体量）
      if (a < BEAM_HOLD_END) {
         float reach = FxMath.easeOutCubic(FxMath.window(a, 0.0F, BEAM_TRAVEL_END));
         float fadeOut = 1.0F - FxMath.window(a, 10.0F, BEAM_HOLD_END);
         if (reach > 0.02F && fadeOut > 0.01F) {
            Vec3 fromW = this.beamStartWorld(frame.level, frame.partialTick);
            Vec3 toW = fromW.lerp(this.gateCenter, reach);
            Vector3f from = frame.rel(fromW);
            Vector3f to = frame.rel(toW);
            float beamR = 1.28F * this.scale * (0.45F + 0.55F * fadeOut);
            b.begin(Style.PLASMA_BEAM, 0.85F, 0.45F, 1.0F, -4.5F, 1.0F - fadeOut, 0.45F);
            b.cylinder(from, to, beamR * 0.72F, beamR * 1.12F, 18, a * 0.05F, DEEP_VIOLET[0], DEEP_VIOLET[1], DEEP_VIOLET[2], fadeOut * 0.85F);
            b.end();
         }
      }

      // 2. 空间斩痕黑芯裂口（令悬停刀痕在亮色场景前也有鲜明暗缝割裂感）
      float scarFade = 1.0F - FxMath.window(a, DETONATE + 3.0F, DETONATE + 10.0F);
      if (scarFade > 0.01F) {
         for (CutScar scar : this.cutScars) {
            float t = a - scar.spawn;
            if (t < 0.0F) {
               continue;
            }
            float open = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 1.8F)) * (scar.major && a >= 34.0F ? 1.45F : 1.0F);
            b.begin(Style.VOID_SLIT, 1.0F, 0.0F, open * 0.58F, 0.65F, 1.0F, 0.0F);
            this.drawCutQuad(b, frame, scar, 0.44F * scar.weight * this.scale, DEEP_VIOLET, scarFade * 0.92F);
            b.end();
         }
      }

      // 3. 3D 棱镜碎裂镜像片暗核
      this.renderShardLayer(b, frame, a, true);
   }

   // ───────────────────────────── HDR 主几何层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float collapse = this.collapse(a);
      float fade = this.fade(a);
      float shrink = 1.0F - 0.65F * collapse;
      Vector3f c = frame.rel(this.center);
      Vector3f core = new Vector3f(c).add(0.0F, 1.0F, 0.0F);

      // 1. 粗虚空光柱（施法者前方 -> 竖直魔法阵中心）
      this.renderBeam(g, frame, a);

      // 2. 竖直六芒星阻挡阵（UsefulMagic 风格双环/反转六角星/六卫星小阵/前级同轴光环）
      this.renderVerticalMagicGate(g, frame, a, collapse, fade, shrink);

      // 3. 地面水平主魔法阵 + 悬浮环 + 陀螺仪
      this.renderHorizontalMagicCircles(g, frame, c, core, a, collapse, fade, shrink);

      // 4. 裂界 / 坍缩 / 爆轰核心辉光
      this.renderCoreGlow(g, core, a, collapse);

      // 5. 刃风暴弧形刀光 & 阵内悬停切碎刀痕
      this.renderSlashesAndCutScars(g, frame, a, fade);

      // 6. 3D 棱镜玻璃碎片棱边发光层
      this.renderShardLayer(g, frame, a, false);

      // 7. 裂隙闪电与目标处决阵
      this.renderLightningAndMarks(g, frame, c, a, shrink);

      // 8. 湮灭冲击波地面环 & 火花
      this.renderShockAndSparks(g, frame, a);
   }

   private void renderBeam(FxGeometry g, FxFrame frame, float a) {
      if (a >= BEAM_HOLD_END) {
         return;
      }
      float reach = FxMath.easeOutCubic(FxMath.window(a, 0.0F, BEAM_TRAVEL_END));
      float fadeOut = 1.0F - FxMath.window(a, 8.0F, BEAM_HOLD_END);
      if (reach <= 0.02F || fadeOut <= 0.01F) {
         return;
      }
      Vec3 fromW = this.beamStartWorld(frame.level, frame.partialTick);
      Vec3 toW = fromW.lerp(this.gateCenter, reach);
      Vector3f from = frame.rel(fromW);
      Vector3f to = frame.rel(toW);
      float rOuter = 1.18F * this.scale * (0.35F + 0.65F * fadeOut);
      float waveFront = reach < 0.99F ? reach : (a * 0.22F) % 1.0F;

      // 外层深紫等离子流光柱
      g.begin(Style.PLASMA_BEAM, 4.5F * fadeOut, 0.3F, 1.0F, -5.5F, 1.0F - fadeOut, waveFront);
      g.cylinder(from, to, rOuter * 0.68F, rOuter * 1.08F, 20, a * 0.14F, VOID[0], VOID[1], VOID[2], fadeOut * 0.9F);
      // 中层苍青高速能量鞘
      g.begin(Style.PLASMA_BEAM, 7.5F * fadeOut, 0.2F, 2.0F, -9.0F, 0.0F, waveFront);
      g.cylinder(from, to, rOuter * 0.42F, rOuter * 0.56F, 16, -a * 0.22F, CYAN[0], CYAN[1], CYAN[2], fadeOut);
      // 最内白热高能光芯
      g.begin(Style.PLASMA_BEAM, 12.0F * fadeOut, 0.15F, 1.0F, -12.0F, 0.0F, 0.5F);
      g.cylinder(from, to, rOuter * 0.18F, rOuter * 0.24F, 12, a * 0.35F, WHITE[0], WHITE[1], WHITE[2], fadeOut);
      g.end();

      // 缠绕光柱的双螺旋光带
      int n = 22;
      for (int s = 0; s < 2; s++) {
         List<Vector3f> pts = new ArrayList<>(n + 1);
         float[] widths = new float[n + 1];
         float[] alphas = new float[n + 1];
         float sign = s == 0 ? 1.0F : -1.0F;
         for (int i = 0; i <= n; i++) {
            float u = (float)i / n;
            float angle = sign * (u * FxMath.TAU * 2.2F - a * 0.45F) + s * 3.1416F;
            float rad = rOuter * (0.78F + 0.35F * u);
            Vector3f offset = new Vector3f(this.gateRight).mul((float)Math.cos(angle) * rad).add(new Vector3f(this.gateUp).mul((float)Math.sin(angle) * rad));
            pts.add(new Vector3f(from).lerp(to, u).add(offset));
            widths[i] = 0.22F * this.scale * (0.4F + 0.6F * (float)Math.sin(Math.PI * u));
            alphas[i] = fadeOut;
         }
         g.begin(Style.RIBBON, 7.0F, 0.2F, 6.0F, 4.0F, 8.0F, 0.4F);
         g.ribbon(pts, widths, alphas, s == 0 ? CYAN[0] : GOLD[0], s == 0 ? CYAN[1] : GOLD[1], s == 0 ? CYAN[2] : GOLD[2]);
      }
      g.end();
   }

   /** 参考 UsefulMagic：垂直于光柱方向的六芒星主阻挡法阵 + 双旋多边形星纹 + 6 个顶点卫星小阵 + 同轴前级光环。 */
   private void renderVerticalMagicGate(FxGeometry g, FxFrame frame, float a, float collapse, float fade, float shrink) {
      float open = FxMath.easeOutCubic(FxMath.window(a, GATE_OPEN_START, GATE_OPEN_END));
      float alpha = open * fade;
      if (alpha <= 0.005F) {
         return;
      }
      Vector3f centerRel = frame.rel(this.gateCenter);
      float r = this.gateRadius * (0.28F + 0.72F * open) * shrink;
      float rot = a * 0.045F;

      // 1. 中心法阵盘面（Rune variant=1 大法阵，与正对施法者方向垂直）
      Vector3f uMain = rotatedBasis(this.gateRight, this.gateUp, rot, r);
      Vector3f vMain = rotatedBasis(this.gateRight, this.gateUp, rot + 1.5707964F, r);
      g.begin(Style.RUNE, 4.2F + 3.8F * collapse, 0.0F, rot * 0.65F, this.seedFloat + 11.0F, 1.0F, open);
      g.quad(centerRel, uMain, vMain, VOID[0], VOID[1], VOID[2], alpha * 0.92F);

      // 2. 双层外框护环（外实环 + 内虚线符文刻度环）
      g.begin(Style.RING, 4.8F + 4.0F * collapse, 0.15F, 0.96F, 0.016F, 0.0F, 0.0F);
      g.quad(centerRel, new Vector3f(this.gateRight).mul(r * 1.04F), new Vector3f(this.gateUp).mul(r * 1.04F), CYAN[0], CYAN[1], CYAN[2], alpha);
      g.begin(Style.RING, 4.2F + 3.5F * collapse, 0.15F, 0.87F, 0.014F, 48.0F, -0.55F);
      g.quad(centerRel, new Vector3f(this.gateRight).mul(r * 1.04F), new Vector3f(this.gateUp).mul(r * 1.04F), GOLD[0], GOLD[1], GOLD[2], alpha * 0.9F);
      g.end();

      // 3. 反向旋转双六边形交织星纹（构成 12 角星骨架，还原 UsefulMagic 几何阵线质感）
      float polyW = Math.max(0.055F, this.gateRadius * 0.016F) * (0.75F + 0.25F * open);
      this.drawPolygonRing(g, centerRel, r * 0.90F, 6, rot, polyW, CYAN, alpha * 0.95F);
      this.drawPolygonRing(g, centerRel, r * 0.78F, 6, -rot * 1.35F + 0.5236F, polyW * 0.88F, WHITE, alpha * 0.9F);
      this.drawPolygonRing(g, centerRel, r * 0.50F, 4, rot * 1.8F, polyW * 0.72F, GOLD, alpha * 0.82F);
      g.end();

      // 4. 六个顶点卫星小六芒星阵（UsefulMagic 经典六方位符文副阵）
      float satDist = r * 0.91F;
      float satR = r * 0.20F;
      for (int i = 0; i < 6; i++) {
         double theta = rot + i * (Math.PI / 3.0);
         Vector3f satPos = new Vector3f(centerRel)
            .add(new Vector3f(this.gateRight).mul((float)Math.cos(theta) * satDist))
            .add(new Vector3f(this.gateUp).mul((float)Math.sin(theta) * satDist))
            .sub(new Vector3f(this.gateForward).mul(0.08F * this.scale));
         Vector3f su = rotatedBasis(this.gateRight, this.gateUp, -a * 0.09F + i, satR);
         Vector3f sv = rotatedBasis(this.gateRight, this.gateUp, -a * 0.09F + i + 1.5707964F, satR);
         g.begin(Style.RUNE, 5.2F + 3.0F * collapse, 0.0F, -a * 0.11F, this.seedFloat + i * 2.3F, 0.0F, open);
         g.quad(satPos, su, sv, i % 2 == 0 ? CYAN[0] : GOLD[0], i % 2 == 0 ? CYAN[1] : GOLD[1], i % 2 == 0 ? CYAN[2] : GOLD[2], alpha * 0.95F);
      }
      g.end();

      // 5. 光柱来向同轴前级小光环（增加纵深立体感）
      float[] offsets = {1.35F * this.scale, 2.75F * this.scale};
      float[] scales = {0.58F, 0.34F};
      for (int i = 0; i < 2; i++) {
         Vector3f ringPos = new Vector3f(centerRel).sub(new Vector3f(this.gateForward).mul(offsets[i] * shrink));
         float rr = r * scales[i];
         float subRot = a * (i == 0 ? -0.08F : 0.12F);
         Vector3f ru = rotatedBasis(this.gateRight, this.gateUp, subRot, rr);
         Vector3f rv = rotatedBasis(this.gateRight, this.gateUp, subRot + 1.5707964F, rr);
         g.begin(Style.RING, 4.4F + 2.5F * collapse, 0.25F, 0.92F, 0.025F, 24.0F + 12.0F * i, i == 0 ? 0.6F : -0.8F);
         g.quad(ringPos, ru, rv, (i == 0 ? CYAN : VOID)[0], (i == 0 ? CYAN : VOID)[1], (i == 0 ? CYAN : VOID)[2], alpha * 0.85F);
      }
      g.end();
   }

   private void renderHorizontalMagicCircles(FxGeometry g, FxFrame frame, Vector3f c, Vector3f core, float a, float collapse, float fade, float shrink) {
      float circleIn = FxMath.easeOutCubic(FxMath.window(a, GATE_OPEN_START, GATE_OPEN_END + 2.0F));
      float circleAlpha = circleIn * fade;
      if (circleAlpha > 0.0F) {
         Vector3f ground = frame.rel(new Vec3(this.center.x, this.groundY + 0.08, this.center.z));
         float r = this.radius * shrink;
         g.begin(Style.RUNE, 3.6F + 4.0F * collapse, 0.0F, a * 0.035F, this.seedFloat, 1.0F, circleIn);
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), VOID[0], VOID[1], VOID[2], circleAlpha);
         g.begin(Style.RUNE, 4.8F + 4.0F * collapse, 0.0F, -a * 0.06F, this.seedFloat + 3.0F, 0.0F, circleIn);
         g.quad(new Vector3f(ground).add(0.0F, 0.04F, 0.0F), new Vector3f(r * 0.56F, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r * 0.56F), CYAN[0], CYAN[1], CYAN[2], circleAlpha);
         g.end();

         // 地面外圈虚线环 + 旋转六角阵线，呼应竖直阵
         float polyW = Math.max(0.06F, this.radius * 0.012F);
         this.drawHorizontalPolygon(g, new Vector3f(ground).add(0.0F, 0.07F, 0.0F), r * 0.94F, 6, a * 0.025F, polyW, GOLD, circleAlpha * 0.72F);
         g.end();
      }

      float haloAlpha = FxMath.window(a, 4.0F, 11.0F) * fade;
      if (haloAlpha > 0.0F) {
         float[] heights = {1.2F, 2.7F, 4.4F};
         float[] radii = {0.72F, 0.48F, 0.28F};
         float[][] colors = {VOID, CYAN, GOLD};
         for (int i = 0; i < 3; i++) {
            float rr = this.radius * radii[i] * shrink;
            float y = FxMath.lerp(heights[i] * this.scale, 1.0F, collapse);
            float rot = a * (0.05F + 0.03F * i) * (i % 2 == 0 ? 1.0F : -1.0F);
            Vector3f u = new Vector3f((float)Math.cos(rot) * rr, 0.0F, (float)Math.sin(rot) * rr);
            Vector3f v = new Vector3f(-(float)Math.sin(rot) * rr, 0.0F, (float)Math.cos(rot) * rr);
            g.begin(Style.RING, 3.0F + 3.0F * collapse, 0.4F, 0.95F, 0.018F, 40.0F + 12.0F * i, 0.4F);
            g.quad(new Vector3f(c).add(0.0F, y, 0.0F), u, v, colors[i][0], colors[i][1], colors[i][2], haloAlpha);
         }
         float gyro = this.radius * 0.85F * shrink;
         for (int i = 0; i < 2; i++) {
            float theta = a * (0.09F + 0.05F * i) + i * 1.5708F;
            Vector3f u = new Vector3f((float)Math.cos(theta) * gyro, 0.0F, (float)Math.sin(theta) * gyro);
            Vector3f v = new Vector3f(0.0F, gyro, 0.0F);
            g.begin(Style.RING, 2.4F + 4.0F * collapse, 0.4F, 0.97F, 0.01F, 0.0F, 0.0F);
            g.quad(core, u, v, (i == 0 ? CYAN : VOID)[0], (i == 0 ? CYAN : VOID)[1], (i == 0 ? CYAN : VOID)[2], haloAlpha * 0.85F);
         }
         g.end();
      }
   }

   private void renderCoreGlow(FxGeometry g, Vector3f core, float a, float collapse) {
      if (a < 10.0F) {
         float t = FxMath.window(a, 1.5F, 10.0F);
         float in = FxMath.easeOutCubic(FxMath.window(a, 0.0F, 3.5F));
         g.begin(Style.GLOW, 9.5F, 0.6F, 6.0F, 2.6F, 1.2F, 0.0F);
         g.billboard(core, this.radius * 0.46F * in, 0.0F, WHITE[0], WHITE[1], WHITE[2], (1.0F - t) * in);
         g.begin(Style.GLOW, 2.6F, 0.6F, 3.0F, 2.0F, 0.0F, 0.0F);
         g.billboard(core, this.radius * 0.92F * in, 0.0F, VOID[0], VOID[1], VOID[2], (1.0F - t) * 0.65F);
      }
      if (a >= COLLAPSE_START && a < DETONATE) {
         g.begin(Style.GLOW, 2.2F + 5.5F * collapse, 0.6F, 4.0F, 2.0F, 0.6F, 0.0F);
         g.billboard(core, this.radius * (0.25F + 0.22F * collapse), a * 0.1F, VOID[0], VOID[1], VOID[2], 0.35F + 0.65F * collapse);
      }
      if (a >= DETONATE && a < DETONATE + 11.0F) {
         float t = (a - DETONATE) / 11.0F;
         float k = (1.0F - t) * (1.0F - t);
         g.begin(Style.GLOW, 15.0F, 0.6F, 5.0F, 2.4F, 1.5F, 0.0F);
         g.billboard(core, this.radius * (0.65F + 1.25F * t), 0.0F, WHITE[0], WHITE[1], WHITE[2], k);
         g.begin(Style.GLOW, 4.4F, 0.6F, 3.0F, 1.8F, 0.0F, 0.0F);
         g.billboard(core, this.radius * (1.05F + 1.65F * t), 0.0F, GOLD[0], GOLD[1], GOLD[2], (1.0F - t) * 0.85F);
      }
      g.end();
   }

   private void renderSlashesAndCutScars(FxGeometry g, FxFrame frame, float a, float fade) {
      // 弧形高速斩击丝带
      for (Slash slash : this.slashes) {
         float t = a - slash.spawn;
         if (t < 0.0F || t > 7.5F) {
            continue;
         }
         float head = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 2.2F));
         float tail = FxMath.easeInCubic(FxMath.window(t, 1.2F, 6.8F));
         if (head - tail < 0.01F) {
            continue;
         }
         float life = 1.0F - FxMath.window(t, 4.0F, 7.5F);
         int n = 24;
         List<Vector3f> points = new ArrayList<>(n + 1);
         float[] widths = new float[n + 1];
         float[] alphas = new float[n + 1];
         for (int i = 0; i <= n; i++) {
            float k = (float)i / n;
            float u = FxMath.lerp(tail, head, k);
            points.add(frame.rel(slash.point(u)));
            widths[i] = slash.width * (float)Math.pow(Math.sin(Math.PI * k), 0.75);
            alphas[i] = life * (0.35F + 0.65F * k);
         }
         float[] tint = slash.golden ? GOLD : CYAN;
         g.begin(Style.RIBBON, 2.4F, 0.3F, 2.5F, 3.0F, 5.0F, 0.8F);
         g.ribbon(points, scaled(widths, 3.2F), alphas, VOID[0], VOID[1], VOID[2]);
         g.begin(Style.RIBBON, 8.5F, 0.3F, 7.0F, 6.0F, 9.0F, 0.3F);
         g.ribbon(points, widths, alphas, tint[0], tint[1], tint[2]);
      }
      g.end();

      // 阵内悬停空间刀痕（细亮白青裂刃，直到终结爆碎后收拢消散）
      float scarFade = 1.0F - FxMath.window(a, DETONATE + 3.0F, DETONATE + 10.0F);
      if (scarFade > 0.01F) {
         for (CutScar scar : this.cutScars) {
            float t = a - scar.spawn;
            if (t < 0.0F) {
               continue;
            }
            float open = FxMath.easeOutCubic(FxMath.window(t, 0.0F, 1.8F)) * (scar.major && a >= 34.0F ? 1.45F : 1.0F);
            float pulse = scar.major && a >= 34.0F ? 1.35F : 1.0F;
            float[] tint = scar.major ? WHITE : CYAN;
            g.begin(Style.VOID_SLIT, 6.5F * pulse, 0.0F, open * 0.55F, 0.65F, 1.0F, 0.2F);
            this.drawCutQuad(g, frame, scar, 0.42F * scar.weight * this.scale, tint, scarFade * fade);
            g.end();
         }
      }
   }

   /** 绘制 3D 碎裂镜面棱镜碎片（body=暗沉观测核，geometry=发光晶格边与溶解灼烧）。 */
   private void renderShardLayer(FxGeometry g, FxFrame frame, float a, boolean bodyPass) {
      float shatterAlpha = 1.0F - FxMath.window(a, DETONATE + 14.0F, LIFETIME);
      if (shatterAlpha <= 0.005F) {
         return;
      }
      for (ShardFragment shard : this.shards) {
         if (a < shard.spawn) {
            continue;
         }
         float appear = FxMath.easeOutCubic(FxMath.window(a, shard.spawn, shard.spawn + 3.5F));
         float post = Math.max(0.0F, a - DETONATE);
         float dissolve = post > 10.0F ? FxMath.window(post, 10.0F, 26.0F) : 0.0F;
         if (dissolve >= 0.98F) {
            continue;
         }
         // 爆裂前随坍缩微微向心震颤；爆裂后向外飞溅
         float burst = post > 0.0F ? post * (1.0F - Math.min(0.65F, post * 0.015F)) * shard.speed : 0.0F;
         float tremor = post <= 0.0F ? (float)Math.sin(a * 0.9F + shard.spawn) * 0.08F * this.scale : 0.0F;
         Vec3 worldPos = shard.origin
            .add(shard.outward.x * (burst + tremor), shard.outward.y * (burst + tremor) - 0.012F * post * post * 0.1F, shard.outward.z * (burst + tremor));
         Vector3f rel = frame.rel(worldPos);
         float angle = (a - shard.spawn) * shard.spin * (post > 0.0F ? 2.4F : 0.35F);
         float cs = (float)Math.cos(angle);
         float sn = (float)Math.sin(angle);
         // 先做平面刚体旋转，再构造固定倾斜角度的菱形玻璃轮廓，避免旋转时形状拉伸
         Vector3f ru = new Vector3f(shard.axisU).mul(cs).add(new Vector3f(shard.axisV).mul(sn));
         Vector3f rv = new Vector3f(shard.axisV).mul(cs).sub(new Vector3f(shard.axisU).mul(sn));
         Vector3f u = new Vector3f(ru).mul(shard.size * appear);
         Vector3f v = new Vector3f(ru).mul(0.42F).add(new Vector3f(rv).mul(1.35F)).mul(shard.size * appear);
         float alpha = appear * shatterAlpha * (1.0F - dissolve);
         if (bodyPass) {
            g.begin(Style.AFTERIMAGE, 0.9F, 0.0F, 1.4F, 0.72F, 0.0F, dissolve);
            g.quad(rel, u, v, DEEP_VIOLET[0], DEEP_VIOLET[1], DEEP_VIOLET[2], alpha * 0.88F);
         } else {
            g.begin(Style.AFTERIMAGE, post > 0.0F && post < 6.0F ? 7.2F : 4.2F, 0.0F, 1.6F, 0.14F, 4.0F, dissolve);
            g.quad(rel, u, v, shard.color[0], shard.color[1], shard.color[2], alpha);
         }
      }
      g.end();
   }

   private void renderLightningAndMarks(FxGeometry g, FxFrame frame, Vector3f c, float a, float shrink) {
      float boltAlpha = FxMath.window(a, 3.0F, 6.0F) * (1.0F - FxMath.window(a, DETONATE - 2.0F, DETONATE));
      if (boltAlpha > 0.0F) {
         float flicker = 0.55F + 0.45F * (float)Math.abs(Math.sin(a * 5.3F + this.seedFloat));
         for (List<Vector3f> bolt : this.bolts) {
            List<Vector3f> points = new ArrayList<>(bolt.size());
            float[] widths = new float[bolt.size()];
            float[] alphas = new float[bolt.size()];
            for (int i = 0; i < bolt.size(); i++) {
               points.add(new Vector3f(bolt.get(i)).mul(shrink).add(c));
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
   }

   private void renderShockAndSparks(FxGeometry g, FxFrame frame, float a) {
      float shock = this.shockProgress(a);
      if (shock >= 0.0F && shock < 1.0F) {
         float r = this.radius * 1.8F * FxMath.easeOutCubic(shock);
         Vector3f ground = frame.rel(new Vec3(this.center.x, this.groundY + 0.08, this.center.z));
         g.begin(Style.RING, 6.2F * (1.0F - shock), 0.0F, 0.94F, 0.05F, 0.0F, 0.0F);
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), GOLD[0], GOLD[1], GOLD[2], 1.0F - shock);
         g.end();
      }
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

   private void drawCutQuad(FxGeometry g, FxFrame frame, CutScar scar, float halfWidth, float[] color, float alpha) {
      Vec3 midW = scar.a.lerp(scar.b, 0.5);
      Vector3f relA = frame.rel(scar.a);
      Vector3f relB = frame.rel(scar.b);
      Vector3f relMid = frame.rel(midW);
      Vector3f axisV = new Vector3f(relB).sub(relA).mul(0.5F);
      Vector3f toCam = new Vector3f(relMid).negate();
      if (toCam.lengthSquared() < 1.0E-5F) {
         toCam.set(0.0F, 0.0F, 1.0F);
      }
      Vector3f cross = new Vector3f(axisV).cross(toCam);
      if (cross.lengthSquared() < 1.0E-5F) {
         return;
      }
      Vector3f axisU = cross.normalize().mul(halfWidth);
      g.quad(relMid, axisU, axisV, color[0], color[1], color[2], alpha);
   }

   private void drawPolygonRing(FxGeometry g, Vector3f center, float r, int sides, float rot, float width, float[] color, float alpha) {
      List<Vector3f> pts = new ArrayList<>(sides + 1);
      float[] widths = new float[sides + 1];
      float[] alphas = new float[sides + 1];
      for (int i = 0; i <= sides; i++) {
         double theta = rot + (double)(i % sides) / sides * FxMath.TAU;
         pts.add(new Vector3f(center)
            .add(new Vector3f(this.gateRight).mul((float)Math.cos(theta) * r))
            .add(new Vector3f(this.gateUp).mul((float)Math.sin(theta) * r)));
         widths[i] = width;
         alphas[i] = alpha;
      }
      g.begin(Style.RIBBON, 5.2F, 0.0F, 7.0F, 0.0F, 0.0F, 0.5F);
      g.ribbon(pts, widths, alphas, color[0], color[1], color[2]);
   }

   private void drawHorizontalPolygon(FxGeometry g, Vector3f center, float r, int sides, float rot, float width, float[] color, float alpha) {
      List<Vector3f> pts = new ArrayList<>(sides + 1);
      float[] widths = new float[sides + 1];
      float[] alphas = new float[sides + 1];
      for (int i = 0; i <= sides; i++) {
         double theta = rot + (double)(i % sides) / sides * FxMath.TAU;
         pts.add(new Vector3f(center.x + (float)Math.cos(theta) * r, center.y, center.z + (float)Math.sin(theta) * r));
         widths[i] = width;
         alphas[i] = alpha;
      }
      g.begin(Style.RIBBON, 4.2F, 0.0F, 7.0F, 0.0F, 0.0F, 0.5F);
      g.ribbon(pts, widths, alphas, color[0], color[1], color[2]);
   }

   private static Vector3f rotatedBasis(Vector3f right, Vector3f up, float angle, float radius) {
      return new Vector3f(right).mul((float)Math.cos(angle) * radius).add(new Vector3f(up).mul((float)Math.sin(angle) * radius));
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

      // 通道 1：法阵域内局部空间碎裂与棱镜倍镜畸变 + 体积裂隙光网 + 事件视界
      Vector3f c = frame.rel(this.center.add(0.0, 1.0, 0.0));
      float reveal = this.reveal(a);
      float collapse = this.collapse(a);
      float shockRadius = shockVisible ? this.radius * 1.8F * FxMath.easeOutCubic(shock) : 0.0F;
      float shockStrength = shockVisible ? (1.0F - shock) * (1.0F - shock) : 0.0F;
      float coreRadius = this.coreRadius(a);
      float flash = Math.max(1.0F - a / 6.0F, a >= DETONATE ? 1.0F - (a - DETONATE) / 6.0F : 0.0F);
      float voidRatio = 0.06F + collapse * 0.62F;
      float cutIntensity = this.shatterCutIntensity(a);
      float cellSize = this.radius * (cutIntensity > 0.1F ? 0.145F : 0.185F);
      float fieldFade = fieldVisible ? fade : 0.0F;
      java.util.function.Consumer<ShaderInstance> fractureUniforms = shader -> {
         shader.safeGetUniform("Sphere").set(c.x, c.y, c.z, this.radius);
         shader.safeGetUniform("Phase").set(reveal, collapse, fieldFade, this.seedFloat);
         shader.safeGetUniform("Shock").set(shockRadius, shockStrength, Math.max(0.8F, this.radius * 0.08F), coreRadius);
         shader.safeGetUniform("Extra").set(Math.max(0.0F, flash), voidRatio, cellSize, cutIntensity);
      };
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.fractureScene, fractureUniforms));
      out.add(new FxPass(FxPass.Kind.VOLUME, () -> FxShaders.fractureVolume, fractureUniforms));

      // 通道 2：空间斩痕局部割裂 + 爆轰阶段全屏错位切屏（双切碎效果合一）
      this.collectSlicePass(frame, out, a);
   }

   private void collectSlicePass(FxFrame frame, List<FxPass> out, float a) {
      if (FxShaders.sliceScene == null) {
         return;
      }
      float att = this.attenuation(frame);
      if (att <= 0.02F) {
         return;
      }
      List<SliceLine> lines = new ArrayList<>();
      boolean fullScreenPhase = a >= 35.0F && a < DETONATE + 8.0F;
      if (fullScreenPhase) {
         float slip;
         float seam;
         if (a < DETONATE) {
            float build = FxMath.easeInCubic(FxMath.window(a, 35.0F, DETONATE));
            slip = 0.028F * build;
            seam = 0.72F * build;
         } else {
            float snap = 1.0F - FxMath.window(a, DETONATE, DETONATE + 7.5F);
            slip = 0.058F * snap * snap;
            seam = 1.05F * snap;
         }
         for (CutScar scar : this.cutScars) {
            if (scar.major && a >= scar.spawn) {
               addSlice(frame, lines, scar, slip * scar.slipSign * att, 0.0036F, 1.0F, seam * att);
            }
         }
      }
      if (a >= STORM_START && a < DETONATE + 2.0F) {
         for (CutScar scar : this.cutScars) {
            if (scar.major && fullScreenPhase) {
               continue;
            }
            float t = a - scar.spawn;
            if (t < 0.0F || t > 18.0F) {
               continue;
            }
            float pulse = t < 3.5F ? 1.0F - t / 3.5F * 0.45F : (1.0F - FxMath.window(t, 3.5F, 18.0F)) * 0.55F;
            addSlice(frame, lines, scar, 0.018F * pulse * scar.slipSign * att, 0.0028F, 0.0F, 0.7F * pulse * att);
         }
      }
      if (lines.isEmpty()) {
         return;
      }
      lines.sort(Comparator.comparingDouble((SliceLine l) -> l.score).reversed());
      int count = Math.min(MAX_SLICES, lines.size());
      float[][] packed = new float[4][16];
      for (int i = 0; i < count; i++) {
         SliceLine line = lines.get(i);
         float[] m = packed[i / 2];
         int base = (i % 2) * 8;
         m[base] = line.ax;
         m[base + 1] = line.ay;
         m[base + 2] = line.bx;
         m[base + 3] = line.by;
         m[base + 4] = line.slip;
         m[base + 5] = line.width;
         m[base + 6] = line.mode;
         m[base + 7] = line.seam;
      }
      Matrix4f cutsA = new Matrix4f().set(packed[0]);
      Matrix4f cutsB = new Matrix4f().set(packed[1]);
      Matrix4f cutsC = new Matrix4f().set(packed[2]);
      Matrix4f cutsD = new Matrix4f().set(packed[3]);
      float gap = fullScreenPhase ? 1.0F : 0.65F;
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.sliceScene, shader -> {
         shader.safeGetUniform("CutsA").set(cutsA);
         shader.safeGetUniform("CutsB").set(cutsB);
         shader.safeGetUniform("CutsC").set(cutsC);
         shader.safeGetUniform("CutsD").set(cutsD);
         shader.safeGetUniform("Slice").set((float)count, gap, 1.0F, 0.0F);
      }));
   }

   private static void addSlice(FxFrame frame, List<SliceLine> lines, CutScar scar, float slip, float width, float mode, float seam) {
      float[] uvA = frame.screenUv(scar.a);
      float[] uvB = frame.screenUv(scar.b);
      if (uvA == null || uvB == null) {
         return;
      }
      float dx = uvB[0] - uvA[0];
      float dy = uvB[1] - uvA[1];
      float score = (float)Math.sqrt(dx * dx + dy * dy);
      if (score < 0.015F) {
         return;
      }
      float influence = Math.min(1.0F, score * 3.8F);
      lines.add(new SliceLine(uvA[0], uvA[1], uvB[0], uvB[1], slip * influence, width, mode, seam * influence, score));
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
         screen.addFlash(0xD8C4FF, 0.34F * t * t * att);
         screen.addAberration(0.75F * t * att);
         screen.addZoomBlur(0.28F * t * att, uv);
      }
      float collapse = this.collapse(a);
      if (collapse > 0.0F && a < DETONATE + 4.0F) {
         float k = a < DETONATE ? collapse : 1.0F - FxMath.window(a, DETONATE, DETONATE + 4.0F);
         screen.addVignette(0x1A0633, 0.55F * k * att);
         screen.addDesaturate(0.45F * k * att);
         screen.addZoomBlur(0.14F * k * att, uv);
         screen.addAberration(0.45F * k * att);
      }
      if (a >= DETONATE) {
         float t = a - DETONATE;
         float flash = Math.max(0.0F, 1.0F - t / 7.5F);
         screen.addFlash(0xFFF4FF, 0.82F * flash * flash * att);
         screen.addAberration(1.35F * Math.max(0.0F, 1.0F - t / 14.0F) * att);
         screen.addZoomBlur(0.9F * Math.max(0.0F, 1.0F - t / 12.0F) * att, uv);
         screen.addBloomBoost(1.25F * Math.max(0.0F, 1.0F - t / 10.0F) * att);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      float att = this.attenuation(frame);
      float value = 0.0F;
      if (a < 8.0F) {
         value = 0.52F * (1.0F - a / 8.0F);
      }
      if (a >= COLLAPSE_START && a < DETONATE) {
         value = Math.max(value, 0.22F * this.collapse(a));
      }
      if (a >= DETONATE) {
         value = Math.max(value, 1.0F - FxMath.window(a, DETONATE, DETONATE + SHOCK_DURATION));
      }
      return value * att;
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private record SliceLine(float ax, float ay, float bx, float by, float slip, float width, float mode, float seam, float score) {
   }

   private static final class CutScar {
      final Vec3 a;
      final Vec3 b;
      final float spawn;
      final boolean major;
      final float slipSign;
      final float weight;

      CutScar(Vec3 a, Vec3 b, float spawn, boolean major, float slipSign, float weight) {
         this.a = a;
         this.b = b;
         this.spawn = spawn;
         this.major = major;
         this.slipSign = slipSign;
         this.weight = weight;
      }
   }

   private static final class ShardFragment {
      final Vec3 origin;
      final Vector3f outward;
      final Vector3f axisU;
      final Vector3f axisV;
      final float spawn;
      final float size;
      final float speed;
      final float spin;
      final float[] color;

      ShardFragment(Vec3 origin, Vector3f outward, Vector3f axisU, Vector3f axisV, float spawn, float size, float speed, float spin, float[] color) {
         this.origin = origin;
         this.outward = outward;
         this.axisU = axisU;
         this.axisV = axisV;
         this.spawn = spawn;
         this.size = size;
         this.speed = speed;
         this.spin = spin;
         this.color = color;
      }
   }

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
