package QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx;

import QWQ.QingYi.annihilationblade.nightfall_dragon.logic.CosmicDescentPattern;
import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxPass;
import QWQ.QingYi.annihilationblade.render.client.core.FxScreenState;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxLightningBolt;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.client.pipeline.FxShaders;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 魔龙夜陨 SA「神陨·宇宙夜陨降临」专属渲染。
 * <pre>
 *  0 ─ 6      天穹撕裂：焦点上空撕开一道金边裂隙，裂口后是虚空星海
 *  0 ─ 10     暗星聚能：焦点处诞生暗星（黑洞），周围旋臂尘埃盘与螺旋引力流把景象卷入；地面展开金色龙纹法阵
 *  6 ─ 44     夜陨星雨：每波流星自裂隙坠落，精确砸在服务端伤害结算点（同种子复现），落地闪光 + 冲击环 + 余烬
 * 30 ─ 38     坍缩前兆：暗星膨胀、漩涡加速、神罚光柱自裂隙贯下
 * 38          暗星坍缩：内爆后爆发——闪光、冲击波球壳、三重环 24 道紫金落雷、全屏闪白
 * 38 ─ +30    裂隙闭合，余烬消散
 * </pre>
 * 施法者中途死亡时服务端发送 STOP：停止后续星雨、不发生坍缩，整体快速淡出。
 */
@OnlyIn(Dist.CLIENT)
public final class CosmicDescentFx extends FxInstance {
   private static final int TAIL = 30;
   private static final int FALL_TICKS = 6;
   private static final int COLLAPSE = CosmicDescentPattern.COLLAPSE_TICK;

   private static final float[] GOLD = {1.0F, 0.72F, 0.28F};
   private static final float[] VIOLET = {0.69F, 0.15F, 1.0F};
   private static final float[] WHITE = {1.0F, 0.96F, 0.88F};

   private final Vec3 focal;
   private final double groundY;
   private final Vec3 riftCenter;
   private final float riftHalfLength;
   private final float riftHalfWidth;
   private final float riftAxisX;
   private final float riftAxisZ;
   private final float explosionRadius;
   private final float vortexRadius;
   private final float starMax;
   private final float seedFloat;
   private final RandomSource random;
   private final List<Meteor> meteors = new ArrayList<>();
   private final List<FxLightningBolt> bolts = new ArrayList<>();
   private final List<Ember> embers = new ArrayList<>();
   private boolean stopped;
   private float stopAge = Float.MAX_VALUE;
   private float strike;

