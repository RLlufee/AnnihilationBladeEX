package QWQ.QingYi.annihilationblade.infinity_stellaris.client.fx;

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
 * 无尽星空 SA「真空衰变·坍缩」专属渲染（持续特效试点）。
 * <p>
 * 物理意象：「伪真空衰变」——一个真真空泡在落点成核并以光速扩张，泡内的物理法则被改写。
 * <pre>
 *  0 ─ 14      真空泡成核：落点白闪，虹彩泡壁扩张至整个结界并折射光线
 *  4 ─ 20      结界成形：四面极光帷幕自地面升起，四角湍流光柱贯天，地表刻蚀出扩散脉冲的晶格
 *  6 ─ end     奇点：结界中央诞生黑洞（引力透镜 + 事件视界），吸积盘差速旋转，上下喷出相对论喷流
 *  2 ─ end     伽马雷暴：每 tick 在结界内随机落下伽马雷电（纯客户端绘制，取代原先每 tick 生成的雷电实体）
 *  结界内      冷色去饱和；身处结界中时天空坍缩为虚空星海
 *  end-8 ─ end 崩解：帷幕碎散、奇点暴涨
 *  end ─ +18   湮灭：奇点闪光、冲击波球壳、全屏闪白与径向模糊
 * </pre>
 * end 默认为服务端给出的结界时长；施法者死亡等提前结束时由 STOP 提前触发崩解。
 */
@OnlyIn(Dist.CLIENT)
public final class VacuumDecayFx extends FxInstance {
   private static final int DEFAULT_DURATION = 100;
   private static final int TAIL = 18;
   private static final int OPEN_END = 14;
   private static final int MAX_BOLTS = 160;

   private static final float[] CYAN = {0.36F, 0.86F, 1.0F};
   private static final float[] VIOLET = {0.55F, 0.32F, 1.0F};
   private static final float[] WHITE = {0.95F, 0.97F, 1.0F};
   private static final float[] GOLD = {1.0F, 0.8F, 0.45F};
   private static final float[][] BOLT_COLORS = {CYAN, VIOLET, WHITE, GOLD, CYAN};

   private final Vec3 center;
   private final float half;
   private final float above;
   private final float below;
   private final double groundY;
   private final Vec3 singularity;
   private final float bubbleMax;
   /** 黑洞事件视界的最大半径（崩解时再放大 60%）。 */
   private final float coreMax;
   private final float diskRadius;
   private final float seedFloat;
   private final int boltsPerTick;
   private final RandomSource random;
   private final List<Bolt> bolts = new ArrayList<>();
   private final List<Ember> embers = new ArrayList<>();
   private int endAge = DEFAULT_DURATION;
   private float strike;

   public VacuumDecayFx(ClientLevel level, FxEventPacket packet) {
      super(DEFAULT_DURATION + TAIL);
      this.center = packet.origin();
      this.half = Math.max(8.0F, packet.radius());
      float height = Math.max(8.0F, packet.scale());
      this.above = height * 0.5F;
      this.below = height * 0.5F;
      this.seedFloat = (float)((packet.seed() & 0xFFFFL) / 65535.0 * 97.0);
      this.boltsPerTick = Math.max(0, Math.min(24, packet.intParam()));
      this.random = RandomSource.create(packet.seed());
      BlockHitResult hit = level.clip(new ClipContext(this.center.add(0.0, 2.0, 0.0), this.center.subtract(0.0, 24.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
      this.groundY = hit.getType() == HitResult.Type.MISS ? this.center.y - 0.5 : hit.getLocation().y;
      // 奇点悬于结界顶端之上，压迫整片天空；全部尺寸与结界大小挂钩
      this.coreMax = this.half * 0.14F;
      this.diskRadius = this.half * 0.8F;
      this.singularity = this.center.add(0.0, this.above + this.half * 0.15F, 0.0);
      this.bubbleMax = this.half * 1.45F;
   }

   @Override
   protected void onSustainedStart(int duration) {
      this.endAge = Math.max(1, duration);
      this.lifetime = this.endAge + TAIL;
   }

   @Override
   public void onSustainedStop() {
      if (this.age < this.endAge) {
         this.endAge = this.age + 6;
         this.lifetime = this.endAge + TAIL;
      }
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public double cullDistance() {
      return this.half + 256.0;
   }

   // ───────────────────────────── 时间轴参数 ─────────────────────────────

   private float open(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 0.0F, OPEN_END));
   }

   private float rise(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 4.0F, 20.0F));
   }

