package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.annihilation_blade.logic.PhantomJudgementPattern;
import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SE「幻影审判·九剑星轨」专属渲染（持续特效）。剑的本体仍是 SlashBlade 原生召唤剑实体，本类只叠加光效。
 * <p>
 * 意象「星轨」：剑阵是绕施法者运行的星辰，锁定即是点星，审判即是星落。
 * <pre>
 *  搜索期   两道倾斜进动的 3D 星轨环；每把环绕剑拖出彗尾（直接跟踪客户端上真实的剑实体，严丝合缝）；
 *           目标被逐个锁定：星轨射出细线连向目标，头顶亮起旋转四芒星标，瞄准环向目标收缩
 *  审判     剑阵各方位向每个目标上空的裂点架起弧形星桥；裂点处 6 把落剑的起点连成一幅星图；
 *           落剑拖着星光下坠，落地星痕涟漪；星标坠入目标，一道白色审判光线自裂点贯下
 *  星残     插地残留剑底部一点呼吸的柔光，脚下一圈淡淡的审判星环，随残留时间淡出
 * </pre>
 * 剑阵几何与服务端共用 {@link PhantomJudgementPattern}；颜色取自刀色。
 */
@OnlyIn(Dist.CLIENT)
public final class PhantomJudgementFx extends FxInstance {
   private static final int TRAIL = 7;
   private static final float SWORD_SPEED = 2.1F;
   private static final int MAX_SOUNDS = 8;

