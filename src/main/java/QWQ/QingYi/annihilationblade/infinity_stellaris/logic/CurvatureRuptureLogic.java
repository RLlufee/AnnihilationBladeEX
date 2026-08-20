package QWQ.QingYi.annihilationblade.infinity_stellaris.logic;

import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
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
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * <h1>曲率撕裂与 AI 抹杀 (Curvature Rupture & AI Erasure) 核心逻辑类</h1>
 * <p>
 * 本类展示了控制周围生物行为、时空冻结与应力印记引爆的高级玩法：
 * <ul>
 *   <li><b>强行抹杀/封印生物 AI (setNoAi)</b>：将范围内的 Mob 设置为 NoAI 并清除寻路，实现“时间停止/时空冻结”的效果。</li>
 *   <li><b>多玩家并发所有权管理 (FrozenMobState)</b>：记录生物原本的 NoAI 状态以及当前正在对其施压的所有玩家 UUID，防止多玩家同时施法导致 AI 恢复逻辑混乱。</li>
 *   <li><b>曲率应力印记 (Strain Marks) 叠加机制</b>：在 Tick 循环中逐渐为目标增加应力印记，达到配置上限后触发“曲率撕裂爆破 (Curvature Burst)”。</li>
 *   <li><b>重置无敌帧 (invulnerableTime = 0)</b>：破除目标的受击无敌保护，确保高频打击能立刻生效。</li>
 * </ul>
 */
@EventBusSubscriber(modid = "annihilationblade")
public final class CurvatureRuptureLogic {
   /** 记录每个玩家是否开启了“AI 抹杀/曲率气场”的开关映射 */
   private static final Map<UUID, Boolean> AI_ERASURE_ENABLED = new HashMap<>();

   /** 记录被冻结生物 UUID 到其冻结前原始状态 (FrozenMobState) 的映射 */
   private static final Map<UUID, FrozenMobState> FROZEN_MOBS = new HashMap<>();

   /** 记录每一个玩家 (Player UUID) 对每一个目标 (Target UUID) 施加的曲率应力印记层数 */
   private static final Map<UUID, Map<UUID, Integer>> STRAIN_MARKS = new HashMap<>();

   /** 记录禁止自动恢复 AI 的玩家集合 */
   private static final Set<UUID> AI_RESTORE_DISABLED = new HashSet<>();

   private CurvatureRuptureLogic() {
   }

   /**
    * 玩家 Tick 监听器（服务端、Phase.END 阶段执行）。
    * 只要玩家手持无限星芒刀并开启了曲率气场，就会持续抹除周围敌对生物的 AI。
    */
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      // 必须在 Tick 结束阶段且为服务端环境
      if (event.phase != Phase.END || event.player.level().isClientSide) {
         return;
      }

      Player player = event.player;
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      // 如果未手持无限星芒，强制关闭 AI 抹杀气场并释放被冻结生物
      if (!InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
         setAiErasureEnabled(player, false);
         releasePlayer(player);
         return;
      }