   /** 崩解进度（end-8 → end）。 */
   private float collapse(float a) {
      return FxMath.easeInCubic(FxMath.window(a, this.endAge - 8.0F, this.endAge));
   }

   /** 湮灭进度（end → end+TAIL）。 */
   private float aftermath(float a) {
      return FxMath.window(a, this.endAge, this.endAge + TAIL);
   }

   private float zoneAlpha(float a) {
      return 1.0F - FxMath.window(a, this.endAge, this.endAge + 6.0F);
   }

   private float coreRadius(float a) {
      if (a < this.endAge) {
         return this.coreMax * FxMath.easeOutCubic(FxMath.window(a, 6.0F, 24.0F)) * (1.0F + 0.6F * this.collapse(a));
      }
      return this.coreMax * 1.6F * (1.0F - FxMath.window(a, this.endAge, this.endAge + 3.0F));
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      this.strike *= 0.8F;

      Iterator<Bolt> boltIterator = this.bolts.iterator();
      while (boltIterator.hasNext()) {
         Bolt bolt = boltIterator.next();
         bolt.tick(this.random);
         if (bolt.dead()) {
            boltIterator.remove();
         }
      }

      if (age >= 2 && age < this.endAge) {
         Bolt nearest = null;
         double nearestDistance = Double.MAX_VALUE;
         for (int i = 0; i < this.boltsPerTick && this.bolts.size() < MAX_BOLTS; i++) {
            Bolt bolt = this.spawnBolt(level);
            if (bolt == null) {
               continue;
            }
            this.bolts.add(bolt);
            double distance = camera.distanceToSqr(bolt.x, bolt.ground, bolt.z);
            if (distance < nearestDistance) {
               nearestDistance = distance;
               nearest = bolt;
            }
            if (distance < 64.0 * 64.0) {
               for (int k = 0; k < 4; k++) {
                  this.embers.add(Ember.at(new Vec3(bolt.x, bolt.ground + 0.2, bolt.z), this.random, bolt.color));
               }
            }
         }
         if (nearest != null) {
            if (age % 2 == 0) {
               level.playLocalSound(nearest.x, nearest.ground, nearest.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 6.0F, 0.8F + this.random.nextFloat() * 0.2F, false);
            }
            if (nearestDistance < 40.0 * 40.0) {
               level.playLocalSound(nearest.x, nearest.ground, nearest.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0F, 0.5F + this.random.nextFloat() * 0.2F, false);
               this.strike = Math.max(this.strike, (float)(1.0 - Math.sqrt(nearestDistance) / 40.0) * 0.35F);
            }
            if (age % 3 == 0) {
               level.setSkyFlashTime(2);
            }
         }
      }

      if (age == this.endAge) {
         level.playLocalSound(this.singularity.x, this.singularity.y, this.singularity.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 6.0F, 0.5F, false);
         level.playLocalSound(this.singularity.x, this.singularity.y, this.singularity.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0F, 0.5F, false);
         float burst = this.half / 64.0F;
         for (int i = 0; i < 180; i++) {
            float[] color = BOLT_COLORS[i % BOLT_COLORS.length];
            this.embers.add(Ember.burst(this.singularity, this.random, (2.0F + this.random.nextFloat() * 3.5F) * burst, color));
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

   private Bolt spawnBolt(ClientLevel level) {
      double x = this.center.x + (this.random.nextDouble() * 2.0 - 1.0) * this.half;
      double z = this.center.z + (this.random.nextDouble() * 2.0 - 1.0) * this.half;
      int bx = (int)Math.floor(x);
      int bz = (int)Math.floor(z);
      if (!level.hasChunk(bx >> 4, bz >> 4)) {
         return null;
      }
      int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
      double ground = height <= level.getMinBuildHeight() ? this.groundY : height;
      double top = Math.min(level.getMaxBuildHeight(), ground + 70.0 + this.random.nextDouble() * 40.0);
      return new Bolt(x, z, top, ground, this.random.nextLong(), 1 + this.random.nextInt(3), BOLT_COLORS[this.random.nextInt(BOLT_COLORS.length)]);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         level.addParticle(ParticleTypes.FLASH, this.center.x, this.center.y + 1.0, this.center.z, 0.0, 0.0, 0.0);
         for (int i = 0; i < 60; i++) {
            Vector3f dir = FxMath.randomUnit(this.random).mul(1.2F);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, this.center.x, this.center.y + 1.0, this.center.z, dir.x, dir.y, dir.z);
         }
      }
      if (age < this.endAge && age % 4 == 0) {
         double y = this.groundY + 0.6;
         for (int i = 0; i <= 12; i++) {
            double offset = -this.half + this.half * 2.0 * i / 12.0;
            level.addParticle(ParticleTypes.END_ROD, this.center.x + offset, y, this.center.z - this.half, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.END_ROD, this.center.x + offset, y, this.center.z + this.half, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.END_ROD, this.center.x - this.half, y, this.center.z + offset, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.END_ROD, this.center.x + this.half, y, this.center.z + offset, 0.0, 0.0, 0.0);
         }
      }
      Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      for (Bolt bolt : this.bolts) {
         if (bolt.fresh && camera.distanceToSqr(bolt.x, bolt.ground, bolt.z) < 96.0 * 96.0) {
            for (int k = 0; k < 8; k++) {
               level.addParticle(ParticleTypes.ELECTRIC_SPARK, bolt.x, bolt.ground + k * 1.5, bolt.z, 0.0, 0.0, 0.0);
            }
         }
      }
      if (age == this.endAge) {
         level.addParticle(ParticleTypes.SONIC_BOOM, this.singularity.x, this.singularity.y, this.singularity.z, 0.0, 0.0, 0.0);
      }
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float open = this.open(a);
      float rise = this.rise(a);
      float collapse = this.collapse(a);
      float after = this.aftermath(a);
      float alpha = this.zoneAlpha(a);
      double bottom = this.groundY - 2.0;
      double top = this.center.y + this.above;
      if (top < bottom + 4.0) {
         top = bottom + 4.0;
      }
      Vector3f s = frame.rel(this.singularity);

      // 1. 真空泡成核
      if (a < 10.0F) {
         float t = a / 10.0F;
         g.begin(Style.GLOW, 12.0F, 0.6F, 6.0F, 2.4F, 1.6F, 0.0F);
         g.billboard(frame.rel(this.center.add(0.0, 1.0, 0.0)), 18.0F * (0.3F + 0.7F * FxMath.easeOutCubic(Math.min(1.0F, a / 2.0F))), 0.0F, WHITE[0], WHITE[1], WHITE[2], (1.0F - t) * (1.0F - t));
         g.end();
      }
      if (open < 1.0F) {
         float r = this.bubbleMax * open;
         Vector3f ground = frame.rel(new Vec3(this.center.x, this.groundY + 0.08, this.center.z));
         g.begin(Style.RING, 5.0F, 0.0F, 0.95F, 0.03F, 0.0F, 0.0F);
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), CYAN[0], CYAN[1], CYAN[2], 1.0F - open);
         g.end();
      }