   private static final float[] STAR = {0.94F, 0.95F, 1.0F};
   private static final float[] LAVENDER = {0.78F, 0.66F, 1.0F};
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);

   private final int casterId;
   private final float scale;
   private final float[] tint;
   private final int swordCount;
   private final int rainSwords;
   private final int searchTicks;
   private final int lingerTicks;
   private final boolean opened;
   private final List<Lock> locks = new ArrayList<>();
   private final List<RainTarget> rain = new ArrayList<>();
   private final Int2ObjectOpenHashMap<Vec3[]> trails = new Int2ObjectOpenHashMap<>();
   private Vec3 orbitFallback;
   private int rainAt = -1;
   private int stopAt = -1;

   public PhantomJudgementFx(ClientLevel level, FxEventPacket packet) {
      super(40);
      this.casterId = packet.casterId();
      this.scale = Math.max(0.4F, Math.min(3.0F, packet.scale()));
      int param = packet.intParam();
      this.swordCount = Math.max(1, param & 0x1F);
      this.rainSwords = Math.max(1, (param >> 5) & 0x1F);
      int kind = (param >> 10) & 0x3;
      this.searchTicks = Math.max(4, (param >> 12) & 0x7F);
      this.lingerTicks = ((param >> 19) & 0x7F) * 2;
      this.tint = decodeColor((int)(packet.seed() & 0xFFFFFFFFL));
      this.orbitFallback = packet.origin();
      this.opened = kind == AnnihilationFxEvents.PHANTOM_SEARCH;
      if (this.opened) {
         int[] ids = packet.entityIds();
         float span = Math.max(1.0F, this.searchTicks - 5.0F);
         for (int i = 0; i < ids.length; i++) {
            float lockAt = 1.5F + (ids.length <= 1 ? 0.0F : span * i / (ids.length - 1));
            Entity entity = level.getEntity(ids[i]);
            if (entity != null) {
               this.locks.add(new Lock(ids[i], lockAt, entity.position().add(0.0, entity.getBbHeight(), 0.0), Math.max(0.6F, entity.getBbHeight() * 0.5F)));
            }
         }
         this.lifetime = this.searchTicks + 12;
      } else {
         this.beginRain(level, packet);
      }
   }

   /** 刀色转浮点；过暗时退回淡紫，并略微提亮。 */
   private static float[] decodeColor(int rgb) {
      float r = (rgb >> 16 & 0xFF) / 255.0F;
      float g = (rgb >> 8 & 0xFF) / 255.0F;
      float b = (rgb & 0xFF) / 255.0F;
      if (r + g + b < 0.3F) {
         return LAVENDER.clone();
      }
      return new float[]{FxMath.lerp(r, 1.0F, 0.25F), FxMath.lerp(g, 1.0F, 0.25F), FxMath.lerp(b, 1.0F, 0.25F)};
   }

   // ───────────────────────────── 持续特效钩子 ─────────────────────────────

   @Override
   public void onSustainedUpdate(ClientLevel level, FxEventPacket event) {
      if (((event.intParam() >> 10) & 0x3) == AnnihilationFxEvents.PHANTOM_RAIN) {
         this.beginRain(level, event);
      }
   }

   @Override
   public void onSustainedStop() {
      if (this.stopAt < 0) {
         this.stopAt = this.age;
      }
      if (this.rainAt < 0) {
         // 被打断：星轨与锁定在 6 tick 内淡出
         this.lifetime = Math.min(this.lifetime, this.age + 6);
      }
   }

   private void beginRain(ClientLevel level, FxEventPacket packet) {
      if (this.rainAt >= 0) {
         return;
      }
      this.rainAt = this.age;
      Vec3 judgementCenter = packet.origin();
      float longest = 0.0F;
      int[] ids = packet.entityIds();
      for (int k = 0; k < ids.length; k++) {
         Entity entity = level.getEntity(ids[k]);
         if (entity == null) {
            continue;
         }
         RainTarget target = new RainTarget(k, entity, judgementCenter);
         longest = Math.max(longest, target.longestFall);
         this.rain.add(target);
      }
      this.lifetime = (int)Math.ceil(this.rainAt + longest + this.lingerTicks + 8.0F);
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public Vec3 focus() {
      return this.orbitFallback;
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }

   @Override
   public void tick(ClientLevel level) {
      Entity caster = level.getEntity(this.casterId);
      if (caster != null) {
         this.orbitFallback = PhantomJudgementPattern.orbitCenter(caster.position(), caster.getBbHeight());
      }
      int age = this.ageTicks();
      // 彗尾：跟踪环绕施法者的原生召唤剑
      if (this.rainAt < 0) {
         double reach = PhantomJudgementPattern.orbitRadius(0, this.scale) + 2.5;
         AABB box = new AABB(this.orbitFallback, this.orbitFallback).inflate(reach);
         List<EntityAbstractSummonedSword> swords = level.getEntitiesOfClass(EntityAbstractSummonedSword.class, box);
         for (EntityAbstractSummonedSword sword : swords) {
            Vec3[] history = this.trails.computeIfAbsent(sword.getId(), ignored -> new Vec3[TRAIL]);
            System.arraycopy(history, 0, history, 1, TRAIL - 1);
            history[0] = sword.position();
         }
      }
      for (Lock lock : this.locks) {
         Entity entity = level.getEntity(lock.entityId);
         if (entity != null && entity.isAlive()) {
            lock.top = entity.position().add(0.0, entity.getBbHeight(), 0.0);
         }
      }

      int stride = Math.max(1, this.locks.size() / MAX_SOUNDS);
      for (int i = 0; i < this.locks.size(); i += stride) {
         if ((int)Math.floor(this.locks.get(i).lockAt) == age && this.rainAt < 0) {
            play(level, this.locks.get(i).top, SoundEvents.AMETHYST_BLOCK_HIT, 0.4F, 1.6F + 0.04F * i);
         }
      }
      if (this.rainAt >= 0 && !this.rain.isEmpty()) {
         if (age == this.rainAt + 1) {
            play(level, this.rain.get(0).split, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.3F);
         }
         int rainStride = Math.max(1, this.rain.size() / MAX_SOUNDS);
         for (int k = 0; k < this.rain.size(); k += rainStride) {
            RainTarget target = this.rain.get(k);
            if ((int)Math.floor(this.rainAt + target.firstFall) == age) {
               play(level, target.feet, SoundEvents.AMETHYST_CLUSTER_PLACE, 0.4F, 1.5F);
            }
         }
      }
   }

   private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      for (Lock lock : this.locks) {
         if ((int)Math.floor(lock.lockAt) == age && this.rainAt < 0) {
            for (int i = 0; i < 4; i++) {
               level.addParticle(ParticleTypes.ENCHANT, lock.top.x, lock.top.y + 0.5, lock.top.z, 0.0, -0.2, 0.0);
            }
         }
      }
      if (this.rainAt >= 0) {
         for (RainTarget target : this.rain) {
            for (int i = 0; i < target.ends.length; i++) {
               if ((int)Math.floor(this.rainAt + target.fall[i]) == age) {
                  level.addParticle(ParticleTypes.END_ROD, target.ends[i].x, target.ends[i].y + 0.1, target.ends[i].z, 0.0, 0.03, 0.0);
               }
            }
         }
      }
   }

   // ───────────────────────────── 时间轴 ─────────────────────────────

   private float searchPresence(float t) {
      float in = this.opened ? FxMath.window(t, 0.0F, 4.0F) : 0.0F;
      if (this.rainAt >= 0) {
         return in * (1.0F - FxMath.window(t, this.rainAt, this.rainAt + 4.0F));
      }
      if (this.stopAt >= 0) {
         return in * (1.0F - FxMath.window(t, this.stopAt, this.stopAt + 6.0F));
      }
      return in;
   }

   private Vec3 orbitCenter(FxFrame frame) {
      Entity caster = frame.level.getEntity(this.casterId);
      return caster != null
         ? PhantomJudgementPattern.orbitCenter(caster.getPosition(frame.partialTick), caster.getBbHeight())
         : this.orbitFallback;
   }

   private static float nearFade(Vector3f rel) {
      return FxMath.window(rel.length(), 1.0F, 3.0F);
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float t = this.age(frame.partialTick);
      float presence = this.searchPresence(t);
      Vec3 orbit = this.orbitCenter(frame);
      Vector3f oc = frame.rel(orbit);
      float radius = (float)PhantomJudgementPattern.orbitRadius(this.ageTicks(), this.scale);

      // 1. 星轨环
      if (presence > 0.01F) {
         g.begin(Style.RIBBON, 3.5F, 0.3F, 6.0F, 0.0F, 0.0F, 0.5F);
         float precess = t * 0.05F;
         Vector3f n1 = new Vector3f((float)Math.sin(precess) * 0.35F, 1.0F, (float)Math.cos(precess) * 0.35F).normalize();
         Vector3f n2 = new Vector3f((float)-Math.cos(precess) * 0.42F, 1.0F, (float)Math.sin(precess) * 0.42F).normalize();
         g.torus(new Vector3f(oc).add(0.0F, 0.25F, 0.0F), n1, radius * 1.02F, 0.025F * this.scale, 48, 4, t * 0.02F, this.tint[0], this.tint[1], this.tint[2], presence * 0.85F);
         g.torus(new Vector3f(oc).add(0.0F, 0.25F, 0.0F), n2, radius * 1.18F, 0.018F * this.scale, 48, 4, -t * 0.02F, STAR[0], STAR[1], STAR[2], presence * 0.55F);
      }

      // 2. 彗尾、锁定细线、星桥、落剑星光、星图连线、审判光线
      g.begin(Style.RIBBON, 5.0F, 0.2F, 6.0F, 0.0F, 0.0F, 0.4F);
      if (presence > 0.01F) {
         for (Int2ObjectOpenHashMap.Entry<Vec3[]> entry : this.trails.int2ObjectEntrySet()) {
            this.drawTrail(g, frame, entry.getIntKey(), entry.getValue(), presence);
         }
         for (Lock lock : this.locks) {
            float e = t - lock.lockAt;
            if (e < 0.0F) {
               continue;
            }
            float reveal = FxMath.easeOutCubic(FxMath.window(e, 0.0F, 0.8F));
            float alpha = presence * (0.25F + 0.75F * (1.0F - FxMath.window(e, 1.0F, 5.0F)));
            Vec3 toTarget = new Vec3(lock.top.x - orbit.x, 0.0, lock.top.z - orbit.z);
            toTarget = toTarget.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : toTarget.normalize();
            Vec3 from = orbit.add(toTarget.scale(radius)).add(0.0, 0.25, 0.0);
            Vec3 to = from.lerp(lock.top.add(0.0, 0.3, 0.0), reveal);
            Vector3f a = frame.rel(from);
            Vector3f b = frame.rel(to);
            g.ribbon(List.of(a, b), new float[]{0.018F * this.scale, 0.012F * this.scale}, new float[]{alpha * nearFade(a), alpha}, STAR[0], STAR[1], STAR[2]);
         }
      }
      if (this.rainAt >= 0) {
         float e = t - this.rainAt;
         for (RainTarget target : this.rain) {
            this.drawBridge(g, frame, target, e);
            this.drawConstellation(g, frame, target, e);
            this.drawFalling(g, frame, target, e);
            if (e >= 0.6F && e < 2.6F) {
               float k = 1.0F - FxMath.window(e, 0.6F, 2.6F);
               Vector3f a = frame.rel(target.split);
               Vector3f b = frame.rel(target.center);
               g.ribbon(List.of(a, b), new float[]{0.05F * this.scale, 0.09F * this.scale}, new float[]{k * 0.6F, k * nearFade(b)}, STAR[0], STAR[1], STAR[2]);
            }
         }
      }

      // 3. 瞄准环、落地星痕、审判星环
      g.begin(Style.RING, 3.5F, 0.3F, 0.95F, 0.035F, 4.0F, 0.0F);
      if (presence > 0.01F) {
         for (Lock lock : this.locks) {
            float e = t - lock.lockAt;
            if (e < 0.0F) {
               continue;
            }
            float shrink = FxMath.easeOutCubic(FxMath.window(e, 0.0F, 3.0F));
            float r = lock.size * (1.4F - 0.8F * shrink) * 1.6F / 0.95F;
            float alpha = presence * (0.3F + 0.7F * (1.0F - FxMath.window(e, 3.0F, 6.0F)));
            float spin = t * 0.15F + lock.lockAt;
            Vector3f c = frame.rel(lock.top.add(0.0, -lock.size, 0.0));
            Vector3f u = new Vector3f(frame.left).mul((float)Math.cos(spin)).add(new Vector3f(frame.up).mul((float)Math.sin(spin))).mul(r);
            Vector3f v = new Vector3f(frame.up).mul((float)Math.cos(spin)).sub(new Vector3f(frame.left).mul((float)Math.sin(spin))).mul(r);
            g.quad(c, u, v, this.tint[0], this.tint[1], this.tint[2], alpha * nearFade(c));
         }
      }
      if (this.rainAt >= 0) {
         float e = t - this.rainAt;
         g.begin(Style.RING, 3.5F, 0.3F, 0.95F, 0.03F, 0.0F, 0.0F);
         for (RainTarget target : this.rain) {
            for (int i = 0; i < target.ends.length; i++) {
               float land = e - target.fall[i];
               if (land < 0.0F || land >= 4.0F) {
                  continue;
               }
               float k = land / 4.0F;
               float r = (0.15F + 0.75F * this.scale * FxMath.easeOutCubic(k)) / 0.95F;
               Vector3f c = frame.rel(target.ends[i]);
               g.quad(c, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), STAR[0], STAR[1], STAR[2], (1.0F - k) * nearFade(c));
            }
         }
         g.begin(Style.RING, 2.0F, 0.0F, 0.95F, 0.02F, 6.0F, 0.12F);
         float circle = FxMath.window(e, 1.0F, 3.0F) * (1.0F - FxMath.window(e, this.lingerTicks - 8.0F, this.lingerTicks + 4.0F));
         if (circle > 0.0F) {
            for (RainTarget target : this.rain) {
               float r = (float)target.rainRadius * 1.08F / 0.95F;
               Vector3f c = frame.rel(target.feet.add(0.0, 0.06, 0.0));
               g.quad(c, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), this.tint[0], this.tint[1], this.tint[2], circle * 0.7F * nearFade(c));
            }
         }
      }

      // 4. 锁定星标（四芒星精灵）
      g.begin(Style.SPRITE, 6.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F);
      for (Lock lock : this.locks) {
         float e = t - lock.lockAt;
         if (e < 0.0F) {
            continue;
         }
         float in = FxMath.easeOutCubic(FxMath.window(e, 0.0F, 1.0F));
         float drop = 0.0F;
         float alpha = in * Math.max(presence, this.rainAt >= 0 ? 1.0F : 0.0F);
         if (this.rainAt >= 0) {
            float r = t - this.rainAt;
            drop = FxMath.easeInCubic(FxMath.window(r, 0.3F, 1.2F));
            alpha *= 1.0F - FxMath.window(r, 1.0F, 1.6F);
         }
         if (alpha <= 0.0F) {
            continue;
         }
         Vector3f p = frame.rel(lock.top.add(0.0, 0.6 - (0.6 + lock.size) * drop, 0.0));
         float pulse = 0.85F + 0.15F * (float)Math.sin(t * 0.6F + lock.lockAt);
         g.billboard(p, 0.32F * this.scale * pulse * (1.0F + drop), t * 0.08F, this.tint[0], this.tint[1], this.tint[2], alpha * nearFade(p));
         g.billboard(p, 0.14F * this.scale, -t * 0.12F, STAR[0], STAR[1], STAR[2], alpha * nearFade(p));
      }

      // 5. 公告板：彗头星芒、星图星点、落地闪光、审判白闪、残留剑柔光
      g.begin(Style.GLOW, 6.5F, 0.0F, 4.0F, 2.6F, 0.4F, 0.0F);
      if (presence > 0.01F && this.rainAt < 0) {
         for (Int2ObjectOpenHashMap.Entry<Vec3[]> entry : this.trails.int2ObjectEntrySet()) {
            Entity sword = frame.level.getEntity(entry.getIntKey());
            if (sword != null && moving(entry.getValue())) {
               Vector3f p = frame.rel(sword.getPosition(frame.partialTick));
               g.billboard(p, 0.28F * this.scale, 0.0F, STAR[0], STAR[1], STAR[2], presence * nearFade(p));
            }
         }
      }
      if (this.rainAt >= 0) {
         float e = t - this.rainAt;
         for (RainTarget target : this.rain) {
            float stars = FxMath.window(e, 0.5F, 1.5F) * (1.0F - FxMath.window(e, 4.0F, 7.0F));
            if (stars > 0.0F) {
               for (Vec3 start : target.starts) {
                  Vector3f p = frame.rel(start);
                  g.billboard(p, 0.3F * this.scale, 0.0F, STAR[0], STAR[1], STAR[2], stars * nearFade(p));
               }
            }
            for (int i = 0; i < target.ends.length; i++) {
               float land = e - target.fall[i];
               if (land >= 0.0F && land < 2.0F) {
                  Vector3f p = frame.rel(target.ends[i].add(0.0, 0.2, 0.0));
                  float k = 1.0F - land / 2.0F;
                  g.billboard(p, 0.7F * this.scale, 0.0F, this.tint[0], this.tint[1], this.tint[2], k * k * nearFade(p));
               }
               float linger = land;
               if (linger >= 0.0F && linger < this.lingerTicks) {
                  float fade = FxMath.window(linger, 0.0F, 2.0F) * (1.0F - FxMath.window(linger, this.lingerTicks - 10.0F, this.lingerTicks));
                  float breath = 0.35F + 0.25F * (float)Math.sin(t * 0.3F + i * 1.3F + target.index);
                  Vector3f p = frame.rel(target.ends[i].add(0.0, 0.25, 0.0));
                  g.billboard(p, 0.45F * this.scale, 0.0F, this.tint[0], this.tint[1], this.tint[2], breath * fade * nearFade(p));
               }
            }
            if (e >= 1.2F && e < 4.2F) {
               float k = 1.0F - (e - 1.2F) / 3.0F;
               Vector3f p = frame.rel(target.center);
               g.billboard(p, 1.3F * target.size * this.scale, 0.0F, STAR[0], STAR[1], STAR[2], k * k * nearFade(p));
            }
         }
      }
      g.end();
   }

   /** 只给正在运动的剑画彗尾（排除附近静止的残留剑等）。 */
   private static boolean moving(Vec3[] history) {
      int last = 0;
      while (last + 1 < history.length && history[last + 1] != null && last < 3) {
         last++;
      }
      return last > 0 && history[0] != null && history[0].distanceToSqr(history[last]) > 0.0025;
   }

   private void drawTrail(FxGeometry g, FxFrame frame, int entityId, Vec3[] history, float presence) {
      Entity sword = frame.level.getEntity(entityId);
      if (sword == null || history[1] == null || !moving(history)) {
         return;
      }
      List<Vector3f> points = new ArrayList<>(TRAIL + 1);
      points.add(frame.rel(sword.getPosition(frame.partialTick)));
      for (Vec3 p : history) {
         if (p == null) {
            break;
         }
         points.add(frame.rel(p));
      }
      if (points.size() < 3) {
         return;
      }
      int n = points.size();
      float[] widths = new float[n];
      float[] alphas = new float[n];
      for (int i = 0; i < n; i++) {
         float k = 1.0F - (float)i / (n - 1);
         widths[i] = 0.08F * this.scale * k;
         alphas[i] = presence * k * nearFade(points.get(i));
      }
      g.ribbon(points, widths, alphas, this.tint[0], this.tint[1], this.tint[2]);
   }

   /** 星桥：剑阵方位 → 裂点的弧形光桥，2 tick 内架起。 */
   private void drawBridge(FxGeometry g, FxFrame frame, RainTarget target, float e) {
      if (e < 0.0F || e >= 8.0F) {
         return;
      }
      float reveal = FxMath.easeOutCubic(FxMath.window(e, 0.0F, 2.0F));
      float fade = 1.0F - FxMath.window(e, 4.0F, 8.0F);
      Vec3 a = target.source;
      Vec3 b = target.split;
      Vec3 control = a.lerp(b, 0.5).add(0.0, 1.5 + a.distanceTo(b) * 0.15, 0.0);
      int n = 16;
      int count = Math.max(2, (int)Math.ceil(n * reveal));
      List<Vector3f> points = new ArrayList<>(count);
      float[] widths = new float[count];
      float[] alphas = new float[count];
      for (int i = 0; i < count; i++) {
         double k = (double)i / (n - 1);
         Vec3 p = a.scale((1 - k) * (1 - k)).add(control.scale(2 * k * (1 - k))).add(b.scale(k * k));
         Vector3f rel = frame.rel(p);
         points.add(rel);
         widths[i] = 0.05F * this.scale;
         alphas[i] = fade * nearFade(rel) * (i == count - 1 ? 0.4F : 1.0F);
      }
      g.ribbon(points, widths, alphas, this.tint[0], this.tint[1], this.tint[2]);
   }

   /** 星图：落剑起点连成的星形（每隔一点相连）与外圈。 */
   private void drawConstellation(FxGeometry g, FxFrame frame, RainTarget target, float e) {
      float alpha = FxMath.window(e, 0.5F, 2.0F) * (1.0F - FxMath.window(e, 4.0F, 7.0F));
      if (alpha <= 0.0F) {
         return;
      }
      int n = target.starts.length;
      int step = n >= 5 ? 2 : 1;
      for (int i = 0; i < n; i++) {
         Vector3f a = frame.rel(target.starts[i]);
         Vector3f inner = frame.rel(target.starts[(i + step) % n]);
         Vector3f outer = frame.rel(target.starts[(i + 1) % n]);
         float fade = alpha * nearFade(a);
         g.ribbon(List.of(a, inner), new float[]{0.02F * this.scale, 0.02F * this.scale}, new float[]{fade, fade}, LAVENDER[0], LAVENDER[1], LAVENDER[2]);
         g.ribbon(List.of(a, outer), new float[]{0.012F * this.scale, 0.012F * this.scale}, new float[]{fade * 0.5F, fade * 0.5F}, STAR[0], STAR[1], STAR[2]);
      }
   }

   /** 落剑星光：与原生召唤剑同速（2.1 格 / tick）自起点落向插地点。 */
   private void drawFalling(FxGeometry g, FxFrame frame, RainTarget target, float e) {
      for (int i = 0; i < target.starts.length; i++) {
         float p = e / target.fall[i];
         if (p < 0.0F || p >= 1.15F) {
            continue;
         }
         Vec3 head = target.starts[i].lerp(target.ends[i], Math.min(1.0F, p));
         Vec3 dir = target.ends[i].subtract(target.starts[i]).normalize();
         Vec3 tail = head.subtract(dir.scale(1.4 * this.scale));
         Vector3f rel = frame.rel(head);
         float fade = p > 1.0F ? 1.0F - (p - 1.0F) / 0.15F : 1.0F;
         g.streak(rel, frame.rel(tail), 0.07F * this.scale, STAR[0], STAR[1], STAR[2], fade * nearFade(rel));
      }
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private static final class Lock {
      final int entityId;
      final float lockAt;
      final float size;
      Vec3 top;

      Lock(int entityId, float lockAt, Vec3 top, float size) {
         this.entityId = entityId;
         this.lockAt = lockAt;
         this.top = top;
         this.size = size;
      }
   }

   private final class RainTarget {
      final int index;
      final Vec3 feet;
      final Vec3 center;
      final float size;
      final Vec3 split;
      final Vec3 source;
      final double rainRadius;
      final Vec3[] starts;
      final Vec3[] ends;
      final float[] fall;
      final float longestFall;
      final float firstFall;

      RainTarget(int index, Entity entity, Vec3 judgementCenter) {
         PhantomJudgementFx fx = PhantomJudgementFx.this;
         this.index = index;
         this.feet = entity.position();
         float height = entity.getBbHeight();
         this.center = this.feet.add(0.0, height * 0.5, 0.0);
         this.size = Math.max(0.6F, height * 0.5F);
         this.split = PhantomJudgementPattern.split(this.center, height, fx.scale);
         this.source = PhantomJudgementPattern.source(judgementCenter, index, fx.swordCount, fx.scale);
         this.rainRadius = PhantomJudgementPattern.rainRadius(entity.getBbWidth(), fx.scale);
         this.starts = new Vec3[fx.rainSwords];
         this.ends = new Vec3[fx.rainSwords];
         this.fall = new float[fx.rainSwords];
         float longest = 0.0F;
         float first = Float.MAX_VALUE;
         for (int i = 0; i < fx.rainSwords; i++) {
            Vec3 offset = PhantomJudgementPattern.rainOffset(i, fx.rainSwords, index, this.rainRadius);
            this.starts[i] = PhantomJudgementPattern.rainStart(this.split, offset, i);
            this.ends[i] = PhantomJudgementPattern.rainEnd(this.feet, offset);
            this.fall[i] = Math.max(0.8F, Math.min(6.0F, (float)this.starts[i].distanceTo(this.ends[i]) / SWORD_SPEED));
            longest = Math.max(longest, this.fall[i]);
            first = Math.min(first, this.fall[i]);
         }
         this.longestFall = longest;
         this.firstFall = first;
      }
   }
}
