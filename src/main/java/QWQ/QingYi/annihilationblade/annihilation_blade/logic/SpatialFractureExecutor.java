package QWQ.QingYi.annihilationblade.annihilation_blade.logic;

import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.render.FxEvents;
import QWQ.QingYi.annihilationblade.render.FxTypes;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.LinkedHashSet;
import java.util.Set;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class SpatialFractureExecutor {
   private SpatialFractureExecutor() {
   }

   public static void unleash(LivingEntity entity) {
      if (entity instanceof Player player) {
         if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel)entity.level();
            ModConfig.SpatialFracture config = ModConfig.COMMON.annihilationBlade.spatialFracture;
            double radius = config.fractureRadius.getValue();
            double visualScale = config.visualScale.getValue();
            Vec3 center = getFractureCenter(level, player, config);
            Set<LivingEntity> targets = gatherTargets(level, player, center, config);
            playOpeningRupture(level, player, center);
            // 视觉全部交给客户端专属渲染器：必须在处决之前发送，保证客户端解析目标实体时它们尚未被移除。
            sendFractureFx(level, player, center, targets, radius * visualScale, visualScale, config);
            spawnCenterSlashes(player, config);
            int visualized = 0;

            for (LivingEntity target : targets) {
               if (visualized < config.maxVisualizedTargets.getValue()) {
                  AttackManager.doSlash(player, player.getRandom().nextInt(360), Vec3.ZERO, true, true, 9999.0);
               }

               TerminusLogic.execute(target, player);
               visualized++;
            }
         }
      }
   }

   /**
    * 发送「空间破碎」特效事件。爆炸音效随特效时间轴在客户端湮灭时刻播放，因此服务端不再播放。
    */
   private static void sendFractureFx(
      ServerLevel level, Player player, Vec3 center, Set<LivingEntity> targets, double visualRadius, double visualScale, ModConfig.SpatialFracture config
   ) {
      int limit = Math.min(Math.min(targets.size(), config.maxVisualizedTargets.getValue()), FxEventPacket.MAX_ENTITIES);
      int[] ids = new int[limit];
      int index = 0;
      for (LivingEntity target : targets) {
         if (index >= limit) {
            break;
         }
         ids[index++] = target.getId();
      }
      FxEvents.send(level, new FxEventPacket(
         FxTypes.SPATIAL_FRACTURE,
         center,
         player.getLookAngle(),
         (float)visualRadius,
         (float)visualScale,
         config.fractureSlashes.getValue(),
         player.getRandom().nextLong(),
         player.getId(),
         ids
      ));
   }

   private static Vec3 getFractureCenter(ServerLevel level, Player player, ModConfig.SpatialFracture config) {
      double maxDistance = config.maxDistance.getValue();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      Vec3 end = eye.add(look.scale(maxDistance));
      HitResult hit = level.clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
      Vec3 blockOrAir = hit.getType() == Type.MISS ? end : hit.getLocation();
      Vec3 aimedTarget = findAimedTargetCenter(level, player, eye, look, eye.distanceTo(blockOrAir), config.entityLockRadius.getValue());
      return aimedTarget == null ? blockOrAir : aimedTarget;
   }

   private static Vec3 findAimedTargetCenter(ServerLevel level, Player player, Vec3 eye, Vec3 look, double maxDistance, double entityLockRadius) {
      Vec3 end = eye.add(look.scale(maxDistance));
      AABB rayArea = new AABB(eye, end).inflate(entityLockRadius);
      Vec3 bestCenter = null;
      double bestProjection = Double.MAX_VALUE;

      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, rayArea, entity -> canTarget(player, entity))) {
         Vec3 candidateCenter = candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0);
         double projection = candidateCenter.subtract(eye).dot(look);
         if (!(projection < 0.0) && !(projection > maxDistance)) {
            Vec3 nearestPoint = eye.add(look.scale(projection));
            double lockRadius = entityLockRadius + candidate.getBbWidth() * 0.5;
            if (candidateCenter.distanceToSqr(nearestPoint) <= lockRadius * lockRadius && projection < bestProjection) {
               bestProjection = projection;
               bestCenter = candidateCenter;
            }
         }
      }

      return bestCenter;
   }

   private static Set<LivingEntity> gatherTargets(ServerLevel level, Player player, Vec3 center, ModConfig.SpatialFracture config) {
      double radius = config.fractureRadius.getValue();
      int maxTargets = config.maxTargets.getValue();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      Set<LivingEntity> targets = new LinkedHashSet<>();
      AABB fracture = new AABB(center, center).inflate(radius);

      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, fracture, entity -> canTarget(player, entity))) {
         if (candidate.position().distanceToSqr(center) <= radius * radius) {
            targets.add(candidate);
            if (targets.size() >= maxTargets) {
               return targets;
            }
         }
      }

      double pathLength = Math.min(config.maxDistance.getValue(), eye.distanceTo(center));
      if (pathLength >= 2.0 && targets.size() < maxTargets) {
         double sampleRadius = config.raySampleRadius.getValue();
         double sampleRadiusSqr = sampleRadius * sampleRadius;
         Vec3 rayEnd = eye.add(look.scale(pathLength));
         AABB rayBox = new AABB(eye, rayEnd).inflate(sampleRadius);

         for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, rayBox, entity -> canTarget(player, entity))) {
            if (targets.contains(candidate)) {
               continue;
            }
            Vec3 candidatePos = candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0);
            Vec3 v = candidatePos.subtract(eye);
            double proj = v.dot(look);
            if (proj >= 2.0 && proj <= pathLength) {
               double distToRaySqr = v.lengthSqr() - proj * proj;
               if (distToRaySqr <= sampleRadiusSqr) {
                  targets.add(candidate);
                  if (targets.size() >= maxTargets) {
                     break;
                  }
               }
            }
         }
      }

      if (targets.isEmpty()) {
         AABB fallback = player.getBoundingBox().inflate(config.backupRadius.getValue());
         targets.addAll(level.getEntitiesOfClass(LivingEntity.class, fallback, entity -> canTarget(player, entity)));
      }

      return targets;
   }

   private static boolean canTarget(Player player, LivingEntity candidate) {
      return SlashBladeTargeting.canAttack(player, candidate);
   }

   private static void playOpeningRupture(ServerLevel level, Player player, Vec3 center) {
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5F, 0.35F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 4.0F, 1.4F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 2.0F, 0.55F);
   }

   /**
    * 中心环形 SlashBlade 原生斩击（伤害/判定由 SlashBlade 处理，此处不涉及任何视觉粒子）。
    */
   private static void spawnCenterSlashes(Player player, ModConfig.SpatialFracture config) {
      int centerSlashes = config.centerSlashes.getValue();
      for (int i = 0; i < centerSlashes; i++) {
         AttackManager.doSlash(player, 360.0F * i / Math.max(1, centerSlashes), Vec3.ZERO, true, true, 9999.0);
      }
   }
}