package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
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
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SE「断空·瞬狱终闪」专属渲染（持续特效，旗舰级）。
 * <p>
 * 意象「居合」：拔刀的每一闪只在空间里留下一道静止的直线和一个全息残影；直到收刀，世界才被一齐斩开。
 * <pre>
 *  开场          起点竖起「空间断面门」（本体层暗缝 + 冰青描边），张开后收窄为常驻的锚缝
 *  每次闪现      瞬步光柱 + 速度线贯穿出发点与落点；目标身上一道直线「一闪」：本体层暗色断缝 + 白热刃心 + 冰青柔光，
 *                1.2 tick 划完；切口处空间涟漪双环、白闪、冰青火花，画面沿切口局部错位一下；
 *                离开的位置留下施法者的全息残影（模型逐面棱边发光，冰青→虚空紫渐变，扫描线流动）
 *  納刀 s=0      鞘鸣。时停：世界去色压暗，所有斩痕延长成横贯画面的切线，残影与斩痕同时绽亮并爆出十字星芒
 *  納刀 s=4      齐斩：画面沿每条切线错位滑开、切缝露出虚空暗隙（场景通道），色差 + 速度线 + 过曝颗粒闪，
 *                起点劈下收刀竖线并扩散地面冲击环
 *  納刀 s=6~16   切缝合拢；斩痕由远到近碎成冰晶细屑，残影随之自下而上烧蚀溶解
 * </pre>
 * 刻意只用直线：与次元斩的暗芯弧形刀弧、血狱闪击的血痕区分开。
 */
@OnlyIn(Dist.CLIENT)
public final class DankongFx extends FxInstance {
   // ───── 纳刀时间轴（相对 STOP 的 tick）─────
   private static final float FREEZE = 4.0F;
   private static final float SLIDE_PEAK = 6.0F;
   private static final float SLIDE_HOLD = 8.0F;
   private static final float SLIDE_END = 11.0F;
   private static final float SHATTER_BASE = 6.0F;
   private static final float SHATTER_SPREAD = 5.0F;
   private static final float SHATTER_LIFE = 9.0F;
   private static final float GHOST_DISSOLVE = 5.0F;
   private static final int EPILOGUE = 34;

   private static final float ANCHOR = 0.22F;
   private static final int SHARDS = 7;
   private static final int SPARKS = 8;
   private static final int SPEED_LINES = 8;
   private static final int MAX_GHOSTS = 24;
   private static final int MAX_SLICES = 8;
   private static final int SHATTER_SOUNDS = 6;
   private static final int SPRAY = 12;
   private static final int SUB_SLASHES = 2;
   private static final int MAX_SPLITS = 32;
   private static final float MARK_SWEEP = 6.0F;
   private static final float SLIP_MAX = 0.022F;
   private static final float LOCAL_SLIP = 0.007F;
   private static final double SCREEN_RANGE = 40.0;

   private static final float[] ICE = {0.55F, 0.92F, 1.0F};
   private static final float[] PALE = {0.82F, 0.96F, 1.0F};
   private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
   private static final float[] VOID = {0.62F, 0.28F, 1.0F};
   private static final float[] DEEP = {0.015F, 0.02F, 0.06F};

   private final Vec3 origin;
   private final float scale;
   private final int casterId;
   private final boolean opened;
   private final RandomSource random;
   private final List<Segment> segments = new ArrayList<>();
   private final List<Cut> cuts = new ArrayList<>();
   private final List<Ghost> ghosts = new ArrayList<>();
   private final List<Mark> marks = new ArrayList<>();
   private final Vector3f q0 = new Vector3f();
   private final Vector3f q1 = new Vector3f();
   private final Vector3f q2 = new Vector3f();
   private final Vector3f q3 = new Vector3f();
   private Vec3 lastPoint;
   private float maxDistance = 1.0F;
   private int sustainDuration = Integer.MAX_VALUE;
   private int stopAt = -1;

