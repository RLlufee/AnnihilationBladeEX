package QWQ.QingYi.annihilationblade.annihilation_blade.visual;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * 旧版服务端粒子视觉（逐点 sendParticles）。
 * <p>
 * 湮灭之刃的 SE 已全部迁移到专属渲染器（见 {@link AnnihilationFxEvents}）。这里只保留仍被魔龙夜陨
 * {@code DemonicBloodParasite} 调用的两个方法，待迁移魔龙夜陨 SE 时一并移除。
 */
public final class AnnihilationVisuals {
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 X_AXIS = new Vec3(1.0, 0.0, 0.0);
   private static final Vec3 Z_AXIS = new Vec3(0.0, 0.0, 1.0);
   private static final ParticleOptions VOID_PURPLE = ParticleTypes.ENCHANT;
   private static final ParticleOptions COSMIC_CYAN = ParticleTypes.ENCHANT;
   private static final ParticleOptions TERMINUS_GOLD = ParticleTypes.ENCHANT;

   private AnnihilationVisuals() {
   }

   public static void spawnSlashBridge(ServerLevel level, Vec3 start, Vec3 end, double width, RandomSource random) {
      Vec3 direction = safeNormalize(end.subtract(start), Z_AXIS);
      Vec3 right = safeNormalize(direction.cross(UP), X_AXIS);
      spawnLine(level, start, end, VOID_PURPLE, 16, 0.018);
      spawnLine(level, start.add(right.scale(width)), end.add(right.scale(-width)), COSMIC_CYAN, 12, 0.018);
      spawnLine(level, start.add(right.scale(-width)), end.add(right.scale(width)), TERMINUS_GOLD, 12, 0.018);

      for (int i = 0; i < 4; i++) {
         Vec3 mid = start.lerp(end, random.nextDouble());
         Vec3 slash = right.scale(width * (0.5 + random.nextDouble() * 0.9));
         spawnLine(level, mid.subtract(slash), mid.add(slash), ParticleTypes.ELECTRIC_SPARK, 6, 0.012);
      }
   }

   public static void spawnBloodPrisonBurst(ServerLevel level, Vec3 center, double radius, RandomSource random) {
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIMSON_SPORE, center.x, center.y, center.z, 72, radius * 0.42, radius * 0.34, radius * 0.42, 0.08);
      level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y, center.z, 26, radius * 0.32, radius * 0.26, radius * 0.32, 0.06);
      level.sendParticles(ParticleTypes.DRIPPING_LAVA, center.x, center.y + 0.15, center.z, 18, radius * 0.24, radius * 0.18, radius * 0.24, 0.02);
      spawnRing(level, center, X_AXIS, Z_AXIS, radius * 1.28, ParticleTypes.DRAGON_BREATH, 54, 0.006);
      spawnRing(level, center.add(0.0, 0.18, 0.0), X_AXIS, UP, radius, ParticleTypes.CRIMSON_SPORE, 42, 0.008);
      spawnRing(level, center.add(0.0, 0.18, 0.0), Z_AXIS, UP, radius, ParticleTypes.ELECTRIC_SPARK, 34, 0.01);

      for (int i = 0; i < 8; i++) {
         Vec3 dir = randomUnit(random);
         spawnLine(
            level,
            center,
            center.add(dir.scale(radius * (1.0 + random.nextDouble() * 0.85))),
            i % 2 == 0 ? ParticleTypes.DRAGON_BREATH : ParticleTypes.ELECTRIC_SPARK,
            7,
            0.018
         );
      }
   }

   private static void spawnRing(ServerLevel level, Vec3 center, Vec3 axisA, Vec3 axisB, double radius, ParticleOptions particle, int points, double jitter) {
      Vec3 a = safeNormalize(axisA, X_AXIS);
      Vec3 b = safeNormalize(axisB, Z_AXIS);

      for (int i = 0; i < points; i++) {
         double angle = (Math.PI * 2) * i / points;
         Vec3 pos = center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, jitter, jitter, jitter, 0.0);
      }
   }

   private static void spawnLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points, double jitter) {
      for (int i = 0; i <= points; i++) {
         Vec3 pos = start.lerp(end, (double)i / points);
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, jitter, jitter, jitter, 0.0);
      }
   }

   private static Vec3 randomUnit(RandomSource random) {
      double yaw = random.nextDouble() * (Math.PI * 2);
      double pitch = (random.nextDouble() - 0.5) * Math.PI;
      double horizontal = Math.cos(pitch);
      return new Vec3(Math.cos(yaw) * horizontal, Math.sin(pitch), Math.sin(yaw) * horizontal).normalize();
   }

   private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
      return vector.lengthSqr() < 1.0E-6 ? fallback : vector.normalize();
   }
}
