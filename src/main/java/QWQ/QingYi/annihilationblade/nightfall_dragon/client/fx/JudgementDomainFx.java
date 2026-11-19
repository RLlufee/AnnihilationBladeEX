package QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxPass;
import QWQ.QingYi.annihilationblade.render.client.core.FxScreenState;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.client.pipeline.FxShaders;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 夜陨次元斩 · 领域与终幕（持续特效）。
 * <pre>
 *  持续 ─ 施法点的夜陨法阵；半径 R 的次元边界环（虚线流动），边界上不时闪现短促的次元裂隙
 *  终幕 ─ 最后一斩之后（未被打断时）：
 *         +0 ~ +6   30 道横贯全域的巨型斩痕在天空中依次划出
 *         +6        冲击帧 + 闪白，整片空间像玻璃一样碎裂（复用「空间破碎」碎裂通道），碎片渐次化为虚空
 *         +6 ~ +30  冲击波扩散，碎裂空间淡出
 * </pre>
 * 终幕为纯视觉收尾，不产生额外伤害；连斩被打断（施法者死亡/切换形态）时服务端 STOP，终幕取消。
 */
@OnlyIn(Dist.CLIENT)
public final class JudgementDomainFx extends FxInstance {
   private static final int FINALE_LINES = 30;
   private static final int SHATTER = 6;
   private static final int TAIL = 32;
   private static final float[] WHITE = {1.0F, 0.95F, 1.0F};
   private static final float[] VOID = {0.69F, 0.15F, 1.0F};

   private final Vec3 center;
   private final Vec3 sphereCenter;
   private final float radius;
   private final float seedFloat;
   private final Vec3[] finaleStarts = new Vec3[FINALE_LINES];
   private final Vec3[] finaleEnds = new Vec3[FINALE_LINES];
   private final float[] finaleDelay = new float[FINALE_LINES];
   private final RandomSource random;
   private int endAge = 200;
   private boolean stopped;
   private float stopAge;

   public JudgementDomainFx(ClientLevel level, FxEventPacket packet) {
      super(200 + TAIL);
      this.center = packet.origin();
      this.radius = Math.max(8.0F, packet.radius());
      this.sphereCenter = this.center.add(0.0, Math.min(12.0, this.radius * 0.2), 0.0);
      this.seedFloat = (float)((packet.seed() & 0xFFFFL) / 65535.0 * 97.0);
      this.random = RandomSource.create(packet.seed());
      float chord = this.radius * 0.8F;
      for (int i = 0; i < FINALE_LINES; i++) {
         Vector3f dir = FxMath.randomUnit(this.random);
         dir.y *= 0.6F;
         dir.normalize();
         Vector3f offset = FxMath.randomUnit(this.random).mul(chord * 0.45F * this.random.nextFloat());
         offset.y = Math.abs(offset.y) * 0.5F;
         Vec3 mid = this.sphereCenter.add(offset.x, offset.y, offset.z);
         Vec3 half = new Vec3(dir.x, dir.y, dir.z).scale(chord * (0.7 + this.random.nextDouble() * 0.5));
         this.finaleStarts[i] = mid.subtract(half);
         this.finaleEnds[i] = mid.add(half);
         this.finaleDelay[i] = this.random.nextFloat() * (SHATTER - 1.0F);
      }
   }

   @Override
   protected void onSustainedStart(int duration) {
      this.endAge = Math.max(1, duration);
      this.lifetime = this.endAge + TAIL;
   }

   @Override
   public void onSustainedStop() {
      if (!this.stopped && this.age < this.endAge) {
         this.stopped = true;
         this.stopAge = this.age;
         this.lifetime = this.age + 10;
      }
   }

   @Override
   public Vec3 focus() {
      return this.center;
   }

   @Override
   public double cullDistance() {
      return this.radius + 192.0;
   }

   private float presence(float a) {
      float in = FxMath.window(a, 0.0F, 10.0F);
      float out = this.stopped ? 1.0F - FxMath.window(a, this.stopAge, this.stopAge + 10.0F) : 1.0F - FxMath.window(a, this.endAge + SHATTER, this.endAge + SHATTER + 10.0F);
      return Math.min(in, out);
   }

   private boolean finale(float a) {
      return !this.stopped && a >= this.endAge;
   }

