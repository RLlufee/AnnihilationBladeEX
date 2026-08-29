package QWQ.QingYi.annihilationblade.nightfall_dragon.logic;

import QWQ.QingYi.annihilationblade.common.ServerTickScheduler;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.EntropyDissolutionLogic;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.GammaThunderburstLogic;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.DemonicBloodParasite;
import QWQ.QingYi.annihilationblade.nightfall_dragon.visual.NightfallDragonFinalVisuals;
import java.util.List;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
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

/**
 * 魔龙夜陨 - 终极 SA【神陨·宇宙夜陨降临】 (Cosmic Nightfall Descent) 逻辑类
 * 
 * 包含三个阶段的天灾级演出与规则：
 * 
 *   一阶段：引力撕裂与暗星聚能 (0 ~ 10 Ticks)：寻找视线前方最远 40 格焦点，拉扯 24 格内敌对实体至焦点并浮空。
 *   二阶段：夜陨星雨轰炸 (10 ~ 40 Ticks)：在焦点高空 15 格处打开多维裂隙，节流下落 15 轮流星/龙刃暴击与地爆。
 *   三阶段：暗星坍缩与法则湮灭 (40 ~ 50 Ticks)：暗星内核剧烈坍缩，引发大范围终极湮灭冲击波、剥离护盾并斩杀半血目标。
 * 
 */
import QWQ.QingYi.annihilationblade.config.ModConfig;

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
         level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F,
               0.65F);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
               1.8F, 0.5F);
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

      // 阶段一：引力漩涡拉扯 & 视听提示
      level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS,
            2.5F, 0.5F);
      level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.AMETHYST_BLOCK_RESONATE,
            SoundSource.PLAYERS, 2.2F, 0.6F);

      // 1. 抓取周围目标拉向焦点
      pullTargetsToFocalPoint(level, serverPlayer, focalPoint);

      // 2. 调度阶段二：夜陨星雨下落 (从第 6 Tick 开始，共 getMeteorWaves() 轮，每 2 Ticks 降落一波)
      int waves = getMeteorWaves();
      for (int wave = 0; wave < waves; wave++) {
         int waveIndex = wave;
         ServerTickScheduler.schedule(6 + wave * METEOR_WAVE_INTERVAL, () -> {
            if (serverPlayer.isAlive()) {
               spawnMeteorWave(level, serverPlayer, focalPoint, waveIndex);
            }
         });
      }

      // 3. 调度阶段三：暗星坍缩与法则湮灭 (在第 38 Tick 时)
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
         double angle = waveIndex * 0.85 + s * (Math.PI * 2.0 / Math.max(1, swordsPerWave))
               + player.getRandom().nextDouble() * 0.5;
         double offsetRadius = (waveIndex % 3 == 0 ? 1.5 : 7.5 + (waveIndex % 5) * 3.6);
         Vec3 meteorImpact = focalPoint.add(Math.cos(angle) * offsetRadius, 0.0, Math.sin(angle) * offsetRadius);
         Vec3 meteorOrigin = meteorImpact.add(0.0, 32.0, 0.0);

         // 下砸降落幻影剑
         EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
         sword.setOwner(player);
         sword.setShooter(player);
         sword.setColor(waveIndex % 2 == 0 ? NightfallDragonDefinitions.FINAL_SUMMONED_SWORD_COLOR
               : NightfallDragonDefinitions.FINAL_VOID_PURPLE);
         sword.setDamage(0.0);
         sword.setPierce((byte) 0);
         sword.setDelay(20);
         sword.setPos(meteorOrigin.x, meteorOrigin.y, meteorOrigin.z);
         sword.moveTo(meteorOrigin.x, meteorOrigin.y, meteorOrigin.z, 0.0F, -90.0F);
         sword.shoot(0.0, -3.5, 0.0, 3.5F, 0.0F);
         level.addFreshEntity(sword);

         // 特效与冲击
         level.sendParticles(ParticleTypes.DRAGON_BREATH, meteorImpact.x, meteorImpact.y + 0.5, meteorImpact.z, 36, 2.4,
               0.8, 2.4, 0.12);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, meteorImpact.x, meteorImpact.y + 0.5, meteorImpact.z, 28,
               1.8, 1.0, 1.8, 0.08);
         level.playSound(null, meteorImpact.x, meteorImpact.y, meteorImpact.z, SoundEvents.GENERIC_EXPLODE,
               SoundSource.PLAYERS, 1.0F, 1.1F);

         AABB area = new AABB(meteorImpact.x - 12.0, meteorImpact.y - 6.0, meteorImpact.z - 12.0, meteorImpact.x + 12.0,
               meteorImpact.y + 6.0, meteorImpact.z + 12.0);
         List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
               target -> SlashBladeTargeting.canAttack(player, target));
         for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().indirectMagic(player, player), getMeteorDamage());
            DemonicBloodParasite.applySummonedSwordMark(player, target);
         }
      }

      // 每一波星雨仅在中心焦点处触发一次屏幕震动
      NightfallDragonScreenShakeEntity.spawn(level, focalPoint, 48.0F, 0.20F, 2, 10);
   }

   private static void triggerCoreCollapse(ServerLevel level, ServerPlayer player, Vec3 focalPoint) {
      // 1. 视听震场
      level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(),
            SoundSource.PLAYERS, 3.0F, 0.5F);
      level.playSound(null, focalPoint.x, focalPoint.y, focalPoint.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
            3.0F, 0.5F);
      NightfallDragonScreenShakeEntity.spawn(level, focalPoint, 96.0F, 0.35F, 5, 30);

      // 2. 特效粒子爆发
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, focalPoint.x, focalPoint.y + 1.0, focalPoint.z, 12, 4.5, 4.5,
            4.5, 0.0);
      level.sendParticles(ParticleTypes.DRAGON_BREATH, focalPoint.x, focalPoint.y + 1.0, focalPoint.z, 256, 13.5, 4.5,
            13.5, 0.25);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, focalPoint.x, focalPoint.y + 1.0, focalPoint.z, 192, 12.0, 6.0,
            12.0, 0.35);

      // 3. 产生 24 道落雷在 3 重圆环上轰击 (12, 24, 36 格半径)
      for (int i = 0; i < 24; i++) {
         double angle = i * (Math.PI * 2.0 / 24.0);
         double r = 12.0 + (i % 3) * 12.0;
         Vec3 boltPos = focalPoint.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
         GammaThunderburstLogic.spawnBolt(level, boltPos, 0xB026FF);
      }

      // 4. 结算大范围破盾与爆伤
      float baseDamage = NightfallDragonDefinitions.BASE_ATTACK_DAMAGE * getCollapsePanelMultiplier();
      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, focalPoint, getExplosionRadius());
      for (LivingEntity target : targets) {
         target.invulnerableTime = 0;
         target.setAbsorptionAmount(0.0F); // 剥离黄血盾

         float damage = baseDamage + (float) (target.getMaxHealth() * COLLAPSE_MAX_HEALTH_RATIO);
         target.hurt(level.damageSources().indirectMagic(player, player), damage);

         // 5. 极低血量直接热寂斩杀
         if (target.isAlive() && target.getHealth() <= target.getMaxHealth() * 0.5F) {
            NightfallDragonFinalVisuals.spawnExecutionBurst(level, target);
            EntropyDissolutionLogic.executeFinal(target, player);
         }
      }
   }
}
