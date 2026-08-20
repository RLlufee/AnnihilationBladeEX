package QWQ.QingYi.annihilationblade.infinity_stellaris.logic;

import QWQ.QingYi.annihilationblade.common.ServerTickScheduler;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import QWQ.QingYi.annihilationblade.infinity_stellaris.visual.InfinityStellarisVisuals;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * <h1>熵灭溶解抹杀 (Entropy Dissolution & Execution) 核心逻辑类</h1>
 * <p>
 * 本类展示了 Minecraft Mod 开发中最为强大的“绝对抹杀/绕过防死机制”的实现方式：
 * <ul>
 *   <li><b>高优先级受击拦截 (LivingHurtEvent HIGH)</b>：直接按目标最大生命值百分比切割 HP，绕过护甲与普通减伤。</li>
 *   <li><b>重入保护锁 (INTERNAL_EXECUTION Set)</b>：在调用 {@code target.hurt()} 时将目标加入防重入集合，防止触发递归事件死循环。</li>
 *   <li><b>三重保底斩杀机制 (executeFinal)</b>：
 *     <ol>
 *       <li>第一重：造成 10 亿点数值伤害 (1.0E9F)。</li>
 *       <li>第二重：若目标仍存活（如带防死图腾、锁血 Boss），直接强行 {@code setHealth(0.0F)} 并调用 {@code die(source)}。</li>
 *       <li>第三重：对于非玩家实体，直接将其从世界移除 {@code target.remove(Entity.RemovalReason.KILLED)} 并 {@code discard()}。</li>
 *       <li>第四重：在 1 Tick 后调度延迟移除，彻底抹除复活/残存实体。</li>
 *     </ol>
 *   </li>
 * </ul>
 */
@EventBusSubscriber(modid = "annihilationblade")
public final class EntropyDissolutionLogic {
   /** 斩杀时使用的基准伤害数值：10 亿点 (1.0E9F) */
   private static final float EXECUTION_DAMAGE = 1.0E9F;

   /** 记录每个玩家对目标累积的熵灭印记层数 */
   private static final Map<UUID, Map<UUID, Integer>> MARKS = new HashMap<>();

   /** 黑名单时间戳映射，防止目标短时间内反复触发受击逻辑 */
   private static final Map<UUID, Long> BLACKLISTED_UNTIL = new HashMap<>();

   /** 内部斩杀重入锁集合（防止 executeFinal 中引发的 LivingHurtEvent 再次进入导致堆栈溢出） */
   private static final Set<UUID> INTERNAL_EXECUTION = new HashSet<>();

   private EntropyDissolutionLogic() {
   }