   @Override
   public void tick(ClientLevel level) {
      if (!this.stopped && this.ageTicks() == this.endAge + SHATTER) {
         level.playLocalSound(this.sphereCenter.x, this.sphereCenter.y, this.sphereCenter.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 4.0F, 0.45F, false);
         level.playLocalSound(this.sphereCenter.x, this.sphereCenter.y, this.sphereCenter.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 0.6F, false);
         level.playLocalSound(this.sphereCenter.x, this.sphereCenter.y, this.sphereCenter.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 3.0F, 0.5F, false);
      }
   }

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      float a = this.age(frame.partialTick);
      float presence = this.presence(a);
      Vector3f ground = frame.rel(this.center.add(0.0, 0.06, 0.0));

      // 1. 夜陨法阵 + 次元边界环
      if (presence > 0.0F) {
         float circle = 7.0F;
         g.begin(Style.RUNE, 3.0F, 0.0F, a * 0.03F, this.seedFloat, 1.0F, FxMath.easeOutCubic(FxMath.window(a, 0.0F, 12.0F)));
         g.quad(ground, new Vector3f(circle, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, circle), VOID[0], VOID[1], VOID[2], presence);
         g.begin(Style.RING, 2.2F, 0.0F, 0.985F, 0.006F, 120.0F, 0.15F);
         g.quad(new Vector3f(ground).add(0.0F, 0.04F, 0.0F), new Vector3f(this.radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, this.radius), VOID[0], VOID[1], VOID[2], presence);
         g.end();

         // 边界上的次元裂隙闪现：按 tick 哈希选择角度，短暂出现一道竖直裂痕
         g.begin(Style.RIBBON, 7.0F, 0.3F, 8.0F, 6.0F, 10.0F, 0.4F);
         int tick = (int)a;
         for (int k = 0; k < 3; k++) {
            float h = hash(tick * 3 + k);
            float local = a - tick;
            float alpha = presence * (1.0F - local) * 0.9F;
            double angle = h * Math.PI * 2.0;
            Vec3 base = this.center.add(Math.cos(angle) * this.radius, 0.0, Math.sin(angle) * this.radius);
            double height = 4.0 + hash(tick * 7 + k) * 6.0;
            List<Vector3f> crack = List.of(frame.rel(base), frame.rel(base.add(0.6 * (hash(tick + k * 13) - 0.5), height * 0.5, 0.6 * (hash(tick + k * 17) - 0.5))), frame.rel(base.add(0.0, height, 0.0)));
            g.ribbon(crack, new float[]{0.02F, 0.12F, 0.02F}, new float[]{alpha, alpha, alpha}, WHITE[0], WHITE[1], WHITE[2]);
         }
         g.end();
      }

