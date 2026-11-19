package QWQ.QingYi.annihilationblade.blood_prison.client.fx;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 血狱 SA「炼狱·杀戮」领域专属渲染（持续特效）——「血之囚笼」。
 * <pre>
 *  0 ─ 10   血狱展开：中心血光迸发，血色冲击环扫向边界，囚笼光栅自地面逐根升起
 *  持续     正方形血色囚笼（与判定范围一致）、底部血雾帷幕、地面血狱法阵、血雨；
 *           施法者周身血气螺旋；每个脉冲周期一道血色冲击环扫过全域，光栅随之闪亮
 *  B 档     领域内世界染为血色，地表血脉随心跳明灭；身处其中时天空化为血色并悬挂血月
 *  结束     光栅沉回地面，血色褪去（施法者死亡/离线时服务端提前 STOP）
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public final class BloodDomainFx extends FxInstance {
   private static final int DEFAULT_DURATION = 400;
   private static final int TAIL = 12;
   private static final float BAR_SPACING = 1.6F;
   private static final float[] CRIMSON = {1.0F, 0.1F, 0.14F};
   private static final float[] DARK = {0.6F, 0.02F, 0.06F};
   private static final float[] WHITE = {1.0F, 0.85F, 0.85F};

   private final Vec3 center;
   private final float half;
   private final float barHeight;
   private final float visualScale;
   private final int pulseInterval;
   private final int casterId;
   private final float seedFloat;
   private final RandomSource random;
   private final List<Drop> drops = new ArrayList<>();
   private int endAge = DEFAULT_DURATION;

   public BloodDomainFx(ClientLevel level, FxEventPacket packet) {
      super(DEFAULT_DURATION + TAIL);
      this.center = packet.origin();
      this.half = Math.max(2.0F, packet.radius());
      this.visualScale = Math.max(0.25F, packet.scale());
      this.barHeight = Math.max(4.0F, this.half * 0.9F);
      this.pulseInterval = Math.max(4, packet.intParam());
      this.casterId = packet.casterId();
      this.seedFloat = (float)((packet.seed() & 0xFFFFL) / 65535.0 * 97.0);
      this.random = RandomSource.create(packet.seed());
   }

   @Override
   protected void onSustainedStart(int duration) {
      this.endAge = Math.max(1, duration);
      this.lifetime = this.endAge + TAIL;
   }

   @Override
   public void onSustainedStop() {
      if (this.age < this.endAge) {
         this.endAge = this.age;
         this.lifetime = this.endAge + TAIL;
      }
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public double cullDistance() {
      return this.half + 160.0;
   }

   private float rise(float a) {
      float up = FxMath.easeOutCubic(FxMath.window(a, 0.0F, 10.0F));
      float down = FxMath.easeInCubic(FxMath.window(a, this.endAge, this.endAge + 10.0F));
      return up * (1.0F - down);
   }

   private float strength(float a) {
      return FxMath.window(a, 0.0F, 8.0F) * (1.0F - FxMath.window(a, this.endAge, this.endAge + TAIL));
   }

   /** 当前脉冲进度 0..1（超出返回 -1）。 */
   private float pulse(float a) {
      if (a < 10.0F || a >= this.endAge) {
         return -1.0F;
      }
      float p = (a % this.pulseInterval) / (this.pulseInterval * 0.7F);
      return p < 1.0F ? p : -1.0F;
   }

   private boolean cameraInside(Vec3 camera) {
      return Math.abs(camera.x - this.center.x) < this.half && Math.abs(camera.z - this.center.z) < this.half
         && camera.y > this.center.y - 2.0 && camera.y < this.center.y + this.barHeight + 2.0;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      if (age < this.endAge) {
         int count = Math.max(1, (int)(this.half * this.half / 25.0F * this.visualScale));
         for (int i = 0; i < Math.min(12, count); i++) {
            double x = this.center.x + (this.random.nextDouble() * 2.0 - 1.0) * this.half;
            double z = this.center.z + (this.random.nextDouble() * 2.0 - 1.0) * this.half;
            this.drops.add(new Drop(x, this.center.y + this.barHeight + 1.0 + this.random.nextDouble() * 2.0, z));
         }
      }
      Iterator<Drop> iterator = this.drops.iterator();
      while (iterator.hasNext()) {
         Drop drop = iterator.next();
         drop.tick();
         if (drop.y < this.center.y) {
            iterator.remove();
         }
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age >= this.endAge || age % 10 != 0) {
         return;
      }
      for (double offset = -this.half; offset <= this.half; offset += 2.0) {
         level.addParticle(ParticleTypes.DRAGON_BREATH, this.center.x + offset, this.center.y + 0.1, this.center.z - this.half, 0.0, 0.0, 0.0);
         level.addParticle(ParticleTypes.DRAGON_BREATH, this.center.x + offset, this.center.y + 0.1, this.center.z + this.half, 0.0, 0.0, 0.0);
         level.addParticle(ParticleTypes.DRAGON_BREATH, this.center.x - this.half, this.center.y + 0.1, this.center.z + offset, 0.0, 0.0, 0.0);
         level.addParticle(ParticleTypes.DRAGON_BREATH, this.center.x + this.half, this.center.y + 0.1, this.center.z + offset, 0.0, 0.0, 0.0);
      }
      for (int i = 0; i < 6; i++) {
         level.addParticle(ParticleTypes.CRIMSON_SPORE, this.center.x + (this.random.nextDouble() * 2.0 - 1.0) * this.half, this.center.y + 0.2, this.center.z + (this.random.nextDouble() * 2.0 - 1.0) * this.half, 0.0, 0.01, 0.0);
      }
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float rise = this.rise(a);
      float strength = this.strength(a);
      float pulse = this.pulse(a);
      float flash = pulse >= 0.0F ? (1.0F - pulse) * (1.0F - pulse) : 0.0F;
      Vector3f ground = frame.rel(this.center.add(0.0, 0.06, 0.0));

      // 1. 开场：中心血光与扫向边界的冲击环
      if (a < 12.0F) {
         float t = a / 12.0F;
         g.begin(Style.GLOW, 8.0F, 0.5F, 5.0F, 2.2F, 0.8F, 0.0F);
         g.billboard(frame.rel(this.center.add(0.0, 1.0, 0.0)), this.half * 0.6F, 0.0F, CRIMSON[0], CRIMSON[1], CRIMSON[2], (1.0F - t) * (1.0F - t));
         float r = this.half * 1.45F * FxMath.easeOutCubic(t);
         g.begin(Style.RING, 5.0F, 0.0F, 0.94F, 0.05F, 0.0F, 0.0F);
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), CRIMSON[0], CRIMSON[1], CRIMSON[2], 1.0F - t);
         g.end();
      }

      // 2. 地面血狱法阵 + 周期脉冲环
      if (strength > 0.0F) {
         float r = this.half * 0.95F;
         g.begin(Style.RUNE, 2.6F + 3.0F * flash, 0.0F, a * 0.02F, this.seedFloat, 1.0F, FxMath.easeOutCubic(FxMath.window(a, 0.0F, 12.0F)));
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), CRIMSON[0], CRIMSON[1], CRIMSON[2], strength);
         if (pulse >= 0.0F) {
            float pr = this.half * 1.414F * FxMath.easeOutCubic(pulse);
            g.begin(Style.RING, 4.0F * (1.0F - pulse), 0.0F, 0.95F, 0.03F, 0.0F, 0.0F);
            g.quad(new Vector3f(ground).add(0.0F, 0.04F, 0.0F), new Vector3f(pr, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, pr), CRIMSON[0], CRIMSON[1], CRIMSON[2], strength * (1.0F - pulse));
         }
         g.end();
      }

      // 3. 血之囚笼：四边光栅、顶框、角柱、底部血雾帷幕
      if (rise > 0.0F) {
         float top = this.barHeight * rise;
         int perEdge = Math.max(2, (int)Math.ceil(this.half * 2.0F / BAR_SPACING));
         g.begin(Style.BEAM, 3.5F + 5.0F * flash, 0.3F, 6.0F, 2.0F, 0.8F, 0.6F);
         for (int edge = 0; edge < 4; edge++) {
            for (int i = 0; i <= perEdge; i++) {
               float k = (float)i / perEdge;
               float along = -this.half + this.half * 2.0F * k;
               double x;
               double z;
               switch (edge) {
                  case 0 -> { x = this.center.x + along; z = this.center.z - this.half; }
                  case 1 -> { x = this.center.x + along; z = this.center.z + this.half; }
                  case 2 -> { x = this.center.x - this.half; z = this.center.z + along; }
                  default -> { x = this.center.x + this.half; z = this.center.z + along; }
               }
               float stagger = FxMath.window(rise, k * 0.3F, 0.7F + k * 0.3F);
               if (stagger <= 0.0F) {
                  continue;
               }
               boolean corner = i == 0 || i == perEdge;
               float barTop = top * stagger + (corner ? 2.0F * rise : 0.0F);
               List<Vector3f> bar = List.of(frame.rel(new Vec3(x, this.center.y, z)), frame.rel(new Vec3(x, this.center.y + barTop * 0.5, z)), frame.rel(new Vec3(x, this.center.y + barTop, z)));
               float w = corner ? 0.35F : 0.12F;
               g.ribbon(bar, new float[]{w, w, w * 0.6F}, new float[]{strength, strength, strength * 0.7F}, CRIMSON[0], CRIMSON[1], CRIMSON[2]);
            }
         }
         // 顶框
         Vec3[] corners = {
            new Vec3(this.center.x - this.half, this.center.y + top, this.center.z - this.half),
            new Vec3(this.center.x + this.half, this.center.y + top, this.center.z - this.half),
            new Vec3(this.center.x + this.half, this.center.y + top, this.center.z + this.half),
            new Vec3(this.center.x - this.half, this.center.y + top, this.center.z + this.half)
         };
         for (int i = 0; i < 4; i++) {
            List<Vector3f> edge = List.of(frame.rel(corners[i]), frame.rel(corners[i].lerp(corners[(i + 1) % 4], 0.5)), frame.rel(corners[(i + 1) % 4]));
            g.ribbon(edge, new float[]{0.2F, 0.2F, 0.2F}, new float[]{strength, strength, strength}, CRIMSON[0], CRIMSON[1], CRIMSON[2]);
         }
         g.end();

         // 血雾帷幕（只露出底部 40%）
         float midY = (float)(this.center.y + this.barHeight * 0.5);
         Vector3f vertical = new Vector3f(0.0F, this.barHeight * 0.5F, 0.0F);
         Vec3[] walls = {
            new Vec3(this.center.x, midY, this.center.z - this.half),
            new Vec3(this.center.x, midY, this.center.z + this.half),
            new Vec3(this.center.x - this.half, midY, this.center.z),
            new Vec3(this.center.x + this.half, midY, this.center.z)
         };
         for (int i = 0; i < 4; i++) {
            Vector3f along = i < 2 ? new Vector3f(this.half, 0.0F, 0.0F) : new Vector3f(0.0F, 0.0F, this.half);
            g.begin(Style.CURTAIN, 1.6F + 2.0F * flash, 0.6F, this.seedFloat + i * 7.0F, 0.4F, 0.4F * rise, 0.0F);
            g.quad(frame.rel(walls[i]), along, vertical, DARK[0], DARK[1], DARK[2], strength);
         }
         g.end();
      }

      // 4. 施法者血气螺旋
      Entity caster = frame.level.getEntity(this.casterId);
      Minecraft minecraft = Minecraft.getInstance();
      boolean firstPersonSelf = caster == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
      if (caster != null && strength > 0.0F && !firstPersonSelf) {
         Vec3 feet = caster.getPosition(frame.partialTick);
         float height = caster.getBbHeight();
         g.begin(Style.RIBBON, 3.5F, 0.3F, 6.0F, 6.0F, 8.0F, 0.6F);
         for (int strand = 0; strand < 3; strand++) {
            int n = 20;
            List<Vector3f> points = new ArrayList<>(n + 1);
            float[] widths = new float[n + 1];
            float[] alphas = new float[n + 1];
            for (int i = 0; i <= n; i++) {
               float k = (float)i / n;
               float theta = -a * 0.25F + strand * FxMath.TAU / 3.0F + k * FxMath.TAU * 1.5F;
               float r = 0.9F - 0.3F * k;
               points.add(frame.rel(feet.add(Math.cos(theta) * r, k * height * 1.1, Math.sin(theta) * r)));
               widths[i] = 0.06F;
               alphas[i] = strength * (float)Math.sin(Math.PI * k);
            }
            g.ribbon(points, widths, alphas, CRIMSON[0], CRIMSON[1], CRIMSON[2]);
         }
         g.end();
      }

      // 5. 血雨
      if (!this.drops.isEmpty() && strength > 0.0F) {
         g.begin(Style.RIBBON, 2.6F, 0.2F, 6.0F, 0.0F, 0.0F, 0.4F);
         for (Drop drop : this.drops) {
            Vec3 p = drop.position(frame.partialTick);
            g.streak(frame.rel(p), frame.rel(p.add(0.0, 1.3, 0.0)), 0.035F, CRIMSON[0], CRIMSON[1], CRIMSON[2], strength * 0.8F);
         }
         g.end();
      }
   }

   // ───────────────────────────── B 档全屏通道 ─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      float a = this.age(frame.partialTick);
      float strength = this.strength(a);
      if (strength <= 0.0F) {
         return;
      }
      Vector3f zone = frame.rel(this.center);
      float pulse = this.pulse(a);
      float pulseRadius = pulse >= 0.0F ? this.half * 1.414F * FxMath.easeOutCubic(pulse) : -1.0F;
      Consumer<ShaderInstance> uniforms = shader -> {
         shader.safeGetUniform("Zone").set(zone.x, zone.y, zone.z, this.half);
         shader.safeGetUniform("ZoneExtra").set(this.barHeight + 2.0F, strength, this.seedFloat, pulseRadius);
      };
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.bloodScene, uniforms));
      out.add(new FxPass(FxPass.Kind.VOLUME, () -> FxShaders.bloodVolume, uniforms));
   }

   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      double distance = frame.camera.distanceTo(this.center);
      float att = (float)Math.max(0.0, 1.0 - Math.max(0.0, distance - this.half) / 48.0);
      if (att <= 0.0F) {
         return;
      }
      if (a < 8.0F) {
         float t = 1.0F - a / 8.0F;
         screen.addFlash(0xFF2424, 0.25F * t * t * att);
         screen.addAberration(0.4F * t * att);
      }
      if (this.cameraInside(frame.camera)) {
         float strength = this.strength(a);
         screen.addVignette(0x3A0004, 0.35F * strength);
         float pulse = this.pulse(a);
         if (pulse >= 0.0F && pulse < 0.3F) {
            screen.addAberration(0.25F * (1.0F - pulse / 0.3F) * strength);
         }
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      if (a >= 8.0F) {
         return 0.0F;
      }
      double distance = frame.camera.distanceTo(this.center);
      float att = (float)Math.max(0.0, 1.0 - Math.max(0.0, distance - this.half) / 32.0);
      return 0.3F * (1.0F - a / 8.0F) * att;
   }

   private static final class Drop {
      final double x;
      final double z;
      double y;
      double py;

      Drop(double x, double y, double z) {
         this.x = x;
         this.y = this.py = y;
         this.z = z;
      }

      void tick() {
         this.py = this.y;
         this.y -= 0.9;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.x, this.py + (this.y - this.py) * partialTick, this.z);
      }
   }
}
