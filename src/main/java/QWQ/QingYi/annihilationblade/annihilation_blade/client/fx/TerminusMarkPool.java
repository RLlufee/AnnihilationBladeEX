package QWQ.QingYi.annihilationblade.annihilation_blade.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxManager;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 湮灭之刃 · 终焉印记（普通命中触发终焉判定时的表现）。
 * <p>
 * 意象「归零」：目标腰间的金色光圈像相机光圈叶片一样旋转收束成一点；收束瞬间针尖白闪，
 * 横向星芒与竖直金色发丝构成一瞬即逝的十字准星，几颗金色余烬缓缓上浮。
 * <pre>
 *  0 ─ 6    外层 6 段 / 内层 12 段虚线光圈反向旋转、加速收束
 *  6        针尖白闪 + 横向星芒 + 竖直发丝（十字准星）
 *  6 ─ 16   余烬减速上浮并淡出
 * </pre>
 * 命中非常频繁，因此所有印记并入同一个池化实例：工厂只把印记追加进池子并返回 {@code null}，
 * 不会为每次命中新建实例占用 {@link FxManager} 的实例上限；绘制按图元种类合批，整池固定 4 次绘制调用。
 */
@OnlyIn(Dist.CLIENT)
public final class TerminusMarkPool extends FxInstance {
   private static final int CLOSE = 6;
   private static final int MARK_LIFE = 16;
   private static final int MAX_MARKS = 256;
   private static final double RENDER_DISTANCE_SQR = 96.0 * 96.0;
   private static final int EMBERS = 4;

   private static final float[] GOLD = {1.0F, 0.76F, 0.36F};
   private static final float[] PALE_GOLD = {1.0F, 0.88F, 0.62F};
   private static final float[] WHITE = {1.0F, 0.97F, 0.92F};

   @Nullable
   private static TerminusMarkPool active;

   private final List<Mark> marks = new ArrayList<>();
   private boolean removed;

   private TerminusMarkPool() {
      super(MARK_LIFE);
   }

   /** 调度器工厂：把印记并入池子并返回 null（池子在首次创建时已加入管理器）。 */
   @Nullable
   public static FxInstance accept(ClientLevel level, FxEventPacket packet) {
      TerminusMarkPool pool = active;
      if (pool == null || pool.removed || pool.finished()) {
         pool = new TerminusMarkPool();
         active = pool;
         FxManager.add(pool);
      }
      pool.add(level, packet);
      return null;
   }

   private void add(ClientLevel level, FxEventPacket packet) {
      int[] ids = packet.entityIds();
      int entityId = ids.length > 0 ? ids[0] : -1;
      Entity entity = entityId >= 0 ? level.getEntity(entityId) : null;
      Vec3 center = entity != null ? entity.getBoundingBox().getCenter() : packet.origin();
      float width = entity != null ? entity.getBbWidth() : Math.max(0.3F, packet.radius());
      float height = entity != null ? entity.getBbHeight() : Math.max(0.3F, packet.scale());
      if (this.marks.size() >= MAX_MARKS) {
         this.marks.remove(0);
      }
      this.marks.add(new Mark(entityId, center, width, height, packet.seed(), this.age));
      this.extendLife(MARK_LIFE + 1);
   }

   @Override
   public void onRemoved() {
      this.removed = true;
      if (active == this) {
         active = null;
      }
   }

   /** 印记分散在各处，由池内逐个剔除；池子本身以相机为焦点，保证始终参与渲染。 */
   @Override
   public Vec3 focus() {
      return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
   }

