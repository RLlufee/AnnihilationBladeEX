package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.annihilation_blade.logic.TerminusEchoPattern;
import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SE「归墟回响·终焉余律」专属渲染（持续特效：一组 5 波共用一个实例）。
 * <p>
 * 意象「余律」：每一刀都是一个音符，归墟把它们谱成一段旋律，最后完整地回放一遍。
 * <pre>
 *  起振     刀身前方一支朝前的金色号角锥，每推出一波就脉动一次
 *  每一波   9 条横向弧带叠成的「频谱声墙」沿判定带推进（弧带长度像均衡器一样跳动）；
 *           两侧拖着示波正弦丝（波序越大波长越短、颜色越白），每推进 7 格留下一个竖直共振环
 *  命中     波前扫过的目标：三连声呐脉冲 + 一圈跳动的频谱柱，并射出一颗光点飞上五线谱，落成一个音符
 *  五线谱   第一波推进时沿判定带在空中写出一行金色五线谱（含小节线）
 *  终章     最后一波走完后，播放头从左到右扫过乐谱，音符依次亮起并按五声音阶奏出这段旋律，以和弦收束
 * </pre>
 * 每一波的起点 / 射程 / 宽度与服务端共用 {@link TerminusEchoPattern}，光效与判定带一致。
 */
@OnlyIn(Dist.CLIENT)
public final class TerminusEchoFx extends FxInstance {
   private static final float TRAVEL = 8.0F;
   private static final float WAVE_LINGER = 8.0F;
   private static final int BARS = 9;
   private static final int SINE_POINTS = 28;
   private static final float RING_SPACING = 7.0F;
   private static final int PULSES = 3;
   private static final int SPECTRUM = 6;
   private static final int MAX_NOTES = 40;
   private static final int MAX_SOUND_NOTES = 16;
   private static final int HIT_SOUNDS_PER_WAVE = 2;
   private static final float FINALE_DELAY = 7.0F;
   private static final float STAFF_FADE = 10.0F;
   private static final float BAR_SPACING = 9.0F;
   private static final int[] PENTATONIC = {0, 2, 4, 7, 9};
   private static final float[] RELEASE_PITCH = {0.6F, 0.75F, 0.9F, 1.06F, 1.26F, 1.5F, 1.78F, 2.0F};

   private static final float[] AMBER = {1.0F, 0.62F, 0.22F};
   private static final float[] GOLD = {1.0F, 0.8F, 0.42F};
   private static final float[] CREAM = {1.0F, 0.95F, 0.82F};
   private static final float[] WHITE = {1.0F, 1.0F, 1.0F};

   private final Vec3 sequenceStart;
   private final Vec3 dir;
   private final Vec3 right;
   private final Vec3 ringUp;
   private final double baseRange;
   private final double baseWidth;
   private final int count;
   private final float scale;
   private final float spacing;
   private final double staffY;
   private final List<Wave> waves = new ArrayList<>();
   private final List<Note> notes = new ArrayList<>();
   private final List<Note> playback = new ArrayList<>();
   private int staffRelease = -1;
   private int sustainDuration = Integer.MAX_VALUE;
   private int stopAt = -1;
   private float finaleStart = Float.MAX_VALUE;
   private float finaleEnd = Float.MAX_VALUE;
   private float playbackInterval = 0.7F;

   public TerminusEchoFx(ClientLevel level, FxEventPacket packet) {
      super(60);
      this.sequenceStart = packet.origin();
      Vec3 direction = packet.direction();
      this.dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
      Vec3 r = this.dir.cross(new Vec3(0.0, 1.0, 0.0));
      this.right = r.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : r.normalize();
      this.ringUp = this.right.cross(this.dir).normalize();
      this.baseRange = Math.max(4.0, packet.radius());
      this.baseWidth = Math.max(1.0, packet.scale());
      int param = packet.intParam();
      this.count = Math.max(1, (param >> 4) & 0xF);
      this.scale = Math.max(0.4F, Math.min(3.0F, ((param >> 12) & 0xFF) / 10.0F));
      this.spacing = 0.32F * this.scale;
      this.staffY = this.sequenceStart.y - 0.7;
      this.addWave(level, packet);
   }

   // ───────────────────────────── 持续特效钩子 ─────────────────────────────

