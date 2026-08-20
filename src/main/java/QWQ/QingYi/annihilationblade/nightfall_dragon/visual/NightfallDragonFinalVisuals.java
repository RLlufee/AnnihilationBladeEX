package QWQ.QingYi.annihilationblade.nightfall_dragon.visual;

import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class NightfallDragonFinalVisuals {
   private static final int MAX_PARTICLES_PER_LEVEL_TICK = 500;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Map<ResourceKey<Level>, ParticleBudget> PARTICLE_BUDGETS = new HashMap<>();

   private NightfallDragonFinalVisuals() {
   }

   public static void spawnGodBodyAura(ServerLevel level, Player player) {
      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.58, 0.0);
      int age = player.tickCount;
      if (age % 20 == 0) {
         sendParticles(level, ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 10, 0.85, 0.65, 0.85, 0.025);
         sendParticles(level, ParticleTypes.END_ROD, center.x, center.y + 0.18, center.z, 5, 0.55, 0.45, 0.55, 0.012);
         spawnFootHalo(level, player.position(), age);
      }
   }

   public static void spawnDomainFrame(ServerLevel level, Vec3 center, int age) {
      double radius = 64.0;
      double y = center.y + 0.15 + Math.sin(age * 0.18) * 0.25;
      BlockParticleOption obsidian = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState());
      for (int i = 0; i < 48; i++) {
         double angle = Math.PI * 2.0 * i / 48.0;
         double x = center.x + Math.cos(angle) * radius;
         double z = center.z + Math.sin(angle) * radius;
         spawnParticle(level, obsidian, x, y, z, 1, 0.04);
         if (i % 3 == 0) {
            spawnParticle(level, ParticleTypes.REVERSE_PORTAL, x, y + 0.2, z, 1, 0.12);
         }
      }

      sendParticles(level, ParticleTypes.SQUID_INK, center.x, center.y + 0.8, center.z, 60, 12.0, 4.0, 12.0, 0.08);
      sendParticles(level, ParticleTypes.DRAGON_BREATH, center.x, center.y + 0.8, center.z, 60, 18.0, 5.0, 18.0, 0.12);
      if (age % 40 == 0) {
         level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.6F, 0.45F);
      }
   }

   public static void spawnPiercingBladeWave(ServerLevel level, Player player, Vec3 start, Vec3 end) {
      Vec3 delta = end.subtract(start);
      Vec3 forward = safe(delta, player.getLookAngle());
      Vec3 right = SpecialEffectSupport.rightOf(forward);
      double length = Math.max(1.0, delta.length());
      Vec3 center = start.add(delta.scale(0.5));
      int samples = Math.min(48, Math.max(18, (int)(length * 0.625)));
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.08F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.1F, 0.62F);

      for (int i = 0; i <= samples; i++) {
         double t = i / (double)samples;
         Vec3 base = start.lerp(end, t);
         double width = 0.45 + t * 5.2;
         double height = 0.25 + Math.sin(t * Math.PI) * 1.35;
         double ripple = Math.sin(i * 0.62) * (0.18 + t * 0.9);
         Vec3 spine = base.add(UP.scale(height * 0.18));
         sendParticles(level, ParticleTypes.END_ROD, spine.x, spine.y, spine.z, 1, 0.025, 0.025, 0.025, 0.012);
         sendParticles(level, ParticleTypes.REVERSE_PORTAL, spine.x, spine.y, spine.z, 2, 0.08, 0.08, 0.08, 0.08);

         Vec3 leftEdge = base.subtract(right.scale(width + ripple)).add(UP.scale(height));
         Vec3 rightEdge = base.add(right.scale(width - ripple)).add(UP.scale(height));
         sendParticles(level, ParticleTypes.SWEEP_ATTACK, leftEdge.x, leftEdge.y, leftEdge.z, 1, 0.0, 0.0, 0.0, 0.0);
         sendParticles(level, ParticleTypes.SWEEP_ATTACK, rightEdge.x, rightEdge.y, rightEdge.z, 1, 0.0, 0.0, 0.0, 0.0);

         if (i % 3 == 0) {
            spawnLine(level, leftEdge, rightEdge, ParticleTypes.DRAGON_BREATH, 3);
         }

         if (i % 8 == 0) {
            Vec3 lowerLeft = base.subtract(right.scale(width * 0.55)).add(UP.scale(-0.18));
            Vec3 lowerRight = base.add(right.scale(width * 0.55)).add(UP.scale(-0.18));
            spawnLine(level, lowerLeft, leftEdge, ParticleTypes.SQUID_INK, 2);
            spawnLine(level, lowerRight, rightEdge, ParticleTypes.SQUID_INK, 2);
            sendParticles(level, ParticleTypes.SONIC_BOOM, spine.x, spine.y, spine.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }

      sendParticles(level, ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   public static void spawnBladeOrbitBurst(ServerLevel level, ParticleOptions particle, Vec3 center, int count) {
      sendParticles(level, particle, center.x, center.y, center.z, Math.max(1, count / 2), 0.16, 0.16, 0.16, 0.04);
   }

   public static void spawnExecutionBurst(ServerLevel level, LivingEntity target) {
      Vec3 center = SpecialEffectSupport.centerOf(target);
      sendParticles(level, ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
      sendParticles(level, ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 60, 1.4, 1.0, 1.4, 0.16);
      sendParticles(level, ParticleTypes.END_ROD, center.x, center.y, center.z, 40, 1.0, 0.8, 1.0, 0.1);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.2F, 0.75F);
   }

   private static void spawnFootHalo(ServerLevel level, Vec3 footPos, int age) {
      double radius = 1.85 + Math.sin(age * 0.15) * 0.15;
      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * 2.0 * i / 8.0 + age * 0.03;
         double x = footPos.x + Math.cos(angle) * radius;
         double z = footPos.z + Math.sin(angle) * radius;
         sendParticles(level, ParticleTypes.DRAGON_BREATH, x, footPos.y + 0.05, z, 1, 0.01, 0.01, 0.01, 0.005);
      }
   }

   private static void spawnLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int samples) {
      for (int i = 0; i <= samples; i++) {
         Vec3 pos = start.lerp(end, i / (double)samples);
         sendParticles(level, particle, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.01);
      }
   }

   private static void spawnParticle(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double speed) {
      sendParticles(level, particle, x, y, z, count, 0.04, 0.04, 0.04, speed);
   }

   private static void sendParticles(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double xDist, double yDist, double zDist, double speed) {
      int allowed = reserveParticles(level, count);
      if (allowed > 0) {
         level.sendParticles(particle, x, y, z, allowed, xDist, yDist, zDist, speed);
      }
   }

   private static int reserveParticles(ServerLevel level, int requested) {
      if (requested <= 0) {
         return 0;
      }

      ResourceKey<Level> key = level.dimension();
      long gameTime = level.getGameTime();
      ParticleBudget budget = PARTICLE_BUDGETS.computeIfAbsent(key, ignored -> new ParticleBudget());
      if (budget.tick != gameTime) {
         budget.tick = gameTime;
         budget.used = 0;
      }

      int remaining = MAX_PARTICLES_PER_LEVEL_TICK - budget.used;
      if (remaining <= 0) {
         return 0;
      }

      int allowed = Math.min(requested, remaining);
      budget.used += allowed;
      return allowed;
   }

   private static Vec3 safe(Vec3 vector, Vec3 fallback) {
      return vector.lengthSqr() < 1.0E-6 ? fallback.normalize() : vector.normalize();
   }

   private static final class ParticleBudget {
      private long tick = Long.MIN_VALUE;
      private int used;
   }
}