      // 根据开关决定冻结周围生物还是释放它们
      if (isAiErasureEnabled(player)) {
         freezeNearby(level, player);
      } else {
         releasePlayer(player);
      }
   }

   /**
    * 查询玩家当前是否开启了 AI 抹杀气场。
    */
   public static boolean isAiErasureEnabled(Player player) {
      return AI_ERASURE_ENABLED.getOrDefault(player.getUUID(), false);
   }

   /**
    * 切换玩家的 AI 抹杀气场开关。
    */
   public static void setAiErasureEnabled(Player player, boolean enabled) {
      UUID playerId = player.getUUID();
      if (enabled) {
         AI_ERASURE_ENABLED.put(playerId, true);
      } else {
         AI_ERASURE_ENABLED.remove(playerId);
         releasePlayer(player); // 关闭时释放所有被该玩家冻结的目标
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

   /**
    * 清理玩家数据（例如玩家离线或切换武器时调用）。
    */
   public static void clearPlayer(Player player) {
      UUID playerId = player.getUUID();
      AI_ERASURE_ENABLED.remove(playerId);
      STRAIN_MARKS.remove(playerId);
      releasePlayer(player);
      AI_RESTORE_DISABLED.remove(playerId);
   }

   /**
    * 搜索玩家周身的敌对生物并执行冻结与曲率应力叠加。
    */
   private static void freezeNearby(ServerLevel level, Player player) {
      ModConfig.InfinityStellaris config = ModConfig.COMMON.infinityStellaris;
      
      // 以玩家坐标为中心，在半径 curvatureRadius 范围内寻找可攻击目标，并限制最大目标数量
      List<LivingEntity> targets = SpecialEffectSupport.limit(
         SpecialEffectSupport.radialTargets(
            level, player, player.position(), config.curvatureRadius.get(), entity -> SlashBladeTargeting.canAttack(player, entity)
         ),
         config.curvatureMaxTargets.get()
      );

      long gameTime = level.getGameTime();
      // 判断当前 Tick 是否需要更新应力印记
      boolean strainTick = gameTime % Math.max(1, config.curvatureTickInterval.get()) == 0L;
      Set<UUID> strainedTargets = new HashSet<>();

      for (LivingEntity target : targets) {
         if (target instanceof Mob mob) {
            freezeMob(mob, player); // 带有 AI 的常规 Mob：剥夺 AI 与寻路
         } else {
            freezeLiving(target);  // 普通 LivingEntity（如玩家等）：锁定动量与位置
         }

         // 满足 Tick 间隔时，增加曲率应力印记
         if (strainTick) {
            strainedTargets.add(target.getUUID());
            applyCurvatureStrain(level, player, target, config);
         }

         // 生成时空锁定的粒子特效（末影棒、反向传送门、电火花与定期音效）
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

      // 清理已离开范围或死亡目标的废弃印记数据
      if (strainTick) {
         clearMissingStrainMarks(player, strainedTargets);
      }
   }

   /**
    * 剥夺 Mob 的 AI 与移动能力（时空冻结核心逻辑）。
    */
   private static void freezeMob(Mob mob, Player player) {
      UUID mobId = mob.getUUID();
      // 获取或记录该 Mob 原始的 isNoAi 状态
      FrozenMobState state = FROZEN_MOBS.computeIfAbsent(mobId, ignored -> new FrozenMobState(mob.isNoAi()));
      state.owners.add(player.getUUID()); // 将当前玩家标记为该 Mob 的控制者之一

      mob.setTarget(null);           // 清除仇恨目标
      mob.getNavigation().stop();    // 立即停止寻路
      mob.setNoAi(true);             // 禁用 AI Tick，使生物木僵在原地
      mob.hasImpulse = true;
      mob.setDeltaMovement(Vec3.ZERO);// 清空三维速度向量（防止惯性滑行）
      mob.fallDistance = 0.0F;        // 清空摔落距离
   }

   /**
    * 释放当前玩家所控制的所有被冻结生物。
    */
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

         // 仅当没有其他玩家也在冻结此生物时，才恢复其原始 AI
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

   /**
    * 恢复 Mob 的原始 AI 状态。
    */
   private static boolean restoreMob(MinecraftServer server, UUID mobId, FrozenMobState state) {
      for (ServerLevel level : server.getAllLevels()) {
         Entity entity = level.getEntity(mobId);
         if (entity instanceof Mob mob && mob.isAlive()) {
            mob.setNoAi(state.originalNoAi); // 还原为冻结前的 isNoAi 状态
            mob.hasImpulse = true;
            return true;
         }
      }
      return false;
   }

   /**
    * 对非 Mob 类活体实体进行速度锁定与骑乘解脱。
    */
   private static void freezeLiving(LivingEntity target) {
      target.stopRiding();
      target.ejectPassengers();
      target.setDeltaMovement(Vec3.ZERO);
      target.fallDistance = 0.0F;
      target.hasImpulse = true;
   }

   /**
    * 施加曲率应力印记。若层数达到阈值，则触发“曲率爆破”。
    */
   private static void applyCurvatureStrain(ServerLevel level, Player player, LivingEntity target, ModConfig.InfinityStellaris config) {
      if (!target.isAlive() || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      int marks = addStrainMark(player, target);
      spawnStrainParticles(level, target, marks);

      // 印记达到爆发上限
      if (marks >= config.curvatureBurstMarks.get()) {
         clearStrainMark(player, target);
         triggerCurvatureBurst(level, player, target);
      }
   }

   /**
    * 为目标累加一层应力印记，并返回累加后的总层数。
    */
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

   /**
    * 触发曲率爆破：重置无敌帧、破除防护并引发高能声波音爆粒子。
    */
   private static void triggerCurvatureBurst(ServerLevel level, Player player, LivingEntity target) {
      if (!target.isAlive() || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      ruptureTargetState(target);
      target.invulnerableTime = 0; // 重置受击无敌帧，使得伤害可以立刻在此 Tick 结算
      spawnBurstParticles(level, target);
   }

   /**
    * 强制撕裂与破坏目标状态（脱离坐骑、移除所有药水 Buff、清除速度与重置 AI）。
    */
   private static void ruptureTargetState(LivingEntity target) {
      target.stopRiding();
      target.ejectPassengers();
      target.removeAllEffects(); // 移除所有增益/减益效果
      target.setDeltaMovement(Vec3.ZERO);
      target.fallDistance = 0.0F;
      target.hasImpulse = true;
      if (target instanceof Mob mob) {
         mob.setTarget(null);
         mob.getNavigation().stop();
         mob.setNoAi(true);
      }
   }

   /**
    * 根据印记层数动态扩大粒子扩散半径与密度。
    */
   private static void spawnStrainParticles(ServerLevel level, LivingEntity target, int marks) {
      Vec3 center = SpecialEffectSupport.centerOf(target);
      double radius = Math.max(0.8, target.getBbWidth() * (1.2 + marks * 0.1));
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 10 + marks * 2, radius * 0.45, radius * 0.45, radius * 0.45, 0.04);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 16 + marks * 3, radius * 0.55, radius * 0.55, radius * 0.55, 0.08);
   }

   /**
    * 生成爆破瞬间的强效冲击波粒子与 Warden 声波爆破音效。
    */
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

   /**
    * 内部静态类：存储被冻结生物在冻结之前的原始 NoAI 状态及控制者所有权列表。
    */
   private static final class FrozenMobState {
      private final boolean originalNoAi;
      private final Set<UUID> owners = new HashSet<>();
      private final Set<UUID> pendingRestoreOwners = new HashSet<>();

      private FrozenMobState(boolean originalNoAi) {
         this.originalNoAi = originalNoAi;
      }
   }
}