   /**
    * 生物受击事件监听（高优先级）。
    */
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onHurt(LivingHurtEvent event) {
      LivingEntity target = event.getEntity();
      // 客户端不处理，或如果当前处于本类的内部斩杀流程中，直接跳过
      if (target.level().isClientSide || isInternalExecution(target)) {
         return;
      }

      purgeBlacklist(target.level().getGameTime());
      Entity source = event.getSource().getEntity();
      Entity directSource = event.getSource().getDirectEntity();

      // 检查伤害来源是否为手持无限星芒刀的玩家
      if (!(source instanceof Player player) || !isInfinityDamage(player, event.getSource(), directSource)) {
         return;
      }

      // 判断目标是否可以被攻击（避免误伤队友或无敌队友）
      if (!SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      // 按配置的百分比（如 20%）直接削减目标当前 HP
      double percent = ModConfig.COMMON.infinityStellaris.entropyPercent.get();
      float entropyDamage = (float)(target.getMaxHealth() * percent);
      float newHealth = Math.max(1.0F, target.getHealth() - entropyDamage);
      target.setHealth(newHealth); // 强行写入目标 HP

      // 增加印记层数
      int marks = addMark(player, target);
      if (marks >= ModConfig.COMMON.infinityStellaris.entropyMarks.get()) {
         clearMark(player, target);
         executeFinal(target, player); // 印记叠满，触发终极绝对抹杀！
      } else if (target.level() instanceof ServerLevel level) {
         InfinityStellarisVisuals.spawnDamageChain(level, player, target);
         spawnEntropyTrace(level, target, marks);
      }
   }

   /**
    * 判断伤害来源是否为无限星芒直接攻击或刀光攻击。
    */
   public static boolean isInfinityDamage(Player player, DamageSource source, Entity directSource) {
      return InfinityStellarisItemSupport.isDirectInfinityAttack(player, source)
         || InfinityStellarisItemSupport.isInfinitySlashEntityAttack(player, directSource);
   }

   /**
    * 判断指定实体当前是否正处于绝对抹杀的执行流程中。
    */
   public static boolean isInternalExecution(LivingEntity target) {
      return target != null && INTERNAL_EXECUTION.contains(target.getUUID());
   }

   /**
    * <b>终极绝对抹杀执行入口 (executeFinal)</b>。
    * 结合了物理伤害、强制清零生命、触发死亡逻辑以及直接销毁实体（discard）。
    *
    * @param target 被斩杀的目标
    * @param attacker 攻击玩家
    */
   public static void executeFinal(LivingEntity target, Player attacker) {
      if (target.level().isClientSide || !SlashBladeTargeting.canAttack(attacker, target)) {
         return;
      }

      target.invulnerableTime = 0; // 重置无敌帧
      DamageSource source = target.level().damageSources().playerAttack(attacker);
      
      if (target.level() instanceof ServerLevel level) {
         InfinityStellarisVisuals.spawnDamageChain(level, attacker, target);
         spawnHeatDeath(level, target); // 播放热寂/热寂灭粒子音效
      }

      // 上锁：防止 target.hurt() 触发 LivingHurtEvent 再次调用本方法造成死循环
      INTERNAL_EXECUTION.add(target.getUUID());
      try {
         if (target instanceof Player) {
            // 对玩家目标：造成 10 亿点伤害，若没死则直接 setHealth(0.0F) 并调用 die()
            target.hurt(source, EXECUTION_DAMAGE);
            if (target.isAlive()) {
               target.invulnerableTime = 0;
               target.setHealth(0.0F);
               target.die(source);
            }
         } else {
            // 对非玩家目标（Boss/普通怪物）：剥夺 AI -> 造成 10 亿伤害 -> 强制 setHealth(0) -> 从世界 discard 物理物理移除
            disableNonPlayerTarget(target);
            target.hurt(source, EXECUTION_DAMAGE);
            if (target.isAlive()) {
               target.invulnerableTime = 0;
               target.setHealth(0.0F);
               target.die(source);
            }

            eraseNonPlayerTarget(target);
            blacklist(target);

            // 1 Tick 后再次调度强行移除，确保挂载死亡动画或带复活逻辑的怪也彻底消失
            ServerTickScheduler.schedule(1, () -> eraseNonPlayerTarget(target));
         }
      } finally {
         // 解锁
         INTERNAL_EXECUTION.remove(target.getUUID());
      }
   }

   public static void clearPlayer(UUID playerId) {
      MARKS.remove(playerId);
   }

   private static int addMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = MARKS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
      int marks = playerMarks.getOrDefault(target.getUUID(), 0) + 1;
      playerMarks.put(target.getUUID(), marks);
      return marks;
   }

   private static void clearMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = MARKS.get(player.getUUID());
      if (playerMarks != null) {
         playerMarks.remove(target.getUUID());
         if (playerMarks.isEmpty()) {
            MARKS.remove(player.getUUID());
         }
      }
   }

   private static void disableNonPlayerTarget(LivingEntity target) {
      target.stopRiding();
      target.ejectPassengers();
      target.setDeltaMovement(Vec3.ZERO);
      target.hasImpulse = true;
      if (target instanceof Mob mob) {
         mob.setTarget(null);
         mob.getNavigation().stop();
         mob.setNoAi(true);
      }
   }

   /**
    * 从世界物理层面上销毁并丢弃非玩家实体。
    */
   private static void eraseNonPlayerTarget(LivingEntity target) {
      if (target instanceof Player) {
         return;
      }

      disableNonPlayerTarget(target);
      target.remove(Entity.RemovalReason.KILLED);
      target.discard(); // 直接丢弃实体，彻底从区块实体列表中抹除
   }

   private static void blacklist(LivingEntity target) {
      long expiry = target.level().getGameTime() + ModConfig.COMMON.infinityStellaris.entropyBlacklistTicks.get();
      BLACKLISTED_UNTIL.put(target.getUUID(), expiry);
   }

   private static void purgeBlacklist(long gameTime) {
      Iterator<Map.Entry<UUID, Long>> iterator = BLACKLISTED_UNTIL.entrySet().iterator();
      while (iterator.hasNext()) {
         if (iterator.next().getValue() <= gameTime) {
            iterator.remove();
         }
      }
   }

   private static void spawnEntropyTrace(ServerLevel level, LivingEntity target, int marks) {
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      double radius = Math.max(0.7, target.getBbWidth() * (1.0 + marks * 0.04));
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 6 + marks, radius * 0.35, radius * 0.35, radius * 0.35, 0.01);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 10 + marks, radius * 0.45, radius * 0.45, radius * 0.45, 0.08);
   }

   /**
    * 生成宇宙热寂级别的死寂粒子与音效爆炸。
    */
   private static void spawnHeatDeath(ServerLevel level, LivingEntity target) {
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      double radius = Math.max(1.0, target.getBbWidth() * 2.0);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 0.45F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 0.8F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 80, radius * 0.5, radius * 0.8, radius * 0.5, 0.1);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 64, radius * 1.0, radius * 1.0, radius * 1.0, 0.2);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 160, radius * 1.2, radius * 1.2, radius * 1.2, 0.5);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 100, radius * 0.8, radius * 0.8, radius * 0.8, 0.05);
   }
}

