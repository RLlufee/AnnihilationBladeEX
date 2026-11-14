package qwq.qingyi.annihilationblade.nightfall_dragon.logic;

import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.init.SBEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qwq.qingyi.annihilationblade.common.ServerTickScheduler;
import qwq.qingyi.annihilationblade.common.SlashBladeTargeting;
import qwq.qingyi.annihilationblade.common.SpecialEffectSupport;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.EntropyDissolutionLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.GammaThunderburstLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import qwq.qingyi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import qwq.qingyi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.DemonicBloodParasite;
import qwq.qingyi.annihilationblade.nightfall_dragon.visual.NightfallDragonFinalVisuals;

import java.util.List;

/**
 * 永夜神降 SA 逻辑（终焉形态）：牵引目标至焦点后降下多波星陨剑雨并引爆终焉坍缩核心。
 */
public final class CosmicNightfallDescentLogic {
    private static final int METEOR_WAVE_INTERVAL = 2;
    private static final float COLLAPSE_MAX_HEALTH_RATIO = 0.15F;

    private CosmicNightfallDescentLogic() {
    }

    private static double getMaxTargetDistance() {
        return ModConfig.COMMON.nightfallDragon.cosmicDescentMaxDistance.getValue();
    }

    private static double getVortexRadius() {
        return ModConfig.COMMON.nightfallDragon.cosmicDescentVortexRadius.getValue();
    }

    private static double getExplosionRadius() {
        return ModConfig.COMMON.nightfallDragon.cosmicDescentExplosionRadius.getValue();
    }

    private static int getMeteorWaves() {
        return ModConfig.COMMON.nightfallDragon.cosmicDescentMeteorWaves.getValue();
    }

    private static int getSwordsPerWave() {
        return ModConfig.COMMON.nightfallDragon.cosmicDescentSwordsPerWave.getValue();
    }

    private static float getMeteorDamage() {
        return (float) ModConfig.COMMON.nightfallDragon.cosmicDescentMeteorDamage.getValue();
    }

    private static float getCollapsePanelMultiplier() {
        return (float) ModConfig.COMMON.nightfallDragon.cosmicDescentCollapsePanelMultiplier.getValue();
    }