   public DankongFx(ClientLevel level, FxEventPacket packet) {
      super(40);
      this.scale = Math.max(0.4F, Math.min(3.0F, packet.scale()));
      this.casterId = packet.casterId();
      this.random = RandomSource.create(packet.seed());
      boolean open = (packet.intParam() & 0xF) == AnnihilationFxEvents.DANKONG_OPEN;
      this.opened = open;
      // 中途进入范围的玩家收到的是最近一次闪现事件：从该次闪现的出发点开始记录
      this.origin = open ? packet.origin() : packet.origin().subtract(packet.direction());
      this.lastPoint = this.origin;
      if (open) {
         Vec3 look = packet.direction();
         this.ghosts.add(new Ghost(this.origin, yawOf(look), 0.0F, 0.0F, 0.0F));
         // 死兆星：从近到远依次亮起
         float far = 1.0F;
         for (int id : packet.entityIds()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
               Mark mark = new Mark(id, entity, this.origin);
               far = Math.max(far, mark.distance);
               this.marks.add(mark);
            }
         }
         for (Mark mark : this.marks) {
            mark.delay = 1.0F + mark.distance / far * MARK_SWEEP;
         }
      } else {
         this.addStrike(level, packet);
      }
   }

   // ───────────────────────────── 持续特效钩子 ─────────────────────────────

   @Override
   protected void onSustainedStart(int duration) {
      this.sustainDuration = duration;
      this.lifetime = Math.max(this.lifetime, duration + EPILOGUE);
   }

   @Override
   public void onSustainedUpdate(ClientLevel level, FxEventPacket event) {
      if ((event.intParam() & 0xF) == AnnihilationFxEvents.DANKONG_STRIKE && this.stopAt < 0) {
         this.addStrike(level, event);
      }
   }

   @Override
   public void onSustainedStop() {
      this.beginSheathe();
   }

   private void beginSheathe() {
      if (this.stopAt >= 0) {
         return;
      }
      this.stopAt = this.age;
      this.lifetime = this.age + EPILOGUE;
      // 最后一个落点的残影在施法者离开（回到起点）时显现
      for (Ghost ghost : this.ghosts) {
         if (ghost.visibleAt < 0 && ghost != this.originGhost()) {
            ghost.visibleAt = this.age;
         }
      }
   }

   private Ghost originGhost() {
      return this.opened && !this.ghosts.isEmpty() ? this.ghosts.get(0) : null;
   }

   private void addStrike(ClientLevel level, FxEventPacket packet) {
      Vec3 to = packet.origin();
      Vec3 from = this.lastPoint;
      if (from.distanceToSqr(to.subtract(packet.direction())) > 4.0) {
         from = to.subtract(packet.direction());
      }
      this.lastPoint = to;
      this.segments.add(new Segment(from, to, this.age, (float)from.lerp(to, 0.5).distanceTo(this.origin), this.random));

      int[] ids = packet.entityIds();
      Entity target = ids.length > 0 ? level.getEntity(ids[0]) : null;
      Vec3 blink = to.subtract(from);
      Vec3 center;
      float height;
      if (target != null) {
         center = target.getBoundingBox().getCenter();
         height = target.getBbHeight();
      } else {
         Vec3 forward = blink.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : blink.normalize();
         center = to.add(forward.scale(1.8));
         height = Math.max(1.0F, packet.radius());
      }
      float distance = (float)center.distanceTo(this.origin);
      Cut cut = new Cut(center, blink, height, this.scale, this.age, distance, this.random);
      if (target instanceof LivingEntity living) {
         cut.attach(living, AfterimageCapture.captureLiving(living));
      }
      this.cuts.add(cut);
      this.maxDistance = Math.max(this.maxDistance, distance);
      if (target != null) {
         for (Mark mark : this.marks) {
            if (mark.entityId == target.getId() && mark.struckAt < 0) {
               mark.struckAt = this.age;
               mark.top = target.getBoundingBox().getCenter().add(0.0, target.getBbHeight() * 0.5, 0.0);
            }
         }
      }

      // 上一个位置的残影在施法者离开时显现；本次落点记录一个新的残影（下次闪现或收刀时显现）
      for (Ghost ghost : this.ghosts) {
         if (ghost.visibleAt < 0) {
            ghost.visibleAt = this.age;
         }
      }
      Vec3 face = center.subtract(to);
      this.ghosts.add(new Ghost(to, yawOf(face), pitchOf(face), 0.32F, distance));
   }

   private static float yawOf(Vec3 direction) {
      return (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
   }

   private static float pitchOf(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float)-Math.toDegrees(Math.atan2(direction.y, horizontal));
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public Vec3 focus() {
      return this.origin;
   }

   @Override
   public double cullDistance() {
      return 224.0;
   }

   @Override
   public void tick(ClientLevel level) {
      // 服务端未能送达 STOP（例如持续时间先到期）时，到点自行收刀
      if (this.stopAt < 0 && this.age >= this.sustainDuration) {
         this.beginSheathe();
      }
      this.captureGhosts(level);
      this.playSounds(level);
      for (Mark mark : this.marks) {
         if (mark.struckAt < 0) {
            Entity entity = level.getEntity(mark.entityId);
            if (entity != null && entity.isAlive()) {
               mark.top = entity.getBoundingBox().getCenter().add(0.0, entity.getBbHeight() * 0.5, 0.0);
            }
         }
      }
   }

   private void captureGhosts(ClientLevel level) {
      Entity caster = level.getEntity(this.casterId);
      if (!(caster instanceof AbstractClientPlayer player)) {
         return;
      }
      for (Ghost ghost : this.ghosts) {
         if (!ghost.captureTried) {
            ghost.captureTried = true;
            ghost.height = player.getBbHeight();
            ghost.quads = AfterimageCapture.capture(player, ghost.yaw, ghost.pitch, ghost.attackTime);
         }
      }
   }

   private void playSounds(ClientLevel level) {
      for (Cut cut : this.cuts) {
         if (cut.at == this.age) {
            play(level, cut.center, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6F, 1.6F + this.random.nextFloat() * 0.4F);
         }
      }
      if (this.stopAt < 0) {
         return;
      }
      int s = this.age - this.stopAt;
      if (s == 0) {
         // 鞘鸣
         play(level, this.origin, SoundEvents.CROSSBOW_LOADING_END, 1.0F, 1.35F);
         play(level, this.origin, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.9F);
      } else if (s == (int)FREEZE) {
         // 齐斩
         play(level, this.origin, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.7F);
         play(level, this.origin, SoundEvents.GLASS_BREAK, 1.0F, 0.6F);
         play(level, this.origin, SoundEvents.TRIDENT_RIPTIDE_1, 0.9F, 1.6F);
         play(level, this.origin, SoundEvents.GENERIC_EXPLODE, 0.35F, 1.8F);
      }
      // 碎散：均匀抽取若干道斩痕，在各自碎裂时刻叮当作响
      int stride = Math.max(1, this.cuts.size() / SHATTER_SOUNDS);
      for (int i = 0; i < this.cuts.size(); i += stride) {
         Cut cut = this.cuts.get(i);
         if ((int)Math.floor(this.shatterTime(cut.distance)) == s) {
            play(level, cut.center, SoundEvents.AMETHYST_BLOCK_BREAK, 0.5F, 1.5F + this.random.nextFloat() * 0.4F);
         }
      }
   }

   private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      for (Cut cut : this.cuts) {
         if (cut.at == this.age) {
            level.addParticle(ParticleTypes.SWEEP_ATTACK, cut.center.x, cut.center.y, cut.center.z, 0.0, 0.0, 0.0);
            for (int i = 0; i < 4; i++) {
               Vec3 p = cut.a.lerp(cut.b, (i + 0.5) / 4.0);
               level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0.0, 0.0, 0.0);
            }
         }
      }
      if (this.stopAt == this.age) {
         for (int i = 0; i < 12; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, this.origin.x, this.origin.y, this.origin.z,
               (this.random.nextDouble() - 0.5) * 0.4, (this.random.nextDouble() - 0.5) * 0.4, (this.random.nextDouble() - 0.5) * 0.4);
         }
      }
   }

   // ───────────────────────────── 时间轴 ─────────────────────────────

   /** 納刀后经过的 tick；未收刀时为负。 */
   private float sheathe(float partialTick) {
      return this.stopAt < 0 ? -1.0F : this.age + partialTick - this.stopAt;
   }

   /** 由远到近：离起点越远的斩痕越早碎（相对 STOP）。 */
   private float shatterTime(float distance) {
      return SHATTER_BASE + (1.0F - FxMath.clamp01(distance / this.maxDistance)) * SHATTER_SPREAD;
   }

   /** 齐斩错位曲线：时停期为 0，s=4 起 2 tick 推开，保持 2 tick，再 3 tick 合拢。 */
   private static float slide(float s) {
      if (s < FREEZE) {
         return 0.0F;
      }
      if (s < SLIDE_PEAK) {
         return FxMath.easeOutCubic((s - FREEZE) / (SLIDE_PEAK - FREEZE));
      }
      if (s < SLIDE_HOLD) {
         return 1.0F;
      }
      return 1.0F - FxMath.easeInCubic(FxMath.window(s, SLIDE_HOLD, SLIDE_END));
   }

   /** 时停强度：s=0 起 0.6 tick 内进入，齐斩后 1.5 tick 内解除。 */
   private static float freeze(float s) {
      if (s < 0.0F) {
         return 0.0F;
      }
      if (s < FREEZE) {
         return FxMath.easeOutCubic(FxMath.window(s, 0.0F, 0.6F));
      }
      return 1.0F - FxMath.window(s, FREEZE, FREEZE + 1.5F);
   }

   /** 断面门宽度系数：开场张开后收窄为锚缝；納刀时再次张开，齐斩时合拢。 */
   private float gateLevel(float a, float s) {
      float level = 0.0F;
      if (this.opened) {
         if (a < 2.0F) {
            level = FxMath.easeOutCubic(a / 2.0F);
         } else if (a < 4.0F) {
            level = 1.0F;
         } else {
            level = FxMath.lerp(1.0F, ANCHOR, FxMath.easeInCubic(FxMath.window(a, 4.0F, 7.0F)));
         }
      }
      if (s >= 0.0F) {
         float base = this.opened ? ANCHOR : 0.0F;
         if (s < 1.0F) {
            level = FxMath.lerp(base, 1.0F, FxMath.easeOutCubic(s));
         } else if (s < FREEZE) {
            level = 1.0F;
         } else {
            level = 1.0F - FxMath.easeInCubic(FxMath.window(s, FREEZE, FREEZE + 2.0F));
         }
      }
      return level;
   }

   /** 靠近相机处淡出（施法者第一人称时，路径、断面门、残影不会糊在脸上）。 */
   private static float nearFade(Vector3f rel) {
      return FxMath.window(rel.length(), 0.9F, 2.6F);
   }

   /**
    * 斩痕专用的近处淡出：施法者每次闪现都站在目标身后约 1.8 格，正对一闪；
    * 不额外压暗的话，每 3 tick 就有一道白线横贯整个屏幕。旁观者距离较远，不受影响。
    */
   private static float cutFade(Vector3f rel) {
      return 0.18F + 0.82F * FxMath.window(rel.length(), 1.4F, 4.5F);
   }

   // ───────────────────────────── 本体层 ─────────────────────────────

   @Override
   public boolean hasBody() {
      return true;
   }

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float s = this.sheathe(frame.partialTick);

      b.begin(Style.RIBBON, 0.9F, 0.2F, 2.5F, 0.0F, 0.0F, 0.6F);
      // 断面门暗缝
      float gate = this.gateLevel(a, s);
      if (gate > 0.0F) {
         GatePoints points = this.gatePoints(frame, gate, 1.0F);
         float[] alphas = new float[points.points.size()];
         for (int i = 0; i < alphas.length; i++) {
            alphas[i] = 0.95F * nearFade(points.points.get(i));
         }
         b.ribbon(points.points, points.widths, alphas, DEEP[0], DEEP[1], DEEP[2]);
      }
      // 斩痕暗色断缝：刚斩出时宽而深，随后收窄为悬空的暗线，直到碎散
      for (Cut cut : this.cuts) {
         float t = a - cut.at;
         if (t < 0.0F || (s >= 0.0F && s >= this.shatterTime(cut.distance))) {
            continue;
         }
         float fresh = 1.0F - FxMath.window(t, 0.5F, 5.0F);
         float width = (0.05F + 0.06F * fresh) * this.scale;
         float alpha = 0.45F + 0.45F * fresh;
         if (s >= 0.0F) {
            width = 0.12F * this.scale;
            alpha = 0.9F;
         }
         this.line(b, frame, cut.a, cut.revealEnd(t), width, alpha, DEEP, true);
         for (int k = 0; k < SUB_SLASHES; k++) {
            float st = t - cut.subDelay[k];
            if (st >= 0.0F) {
               this.line(b, frame, cut.subA[k], cut.subEnd(k, st), width * 0.7F, alpha * 0.85F, DEEP, true);
            }
         }
      }
      b.end();

      // 残影暗色剪影
      for (Ghost ghost : this.visibleGhosts(a)) {
         float dissolve = this.ghostDissolve(ghost, s);
         if (dissolve >= 1.0F) {
            continue;
         }
         b.begin(Style.AFTERIMAGE, 0.8F, 0.1F, 1.0F, 0.0F, 0.0F, dissolve);
         this.drawGhost(b, frame, ghost, 0.85F, true);
      }
      b.end();
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float s = this.sheathe(frame.partialTick);
      float frozen = freeze(s);

      // 1. 残线层：淡青路径残线、斩痕柔光与悬空残痕
      g.begin(Style.RIBBON, 2.4F, 0.2F, 7.0F, 0.0F, 0.0F, 0.35F);
      for (Segment seg : this.segments) {
         float alpha = FxMath.window(a - seg.at, 0.0F, 1.0F) * 0.3F * this.afterShatter(s, seg.distance, 3.0F);
         if (alpha > 0.0F) {
            this.line(g, frame, seg.from, seg.to, 0.022F * this.scale, alpha, ICE, false);
         }
      }
      for (Cut cut : this.cuts) {
         float t = a - cut.at;
         if (t < 0.0F || (s >= 0.0F && s >= this.shatterTime(cut.distance))) {
            continue;
         }
         float alpha = FxMath.window(t, 0.0F, 1.2F);
         Vec3 end = cut.revealEnd(t);
         this.line(g, frame, cut.a, end, 0.2F * this.scale, alpha * 0.16F, ICE, true);
         this.line(g, frame, cut.a, end, 0.032F * this.scale, alpha * 0.5F, PALE, true);
         for (int k = 0; k < SUB_SLASHES; k++) {
            float st = t - cut.subDelay[k];
            if (st >= 0.0F) {
               Vec3 subEnd = cut.subEnd(k, st);
               float subAlpha = FxMath.window(st, 0.0F, 0.9F);
               this.line(g, frame, cut.subA[k], subEnd, 0.14F * this.scale, subAlpha * 0.12F, k == 0 ? VOID : ICE, true);
               this.line(g, frame, cut.subA[k], subEnd, 0.024F * this.scale, subAlpha * 0.42F, PALE, true);
            }
         }
      }

      // 2. 白热层：刚划出的发丝与一闪刃心；納刀时整体绽亮，斩痕延长为横贯画面的切线
      g.begin(Style.RIBBON, 11.0F, 0.2F, 8.0F, 0.0F, 0.0F, 0.25F);
      for (Segment seg : this.segments) {
         float t = a - seg.at;
         float fresh = t < 0.0F ? 0.0F : (1.0F - FxMath.window(t, 0.0F, 2.5F));
         float flare = s >= 0.0F ? FxMath.window(s, 0.0F, 0.6F) * 0.7F * this.afterShatter(s, seg.distance, 3.0F) : 0.0F;
         float alpha = Math.max(fresh * fresh, flare);
         if (alpha > 0.0F) {
            this.line(g, frame, seg.from, seg.to, 0.02F * this.scale, alpha, WHITE, false);
         }
      }
      for (Cut cut : this.cuts) {
         float t = a - cut.at;
         if (t < 0.0F) {
            continue;
         }
         float fresh = 1.0F - FxMath.window(t, 0.5F, 4.0F);
         float width = 0.075F * this.scale * (0.6F + 0.4F * fresh);
         float alpha = fresh;
         if (s >= 0.0F) {
            float shatter = this.shatterTime(cut.distance);
            if (s >= shatter) {
               continue;
            }
            float flare = FxMath.window(s, 0.0F, 0.6F);
            float strike = s >= FREEZE ? 1.0F - FxMath.window(s, FREEZE, shatter) : 0.0F;
            alpha = Math.max(alpha, flare);
            width = 0.075F * this.scale * (1.0F + 0.6F * flare + 0.9F * strike);
         }
         if (alpha > 0.0F) {
            this.line(g, frame, cut.a, cut.revealEnd(t), width, alpha, WHITE, true);
         }
         // 瞬狱连斩：主斩之后 1 / 2 tick 追加两道交叉短斩
         for (int k = 0; k < SUB_SLASHES; k++) {
            float st = t - cut.subDelay[k];
            if (st < 0.0F) {
               continue;
            }
            float subFresh = 1.0F - FxMath.window(st, 0.4F, 3.0F);
            float subAlpha = Math.max(subFresh, s >= 0.0F ? alpha * 0.8F : 0.0F);
            if (subAlpha > 0.0F) {
               this.line(g, frame, cut.subA[k], cut.subEnd(k, st), width * 0.65F, subAlpha, WHITE, true);
            }
         }
      }
      if (s >= 0.0F && s < SLIDE_END) {
         float extend = FxMath.easeOutCubic(FxMath.window(s, 0.0F, 1.6F));
         float strength = s < FREEZE ? 0.85F : 1.0F - FxMath.window(s, FREEZE, SLIDE_END);
         for (Cut cut : this.cuts) {
            if (s >= this.shatterTime(cut.distance)) {
               continue;
            }
            double reach = cut.length * 0.5 + 28.0 * extend;
            this.longLine(g, frame, cut.center.subtract(cut.dir.scale(reach)), cut.center.add(cut.dir.scale(reach)), strength);
         }
      }

      // 3. 瞬步光柱：出发点细、落点粗的等离子光柱，4 tick 内收束消失
      g.begin(Style.PLASMA_BEAM, 5.5F, 0.2F, 1.0F, 8.0F, 0.0F, 0.5F);
      for (Segment seg : this.segments) {
         float t = a - seg.at;
         if (t < 0.0F || t >= 4.0F) {
            continue;
         }
         float k = 1.0F - t / 4.0F;
         Vector3f from = frame.rel(seg.from);
         Vector3f to = frame.rel(seg.to);
         float fade = k * k * Math.min(nearFade(from), 1.0F) * Math.max(0.3F, nearFade(to));
         g.cylinder(from, to, 0.04F * this.scale * k, 0.2F * this.scale * k, 10, a * 0.4F, ICE[0], ICE[1], ICE[2], fade);
      }

      // 4. 速度线、斩击火花、碎散冰晶、收刀竖线
      g.begin(Style.RIBBON, 4.5F, 0.2F, 6.0F, 0.0F, 0.0F, 0.4F);
      for (Segment seg : this.segments) {
         float t = a - seg.at;
         if (t < 0.0F || t >= 3.0F) {
            continue;
         }
         Vec3 axis = seg.to.subtract(seg.from);
         double length = axis.length();
         if (length < 0.5) {
            continue;
         }
         Vec3 unit = axis.scale(1.0 / length);
         float fade = 1.0F - t / 3.0F;
         for (int i = 0; i < SPEED_LINES; i++) {
            double travel = FxMath.clamp01(t / 1.6F + seg.linePhase[i] * 0.35F);
            Vec3 head = seg.from.add(axis.scale(travel)).add(seg.lineOffset[i]);
            Vec3 tail = head.subtract(unit.scale(Math.min(length * travel, 2.6 * this.scale)));
            Vector3f rel = frame.rel(head);
            float[] tint = i % 3 == 0 ? VOID : ICE;
            g.streak(rel, frame.rel(tail), 0.018F * this.scale, tint[0], tint[1], tint[2], fade * nearFade(rel));
         }
      }
      for (Cut cut : this.cuts) {
         float t = a - cut.at;
         if (t >= 0.0F && t < 7.0F) {
            float fade = 1.0F - t / 7.0F;
            for (int i = 0; i < SPARKS; i++) {
               Vec3 head = cut.center.add(cut.sparkVelocity[i].scale(t * (1.0 - t / 16.0)));
               Vec3 tail = head.subtract(cut.sparkVelocity[i].scale(0.8));
               Vector3f rel = frame.rel(head);
               float[] tint = i % 4 == 0 ? VOID : ICE;
               g.streak(rel, frame.rel(tail), 0.024F * this.scale, tint[0], tint[1], tint[2], fade * fade * cutFade(rel));
            }
         }
         if (s >= 0.0F) {
            float e = s - this.shatterTime(cut.distance);
            if (e >= 0.0F && e < SHATTER_LIFE) {
               float k = 1.0F - e / SHATTER_LIFE;
               double pieceLength = cut.length / SHARDS * 0.8 * k;
               for (int i = 0; i < SHARDS; i++) {
                  Vec3 mid = cut.a.lerp(cut.b, (i + 0.5) / SHARDS).add(cut.shardVelocity[i].scale(e)).add(0.0, -0.01 * e * e, 0.0);
                  Vec3 half = cut.shardAxis[i].scale(pieceLength * 0.5);
                  Vector3f rel = frame.rel(mid);
                  float[] tint = i % 3 == 1 ? VOID : (i % 3 == 2 ? PALE : ICE);
                  g.streak(frame.rel(mid.add(half)), frame.rel(mid.subtract(half)), 0.034F * this.scale * k, tint[0], tint[1], tint[2], (float)Math.pow(k, 1.2) * nearFade(rel));
               }
            }
         }
      }
      if (s >= FREEZE && s < FREEZE + 6.0F) {
         float e = s - FREEZE;
         float k = 1.0F - e / 6.0F;
         float half = 2.2F * this.scale * FxMath.easeOutCubic(FxMath.window(e, 0.0F, 0.8F));
         List<Vector3f> points = List.of(
            frame.rel(this.origin.add(0.0, half, 0.0)), frame.rel(this.origin), frame.rel(this.origin.add(0.0, -half, 0.0))
         );
         g.ribbon(points, new float[]{0.004F, 0.05F * this.scale, 0.004F}, new float[]{0.0F, k * nearFade(points.get(1)), 0.0F}, WHITE[0], WHITE[1], WHITE[2]);
      }

      // 5. 空间涟漪：切口处两圈环（冰青先、虚空紫后），所在平面垂直于闪现方向
      g.begin(Style.RING, 3.5F, 0.3F, 0.95F, 0.03F, 0.0F, 0.0F);
      for (Cut cut : this.cuts) {
         this.ripple(g, frame, cut, a - cut.at, 1.0F, ICE);
         this.ripple(g, frame, cut, a - cut.at - 0.8F, 0.7F, VOID);
      }
      if (s >= FREEZE && s < FREEZE + 7.0F) {
         // 齐斩时起点脚下扩散的地面冲击环
         float e = (s - FREEZE) / 7.0F;
         float radius = (1.5F + Math.min(24.0F, this.maxDistance * 0.6F) * FxMath.easeOutCubic(e)) / 0.95F;
         Vector3f feet = frame.rel(this.origin.add(0.0, -0.85, 0.0));
         g.quad(feet, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), ICE[0], ICE[1], ICE[2], (1.0F - e) * (1.0F - e));
         float inner = radius * 0.72F;
         g.quad(feet, new Vector3f(inner, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, inner), VOID[0], VOID[1], VOID[2], (1.0F - e) * 0.8F);
      }

      // 6. 断面门描边与合拢发丝
      float gate = this.gateLevel(a, s);
      if (gate > 0.0F) {
         this.gateEdges(g, frame, gate, a, s);
      }

      // 7. 公告板：切口白闪、残痕闪烁星点、納刀十字星芒
      g.begin(Style.GLOW, 9.0F, 0.0F, 7.0F, 3.4F, 1.0F, 0.0F);
      for (Cut cut : this.cuts) {
         float t = a - cut.at;
         Vector3f c = frame.rel(cut.center);
         if (t >= 0.0F && t < 3.0F) {
            float k = 1.0F - t / 3.0F;
            g.billboard(c, 1.2F * this.scale, cut.flashRotation, WHITE[0], WHITE[1], WHITE[2], k * k * cutFade(c));
         }
         boolean alive = s < 0.0F || s < this.shatterTime(cut.distance);
         if (t >= 1.2F && alive) {
            for (int i = 0; i < cut.glints.length; i++) {
               float twinkle = (float)Math.pow(0.5 + 0.5 * Math.sin(frame.time * 9.0F + cut.glints[i] * 40.0F), 6.0);
               Vector3f p = frame.rel(cut.a.lerp(cut.b, cut.glints[i]));
               g.billboard(p, 0.22F * this.scale, 0.0F, PALE[0], PALE[1], PALE[2], 0.55F * twinkle * cutFade(p));
            }
         }
         if (s >= 0.0F && alive) {
            float shatter = this.shatterTime(cut.distance);
            float pop = FxMath.window(s, 0.0F, 0.8F);
            float strike = s >= FREEZE ? 1.0F - FxMath.window(s, FREEZE, FREEZE + 2.0F) : 0.0F;
            float alpha = pop * (0.55F + 0.45F * strike) * (1.0F - FxMath.window(s, shatter - 1.0F, shatter));
            float size = (0.6F + 0.4F * pop + 0.5F * strike) * this.scale * Math.max(1.0F, cut.height * 0.5F);
            float fade = alpha * cutFade(c);
            g.billboard(c, size, 0.0F, WHITE[0], WHITE[1], WHITE[2], fade);
            g.billboard(c, size, (float)(Math.PI * 0.5), ICE[0], ICE[1], ICE[2], fade * 0.85F);
         }
      }
      g.end();

      this.renderSeamDiscs(g, frame, a, s);
      this.renderCrystalSpray(g, frame, a, s);
      this.renderMarks(g, frame, a, s);
      this.renderSplits(g, frame, a, s);

      // 8. 全息残影
      List<Ghost> visible = this.visibleGhosts(a);
      if (!visible.isEmpty()) {
         float flare = s >= 0.0F ? FxMath.window(s, 0.0F, 0.6F) * (1.0F - 0.5F * FxMath.window(s, FREEZE, FREEZE + 3.0F)) : 0.0F;
         g.begin(Style.AFTERIMAGE, 2.6F + 2.4F * flare, 0.1F, 1.0F, 0.06F, 3.5F, 0.0F);
         for (Ghost ghost : visible) {
            if (this.ghostDissolve(ghost, s) <= 0.0F) {
               float appear = 1.0F - FxMath.window(a - ghost.visibleAt, 0.0F, 3.0F);
               this.drawGhost(g, frame, ghost, Math.min(1.0F, 0.6F + appear * 0.8F + frozen * 0.3F), false);
            }
         }
         for (Ghost ghost : visible) {
            float dissolve = this.ghostDissolve(ghost, s);
            if (dissolve > 0.0F && dissolve < 1.0F) {
               g.begin(Style.AFTERIMAGE, 3.2F, 0.1F, 1.0F, 0.06F, 3.5F, dissolve);
               this.drawGhost(g, frame, ghost, 0.9F, false);
            }
         }
         g.end();
      }
   }

   // ───────────────────────────── 敌人身上：断面、冰晶、死兆星、剪影斩断 ─────────────────────────────

   /** 齐斩时被斩成两半的剪影沿刀口错开的进度。 */
   private static float burst(float s) {
      return s < FREEZE ? 0.0F : FxMath.easeOutCubic(FxMath.window(s, FREEZE, FREEZE + 2.2F));
   }

   /** 断面光盘：刀口平面与身体相交处的一片发光截面。 */
   private void renderSeamDiscs(FxGeometry g, FxFrame frame, float a, float s) {
      g.begin(Style.GLOW, 7.0F, 0.2F, 2.6F, 2.2F, 1.2F, 0.0F);
      for (Cut cut : this.cuts) {
         float t = a - cut.at;
         if (t < 0.0F) {
            continue;
         }
         float alpha = 0.25F + 0.75F * (1.0F - FxMath.window(t, 0.0F, 3.0F));
         if (s >= 0.0F) {
            float shatter = this.shatterTime(cut.distance);
            if (s >= shatter) {
               continue;
            }
            float pop = FxMath.window(s, 0.0F, 0.8F);
            float strike = s >= FREEZE ? 1.0F - FxMath.window(s, FREEZE, shatter) : 0.0F;
            alpha = Math.max(alpha, 0.35F + 0.35F * pop + 0.6F * strike);
         }
         float spread = 1.0F + 0.5F * burst(s);
         Vector3f c = frame.rel(cut.center);
         Vector3f u = toVector(cut.dir, cut.discU * spread);
         Vector3f v = toVector(cut.forward, cut.discV * spread);
         g.quad(c, u, v, PALE[0], PALE[1], PALE[2], Math.min(1.0F, alpha) * cutFade(c));
      }
      g.end();
   }

   /** 3D 冰晶：斩中时从刀口两侧迸出一小簇，齐斩时大量喷射。 */
   private void renderCrystalSpray(FxGeometry g, FxFrame frame, float a, float s) {
      g.begin(Style.GLOW, 6.0F, 0.0F, 2.0F, 1.0F, 0.0F, 0.0F);
      for (Cut cut : this.cuts) {
         float t = a - cut.at;
         if (t >= 0.0F && t < 8.0F) {
            this.spray(g, frame, cut, t, 8.0F, SPRAY / 2, 0.6F);
         }
         if (s >= FREEZE) {
            float e = s - FREEZE;
            if (e < 12.0F) {
               this.spray(g, frame, cut, e, 12.0F, SPRAY, 1.0F);
            }
         }
      }
      g.end();
   }

   private void spray(FxGeometry g, FxFrame frame, Cut cut, float e, float life, int count, float power) {
      float k = 1.0F - e / life;
      for (int i = 0; i < count; i++) {
         Vec3 velocity = cut.sprayVelocity[i].scale(power);
         Vec3 position = cut.center.add(velocity.scale(e * (1.0 - e / (life * 2.2)))).add(0.0, -0.008 * e * e, 0.0);
         Vector3f rel = frame.rel(position);
         float spin = cut.spraySpin[i] * e;
         float size = cut.spraySize[i] * this.scale * (0.6F + 0.4F * k) * power;
         float cs = (float)Math.cos(spin);
         float sn = (float)Math.sin(spin);
         Vector3f u = new Vector3f(cs * size, sn * size * 0.6F, -sn * size * 0.8F);
         Vector3f v = new Vector3f(-sn * size * 0.5F, cs * size * 1.6F, cs * size * 0.5F);
         Vector3f n = new Vector3f(u).cross(v).normalize();
         float[] tint = i % 3 == 0 ? VOID : (i % 3 == 1 ? ICE : PALE);
         g.shard(rel, u, v, n, size * 0.3F, tint[0], tint[1], tint[2], (float)Math.pow(k, 1.1) * nearFade(rel));
      }
   }

   /** 死兆星：锁定的敌人头顶悬一颗冰青四芒星，斩中时坠入体内。 */
   private void renderMarks(FxGeometry g, FxFrame frame, float a, float s) {
      if (this.marks.isEmpty()) {
         return;
      }
      g.begin(Style.SPRITE, 6.5F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F);
      for (Mark mark : this.marks) {
         float in = FxMath.easeOutCubic(FxMath.window(a, mark.delay, mark.delay + 2.0F));
         if (in <= 0.0F) {
            continue;
         }
         float alpha = in;
         float drop = 0.0F;
         if (mark.struckAt >= 0) {
            float t = a - mark.struckAt;
            drop = FxMath.easeInCubic(FxMath.window(t, 0.0F, 1.5F));
            alpha *= 1.0F - FxMath.window(t, 1.2F, 2.0F);
         } else if (s >= 0.0F) {
            alpha *= 1.0F - FxMath.window(s, 0.0F, FREEZE);
         }
         if (alpha <= 0.0F) {
            continue;
         }
         float pulse = 0.85F + 0.15F * (float)Math.sin(frame.time * 6.0F + mark.distance);
         Vec3 star = mark.top.add(0.0, (0.75 - 1.4 * drop) * this.scale, 0.0);
         Vector3f rel = frame.rel(star);
         float size = (0.32F * pulse + 0.4F * drop) * this.scale;
         g.billboard(rel, size, frame.time * 0.8F + mark.distance, ICE[0], ICE[1], ICE[2], alpha * cutFade(rel));
         g.billboard(rel, size * 0.45F, -frame.time * 1.3F, WHITE[0], WHITE[1], WHITE[2], alpha * cutFade(rel));
      }
      g.begin(Style.RIBBON, 3.0F, 0.0F, 7.0F, 0.0F, 0.0F, 0.3F);
      for (Mark mark : this.marks) {
         if (mark.struckAt >= 0) {
            continue;
         }
         float in = FxMath.window(a, mark.delay + 0.5F, mark.delay + 2.5F);
         float alpha = in * (s >= 0.0F ? 1.0F - FxMath.window(s, 0.0F, FREEZE) : 1.0F);
         if (alpha <= 0.0F) {
            continue;
         }
         Vec3 star = mark.top.add(0.0, 0.75 * this.scale, 0.0);
         List<Vector3f> points = List.of(frame.rel(star), frame.rel(mark.top.add(0.0, 0.1, 0.0)));
         g.ribbon(points, new float[]{0.012F * this.scale, 0.004F}, new float[]{alpha * 0.7F, 0.0F}, ICE[0], ICE[1], ICE[2]);
      }
      g.end();
   }

   /**
    * 剪影斩断：斩中瞬间整具身体以白热剪影闪亮，随后沿刀口一分为二、悬停在原地；
    * 齐斩时两半猛地沿刀口错开并分离，随后与斩痕一同溶解。
    */
   private void renderSplits(FxGeometry g, FxFrame frame, float a, float s) {
      List<Cut> splits = new ArrayList<>();
      for (int i = Math.max(0, this.cuts.size() - MAX_SPLITS); i < this.cuts.size(); i++) {
         Cut cut = this.cuts.get(i);
         if (cut.quads != null && a >= cut.at) {
            splits.add(cut);
         }
      }
      if (splits.isEmpty()) {
         return;
      }
      float frozen = freeze(s);
      float open = burst(s);
      // 白热剪影（刚斩中）
      g.begin(Style.AFTERIMAGE, 7.5F, 0.1F, 1.3F, 0.55F, 0.0F, 0.0F);
      for (Cut cut : splits) {
         float t = a - cut.at;
         if (t < 2.5F) {
            float flash = 1.0F - FxMath.window(t, 0.6F, 2.5F);
            this.drawSplit(g, frame, cut, 0.0F, 0.0F, flash, WHITE, WHITE);
         }
      }
      // 悬停的两半与齐斩分离
      g.begin(Style.AFTERIMAGE, 2.6F + 2.4F * frozen + 2.0F * (1.0F - FxMath.window(s, FREEZE, FREEZE + 3.0F)) * open, 0.1F, 1.0F, 0.08F, 3.0F, 0.0F);
      for (Cut cut : splits) {
         float t = a - cut.at;
         if (t < 1.2F || this.splitDissolve(cut, s) > 0.0F) {
            continue;
         }
         float settle = FxMath.easeOutCubic(FxMath.window(t, 1.2F, 4.0F));
         float alpha = 0.5F * settle + 0.4F * frozen + 0.3F * open;
         this.drawSplit(g, frame, cut, (0.05F + 0.45F * open) * this.scale, (0.03F + 0.6F * open) * this.scale, Math.min(1.0F, alpha), ICE, VOID);
      }
      for (Cut cut : splits) {
         float dissolve = this.splitDissolve(cut, s);
         if (dissolve > 0.0F && dissolve < 1.0F) {
            g.begin(Style.AFTERIMAGE, 3.4F, 0.1F, 1.0F, 0.08F, 3.0F, dissolve);
            this.drawSplit(g, frame, cut, (0.05F + 0.45F * open) * this.scale, (0.03F + 0.6F * open) * this.scale, 0.9F, ICE, VOID);
         }
      }
      g.end();
   }

   private float splitDissolve(Cut cut, float s) {
      if (s < 0.0F) {
         return 0.0F;
      }
      float start = this.shatterTime(cut.distance) - 0.5F;
      return FxMath.window(s, start, start + GHOST_DISSOLVE);
   }

   /** 按刀口平面把截取到的面分成两半：两半沿法线分离 separation，并沿刀口方向反向滑移 slide。 */
   private void drawSplit(FxGeometry g, FxFrame frame, Cut cut, float separation, float slide, float alpha, float[] top, float[] bottom) {
      if (alpha <= 0.0F) {
         return;
      }
      Vector3f base = frame.rel(cut.feet);
      float fade = alpha * cutFade(new Vector3f(base).add(0.0F, cut.height * 0.5F, 0.0F));
      if (fade <= 0.0F) {
         return;
      }
      float[] quads = cut.quads;
      for (int i = 0, face = 0; i + 11 < quads.length; i += 12, face++) {
         float side = cut.sides[face];
         float ox = (float)(cut.normal.x * separation + cut.dir.x * slide) * side;
         float oy = (float)(cut.normal.y * separation + cut.dir.y * slide) * side;
         float oz = (float)(cut.normal.z * separation + cut.dir.z * slide) * side;
         this.q0.set(base.x + quads[i] + ox, base.y + quads[i + 1] + oy, base.z + quads[i + 2] + oz);
         this.q1.set(base.x + quads[i + 3] + ox, base.y + quads[i + 4] + oy, base.z + quads[i + 5] + oz);
         this.q2.set(base.x + quads[i + 6] + ox, base.y + quads[i + 7] + oy, base.z + quads[i + 8] + oz);
         this.q3.set(base.x + quads[i + 9] + ox, base.y + quads[i + 10] + oy, base.z + quads[i + 11] + oz);
         float k = FxMath.clamp01((quads[i + 1] + quads[i + 4] + quads[i + 7] + quads[i + 10]) * 0.25F / Math.max(0.4F, cut.height));
         g.rawQuad(this.q0, this.q1, this.q2, this.q3,
            FxMath.lerp(bottom[0], top[0], k), FxMath.lerp(bottom[1], top[1], k), FxMath.lerp(bottom[2], top[2], k), fade);
      }
   }

   private static Vector3f toVector(Vec3 v, float length) {
      return new Vector3f((float)v.x, (float)v.y, (float)v.z).mul(length);
   }

   /** 納刀后按距离由远到近淡出的系数。 */
   private float afterShatter(float s, float distance, float fade) {
      if (s < 0.0F) {
         return 1.0F;
      }
      float shatter = this.shatterTime(distance);
      return 1.0F - FxMath.window(s, shatter, shatter + fade);
   }

   private void ripple(FxGeometry g, FxFrame frame, Cut cut, float t, float size, float[] color) {
      if (t < 0.0F || t >= 5.0F) {
         return;
      }
      float k = t / 5.0F;
      float radius = (0.3F + 2.6F * size * FxMath.easeOutCubic(k)) * this.scale / 0.95F;
      Vector3f c = frame.rel(cut.center);
      Vector3f u = new Vector3f((float)cut.ringU.x, (float)cut.ringU.y, (float)cut.ringU.z).mul(radius);
      Vector3f v = new Vector3f((float)cut.ringV.x, (float)cut.ringV.y, (float)cut.ringV.z).mul(radius);
      g.quad(c, u, v, color[0], color[1], color[2], (float)Math.pow(1.0F - k, 1.5) * cutFade(c));
   }

   // ───────────────────────────── 残影 ─────────────────────────────

   /** 当前可见的残影（只保留最近的 MAX_GHOSTS 个，避免长序列时残影过多）。 */
   private List<Ghost> visibleGhosts(float a) {
      List<Ghost> out = new ArrayList<>();
      int first = Math.max(0, this.ghosts.size() - MAX_GHOSTS);
      Ghost originGhost = this.originGhost();
      for (int i = 0; i < this.ghosts.size(); i++) {
         Ghost ghost = this.ghosts.get(i);
         if (ghost.quads == null || ghost.visibleAt < 0 || a < ghost.visibleAt) {
            continue;
         }
         if (i < first && ghost != originGhost) {
            continue;
         }
         out.add(ghost);
      }
      return out;
   }

   /** 残影溶解进度：起点残影在施法者回到原位时最先溶解，其余与对应斩痕同时碎散。 */
   private float ghostDissolve(Ghost ghost, float s) {
      if (s < 0.0F) {
         return 0.0F;
      }
      float start = ghost == this.originGhost() ? 0.5F : this.shatterTime(ghost.distance);
      return FxMath.window(s, start, start + GHOST_DISSOLVE);
   }

   private void drawGhost(FxGeometry g, FxFrame frame, Ghost ghost, float alpha, boolean body) {
      float[] quads = ghost.quads;
      Vec3 feet = ghost.chest.subtract(0.0, ghost.height * 0.5, 0.0);
      Vector3f base = frame.rel(feet);
      float fade = alpha * FxMath.window(new Vector3f(base).add(0.0F, ghost.height * 0.5F, 0.0F).length(), 1.0F, 2.8F);
      if (fade <= 0.0F) {
         return;
      }
      for (int i = 0; i + 11 < quads.length; i += 12) {
         this.q0.set(base.x + quads[i], base.y + quads[i + 1], base.z + quads[i + 2]);
         this.q1.set(base.x + quads[i + 3], base.y + quads[i + 4], base.z + quads[i + 5]);
         this.q2.set(base.x + quads[i + 6], base.y + quads[i + 7], base.z + quads[i + 8]);
         this.q3.set(base.x + quads[i + 9], base.y + quads[i + 10], base.z + quads[i + 11]);
         if (body) {
            g.rawQuad(this.q0, this.q1, this.q2, this.q3, DEEP[0], DEEP[1], DEEP[2], fade);
         } else {
            // 自下而上：虚空紫 → 冰青
            float k = FxMath.clamp01((quads[i + 1] + quads[i + 4] + quads[i + 7] + quads[i + 10]) * 0.25F / Math.max(0.5F, ghost.height));
            float r = FxMath.lerp(VOID[0], ICE[0], k);
            float gr = FxMath.lerp(VOID[1], ICE[1], k);
            float bl = FxMath.lerp(VOID[2], ICE[2], k);
            g.rawQuad(this.q0, this.q1, this.q2, this.q3, r, gr, bl, fade);
         }
      }
   }

   // ───────────────────────────── 断面门 ─────────────────────────────

   private GatePoints gatePoints(FxFrame frame, float level, float size) {
      float height = 2.6F * this.scale * size;
      float width = 0.42F * this.scale * size * level;
      int n = 11;
      List<Vector3f> points = new ArrayList<>(n);
      float[] widths = new float[n];
      for (int i = 0; i < n; i++) {
         float k = (float)i / (n - 1);
         points.add(frame.rel(this.origin.add(0.0, (k - 0.5F) * height, 0.0)));
         widths[i] = width * (float)Math.pow(Math.sin(Math.PI * k), 0.7);
      }
      return new GatePoints(points, widths);
   }

   private void gateEdges(FxGeometry g, FxFrame frame, float level, float a, float s) {
      GatePoints gate = this.gatePoints(frame, level, 1.0F);
      int n = gate.points.size();
      float glow = Math.min(1.0F, 0.45F + level);
      g.begin(Style.RIBBON, 5.5F, 0.2F, 7.0F, 0.0F, 0.0F, 0.4F);
      for (int side = -1; side <= 1; side += 2) {
         List<Vector3f> edge = new ArrayList<>(n);
         float[] widths = new float[n];
         float[] alphas = new float[n];
         for (int i = 0; i < n; i++) {
            Vector3f p = gate.points.get(i);
            edge.add(new Vector3f(frame.left).mul(gate.widths[i] * side).add(p));
            widths[i] = 0.03F * this.scale;
            alphas[i] = glow * nearFade(p) * (float)Math.sin(Math.PI * i / (n - 1));
         }
         float[] tint = side < 0 ? ICE : VOID;
         g.ribbon(edge, widths, alphas, tint[0], tint[1], tint[2]);
      }
      // 合拢时中线闪出一道白色发丝
      float seam = 0.0F;
      if (this.opened) {
         seam = Math.max(seam, FxMath.window(a, 4.0F, 5.5F) * (1.0F - FxMath.window(a, 6.0F, 8.0F)));
      }
      if (s >= FREEZE) {
         seam = Math.max(seam, 1.0F - FxMath.window(s, FREEZE + 1.0F, FREEZE + 3.0F));
      }
      if (seam > 0.0F) {
         float[] alphas = new float[n];
         float[] widths = new float[n];
         for (int i = 0; i < n; i++) {
            alphas[i] = seam * nearFade(gate.points.get(i)) * (float)Math.sin(Math.PI * i / (n - 1));
            widths[i] = 0.016F * this.scale;
         }
         g.ribbon(gate.points, widths, alphas, WHITE[0], WHITE[1], WHITE[2]);
      }
   }

   // ───────────────────────────── 线条工具 ─────────────────────────────

   /** 一条直线：taper 为真时两端收尖（斩痕），否则近似等宽（路径）。 */
   private void line(FxGeometry g, FxFrame frame, Vec3 from, Vec3 to, float width, float alpha, float[] color, boolean taper) {
      int n = 7;
      List<Vector3f> points = new ArrayList<>(n);
      float[] widths = new float[n];
      float[] alphas = new float[n];
      for (int i = 0; i < n; i++) {
         float k = (float)i / (n - 1);
         Vector3f p = frame.rel(from.lerp(to, k));
         points.add(p);
         float shape = taper ? (float)Math.pow(Math.sin(Math.PI * k), 0.6) : 0.55F + 0.45F * (float)Math.sin(Math.PI * k);
         widths[i] = width * Math.max(0.08F, shape);
         alphas[i] = alpha * (taper ? cutFade(p) * Math.max(0.15F, shape) : nearFade(p));
      }
      g.ribbon(points, widths, alphas, color[0], color[1], color[2]);
   }

   /** 横贯画面的长切线：线宽随距离增大，远处不会细到闪烁。 */
   private void longLine(FxGeometry g, FxFrame frame, Vec3 from, Vec3 to, float alpha) {
      int n = 15;
      List<Vector3f> points = new ArrayList<>(n);
      float[] widths = new float[n];
      float[] alphas = new float[n];
      for (int i = 0; i < n; i++) {
         float k = (float)i / (n - 1);
         Vector3f p = frame.rel(from.lerp(to, k));
         points.add(p);
         widths[i] = Math.max(0.012F * this.scale, p.length() * 0.0022F);
         alphas[i] = alpha * (float)Math.pow(Math.sin(Math.PI * k), 0.35) * cutFade(p);
      }
      g.ribbon(points, widths, alphas, PALE[0], PALE[1], PALE[2]);
   }

   // ───────────────────────────── 场景通道：世界被切开 ─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      if (FxShaders.sliceScene == null) {
         return;
      }
      float a = this.age(frame.partialTick);
      float s = this.sheathe(frame.partialTick);
      List<SliceLine> lines = new ArrayList<>();
      if (s >= 0.0F && s < SLIDE_END) {
         float slip = SLIP_MAX * slide(s);
         float seam = s < FREEZE ? 0.55F * FxMath.window(s, 0.0F, 1.2F) : 0.9F * (1.0F - FxMath.window(s, FREEZE + 1.0F, SLIDE_END));
         for (Cut cut : this.cuts) {
            if (s >= this.shatterTime(cut.distance) + 1.0F) {
               continue;
            }
            this.addSlice(frame, lines, cut, slip * cut.slipSign, 0.0025F + slip * 0.25F, 1.0F, seam);
         }
      } else if (s < 0.0F) {
         for (Cut cut : this.cuts) {
            float t = a - cut.at;
            if (t >= 0.0F && t < 4.0F) {
               float k = 1.0F - t / 4.0F;
               this.addSlice(frame, lines, cut, LOCAL_SLIP * k * cut.slipSign, 0.0035F, 0.0F, 0.5F * k);
            }
         }
      }
      if (lines.isEmpty()) {
         return;
      }
      lines.sort(Comparator.comparingDouble((SliceLine line) -> line.score).reversed());
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
      float gap = s >= 0.0F ? 1.0F : 0.6F;
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.sliceScene, shader -> {
         shader.safeGetUniform("CutsA").set(cutsA);
         shader.safeGetUniform("CutsB").set(cutsB);
         shader.safeGetUniform("CutsC").set(cutsC);
         shader.safeGetUniform("CutsD").set(cutsD);
         shader.safeGetUniform("Slice").set((float)count, gap, 1.0F, 0.0F);
      }));
   }

   private void addSlice(FxFrame frame, List<SliceLine> lines, Cut cut, float slip, float width, float mode, float seam) {
      float[] uvA = frame.screenUv(cut.a);
      float[] uvB = frame.screenUv(cut.b);
      if (uvA == null || uvB == null) {
         return;
      }
      float dx = uvB[0] - uvA[0];
      float dy = uvB[1] - uvA[1];
      float score = (float)Math.sqrt(dx * dx + dy * dy);
      if (score < 0.01F) {
         return;
      }
      // 远处的斩痕在屏幕上很短：缝口与错位按投影长度收敛，避免远处一条线把整块画面推走
      float influence = Math.min(1.0F, score * 4.0F);
      lines.add(new SliceLine(uvA[0], uvA[1], uvB[0], uvB[1], slip * influence, width, mode, seam * influence, score));
   }

   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float s = this.sheathe(frame.partialTick);
      if (s < 0.0F || s > FREEZE + 8.0F) {
         return;
      }
      double distance = frame.camera.distanceTo(this.origin);
      if (distance > SCREEN_RANGE) {
         return;
      }
      float near = 1.0F - (float)(distance / SCREEN_RANGE);
      float frozen = freeze(s);
      if (frozen > 0.0F) {
         screen.addDesaturate(0.92F * frozen * near);
         screen.addVignette(0x050A14, 0.45F * frozen * near);
      }
      if (s >= FREEZE) {
         float k = 1.0F - FxMath.window(s, FREEZE, FREEZE + 7.0F);
         float[] center = frame.screenUv(this.origin);
         screen.addAberration(1.1F * k * near);
         screen.addDashBlur(0.7F * k * near, center);
         screen.addBloomBoost(0.6F * k * near);
         screen.addFlash(0xD8F4FF, 0.28F * k * k * k * near, 0.7F);
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float s = this.sheathe(frame.partialTick);
      if (s < FREEZE || s > FREEZE + 6.0F) {
         return 0.0F;
      }
      double distance = frame.camera.distanceTo(this.origin);
      if (distance > SCREEN_RANGE) {
         return 0.0F;
      }
      return 0.4F * (1.0F - (s - FREEZE) / 6.0F) * (1.0F - (float)(distance / SCREEN_RANGE));
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private record GatePoints(List<Vector3f> points, float[] widths) {
   }

   private record SliceLine(float ax, float ay, float bx, float by, float slip, float width, float mode, float seam, float score) {
   }

   private static final class Segment {
      final Vec3 from;
      final Vec3 to;
      final int at;
      final float distance;
      final float[] linePhase = new float[SPEED_LINES];
      final Vec3[] lineOffset = new Vec3[SPEED_LINES];

      Segment(Vec3 from, Vec3 to, int at, float distance, RandomSource random) {
         this.from = from;
         this.to = to;
         this.at = at;
         this.distance = distance;
         Vec3 axis = to.subtract(from);
         Vec3 forward = axis.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : axis.normalize();
         Vec3 side = forward.cross(new Vec3(0.0, 1.0, 0.0));
         side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         Vec3 up = side.cross(forward).normalize();
         for (int i = 0; i < SPEED_LINES; i++) {
            double angle = (i + random.nextDouble() * 0.6) / SPEED_LINES * Math.PI * 2.0;
            double radius = 0.3 + random.nextDouble() * 0.45;
            this.lineOffset[i] = side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
            this.linePhase[i] = random.nextFloat();
         }
      }
   }

   private static final class Ghost {
      final Vec3 chest;
      final float yaw;
      final float pitch;
      final float attackTime;
      final float distance;
      float height = 1.8F;
      float[] quads;
      boolean captureTried;
      int visibleAt = -1;

      Ghost(Vec3 chest, float yaw, float pitch, float attackTime, float distance) {
         this.chest = chest;
         this.yaw = yaw;
         this.pitch = pitch;
         this.attackTime = attackTime;
         this.distance = distance;
      }
   }

   private static final class Mark {
      final int entityId;
      final float distance;
      float delay;
      int struckAt = -1;
      Vec3 top;

      Mark(int entityId, Entity entity, Vec3 origin) {
         this.entityId = entityId;
         Vec3 center = entity.getBoundingBox().getCenter();
         this.distance = (float)center.distanceTo(origin);
         this.top = center.add(0.0, entity.getBbHeight() * 0.5, 0.0);
      }
   }

   private static final class Cut {
      final Vec3 center;
      final Vec3 a;
      final Vec3 b;
      final Vec3 dir;
      /** 闪现方向（刀刃扫过的方向）与刀口平面法线。 */
      final Vec3 forward;
      final Vec3 normal;
      final float discU;
      final float discV;
      final Vec3[] subA = new Vec3[SUB_SLASHES];
      final Vec3[] subB = new Vec3[SUB_SLASHES];
      final float[] subDelay = new float[SUB_SLASHES];
      final Vec3[] sprayVelocity = new Vec3[SPRAY];
      final float[] spraySpin = new float[SPRAY];
      final float[] spraySize = new float[SPRAY];
      /** 剪影斩断：目标脚底、截取到的面（相对脚底）与每个面位于刀口哪一侧。 */
      Vec3 feet;
      float[] quads;
      float[] sides;
      final double length;
      final float height;
      final int at;
      final float distance;
      final float slipSign;
      final float flashRotation;
      /** 空间涟漪所在平面（垂直于闪现方向）的两条正交基。 */
      final Vec3 ringU;
      final Vec3 ringV;
      final float[] glints = new float[2];
      final Vec3[] sparkVelocity = new Vec3[SPARKS];
      final Vec3[] shardVelocity = new Vec3[SHARDS];
      final Vec3[] shardAxis = new Vec3[SHARDS];

      Cut(Vec3 center, Vec3 blink, float height, float scale, int at, float distance, RandomSource random) {
         this.center = center;
         this.height = height;
         this.at = at;
         this.distance = distance;
         // 斩痕位于与闪现方向垂直的平面内，随机斜切 20° – 63°
         Vec3 forward = blink.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : blink.normalize();
         Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
         right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
         Vec3 up = right.cross(forward).normalize();
         this.ringU = right;
         this.ringV = up;
         double theta = (0.35 + random.nextDouble() * 0.75) * (random.nextBoolean() ? 1.0 : -1.0);
         Vec3 direction = right.scale(Math.cos(theta)).add(up.scale(Math.sin(theta)));
         this.length = Math.max(2.4, height * 1.6) * scale;
         Vec3 half = direction.scale(this.length * 0.5);
         boolean flip = random.nextBoolean();
         this.a = flip ? center.add(half) : center.subtract(half);
         this.b = flip ? center.subtract(half) : center.add(half);
         this.dir = this.b.subtract(this.a).normalize();
         this.slipSign = random.nextBoolean() ? 1.0F : -1.0F;
         this.flashRotation = random.nextFloat() * (float)Math.PI;
         for (int i = 0; i < this.glints.length; i++) {
            this.glints[i] = 0.2F + random.nextFloat() * 0.6F;
         }
         for (int i = 0; i < SPARKS; i++) {
            double speed = 0.22 + random.nextDouble() * 0.24;
            Vec3 jitter = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.3, random.nextDouble() - 0.5).scale(0.14);
            this.sparkVelocity[i] = this.dir.scale(speed * (i % 2 == 0 ? 1.0 : -0.7)).add(jitter);
         }
         for (int i = 0; i < SHARDS; i++) {
            Vec3 drift = new Vec3(random.nextDouble() - 0.5, random.nextDouble() * 0.6 - 0.2, random.nextDouble() - 0.5);
            this.shardVelocity[i] = drift.normalize().scale(0.05 + random.nextDouble() * 0.07);
            Vec3 tilt = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(0.6);
            this.shardAxis[i] = this.dir.add(tilt).normalize();
         }

         this.forward = forward;
         Vec3 n = this.dir.cross(forward);
         this.normal = n.lengthSqr() < 1.0E-6 ? up : n.normalize();
         this.discU = (float)Math.min(this.length * 0.5, Math.max(0.6, height * 0.42) * scale);
         this.discV = Math.max(0.35F, height * 0.22F) * scale;

         // 两道交叉短斩：与主斩成 50° – 80°，略微错开中心
         for (int k = 0; k < SUB_SLASHES; k++) {
            double cross = theta + (k == 0 ? 1.0 : -1.0) * (0.9 + random.nextDouble() * 0.5);
            Vec3 subDir = right.scale(Math.cos(cross)).add(up.scale(Math.sin(cross)));
            Vec3 shift = this.dir.scale((random.nextDouble() - 0.5) * this.length * 0.25);
            Vec3 subHalf = subDir.scale(this.length * 0.32);
            Vec3 subCenter = center.add(shift);
            this.subA[k] = subCenter.subtract(subHalf);
            this.subB[k] = subCenter.add(subHalf);
            this.subDelay[k] = 1.0F + k;
         }

         // 冰晶喷射：沿刀口法线向两侧迸出
         for (int i = 0; i < SPRAY; i++) {
            double side = i % 2 == 0 ? 1.0 : -1.0;
            Vec3 velocity = this.normal.scale(side * (0.12 + random.nextDouble() * 0.16))
               .add(this.dir.scale((random.nextDouble() - 0.5) * 0.22))
               .add(0.0, 0.03 + random.nextDouble() * 0.06, 0.0);
            this.sprayVelocity[i] = velocity;
            this.spraySpin[i] = (random.nextFloat() - 0.5F) * 1.2F;
            this.spraySize[i] = 0.07F + random.nextFloat() * 0.09F;
         }
      }

      void attach(LivingEntity target, float[] captured) {
         if (captured == null) {
            return;
         }
         this.feet = target.position();
         this.quads = captured;
         int faces = captured.length / 12;
         this.sides = new float[faces];
         for (int f = 0; f < faces; f++) {
            int i = f * 12;
            double cx = this.feet.x + (captured[i] + captured[i + 3] + captured[i + 6] + captured[i + 9]) * 0.25;
            double cy = this.feet.y + (captured[i + 1] + captured[i + 4] + captured[i + 7] + captured[i + 10]) * 0.25;
            double cz = this.feet.z + (captured[i + 2] + captured[i + 5] + captured[i + 8] + captured[i + 11]) * 0.25;
            double d = (cx - this.center.x) * this.normal.x + (cy - this.center.y) * this.normal.y + (cz - this.center.z) * this.normal.z;
            this.sides[f] = d >= 0.0 ? 1.0F : -1.0F;
         }
      }

      /** 交叉短斩在 0.9 tick 内划完。 */
      Vec3 subEnd(int k, float t) {
         return this.subA[k].lerp(this.subB[k], FxMath.easeOutCubic(FxMath.window(t, 0.0F, 0.9F)));
      }

      /** 一闪在 1.2 tick 内从 a 端划到 b 端。 */
      Vec3 revealEnd(float t) {
         return this.a.lerp(this.b, FxMath.easeOutCubic(FxMath.window(t, 0.0F, 1.2F)));
      }
   }
}
