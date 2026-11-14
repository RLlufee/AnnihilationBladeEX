package qwq.qingyi.annihilationblade.infinity_stellaris.logic;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qwq.qingyi.annihilationblade.common.ServerTickScheduler;
import qwq.qingyi.annihilationblade.common.SlashBladeTargeting;
import qwq.qingyi.annihilationblade.registry.ModSpecialEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 宇宙弦切逻辑：挥刀瞬间触发伽马雷暴并展开立体弦网格延迟终结大范围目标。
 */
public final class CosmicStringCutLogic {
    private static final double STRING_CUBE_SIZE = 128.0;
    private static final double LOCAL_LATTICE_HALF_SIZE = 2.0;
    private static final ParticleOptions FLASH_PARTICLE = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    private CosmicStringCutLogic() {
    }

    public static void clearPlayer(UUID playerId) {
        LAST_TRIGGER_TICK.remove(playerId);
    }

    public static void onDoingSlash(DoSlashEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        ISlashBladeState state = event.getSlashBladeState();
        if (!state.hasSpecialEffect(ModSpecialEffects.COSMIC_STRING_CUT_ID)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        UUID playerId = player.getUUID();
        if (LAST_TRIGGER_TICK.getOrDefault(playerId, Long.MIN_VALUE) == gameTime) {
            return;
        }
        LAST_TRIGGER_TICK.put(playerId, gameTime);

        GammaThunderburstLogic.trigger(player);

        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        spawnLocalStringLattice(level, center);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 6.0F, 0.35F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 6.0F, 1.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0F, 0.7F);

        AABB hitBox = AABB.ofSize(center, STRING_CUBE_SIZE, STRING_CUBE_SIZE, STRING_CUBE_SIZE);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox, entity -> SlashBladeTargeting.canAttack(player, entity));
        for (LivingEntity target : targets) {
            ServerTickScheduler.schedule(1, () -> {
                if (target.isAlive() && SlashBladeTargeting.canAttack(player, target)) {
                    EntropyDissolutionLogic.executeFinal(target, player);
                }
            });
        }
    }

    private static void spawnLocalStringLattice(ServerLevel level, Vec3 center) {
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                double da = a;
                double db = b;
                spawnLine(
                        level,
                        center.add(-LOCAL_LATTICE_HALF_SIZE, da, db),
                        center.add(LOCAL_LATTICE_HALF_SIZE, da, db),
                        ParticleTypes.END_ROD,
                        24,
                        0.002
                );
                spawnLine(
                        level,
                        center.add(da, -LOCAL_LATTICE_HALF_SIZE, db),
                        center.add(da, LOCAL_LATTICE_HALF_SIZE, db),
                        ParticleTypes.ELECTRIC_SPARK,
                        24,
                        0.004
                );
                spawnLine(
                        level,
                        center.add(da, db, -LOCAL_LATTICE_HALF_SIZE),
                        center.add(da, db, LOCAL_LATTICE_HALF_SIZE),
                        ParticleTypes.REVERSE_PORTAL,
                        24,
                        0.006
                );
            }
        }

        level.sendParticles(FLASH_PARTICLE, center.x, center.y, center.z, 4, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
    }

    private static void spawnLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points, double jitter) {
        if (points <= 0) {
            return;
        }

        for (int i = 0; i <= points; i++) {
            Vec3 pos = start.lerp(end, (double) i / points);
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, jitter, jitter, jitter, 0.0);
        }
    }
}
