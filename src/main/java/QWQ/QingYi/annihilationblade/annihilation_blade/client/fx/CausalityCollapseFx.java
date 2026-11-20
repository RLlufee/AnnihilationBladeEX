package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 湮灭之刃 SE「因果坍缩·连锁抹消」专属渲染（一次性）。
 * <p>
 * 意象「命运之链」：一条因果丝把刀与敌人逐个串起、锁死，再从末端逆流回溯，整条因果被抹消。
 * <pre>
 *  传导     因果脉冲光珠自刀身出发，每 tick 走过 2.5 个节点；身后留下青色主丝与金 / 青双螺旋
 *  上锁     光珠抵达处套上两枚交错旋转的金色因果锁环与一圈钟面刻度，旋入收紧后「咔」地扣死
 *  绷紧     刚连起时链略微下垂，全链连通后被拉直绷紧，丝上闪过一道张力光
 *  因果倒流 自最后一个节点起逆向回溯：钟面刻度飞速逆转，锁环向内塌缩，反向涟漪向内收拢（时间倒流），
 *           身后的链段碎成短段坠落
 *  因果闭环 回到第一个节点时，一枚水平金环猛地合拢并爆出星芒，随后散开一圈淡光，钟鸣
 * </pre>
 * 与裂界的区别在拓扑：裂界是以伤口为中心的星形牵引，这里是一条贯穿所有目标的路径。
 */
@OnlyIn(Dist.CLIENT)
public final class CausalityCollapseFx extends FxInstance {
   private static final float START = 0.5F;
   private static final float NODE_INTERVAL = 0.4F;
   private static final float KNOT_LOCK = 2.0F;
   private static final float HOLD = 3.0F;
   private static final float REWIND_INTERVAL = 0.45F;
   private static final float SHATTER_LIFE = 10.0F;
   private static final float FINAL_LIFE = 12.0F;
   private static final float SNAP = 2.5F;
   private static final int SEGMENT_POINTS = 16;
   private static final int SHARDS = 6;
   private static final int SOUND_SAMPLES = 6;

   private static final float[] CYAN = {0.2F, 0.85F, 0.95F};
   private static final float[] DEEP_CYAN = {0.1F, 0.5F, 0.8F};
   private static final float[] GOLD = {1.0F, 0.76F, 0.36F};
   private static final float[] PALE_GOLD = {1.0F, 0.9F, 0.65F};
   private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

   private final float scale;
   private final List<Vec3> nodes = new ArrayList<>();
   private final List<Float> heights = new ArrayList<>();
   private final float[] arrive;
   private final float[] collapse;
   private final float[] shatter;
   private final Vec3[] knotAxisA;
   private final Vec3[] knotAxisB;
   private final Vec3[][] shardVelocity;
   private final float allLit;
   private final float rewindStart;
   private final float finalAt;
   private final RandomSource random;

