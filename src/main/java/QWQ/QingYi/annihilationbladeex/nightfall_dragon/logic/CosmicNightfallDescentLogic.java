package QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic;

import QWQ.QingYi.annihilationbladeex.common.ServerTickScheduler;
import QWQ.QingYi.annihilationbladeex.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationbladeex.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic.EntropyDissolutionLogic;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic.GammaThunderburstLogic;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.specialeffect.DemonicBloodParasite;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.visual.NightfallDragonFinalVisuals;
import java.util.List;
import mods.flammpfeil.slashblade.RegistryEvents;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CosmicNightfallDescentLogic {
   private static final int METEOR_WAVE_INTERVAL = 2;
   private static final float COLLAPSE_MAX_HEALTH_RATIO = 0.15F;

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

   private static float getMeteorDamage() {
      return ModConfig.COMMON.nightfallDragon.cosmicDescentMeteorDamage.getValue().floatValue();
   }

   private static float getCollapsePanelMultiplier() {
      return ModConfig.COMMON.nightfallDragon.cosmicDescentCollapsePanelMultiplier.getValue().floatValue();
   }

   private CosmicNightfallDescentLogic() {
   }

   public static void prepareCast(Player player) {
      if (player instanceof ServerPlayer serverPlayer && canUseFinalArt(serverPlayer)) {
         ServerLevel level = serverPlayer.serverLevel();
         Vec3 center = serverPlayer.position().add(0.0, serverPlayer.getBbHeight() * 0.5, 0.0);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.65F);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.8F, 0.5F);
         level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 96, 4.5, 2.0, 4.5, 0.2);
         level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 72, 3.5, 1.2, 3.5, 0.1);
      }
   }

   public static void unleash(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer) || !canUseFinalArt(serverPlayer)) {
         return;
      }

      ServerLevel level = serverPlayer.serverLevel();
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
      return NightfallDragonItemSupport.isNightfallDragon(stack) && NightfallDragonDefinitions.FORM_FINAL.equals(NightfallDragonDefinitions.getForm(stack));
   }

   private static Vec3 findFocalPoint(ServerLevel level, ServerPlayer player) {
      Vec3 start = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      double maxDist = getMaxTargetDistance();
      Vec3 end = start.add(look.scale(maxDist));

      List<LivingEntity> targets = SpecialEffectSupport.beamTargets(level, player, start, look, maxDist, 12.0, 32);
      if (!targets.isEmpty()) {
         return SpecialEffectSupport.centerOf(targets.get(0));
      }

      return end;
   }

   private static void pullTargetsToFocalPoint(ServerLevel level, ServerPlayer player, Vec3 focalPoint) {
      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, focalPoint, getVortexRadius());
      for (LivingEntity target : targets) {
         target.invulnerableTime = 0;
         SpecialEffectSupport.pullToward(target, focalPoint, 1.85);
         target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.35, 0.0));
         target.hasImpulse = true;
      }

      NightfallDragonFinalVisuals.spawnBladeOrbitBurst(level, ParticleTypes.REVERSE_PORTAL, focalPoint, 150);
   }

   private static int getSwordsPerWave() {
      return ModConfig.COMMON.nightfallDragon.cosmicDescentSwordsPerWave.getValue();
   }

   private static void spawnMeteorWave(ServerLevel level, ServerPlayer player, Vec3 focalPoint, int waveIndex) {
      int swordsPerWave = getSwordsPerWave();
      for (int s = 0; s < swordsPerWave; s++) {
         double angle = waveIndex * 0.85 + s * (Math.PI * 2.0 / Math.max(1, swordsPerWave)) + player.getRandom().nextDouble() * 0.5;
         double offsetRadius = (waveIndex % 3 == 0 ? 1.5 : 7.5 + (waveIndex % 5) * 3.6);
         Vec3 meteorImpact = focalPoint.add(Math.cos(angle) * offsetRadius, 0.0, Math.sin(angle) * offsetRadius);
         Vec3 meteorOrigin = meteorImpact.add(0.0, 32.0, 0.0);

         EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
         sword.setOwner(player);
         sword.setShooter(player);
         sword.setColor(waveIndex % 2 == 0 ? NightfallDragonDefinitions.FINAL_SUMMONED_SWORD_COLOR : NightfallDragonDefinitions.FINAL_VOID_PURPLE);
         sword.setDamage(0.0);
         sword.setPierce((byte)0);
         sword.setDelay(20);
         sword.setPos(meteorOrigin.x, meteorOrigin.y, meteorOrigin.z);
         sword.moveTo(meteorOrigin.x, meteorOrigin.y, meteorOrigin.z, 0.0F, -90.0F);
         sword.shoot(0.0, -3.5, 0.0, 3.5F, 0.0F);
         level.addFreshEntity(sword);

         level.sendParticles(ParticleTypes.DRAGON_BREATH, meteorImpact.x, meteorImpact.y + 0.5, meteorImpact.z, 36, 2.4, 0.8, 2.4, 0.12);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, meteorImpact.x, meteorImpact.y + 0.5, meteorImpact.z, 28, 1.8, 1.0, 1.8, 0.08);
         level.playSound(null, meteorImpact.x, meteorImpact.y, meteorImpact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.1F);

         AABB area = new AABB(meteorImpact.x - 12.0, meteorImpact.y - 6.0, meteorImpact.z - 12.0, meteorImpact.x + 12.0, meteorImpact.y + 6.0, meteorImpact.z + 12.0);
         List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> SlashBladeTargeting.canAttack(player, target));
         for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().indirectMagic(player, player), getMeteorDamage());
            DemonicBloodParasite.applySummonedSwordMark(player, target);
         }
      }

      NightfallDragonScreenShakeEntity.spawn(level, focalPoint, 48.0F, 0.20F, 2, 10);
   }

   private static void triggerCoreCollapse(ServerLevel level, ServerPlayer player, Vec3 focalPoint) {
      level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 3.0F, 0.5F);
      level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 0.5F);
      NightfallDragonScreenShakeEntity.spawn(level, focalPoint, 96.0F, 0.35F, 5, 30);

      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, focalPoint.x, focalPoint.y + 1.0, focalPoint.z, 12, 4.5, 4.5, 4.5, 0.0);
      level.sendParticles(ParticleTypes.DRAGON_BREATH, focalPoint.x, focalPoint.y + 1.0, focalPoint.z, 256, 13.5, 4.5, 13.5, 0.25);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, focalPoint.x, focalPoint.y + 1.0, focalPoint.z, 192, 12.0, 6.0, 12.0, 0.35);

      for (int i = 0; i < 24; i++) {
         double angle = i * (Math.PI * 2.0 / 24.0);
         double r = 12.0 + (i % 3) * 12.0;
         Vec3 boltPos = focalPoint.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
         GammaThunderburstLogic.spawnBolt(level, boltPos, 0xB026FF);
      }

      float baseDamage = NightfallDragonDefinitions.BASE_ATTACK_DAMAGE * getCollapsePanelMultiplier();
      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, focalPoint, getExplosionRadius());
      for (LivingEntity target : targets) {
         target.invulnerableTime = 0;
         target.setAbsorptionAmount(0.0F);

         float damage = baseDamage + (float)(target.getMaxHealth() * COLLAPSE_MAX_HEALTH_RATIO);
         target.hurt(level.damageSources().indirectMagic(player, player), damage);

         if (target.isAlive() && target.getHealth() <= target.getMaxHealth() * 0.5F) {
            NightfallDragonFinalVisuals.spawnExecutionBurst(level, target);
            EntropyDissolutionLogic.executeFinal(target, player);
         }
      }
   }
}
