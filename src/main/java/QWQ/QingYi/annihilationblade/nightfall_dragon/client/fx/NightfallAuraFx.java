package QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx;

import QWQ.QingYi.annihilationblade.render.client.core.FxFrame;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry.Style;
import QWQ.QingYi.annihilationblade.render.client.geometry.FxMath;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
 * 魔龙夜陨终极形态的常驻光环（纯客户端，由 {@link NightfallAuraTracker} 按玩家手持状态维持寿命）。
 * <ul>
 *   <li>龙神之躯：脚下旋转的龙纹处决阵、环绕全身的两道龙魂螺旋、向上飘散的星屑。</li>
 *   <li>绝对湮灭结界：以持有者为中心的巨型地面结界环（常驻，亮度刻意压低），每秒向外扫出一道脉冲。</li>
 * </ul>
 * 取代服务端每 tick 发送的粒子（{@code spawnGodBodyAura} / {@code spawnDomainFrame}）。
 */
@OnlyIn(Dist.CLIENT)
public final class NightfallAuraFx extends FxInstance {
   private static final int FADE = 6;
   private static final float[] GOLD = {1.0F, 0.72F, 0.28F};
   private static final float[] VIOLET = {0.69F, 0.15F, 1.0F};

   private final int entityId;
   private final RandomSource random = RandomSource.create();
   private final List<Mote> motes = new ArrayList<>();
   private boolean godBody;
   private boolean domain;
   private float domainRadius = 64.0F;
   private float godBodyLevel;
   private float domainLevel;
   private Vec3 lastPosition = Vec3.ZERO;
   private boolean removed;

   public NightfallAuraFx(int entityId) {
      super(20);
      this.entityId = entityId;
   }

   public int entityId() {
      return this.entityId;
   }

   public boolean removed() {
      return this.removed || this.finished();
   }

   /** 由追踪器每 tick 更新当前状态并续命。 */
   public void update(boolean godBody, boolean domain, float domainRadius) {
      this.godBody = godBody;
      this.domain = domain;
      this.domainRadius = domainRadius;
      this.extendLife(FADE);
   }

   @Override
   public void onRemoved() {
      this.removed = true;
   }

   @Override
   public Vec3 focus() {
      return this.lastPosition;
   }

   @Override
   public double cullDistance() {
      return this.domainRadius + 96.0;
   }

   private float fade(float partialTick) {
      float in = FxMath.window(this.age(partialTick), 0.0F, 10.0F);
      float out = Math.min(1.0F, (this.remainingTicks() - partialTick) / FADE);
      return Math.max(0.0F, Math.min(in, out));
   }