      // 2. 终幕：横贯全域的巨型斩痕
      if (this.finale(a)) {
         float t = a - this.endAge;
         float fade = 1.0F - FxMath.window(t, SHATTER + 1.0F, SHATTER + 8.0F);
         if (fade > 0.0F) {
            g.begin(Style.RIBBON, 3.0F, 0.6F, 3.0F, 4.0F, 8.0F, 0.9F);
            this.drawFinaleLines(g, frame, t, fade, 0.9F, VOID);
            g.begin(Style.RIBBON, 12.0F, 0.6F, 10.0F, 6.0F, 12.0F, 0.2F);
            this.drawFinaleLines(g, frame, t, fade, 0.22F, WHITE);
            g.end();
         }
         float shock = FxMath.window(t, SHATTER, SHATTER + 16.0F);
         if (shock > 0.0F && shock < 1.0F) {
            float r = this.radius * 1.3F * FxMath.easeOutCubic(shock);
            g.begin(Style.RING, 5.0F * (1.0F - shock), 0.0F, 0.95F, 0.03F, 0.0F, 0.0F);
            g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), WHITE[0], WHITE[1], WHITE[2], 1.0F - shock);
            g.end();
         }
      }
   }

   private void drawFinaleLines(FxGeometry g, FxFrame frame, float t, float fade, float width, float[] color) {
      for (int i = 0; i < FINALE_LINES; i++) {
         float head = FxMath.easeOutCubic(FxMath.window(t, this.finaleDelay[i], this.finaleDelay[i] + 1.2F));
         if (head <= 0.0F) {
            continue;
         }
         Vec3 end = this.finaleStarts[i].lerp(this.finaleEnds[i], head);
         List<Vector3f> line = List.of(frame.rel(this.finaleStarts[i]), frame.rel(this.finaleStarts[i].lerp(end, 0.5)), frame.rel(end));
         g.ribbon(line, new float[]{0.0F, width, width * 0.3F}, new float[]{fade, fade, fade}, color[0], color[1], color[2]);
      }
   }

   // ───────────────────────────── 终幕碎裂（复用空间破碎通道）─────────────────────────────

   @Override
   public void collectPasses(FxFrame frame, List<FxPass> out) {
      float a = this.age(frame.partialTick);
      if (!this.finale(a)) {
         return;
      }
      float t = a - this.endAge;
      if (t < SHATTER || t > SHATTER + 26.0F) {
         return;
      }
      float local = t - SHATTER;
      Vector3f c = frame.rel(this.sphereCenter);
      float sphere = this.radius * 0.85F;
      float reveal = FxMath.easeOutCubic(FxMath.window(local, 0.0F, 3.0F));
      float collapse = FxMath.easeInCubic(FxMath.window(local, 4.0F, 16.0F));
      float fade = 1.0F - FxMath.window(local, 12.0F, 26.0F);
      float shock = FxMath.window(local, 0.0F, 16.0F);
      float shockRadius = this.radius * 1.3F * FxMath.easeOutCubic(shock);
      float shockStrength = (1.0F - shock) * (1.0F - shock);
      float flash = Math.max(0.0F, 1.0F - local / 4.0F);
      Consumer<ShaderInstance> uniforms = shader -> {
         shader.safeGetUniform("Sphere").set(c.x, c.y, c.z, sphere);
         shader.safeGetUniform("Phase").set(reveal, collapse, fade, this.seedFloat);
         shader.safeGetUniform("Shock").set(shockRadius, shockStrength, Math.max(1.0F, this.radius * 0.05F), 0.0F);
         shader.safeGetUniform("Extra").set(flash, 0.12F + collapse * 0.55F, sphere * 0.16F, 0.0F);
      };
      out.add(new FxPass(FxPass.Kind.SCENE, () -> FxShaders.fractureScene, uniforms));
      out.add(new FxPass(FxPass.Kind.VOLUME, () -> FxShaders.fractureVolume, uniforms));
   }

   // ───────────────────────────── 屏幕效果 ─────────────────────────────

   @Override
   public void screen(FxFrame frame, FxScreenState screen) {
      float a = this.age(frame.partialTick);
      double distance = frame.camera.distanceTo(this.center);
      float att = (float)Math.max(0.0, 1.0 - Math.max(0.0, distance - this.radius) / 128.0);
      if (att <= 0.0F) {
         return;
      }
      if (distance < this.radius) {
         screen.addVignette(0x0E0218, 0.25F * this.presence(a));
      }
      if (this.finale(a)) {
         float t = a - this.endAge;
         float[] uv = frame.screenUv(this.sphereCenter);
         if (t < SHATTER) {
            screen.addAberration(0.4F * (t / SHATTER) * att);
         } else {
            float local = t - SHATTER;
            if (local < 2.0F) {
               screen.addImpactFrame(att, local >= 1.0F);
            }
            float flash = local < 2.0F ? 0.0F : Math.max(0.0F, 1.0F - (local - 2.0F) / 6.0F);
            screen.addFlash(0xF2E6FF, 0.7F * flash * flash * att);
            screen.addAberration(1.2F * Math.max(0.0F, 1.0F - local / 14.0F) * att);
            screen.addZoomBlur(0.6F * Math.max(0.0F, 1.0F - local / 10.0F) * att, uv);
            screen.addBloomBoost(1.0F * Math.max(0.0F, 1.0F - local / 10.0F) * att);
            float kick = local < 2.0F ? local / 2.0F : 1.0F - FxMath.easeInOutSine(FxMath.window(local, 2.0F, 12.0F));
            screen.addFovKick(0.1F * kick * att);
         }
      }
   }

   @Override
   public float shake(FxFrame frame) {
      float a = this.age(frame.partialTick);
      if (!this.finale(a)) {
         return 0.0F;
      }
      float local = a - this.endAge - SHATTER;
      if (local < 0.0F) {
         return 0.0F;
      }
      double distance = frame.camera.distanceTo(this.center);
      float att = (float)Math.max(0.0, 1.0 - Math.max(0.0, distance - this.radius) / 96.0);
      return 0.8F * (1.0F - FxMath.window(local, 0.0F, 16.0F)) * att;
   }

   private float hash(int n) {
      int x = n * 374761393 + (int)(this.seedFloat * 1000.0F) * 668265263;
      x = (x ^ (x >>> 13)) * 1274126177;
      return ((x ^ (x >>> 16)) & 0xFFFFFF) / (float)0xFFFFFF;
   }
}