      // 2. 四面极光帷幕
      if (rise > 0.0F && alpha > 0.0F) {
         float midY = (float)((bottom + top) * 0.5);
         float halfH = (float)((top - bottom) * 0.5);
         Vector3f vertical = new Vector3f(0.0F, halfH, 0.0F);
         Vector3f alongX = new Vector3f(this.half, 0.0F, 0.0F);
         Vector3f alongZ = new Vector3f(0.0F, 0.0F, this.half);
         Vec3[] wallCenters = {
            new Vec3(this.center.x, midY, this.center.z - this.half),
            new Vec3(this.center.x, midY, this.center.z + this.half),
            new Vec3(this.center.x - this.half, midY, this.center.z),
            new Vec3(this.center.x + this.half, midY, this.center.z)
         };
         for (int i = 0; i < 4; i++) {
            g.begin(Style.CURTAIN, 2.2F + 2.0F * collapse, 1.0F, this.seedFloat + i * 13.0F, 1.0F, rise, collapse);
            g.quad(frame.rel(wallCenters[i]), i < 2 ? alongX : alongZ, vertical, CYAN[0], CYAN[1], CYAN[2], alpha);
         }
         g.end();

         // 顶部边缘能量线
         double edgeY = bottom + (top - bottom) * rise;
         Vec3[] corners = {
            new Vec3(this.center.x - this.half, edgeY, this.center.z - this.half),
            new Vec3(this.center.x + this.half, edgeY, this.center.z - this.half),
            new Vec3(this.center.x + this.half, edgeY, this.center.z + this.half),
            new Vec3(this.center.x - this.half, edgeY, this.center.z + this.half)
         };
         g.begin(Style.BEAM, 4.0F, 0.3F, 24.0F, 1.5F, 0.6F, 0.6F);
         for (int i = 0; i < 4; i++) {
            List<Vector3f> edge = segment(frame, corners[i], corners[(i + 1) % 4], 12);
            g.ribbon(edge, constant(edge.size(), 0.45F), constant(edge.size(), alpha), CYAN[0], CYAN[1], CYAN[2]);
         }
         g.end();

         // 四角湍流光柱
         double pillarTop = bottom + (top + 14.0 - bottom) * rise;
         g.begin(Style.BEAM, 5.0F, 0.6F, 8.0F, 2.0F, 1.0F, 0.6F);
         for (int i = 0; i < 4; i++) {
            Vec3 base = new Vec3(corners[i].x, bottom, corners[i].z);
            List<Vector3f> pillar = segment(frame, base, new Vec3(base.x, pillarTop, base.z), 10);
            float[] widths = new float[pillar.size()];
            for (int k = 0; k < widths.length; k++) {
               widths[k] = FxMath.lerp(1.7F, 0.5F, (float)k / (widths.length - 1)) * (1.0F + 0.5F * collapse);
            }
            g.ribbon(pillar, widths, constant(pillar.size(), alpha), VIOLET[0], VIOLET[1], VIOLET[2]);
         }
         g.end();
      }