    public static void prepareCast(Player player) {
        if (player instanceof ServerPlayer serverPlayer && canUseFinalArt(serverPlayer)) {
            ServerLevel level = (ServerLevel) serverPlayer.level();
            Vec3 center = serverPlayer.position().add(0.0D, serverPlayer.getBbHeight() * 0.5D, 0.0D);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.65F);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.8F, 0.5F);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 96, 4.5D, 2.0D, 4.5D, 0.2D);
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), center.x, center.y, center.z, 72, 3.5D, 1.2D, 3.5D, 0.1D);
        }
    }

    public static void unleash(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !canUseFinalArt(serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();
        Vec3 focalPoint = findFocalPoint(level, serverPlayer);

        level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.5F, 0.5F);
        level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2.2F, 0.6F);

        pullTargetsToFocalPoint(level, serverPlayer, focalPoint);

        int waves = getMeteorWaves();
        for (int wave = 0; wave < waves; wave++) {
            int waveIndex = wave;
            ServerTickScheduler.schedule(6 + wave * METEOR_WAVE_INTERVAL, () -> {
                if (serverPlayer.isAlive()) {
                    spawnMeteorWave(level, serverPlayer, focalPoint, waveIndex);
                }
            });
        }

        ServerTickScheduler.schedule(38, () -> {
            if (serverPlayer.isAlive()) {
                triggerCoreCollapse(level, serverPlayer, focalPoint);
            }
        });
    }

    private static boolean canUseFinalArt(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return NightfallDragonItemSupport.isNightfallDragon(stack)
                && NightfallDragonDefinitions.FORM_FINAL.equals(NightfallDragonDefinitions.getForm(stack));
    }

    private static Vec3 findFocalPoint(ServerLevel level, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double maxDist = getMaxTargetDistance();
        Vec3 end = start.add(look.scale(maxDist));

        List<LivingEntity> targets = SpecialEffectSupport.beamTargets(level, player, start, look, maxDist, 12.0D, 32);
        if (!targets.isEmpty()) {
            return SpecialEffectSupport.centerOf(targets.get(0));
        }

        return end;
    }

    private static void pullTargetsToFocalPoint(ServerLevel level, ServerPlayer player, Vec3 focalPoint) {
        List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, focalPoint, getVortexRadius());
        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            SpecialEffectSupport.pullToward(target, focalPoint, 1.85D);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.35D, 0.0D));
            target.needsSync = true;
        }

        NightfallDragonFinalVisuals.spawnBladeOrbitBurst(level, ParticleTypes.REVERSE_PORTAL, focalPoint, 150);
    }

    private static void spawnMeteorWave(ServerLevel level, ServerPlayer player, Vec3 focalPoint, int waveIndex) {
        int swordsPerWave = getSwordsPerWave();
        for (int s = 0; s < swordsPerWave; s++) {
            double angle = waveIndex * 0.85D + s * (Math.PI * 2.0D / Math.max(1, swordsPerWave)) + player.getRandom().nextDouble() * 0.5D;
            double offsetRadius = waveIndex % 3 == 0 ? 1.5D : 7.5D + (waveIndex % 5) * 3.6D;
            Vec3 meteorImpact = focalPoint.add(Math.cos(angle) * offsetRadius, 0.0D, Math.sin(angle) * offsetRadius);
            Vec3 meteorOrigin = meteorImpact.add(0.0D, 32.0D, 0.0D);

            EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(SBEntityTypes.SUMMONED_SWORD, level);
            sword.setOwner(player);
            sword.setShooter(player);
            sword.setColor(waveIndex % 2 == 0 ? NightfallDragonDefinitions.FINAL_SUMMONED_SWORD_COLOR : NightfallDragonDefinitions.FINAL_VOID_PURPLE);
            sword.setDamage(0.0D);
            sword.setPierce((byte) 0);
            sword.setDelay(20);
            sword.setPos(meteorOrigin.x, meteorOrigin.y, meteorOrigin.z);
            sword.snapTo(meteorOrigin.x, meteorOrigin.y, meteorOrigin.z, 0.0F, -90.0F);
            sword.shoot(0.0D, -3.5D, 0.0D, 3.5F, 0.0F);
            level.addFreshEntity(sword);

            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), meteorImpact.x, meteorImpact.y + 0.5D, meteorImpact.z, 36, 2.4D, 0.8D, 2.4D, 0.12D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, meteorImpact.x, meteorImpact.y + 0.5D, meteorImpact.z, 28, 1.8D, 1.0D, 1.8D, 0.08D);
            level.playSound(null, meteorImpact.x, meteorImpact.y, meteorImpact.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.1F);

            AABB area = new AABB(meteorImpact.x - 12.0D, meteorImpact.y - 6.0D, meteorImpact.z - 12.0D, meteorImpact.x + 12.0D, meteorImpact.y + 6.0D, meteorImpact.z + 12.0D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> SlashBladeTargeting.canAttack(player, target));
            for (LivingEntity target : targets) {
                target.invulnerableTime = 0;
                target.hurtServer(level, level.damageSources().indirectMagic(player, player), getMeteorDamage());
                DemonicBloodParasite.applySummonedSwordMark(player, target);
            }
        }

        NightfallDragonScreenShakeEntity.spawn(level, focalPoint, 48.0F, 0.20F, 2, 10);
    }

    private static void triggerCoreCollapse(ServerLevel level, ServerPlayer player, Vec3 focalPoint) {
        level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.5F);
        NightfallDragonScreenShakeEntity.spawn(level, focalPoint, 96.0F, 0.35F, 5, 30);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, focalPoint.x, focalPoint.y + 1.0D, focalPoint.z, 12, 4.5D, 4.5D, 4.5D, 0.0D);
        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), focalPoint.x, focalPoint.y + 1.0D, focalPoint.z, 256, 13.5D, 4.5D, 13.5D, 0.25D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, focalPoint.x, focalPoint.y + 1.0D, focalPoint.z, 192, 12.0D, 6.0D, 12.0D, 0.35D);

        for (int i = 0; i < 24; i++) {
            double angle = i * (Math.PI * 2.0D / 24.0D);
            double r = 12.0D + (i % 3) * 12.0D;
            Vec3 boltPos = focalPoint.add(Math.cos(angle) * r, 0.0D, Math.sin(angle) * r);
            GammaThunderburstLogic.spawnBolt(level, boltPos, 0xB026FF);
        }

        float baseDamage = NightfallDragonDefinitions.BASE_ATTACK_DAMAGE * getCollapsePanelMultiplier();
        List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, focalPoint, getExplosionRadius());
        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            target.setAbsorptionAmount(0.0F);

            float damage = baseDamage + (float) (target.getMaxHealth() * COLLAPSE_MAX_HEALTH_RATIO);
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), damage);

            if (target.isAlive() && target.getHealth() <= target.getMaxHealth() * 0.5F) {
                NightfallDragonFinalVisuals.spawnExecutionBurst(level, target);
                EntropyDissolutionLogic.executeFinal(target, player);
            }
        }
    }
}