   public CosmicDescentFx(ClientLevel level, FxEventPacket packet) {
      super(CosmicDescentPattern.duration(CosmicDescentPattern.unpackWaves(packet.intParam())) + TAIL);
      this.focal = packet.origin();
      this.explosionRadius = Math.max(8.0F, packet.radius());
      this.vortexRadius = Math.max(8.0F, packet.scale());
      this.seedFloat = (float)((packet.seed() & 0xFFFFL) / 65535.0 * 97.0);
      this.random = RandomSource.create(packet.seed() ^ 0x5DEECE66DL);
      BlockHitResult hit = level.clip(new ClipContext(this.focal.add(0.0, 1.0, 0.0), this.focal.subtract(0.0, 48.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
      this.groundY = hit.getType() == HitResult.Type.MISS ? this.focal.y - 1.0 : hit.getLocation().y;
      this.riftCenter = this.focal.add(0.0, 56.0, 0.0);
      this.riftHalfLength = Math.max(32.0F, Math.min(96.0F, this.explosionRadius));
      this.riftHalfWidth = this.riftHalfLength * 0.2F;
      Vec3 look = packet.direction();
      double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
      // 裂隙长轴与视线水平方向垂直，横贯施法者的视野
      this.riftAxisX = horizontal > 1.0E-4 ? (float)(-look.z / horizontal) : 1.0F;
      this.riftAxisZ = horizontal > 1.0E-4 ? (float)(look.x / horizontal) : 0.0F;
      this.starMax = Math.max(3.5F, this.explosionRadius * 0.12F);
      this.buildMeteors(packet.seed(), CosmicDescentPattern.unpackWaves(packet.intParam()), CosmicDescentPattern.unpackSwords(packet.intParam()));
   }

   /** 用与服务端完全相同的随机序列复现全部落点；起点为裂口内的随机位置。 */
   private void buildMeteors(long seed, int waves, int swordsPerWave) {
      RandomSource pattern = RandomSource.create(seed);
      for (int wave = 0; wave < waves; wave++) {
         for (int sword = 0; sword < swordsPerWave; sword++) {
            Vec3 impact = CosmicDescentPattern.impact(this.focal, wave, sword, swordsPerWave, pattern);
            double along = (this.random.nextDouble() * 2.0 - 1.0) * this.riftHalfLength * 0.7;
            double across = (this.random.nextDouble() * 2.0 - 1.0) * this.riftHalfWidth * 0.4;
            Vec3 start = this.riftCenter.add(this.riftAxisX * along - this.riftAxisZ * across, -1.0, this.riftAxisZ * along + this.riftAxisX * across);
            this.meteors.add(new Meteor(start, impact, CosmicDescentPattern.waveTick(wave), wave % 2 == 0 ? GOLD : VIOLET));
         }
      }
   }

   @Override
   protected void onSustainedStart(int duration) {
      // 时间轴由 CosmicDescentPattern 决定，lifetime 已在构造时设定
   }

   @Override
   public void onSustainedStop() {
      if (!this.stopped && this.age < COLLAPSE) {
         this.stopped = true;
         this.stopAge = this.age;
         this.lifetime = Math.min(this.lifetime, this.age + 14);
      }
   }

   @Override
   public Vec3 focus() {
      return this.focal;
   }

   @Override
   public double cullDistance() {
      return this.explosionRadius + 256.0;
   }

   // ───────────────────────────── 时间轴参数 ─────────────────────────────

   private boolean collapsed(float a) {
      return !this.stopped && a >= COLLAPSE;
   }

   /** 中途停止后的整体淡出。 */
   private float stopFade(float a) {
      return this.stopped ? 1.0F - FxMath.window(a, this.stopAge, this.stopAge + 12.0F) : 1.0F;
   }

   private float riftOpen(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 0.0F, 6.0F)) * (1.0F - FxMath.window(a, COLLAPSE + 2.0F, COLLAPSE + 16.0F)) * this.stopFade(a);
   }

   /** 坍缩前兆（30 → 38）。 */
   private float buildup(float a) {
      return this.stopped ? 0.0F : FxMath.easeInCubic(FxMath.window(a, COLLAPSE - 8.0F, COLLAPSE));
   }

   private float aftermath(float a) {
      return this.collapsed(a) ? FxMath.window(a, COLLAPSE, COLLAPSE + 18.0F) : 0.0F;
   }

   /** 夜幕：释放后迅速入夜，坍缩爆发后缓缓天明。 */
   private float night(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 0.0F, 10.0F)) * (1.0F - FxMath.window(a, COLLAPSE + 6.0F, COLLAPSE + 30.0F)) * this.stopFade(a);
   }

   /** 夜幕的距离衰减：身处爆炸范围附近时完全入夜，远处逐渐恢复。 */
   private float nightAttenuation(FxFrame frame) {
      double distance = frame.camera.distanceTo(this.focal);
      return (float)Math.max(0.0, Math.min(1.0, 1.0 - (distance - this.explosionRadius * 1.5) / (this.explosionRadius * 2.5)));
   }

   private float vortexStrength(float a) {
      return FxMath.window(a, 0.0F, 8.0F) * (1.0F + 0.8F * this.buildup(a)) * (1.0F - FxMath.window(a, COLLAPSE, COLLAPSE + 3.0F)) * this.stopFade(a);
   }

   private float starRadius(float a) {
      float grow = FxMath.easeOutCubic(FxMath.window(a, 1.0F, 10.0F));
      if (a < COLLAPSE || this.stopped) {
         return this.starMax * grow * (1.0F + 0.6F * this.buildup(a)) * this.stopFade(a);
      }
      return this.starMax * 1.6F * (1.0F - FxMath.window(a, COLLAPSE, COLLAPSE + 2.0F));
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      this.strike *= 0.75F;

      if (!this.stopped) {
         for (Meteor meteor : this.meteors) {
            if (meteor.impactTick == age) {
               double distance = camera.distanceTo(meteor.impact);
               if (distance < 48.0) {
                  this.strike = Math.max(this.strike, (float)(1.0 - distance / 48.0) * 0.3F);
                  for (int k = 0; k < 5; k++) {
                     this.embers.add(Ember.burst(meteor.impact.add(0.0, 0.3, 0.0), this.random, 0.35F + this.random.nextFloat() * 0.5F, meteor.color, true));
                  }
               }
            }
         }
      }

      if (age == COLLAPSE && !this.stopped) {
         this.collapse(level, camera);
      }

      Iterator<FxLightningBolt> boltIterator = this.bolts.iterator();
      while (boltIterator.hasNext()) {
         FxLightningBolt bolt = boltIterator.next();
         bolt.tick(this.random);
         if (bolt.dead()) {
            boltIterator.remove();
         }
      }
      Iterator<Ember> emberIterator = this.embers.iterator();
      while (emberIterator.hasNext()) {
         Ember ember = emberIterator.next();
         ember.tick();
         if (ember.life <= 0) {
            emberIterator.remove();
         }
      }
   }

   private void collapse(ClientLevel level, Vec3 camera) {
      for (int i = 0; i < CosmicDescentPattern.COLLAPSE_BOLTS; i++) {
         Vec3 at = CosmicDescentPattern.collapseBolt(this.focal, i);
         int bx = (int)Math.floor(at.x);
         int bz = (int)Math.floor(at.z);
         double ground = this.groundY;
         if (level.hasChunk(bx >> 4, bz >> 4)) {
            int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
            if (height > level.getMinBuildHeight()) {
               ground = height;
            }
         }
         double top = Math.min(level.getMaxBuildHeight(), Math.max(ground, this.riftCenter.y) + 10.0 + this.random.nextDouble() * 20.0);
         this.bolts.add(new FxLightningBolt(at.x, at.z, top, ground, this.random.nextLong(), 1 + this.random.nextInt(2), i % 2 == 0 ? VIOLET : GOLD));
      }
      level.playLocalSound(this.focal.x, this.focal.y, this.focal.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 8.0F, 0.75F, false);
      level.playLocalSound(this.focal.x, this.focal.y, this.focal.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 3.0F, 0.5F, false);
      level.setSkyFlashTime(3);
      for (int i = 0; i < 200; i++) {
         float[] color = i % 3 == 0 ? WHITE : (i % 3 == 1 ? GOLD : VIOLET);
         this.embers.add(Ember.burst(this.focal, this.random, 1.4F + this.random.nextFloat() * 3.0F, color, false));
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         for (int i = 0; i < 80; i++) {
            Vector3f dir = FxMath.randomUnit(this.random).mul(0.9F);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, this.focal.x, this.focal.y, this.focal.z, dir.x, dir.y, dir.z);
         }
      }
      if (!this.stopped) {
         for (Meteor meteor : this.meteors) {
            if (meteor.impactTick == age) {
               level.addParticle(ParticleTypes.FLASH, meteor.impact.x, meteor.impact.y + 0.5, meteor.impact.z, 0.0, 0.0, 0.0);
               for (int k = 0; k < 6; k++) {
                  level.addParticle(ParticleTypes.DRAGON_BREATH, meteor.impact.x, meteor.impact.y + 0.5, meteor.impact.z, (this.random.nextDouble() - 0.5) * 0.3, 0.05, (this.random.nextDouble() - 0.5) * 0.3);
               }
            }
         }
      }
      for (FxLightningBolt bolt : this.bolts) {
         if (bolt.fresh()) {
            for (int k = 0; k < 8; k++) {
               level.addParticle(ParticleTypes.ELECTRIC_SPARK, bolt.x, bolt.ground + k * 1.5, bolt.z, 0.0, 0.0, 0.0);
            }
         }
      }
      if (age == COLLAPSE && !this.stopped) {
         level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.focal.x, this.focal.y + 1.0, this.focal.z, 0.0, 0.0, 0.0);
      }
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float fade = this.stopFade(a);
      float build = this.buildup(a);
      float after = this.aftermath(a);
      float vortex = this.vortexStrength(a);
      Vector3f star = frame.rel(this.focal);
      float starR = this.starRadius(a);

      // 1. 地面金色龙纹法阵 + 紫色内阵
      float circleIn = FxMath.easeOutCubic(FxMath.window(a, 0.0F, 7.0F));
      float circleAlpha = circleIn * fade * (1.0F - FxMath.window(a, COLLAPSE + 4.0F, COLLAPSE + 16.0F));
      if (circleAlpha > 0.0F) {
         Vector3f ground = frame.rel(new Vec3(this.focal.x, this.groundY + 0.06, this.focal.z));
         float outer = this.vortexRadius * 0.4F;
         g.begin(Style.RUNE, 3.2F + 4.0F * build, 0.0F, a * 0.03F, this.seedFloat, 1.0F, circleIn);
         g.quad(ground, new Vector3f(outer, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, outer), GOLD[0], GOLD[1], GOLD[2], circleAlpha);
         float inner = this.vortexRadius * 0.18F;
         g.begin(Style.RUNE, 4.0F + 4.0F * build, 0.0F, -a * 0.07F, this.seedFloat + 5.0F, 0.0F, circleIn);
         g.quad(new Vector3f(ground).add(0.0F, 0.04F, 0.0F), new Vector3f(inner, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, inner), VIOLET[0], VIOLET[1], VIOLET[2], circleAlpha);
         g.end();
      }

      // 1.5 天穹法阵：裂隙正下方展开的巨型金色法阵 + 外圈紫色虚线环（随裂隙开合）
      float riftOpen = this.riftOpen(a);
      if (riftOpen > 0.0F) {
         Vector3f sky = frame.rel(this.riftCenter.subtract(0.0, 2.0, 0.0));
         float skyR = this.riftHalfLength * 0.9F;
         float skyIn = FxMath.easeOutCubic(FxMath.window(a, 2.0F, 12.0F));
         g.begin(Style.RUNE, 3.0F + 4.0F * build, 0.4F, -a * 0.02F, this.seedFloat + 11.0F, 1.0F, skyIn);
         g.quad(sky, new Vector3f(skyR, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, skyR), GOLD[0], GOLD[1], GOLD[2], riftOpen);
         float ringR = skyR * 1.18F;
         g.begin(Style.RING, 3.0F + 3.0F * build, 0.4F, 0.96F, 0.012F, 64.0F, 0.25F);
         g.quad(new Vector3f(sky).add(0.0F, -0.5F, 0.0F), new Vector3f(ringR, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, ringR), VIOLET[0], VIOLET[1], VIOLET[2], riftOpen);
         g.end();
      }

      // 2. 螺旋引力流（对数螺线，向暗星收拢，坍缩前加速）
      if (vortex > 0.0F && starR > 0.05F) {
         float spin = a * (0.12F + 0.25F * build);
         float outerR = this.vortexRadius * 0.45F;
         float innerR = Math.max(starR * 1.5F, 1.0F);
         float ln = (float)Math.log(outerR / innerR);
         g.begin(Style.RIBBON, 2.6F + 3.0F * build, 0.4F, 5.0F, 4.0F, 7.0F, 0.6F);
         for (int arm = 0; arm < 10; arm++) {
            float theta0 = arm * FxMath.TAU / 10.0F + spin;
            float yOffset = (float)Math.sin(arm * 2.3F + this.seedFloat) * this.vortexRadius * 0.06F;
            int n = 28;
            List<Vector3f> points = new ArrayList<>(n + 1);
            float[] widths = new float[n + 1];
            float[] alphas = new float[n + 1];
            for (int i = 0; i <= n; i++) {
               float k = (float)i / n;
               float r = outerR * (float)Math.exp(-k * ln);
               float theta = theta0 + k * FxMath.TAU * 0.9F;
               points.add(new Vector3f(star).add((float)Math.cos(theta) * r, yOffset * (1.0F - k), (float)Math.sin(theta) * r));
               widths[i] = 0.35F * (1.0F - k * 0.6F);
               alphas[i] = vortex * (float)Math.sin(Math.PI * k) * 0.8F;
            }
            float[] tint = arm % 2 == 0 ? GOLD : VIOLET;
            g.ribbon(points, widths, alphas, tint[0], tint[1], tint[2]);
         }
         g.end();
      }

      // 3. 暗星：外晕 + 吸积盘（事件视界作为遮挡球）
      if (starR > 0.05F) {
         g.occluder(star, starR);
         g.begin(Style.GLOW, 2.2F + 4.0F * build, 0.6F, 2.5F, 1.4F, 0.5F, 0.0F);
         g.billboard(star, starR * 7.0F, a * 0.05F, VIOLET[0], VIOLET[1], VIOLET[2], 0.8F * fade);
         float diskR = starR * 4.5F;
         float tilt = 0.35F;
         float rot = a * 0.01F;
         Vector3f u = new Vector3f((float)Math.cos(rot) * diskR, 0.0F, (float)Math.sin(rot) * diskR).rotateX(tilt);
         Vector3f v = new Vector3f(-(float)Math.sin(rot) * diskR, 0.0F, (float)Math.cos(rot) * diskR).rotateX(tilt);
         g.begin(Style.DISK, 3.5F + 4.0F * build, 0.5F, 0.3F, 2.0F, this.seedFloat, 0.6F);
         g.quad(star, u, v, GOLD[0], GOLD[1], GOLD[2], FxMath.window(a, 2.0F, 10.0F) * fade);
         if (build > 0.0F && a < COLLAPSE) {
            g.begin(Style.GLOW, 8.0F + 10.0F * build, 0.6F, 5.0F, 2.0F, 1.2F, 0.0F);
            g.billboard(star, starR * (9.0F - 5.0F * build), 0.0F, WHITE[0], WHITE[1], WHITE[2], build);
         }
         g.clearOccluder();
      }

      // 4. 神罚光柱：坍缩前后自裂隙贯下
      float pillar = FxMath.window(a, COLLAPSE - 6.0F, COLLAPSE) * (1.0F - FxMath.window(a, COLLAPSE + 2.0F, COLLAPSE + 10.0F)) * (this.stopped ? 0.0F : 1.0F);
      if (pillar > 0.0F) {
         List<Vector3f> column = new ArrayList<>();
         for (int i = 0; i <= 12; i++) {
            column.add(frame.rel(this.riftCenter.lerp(new Vec3(this.focal.x, this.groundY, this.focal.z), i / 12.0)));
         }
         float[] widths = new float[column.size()];
         Arrays.fill(widths, this.starMax * 1.4F * pillar);
         float[] alphas = new float[column.size()];
         Arrays.fill(alphas, pillar);
         g.begin(Style.BEAM, 7.0F, 0.6F, 10.0F, 4.0F, 1.0F, 0.7F);
         g.ribbon(column, widths, alphas, GOLD[0], GOLD[1], GOLD[2]);
         g.end();
      }

      // 5. 夜陨星雨
      if (!this.stopped || a < this.stopAge + 1.0F) {
         g.begin(Style.BEAM, 5.0F, 0.4F, 6.0F, 5.0F, 0.8F, 0.6F);
         for (Meteor meteor : this.meteors) {
            float start = meteor.impactTick - FALL_TICKS;
            if (a < start || a >= meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            float p = FxMath.easeInCubic(FxMath.window(a, start, meteor.impactTick));
            Vec3 head = meteor.start.lerp(meteor.impact, p);
            Vec3 dir = meteor.impact.subtract(meteor.start).normalize();
            double trail = Math.min(28.0, meteor.start.distanceTo(head));
            Vec3 tail = head.subtract(dir.scale(trail));
            List<Vector3f> line = List.of(frame.rel(head), frame.rel(head.lerp(tail, 0.25)), frame.rel(head.lerp(tail, 0.6)), frame.rel(tail));
            g.ribbon(line, new float[]{1.6F, 1.1F, 0.5F, 0.05F}, new float[]{1.0F, 0.9F, 0.5F, 0.0F}, meteor.color[0], meteor.color[1], meteor.color[2]);
         }
         g.begin(Style.GLOW, 12.0F, 0.4F, 6.0F, 2.6F, 0.8F, 0.0F);
         for (Meteor meteor : this.meteors) {
            float start = meteor.impactTick - FALL_TICKS;
            if (a < start || a >= meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            float p = FxMath.easeInCubic(FxMath.window(a, start, meteor.impactTick));
            g.billboard(frame.rel(meteor.start.lerp(meteor.impact, p)), 3.0F, 0.0F, WHITE[0], WHITE[1], WHITE[2], 1.0F);
         }
         g.end();
      }

      // 6. 落地闪光与冲击环
      g.begin(Style.GLOW, 8.0F, 0.6F, 5.0F, 2.2F, 1.0F, 0.0F);
      for (Meteor meteor : this.meteors) {
         float t = (a - meteor.impactTick) / 8.0F;
         if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         g.billboard(frame.rel(meteor.impact.add(0.0, 0.8, 0.0)), 4.0F + 9.0F * t, 0.0F, meteor.color[0], meteor.color[1], meteor.color[2], (1.0F - t) * (1.0F - t));
      }
      // 落地冲起的短光柱
      g.begin(Style.BEAM, 6.0F, 0.4F, 6.0F, 4.0F, 0.8F, 0.6F);
      for (Meteor meteor : this.meteors) {
         float t = (a - meteor.impactTick) / 5.0F;
         if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         float height = 6.0F + 14.0F * FxMath.easeOutCubic(t);
         List<Vector3f> column = List.of(frame.rel(meteor.impact), frame.rel(meteor.impact.add(0.0, height * 0.5, 0.0)), frame.rel(meteor.impact.add(0.0, height, 0.0)));
         float w = 1.4F * (1.0F - t);
         g.ribbon(column, new float[]{w, w * 0.7F, w * 0.2F}, new float[]{1.0F - t, (1.0F - t) * 0.8F, 0.0F}, meteor.color[0], meteor.color[1], meteor.color[2]);
      }
      g.begin(Style.RING, 5.0F, 0.0F, 0.94F, 0.05F, 0.0F, 0.0F);
      for (Meteor meteor : this.meteors) {
         float t = (a - meteor.impactTick) / 8.0F;
         if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         float r = 1.0F + 11.0F * FxMath.easeOutCubic(t);
         g.quad(frame.rel(meteor.impact.add(0.0, 0.1, 0.0)), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), meteor.color[0], meteor.color[1], meteor.color[2], 1.0F - t);
      }
      g.end();

      // 7. 坍缩落雷
      if (!this.bolts.isEmpty()) {
         g.begin(Style.RIBBON, 2.5F, 0.4F, 2.0F, 9.0F, 18.0F, 0.9F);
         for (FxLightningBolt bolt : this.bolts) {
            if (bolt.visible()) {
               bolt.draw(g, frame, 4.0F, 0.55F, VIOLET);
            }
         }
         g.begin(Style.RIBBON, 10.0F, 0.4F, 9.0F, 12.0F, 24.0F, 0.2F);
         for (FxLightningBolt bolt : this.bolts) {
            if (bolt.visible()) {
               bolt.draw(g, frame, 1.0F, 1.0F, bolt.color);
            }
         }
         g.end();
      }

      // 8. 坍缩爆发：闪光与地面冲击环
      if (after > 0.0F && after < 1.0F) {
         float t = FxMath.window(a, COLLAPSE, COLLAPSE + 8.0F);
         if (t < 1.0F) {
            g.begin(Style.GLOW, 16.0F, 0.8F, 5.0F, 2.4F, 1.8F, 0.0F);
            g.billboard(star, this.explosionRadius * (0.35F + 0.4F * t), 0.0F, WHITE[0], WHITE[1], WHITE[2], (1.0F - t) * (1.0F - t));
            g.begin(Style.GLOW, 5.0F, 0.8F, 3.0F, 1.6F, 0.0F, 0.0F);
            g.billboard(star, this.explosionRadius * (0.6F + 0.6F * t), 0.0F, GOLD[0], GOLD[1], GOLD[2], 1.0F - t);
         }
         float r = this.explosionRadius * FxMath.easeOutCubic(after);
         Vector3f groundCenter = frame.rel(new Vec3(this.focal.x, this.groundY + 0.1, this.focal.z));
         g.begin(Style.RING, 6.0F * (1.0F - after), 0.0F, 0.94F, 0.04F, 0.0F, 0.0F);
         g.quad(groundCenter, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), GOLD[0], GOLD[1], GOLD[2], 1.0F - after);
         // 第二道冲击环：延迟 4 tick、范围更大的紫色余波（需在 aftermath 窗口 18 tick 内结束）
         float second = FxMath.window(a, COLLAPSE + 4.0F, COLLAPSE + 17.0F);
         if (second > 0.0F && second < 1.0F) {
            float r2 = this.explosionRadius * 1.4F * FxMath.easeOutCubic(second);
            g.begin(Style.RING, 4.0F * (1.0F - second), 0.0F, 0.95F, 0.03F, 0.0F, 0.0F);
            g.quad(new Vector3f(groundCenter).add(0.0F, 0.05F, 0.0F), new Vector3f(r2, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r2), VIOLET[0], VIOLET[1], VIOLET[2], 1.0F - second);
         }
         g.end();
      }

      // 8.5 湮灭光柱：坍缩后自地面直冲天顶
      float annihilation = this.collapsed(a) ? FxMath.window(a, COLLAPSE, COLLAPSE + 2.0F) * (1.0F - FxMath.window(a, COLLAPSE + 6.0F, COLLAPSE + 18.0F)) : 0.0F;
      if (annihilation > 0.0F) {
         double ceiling = Math.min(frame.level.getMaxBuildHeight(), this.riftCenter.y + 120.0);
         List<Vector3f> column = new ArrayList<>();
         for (int i = 0; i <= 16; i++) {
            column.add(frame.rel(new Vec3(this.focal.x, this.groundY + (ceiling - this.groundY) * i / 16.0, this.focal.z)));
         }
         float width = this.explosionRadius * 0.12F * (0.6F + 0.4F * annihilation);
         float[] widths = new float[column.size()];
         float[] alphas = new float[column.size()];
         for (int i = 0; i < widths.length; i++) {
            float k = (float)i / (widths.length - 1);
            widths[i] = width * (1.0F - 0.35F * k);
            alphas[i] = annihilation * (1.0F - 0.5F * k);
         }
         g.begin(Style.BEAM, 3.0F, 0.6F, 12.0F, 5.0F, 1.0F, 0.8F);
         g.ribbon(column, scaled(widths, 2.2F), alphas, GOLD[0], GOLD[1], GOLD[2]);
         g.begin(Style.BEAM, 10.0F, 0.6F, 16.0F, 7.0F, 0.6F, 0.3F);
         g.ribbon(column, widths, alphas, WHITE[0], WHITE[1], WHITE[2]);
         g.end();
      }

      // 9. 余烬
      if (!this.embers.isEmpty()) {
         g.begin(Style.RIBBON, 5.0F, 0.0F, 6.0F, 0.0F, 0.0F, 0.5F);
         for (Ember ember : this.embers) {
            Vec3 p = ember.position(frame.partialTick);
            Vec3 trail = p.subtract(ember.vx * 2.5, ember.vy * 2.5, ember.vz * 2.5);
            g.streak(frame.rel(p), frame.rel(trail), 0.07F, ember.color[0], ember.color[1], ember.color[2], (float)ember.life / ember.maxLife);
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
      float rift = this.riftOpen(a);
      float vortex = this.vortexStrength(a);
      float starR = this.starRadius(a);
      float after = this.aftermath(a);
      float night = this.night(a) * this.nightAttenuation(frame);
      if (rift <= 0.0F && vortex <= 0.0F && starR <= 0.0F && night <= 0.0F && (after <= 0.0F || after >= 1.0F)) {
         return;
      }
      Vector3f star = frame.rel(this.focal);
      Vector3f riftRel = frame.rel(this.riftCenter);
      float shockRadius = after > 0.0F ? this.explosionRadius * FxMath.easeOutCubic(after) : 0.0F;
      float shockStrength = after > 0.0F && after < 1.0F ? (1.0F - after) * (1.0F - after) : 0.0F;
      float shockThickness = this.explosionRadius * 0.06F;
      float flash = this.collapsed(a) ? Math.max(0.0F, 1.0F - (a - COLLAPSE) / 8.0F) : 0.0F;
      float spin = 1.5F + 3.0F * this.buildup(a);
      Consumer<ShaderInstance> uniforms = shader -> {
         shader.safeGetUniform("Star").set(star.x, star.y, star.z, starR);
         shader.safeGetUniform("Rift").set(riftRel.x, riftRel.y, riftRel.z, rift);
         shader.safeGetUniform("RiftShape").set(this.riftAxisX, this.riftAxisZ, this.riftHalfLength, this.riftHalfWidth);
         shader.safeGetUniform("Vortex").set(Math.min(1.5F, vortex), this.vortexRadius * 0.45F, spin, this.seedFloat);
         shader.safeGetUniform("Shock").set(shockRadius, shockStrength, shockThickness, flash * flash);
         shader.safeGetUniform("Night").set(night * 0.85F, night, 0.0F, 0.0F);
      };
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.descentScene, uniforms));
      out.add(new FxPass(FxPass.Kind.VOLUME, () -> FxShaders.descentVolume, uniforms));
   }

   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   private float attenuation(FxFrame frame) {
      double distance = frame.camera.distanceTo(this.focal);
      return (float)Math.max(0.0, 1.0 - Math.max(0.0, distance - this.vortexRadius * 0.5) / 200.0);
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      float att = this.attenuation(frame);
      if (att <= 0.0F) {
         return;
      }
      float[] starUv = frame.screenUv(this.focal);
      if (a < 6.0F) {
         float t = 1.0F - a / 6.0F;
         screen.addFlash(0xB026FF, 0.22F * t * t * att);
         screen.addAberration(0.5F * t * att);
         screen.addZoomBlur(0.3F * t * att, starUv);
      }
      if (this.strike > 0.01F) {
         screen.addAberration(this.strike * 1.5F);
      }
      if (frame.camera.distanceTo(this.focal) < this.vortexRadius * 0.6) {
         screen.addVignette(0x12021E, 0.3F * this.vortexStrength(a));
      }
      float build = this.buildup(a);
      if (build > 0.0F && a < COLLAPSE) {
         screen.addZoomBlur(0.3F * build * att, starUv);
         screen.addDesaturate(0.3F * build * att);
         screen.addVignette(0x12021E, 0.5F * build * att);
      }
      if (build > 0.0F && a < COLLAPSE) {
         // 坍缩前的吸气：视野轻微收缩
         screen.addFovKick(-0.08F * build * att);
      }
      if (this.collapsed(a)) {
         float t = a - COLLAPSE;
         // 冲击帧：坍缩瞬间 3 tick 黑白定格，第二拍反相
         if (t < 3.0F) {
            screen.addImpactFrame(att, t >= 1.0F && t < 2.0F);
         }
         float flash = t < 3.0F ? 0.0F : Math.max(0.0F, 1.0F - (t - 3.0F) / 7.0F);
         screen.addFlash(0xFFF1D6, 0.85F * flash * flash * att);
         screen.addAberration(1.4F * Math.max(0.0F, 1.0F - t / 14.0F) * att);
         screen.addZoomBlur(0.9F * Math.max(0.0F, 1.0F - t / 12.0F) * att, starUv);
         screen.addBloomBoost(1.4F * Math.max(0.0F, 1.0F - t / 10.0F) * att);
         // 爆发：视野猛然扩张后回弹
         float kick = t < 2.0F ? FxMath.easeOutCubic(t / 2.0F) : 1.0F - FxMath.easeInOutSine(FxMath.window(t, 2.0F, 14.0F));
         screen.addFovKick(0.15F * kick * att);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      float value = this.strike;
      if (a < 8.0F) {
         value = Math.max(value, 0.4F * (1.0F - a / 8.0F));
      }
      if (this.collapsed(a)) {
         value = Math.max(value, 0.6F * (1.0F - this.aftermath(a)));
      }
      return value * this.attenuation(frame);
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private record Meteor(Vec3 start, Vec3 impact, int impactTick, float[] color) {
   }

   private static final class Ember {
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

      private Ember(Vec3 origin, Vector3f velocity, int life, float[] color, boolean gravity) {
         this.x = this.px = origin.x;
         this.y = this.py = origin.y;
         this.z = this.pz = origin.z;
         this.vx = velocity.x;
         this.vy = velocity.y;
         this.vz = velocity.z;
         this.life = life;
         this.maxLife = life;
         this.color = color;
         this.gravity = gravity;
      }

      static Ember burst(Vec3 origin, RandomSource random, float speed, float[] color, boolean upward) {
         Vector3f dir = FxMath.randomUnit(random).mul(speed);
         if (upward) {
            dir.y = Math.abs(dir.y) + speed * 0.5F;
         }
         return new Ember(origin, dir, 12 + random.nextInt(18), color, upward);
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         this.vx *= 0.88;
         this.vy = this.vy * 0.88 - (this.gravity ? 0.03 : 0.0);
         this.vz *= 0.88;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
