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
 * 湮灭之刃 SE「虚无权域·万象湮灭」专属渲染（一次性）。
 * <p>
 * 意象「格式化」：领域之内，现实被还原为数据，再被逐条删除。全程只用黑、白、灰。
 * <pre>
 *  0 ─ 2     显现：中心白闪，穹顶轮廓亮起；中心浮现巨大的「∅」（空集符号：白环 + 斜杠 + 黑色空洞）
 *  1 ─ 4     领域内画面马赛克化、去色，所有方块表面浮现与方块边缘对齐的坐标网格；地面铺开极坐标网格
 *  3 ─ 15    抹除前沿：白色扫描墙自中心扩张，经过处画面反相；扫到的敌人被一道删除线划过、
 *            盖上黑色涂黑框与白色边框，随后碎成像素方块向上飘散
 * 15 ─ 17    万象湮灭：整个穹顶瞬间反相（格式化完成）
 * 15 ─ 34    马赛克由粗变细，颜色回归，网格与 ∅ 淡去
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public final class VoidDominionFx extends FxInstance {
   private static final int LIFETIME = 36;
   private static final float FRONT_START = 3.0F;
   private static final float FRONT_END = 15.0F;
   private static final float FORMAT = 15.0F;
   private static final int PIXELS = 10;
   private static final int SOUND_SAMPLES = 10;
   private static final int MAX_TARGETS = 64;

   private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
   private static final float[] GREY = {0.72F, 0.72F, 0.74F};
   private static final float[] BLACK = {0.0F, 0.0F, 0.0F};

   private final Vec3 center;
   private final Vec3 ground;
   private final float radius;
   private final float scale;
   private final List<Erased> erased = new ArrayList<>();

   public VoidDominionFx(ClientLevel level, FxEventPacket packet) {
      super(LIFETIME);
      this.center = packet.origin();
      this.radius = Math.max(4.0F, packet.radius());
      this.scale = Math.max(0.4F, Math.min(3.0F, packet.scale()));
      double surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(this.center.x), Mth.floor(this.center.z));
      double groundY = surface > this.center.y + 1.0 ? this.center.y - 1.6 : surface + 0.05;
      this.ground = new Vec3(this.center.x, groundY, this.center.z);
      RandomSource random = RandomSource.create(packet.seed());
      for (int id : packet.entityIds()) {
         if (this.erased.size() >= MAX_TARGETS) {
            break;
         }
         Entity entity = level.getEntity(id);
         if (entity == null) {
            continue;
         }
         Vec3 c = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
         float fraction = FxMath.clamp01((float)(c.distanceTo(this.center) / this.radius));
         float eraseAt = FRONT_START + (FRONT_END - FRONT_START) * invEaseOut(fraction);
         this.erased.add(new Erased(c, entity.getBbWidth(), entity.getBbHeight(), eraseAt, random));
      }
   }

   /** easeOutCubic 的反函数。 */
   private static float invEaseOut(float y) {
      return 1.0F - (float)Math.cbrt(1.0F - FxMath.clamp01(y));
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public double cullDistance() {
      return this.radius + 160.0;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         play(level, this.center, SoundEvents.CONDUIT_ACTIVATE, 1.0F, 0.6F);
      }
      if (age == (int)FORMAT) {
         play(level, this.center, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.5F);
         play(level, this.center, SoundEvents.CONDUIT_DEACTIVATE, 0.8F, 0.8F);
      }
      int stride = Math.max(1, this.erased.size() / SOUND_SAMPLES);
      for (int i = 0; i < this.erased.size(); i += stride) {
         Erased target = this.erased.get(i);
         if ((int)Math.floor(target.eraseAt) == age) {
            play(level, target.center, SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.7F + 0.3F * target.jitter);
         }
      }
   }

   private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int age = this.ageTicks();
      if (age == 0) {
         for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.END_ROD, this.center.x, this.center.y, this.center.z, 0.0, 0.0, 0.0);
         }
      }
      for (Erased target : this.erased) {
         if ((int)Math.floor(target.eraseAt) == age) {
            for (int i = 0; i < 6; i++) {
               level.addParticle(ParticleTypes.WHITE_ASH, target.center.x, target.center.y, target.center.z, 0.0, 0.05, 0.0);
            }
         }
      }
   }

   // ───────────────────────────── 时间轴 ─────────────────────────────

   private static float presence(float t) {
      return FxMath.window(t, 0.0F, 2.0F) * (1.0F - FxMath.window(t, 26.0F, 34.0F));
   }

   private float front(float t) {
      return this.radius * FxMath.easeOutCubic(FxMath.window(t, FRONT_START, FRONT_END));
   }

   private static float mosaic(float t) {
      if (t < FORMAT) {
         return FxMath.lerp(1.0F, 12.0F, FxMath.window(t, 0.0F, 3.0F));
      }
      return FxMath.lerp(12.0F, 1.0F, FxMath.window(t, FORMAT, 28.0F));
   }

   private static float invertPulse(float t) {
      return 0.9F * FxMath.envelope(FxMath.window(t, FORMAT - 0.5F, FORMAT + 2.5F), 0.3F, 0.4F);
   }

   private static float backOut(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float m = x - 1.0F;
      return 1.0F + c3 * m * m * m + c1 * m * m;
   }

   /** 「∅」的显现缩放与亮度。 */
   private static float nullScale(float t) {
      return Math.max(0.0F, backOut(FxMath.window(t, 0.0F, 2.0F)));
   }

   private static float nullAlpha(float t) {
      return FxMath.window(t, 0.0F, 1.0F) * (1.0F - FxMath.window(t, 20.0F, 28.0F));
   }

   private static float nearFade(Vector3f rel) {
      return FxMath.window(rel.length(), 1.2F, 3.5F);
   }

   // ───────────────────────────── 本体层 ─────────────────────────────

   @Override
   public boolean hasBody() {
      return true;
   }

   @Override
   public void renderBody(FxGeometry b, FxFrame frame) {
      float t = this.age(frame.partialTick);
      // ∅ 的黑色空洞
      float alpha = nullAlpha(t);
      if (alpha > 0.0F) {
         Vector3f c = frame.rel(this.center);
         b.begin(Style.GLOW, 1.0F, 0.0F, 2.5F, 3.0F, 0.0F, 0.0F);
         b.billboard(c, 1.9F * this.scale * nullScale(t), 0.0F, BLACK[0], BLACK[1], BLACK[2], 0.9F * alpha * nearFade(c));
      }
      // 涂黑框
      b.begin(Style.AFTERIMAGE, 1.0F, 0.0F, 1.0F, 0.85F, 0.0F, 0.0F);
      for (Erased target : this.erased) {
         float size = this.boxSize(target, t);
         if (size > 0.0F) {
            this.square(b, frame, target.center, size, BLACK, 1.0F);
         }
      }
      b.end();
   }

   /** 涂黑框尺寸：抹除瞬间弹出，6 tick 后收缩消散。 */
   private float boxSize(Erased target, float t) {
      float e = t - target.eraseAt;
      if (e < 0.2F || e >= 9.0F) {
         return 0.0F;
      }
      float grow = FxMath.easeOutCubic(FxMath.window(e, 0.2F, 0.7F));
      float shrink = 1.0F - 0.9F * FxMath.easeInCubic(FxMath.window(e, 6.0F, 9.0F));
      return Math.max(0.35F, target.height * 0.42F) * grow * shrink;
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float t = this.age(frame.partialTick);
      Vector3f c = frame.rel(this.center);

      // 1. 中心白闪
      g.begin(Style.GLOW, 10.0F, 0.0F, 4.0F, 2.5F, 1.0F, 0.0F);
      if (t < 3.0F) {
         float k = 1.0F - t / 3.0F;
         g.billboard(c, 4.0F * this.scale, 0.0F, WHITE[0], WHITE[1], WHITE[2], k * k);
      }
      for (Erased target : this.erased) {
         float e = t - target.eraseAt;
         if (e >= 0.0F && e < 1.5F) {
            Vector3f p = frame.rel(target.center);
            float k = 1.0F - e / 1.5F;
            g.billboard(p, 0.9F * this.scale, 0.0F, WHITE[0], WHITE[1], WHITE[2], k * nearFade(p));
         }
      }

      // 2. ∅：白环 + 斜杠
      float alpha = nullAlpha(t);
      if (alpha > 0.0F) {
         float s = nullScale(t);
         float flare = 1.0F + 0.8F * FxMath.envelope(FxMath.window(t, FORMAT - 0.5F, FORMAT + 3.0F), 0.2F, 0.5F);
         float spin = t * 0.01F;
         float r = 2.2F * this.scale * s / 0.95F;
         Vector3f u = new Vector3f(frame.left).mul((float)Math.cos(spin)).add(new Vector3f(frame.up).mul((float)Math.sin(spin))).mul(r);
         Vector3f v = new Vector3f(frame.up).mul((float)Math.cos(spin)).sub(new Vector3f(frame.left).mul((float)Math.sin(spin))).mul(r);
         g.begin(Style.RING, 5.0F * flare, 0.0F, 0.95F, 0.03F, 0.0F, 0.0F);
         g.quad(c, u, v, WHITE[0], WHITE[1], WHITE[2], alpha * nearFade(c));
         float theta = (float)Math.toRadians(60.0) + spin;
         Vector3f diagonal = new Vector3f(frame.left).mul((float)Math.cos(theta)).add(new Vector3f(frame.up).mul((float)Math.sin(theta))).mul(2.7F * this.scale * s);
         g.begin(Style.RIBBON, 8.0F * flare, 0.0F, 7.0F, 0.0F, 0.0F, 0.3F);
         g.ribbon(List.of(new Vector3f(c).sub(diagonal), new Vector3f(c), new Vector3f(c).add(diagonal)),
            new float[]{0.04F * this.scale, 0.07F * this.scale, 0.04F * this.scale}, new float[]{alpha * 0.6F, alpha, alpha * 0.6F}, WHITE[0], WHITE[1], WHITE[2]);
      }

      // 3. 地面极坐标网格：4 圈虚线环 + 8 根辐线
      float grid = FxMath.window(t, 1.0F, 4.0F) * (1.0F - FxMath.window(t, 22.0F, 32.0F));
      if (grid > 0.0F) {
         Vector3f gc = frame.rel(this.ground);
         int[] dashes = {8, 12, 18, 24};
         for (int i = 0; i < 4; i++) {
            float k = (i + 1) / 4.0F;
            float r = this.radius * k * FxMath.easeOutCubic(FxMath.window(t, 1.0F + i * 0.4F, 4.0F + i * 0.4F)) / 0.95F;
            if (r <= 0.01F) {
               continue;
            }
            g.begin(Style.RING, 2.0F, 0.0F, 0.95F, i == 3 ? 0.006F : 0.012F, dashes[i], 0.06F * (i % 2 == 0 ? 1.0F : -1.0F));
            g.quad(gc, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), GREY[0], GREY[1], GREY[2], grid * (i == 3 ? 1.0F : 0.6F));
         }
         g.begin(Style.RIBBON, 2.0F, 0.0F, 7.0F, 0.0F, 0.0F, 0.2F);
         float reach = this.radius * FxMath.easeOutCubic(FxMath.window(t, 1.0F, 4.0F));
         for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            Vector3f end = new Vector3f(gc).add((float)Math.cos(angle) * reach, 0.0F, (float)Math.sin(angle) * reach);
            float w = Math.max(0.02F, end.length() * 0.0015F);
            g.ribbon(List.of(new Vector3f(gc), end), new float[]{w, w}, new float[]{grid * 0.5F, grid * 0.2F}, GREY[0], GREY[1], GREY[2]);
         }
      }

      // 4. 抹除前沿：白色扫描墙 + 地面刃线
      if (t >= FRONT_START && t < FRONT_END + 1.0F) {
         float r = this.front(t);
         float fade = 1.0F - FxMath.window(t, FRONT_END - 1.0F, FRONT_END + 1.0F);
         Vector3f base = frame.rel(this.ground.add(0.0, -0.4, 0.0));
         Vector3f top = frame.rel(this.ground.add(0.0, 3.0 * this.scale, 0.0));
         g.begin(Style.PLASMA_BEAM, 4.0F, 0.2F, 1.0F, 6.0F, 0.0F, 0.5F);
         g.cylinder(base, top, r, r, 64, 0.0F, WHITE[0], WHITE[1], WHITE[2], 0.55F * fade);
         g.begin(Style.RING, 6.0F, 0.0F, 0.95F, 0.008F, 0.0F, 0.0F);
         Vector3f gc = frame.rel(this.ground);
         float rr = r / 0.95F;
         g.quad(gc, new Vector3f(rr, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, rr), WHITE[0], WHITE[1], WHITE[2], fade);
      }

      // 5. 删除线
      g.begin(Style.RIBBON, 9.0F, 0.0F, 9.0F, 0.0F, 0.0F, 0.2F);
      for (Erased target : this.erased) {
         float e = t - target.eraseAt;
         if (e < 0.0F || e >= 7.0F) {
            continue;
         }
         float reveal = FxMath.easeOutCubic(FxMath.window(e, 0.0F, 0.8F));
         float fade = 1.0F - FxMath.window(e, 6.0F, 7.0F);
         float length = Math.max(1.2F, target.width * 2.4F) * this.scale;
         Vector3f p = frame.rel(target.center);
         Vector3f start = new Vector3f(p).add(new Vector3f(frame.left).mul(length * 0.5F));
         Vector3f end = new Vector3f(start).sub(new Vector3f(frame.left).mul(length * reveal));
         float w = 0.05F * this.scale;
         g.ribbon(List.of(start, end), new float[]{w, w}, new float[]{fade * nearFade(p), fade * nearFade(p)}, WHITE[0], WHITE[1], WHITE[2]);
      }

      // 6. 涂黑框的白色边框与像素碎散
      g.begin(Style.AFTERIMAGE, 3.0F, 0.0F, 1.2F, 0.0F, 0.0F, 0.0F);
      for (Erased target : this.erased) {
         float size = this.boxSize(target, t);
         if (size > 0.0F) {
            this.square(g, frame, target.center, size * 1.08F, WHITE, 1.0F);
         }
      }
      g.begin(Style.AFTERIMAGE, 3.5F, 0.0F, 1.0F, 0.6F, 0.0F, 0.0F);
      for (Erased target : this.erased) {
         float e = t - target.eraseAt - 6.0F;
         if (e < 0.0F || e >= 10.0F) {
            continue;
         }
         float k = 1.0F - e / 10.0F;
         for (int i = 0; i < PIXELS; i++) {
            Vec3 p = target.pixelStart[i].add(target.pixelVelocity[i].scale(e));
            float[] tint = i % 2 == 0 ? WHITE : GREY;
            this.square(g, frame, p, target.pixelSize[i] * this.scale * k, tint, k);
         }
      }
      g.end();
   }

   /** 正对相机的方块（rawQuad，配合 AFTERIMAGE 风格得到实心块或空心边框）。 */
   private void square(FxGeometry g, FxFrame frame, Vec3 position, float half, float[] color, float alpha) {
      Vector3f c = frame.rel(position);
      float fade = alpha * nearFade(c);
      if (fade <= 0.0F || half <= 0.0F) {
         return;
      }
      Vector3f l = new Vector3f(frame.left).mul(half);
      Vector3f u = new Vector3f(frame.up).mul(half);
      g.rawQuad(
         new Vector3f(c).sub(l).sub(u), new Vector3f(c).add(l).sub(u), new Vector3f(c).add(l).add(u), new Vector3f(c).sub(l).add(u),
         color[0], color[1], color[2], fade
      );
   }

   // ───────────────────────────── 场景通道与屏幕 ─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      if (FxShaders.eraseScene == null) {
         return;
      }
      float t = this.age(frame.partialTick);
      float presence = presence(t);
      if (presence <= 0.001F) {
         return;
      }
      Vector3f c = frame.rel(this.center);
      float front = t >= FRONT_START && t < FRONT_END + 1.0F ? this.front(t) : 0.0F;
      float block = mosaic(t);
      float gridStrength = FxMath.window(t, 1.0F, 4.0F) * (1.0F - FxMath.window(t, 20.0F, 30.0F));
      float desaturate = 0.95F * (1.0F - FxMath.window(t, 18.0F, 30.0F));
      float invert = invertPulse(t);
      float rim = 0.6F + 0.4F * (1.0F - FxMath.window(t, 0.0F, 3.0F));
      float fx = (float)(frame.camera.x - Math.floor(frame.camera.x));
      float fy = (float)(frame.camera.y - Math.floor(frame.camera.y));
      float fz = (float)(frame.camera.z - Math.floor(frame.camera.z));
      float radius = this.radius;
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.eraseScene, shader -> {
         shader.safeGetUniform("Dome").set(c.x, c.y, c.z, radius);
         shader.safeGetUniform("Erase").set(presence, block, front, 0.6F);
         shader.safeGetUniform("Grid").set(gridStrength, desaturate, invert, rim);
         shader.safeGetUniform("CamFract").set(fx, fy, fz, 0.0F);
      }));
   }

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float t = this.age(frame.partialTick);
      double distance = frame.camera.distanceTo(this.center);
      float inside = 1.0F - FxMath.window((float)distance, this.radius - 2.0F, this.radius);
      if (inside > 0.0F) {
         screen.addDesaturate(0.8F * inside * presence(t) * (1.0F - FxMath.window(t, 18.0F, 30.0F)));
      }
      if (distance < this.radius + 24.0) {
         if (t < 3.0F) {
            screen.addFlash(0xFFFFFF, 0.35F * (1.0F - t / 3.0F), 0.4F);
         }
         float pulse = invertPulse(t);
         if (pulse > 0.0F) {
            screen.addFlash(0xFFFFFF, 0.3F * pulse, 0.6F);
         }
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float t = this.age(frame.partialTick);
      if (t >= 3.0F) {
         return 0.0F;
      }
      double distance = frame.camera.distanceTo(this.center);
      return distance > this.radius + 24.0 ? 0.0F : 0.3F * (1.0F - t / 3.0F);
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private static final class Erased {
      final Vec3 center;
      final float width;
      final float height;
      final float eraseAt;
      final float jitter;
      final Vec3[] pixelStart = new Vec3[PIXELS];
      final Vec3[] pixelVelocity = new Vec3[PIXELS];
      final float[] pixelSize = new float[PIXELS];

      Erased(Vec3 center, float width, float height, float eraseAt, RandomSource random) {
         this.center = center;
         this.width = width;
         this.height = height;
         this.eraseAt = eraseAt;
         this.jitter = random.nextFloat();
         for (int i = 0; i < PIXELS; i++) {
            this.pixelStart[i] = center.add((random.nextDouble() - 0.5) * width, (random.nextDouble() - 0.5) * height, (random.nextDouble() - 0.5) * width);
            Vec3 outward = this.pixelStart[i].subtract(center);
            this.pixelVelocity[i] = outward.scale(0.04).add(0.0, 0.05 + random.nextDouble() * 0.06, 0.0);
            this.pixelSize[i] = 0.06F + random.nextFloat() * 0.06F;
         }
      }
   }
}