   @Override
   public void tick(ClientLevel level) {
      Iterator<Mark> iterator = this.marks.iterator();
      while (iterator.hasNext()) {
         Mark mark = iterator.next();
         if (this.age - mark.born >= MARK_LIFE) {
            iterator.remove();
         } else {
            mark.follow(level);
         }
      }
      if (this.marks.isEmpty()) {
         this.lifetime = this.age;
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      for (Mark mark : this.marks) {
         if (this.age - mark.born == CLOSE) {
            Vec3 c = mark.center;
            for (int i = 0; i < 3; i++) {
               level.addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, mark.emberDx[i] * 0.6, 0.04 + mark.emberDy[i] * 0.05, mark.emberDz[i] * 0.6);
            }
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 0.0, 0.02, 0.0);
         }
      }
   }

   // ───────────────────────────── 网格层 ─────────────────────────────

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      if (this.marks.isEmpty()) {
         return;
      }
      List<View> views = new ArrayList<>(this.marks.size());
      for (Mark mark : this.marks) {
         float t = (this.age - mark.born) + frame.partialTick;
         if (t < 0.0F || t >= MARK_LIFE) {
            continue;
         }
         Vec3 world = mark.position(frame);
         if (world.distanceToSqr(frame.camera) > RENDER_DISTANCE_SQR) {
            continue;
         }
         views.add(new View(mark, frame.rel(world), t));
      }
      if (views.isEmpty()) {
         return;
      }

      // 1. 外层光圈：6 段虚线，加速旋转收束
      g.begin(Style.RING, 3.2F, 0.3F, 0.95F, 0.035F, 6.0F, 0.0F);
      for (View v : views) {
         float alpha = ringAlpha(v.t);
         if (alpha > 0.0F) {
            float close = FxMath.easeInCubic(FxMath.window(v.t, 0.0F, CLOSE));
            float radius = v.mark.ringRadius * (1.0F - 0.92F * close);
            float angle = v.mark.spin * (v.t * 0.25F + close * 2.4F);
            this.ring(g, v, radius, angle, GOLD, alpha);
         }
      }

      // 2. 内层光圈：12 段细虚线，反向旋转
      g.begin(Style.RING, 2.4F, 0.3F, 0.95F, 0.025F, 12.0F, 0.0F);
      for (View v : views) {
         float alpha = ringAlpha(v.t);
         if (alpha > 0.0F) {
            float close = FxMath.easeInCubic(FxMath.window(v.t, 0.0F, CLOSE));
            float radius = v.mark.ringRadius * 0.6F * (1.0F - 0.9F * close);
            float angle = -v.mark.spin * (v.t * 0.32F + close * 3.1F);
            this.ring(g, v, radius, angle, PALE_GOLD, alpha * 0.85F);
         }
      }

      // 3. 收束瞬间的针尖白闪（GLOW 自带横向星芒，构成十字准星的横线）
      g.begin(Style.GLOW, 11.0F, 0.0F, 6.0F, 3.0F, 1.0F, 0.0F);
      for (View v : views) {
         if (v.t < CLOSE || v.t > CLOSE + 5.0F) {
            continue;
         }
         float k = 1.0F - FxMath.window(v.t, CLOSE, CLOSE + 5.0F);
         float grow = 0.3F + 0.7F * FxMath.easeOutCubic(FxMath.window(v.t, CLOSE, CLOSE + 1.2F));
         float size = grow * Math.max(0.8F, v.mark.height * 0.45F);
         g.billboard(v.center, size, 0.0F, WHITE[0], WHITE[1], WHITE[2], k * k);
      }

      // 4. 竖直金色发丝（十字准星的竖线）与上浮余烬
      g.begin(Style.RIBBON, 5.0F, 0.2F, 6.0F, 0.0F, 0.0F, 0.5F);
      for (View v : views) {
         if (v.t < CLOSE) {
            continue;
         }
         float e = v.t - CLOSE;
         if (e < 4.0F) {
            float k = 1.0F - e / 4.0F;
            float half = v.mark.height * 0.8F * FxMath.easeOutCubic(FxMath.window(e, 0.0F, 1.0F));
            List<Vector3f> points = new ArrayList<>(3);
            points.add(new Vector3f(v.center).add(0.0F, half, 0.0F));
            points.add(new Vector3f(v.center));
            points.add(new Vector3f(v.center).add(0.0F, -half, 0.0F));
            g.ribbon(points, new float[]{0.004F, 0.035F, 0.004F}, new float[]{0.0F, k, 0.0F}, GOLD[0], GOLD[1], GOLD[2]);
         }
         for (int i = 0; i < EMBERS; i++) {
            float life = v.mark.emberLife[i];
            if (e >= life) {
               continue;
            }
            float travel = v.mark.emberSpeed[i] * e * (1.0F - e / (2.0F * life));
            float rise = 0.015F * e;
            Vector3f head = new Vector3f(v.center).add(
               v.mark.emberDx[i] * travel, v.mark.emberDy[i] * travel + rise, v.mark.emberDz[i] * travel
            );
            float tailLength = 0.22F * (1.0F - e / life) + 0.05F;
            Vector3f tail = new Vector3f(head).sub(v.mark.emberDx[i] * tailLength, v.mark.emberDy[i] * tailLength, v.mark.emberDz[i] * tailLength);
            float fade = (float)Math.pow(1.0F - e / life, 1.5);
            g.streak(head, tail, 0.028F, GOLD[0], GOLD[1], GOLD[2], fade);
         }
      }
      g.end();
   }

   private static float ringAlpha(float t) {
      return FxMath.window(t, 0.0F, 1.2F) * (1.0F - FxMath.window(t, CLOSE - 0.6F, CLOSE + 0.4F));
   }

   /** 在印记自身的倾斜平面内画一个光圈：radius 为实际环半径（RING 风格环位于 quad 的 0.95 处）。 */
   private void ring(FxGeometry g, View v, float radius, float angle, float[] color, float alpha) {
      if (radius <= 0.01F) {
         return;
      }
      float cos = (float)Math.cos(angle);
      float sin = (float)Math.sin(angle);
      float half = radius / 0.95F;
      Vector3f u = new Vector3f(v.mark.axisU).mul(cos).add(new Vector3f(v.mark.axisW).mul(sin)).mul(half);
      Vector3f w = new Vector3f(v.mark.axisW).mul(cos).sub(new Vector3f(v.mark.axisU).mul(sin)).mul(half);
      g.quad(v.center, u, w, color[0], color[1], color[2], alpha);
   }

   // ───────────────────────────── 数据结构 ─────────────────────────────

   private record View(Mark mark, Vector3f center, float t) {
   }

   private static final class Mark {
      final int entityId;
      final int born;
      final float height;
      final float ringRadius;
      final float spin;
      /** 光圈所在平面的两条正交基（带轻微随机倾斜，避免一群目标的光圈完全平行）。 */
      final Vector3f axisU;
      final Vector3f axisW;
      final float[] emberDx = new float[EMBERS];
      final float[] emberDy = new float[EMBERS];
      final float[] emberDz = new float[EMBERS];
      final float[] emberSpeed = new float[EMBERS];
      final float[] emberLife = new float[EMBERS];
      Vec3 center;

      Mark(int entityId, Vec3 center, float width, float height, long seed, int born) {
         this.entityId = entityId;
         this.center = center;
         this.height = height;
         this.born = born;
         this.ringRadius = Math.max(0.85F, width * 1.4F);
         RandomSource random = RandomSource.create(seed);
         this.spin = random.nextBoolean() ? 1.0F : -1.0F;
         Vector3f normal = new Vector3f((random.nextFloat() - 0.5F) * 0.4F, 1.0F, (random.nextFloat() - 0.5F) * 0.4F).normalize();
         this.axisU = FxMath.perpendicular(normal);
         this.axisW = new Vector3f(normal).cross(this.axisU).normalize();
         for (int i = 0; i < EMBERS; i++) {
            double yaw = (i + random.nextFloat() * 0.7) / EMBERS * Math.PI * 2.0;
            float up = 0.45F + random.nextFloat() * 0.45F;
            float horizontal = (float)Math.sqrt(1.0F - up * up);
            this.emberDx[i] = (float)Math.cos(yaw) * horizontal;
            this.emberDy[i] = up;
            this.emberDz[i] = (float)Math.sin(yaw) * horizontal;
            this.emberSpeed[i] = 0.07F + random.nextFloat() * 0.05F;
            this.emberLife[i] = 7.0F + random.nextFloat() * 3.0F;
         }
      }

      /** 目标仍在客户端时跟随其位置（被击退、死亡倒地动画期间印记不脱离身体）。 */
      void follow(ClientLevel level) {
         if (this.entityId < 0) {
            return;
         }
         Entity entity = level.getEntity(this.entityId);
         if (entity != null) {
            this.center = entity.getBoundingBox().getCenter();
         }
      }

      Vec3 position(FxFrame frame) {
         if (this.entityId >= 0) {
            Entity entity = frame.level.getEntity(this.entityId);
            if (entity != null) {
               return entity.getPosition(frame.partialTick).add(0.0, entity.getBbHeight() * 0.5, 0.0);
            }
         }
         return this.center;
      }
   }
}