      // 3. 奇点：外晕、吸积盘、相对论喷流（事件视界作为遮挡球，挡住其背后的吸积盘与外晕）
      float coreR = this.coreRadius(a);
      if (coreR > 0.05F) {
         g.occluder(s, coreR);
         float diskIn = FxMath.window(a, 8.0F, 26.0F) * alpha;
         float grow = FxMath.easeOutCubic(FxMath.window(a, 6.0F, 26.0F));
         g.begin(Style.GLOW, 2.0F + 4.0F * collapse, 0.8F, 2.5F, 1.4F, 0.6F, 0.0F);
         g.billboard(s, this.half * (0.65F + 0.3F * collapse) * (0.4F + 0.6F * grow), a * 0.03F, VIOLET[0], VIOLET[1], VIOLET[2], 0.75F);
         g.end();
         if (diskIn > 0.0F) {
            float diskR = this.diskRadius * (0.55F + 0.45F * grow) * (1.0F - 0.6F * collapse);
            float tilt = 0.3F;
            float spin = a * 0.003F;
            Vector3f u = new Vector3f((float)Math.cos(spin) * diskR, 0.0F, (float)Math.sin(spin) * diskR).rotateX(tilt);
            Vector3f v = new Vector3f(-(float)Math.sin(spin) * diskR, 0.0F, (float)Math.cos(spin) * diskR).rotateX(tilt);
            g.begin(Style.DISK, 3.5F + 3.0F * collapse, 0.5F, 0.3F, 1.4F, this.seedFloat, 0.6F);
            g.quad(s, u, v, VIOLET[0], VIOLET[1], VIOLET[2], diskIn);
            g.end();
            float jet = FxMath.easeOutCubic(FxMath.window(a, 10.0F, 28.0F)) * alpha;
            if (jet > 0.0F) {
               double ceiling = frame.level.getMaxBuildHeight();
               double upLength = Math.max(8.0, Math.min(150.0, ceiling - this.singularity.y - coreR));
               float jetWidth = coreR * 0.55F;
               List<Vector3f> up = segment(frame, this.singularity.add(0.0, coreR * 0.8, 0.0), this.singularity.add(0.0, coreR * 0.8 + upLength * jet, 0.0), 16);
               List<Vector3f> down = segment(frame, this.singularity.subtract(0.0, coreR * 0.8, 0.0), new Vec3(this.singularity.x, this.groundY, this.singularity.z), 10);
               g.begin(Style.BEAM, 5.0F, 0.8F, 14.0F, 3.0F, 1.0F, 0.5F);
               g.ribbon(up, taper(up.size(), jetWidth, jetWidth * 0.25F), constant(up.size(), jet), CYAN[0], CYAN[1], CYAN[2]);
               g.ribbon(down, taper(down.size(), jetWidth * 0.8F, jetWidth * 0.45F), constant(down.size(), jet * 0.8F), CYAN[0], CYAN[1], CYAN[2]);
               g.end();
            }
         }
         g.clearOccluder();
      }