   @Override
   public void tick(ClientLevel level) {
      Entity entity = level.getEntity(this.entityId);
      if (entity == null) {
         this.lifetime = this.age;
         return;
      }
      this.lastPosition = entity.position();
      // 状态切换时平滑过渡，而不是突然出现/消失
      this.godBodyLevel = approach(this.godBodyLevel, this.godBody ? 1.0F : 0.0F, 0.15F);
      this.domainLevel = approach(this.domainLevel, this.domain ? 1.0F : 0.0F, 0.1F);
      if (this.godBodyLevel > 0.05F && this.motes.size() < 40) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double r = 0.4 + this.random.nextDouble() * 0.6;
         this.motes.add(new Mote(entity.position().add(Math.cos(angle) * r, this.random.nextDouble() * entity.getBbHeight(), Math.sin(angle) * r), this.random.nextBoolean() ? GOLD : VIOLET, 18 + this.random.nextInt(14)));
      }
      Iterator<Mote> iterator = this.motes.iterator();
      while (iterator.hasNext()) {
         Mote mote = iterator.next();
         mote.tick();
         if (mote.life <= 0) {
            iterator.remove();
         }
      }
   }

   @Override
   public void tickFallback(ClientLevel level) {
      if (this.ageTicks() % 20 != 0 || this.godBodyLevel <= 0.05F) {
         return;
      }
      Vec3 foot = this.lastPosition;
      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * 2.0 * i / 8.0 + this.ageTicks() * 0.03;
         level.addParticle(ParticleTypes.DRAGON_BREATH, foot.x + Math.cos(angle) * 1.85, foot.y + 0.05, foot.z + Math.sin(angle) * 1.85, 0.0, 0.005, 0.0);
      }
   }

   @Override
   public void renderGeometry(FxGeometry g, FxFrame frame) {
      Entity entity = frame.level.getEntity(this.entityId);
      if (entity == null) {
         return;
      }
      float fade = this.fade(frame.partialTick);
      if (fade <= 0.0F) {
         return;
      }
      float a = this.age(frame.partialTick);
      Vec3 feet = entity.getPosition(frame.partialTick);
      Minecraft minecraft = Minecraft.getInstance();
      boolean firstPersonSelf = entity == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();

      // 龙神之躯
      float body = this.godBodyLevel * fade;
      if (body > 0.01F) {
         Vector3f foot = frame.rel(feet.add(0.0, 0.05, 0.0));
         float halo = 1.85F + (float)Math.sin(a * 0.15F) * 0.15F;
         g.begin(Style.RUNE, 2.4F, 0.0F, a * 0.05F, 7.0F, 0.0F, 1.0F);
         g.quad(foot, new Vector3f(halo, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, halo), VIOLET[0], VIOLET[1], VIOLET[2], body);
         g.end();
         if (!firstPersonSelf) {
            float height = entity.getBbHeight();
            g.begin(Style.RIBBON, 3.0F, 0.3F, 6.0F, 5.0F, 6.0F, 0.6F);
            for (int strand = 0; strand < 2; strand++) {
               int n = 24;
               List<Vector3f> points = new ArrayList<>(n + 1);
               float[] widths = new float[n + 1];
               float[] alphas = new float[n + 1];
               for (int i = 0; i <= n; i++) {
                  float k = (float)i / n;
                  float theta = a * 0.12F + strand * (float)Math.PI + k * FxMath.TAU * 2.0F;
                  float r = 0.75F + 0.15F * (float)Math.sin(k * Math.PI * 3.0 + a * 0.2F);
                  points.add(frame.rel(feet.add(Math.cos(theta) * r, k * height * 1.05, Math.sin(theta) * r)));
                  widths[i] = 0.05F;
                  alphas[i] = body * (float)Math.sin(Math.PI * k);
               }
               float[] tint = strand == 0 ? GOLD : VIOLET;
               g.ribbon(points, widths, alphas, tint[0], tint[1], tint[2]);
            }
            g.begin(Style.GLOW, 0.8F, 0.5F, 2.0F, 1.6F, 0.0F, 0.0F);
            g.billboard(frame.rel(feet.add(0.0, height * 0.58, 0.0)), 1.5F, 0.0F, VIOLET[0], VIOLET[1], VIOLET[2], body * 0.6F);
            g.end();
         }
         if (!this.motes.isEmpty()) {
            g.begin(Style.RIBBON, 3.5F, 0.0F, 6.0F, 0.0F, 0.0F, 0.5F);
            for (Mote mote : this.motes) {
               Vec3 p = mote.position(frame.partialTick);
               g.streak(frame.rel(p), frame.rel(p.subtract(0.0, 0.25, 0.0)), 0.035F, mote.color[0], mote.color[1], mote.color[2], body * mote.life / mote.maxLife);
            }
            g.end();
         }
      }

      // 绝对湮灭结界
      float field = this.domainLevel * fade;
      if (field > 0.01F) {
         Vector3f ground = frame.rel(feet.add(0.0, 0.08, 0.0));
         float r = this.domainRadius;
         g.begin(Style.RUNE, 0.9F, 0.0F, -a * 0.004F, 13.0F, 1.0F, 1.0F);
         g.quad(ground, new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), VIOLET[0], VIOLET[1], VIOLET[2], field * 0.6F);
         g.begin(Style.RING, 1.6F, 0.0F, 0.985F, 0.006F, 96.0F, 0.08F);
         g.quad(new Vector3f(ground).add(0.0F, 0.03F, 0.0F), new Vector3f(r, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, r), GOLD[0], GOLD[1], GOLD[2], field);
         float pulse = (a % 20.0F) / 14.0F;
         if (pulse < 1.0F) {
            float pr = r * FxMath.easeOutCubic(pulse);
            g.begin(Style.RING, 3.0F * (1.0F - pulse), 0.0F, 0.95F, 0.02F, 0.0F, 0.0F);
            g.quad(new Vector3f(ground).add(0.0F, 0.06F, 0.0F), new Vector3f(pr, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, pr), VIOLET[0], VIOLET[1], VIOLET[2], field * (1.0F - pulse));
         }
         g.end();
      }
   }

   private static float approach(float value, float target, float step) {
      return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
   }

   private static final class Mote {
      double x;
      double y;
      double z;
      double py;
      int life;
      final int maxLife;
      final float[] color;

      Mote(Vec3 origin, float[] color, int life) {
         this.x = origin.x;
         this.y = this.py = origin.y;
         this.z = origin.z;
         this.color = color;
         this.life = life;
         this.maxLife = life;
      }

      void tick() {
         this.py = this.y;
         this.y += 0.06;
         this.life--;
      }

      Vec3 position(float partialTick) {
         return new Vec3(this.x, this.py + (this.y - this.py) * partialTick, this.z);
      }
   }
}