   @Override
   protected void onSustainedStart(int duration) {
      this.sustainDuration = duration;
      this.lifetime = Math.max(this.lifetime, duration + 60);
   }

   @Override
   public void onSustainedUpdate(ClientLevel level, FxEventPacket event) {
      if (this.stopAt < 0) {
         this.addWave(level, event);
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
      this.finaleStart = this.age + FINALE_DELAY;
      this.playback.clear();
      this.playback.addAll(this.notes);
      this.playback.sort(Comparator.comparingDouble(note -> note.x));
      for (int i = 0; i < this.playback.size(); i++) {
         this.playback.get(i).playIndex = i;
      }
      this.playbackInterval = this.playback.size() > 1 ? Math.min(0.7F, 12.0F / this.playback.size()) : 0.7F;
      this.finaleEnd = this.finaleStart + this.playback.size() * this.playbackInterval + 3.0F;
      this.lifetime = (int)Math.ceil(this.finaleEnd + STAFF_FADE + 2.0F);
   }

   private void addWave(ClientLevel level, FxEventPacket packet) {
      int index = packet.intParam() & 0xF;
      for (Wave wave : this.waves) {
         if (wave.index == index) {
            return;
         }
      }
      Wave wave = new Wave(index, this.age);
      if (this.staffRelease < 0) {
         this.staffRelease = this.age;
      }
      int hitSounds = 0;
      for (int id : packet.entityIds()) {
         Entity entity = level.getEntity(id);
         if (entity == null) {
            continue;
         }
         Vec3 center = entity.getBoundingBox().getCenter();
         double projection = Math.max(0.0, Math.min(wave.range, center.subtract(wave.start).dot(this.dir)));
         float hitTime = this.age + travelTime((float)(projection / wave.range));
         Note note = null;
         if (this.notes.size() < MAX_NOTES) {
            double x = Math.max(0.3, center.subtract(this.sequenceStart).dot(this.dir));
            int slot = (int)Math.round((center.y - this.staffY) / (this.spacing * 0.5));
            note = new Note(x, Math.max(-6, Math.min(6, slot)), hitTime + 1.0F);
            this.notes.add(note);
         }
         wave.hits.add(new Hit(center, Math.max(0.6F, entity.getBbHeight() * 0.5F), hitTime, note, hitSounds++ < HIT_SOUNDS_PER_WAVE));
      }
      this.waves.add(wave);
   }

   // ───────────────────────────── 时间轴 ─────────────────────────────

   /** 波前距离：带轻微减速的推进，TRAVEL tick 走完全程。 */
   private static double frontDistance(float e, double range) {
      float k = FxMath.clamp01(e / TRAVEL);
      return range * (1.0 - Math.pow(1.0 - k, 1.6));
   }

   /** frontDistance 的反函数：波前走到 fraction 处所需的 tick。 */
   private static float travelTime(float fraction) {
      return TRAVEL * (1.0F - (float)Math.pow(1.0F - FxMath.clamp01(fraction), 1.0 / 1.6));
   }

   private static float[] tint(int wave, int count) {
      float k = count <= 1 ? 0.0F : FxMath.clamp01((float)wave / (count - 1));
      return new float[]{FxMath.lerp(AMBER[0], CREAM[0], k), FxMath.lerp(AMBER[1], CREAM[1], k), FxMath.lerp(AMBER[2], CREAM[2], k)};
   }

   private static float pitchOf(int slot) {
      int index = slot + 6;
      int semitone = PENTATONIC[index % 5] + 12 * (index / 5);
      return Math.max(0.5F, Math.min(2.0F, (float)Math.pow(2.0, (semitone - 14) / 12.0)));
   }

   private float staffAlpha(float t) {
      if (this.staffRelease < 0) {
         return 0.0F;
      }
      return FxMath.window(t, this.staffRelease, this.staffRelease + 1.0F) * (1.0F - FxMath.window(t, this.finaleEnd, this.finaleEnd + STAFF_FADE));
   }

   private Vec3 staffPoint(double x, double y) {
      Vec3 along = this.sequenceStart.add(this.dir.scale(x));
      return new Vec3(along.x, this.staffY + (along.y - this.sequenceStart.y) + y, along.z);
   }

   private static float nearFade(Vector3f rel) {
      return FxMath.window(rel.length(), 1.2F, 3.5F);
   }

   @Override
   public Vec3 focus() {
      return this.sequenceStart.add(this.dir.scale(this.baseRange * 0.5));
   }

   @Override
   public double cullDistance() {
      return this.baseRange + 160.0;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      if (this.stopAt < 0 && this.age >= this.sustainDuration) {
         this.beginFinale();
      }
      int age = this.ageTicks();
      for (Wave wave : this.waves) {
         if (wave.releasedAt == age) {
            play(level, wave.start, SoundEvents.NOTE_BLOCK_BELL.value(), 0.4F, RELEASE_PITCH[Math.min(wave.index, RELEASE_PITCH.length - 1)]);
            play(level, wave.start, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.3F, 1.4F);
         }
         for (Hit hit : wave.hits) {
            if (hit.sound && (int)Math.floor(hit.time) == age) {
               play(level, hit.center, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.35F, hit.note != null ? pitchOf(hit.note.slot) : 1.0F);
            }
         }
      }
      if (this.stopAt >= 0) {
         int stride = Math.max(1, (int)Math.ceil(this.playback.size() / (double)MAX_SOUND_NOTES));
         for (int i = 0; i < this.playback.size(); i += stride) {
            if ((int)Math.floor(this.finaleStart + i * this.playbackInterval) == age) {
               Note note = this.playback.get(i);
               SoundEvent sound = (i / stride) % 2 == 0 ? SoundEvents.NOTE_BLOCK_BELL.value() : SoundEvents.NOTE_BLOCK_CHIME.value();
               play(level, this.staffPoint(note.x, 0.0), sound, 0.55F, pitchOf(note.slot));
            }
         }
         if ((int)Math.floor(this.finaleEnd - 1.0F) == age) {
            Vec3 at = this.staffPoint(this.playback.isEmpty() ? this.baseRange * 0.5 : this.playback.get(this.playback.size() - 1).x, 0.0);
            play(level, at, SoundEvents.NOTE_BLOCK_HARP.value(), 0.5F, 0.75F);
            play(level, at, SoundEvents.NOTE_BLOCK_HARP.value(), 0.5F, 1.0F);
            play(level, at, SoundEvents.NOTE_BLOCK_HARP.value(), 0.5F, 1.5F);
         }
      }
   }

   private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      for (Wave wave : this.waves) {
         if (wave.releasedAt == age) {
            for (int i = 1; i <= 6; i++) {
               Vec3 p = wave.start.add(this.dir.scale(wave.range * i / 7.0));
               level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0.0, 0.0, 0.0);
            }
         }
         for (Hit hit : wave.hits) {
            if ((int)Math.floor(hit.time) == age) {
               double color = hit.note != null ? (hit.note.slot + 6) / 24.0 : 0.5;
               level.addParticle(ParticleTypes.NOTE, hit.center.x, hit.center.y + 0.6, hit.center.z, color, 0.0, 0.0);
            }
         }
      }
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float t = this.age(frame.partialTick);
      this.renderHorn(g, frame, t);
      this.renderWavefronts(g, frame, t);
      this.renderSines(g, frame, t);
      this.renderRings(g, frame, t);
      this.renderHits(g, frame, t);
      this.renderStaff(g, frame, t);
      this.renderNotes(g, frame, t);
      g.end();
   }

   /** 归墟之号：起点处朝前的号角锥，每推出一波脉动一次。 */
   private void renderHorn(FxGeometry g, FxFrame frame, float t) {
      float alpha = this.staffAlpha(t) * (this.stopAt >= 0 ? 1.0F - FxMath.window(t, this.stopAt, this.stopAt + 6.0F) : 1.0F);
      if (alpha <= 0.0F) {
         return;
      }
      float lastRelease = -100.0F;
      for (Wave wave : this.waves) {
         if (wave.releasedAt <= t) {
            lastRelease = Math.max(lastRelease, wave.releasedAt);
         }
      }
      float pulse = FxMath.window(t - lastRelease, 0.0F, 3.0F);
      float swell = 1.0F + 0.25F * (1.0F - pulse);
      Vector3f base = frame.rel(this.sequenceStart.add(this.dir.scale(0.6)));
      Vector3f top = frame.rel(this.sequenceStart.add(this.dir.scale(2.6 * this.scale)));
      float fade = alpha * (0.15F + 0.85F * FxMath.window(base.length(), 2.0F, 5.0F));
      float hs = (float)(this.baseWidth / 4.4) * this.scale;
      g.begin(Style.SHOCK_CONE, 4.5F, 0.3F, pulse, 1.4F, 0.6F, 0.9F);
      g.cylinder(base, top, 0.12F * this.scale, 0.9F * hs * swell, 18, t * 0.05F, GOLD[0], GOLD[1], GOLD[2], fade);
   }

   /** 频谱声墙：9 条横向弧带叠成的波前，弧带长度像均衡器一样跳动。 */
   private void renderWavefronts(FxGeometry g, FxFrame frame, float t) {
      g.begin(Style.SLASH_ARC, 5.5F, 0.3F, 8.0F, 0.6F, 1.0F, 0.0F);
      for (Wave wave : this.waves) {
         float e = t - wave.releasedAt;
         if (e < 0.0F || e > TRAVEL + 1.0F) {
            continue;
         }
         double d = frontDistance(e, wave.range);
         if (d < 0.6) {
            continue;
         }
         float alpha = FxMath.window(e, 0.0F, 0.5F) * (1.0F - FxMath.window(e, TRAVEL - 1.0F, TRAVEL + 1.0F));
         float hs = (float)(wave.width / 4.4) * this.scale;
         float thetaMax = (float)Math.max(0.15, Math.min(1.2, Math.asin(Math.min(1.0, wave.width * 0.6 / d))));
         Vector3f u = toVector(this.dir);
         Vector3f v = toVector(this.right);
         for (int k = 0; k < BARS; k++) {
            float y = (k - (BARS - 1) * 0.5F) * 0.28F * hs;
            float centerBias = 1.0F - Math.abs(k - (BARS - 1) * 0.5F) / (BARS * 0.5F) * 0.55F;
            float amp = (0.35F + 0.65F * Math.abs((float)Math.sin(t * 1.9F + k * 1.37F + wave.index * 0.7F))) * centerBias;
            float theta = thetaMax * amp;
            Vector3f c = frame.rel(wave.start.add(0.0, y, 0.0));
            float fade = alpha * (0.5F + 0.5F * amp) * nearFade(new Vector3f(c).add(new Vector3f(u).mul((float)d)));
            g.slashArc(c, u, v, (float)d - 0.18F * hs, (float)d, -theta, theta, 14, wave.tint[0], wave.tint[1], wave.tint[2], fade);
         }
      }
      // 波前之后的宽幅余晖
      g.begin(Style.SLASH_ARC, 2.0F, 0.3F, 4.0F, 0.8F, 1.0F, 0.0F);
      for (Wave wave : this.waves) {
         float e = t - wave.releasedAt;
         if (e < 0.0F || e > TRAVEL + 1.0F) {
            continue;
         }
         double d = frontDistance(e, wave.range);
         if (d < 1.6) {
            continue;
         }
         float alpha = 0.35F * (1.0F - FxMath.window(e, TRAVEL - 1.0F, TRAVEL + 1.0F));
         float hs = (float)(wave.width / 4.4) * this.scale;
         float theta = (float)Math.max(0.15, Math.min(1.1, Math.asin(Math.min(1.0, wave.width * 0.55 / d))));
         Vector3f c = frame.rel(wave.start);
         g.slashArc(c, toVector(this.dir), toVector(this.right), (float)d - 1.4F * hs, (float)d, -theta, theta, 16, wave.tint[0], wave.tint[1], wave.tint[2], alpha);
      }
   }

   /** 示波正弦丝：判定带两侧的纵向正弦，随波前写出，波序越大波长越短。 */
   private void renderSines(FxGeometry g, FxFrame frame, float t) {
      g.begin(Style.RIBBON, 4.0F, 0.2F, 7.0F, 0.0F, 0.0F, 0.4F);
      for (Wave wave : this.waves) {
         float e = t - wave.releasedAt;
         if (e < 0.0F || e > TRAVEL + WAVE_LINGER) {
            continue;
         }
         double d = frontDistance(e, wave.range);
         if (d < 1.0) {
            continue;
         }
         float fade = 1.0F - FxMath.window(e, TRAVEL, TRAVEL + WAVE_LINGER);
         float hs = (float)(wave.width / 4.4) * this.scale;
         double wavelength = Math.max(1.2, 3.2 - wave.index * 0.45);
         for (int side = -1; side <= 1; side += 2) {
            List<Vector3f> points = new ArrayList<>(SINE_POINTS);
            float[] widths = new float[SINE_POINTS];
            float[] alphas = new float[SINE_POINTS];
            for (int i = 0; i < SINE_POINTS; i++) {
               double x = 0.6 + (d - 0.6) * i / (SINE_POINTS - 1);
               double amplitude = 0.4 * hs * (1.0 - 0.6 * x / wave.range);
               double y = amplitude * Math.sin(Math.PI * 2.0 * x / wavelength - e * 2.2 + side);
               Vec3 p = wave.start.add(this.dir.scale(x)).add(this.right.scale(side * wave.width * 0.5)).add(0.0, y, 0.0);
               Vector3f rel = frame.rel(p);
               points.add(rel);
               widths[i] = 0.03F * this.scale;
               alphas[i] = fade * FxMath.window((float)x, 0.6F, 2.0F) * nearFade(rel) * (i == SINE_POINTS - 1 ? 0.3F : 1.0F);
            }
            g.ribbon(points, widths, alphas, wave.tint[0], wave.tint[1], wave.tint[2]);
         }
      }
   }

   /** 共振环：波前每推进 7 格，在判定带上留下一个竖直环并向外扩散。 */
   private void renderRings(FxGeometry g, FxFrame frame, float t) {
      g.begin(Style.RING, 3.2F, 0.3F, 0.95F, 0.035F, 0.0F, 0.0F);
      for (Wave wave : this.waves) {
         for (float distance = RING_SPACING; distance < wave.range; distance += RING_SPACING) {
            float start = wave.releasedAt + travelTime((float)(distance / wave.range));
            float e = t - start;
            if (e < 0.0F || e >= 6.0F) {
               continue;
            }
            float k = e / 6.0F;
            float radius = (float)(wave.width * (0.3 + 0.45 * FxMath.easeOutCubic(k))) / 0.95F;
            Vector3f c = frame.rel(wave.start.add(this.dir.scale(distance)));
            Vector3f u = toVector(this.right).mul(radius);
            Vector3f v = toVector(this.ringUp).mul(radius);
            g.quad(c, u, v, wave.tint[0], wave.tint[1], wave.tint[2], (float)Math.pow(1.0F - k, 1.3) * nearFade(c));
         }
      }
   }

   /** 命中：三连声呐脉冲、跳动的频谱柱、飞向五线谱的光点。 */
   private void renderHits(FxGeometry g, FxFrame frame, float t) {
      g.begin(Style.RING, 4.0F, 0.3F, 0.95F, 0.04F, 0.0F, 0.0F);
      for (Wave wave : this.waves) {
         for (Hit hit : wave.hits) {
            Vector3f c = frame.rel(hit.center);
            for (int p = 0; p < PULSES; p++) {
               float e = t - hit.time - p * 1.2F;
               if (e < 0.0F || e >= 4.0F) {
                  continue;
               }
               float k = e / 4.0F;
               float radius = (0.3F + 1.3F * FxMath.easeOutCubic(k)) * hit.size / 0.95F;
               g.quad(c, new Vector3f(frame.left).mul(radius), new Vector3f(frame.up).mul(radius), wave.tint[0], wave.tint[1], wave.tint[2], (1.0F - k) * nearFade(c));
            }
         }
      }
      g.begin(Style.RIBBON, 5.0F, 0.2F, 6.0F, 0.0F, 0.0F, 0.4F);
      for (Wave wave : this.waves) {
         for (Hit hit : wave.hits) {
            float e = t - hit.time;
            if (e < 0.0F || e >= 6.0F) {
               continue;
            }
            float fade = 1.0F - e / 6.0F;
            for (int j = 0; j < SPECTRUM; j++) {
               double angle = Math.PI * 2.0 * j / SPECTRUM + hit.time;
               float height = (0.3F + 0.5F * Math.abs((float)Math.sin(e * 3.0F + j * 1.7F))) * hit.size;
               Vec3 foot = hit.center.add(Math.cos(angle) * 0.75 * hit.size, -hit.size * 0.6, Math.sin(angle) * 0.75 * hit.size);
               Vector3f a = frame.rel(foot);
               g.streak(frame.rel(foot.add(0.0, height, 0.0)), a, 0.05F * this.scale, wave.tint[0], wave.tint[1], wave.tint[2], fade * nearFade(a));
            }
            if (hit.note != null && e < 1.2F) {
               float k = FxMath.easeInCubic(FxMath.window(e, 0.0F, 1.0F));
               Vec3 target = this.staffPoint(hit.note.x, hit.note.slot * this.spacing * 0.5);
               Vec3 head = hit.center.lerp(target, k);
               Vec3 tail = hit.center.lerp(target, Math.max(0.0F, k - 0.25F));
               Vector3f rel = frame.rel(head);
               g.streak(rel, frame.rel(tail), 0.04F * this.scale, CREAM[0], CREAM[1], CREAM[2], nearFade(rel));
            }
         }
      }
   }

   /** 五线谱：随第一波写出的五条金线、小节线，以及终章的播放头。 */
   private void renderStaff(FxGeometry g, FxFrame frame, float t) {
      float alpha = this.staffAlpha(t);
      if (alpha <= 0.0F) {
         return;
      }
      double written = frontDistance(t - this.staffRelease, this.baseRange);
      if (written < 0.5) {
         return;
      }
      g.begin(Style.RIBBON, 3.0F, 0.0F, 7.0F, 0.0F, 0.0F, 0.3F);
      int n = 18;
      for (int line = -2; line <= 2; line++) {
         List<Vector3f> points = new ArrayList<>(n);
         float[] widths = new float[n];
         float[] alphas = new float[n];
         for (int i = 0; i < n; i++) {
            double x = 0.3 + (written - 0.3) * i / (n - 1);
            Vector3f rel = frame.rel(this.staffPoint(x, line * this.spacing));
            points.add(rel);
            widths[i] = Math.max(0.012F * this.scale, rel.length() * 0.0012F);
            alphas[i] = 0.38F * alpha * nearFade(rel) * (i == n - 1 ? 0.2F : 1.0F);
         }
         g.ribbon(points, widths, alphas, GOLD[0], GOLD[1], GOLD[2]);
      }
      double top = 2.0 * this.spacing;
      for (double x = 0.3; x <= written; x += BAR_SPACING) {
         float barAlpha = (x < 0.5 ? 0.85F : 0.5F) * alpha;
         this.verticalLine(g, frame, this.staffPoint(x, -top), this.staffPoint(x, top), x < 0.5 ? 0.04F : 0.02F, barAlpha, GOLD);
      }
      if (t >= this.finaleStart && !this.playback.isEmpty()) {
         // 终止双小节线与播放头
         double end = Math.max(this.playback.get(this.playback.size() - 1).x + 1.0, written);
         this.verticalLine(g, frame, this.staffPoint(end, -top), this.staffPoint(end, top), 0.02F, alpha * 0.6F, GOLD);
         this.verticalLine(g, frame, this.staffPoint(end + 0.18, -top), this.staffPoint(end + 0.18, top), 0.045F, alpha * 0.8F, GOLD);
         float span = this.playback.size() * this.playbackInterval;
         float k = FxMath.window(t, this.finaleStart, this.finaleStart + span);
         if (k > 0.0F && k < 1.0F) {
            double first = this.playback.get(0).x - 0.5;
            double last = this.playback.get(this.playback.size() - 1).x + 0.5;
            double x = first + (last - first) * k;
            this.verticalLine(g, frame, this.staffPoint(x, -top * 1.4), this.staffPoint(x, top * 1.4), 0.05F, alpha, CREAM);
         }
      }
   }

   /** 音符：椭圆符头 + 符干；终章依次亮起并升起一道音柱。 */
   private void renderNotes(FxGeometry g, FxFrame frame, float t) {
      float alpha = this.staffAlpha(t);
      if (alpha <= 0.0F || this.notes.isEmpty()) {
         return;
      }
      float cos = (float)Math.cos(0.35);
      float sin = (float)Math.sin(0.35);
      float headW = 0.17F * this.scale;
      float headH = 0.12F * this.scale;
      Vector3f u = new Vector3f(frame.left).mul(cos).add(new Vector3f(frame.up).mul(sin)).mul(headW);
      Vector3f v = new Vector3f(frame.up).mul(cos).sub(new Vector3f(frame.left).mul(sin)).mul(headH);
      g.begin(Style.GLOW, 7.0F, 0.0F, 3.0F, 2.4F, 0.0F, 0.0F);
      for (Note note : this.notes) {
         float in = FxMath.window(t, note.arrive, note.arrive + 0.6F);
         if (in <= 0.0F) {
            continue;
         }
         float flare = this.flare(note, t);
         Vector3f c = frame.rel(this.staffPoint(note.x, note.slot * this.spacing * 0.5));
         float grow = 1.0F + 0.6F * flare;
         g.quad(c, new Vector3f(u).mul(grow), new Vector3f(v).mul(grow), CREAM[0], CREAM[1], CREAM[2], in * alpha * nearFade(c));
         if (flare > 0.0F) {
            g.billboard(c, (0.5F + 0.4F * flare) * this.scale, 0.0F, GOLD[0], GOLD[1], GOLD[2], flare * alpha * nearFade(c));
         }
      }
      g.begin(Style.RIBBON, 4.0F, 0.0F, 7.0F, 0.0F, 0.0F, 0.3F);
      for (Note note : this.notes) {
         float in = FxMath.window(t, note.arrive, note.arrive + 0.6F);
         if (in <= 0.0F) {
            continue;
         }
         Vec3 head = this.staffPoint(note.x, note.slot * this.spacing * 0.5);
         Vector3f headRel = frame.rel(head);
         Vector3f stemBase = new Vector3f(headRel).add(new Vector3f(frame.left).mul(-headW * 0.85F));
         Vector3f stemTop = new Vector3f(stemBase).add(0.0F, 0.55F * this.scale, 0.0F);
         float fade = in * alpha * nearFade(headRel);
         g.ribbon(List.of(stemBase, stemTop), new float[]{0.012F * this.scale, 0.012F * this.scale}, new float[]{fade, fade}, CREAM[0], CREAM[1], CREAM[2]);
         float flare = this.flare(note, t);
         if (flare > 0.0F) {
            Vector3f pillarTop = new Vector3f(headRel).add(0.0F, 1.2F * this.scale * flare, 0.0F);
            g.ribbon(List.of(headRel, pillarTop), new float[]{0.06F * this.scale, 0.005F}, new float[]{flare * alpha, 0.0F}, GOLD[0], GOLD[1], GOLD[2]);
         }
      }
   }

   /** 终章回放中该音符的亮起程度（0..1）。 */
   private float flare(Note note, float t) {
      if (note.playIndex < 0 || t < this.finaleStart) {
         return 0.0F;
      }
      float e = t - (this.finaleStart + note.playIndex * this.playbackInterval);
      return e < 0.0F || e >= 3.0F ? 0.0F : 1.0F - e / 3.0F;
   }

   private void verticalLine(FxGeometry g, FxFrame frame, Vec3 from, Vec3 to, float width, float alpha, float[] color) {
      Vector3f a = frame.rel(from);
      Vector3f b = frame.rel(to);
      float fade = alpha * nearFade(a);
      if (fade <= 0.0F) {
         return;
      }
      g.ribbon(List.of(a, b), new float[]{width * this.scale, width * this.scale}, new float[]{fade, fade}, color[0], color[1], color[2]);
   }

   private static Vector3f toVector(Vec3 v) {
      return new Vector3f((float)v.x, (float)v.y, (float)v.z);
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private final class Wave {
      final int index;
      final int releasedAt;
      final Vec3 start;
      final double range;
      final double width;
      final float[] tint;
      final List<Hit> hits = new ArrayList<>();

      Wave(int index, int releasedAt) {
         this.index = index;
         this.releasedAt = releasedAt;
         this.start = TerminusEchoPattern.start(TerminusEchoFx.this.sequenceStart, TerminusEchoFx.this.right, index);
         this.range = TerminusEchoPattern.range(TerminusEchoFx.this.baseRange, index);
         this.width = TerminusEchoPattern.width(TerminusEchoFx.this.baseWidth, index);
         this.tint = tint(index, TerminusEchoFx.this.count);
      }
   }

   private record Hit(Vec3 center, float size, float time, Note note, boolean sound) {
   }

   private static final class Note {
      final double x;
      final int slot;
      final float arrive;
      int playIndex = -1;

      Note(double x, int slot, float arrive) {
         this.x = x;
         this.slot = slot;
         this.arrive = arrive;
      }
   }
}