      // 4. 伽马雷电
      if (!this.bolts.isEmpty()) {
         g.begin(Style.RIBBON, 2.5F, 0.4F, 2.0F, 9.0F, 18.0F, 0.9F);
         for (Bolt bolt : this.bolts) {
            if (bolt.visible()) {
               bolt.draw(g, frame, 4.0F, 0.55F, VIOLET);
            }
         }
         g.begin(Style.RIBBON, 10.0F, 0.4F, 9.0F, 12.0F, 24.0F, 0.2F);
         for (Bolt bolt : this.bolts) {
            if (bolt.visible()) {
               bolt.draw(g, frame, 1.0F, 1.0F, bolt.color);
            }
         }
         g.begin(Style.GLOW, 6.0F, 0.6F, 5.0F, 2.2F, 1.0F, 0.0F);
         for (Bolt bolt : this.bolts) {
            if (bolt.visible()) {
               g.billboard(frame.rel(new Vec3(bolt.x, bolt.ground + 0.6, bolt.z)), 3.5F, 0.0F, bolt.color[0], bolt.color[1], bolt.color[2], 1.0F);
            }
         }
         g.end();
      }

      // 5. 崩解与湮灭
      if (collapse > 0.0F && a < this.endAge) {
         g.occluder(s, coreR);
         g.begin(Style.GLOW, 6.0F + 10.0F * collapse, 0.8F, 5.0F, 2.0F, 1.2F, 0.0F);
         g.billboard(s, this.half * 0.9F * (1.0F - collapse * 0.6F), 0.0F, WHITE[0], WHITE[1], WHITE[2], collapse);
         g.clearOccluder();
      }
      if (after > 0.0F && after < 1.0F) {
         float t = FxMath.window(a, this.endAge, this.endAge + 8.0F);
         if (t < 1.0F) {
            g.begin(Style.GLOW, 16.0F, 0.8F, 5.0F, 2.4F, 1.8F, 0.0F);
            g.billboard(s, this.half * (1.1F + 1.1F * t), 0.0F, WHITE[0], WHITE[1], WHITE[2], (1.0F - t) * (1.0F - t));
         }
         float r = this.half * 1.9F * FxMath.easeOutCubic(after);
         g.begin(Style.RING, 6.0F * (1.0F - after), 0.0F, 0.94F, 0.04F, 0.0F, 0.0F);
         g.quad(frame.rel(new Vec3(this.center.x, this.groundY + 0.1, this.center.z)), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), CYAN[0], CYAN[1], CYAN[2], 1.0F - after);
         g.end();
      }

      // 6. 余烬
      if (!this.embers.isEmpty()) {
         g.begin(Style.RIBBON, 5.0F, 0.0F, 6.0F, 0.0F, 0.0F, 0.5F);
         for (Ember ember : this.embers) {
            Vec3 p = ember.position(frame.partialTick);
            Vec3 tail = p.subtract(ember.vx * 2.5, ember.vy * 2.5, ember.vz * 2.5);
            g.streak(frame.rel(p), frame.rel(tail), 0.07F, ember.color[0], ember.color[1], ember.color[2], (float)ember.life / ember.maxLife);
         }
         g.end();
      }
   }

   private static List<Vector3f> segment(FxFrame frame, Vec3 from, Vec3 to, int pieces) {
      List<Vector3f> points = new ArrayList<>(pieces + 1);
      for (int i = 0; i <= pieces; i++) {
         points.add(frame.rel(from.lerp(to, (double)i / pieces)));
      }
      return points;
   }

   private static float[] constant(int size, float value) {
      float[] out = new float[size];
      java.util.Arrays.fill(out, value);
      return out;
   }

   private static float[] taper(int size, float from, float to) {
      float[] out = new float[size];
      for (int i = 0; i < size; i++) {
         out[i] = FxMath.lerp(from, to, size <= 1 ? 0.0F : (float)i / (size - 1));
      }
      return out;
   }

   // ───────────────────────────── B 档全屏通道 ─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      float a = this.age(frame.partialTick);
      float after = this.aftermath(a);
      if (after >= 1.0F) {
         return;
      }
      Vector3f zone = frame.rel(this.center);
      Vector3f core = frame.rel(this.singularity);
      float strength = FxMath.window(a, 2.0F, 14.0F) * this.zoneAlpha(a);
      float bubbleRadius = Math.max(0.01F, this.bubbleMax * this.open(a));
      float wall = 1.0F - FxMath.window(a, OPEN_END - 4.0F, OPEN_END + 6.0F);
      float collapse = this.collapse(a) * 0.6F + after * 0.4F;
      float coreR = this.coreRadius(a);
      float shockRadius = after > 0.0F ? this.half * 1.9F * FxMath.easeOutCubic(after) : 0.0F;
      float shockThickness = this.half * 0.08F;
      float shockStrength = after > 0.0F ? (1.0F - after) * (1.0F - after) : 0.0F;
      float flash = a >= this.endAge ? Math.max(0.0F, 1.0F - (a - this.endAge) / 8.0F) : 0.0F;
      Consumer<ShaderInstance> uniforms = shader -> {
         shader.safeGetUniform("Zone").set(zone.x, zone.y, zone.z, this.half);
         shader.safeGetUniform("ZoneExtra").set(this.above, this.below, strength, this.seedFloat);
         shader.safeGetUniform("Bubble").set(bubbleRadius, Math.max(0.0F, wall), 1.6F, Math.min(1.0F, collapse));
         shader.safeGetUniform("Core").set(core.x, core.y, core.z, coreR);
         shader.safeGetUniform("Shock").set(shockRadius, shockStrength, shockThickness, flash * flash);
      };
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.vacuumScene, uniforms));
      out.add(new FxPass(FxPass.Kind.VOLUME, () -> FxShaders.vacuumVolume, uniforms));
   }

   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   private boolean cameraInside(FxFrame frame) {
      Vec3 c = frame.camera;
      return Math.abs(c.x - this.center.x) < this.half && Math.abs(c.z - this.center.z) < this.half && c.y > this.center.y - this.below && c.y < this.center.y + this.above;
   }

   private float attenuation(FxFrame frame) {
      double dx = Math.max(0.0, Math.abs(frame.camera.x - this.center.x) - this.half);
      double dz = Math.max(0.0, Math.abs(frame.camera.z - this.center.z) - this.half);
      return (float)Math.max(0.0, 1.0 - Math.sqrt(dx * dx + dz * dz) / 160.0);
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      float att = this.attenuation(frame);
      if (att <= 0.0F) {
         return;
      }
      float[] zoneUv = frame.screenUv(this.center.add(0.0, 1.0, 0.0));
      float[] coreUv = frame.screenUv(this.singularity);
      if (a < 10.0F) {
         float t = 1.0F - a / 10.0F;
         screen.addFlash(0xCFE8FF, 0.5F * t * t * att);
         screen.addAberration(0.9F * t * att);
         screen.addZoomBlur(0.45F * t * att, zoneUv);
      }
      float strength = FxMath.window(a, 2.0F, 14.0F) * this.zoneAlpha(a);
      if (strength > 0.0F && this.cameraInside(frame)) {
         screen.addDesaturate(0.2F * strength);
         screen.addVignette(0x050218, 0.35F * strength);
      }
      float collapse = this.collapse(a);
      if (collapse > 0.0F && a < this.endAge) {
         screen.addAberration(0.6F * collapse * att);
         screen.addZoomBlur(0.25F * collapse * att, coreUv);
      }
      if (a >= this.endAge) {
         float t = a - this.endAge;
         float flash = Math.max(0.0F, 1.0F - t / 7.0F);
         screen.addFlash(0xFFFFFF, 0.75F * flash * flash * att);
         screen.addAberration(1.2F * Math.max(0.0F, 1.0F - t / 14.0F) * att);
         screen.addZoomBlur(0.8F * Math.max(0.0F, 1.0F - t / 12.0F) * att, coreUv);
         screen.addBloomBoost(1.2F * Math.max(0.0F, 1.0F - t / 10.0F) * att);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      float att = this.attenuation(frame);
      float value = 0.0F;
      if (a < 10.0F) {
         value = 0.6F * (1.0F - a / 10.0F);
      }
      if (a < this.endAge && this.cameraInside(frame)) {
         value = Math.max(value, 0.06F + this.strike);
      }
      if (a >= this.endAge) {
         value = Math.max(value, 0.9F * (1.0F - this.aftermath(a)));
      }
      return value * att;
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   /** 伽马雷电：生命周期与原版闪电一致（显示 → 熄灭 → 随机再闪 1~3 次），形状由种子决定。 */
   private static final class Bolt {
      final double x;
      final double z;
      final double top;
      final double ground;
      final float[] color;
      int life = 2;
      int flashes;
      boolean fresh = true;
      private List<Vec3> trunk;
      private List<Vec3> branch;

      Bolt(double x, double z, double top, double ground, long seed, int flashes, float[] color) {
         this.x = x;
         this.z = z;
         this.top = top;
         this.ground = ground;
         this.flashes = flashes;
         this.color = color;
         this.reshape(seed);
      }

      private void reshape(long seed) {
         RandomSource random = RandomSource.create(seed);
         Vec3 from = new Vec3(this.x + (random.nextDouble() - 0.5) * 8.0, this.top, this.z + (random.nextDouble() - 0.5) * 8.0);
         Vec3 to = new Vec3(this.x, this.ground, this.z);
         this.trunk = displace(from, to, 5, (this.top - this.ground) * 0.06, random);
         int index = this.trunk.size() / 4 + random.nextInt(Math.max(1, this.trunk.size() / 3));
         Vec3 start = this.trunk.get(Math.min(index, this.trunk.size() - 2));
         double length = (this.top - this.ground) * (0.2 + random.nextDouble() * 0.15);
         double angle = random.nextDouble() * Math.PI * 2.0;
         Vec3 end = start.add(Math.cos(angle) * length * 0.6, -length, Math.sin(angle) * length * 0.6);
         this.branch = displace(start, end, 3, length * 0.12, random);
      }

      private static List<Vec3> displace(Vec3 from, Vec3 to, int depth, double amplitude, RandomSource random) {
         List<Vec3> points = new ArrayList<>();
         points.add(from);
         points.add(to);
         for (int level = 0; level < depth; level++) {
            List<Vec3> next = new ArrayList<>(points.size() * 2);
            for (int i = 0; i < points.size() - 1; i++) {
               Vec3 a = points.get(i);
               Vec3 b = points.get(i + 1);
               next.add(a);
               next.add(a.add(b).scale(0.5).add((random.nextDouble() - 0.5) * 2.0 * amplitude, 0.0, (random.nextDouble() - 0.5) * 2.0 * amplitude));
            }
            next.add(points.get(points.size() - 1));
            points = next;
            amplitude *= 0.55;
         }
         return points;
      }

      void tick(RandomSource random) {
         this.fresh = false;
         this.life--;
         if (this.life < 0 && this.flashes > 0 && this.life < -random.nextInt(10)) {
            this.flashes--;
            this.life = 1;
            this.reshape(random.nextLong());
         }
      }

      boolean visible() {
         return this.life >= 0;
      }

      boolean dead() {
         return this.life < 0 && this.flashes <= 0;
      }

      void draw(FxGeometry g, FxFrame frame, float widthScale, float alphaScale, float[] tint) {
         drawLine(g, frame, this.trunk, 0.32F * widthScale, alphaScale, tint);
         drawLine(g, frame, this.branch, 0.18F * widthScale, alphaScale * 0.8F, tint);
      }

      private static void drawLine(FxGeometry g, FxFrame frame, List<Vec3> line, float width, float alpha, float[] tint) {
         List<Vector3f> points = new ArrayList<>(line.size());
         float[] widths = new float[line.size()];
         float[] alphas = new float[line.size()];
         for (int i = 0; i < line.size(); i++) {
            points.add(frame.rel(line.get(i)));
            widths[i] = width;
            alphas[i] = alpha;
         }
         g.ribbon(points, widths, alphas, tint[0], tint[1], tint[2]);
      }
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

      private Ember(Vec3 origin, double vx, double vy, double vz, int life, float[] color) {
         this.x = this.px = origin.x;
         this.y = this.py = origin.y;
         this.z = this.pz = origin.z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.life = life;
         this.maxLife = life;
         this.color = color;
      }

      static Ember at(Vec3 origin, RandomSource random, float[] color) {
         Vector3f dir = FxMath.randomUnit(random).mul(0.35F);
         return new Ember(origin, dir.x, Math.abs(dir.y) + 0.25, dir.z, 10 + random.nextInt(10), color);
      }

      static Ember burst(Vec3 origin, RandomSource random, float speed, float[] color) {
         Vector3f dir = FxMath.randomUnit(random).mul(speed);
         return new Ember(origin, dir.x, dir.y, dir.z, 18 + random.nextInt(20), color);
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         this.vx *= 0.9;
         this.vy = this.vy * 0.9 + 0.01;
         this.vz *= 0.9;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
      }
   }
}