   public CausalityCollapseFx(ClientLevel level, FxEventPacket packet) {
      super(40);
      this.scale = Math.max(0.4F, Math.min(3.0F, packet.scale()));
      this.random = RandomSource.create(packet.seed());
      this.nodes.add(packet.origin());
      this.heights.add(1.0F);
      for (int id : packet.entityIds()) {
         Entity entity = level.getEntity(id);
         if (entity != null) {
            this.nodes.add(entity.getBoundingBox().getCenter());
            this.heights.add(entity.getBbHeight());
         }
      }
      int n = this.nodes.size() - 1;
      this.arrive = new float[n + 1];
      this.collapse = new float[n + 1];
      this.shatter = new float[n + 1];
      this.knotAxisA = new Vec3[n + 1];
      this.knotAxisB = new Vec3[n + 1];
      this.shardVelocity = new Vec3[n + 1][SHARDS];
      for (int i = 0; i <= n; i++) {
         this.arrive[i] = START + i * NODE_INTERVAL;
         double yaw = this.random.nextDouble() * Math.PI * 2.0;
         this.knotAxisA[i] = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
         this.knotAxisB[i] = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
         for (int s = 0; s < SHARDS; s++) {
            this.shardVelocity[i][s] = new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() * 0.5, this.random.nextDouble() - 0.5).scale(0.12);
         }
      }
      this.allLit = this.arrive[n] + KNOT_LOCK;
      this.rewindStart = this.allLit + HOLD;
      for (int i = 1; i <= n; i++) {
         this.collapse[i] = this.rewindStart + (n - i) * REWIND_INTERVAL;
      }
      this.finalAt = n >= 1 ? this.collapse[1] + 0.6F : this.rewindStart;
      for (int j = 2; j <= n; j++) {
         this.shatter[j] = this.collapse[j] + REWIND_INTERVAL;
      }
      if (n >= 1) {
         this.shatter[1] = this.finalAt;
      }
      this.lifetime = (int)Math.ceil(this.finalAt + FINAL_LIFE + 2.0F);
   }

   private int count() {
      return this.nodes.size() - 1;
   }

   @Override
   public Vec3 focus() {
      return this.nodes.size() > 1 ? this.nodes.get(1) : this.nodes.get(0);
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }

   // ───────────────────────────── 逻辑 tick ─────────────────────────────

   @Override
   public void tick(ClientLevel level) {
      int n = this.count();
      if (n <= 0) {
         return;
      }
      int age = this.ageTicks();
      int stride = Math.max(1, n / SOUND_SAMPLES);
      for (int i = 1; i <= n; i += stride) {
         Vec3 node = this.nodes.get(i);
         if ((int)Math.floor(this.arrive[i] + KNOT_LOCK) == age) {
            play(level, node, SoundEvents.CHAIN_PLACE, 0.4F, 1.5F + this.random.nextFloat() * 0.4F);
         }
         if ((int)Math.floor(this.collapse[i]) == age) {
            play(level, node, SoundEvents.CHAIN_BREAK, 0.4F, 1.3F + this.random.nextFloat() * 0.3F);
         }
      }
      if ((int)Math.floor(this.finalAt + SNAP) == age) {
         Vec3 anchor = this.nodes.get(1);
         play(level, anchor, SoundEvents.BELL_BLOCK, 0.7F, 1.6F);
         play(level, anchor, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.5F);
      }
   }

   private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   @Override
   public void tickFallback(ClientLevel level) {
      int n = this.count();
      int age = this.ageTicks();
      for (int i = 1; i <= n; i++) {
         if ((int)Math.floor(this.arrive[i] + KNOT_LOCK) == age) {
            Vec3 node = this.nodes.get(i);
            for (int k = 0; k < 4; k++) {
               level.addParticle(ParticleTypes.ENCHANTED_HIT, node.x, node.y, node.z, 0.0, 0.05, 0.0);
            }
         }
      }
      if (n >= 1 && (int)Math.floor(this.finalAt + SNAP) == age) {
         Vec3 anchor = this.nodes.get(1);
         for (int k = 0; k < 8; k++) {
            double angle = Math.PI * 2.0 * k / 8.0;
            level.addParticle(ParticleTypes.END_ROD, anchor.x + Math.cos(angle) * 0.8, anchor.y, anchor.z + Math.sin(angle) * 0.8, 0.0, 0.0, 0.0);
         }
      }
   }

   // ───────────────────────────── 几何工具 ─────────────────────────────

   /** 段 j（节点 j-1 → j）上参数 k 处的位置，带随时间收紧的下垂。 */
   private Vec3 pointOn(int j, double k, float t) {
      Vec3 a = this.nodes.get(j - 1);
      Vec3 b = this.nodes.get(j);
      double length = a.distanceTo(b);
      float tighten = FxMath.easeOutCubic(FxMath.window(t, this.arrive[j], this.allLit + 1.0F));
      double sag = 0.22 * Math.min(length, 12.0) * (1.0 - tighten) * Math.sin(Math.PI * k);
      return a.lerp(b, k).subtract(0.0, sag, 0.0);
   }

   private static float nearFade(Vector3f rel) {
      return FxMath.window(rel.length(), 0.8F, 2.5F);
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      int n = this.count();
      if (n <= 0) {
         return;
      }
      float t = this.age(frame.partialTick);
      float tension = FxMath.envelope(FxMath.window(t, this.allLit - 0.5F, this.allLit + 2.0F), 0.25F, 0.4F);

      // 1. 因果主丝：青色外丝 + 白热内芯
      g.begin(Style.RIBBON, 6.0F + 6.0F * tension, 0.2F, 6.0F, 5.0F, 7.0F, 0.5F);
      for (int j = 1; j <= n; j++) {
         this.drawCore(g, frame, j, t, 0.035F * this.scale, CYAN, 1.0F);
      }
      g.begin(Style.RIBBON, 10.0F, 0.2F, 10.0F, 0.0F, 0.0F, 0.2F);
      for (int j = 1; j <= n; j++) {
         this.drawCore(g, frame, j, t, 0.011F * this.scale, WHITE, 0.8F + 0.2F * tension);
      }

      // 2. 因果双螺旋：金 / 青两股细丝缠绕主丝
      g.begin(Style.RIBBON, 4.0F, 0.2F, 7.0F, 0.0F, 0.0F, 0.4F);
      for (int j = 1; j <= n; j++) {
         this.drawHelix(g, frame, j, t, 0.0F, GOLD);
         this.drawHelix(g, frame, j, t, (float)Math.PI, DEEP_CYAN);
      }

      // 3. 碎段、光珠拖尾
      g.begin(Style.RIBBON, 5.0F, 0.2F, 6.0F, 0.0F, 0.0F, 0.4F);
      for (int j = 1; j <= n; j++) {
         float e = t - this.shatter[j];
         if (e < 0.0F || e >= SHATTER_LIFE) {
            continue;
         }
         float k = 1.0F - e / SHATTER_LIFE;
         Vec3 a = this.nodes.get(j - 1);
         Vec3 b = this.nodes.get(j);
         Vec3 unit = b.subtract(a).normalize();
         double pieceLength = a.distanceTo(b) / SHARDS * 0.7 * k;
         for (int s = 0; s < SHARDS; s++) {
            Vec3 mid = a.lerp(b, (s + 0.5) / SHARDS).add(this.shardVelocity[j][s].scale(e)).add(0.0, -0.012 * e * e, 0.0);
            Vec3 half = unit.scale(pieceLength * 0.5);
            Vector3f rel = frame.rel(mid);
            float[] tint = s % 2 == 0 ? CYAN : GOLD;
            g.streak(frame.rel(mid.add(half)), frame.rel(mid.subtract(half)), 0.03F * this.scale * k, tint[0], tint[1], tint[2], (float)Math.pow(k, 1.2) * nearFade(rel));
         }
      }
      Vec3 forward = this.beadForward(t);
      Vec3 backward = this.beadBackward(t);
      if (forward != null) {
         Vec3 trail = this.beadForward(t - 0.5F);
         if (trail != null) {
            Vector3f head = frame.rel(forward);
            g.streak(head, frame.rel(trail), 0.07F * this.scale, CYAN[0], CYAN[1], CYAN[2], nearFade(head));
         }
      }
      if (backward != null) {
         Vec3 trail = this.beadBackward(t - 0.4F);
         if (trail != null) {
            Vector3f head = frame.rel(backward);
            g.streak(head, frame.rel(trail), 0.08F * this.scale, GOLD[0], GOLD[1], GOLD[2], nearFade(head));
         }
      }

      // 4. 因果锁环（两枚交错旋转的金环）
      g.begin(Style.RIBBON, 4.5F, 0.3F, 6.0F, 0.0F, 0.0F, 0.5F);
      for (int i = 1; i <= n; i++) {
         this.drawKnotRings(g, frame, i, t);
      }

      // 5. 钟面刻度、反向涟漪、闭环金环
      g.begin(Style.RING, 3.6F, 0.3F, 0.95F, 0.03F, 12.0F, 0.0F);
      for (int i = 1; i <= n; i++) {
         this.drawDial(g, frame, i, t);
      }
      g.begin(Style.RING, 4.0F, 0.3F, 0.95F, 0.04F, 0.0F, 0.0F);
      for (int i = 1; i <= n; i++) {
         this.drawInverseRipple(g, frame, i, t);
      }
      this.drawFinalRings(g, frame, t);

      // 6. 公告板：光珠、上锁闪光、坍缩点、闭环星芒
      g.begin(Style.GLOW, 8.0F, 0.0F, 6.0F, 3.0F, 0.9F, 0.0F);
      if (forward != null) {
         Vector3f p = frame.rel(forward);
         g.billboard(p, 0.45F * this.scale, 0.0F, CYAN[0], CYAN[1], CYAN[2], nearFade(p));
         g.billboard(p, 0.18F * this.scale, 0.0F, WHITE[0], WHITE[1], WHITE[2], nearFade(p));
      }
      if (backward != null) {
         Vector3f p = frame.rel(backward);
         g.billboard(p, 0.5F * this.scale, 0.0F, GOLD[0], GOLD[1], GOLD[2], nearFade(p));
      }
      for (int i = 1; i <= n; i++) {
         Vector3f p = frame.rel(this.nodes.get(i));
         float lock = t - (this.arrive[i] + KNOT_LOCK);
         if (lock >= 0.0F && lock < 2.0F) {
            float k = 1.0F - lock / 2.0F;
            g.billboard(p, 0.9F * this.scale, 0.0F, PALE_GOLD[0], PALE_GOLD[1], PALE_GOLD[2], k * k * nearFade(p));
         }
         float pop = t - (this.collapse[i] + 1.5F);
         if (pop >= 0.0F && pop < 2.5F) {
            float k = 1.0F - pop / 2.5F;
            g.billboard(p, 1.1F * this.scale, (float)(Math.PI * 0.25), WHITE[0], WHITE[1], WHITE[2], k * k * nearFade(p));
         }
      }
      float e = t - this.finalAt;
      if (e >= SNAP && e < SNAP + 4.0F) {
         Vector3f p = frame.rel(this.nodes.get(1));
         float k = 1.0F - (e - SNAP) / 4.0F;
         float size = (1.2F + 1.4F * (1.0F - k)) * this.scale;
         g.billboard(p, size, 0.0F, WHITE[0], WHITE[1], WHITE[2], k * nearFade(p));
         g.billboard(p, size, (float)(Math.PI * 0.5), GOLD[0], GOLD[1], GOLD[2], k * nearFade(p));
      }
      g.end();
   }

   private void drawCore(FxGeometry g, FxFrame frame, int j, float t, float width, float[] color, float alphaScale) {
      float lit = FxMath.window(t, this.arrive[j - 1], this.arrive[j]);
      if (lit <= 0.0F || t >= this.shatter[j]) {
         return;
      }
      List<Vector3f> points = new ArrayList<>(SEGMENT_POINTS);
      float[] widths = new float[SEGMENT_POINTS];
      float[] alphas = new float[SEGMENT_POINTS];
      for (int i = 0; i < SEGMENT_POINTS; i++) {
         double k = lit * i / (double)(SEGMENT_POINTS - 1);
         Vector3f p = frame.rel(this.pointOn(j, k, t));
         points.add(p);
         widths[i] = width;
         alphas[i] = alphaScale * nearFade(p) * (i == SEGMENT_POINTS - 1 && lit < 1.0F ? 0.4F : 1.0F);
      }
      g.ribbon(points, widths, alphas, color[0], color[1], color[2]);
   }

   private void drawHelix(FxGeometry g, FxFrame frame, int j, float t, float phase, float[] color) {
      float lit = FxMath.window(t, this.arrive[j - 1], this.arrive[j]);
      if (lit <= 0.0F || t >= this.shatter[j]) {
         return;
      }
      Vec3 a = this.nodes.get(j - 1);
      Vec3 b = this.nodes.get(j);
      Vec3 axis = b.subtract(a);
      double length = axis.length();
      if (length < 0.3) {
         return;
      }
      Vec3 unit = axis.scale(1.0 / length);
      Vec3 side = unit.cross(UP);
      side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
      Vec3 up = side.cross(unit).normalize();
      double turns = Math.max(2.0, length / 1.2);
      double radius = 0.13 * this.scale;
      int samples = Math.max(SEGMENT_POINTS, (int)(turns * 8.0));
      List<Vector3f> points = new ArrayList<>(samples);
      float[] widths = new float[samples];
      float[] alphas = new float[samples];
      for (int i = 0; i < samples; i++) {
         double k = lit * i / (double)(samples - 1);
         double angle = k * turns * Math.PI * 2.0 + t * 0.25 + phase;
         double r = radius * Math.sqrt(Math.sin(Math.PI * k));
         Vec3 p = this.pointOn(j, k, t).add(side.scale(Math.cos(angle) * r)).add(up.scale(Math.sin(angle) * r));
         Vector3f rel = frame.rel(p);
         points.add(rel);
         widths[i] = 0.016F * this.scale;
         alphas[i] = 0.85F * nearFade(rel);
      }
      g.ribbon(points, widths, alphas, color[0], color[1], color[2]);
   }

   /** 正向传导光珠的位置（未在传导期返回 null）。 */
   private Vec3 beadForward(float t) {
      int n = this.count();
      if (t < this.arrive[0] || t >= this.arrive[n]) {
         return null;
      }
      for (int j = 1; j <= n; j++) {
         if (t < this.arrive[j]) {
            return this.pointOn(j, FxMath.window(t, this.arrive[j - 1], this.arrive[j]), t);
         }
      }
      return null;
   }

   /** 因果倒流光珠的位置：从最后一个节点逆向回到第一个节点。 */
   private Vec3 beadBackward(float t) {
      int n = this.count();
      if (n < 1 || t < this.collapse[n] || t >= this.finalAt) {
         return null;
      }
      for (int j = n; j >= 2; j--) {
         if (t < this.collapse[j - 1]) {
            double k = FxMath.window(t, this.collapse[j], this.collapse[j - 1]);
            return this.nodes.get(j).lerp(this.nodes.get(j - 1), k);
         }
      }
      return this.nodes.get(1);
   }

   private float knotRadius(int i) {
      return Math.max(0.55F, this.heights.get(i) * 0.45F) * this.scale;
   }

   /** 锁环与钟面的整体缩放：旋入 1.6 → 1，坍缩时塌缩到 0。 */
   private float knotScale(int i, float t) {
      float a = t - this.arrive[i];
      if (a < 0.0F) {
         return 0.0F;
      }
      float spinIn = 1.6F - 0.6F * FxMath.easeOutCubic(FxMath.window(a, 0.0F, KNOT_LOCK));
      float implode = 1.0F - FxMath.easeInCubic(FxMath.window(t, this.collapse[i], this.collapse[i] + 1.5F));
      return spinIn * implode;
   }

   private void drawKnotRings(FxGeometry g, FxFrame frame, int i, float t) {
      float size = this.knotScale(i, t);
      if (size <= 0.01F) {
         return;
      }
      float a = t - this.arrive[i];
      float theta = a < KNOT_LOCK ? a * 1.6F : KNOT_LOCK * 1.6F + (a - KNOT_LOCK) * 0.15F;
      float alpha = FxMath.window(a, 0.0F, 0.6F);
      Vec3 axisA = UP.scale(Math.cos(theta)).add(this.knotAxisA[i].scale(Math.sin(theta)));
      Vec3 axisB = this.knotAxisB[i].scale(Math.cos(theta * 0.7)).add(UP.scale(Math.sin(theta * 0.7) * 0.4));
      Vector3f c = frame.rel(this.nodes.get(i));
      float fade = alpha * nearFade(c);
      float radius = this.knotRadius(i) * size;
      g.torus(c, toVector(axisA.normalize()), radius, 0.035F * this.scale, 28, 5, theta, GOLD[0], GOLD[1], GOLD[2], fade);
      g.torus(c, toVector(axisB.normalize()), radius * 0.82F, 0.03F * this.scale, 24, 5, -theta, PALE_GOLD[0], PALE_GOLD[1], PALE_GOLD[2], fade);
   }

   private void drawDial(FxGeometry g, FxFrame frame, int i, float t) {
      float size = this.knotScale(i, t);
      if (size <= 0.01F) {
         return;
      }
      float a = t - this.arrive[i];
      float rotation = -a * 0.05F;
      // 因果倒流：坍缩前后刻度飞速逆转
      rotation += FxMath.easeInCubic(FxMath.window(t, this.collapse[i] - 0.5F, this.collapse[i] + 1.5F)) * (float)(Math.PI * 6.0);
      Vector3f c = frame.rel(this.nodes.get(i));
      float radius = this.knotRadius(i) * 1.25F * size / 0.95F;
      float cos = (float)Math.cos(rotation);
      float sin = (float)Math.sin(rotation);
      Vector3f u = new Vector3f(frame.left).mul(cos).add(new Vector3f(frame.up).mul(sin)).mul(radius);
      Vector3f v = new Vector3f(frame.up).mul(cos).sub(new Vector3f(frame.left).mul(sin)).mul(radius);
      g.quad(c, u, v, CYAN[0], CYAN[1], CYAN[2], FxMath.window(a, 0.5F, 1.5F) * 0.8F * nearFade(c));
   }

   /** 反向涟漪：坍缩时两圈环（水平 + 正对相机）由外向内收拢，越收越亮。 */
   private void drawInverseRipple(FxGeometry g, FxFrame frame, int i, float t) {
      float e = t - this.collapse[i];
      if (e < 0.0F || e >= 3.0F) {
         return;
      }
      float k = e / 3.0F;
      float radius = this.knotRadius(i) * 2.6F * (1.0F - FxMath.easeInCubic(k)) / 0.95F + 0.02F;
      float alpha = (0.3F + 0.7F * k) * (1.0F - FxMath.window(e, 2.6F, 3.0F));
      Vector3f c = frame.rel(this.nodes.get(i));
      float fade = alpha * nearFade(c);
      g.quad(c, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), CYAN[0], CYAN[1], CYAN[2], fade);
      g.quad(c, new Vector3f(frame.left).mul(radius * 0.8F), new Vector3f(frame.up).mul(radius * 0.8F), WHITE[0], WHITE[1], WHITE[2], fade * 0.7F);
   }

   /** 因果闭环：第一个节点处的水平金环猛然合拢，随后散开一圈淡光。 */
   private void drawFinalRings(FxGeometry g, FxFrame frame, float t) {
      float e = t - this.finalAt;
      if (e < 0.0F || e >= FINAL_LIFE) {
         return;
      }
      Vector3f c = frame.rel(this.nodes.get(1));
      float base = 3.2F * this.scale;
      if (e < 6.0F) {
         float snap = FxMath.easeInCubic(FxMath.window(e, 0.0F, SNAP));
         float radius = (base - (base - 0.35F * this.scale) * snap) / 0.95F;
         float alpha = FxMath.window(e, 0.0F, 0.5F) * (1.0F - FxMath.window(e, 4.0F, 6.0F));
         g.quad(c, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), GOLD[0], GOLD[1], GOLD[2], alpha * nearFade(c));
      }
      if (e >= SNAP) {
         float k = FxMath.window(e, SNAP, FINAL_LIFE);
         float radius = (0.35F + 5.0F * FxMath.easeOutCubic(k)) * this.scale / 0.95F;
         g.quad(c, new Vector3f(radius, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, radius), PALE_GOLD[0], PALE_GOLD[1], PALE_GOLD[2], (1.0F - k) * (1.0F - k) * nearFade(c));
      }
   }

   private static Vector3f toVector(Vec3 v) {
      return new Vector3f((float)v.x, (float)v.y, (float)v.z);
   }
}
