package QWQ.QingYi.annihilationblade.annihilation_blade.logic;

import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
import QWQ.QingYi.annihilationblade.common.ServerTickScheduler;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WorldRiftChain {
   private static final int DELAY_TICKS = 5;
   private static boolean executingChain;

   private WorldRiftChain() {
   }

   public static boolean isExecutingChain() {
      return executingChain;
   }

   public static void scheduleFromDamage(ServerLevel level, Player attacker, LivingEntity damaged) {
      if (!executingChain) {
         schedule(level, attacker.getUUID(), SpecialEffectSupport.centerOf(attacker), SpecialEffectSupport.centerOf(damaged), 1);
      }
   }

   private static void schedule(ServerLevel level, UUID attackerId, Vec3 attackerOrigin, Vec3 center, int depth) {
      int maxDepth = ModConfig.COMMON.annihilationBlade.worldRift.chainCount.getValue();
      if (depth > maxDepth) {
         return;
      }

      ServerTickScheduler.schedule(DELAY_TICKS, () -> execute(level, attackerId, attackerOrigin, center, depth));
   }

   private static void execute(ServerLevel level, UUID attackerId, Vec3 attackerOrigin, Vec3 center, int depth) {
      ServerPlayer attacker = level.getServer().getPlayerList().getPlayer(attackerId);
      if (attacker == null || attacker.level() != level) {
         return;
      }

      ModConfig.WorldRift config = ModConfig.COMMON.annihilationBlade.worldRift;
      double chainRange = config.chainRange.getValue();
      double chainRangeSqr = chainRange * chainRange;
      if (center.distanceToSqr(attackerOrigin) > chainRangeSqr) {
         return;
      }

      double radius = config.radius.getValue();
      double visualScale = config.visualScale.getValue();
      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(
         level,
         attacker,
         center,
         radius,
         entity -> SpecialEffectSupport.centerOf(entity).distanceToSqr(attackerOrigin) <= chainRangeSqr
      );
      if (targets.isEmpty()) {
         return;
      }

      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.8F, 0.45F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.2F, 1.7F);
      // 先发事件再处决：客户端需要在目标被移除前解析到它们
      AnnihilationFxEvents.worldRift(level, attacker, center, radius, visualScale, depth, config.chainCount.getValue(), targets);
      int count = 0;
      for (LivingEntity target : targets) {
         Vec3 targetCenter = SpecialEffectSupport.centerOf(target);
         SpecialEffectSupport.pullToward(target, center, 0.18);
         executingChain = true;
         try {
            TerminusLogic.execute(target, attacker);
         } finally {
            executingChain = false;
         }

         if (depth < config.chainCount.getValue()) {
            schedule(level, attackerId, attackerOrigin, targetCenter, depth + 1);
         }
         count++;
      }
   }
}
