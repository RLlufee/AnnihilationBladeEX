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
 * 魔龙夜陨终极 SA「神陨·宇宙夜陨降临」专属渲染（导演级三层空间构图版）。
 * <p>
 * 空间三层垂直分离（避免所有特效挤在地面焦点糊成一团）：
 * <ul>
 *   <li><b>天穹层（groundY + 60）</b>：虚空裂隙 + 三重交错旋转的 3D 立体浑天仪星环（Armillary Rings）；</li>
 *   <li><b>中空神域层（groundY + 18）</b>：悬空日蚀暗星（双层沸腾球壳 + 纤细立体星轨）+ 横贯视野的 3D 魔龙星穹光翼 + 自天穹贯落的 48 格 3D 终焉神陨巨剑；</li>
 *   <li><b>地面舞台层（groundY）</b>：克制底纹龙阵 + 主次分级的斜向星雨（主星陨带 3D 再入壳并在弹坑留下插地光剑碑；伴随星矢纤细凌厉不遮挡视野）。</li>
 * </ul>
 * <pre>
 *  0 ─ 8      第一幕·天幕初裂：裂隙与三重浑天仪展开，悬空日蚀暗星凝聚，大地能量沿 6 道细流向上抽吸
 *  6 ─ 32     第二幕·夜陨剑雨：斜向星雨倾泻，每波主星陨砸地化作插地星陨剑碑（坍缩前剑碑能量归宗）
 * 22 ─ 38     第三幕·真灵降世：悬空暗星两侧缓缓张开巨型 3D 魔龙光翼；第 30 tick 起 3D 神陨巨剑穿透浑天仪压下
 * 38          第四幕·神陨贯地：巨剑刺穿焦点 → 2 tick 反相定格剪影 → 过曝颗粒爆闪 + 速度线 + 3D 三层湮灭天柱 + 24 道外围落雷
 * 38 ─ +30    第五幕·星晶余韵：3D 旋转星晶与余烬飘散，夜幕缓缓退去
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public final class CosmicDescentFx extends FxInstance {
   private static final int TAIL = 30;
   private static final int FALL_TICKS = 6;
   private static final int COLLAPSE = CosmicDescentPattern.COLLAPSE_TICK;

   private static final float[] GOLD = {1.0F, 0.75F, 0.30F};
   private static final float[] VIOLET = {0.66F, 0.16F, 1.0F};
   private static final float[] DEEP_ABYSS = {0.08F, 0.02F, 0.18F};
   private static final float[] WHITE = {1.0F, 0.96F, 0.88F};

   private final Vec3 focal;
   private final double groundY;
   /** 悬空暗星/日蚀中心（抬升至高空 18 格，为地面留出清透舞台）。 */
   private final Vec3 starCenter;
   private final Vec3 riftCenter;
   private final float riftHalfLength;
   private final float riftHalfWidth;
   /** 横贯施法者视野的水平轴（用于裂隙长轴与魔龙双翼展开方向）。 */
   private final float riftAxisX;
   private final float riftAxisZ;
   /** 面向施法者的水平法向（与 riftAxis 垂直）。 */
   private final float lookAxisX;
   private final float lookAxisZ;
   private final float explosionRadius;
   private final float vortexRadius;
   private final float starMax;
   private final float seedFloat;
   private final RandomSource random;
   private final List<Meteor> meteors = new ArrayList<>();
   private final List<FxLightningBolt> bolts = new ArrayList<>();
   private final List<Ember> embers = new ArrayList<>();
   private final List<StarShard> shards = new ArrayList<>();
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
      BlockHitResult hit = level.clip(new ClipContext(this.focal.add(0.0, 2.0, 0.0), this.focal.subtract(0.0, 48.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
      this.groundY = hit.getType() == HitResult.Type.MISS ? this.focal.y - 1.0 : hit.getLocation().y;
      // 三层立体分区：地面 groundY / 中空暗星 +18 / 天穹裂隙 +60
      this.starCenter = new Vec3(this.focal.x, this.groundY + 18.0, this.focal.z);
      this.riftCenter = new Vec3(this.focal.x, this.groundY + 60.0, this.focal.z);
      this.riftHalfLength = Math.max(34.0F, Math.min(88.0F, this.explosionRadius * 0.95F));
      this.riftHalfWidth = this.riftHalfLength * 0.18F;
      Vec3 look = packet.direction();
      double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
      this.lookAxisX = horizontal > 1.0E-4 ? (float)(look.x / horizontal) : 0.0F;
      this.lookAxisZ = horizontal > 1.0E-4 ? (float)(look.z / horizontal) : 1.0F;
      this.riftAxisX = -this.lookAxisZ;
      this.riftAxisZ = this.lookAxisX;
      // 克制暗星半径（不超过 5.2 格），保留精致轮廓而不是糊住半边天
      this.starMax = Math.max(2.6F, Math.min(5.2F, this.explosionRadius * 0.068F));
      this.buildMeteors(packet.seed(), CosmicDescentPattern.unpackWaves(packet.intParam()), CosmicDescentPattern.unpackSwords(packet.intParam()));
   }

   /**
    * 用与服务端完全相同的随机序列复现全部落点。
    * 视觉上做主次分级（每波仅第 0 颗为主星陨并留下插地光剑碑，其余为纤细星矢），
    * 且统一带约 18° 斜向天幕切入角度，彻底告别垂直白墙拥挤感。
    */
   private void buildMeteors(long seed, int waves, int swordsPerWave) {
      RandomSource pattern = RandomSource.create(seed);
      for (int wave = 0; wave < waves; wave++) {
         int tick = CosmicDescentPattern.waveTick(wave);
         for (int sword = 0; sword < swordsPerWave; sword++) {
            Vec3 impact = CosmicDescentPattern.impact(this.focal, wave, sword, swordsPerWave, pattern);
            Vec3 groundImpact = new Vec3(impact.x, this.groundY + 0.08, impact.z);
            // 统一斜向切入：所有流星沿天穹裂口斜劈而下，动势一致、视觉极其通透
            double slantX = this.riftAxisX * (16.0 + (this.random.nextDouble() - 0.5) * 14.0) - this.lookAxisX * 9.0;
            double slantZ = this.riftAxisZ * (16.0 + (this.random.nextDouble() - 0.5) * 14.0) - this.lookAxisZ * 9.0;
            Vec3 start = new Vec3(groundImpact.x + slantX, this.riftCenter.y - 1.5, groundImpact.z + slantZ);
            boolean hero = sword == (wave % Math.max(1, swordsPerWave)) && wave % 2 == 0 && tick <= COLLAPSE - 4;
            boolean monolith = hero && groundImpact.distanceToSqr(this.focal.x, this.groundY + 0.08, this.focal.z) > 18.0;
            float tiltX = (this.random.nextFloat() - 0.5F) * 0.28F;
            float tiltZ = (this.random.nextFloat() - 0.5F) * 0.28F;
            this.meteors.add(new Meteor(start, groundImpact, tick, wave % 2 == 0 ? GOLD : VIOLET, hero, monolith, tiltX, tiltZ));
         }
      }
   }

   @Override
   protected void onSustainedStart(int duration) {
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

   @Override
   public boolean hasBody() {
      return true;
   }

   // ───────────────────────────── 时间轴参数 ─────────────────────────────

   private boolean collapsed(float a) {
      return !this.stopped && a >= COLLAPSE;
   }

   private float stopFade(float a) {
      return this.stopped ? 1.0F - FxMath.window(a, this.stopAge, this.stopAge + 12.0F) : 1.0F;
   }

   private float riftOpen(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 0.0F, 7.0F)) * (1.0F - FxMath.window(a, COLLAPSE + 4.0F, COLLAPSE + 18.0F)) * this.stopFade(a);
   }

   /** 终幕前蓄势（30 → 38）：杂乱星雨退让，巨剑压下、魔龙光翼舒展。 */
   private float buildup(float a) {
      return this.stopped ? 0.0F : FxMath.easeInCubic(FxMath.window(a, COLLAPSE - 8.0F, COLLAPSE));
   }

   private float aftermath(float a) {
      return this.collapsed(a) ? FxMath.window(a, COLLAPSE, COLLAPSE + 18.0F) : 0.0F;
   }

   /** 魔龙星穹光翼舒展进度（20 → 36 展开，38 → 45 随爆发震散）。 */
   private float wingReveal(float a) {
      if (this.stopped) {
         return 0.0F;
      }
      float open = FxMath.easeOutCubic(FxMath.window(a, 20.0F, 34.0F));
      float dissolve = 1.0F - FxMath.window(a, COLLAPSE + 1.0F, COLLAPSE + 8.0F);
      return open * dissolve;
   }

   /** 神陨巨剑当前剑尖高度（30→35 悬停蓄势，35→38 加速贯地）。 */
   private double greatswordTipY(float a) {
      float emerge = FxMath.easeOutCubic(FxMath.window(a, 29.0F, 35.0F));
      float plunge = FxMath.easeInCubic(FxMath.window(a, 35.0F, COLLAPSE));
      double hoverY = this.groundY + 28.0;
      double startY = this.riftCenter.y + 12.0;
      if (a < 35.0F) {
         return startY + (hoverY - startY) * emerge;
      }
      return hoverY + (this.groundY - hoverY) * plunge;
   }

   private float night(float a) {
      return FxMath.easeOutCubic(FxMath.window(a, 0.0F, 10.0F)) * (1.0F - FxMath.window(a, COLLAPSE + 6.0F, COLLAPSE + 30.0F)) * this.stopFade(a);
   }

   private float nightAttenuation(FxFrame frame) {
      double distance = frame.camera.distanceTo(this.focal);
      return (float)Math.max(0.0, Math.min(1.0, 1.0 - (distance - this.explosionRadius * 1.5) / (this.explosionRadius * 2.5)));
   }

   private float vortexStrength(float a) {
      return FxMath.window(a, 0.0F, 8.0F) * (1.0F + 0.65F * this.buildup(a)) * (1.0F - FxMath.window(a, COLLAPSE - 1.0F, COLLAPSE + 2.0F)) * this.stopFade(a);
   }

   private float starRadius(float a) {
      float grow = FxMath.easeOutCubic(FxMath.window(a, 1.0F, 10.0F));
      if (a < COLLAPSE || this.stopped) {
         // 35→38 tick 暗星先极速向内坍缩压缩再爆发，拉出「吸气→炸裂」的节奏张力
         float shrink = 1.0F - 0.42F * FxMath.easeInCubic(FxMath.window(a, COLLAPSE - 3.0F, COLLAPSE));
         return this.starMax * grow * (1.0F + 0.35F * this.buildup(a)) * shrink * this.stopFade(a);
      }
      return this.starMax * (1.0F - FxMath.window(a, COLLAPSE, COLLAPSE + 2.0F));
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
               if (distance < 56.0) {
                  this.strike = Math.max(this.strike, (float)(1.0 - distance / 56.0) * (meteor.hero ? 0.32F : 0.14F));
                  int count = meteor.hero ? 5 : 2;
                  for (int k = 0; k < count; k++) {
                     this.embers.add(Ember.burst(meteor.impact.add(0.0, 0.3, 0.0), this.random, 0.32F + this.random.nextFloat() * 0.42F, meteor.color, true));
                  }
               }
            }
         }
      }

      // 第 30 tick：魔龙神陨巨剑现身低鸣
      if (age == 30 && !this.stopped) {
         level.playLocalSound(this.starCenter.x, this.starCenter.y, this.starCenter.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.5F, 0.55F, false);
      }

      if (age == COLLAPSE && !this.stopped) {
         this.collapse(level);
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
      Iterator<StarShard> shardIterator = this.shards.iterator();
      while (shardIterator.hasNext()) {
         StarShard shard = shardIterator.next();
         shard.tick();
         if (shard.life <= 0) {
            shardIterator.remove();
         }
      }
   }

   private void collapse(ClientLevel level) {
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
         double top = Math.min(level.getMaxBuildHeight(), Math.max(ground, this.riftCenter.y) + 12.0 + this.random.nextDouble() * 18.0);
         this.bolts.add(new FxLightningBolt(at.x, at.z, top, ground, this.random.nextLong(), 1 + this.random.nextInt(2), i % 2 == 0 ? VIOLET : GOLD));
      }
      level.playLocalSound(this.focal.x, this.focal.y, this.focal.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 8.0F, 0.72F, false);
      level.playLocalSound(this.focal.x, this.focal.y, this.focal.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 5.0F, 0.55F, false);
      level.setSkyFlashTime(3);
      // 精简长拖尾余烬（60 条）+ 新增 65 颗优雅旋转悬停的 3D 立体星晶，画面更高级不杂乱
      Vec3 burstOrigin = new Vec3(this.focal.x, this.groundY + 3.0, this.focal.z);
      for (int i = 0; i < 60; i++) {
         float[] color = i % 3 == 0 ? WHITE : (i % 3 == 1 ? GOLD : VIOLET);
         this.embers.add(Ember.burst(burstOrigin, this.random, 1.3F + this.random.nextFloat() * 2.6F, color, false));
      }
      for (int i = 0; i < 65; i++) {
         Vector3f dir = FxMath.randomUnit(this.random);
         dir.y = Math.abs(dir.y) * 0.75F + 0.15F;
         dir.normalize();
         float speed = 0.85F + this.random.nextFloat() * 2.2F;
         float size = 0.35F + this.random.nextFloat() * 0.55F;
         float[] color = i % 3 == 0 ? GOLD : (i % 3 == 1 ? VIOLET : WHITE);
         this.shards.add(new StarShard(burstOrigin.add(dir.x * 2.0, dir.y * 2.0, dir.z * 2.0), dir.x * speed, dir.y * speed, dir.z * speed, 20 + this.random.nextInt(14), size, color, this.random.nextFloat() * FxMath.TAU));
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         for (int i = 0; i < 60; i++) {
            Vector3f dir = FxMath.randomUnit(this.random).mul(0.9F);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, this.focal.x, this.focal.y, this.focal.z, dir.x, dir.y, dir.z);
         }
      }
      if (!this.stopped) {
         for (Meteor meteor : this.meteors) {
            if (meteor.impactTick == age) {
               level.addParticle(ParticleTypes.FLASH, meteor.impact.x, meteor.impact.y + 0.5, meteor.impact.z, 0.0, 0.0, 0.0);
               for (int k = 0; k < 5; k++) {
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

   // ───────────────────────────── 本体/暗部层（预乘 Alpha：构图剪影的灵魂）─────────────────────────────

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float fade = this.stopFade(a);
      float build = this.buildup(a);
      Vector3f star = frame.rel(this.starCenter);
      float starR = this.starRadius(a);

      // 1. 悬空日蚀暗星实心内核（360° 三平面无缝 CORONA 着色，从地面仰视也保留完整日蚀剪影与暗裂纹）
      if (starR > 0.08F) {
         b.geoCenter(star);
         b.begin(Style.CORONA, 0.55F, 0.6F, 2.2F, 1.3F, 0.7F + 0.9F * build, 0.35F + 0.65F * build);
         b.deformedSphere(star, starR * 1.02F, 18, 26, a * 0.04F, 0.035F + 0.03F * build, 1.2F, a * 0.22F, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.96F * fade);
         b.clearGeoCenter();
      }

      // 2. 魔龙星穹光翼 · 暗黑羽翼剪影（横贯施法者视野，20~45 tick）
      float wings = this.wingReveal(a);
      if (wings > 0.01F) {
         this.drawDragonWings(b, star, wings, a, true);
      }

      // 3. 神罚光柱外暗烟壳（22~35 tick：先自浑天仪贯地召唤巨剑，随后淡出让位给巨剑与终幕天柱）
      float pillar = FxMath.window(a, 22.0F, 25.0F) * (1.0F - FxMath.window(a, 32.5F, 35.0F)) * (this.stopped ? 0.0F : 1.0F);
      if (pillar > 0.01F) {
         Vector3f base = frame.rel(new Vec3(this.focal.x, this.groundY, this.focal.z));
         Vector3f top = frame.rel(this.riftCenter);
         float outerR = 2.95F * pillar;
         b.begin(Style.PLASMA_BEAM, 0.85F, 0.8F, 1.0F, 2.2F, 1.0F - pillar, 1.0F - FxMath.window(a, 22.0F, 28.0F));
         b.cylinder(base, top, outerR * 1.15F, outerR * 0.85F, 16, a * 0.08F, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.78F * pillar);
         b.end();
      }

      // 4. 天穹神陨巨剑 · 3D 暗紫实心棱面剑体（29~44 tick）
      if (!this.stopped && a >= 29.0F && a < COLLAPSE + 6.0F) {
         float swordFade = FxMath.window(a, 29.0F, 31.5F) * (1.0F - FxMath.window(a, COLLAPSE + 1.5F, COLLAPSE + 6.0F));
         if (swordFade > 0.01F) {
            this.drawCelestialGreatsword(b, frame, a, swordFade, true);
         }
      }

      // 5. 仅为主星陨绘制暗焰再入尾壳 + 落地 3D 短烟柱 + 插地星陨剑碑暗体
      if (!this.stopped || a < this.stopAge + 1.0F) {
         b.begin(Style.REENTRY, 0.75F, 0.4F, 1.1F, 1.5F, 2.2F, 0.45F);
         for (Meteor meteor : this.meteors) {
            if (!meteor.hero) {
               continue;
            }
            float start = meteor.impactTick - FALL_TICKS;
            if (a < start || a >= meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            float p = FxMath.easeInCubic(FxMath.window(a, start, meteor.impactTick));
            Vec3 head = meteor.start.lerp(meteor.impact, p);
            Vec3 dir = meteor.impact.subtract(meteor.start).normalize();
            double len = Math.min(14.0, meteor.start.distanceTo(head));
            if (len < 1.0) {
               continue;
            }
            Vec3 tail = head.subtract(dir.scale(len));
            b.cylinder(frame.rel(head), frame.rel(tail), 0.26F, 1.35F, 10, a * 0.25F, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.82F);
         }
         b.begin(Style.PLASMA_BEAM, 0.85F, 0.4F, 1.0F, -2.6F, 0.0F, 0.4F);
         for (Meteor meteor : this.meteors) {
            if (!meteor.hero) {
               continue;
            }
            float t = (a - meteor.impactTick) / 6.0F;
            if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            float height = 3.6F + 5.8F * FxMath.easeOutCubic(t);
            float r = 0.85F * (1.0F - t * 0.65F);
            b.cylinder(frame.rel(meteor.impact), frame.rel(meteor.impact.add(0.0, height, 0.0)), r, r * 0.42F, 10, -a * 0.2F, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.72F * (1.0F - t));
         }
         b.end();
         this.drawImpactSwordMonoliths(b, frame, a, true);
      }

      // 6. 终幕湮灭天柱外层暗烟壳（收窄至挺拔修长比例，清晰展现烟沟纹理而不挡死视野）
      float annihilation = this.collapsed(a) ? FxMath.window(a, COLLAPSE, COLLAPSE + 2.0F) * (1.0F - FxMath.window(a, COLLAPSE + 7.0F, COLLAPSE + 19.0F)) : 0.0F;
      if (annihilation > 0.01F) {
         double ceiling = Math.min(frame.level.getMaxBuildHeight(), this.riftCenter.y + 110.0);
         Vector3f base = frame.rel(new Vec3(this.focal.x, this.groundY, this.focal.z));
         Vector3f top = frame.rel(new Vec3(this.focal.x, ceiling, this.focal.z));
         float radius = Math.min(10.5F, this.explosionRadius * 0.115F) * (0.72F + 0.28F * annihilation);
         float collapseProg = FxMath.window(a, COLLAPSE + 7.0F, COLLAPSE + 19.0F);
         b.begin(Style.PLASMA_BEAM, 0.9F, 1.0F, 1.5F, -3.0F, collapseProg, FxMath.window(a, COLLAPSE, COLLAPSE + 6.0F));
         b.cylinder(base, top, radius * 1.18F, radius * 0.72F, 22, -a * 0.09F, VIOLET[0], VIOLET[1], VIOLET[2], 0.72F * annihilation);
         b.end();
      }
   }

   // ───────────────────────────── 网格 HDR 发光层（三层清透立体构图）─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float fade = this.stopFade(a);
      float build = this.buildup(a);
      float after = this.aftermath(a);
      float vortex = this.vortexStrength(a);
      Vector3f star = frame.rel(this.starCenter);
      float starR = this.starRadius(a);

      // ══ 第一层：地面舞台（克制底纹，留白给插地剑碑与爆发）══
      float circleIn = FxMath.easeOutCubic(FxMath.window(a, 0.0F, 8.0F));
      float circleAlpha = circleIn * fade * (1.0F - FxMath.window(a, COLLAPSE + 4.0F, COLLAPSE + 16.0F));
      if (circleAlpha > 0.01F) {
         Vector3f ground = frame.rel(new Vec3(this.focal.x, this.groundY + 0.06, this.focal.z));
         float outer = Math.min(34.0F, this.vortexRadius * 0.42F);
         g.begin(Style.RUNE, 1.65F + 2.2F * build, 0.0F, a * 0.025F, this.seedFloat, 1.0F, circleIn);
         g.quad(ground, new Vector3f(outer, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, outer), GOLD[0], GOLD[1], GOLD[2], circleAlpha * 0.72F);
         // 坍缩前聚焦锁定靶环：指示巨剑即将刺穿的中心点
         if (build > 0.01F && a < COLLAPSE) {
            float lockR = FxMath.lerp(12.0F, 3.2F, build);
            g.begin(Style.RING, 4.5F * build, 0.0F, 0.94F, 0.025F, 32.0F, 0.45F);
            g.quad(new Vector3f(ground).add(0.0F, 0.05F, 0.0F), new Vector3f(lockR, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, lockR), WHITE[0], WHITE[1], WHITE[2], build);
         }
         g.end();
      }

      // ══ 第二层：天穹浑天仪星环（groundY + 60，高空宏大立体构图，零地面干扰）══
      float riftOpen = this.riftOpen(a);
      if (riftOpen > 0.01F) {
         Vector3f sky = frame.rel(this.riftCenter.subtract(0.0, 2.5, 0.0));
         float skyR = this.riftHalfLength * 0.72F;
         g.begin(Style.RIBBON, 3.4F + 2.8F * build, 0.6F, 6.0F, 8.0F, 4.5F, 0.5F);
         // 三重交错倾角旋转的 3D 立体浑天仪星环
         g.torus(sky, new Vector3f(0.06F, 1.0F, 0.04F), skyR * 1.05F, 0.34F, 36, 6, a * 0.022F, GOLD[0], GOLD[1], GOLD[2], riftOpen * 0.85F);
         g.torus(new Vector3f(sky).add(0.0F, -2.0F, 0.0F), new Vector3f(0.28F, 0.94F, -0.18F), skyR * 0.72F, 0.26F, 30, 6, -a * 0.042F, VIOLET[0], VIOLET[1], VIOLET[2], riftOpen * 0.88F);
         g.torus(new Vector3f(sky).add(0.0F, -4.0F, 0.0F), new Vector3f(-0.24F, 0.95F, 0.22F), skyR * 0.42F, 0.22F, 26, 6, a * 0.075F, WHITE[0], WHITE[1], WHITE[2], riftOpen * 0.92F);
         g.end();
      }

      // ══ 第三层：中空神域（悬空日蚀暗星 + 6 道纤细抽吸流 + 魔龙星穹光翼 + 神陨巨剑）══
      // 3.1 从地面向上螺旋抽吸至悬空暗星的 6 道纤细能量流（精简条数与宽度，清晰勾勒重力井）
      if (vortex > 0.01F && starR > 0.05F) {
         float spin = a * (0.10F + 0.22F * build);
         float outerR = Math.min(28.0F, this.vortexRadius * 0.36F);
         float innerR = Math.max(starR * 1.35F, 1.5F);
         float ln = (float)Math.log(outerR / innerR);
         g.begin(Style.RIBBON, 2.2F + 2.4F * build, 0.4F, 6.5F, 4.0F, 6.0F, 0.45F);
         for (int arm = 0; arm < 6; arm++) {
            float theta0 = arm * FxMath.TAU / 6.0F + spin;
            int n = 22;
            List<Vector3f> points = new ArrayList<>(n + 1);
            float[] widths = new float[n + 1];
            float[] alphas = new float[n + 1];
            for (int i = 0; i <= n; i++) {
               float k = (float)i / n;
               float r = outerR * (float)Math.exp(-k * ln);
               float theta = theta0 + k * FxMath.TAU * 0.78F;
               double y = FxMath.lerp((float)this.groundY + 0.5F, (float)this.starCenter.y, FxMath.easeInOutSine(k));
               points.add(frame.rel(new Vec3(this.focal.x + Math.cos(theta) * r, y, this.focal.z + Math.sin(theta) * r)));
               widths[i] = 0.20F * (1.0F - k * 0.55F);
               alphas[i] = vortex * (float)Math.sin(Math.PI * k) * 0.65F;
            }
            float[] tint = arm % 2 == 0 ? GOLD : VIOLET;
            g.ribbon(points, widths, alphas, tint[0], tint[1], tint[2]);
         }
         g.end();
      }

      // 3.2 悬空日蚀暗星：360° 三平面无缝沸腾日冕外球壳 + 双重纤细 3D 轨道星环
      if (starR > 0.05F) {
         g.occluder(star, starR);
         g.geoCenter(star);
         g.begin(Style.CORONA, 2.4F + 3.4F * build, 0.6F, 2.2F, 1.4F, 0.9F + 1.1F * build, 0.45F + 0.55F * build);
         g.deformedSphere(star, starR * 1.14F, 18, 26, -a * 0.06F, 0.055F + 0.045F * build, 1.5F, a * 0.30F, GOLD[0], GOLD[1], GOLD[2], 0.88F * fade);
         g.clearGeoCenter();

         float orbitIn = FxMath.window(a, 2.0F, 10.0F) * fade;
         if (orbitIn > 0.01F) {
            g.begin(Style.RIBBON, 3.8F + 3.5F * build, 0.5F, 6.5F, 10.0F, 7.0F, 0.5F);
            g.torus(star, new Vector3f(0.32F, 0.94F, 0.16F), starR * 2.15F, starR * 0.065F, 28, 6, a * 0.14F, GOLD[0], GOLD[1], GOLD[2], orbitIn * 0.95F);
            g.torus(star, new Vector3f(-0.38F, 0.90F, -0.22F), starR * 2.85F, starR * 0.055F, 30, 6, -a * 0.11F, VIOLET[0], VIOLET[1], VIOLET[2], orbitIn * 0.82F);
            g.end();
         }

         g.begin(Style.GLOW, 2.0F + 3.0F * build, 0.6F, 3.8F, 2.2F, 0.9F, 0.0F);
         g.billboard(star, starR * 2.9F, a * 0.04F, VIOLET[0], VIOLET[1], VIOLET[2], 0.65F * fade);
         if (build > 0.0F && a < COLLAPSE) {
            g.begin(Style.GLOW, 7.0F + 8.0F * build, 0.6F, 6.0F, 2.6F, 1.6F, 0.0F);
            g.billboard(star, starR * (4.6F - 2.2F * build), 0.0F, WHITE[0], WHITE[1], WHITE[2], build);
         }
         g.clearOccluder();
      }

      // 3.25 神罚光柱：自天穹浑天仪贯下的 3D 三层同心嵌套圆柱（中金紫流柱 + 内白热芯）+ 3 道下压 3D 能量压环
      float pillar = FxMath.window(a, 22.0F, 25.0F) * (1.0F - FxMath.window(a, 32.5F, 35.0F)) * (this.stopped ? 0.0F : 1.0F);
      if (pillar > 0.01F) {
         Vector3f base = frame.rel(new Vec3(this.focal.x, this.groundY, this.focal.z));
         Vector3f top = frame.rel(this.riftCenter);
         float outerR = 2.35F * pillar;
         float coreR = Math.max(outerR * 0.45F, 0.55F);
         float wavePos = 1.0F - FxMath.window(a, 22.0F, 28.0F);
         g.begin(Style.PLASMA_BEAM, 4.0F, 0.7F, 1.0F, 2.6F, 1.0F - pillar, wavePos);
         g.cylinder(base, top, outerR, outerR * 0.82F, 16, a * 0.1F, GOLD[0], GOLD[1], GOLD[2], pillar * 0.92F);
         g.begin(Style.PLASMA_BEAM, 9.5F, 0.7F, 2.0F, 4.0F, 1.0F - pillar, wavePos);
         g.cylinder(base, top, coreR, coreR * 0.78F, 12, -a * 0.16F, WHITE[0], WHITE[1], WHITE[2], pillar);
         g.begin(Style.RIBBON, 5.5F, 0.5F, 6.0F, 8.0F, 10.0F, 0.5F);
         for (int k = 0; k < 3; k++) {
            float ringT = (a * 0.15F + k * 0.333F) % 1.0F;
            Vec3 pos = this.riftCenter.lerp(new Vec3(this.focal.x, this.groundY, this.focal.z), ringT);
            g.torus(frame.rel(pos), new Vector3f(0.0F, 1.0F, 0.0F), outerR * (1.45F + 0.55F * ringT), 0.20F, 20, 5, a * 0.2F, GOLD[0], GOLD[1], GOLD[2], pillar * (1.0F - ringT * 0.4F));
         }
         g.end();
      }

      // 3.3 魔龙星穹光翼 · 高能金紫羽刃（横贯施法者视野，极具终极仪式感！）
      float wings = this.wingReveal(a);
      if (wings > 0.01F) {
         this.drawDragonWings(g, star, wings, a, false);
      }

      // 3.4 天穹神陨巨剑 · 白金高能棱锋 + 剑尖破空激波锥（第 29~44 tick）
      if (!this.stopped && a >= 29.0F && a < COLLAPSE + 6.0F) {
         float swordFade = FxMath.window(a, 29.0F, 31.5F) * (1.0F - FxMath.window(a, COLLAPSE + 1.5F, COLLAPSE + 6.0F));
         if (swordFade > 0.01F) {
            this.drawCelestialGreatsword(g, frame, a, swordFade, false);
         }
      }

      // ══ 第四层：主次分级夜陨星雨 + 插地星陨剑碑（彻底消除互叠糊屏）══
      // 在 32→38 tick 蓄势阶段自动压暗退让，把舞台让给神陨巨剑与魔龙光翼
      float rainYield = 1.0F - 0.65F * FxMath.window(a, COLLAPSE - 6.0F, COLLAPSE);
      if ((!this.stopped || a < this.stopAge + 1.0F) && rainYield > 0.05F) {
         // A. 主星陨：3D 大气再入燃烧锥壳（比例修长精致，不遮挡整屏）
         g.begin(Style.REENTRY, 4.8F * rainYield, 0.4F, 1.3F, 1.4F, 2.8F, 0.55F);
         for (Meteor meteor : this.meteors) {
            if (!meteor.hero) {
               continue;
            }
            float start = meteor.impactTick - FALL_TICKS;
            if (a < start || a >= meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            float p = FxMath.easeInCubic(FxMath.window(a, start, meteor.impactTick));
            Vec3 head = meteor.start.lerp(meteor.impact, p);
            Vec3 dir = meteor.impact.subtract(meteor.start).normalize();
            double len = Math.min(15.0, meteor.start.distanceTo(head));
            if (len < 0.8) {
               continue;
            }
            Vec3 tail = head.subtract(dir.scale(len));
            g.cylinder(frame.rel(head), frame.rel(tail), 0.22F, 1.18F, 10, -a * 0.35F, meteor.color[0], meteor.color[1], meteor.color[2], 1.0F);
         }

         // B. 全部星雨核心箭矢：主星陨较粗，伴随星矢纤细凌厉（视觉密度恰到好处）
         g.begin(Style.RIBBON, 8.5F * rainYield, 0.35F, 7.5F, 5.0F, 9.0F, 0.35F);
         for (Meteor meteor : this.meteors) {
            float start = meteor.impactTick - FALL_TICKS;
            if (a < start || a >= meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            float p = FxMath.easeInCubic(FxMath.window(a, start, meteor.impactTick));
            Vec3 head = meteor.start.lerp(meteor.impact, p);
            Vec3 dir = meteor.impact.subtract(meteor.start).normalize();
            double trail = Math.min(meteor.hero ? 22.0 : 14.0, meteor.start.distanceTo(head));
            Vec3 tail = head.subtract(dir.scale(trail));
            float w = meteor.hero ? 0.52F : 0.22F;
            List<Vector3f> line = List.of(frame.rel(head), frame.rel(head.lerp(tail, 0.3)), frame.rel(tail));
            float[] tint = meteor.hero ? WHITE : meteor.color;
            g.ribbon(line, new float[]{w, w * 0.55F, 0.02F}, new float[]{1.0F, 0.72F, 0.0F}, tint[0], tint[1], tint[2]);
         }

         // C. 仅为主星陨加锐利流星头星芒
         g.begin(Style.GLOW, 11.0F * rainYield, 0.4F, 6.5F, 2.8F, 1.2F, 0.0F);
         for (Meteor meteor : this.meteors) {
            if (!meteor.hero) {
               continue;
            }
            float start = meteor.impactTick - FALL_TICKS;
            if (a < start || a >= meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            float p = FxMath.easeInCubic(FxMath.window(a, start, meteor.impactTick));
            g.billboard(frame.rel(meteor.start.lerp(meteor.impact, p)), 2.1F, 0.0F, WHITE[0], WHITE[1], WHITE[2], 1.0F);
         }
         g.end();
      }

      // 4.2 落地反应（主星陨：3D 喇叭形气浪冲击锥 + 3D 短冲柱 + 插地剑碑；伴随星矢：简洁星环）
      g.begin(Style.SHOCK_CONE, 4.4F * rainYield, 0.45F, 0.55F, 3.6F, 0.9F, 0.85F);
      for (Meteor meteor : this.meteors) {
         if (!meteor.hero) {
            continue;
         }
         float t = (a - meteor.impactTick) / 6.0F;
         if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         float ease = FxMath.easeOutCubic(t);
         Vector3f base = frame.rel(meteor.impact.add(0.0, 0.08, 0.0));
         Vector3f top = frame.rel(meteor.impact.add(0.0, 1.4 + 4.2 * ease, 0.0));
         g.cylinder(base, top, 0.45F + 1.1F * ease, 1.6F + 4.8F * ease, 14, a * 0.2F, meteor.color[0], meteor.color[1], meteor.color[2], (1.0F - t) * (1.0F - t * 0.35F));
      }
      g.begin(Style.PLASMA_BEAM, 5.8F * rainYield, 0.4F, 1.0F, -3.6F, 0.0F, 0.35F);
      for (Meteor meteor : this.meteors) {
         if (!meteor.hero) {
            continue;
         }
         float t = (a - meteor.impactTick) / 6.0F;
         if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         float height = 4.0F + 6.5F * FxMath.easeOutCubic(t);
         float r = 0.65F * (1.0F - t * 0.68F);
         g.cylinder(frame.rel(meteor.impact), frame.rel(meteor.impact.add(0.0, height, 0.0)), r, r * 0.28F, 10, a * 0.22F, meteor.color[0], meteor.color[1], meteor.color[2], 1.0F - t);
      }
      g.begin(Style.RING, 4.2F * rainYield, 0.0F, 0.94F, 0.045F, 0.0F, 0.0F);
      for (Meteor meteor : this.meteors) {
         float t = (a - meteor.impactTick) / 5.5F;
         if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         float maxR = meteor.hero ? 6.5F : 3.0F;
         float r = 0.6F + maxR * FxMath.easeOutCubic(t);
         g.quad(frame.rel(meteor.impact.add(0.0, 0.1, 0.0)), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), meteor.color[0], meteor.color[1], meteor.color[2], (1.0F - t) * (meteor.hero ? 0.95F : 0.55F));
      }
      g.begin(Style.GLOW, 6.5F * rainYield, 0.45F, 6.0F, 2.8F, 1.0F, 0.0F);
      for (Meteor meteor : this.meteors) {
         float t = (a - meteor.impactTick) / 5.0F;
         if (t < 0.0F || t >= 1.0F || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         float sz = meteor.hero ? (2.2F + 3.2F * t) : (1.1F + 1.4F * t);
         g.billboard(frame.rel(meteor.impact.add(0.0, 0.6, 0.0)), sz, 0.0F, meteor.color[0], meteor.color[1], meteor.color[2], (1.0F - t) * (1.0F - t) * (meteor.hero ? 0.9F : 0.55F));
      }
      g.end();

      // 4.3 地面插地星陨光剑碑（高光剑锋与万剑归宗能量丝）
      this.drawImpactSwordMonoliths(g, frame, a, false);

      // ══ 第五层：终幕坍缩爆发（神陨贯地 + 3D 湮灭天柱 + 24 道外围落雷 + 3D 星晶余韵）══
      if (!this.bolts.isEmpty()) {
         g.begin(Style.RIBBON, 2.4F, 0.4F, 2.2F, 9.0F, 18.0F, 0.85F);
         for (FxLightningBolt bolt : this.bolts) {
            if (bolt.visible()) {
               bolt.draw(g, frame, 3.6F, 0.5F, VIOLET);
            }
         }
         g.begin(Style.RIBBON, 10.0F, 0.4F, 9.5F, 12.0F, 24.0F, 0.2F);
         for (FxLightningBolt bolt : this.bolts) {
            if (bolt.visible()) {
               bolt.draw(g, frame, 0.9F, 1.0F, bolt.color);
            }
         }
         g.end();
      }

      // 5.1 坍缩爆发：贴地巨型 3D 气浪冲击锥 + 双重立体冲击环
      if (after > 0.0F && after < 1.0F) {
         float t = FxMath.window(a, COLLAPSE, COLLAPSE + 7.0F);
         Vector3f groundCenter = frame.rel(new Vec3(this.focal.x, this.groundY + 0.2, this.focal.z));
         if (t < 1.0F) {
            g.begin(Style.GLOW, 13.5F, 0.8F, 5.5F, 2.6F, 2.0F, 0.0F);
            g.billboard(new Vector3f(groundCenter).add(0.0F, 2.5F, 0.0F), this.explosionRadius * (0.22F + 0.30F * t), 0.0F, WHITE[0], WHITE[1], WHITE[2], (1.0F - t) * (1.0F - t));
         }
         float ease = FxMath.easeOutCubic(after);
         Vector3f coneTop = frame.rel(new Vec3(this.focal.x, this.groundY + 4.5 + this.explosionRadius * 0.34F * ease, this.focal.z));
         g.begin(Style.SHOCK_CONE, 5.5F * (1.0F - after), 0.8F, ease, 2.8F, 1.0F, 0.85F);
         g.cylinder(groundCenter, coneTop, this.explosionRadius * 0.10F, this.explosionRadius * (0.28F + 0.78F * ease), 28, a * 0.05F, GOLD[0], GOLD[1], GOLD[2], 1.0F - after);

         g.begin(Style.RIBBON, 5.8F * (1.0F - after), 0.5F, 5.5F, 6.0F, 8.0F, 0.45F);
         g.torus(new Vector3f(groundCenter).add(0.0F, 0.9F, 0.0F), new Vector3f(0.0F, 1.0F, 0.0F), this.explosionRadius * ease, 0.55F * (1.0F - after * 0.5F), 36, 6, a * 0.08F, GOLD[0], GOLD[1], GOLD[2], 1.0F - after);

         float second = FxMath.window(a, COLLAPSE + 4.0F, COLLAPSE + 17.0F);
         if (second > 0.0F && second < 1.0F) {
            float r2 = this.explosionRadius * 1.28F * FxMath.easeOutCubic(second);
            g.torus(new Vector3f(groundCenter).add(0.0F, 1.6F, 0.0F), new Vector3f(0.0F, 1.0F, 0.0F), r2, 0.45F * (1.0F - second * 0.5F), 36, 6, -a * 0.06F, VIOLET[0], VIOLET[1], VIOLET[2], 1.0F - second);
         }
         g.end();
      }

      // 5.2 终幕湮灭天柱：挺拔修长的 3D 同心嵌套圆柱 + 沿柱冲天的立体冲击压环
      float annihilation = this.collapsed(a) ? FxMath.window(a, COLLAPSE, COLLAPSE + 2.0F) * (1.0F - FxMath.window(a, COLLAPSE + 7.0F, COLLAPSE + 19.0F)) : 0.0F;
      if (annihilation > 0.01F) {
         double ceiling = Math.min(frame.level.getMaxBuildHeight(), this.riftCenter.y + 110.0);
         Vector3f base = frame.rel(new Vec3(this.focal.x, this.groundY, this.focal.z));
         Vector3f top = frame.rel(new Vec3(this.focal.x, ceiling, this.focal.z));
         float radius = Math.min(9.0F, this.explosionRadius * 0.095F) * (0.72F + 0.28F * annihilation);
         float coreR = radius * 0.46F;
         float collapseProg = FxMath.window(a, COLLAPSE + 7.0F, COLLAPSE + 19.0F);
         float wavePos = FxMath.window(a, COLLAPSE, COLLAPSE + 8.0F);
         g.begin(Style.PLASMA_BEAM, 4.4F, 0.8F, 1.5F, -3.4F, collapseProg, wavePos);
         g.cylinder(base, top, radius * 1.12F, radius * 0.68F, 22, a * 0.11F, GOLD[0], GOLD[1], GOLD[2], annihilation);
         g.begin(Style.PLASMA_BEAM, 11.5F, 0.8F, 2.0F, -5.0F, collapseProg, wavePos);
         g.cylinder(base, top, coreR, coreR * 0.62F, 16, -a * 0.18F, WHITE[0], WHITE[1], WHITE[2], annihilation);
         g.begin(Style.RIBBON, 6.2F, 0.6F, 6.5F, 8.0F, 10.0F, 0.45F);
         for (int k = 0; k < 4; k++) {
            float ringT = ((a - COLLAPSE) * 0.10F + k * 0.25F) % 1.0F;
            double y = this.groundY + (ceiling - this.groundY) * ringT * 0.60;
            g.torus(frame.rel(new Vec3(this.focal.x, y, this.focal.z)), new Vector3f(0.0F, 1.0F, 0.0F), radius * (1.6F + 1.0F * (1.0F - ringT)), 0.30F, 24, 6, a * 0.16F, GOLD[0], GOLD[1], GOLD[2], annihilation * (1.0F - ringT * 0.6F));
         }
         g.end();
      }

      // 5.3 3D 浮空旋转星晶碎片 + 长拖尾余烬
      if (!this.shards.isEmpty()) {
         g.begin(Style.GLOW, 5.2F, 0.0F, 2.2F, 1.1F, 0.0F, 0.0F);
         for (StarShard shard : this.shards) {
            Vec3 p = shard.position(frame.partialTick);
            Vector3f rel = frame.rel(p);
            float lifeAlpha = (float)shard.life / shard.maxLife;
            float spin = shard.phase + a * 0.28F;
            float cs = (float)Math.cos(spin);
            float sn = (float)Math.sin(spin);
            float s = shard.size;
            Vector3f u = new Vector3f(cs * s, sn * s * 0.6F, -sn * s * 0.8F);
            Vector3f v = new Vector3f(-sn * s * 0.55F, cs * s * 0.85F, cs * s * 0.6F);
            Vector3f n = new Vector3f(u).cross(v).normalize();
            g.shard(rel, u, v, n, s * 0.28F, shard.color[0], shard.color[1], shard.color[2], lifeAlpha);
         }
         g.end();
      }
      if (!this.embers.isEmpty()) {
         g.begin(Style.RIBBON, 4.8F, 0.0F, 6.0F, 0.0F, 0.0F, 0.45F);
         for (Ember ember : this.embers) {
            Vec3 p = ember.position(frame.partialTick);
            Vec3 trail = p.subtract(ember.vx * 2.4, ember.vy * 2.4, ember.vz * 2.4);
            g.streak(frame.rel(p), frame.rel(trail), 0.065F, ember.color[0], ember.color[1], ember.color[2], (float)ember.life / ember.maxLife);
         }
         g.end();
      }
   }

   // ───────────────────────────── 终极新奇观绘制子模块 ─────────────────────────────

   /**
    * 奇观①：魔龙星穹光翼（横贯施法者视野，左右各 5 片上长下短、层叠舒展的 3D 弧面羽刃）。
    * Body 层压出深渊暗紫实心羽翼剪影，FX 层叠金紫/白金流光翼锋。
    */
   private void drawDragonWings(FxGeometry g, Vector3f star, float reveal, float a, boolean bodyPass) {
      float spanScale = Math.max(14.0F, Math.min(28.0F, this.starMax * 5.0F));
      float flap = (float)Math.sin(a * 0.22F) * 0.10F;
      g.begin(Style.SLASH_ARC, bodyPass ? 0.42F : 5.2F, 0.5F, bodyPass ? 5.5F : 8.5F, 0.85F, reveal, 0.0F);
      for (int side = -1; side <= 1; side += 2) {
         // 每侧翼面略微向后折掠 16°，呈现立体展翼弧度
         float sx = this.riftAxisX * side - this.lookAxisX * 0.28F;
         float sz = this.riftAxisZ * side - this.lookAxisZ * 0.28F;
         Vector3f wingOut = new Vector3f(sx, 0.0F, sz).normalize();
         Vector3f wingUp = new Vector3f(0.0F, 1.0F, 0.0F);
         for (int feather = 0; feather < 5; feather++) {
            float fk = feather / 4.0F;
            // 最上方主羽翼最长且昂扬向上，下层副羽依次缩短错落，形成真正的龙翼展翅轮廓
            float baseLen = spanScale * (1.06F - 0.56F * fk) * (0.35F + 0.65F * reveal);
            float innerR = baseLen * (bodyPass ? 0.60F : 0.64F);
            float outerR = baseLen * (bodyPass ? 1.02F : 1.0F);
            float pitchOffset = (0.12F - fk * 0.72F) + flap * (1.0F - fk * 0.5F);
            float startAng = pitchOffset - 0.15F;
            float endAng = pitchOffset + 0.85F;
            Vector3f anchor = new Vector3f(star).add(wingOut.x * (this.starMax * 0.82F), 1.6F - fk * 2.2F, wingOut.z * (this.starMax * 0.82F));
            float[] tint = bodyPass ? DEEP_ABYSS : (feather % 2 == 0 ? GOLD : VIOLET);
            g.slashArc(anchor, wingOut, wingUp, innerR, outerR, startAng, endAng, 14, tint[0], tint[1], tint[2], reveal * (bodyPass ? 0.88F : 0.92F));
         }
      }
      g.end();
   }

   /**
    * 奇观②：天穹终焉神陨巨剑（高达 46 格的 3D 扁平棱面菱形截面巨剑，自浑天仪星环中心垂直贯落）。
    * 第 38 tick 剑尖精准刺穿地面焦点，配合黑白反相冲击帧形成标志性终极剪影。
    */
   private void drawCelestialGreatsword(FxGeometry g, FxFrame frame, float a, float alpha, boolean bodyPass) {
      double tipY = this.greatswordTipY(a);
      double bladeLen = 34.0;
      double hiltLen = 11.0;
      double guardY = tipY + bladeLen;
      double pommelY = guardY + hiltLen;
      Vector3f tip = frame.rel(new Vec3(this.focal.x, tipY, this.focal.z));
      Vector3f shoulder = frame.rel(new Vec3(this.focal.x, tipY + 7.5, this.focal.z));
      Vector3f guard = frame.rel(new Vec3(this.focal.x, guardY, this.focal.z));
      Vector3f pommel = frame.rel(new Vec3(this.focal.x, pommelY, this.focal.z));

      // 宽阔扁平剑身面向玩家视线，龙翼十字护手左右展开
      Vector3f bladeRight = new Vector3f(this.riftAxisX, 0.0F, this.riftAxisZ);
      Vector3f verticalUp = new Vector3f(0.0F, 1.0F, 0.0F);
      float guardSpan = 8.4F;
      Vector3f guardL = frame.rel(new Vec3(this.focal.x - this.riftAxisX * guardSpan, guardY + 1.8, this.focal.z - this.riftAxisZ * guardSpan));
      Vector3f guardR = frame.rel(new Vec3(this.focal.x + this.riftAxisX * guardSpan, guardY + 1.8, this.focal.z + this.riftAxisZ * guardSpan));

      if (bodyPass) {
         // 实体暗紫扁平菱形剑刃 + 龙翼十字护手 + 剑柄
         g.begin(Style.PLASMA_BEAM, 0.55F, 0.5F, 1.0F, 1.8F, 0.0F, 0.5F);
         g.orientedCylinder(tip, shoulder, bladeRight, 0.14F, 0.08F, 2.75F, 0.62F, 4, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.96F * alpha);
         g.orientedCylinder(shoulder, guard, bladeRight, 2.75F, 0.62F, 3.05F, 0.72F, 4, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.96F * alpha);
         g.orientedCylinder(guard, pommel, bladeRight, 0.72F, 0.45F, 0.52F, 0.35F, 6, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.94F * alpha);
         g.orientedCylinder(guard, guardL, verticalUp, 1.25F, 0.55F, 0.18F, 0.10F, 4, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.95F * alpha);
         g.orientedCylinder(guard, guardR, verticalUp, 1.25F, 0.55F, 0.18F, 0.10F, 4, DEEP_ABYSS[0], DEEP_ABYSS[1], DEEP_ABYSS[2], 0.95F * alpha);
         g.end();
         return;
      }

      // HDR 层：白金/金紫高能剑锋棱面 + 龙翼护手金光 + 剑首星核 + 下坠破空激波锥
      g.begin(Style.PLASMA_BEAM, 7.2F, 0.5F, 1.0F, 4.2F, 0.0F, 0.35F);
      g.orientedCylinder(tip, shoulder, bladeRight, 0.12F, 0.06F, 2.55F, 0.52F, 4, WHITE[0], WHITE[1], WHITE[2], alpha);
      g.orientedCylinder(shoulder, guard, bladeRight, 2.55F, 0.52F, 2.82F, 0.62F, 4, GOLD[0], GOLD[1], GOLD[2], alpha * 0.94F);
      g.orientedCylinder(guard, pommel, bladeRight, 0.64F, 0.40F, 0.44F, 0.30F, 6, VIOLET[0], VIOLET[1], VIOLET[2], alpha * 0.85F);
      g.orientedCylinder(guard, guardL, verticalUp, 1.15F, 0.48F, 0.16F, 0.08F, 4, GOLD[0], GOLD[1], GOLD[2], alpha);
      g.orientedCylinder(guard, guardR, verticalUp, 1.15F, 0.48F, 0.16F, 0.08F, 4, GOLD[0], GOLD[1], GOLD[2], alpha);

      // 剑尖加速贯落时的 3D 破空激波锥（35→38 tick）
      float plunge = FxMath.window(a, 34.5F, COLLAPSE);
      if (plunge > 0.01F && a < COLLAPSE + 1.0F) {
         Vector3f coneTop = frame.rel(new Vec3(this.focal.x, tipY + 14.0, this.focal.z));
         g.begin(Style.SHOCK_CONE, 6.8F * plunge, 0.5F, plunge, 4.5F, 1.0F, 1.0F);
         g.cylinder(tip, coneTop, 0.35F, 5.2F + 4.5F * plunge, 16, a * 0.3F, WHITE[0], WHITE[1], WHITE[2], alpha * plunge);
      }

      // 剑首星核辉光 + 护手核心闪芒
      g.begin(Style.GLOW, 9.0F, 0.4F, 5.5F, 2.4F, 1.6F, 0.0F);
      g.billboard(guard, 4.2F, a * 0.08F, GOLD[0], GOLD[1], GOLD[2], alpha);
      g.billboard(pommel, 3.0F, -a * 0.1F, VIOLET[0], VIOLET[1], VIOLET[2], alpha);
      g.end();
   }

   /**
    * 奇观③：外围弹坑插地星陨光剑碑（3D 扁平棱面剑碑环绕中心形成剑冢，34→38 tick 万剑归宗抽向暗星）。
    */
   private void drawImpactSwordMonoliths(FxGeometry g, FxFrame frame, float a, boolean bodyPass) {
      if (a >= COLLAPSE + 1.0F) {
         return;
      }
      float recall = FxMath.window(a, COLLAPSE - 3.5F, COLLAPSE);
      float globalFade = (1.0F - recall) * this.stopFade(a);
      if (globalFade <= 0.01F) {
         return;
      }
      Vector3f bladeRight = new Vector3f(this.riftAxisX, 0.0F, this.riftAxisZ);
      Vector3f verticalUp = new Vector3f(0.0F, 1.0F, 0.0F);
      g.begin(Style.PLASMA_BEAM, bodyPass ? 0.55F : 4.2F, 0.3F, 1.0F, -2.2F, recall, 0.5F);
      for (Meteor meteor : this.meteors) {
         if (!meteor.monolith || a < meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
            continue;
         }
         float appear = FxMath.easeOutCubic(Math.min(1.0F, (a - meteor.impactTick) / 2.0F));
         float h = 5.8F * appear;
         Vec3 base = meteor.impact;
         Vec3 top = base.add(meteor.tiltX * h, h, meteor.tiltZ * h);
         Vec3 guardPos = base.lerp(top, 0.74);
         float[] tint = bodyPass ? DEEP_ABYSS : meteor.color;
         float alpha = globalFade * (bodyPass ? 0.88F : 0.85F);
         // 扁平菱形截面剑碑 + 十字小护手
         g.orientedCylinder(frame.rel(base), frame.rel(top), bladeRight, bodyPass ? 0.34F : 0.28F, bodyPass ? 0.11F : 0.08F, 0.06F, 0.04F, 4, tint[0], tint[1], tint[2], alpha);
         Vector3f side = new Vector3f(-meteor.tiltZ + 0.8F, 0.0F, meteor.tiltX + 0.6F).normalize().mul(1.15F);
         Vector3f gCenter = frame.rel(guardPos);
         g.orientedCylinder(new Vector3f(gCenter).sub(side), new Vector3f(gCenter).add(side), verticalUp, 0.12F, 0.06F, 0.12F, 0.06F, 4, tint[0], tint[1], tint[2], alpha);
      }
      g.end();

      // 34→38 tick 万剑归宗：插地剑碑向悬空暗星牵出纤细能量丝
      if (!bodyPass && recall > 0.01F) {
         Vector3f star = frame.rel(this.starCenter);
         g.begin(Style.RIBBON, 5.5F * recall, 0.3F, 8.0F, 8.0F, 14.0F, 0.35F);
         for (Meteor meteor : this.meteors) {
            if (!meteor.monolith || a < meteor.impactTick || (this.stopped && meteor.impactTick > this.stopAge)) {
               continue;
            }
            Vector3f from = frame.rel(meteor.impact.add(0.0, 4.2, 0.0));
            Vector3f mid = new Vector3f(from).lerp(star, 0.5F).add(0.0F, 2.5F, 0.0F);
            g.ribbon(List.of(from, mid, star), new float[]{0.08F, 0.12F, 0.03F}, new float[]{recall * 0.7F, recall, recall * 0.4F}, GOLD[0], GOLD[1], GOLD[2]);
         }
         g.end();
      }
   }

   // ───────────────────────────── B 档全屏通道（中心对齐悬空暗星）─────────────────────────────

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
      // 坍缩前引力漩涡与透镜对齐悬空暗星，坍缩瞬间冲击波中心落回地面焦点
      Vector3f star = frame.rel(a < COLLAPSE ? this.starCenter : new Vec3(this.focal.x, this.groundY + 2.0, this.focal.z));
      Vector3f riftRel = frame.rel(this.riftCenter);
      float shockRadius = after > 0.0F ? this.explosionRadius * FxMath.easeOutCubic(after) : 0.0F;
      float shockStrength = after > 0.0F && after < 1.0F ? (1.0F - after) * (1.0F - after) : 0.0F;
      float shockThickness = this.explosionRadius * 0.055F;
      float flash = this.collapsed(a) ? Math.max(0.0F, 1.0F - (a - COLLAPSE) / 8.0F) : 0.0F;
      float spin = 1.4F + 2.8F * this.buildup(a);
      Consumer<ShaderInstance> uniforms = shader -> {
         shader.safeGetUniform("Star").set(star.x, star.y, star.z, starR);
         shader.safeGetUniform("Rift").set(riftRel.x, riftRel.y, riftRel.z, rift);
         shader.safeGetUniform("RiftShape").set(this.riftAxisX, this.riftAxisZ, this.riftHalfLength, this.riftHalfWidth);
         shader.safeGetUniform("Vortex").set(Math.min(1.25F, vortex), Math.min(28.0F, this.vortexRadius * 0.36F), spin, this.seedFloat);
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
      float[] focusUv = frame.screenUv(a < COLLAPSE ? this.starCenter : this.focal);
      if (a < 6.0F) {
         float t = 1.0F - a / 6.0F;
         screen.addFlash(0xB026FF, 0.20F * t * t * att, 0.4F);
         screen.addAberration(0.45F * t * att);
         screen.addDashBlur(0.35F * t * att, focusUv);
      }
      if (this.strike > 0.01F) {
         screen.addAberration(this.strike * 1.15F);
      }
      if (frame.camera.distanceTo(this.focal) < this.vortexRadius * 0.6) {
         screen.addVignette(0x10021C, 0.28F * this.vortexStrength(a));
      }
      float build = this.buildup(a);
      if (build > 0.0F && a < COLLAPSE) {
         screen.addDashBlur(0.45F * build * att, focusUv);
         screen.addDesaturate(0.28F * build * att);
         screen.addVignette(0x10021C, 0.45F * build * att);
         screen.addFovKick(-0.08F * build * att);
      }
      if (this.collapsed(a)) {
         float t = a - COLLAPSE;
         // 冲击帧：巨剑贯地瞬间 3 tick 高对比定格（巨剑+龙翼剪影），第二拍反相
         if (t < 3.0F) {
            screen.addImpactFrame(att, t >= 1.0F && t < 2.0F);
         }
         float flash = t < 3.0F ? 0.0F : Math.max(0.0F, 1.0F - (t - 3.0F) / 8.0F);
         screen.addFlash(0xFFF1D6, 0.85F * flash * flash * att, 1.15F);
         screen.addAberration(1.3F * Math.max(0.0F, 1.0F - t / 14.0F) * att);
         screen.addDashBlur(1.15F * Math.max(0.0F, 1.0F - t / 12.0F) * att, focusUv);
         screen.addBloomBoost(1.2F * Math.max(0.0F, 1.0F - t / 10.0F) * att);
         float kick = t < 2.0F ? FxMath.easeOutCubic(t / 2.0F) : 1.0F - FxMath.easeInOutSine(FxMath.window(t, 2.0F, 14.0F));
         screen.addFovKick(0.15F * kick * att);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      float value = this.strike;
      if (a < 8.0F) {
         value = Math.max(value, 0.35F * (1.0F - a / 8.0F));
      }
      if (this.collapsed(a)) {
         value = Math.max(value, 0.65F * (1.0F - this.aftermath(a)));
      }
      return value * this.attenuation(frame);
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private record Meteor(Vec3 start, Vec3 impact, int impactTick, float[] color, boolean hero, boolean monolith, float tiltX, float tiltZ) {
   }

   private static final class StarShard {
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
      final float size;
      final float[] color;
      final float phase;

      StarShard(Vec3 origin, double vx, double vy, double vz, int life, float size, float[] color, float phase) {
         this.x = this.px = origin.x;
         this.y = this.py = origin.y;
         this.z = this.pz = origin.z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.life = life;
         this.maxLife = life;
         this.size = size;
         this.color = color;
         this.phase = phase;
      }

      void tick() {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x += this.vx;
         this.y += this.vy;
         this.z += this.vz;
         // 炸开后迅速减速进入慢动作悬浮飘升
         this.vx *= 0.84;
         this.vy = this.vy * 0.84 + 0.015;
         this.vz *= 0.84;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.px + (this.x - this.px) * partialTick, this.py + (this.y - this.py) * partialTick, this.pz + (this.z - this.pz) * partialTick);
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
            dir.y = Math.abs(dir.y) + speed * 0.45F;
         }
         return new Ember(origin, dir, 12 + random.nextInt(16), color, upward);
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
