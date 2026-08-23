package QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationbladeex.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.item.InfinityStellarisItemSupport;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 曲率撕裂逻辑 (含 AI 擦除与生物状态还原)
 */
@EventBusSubscriber(modid = AnnihilationBladeEX.MODID)
public final class CurvatureRuptureLogic {
   private static final Map<UUID, Boolean> AI_ERASURE_ENABLED = new HashMap<>();
   private static final Map<UUID, FrozenMobState> FROZEN_MOBS = new HashMap<>();
   private static final Map<UUID, Map<UUID, Integer>> STRAIN_MARKS = new HashMap<>();
   private static final Set<UUID> AI_RESTORE_DISABLED = new HashSet<>();

   private CurvatureRuptureLogic() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      if (player.level().isClientSide) {
         return;
      }

      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      if (!InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
         setAiErasureEnabled(player, false);
         releasePlayer(player);
         return;
      }

      if (isAiErasureEnabled(player)) {
         freezeNearby(level, player);
      } else {
         releasePlayer(player);
      }
   }

   public static boolean isAiErasureEnabled(Player player) {
      return AI_ERASURE_ENABLED.getOrDefault(player.getUUID(), false);
   }

   public static void setAiErasureEnabled(Player player, boolean enabled) {
      UUID playerId = player.getUUID();
      if (enabled) {
         AI_ERASURE_ENABLED.put(playerId, true);
      } else {
         AI_ERASURE_ENABLED.remove(playerId);
         releasePlayer(player);
      }
   }

   public static boolean isAiRestoreEnabled(Player player) {
      return !AI_RESTORE_DISABLED.contains(player.getUUID());
   }

   public static void setAiRestoreEnabled(Player player, boolean enabled) {
      UUID playerId = player.getUUID();
      if (enabled) {
         AI_RESTORE_DISABLED.remove(playerId);
         restorePendingMobs(player);
      } else {
         AI_RESTORE_DISABLED.add(playerId);
      }
   }

   public static void clearPlayer(Player player) {
      UUID playerId = player.getUUID();
      AI_ERASURE_ENABLED.remove(playerId);
      STRAIN_MARKS.remove(playerId);
      releasePlayer(player);
      AI_RESTORE_DISABLED.remove(playerId);
   }

   private static void freezeNearby(ServerLevel level, Player player) {
      ModConfig.InfinityStellaris config = ModConfig.COMMON.infinityStellaris;
      List<LivingEntity> targets = SpecialEffectSupport.limit(
         SpecialEffectSupport.radialTargets(
            level, player, player.position(), config.curvatureRadius.getValue(), entity -> SlashBladeTargeting.canAttack(player, entity)
         ),
         config.curvatureMaxTargets.getValue()
      );
      long gameTime = level.getGameTime();
      boolean strainTick = gameTime % Math.max(1, config.curvatureTickInterval.getValue()) == 0L;
      Set<UUID> strainedTargets = new HashSet<>();
      for (LivingEntity target : targets) {
         if (target instanceof Mob mob) {
            freezeMob(mob, player);
         } else {
            freezeLiving(target);
         }

         if (strainTick) {
            strainedTargets.add(target.getUUID());
            applyCurvatureStrain(level, player, target, config);
         }

         if (gameTime % 2L == 0L) {
            Vec3 center = SpecialEffectSupport.centerOf(target);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 24, 0.45, 0.55, 0.45, 0.015);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 36, 0.6, 0.6, 0.6, 0.12);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 12, 0.5, 0.5, 0.5, 0.05);
            if (gameTime % 20L == 0L) {
               level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.5, center.z, 1, 0.0, 0.0, 0.0, 0.0);
               level.playSound(null, center.x, center.y, center.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7F, 1.5F);
            }
         }
      }

      if (strainTick) {
         clearMissingStrainMarks(player, strainedTargets);
      }
   }

   private static void freezeMob(Mob mob, Player player) {
      UUID mobId = mob.getUUID();
      FrozenMobState state = FROZEN_MOBS.computeIfAbsent(mobId, ignored -> new FrozenMobState(mob.isNoAi()));
      state.owners.add(player.getUUID());
      mob.setTarget(null);
      mob.getNavigation().stop();
      mob.setNoAi(true);
      mob.hasImpulse = true;
      mob.setDeltaMovement(Vec3.ZERO);
      mob.fallDistance = 0.0F;
   }

   private static void releasePlayer(Player player) {
      MinecraftServer server = player.getServer();
      STRAIN_MARKS.remove(player.getUUID());
      if (server == null || FROZEN_MOBS.isEmpty()) {
         return;
      }

      UUID playerId = player.getUUID();
      boolean restoreAi = isAiRestoreEnabled(player);
      Iterator<Map.Entry<UUID, FrozenMobState>> iterator = FROZEN_MOBS.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<UUID, FrozenMobState> entry = iterator.next();
         FrozenMobState state = entry.getValue();
         if (!state.owners.remove(playerId)) {
            continue;
         }

         if (!restoreAi) {
            state.pendingRestoreOwners.add(playerId);
            continue;
         }

         if (!state.owners.isEmpty()) {
            continue;
         }

         state.pendingRestoreOwners.remove(playerId);
         restoreMob(server, entry.getKey(), state);
         iterator.remove();
      }
   }

   private static void restorePendingMobs(Player player) {
      MinecraftServer server = player.getServer();
      if (server == null || FROZEN_MOBS.isEmpty()) {
         return;
      }

      UUID playerId = player.getUUID();
      Iterator<Map.Entry<UUID, FrozenMobState>> iterator = FROZEN_MOBS.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<UUID, FrozenMobState> entry = iterator.next();
         FrozenMobState state = entry.getValue();
         if (!state.pendingRestoreOwners.contains(playerId) || !state.owners.isEmpty()) {
            continue;
         }

         state.pendingRestoreOwners.remove(playerId);
         restoreMob(server, entry.getKey(), state);
         iterator.remove();
      }
   }

   private static boolean restoreMob(MinecraftServer server, UUID mobId, FrozenMobState state) {
      for (ServerLevel level : server.getAllLevels()) {
         Entity entity = level.getEntity(mobId);
         if (entity instanceof Mob mob && mob.isAlive()) {
            mob.setNoAi(state.originalNoAi);
            mob.hasImpulse = true;
            return true;
         }
      }
      return false;
   }

   private static void freezeLiving(LivingEntity target) {
      target.stopRiding();
      target.ejectPassengers();
      target.setDeltaMovement(Vec3.ZERO);
      target.fallDistance = 0.0F;
      target.hasImpulse = true;
   }

   private static void applyCurvatureStrain(ServerLevel level, Player player, LivingEntity target, ModConfig.InfinityStellaris config) {
      if (!target.isAlive() || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      int marks = addStrainMark(player, target);
      spawnStrainParticles(level, target, marks);
      if (marks >= config.curvatureBurstMarks.getValue()) {
         clearStrainMark(player, target);
         triggerCurvatureBurst(level, player, target);
      }
   }

   private static int addStrainMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = STRAIN_MARKS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
      int marks = playerMarks.getOrDefault(target.getUUID(), 0) + 1;
      playerMarks.put(target.getUUID(), marks);
      return marks;
   }

   private static void clearStrainMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = STRAIN_MARKS.get(player.getUUID());
      if (playerMarks == null) {
         return;
      }

      playerMarks.remove(target.getUUID());
      if (playerMarks.isEmpty()) {
         STRAIN_MARKS.remove(player.getUUID());
      }
   }

   private static void clearMissingStrainMarks(Player player, Set<UUID> activeTargets) {
      Map<UUID, Integer> playerMarks = STRAIN_MARKS.get(player.getUUID());
      if (playerMarks == null) {
         return;
      }

      playerMarks.keySet().removeIf(targetId -> !activeTargets.contains(targetId));
      if (playerMarks.isEmpty()) {
         STRAIN_MARKS.remove(player.getUUID());
      }
   }

   private static void triggerCurvatureBurst(ServerLevel level, Player player, LivingEntity target) {
      if (!target.isAlive() || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      ruptureTargetState(target);
      target.invulnerableTime = 0;
      spawnBurstParticles(level, target);
   }

   private static void ruptureTargetState(LivingEntity target) {
      target.stopRiding();
      target.ejectPassengers();
      target.removeAllEffects();
      target.setDeltaMovement(Vec3.ZERO);
      target.fallDistance = 0.0F;
      target.hasImpulse = true;
      if (target instanceof Mob mob) {
         mob.setTarget(null);
         mob.getNavigation().stop();
         mob.setNoAi(true);
      }
   }

   private static void spawnStrainParticles(ServerLevel level, LivingEntity target, int marks) {
      Vec3 center = SpecialEffectSupport.centerOf(target);
      double radius = Math.max(0.8, target.getBbWidth() * (1.2 + marks * 0.1));
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 10 + marks * 2, radius * 0.45, radius * 0.45, radius * 0.45, 0.04);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 16 + marks * 3, radius * 0.55, radius * 0.55, radius * 0.55, 0.08);
   }

   private static void spawnBurstParticles(ServerLevel level, LivingEntity target) {
      Vec3 center = SpecialEffectSupport.centerOf(target);
      double radius = Math.max(1.0, target.getBbWidth() * 2.5);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.35, center.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 80, radius, radius * 0.8, radius, 0.12);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 96, radius, radius, radius, 0.18);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8F, 0.7F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4F, 0.55F);
   }

   private static final class FrozenMobState {
      private final boolean originalNoAi;
      private final Set<UUID> owners = new HashSet<>();
      private final Set<UUID> pendingRestoreOwners = new HashSet<>();

      private FrozenMobState(boolean originalNoAi) {
         this.originalNoAi = originalNoAi;
      }
   }
}
