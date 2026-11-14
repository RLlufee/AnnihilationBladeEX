package qwq.qingyi.annihilationblade.nightfall_dragon.visual;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import qwq.qingyi.annihilationblade.common.SpecialEffectSupport;

import java.util.HashMap;
import java.util.Map;

/**
 * 夜临玄龙终焉形态粒子与音效控制器（带单维度单 tick 粒子预算上限，防止高频招式过载）。
 */
public final class NightfallDragonFinalVisuals {
    private static final int MAX_PARTICLES_PER_LEVEL_TICK = 500;
    private static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Map<ResourceKey<Level>, ParticleBudget> PARTICLE_BUDGETS = new HashMap<>();

    private NightfallDragonFinalVisuals() {
    }

    private static ParticleOptions dragonBreath() {
        return PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
    }

    private static ParticleOptions flashWhite() {
        return ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);
    }

    public static void spawnGodBodyAura(ServerLevel level, Player player) {
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.58D, 0.0D);
        int age = player.tickCount;
        if (age % 20 == 0) {
            sendParticles(level, dragonBreath(), center.x, center.y, center.z, 10, 0.85D, 0.65D, 0.85D, 0.025D);
            sendParticles(level, ParticleTypes.END_ROD, center.x, center.y + 0.18D, center.z, 5, 0.55D, 0.45D, 0.55D, 0.012D);
            spawnFootHalo(level, player.position(), age);
        }
    }

    public static void spawnDomainFrame(ServerLevel level, Vec3 center, int age) {
        double radius = 64.0D;
        double y = center.y + 0.15D + Math.sin(age * 0.18D) * 0.25D;
        BlockParticleOption obsidian = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2.0D * i / 48.0D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            spawnParticle(level, obsidian, x, y, z, 1, 0.04D);
            if (i % 3 == 0) {
                spawnParticle(level, ParticleTypes.REVERSE_PORTAL, x, y + 0.2D, z, 1, 0.12D);
            }
        }

        sendParticles(level, ParticleTypes.SQUID_INK, center.x, center.y + 0.8D, center.z, 60, 12.0D, 4.0D, 12.0D, 0.08D);
        sendParticles(level, dragonBreath(), center.x, center.y + 0.8D, center.z, 60, 18.0D, 5.0D, 18.0D, 0.12D);
        if (age % 40 == 0) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.6F, 0.45F);
        }
    }

    public static void spawnPiercingBladeWave(ServerLevel level, Player player, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        Vec3 forward = safe(delta, player.getLookAngle());
        Vec3 right = SpecialEffectSupport.rightOf(forward);
        double length = Math.max(1.0D, delta.length());
        Vec3 center = start.add(delta.scale(0.5D));
        int samples = Math.min(48, Math.max(18, (int) (length * 0.625D)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.08F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.1F, 0.62F);

        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            Vec3 base = start.lerp(end, t);
            double width = 0.45D + t * 5.2D;
            double height = 0.25D + Math.sin(t * Math.PI) * 1.35D;
            double ripple = Math.sin(i * 0.62D) * (0.18D + t * 0.9D);
            Vec3 spine = base.add(UP.scale(height * 0.18D));
            sendParticles(level, ParticleTypes.END_ROD, spine.x, spine.y, spine.z, 1, 0.025D, 0.025D, 0.025D, 0.012D);
            sendParticles(level, ParticleTypes.REVERSE_PORTAL, spine.x, spine.y, spine.z, 2, 0.08D, 0.08D, 0.08D, 0.08D);

            Vec3 leftEdge = base.subtract(right.scale(width + ripple)).add(UP.scale(height));
            Vec3 rightEdge = base.add(right.scale(width - ripple)).add(UP.scale(height));
            sendParticles(level, ParticleTypes.SWEEP_ATTACK, leftEdge.x, leftEdge.y, leftEdge.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sendParticles(level, ParticleTypes.SWEEP_ATTACK, rightEdge.x, rightEdge.y, rightEdge.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

            if (i % 3 == 0) {
                spawnLine(level, leftEdge, rightEdge, dragonBreath(), 3);
            }

            if (i % 8 == 0) {
                Vec3 lowerLeft = base.subtract(right.scale(width * 0.55D)).add(UP.scale(-0.18D));
                Vec3 lowerRight = base.add(right.scale(width * 0.55D)).add(UP.scale(-0.18D));
                spawnLine(level, lowerLeft, leftEdge, ParticleTypes.SQUID_INK, 2);
                spawnLine(level, lowerRight, rightEdge, ParticleTypes.SQUID_INK, 2);
                sendParticles(level, ParticleTypes.SONIC_BOOM, spine.x, spine.y, spine.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        sendParticles(level, flashWhite(), center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static void spawnBladeOrbitBurst(ServerLevel level, ParticleOptions particle, Vec3 center, int count) {
        sendParticles(level, particle, center.x, center.y, center.z, Math.max(1, count / 2), 0.16D, 0.16D, 0.16D, 0.04D);
    }

    public static void spawnExecutionBurst(ServerLevel level, LivingEntity target) {
        Vec3 center = SpecialEffectSupport.centerOf(target);
        sendParticles(level, flashWhite(), center.x, center.y, center.z, 2, 0.0D, 0.0D, 0.0D, 0.0D);
        sendParticles(level, dragonBreath(), center.x, center.y, center.z, 60, 1.4D, 1.0D, 1.4D, 0.16D);
        sendParticles(level, ParticleTypes.END_ROD, center.x, center.y, center.z, 40, 1.0D, 0.8D, 1.0D, 0.1D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.2F, 0.75F);
    }

    private static void spawnFootHalo(ServerLevel level, Vec3 footPos, int age) {
        double radius = 1.85D + Math.sin(age * 0.15D) * 0.15D;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D + age * 0.03D;
            double x = footPos.x + Math.cos(angle) * radius;
            double z = footPos.z + Math.sin(angle) * radius;
            sendParticles(level, dragonBreath(), x, footPos.y + 0.05D, z, 1, 0.01D, 0.01D, 0.01D, 0.005D);
        }
    }

    private static void spawnLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int samples) {
        for (int i = 0; i <= samples; i++) {
            Vec3 pos = start.lerp(end, i / (double) samples);
            sendParticles(level, particle, pos.x, pos.y, pos.z, 1, 0.02D, 0.02D, 0.02D, 0.01D);
        }
    }

    private static void spawnParticle(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double speed) {
        sendParticles(level, particle, x, y, z, count, 0.04D, 0.04D, 0.04D, speed);
    }

    private static void sendParticles(
            ServerLevel level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double xDist,
            double yDist,
            double zDist,
            double speed
    ) {
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
        return vector.lengthSqr() < 1.0E-6D ? fallback.normalize() : vector.normalize();
    }

    private static final class ParticleBudget {
        private long tick = Long.MIN_VALUE;
        private int used;
    }
}
