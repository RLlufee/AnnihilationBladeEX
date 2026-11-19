package QWQ.QingYi.annihilationblade.render.client.geometry;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * 客户端闪电（取代纯视觉的雷电实体）。
 * <p>
 * 生命周期与原版闪电一致：显示 2 tick → 熄灭 → 随机再闪 {@code flashes} 次（每次换一个形状）。
 * 形状为中点位移折线主干 + 一条分叉，由种子决定。
 */
@OnlyIn(Dist.CLIENT)
public final class FxLightningBolt {
   public final double x;
   public final double z;
   public final double top;
   public final double ground;
   public final float[] color;
   private int life = 2;
   private int flashes;
   private boolean fresh = true;
   private List<Vec3> trunk;
   private List<Vec3> branch;

   public FxLightningBolt(double x, double z, double top, double ground, long seed, int flashes, float[] color) {
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
      double height = Math.max(1.0, this.top - this.ground);
      Vec3 from = new Vec3(this.x + (random.nextDouble() - 0.5) * 8.0, this.top, this.z + (random.nextDouble() - 0.5) * 8.0);
      Vec3 to = new Vec3(this.x, this.ground, this.z);
      this.trunk = displace(from, to, 5, height * 0.06, random);
      int index = this.trunk.size() / 4 + random.nextInt(Math.max(1, this.trunk.size() / 3));
      Vec3 start = this.trunk.get(Math.min(index, this.trunk.size() - 2));
      double length = height * (0.2 + random.nextDouble() * 0.15);
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

   public void tick(RandomSource random) {
      this.fresh = false;
      this.life--;
      if (this.life < 0 && this.flashes > 0 && this.life < -random.nextInt(10)) {
         this.flashes--;
         this.life = 1;
         this.reshape(random.nextLong());
      }
   }

   /** 本 tick 刚生成（用于降级粒子与音效只触发一次）。 */
   public boolean fresh() {
      return this.fresh;
   }

   public boolean visible() {
      return this.life >= 0;
   }

   public boolean dead() {
      return this.life < 0 && this.flashes <= 0;
   }

   /** 绘制主干与分叉，调用前需已 {@code begin(Style.RIBBON, ...)}。 */
   public void draw(FxGeometry g, FxFrame frame, float widthScale, float alphaScale, float[] tint) {
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
