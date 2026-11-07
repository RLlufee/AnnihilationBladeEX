package QWQ.QingYi.annihilationblade.loli_blade.logic;

import QWQ.QingYi.annihilationblade.common.SlashBladeStateSupport;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.loli_blade.LoliBladeDefinitions;
import QWQ.QingYi.annihilationblade.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraft.server.level.ServerPlayer;

/** 萝莉刀的服务端处决逻辑，只处理 Minecraft 世界内的实体状态。 */
public final class LoliBladeCombatLogic {
   private static final float EXECUTION_DAMAGE = 1.0E9F;
   private static final double CONE_HALF_ANGLE_COSINE = 0.8660254037844386D;
   private static final Set<UUID> ACTIVE_EXECUTIONS = new HashSet<>();
   private static final Set<UUID> ACTIVE_ACTIONS = new HashSet<>();
   private static final java.util.Map<UUID, Long> FACING_COOLDOWNS = new java.util.HashMap<>();
   private static final java.util.Map<UUID, Long> AREA_COOLDOWNS = new java.util.HashMap<>();
   private static final java.util.Map<UUID, Long> LAST_SOUND_TICK = new java.util.HashMap<>();

   private LoliBladeCombatLogic() {
   }

   /** 准备施法：确保手持萝莉之刃属性与附魔就绪。 */
   public static void prepareCast(Player player) {
      ItemStack blade = player.getMainHandItem();
      if (!blade.isEmpty()) {
         LoliBladeDefinitions.ensureStats(blade, player.level());
      }
   }

   public static void clearPlayerState(UUID id) {
      FACING_COOLDOWNS.remove(id);
      AREA_COOLDOWNS.remove(id);
      LAST_SOUND_TICK.remove(id);
      ACTIVE_ACTIONS.remove(id);
   }

   public static void handleLivingAttack(LivingAttackEvent event) {
      if (event.isCanceled()
         || event.getEntity().level().isClientSide
         || "thorns".equals(event.getSource().getMsgId())
         || !(event.getSource().getEntity() instanceof Player attacker)
         || event.getSource().getDirectEntity() != attacker
         || !LoliBladeDefinitions.isLoliBlade(attacker.getMainHandItem())) {
         return;
      }

      if (!isActiveAttacker(attacker)) {
         event.setCanceled(true);
         return;
      }

      if (!SlashBladeTargeting.canAttack(attacker, event.getEntity()) || isProtectedOwner(event.getEntity())) {
         return;
      }

      event.setCanceled(true);
      UUID targetId = event.getEntity().getUUID();
      if (ACTIVE_EXECUTIONS.add(targetId)) {
         try {
            execute(event.getEntity(), attacker);
         } finally {
            ACTIVE_EXECUTIONS.remove(targetId);
         }
      }
   }

   public static void handleEntityAttack(AttackEntityEvent event) {
      Player attacker = event.getEntity();
      if (!LoliBladeDefinitions.isLoliBlade(attacker.getMainHandItem())) {
         return;
      }

      event.setCanceled(true);
      if (attacker.level().isClientSide || !isActiveAttacker(attacker)) {
         return;
      }

      if (event.getTarget() instanceof LivingEntity living) {
         if (!isProtectedOwner(living)) {
            execute(living, attacker);
         }
      } else if (ModConfig.COMMON.loliBlade.removeEntity.getValue()) {
         removeEntity(event.getTarget());
      }
   }

   /** 监听任意伤害事件：主手持有萝莉之刃且对合法生物造成任何伤害时播放音效。 */
   public static void handleLivingDamage(LivingDamageEvent event) {
      if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F) {
         return;
      }

      Entity directSource = event.getSource().getDirectEntity();
      Entity trueSource = event.getSource().getEntity();
      Player attacker = null;
      if (trueSource instanceof Player player) {
         attacker = player;
      } else if (directSource instanceof Player player) {
         attacker = player;
      }

      if (attacker == null) {
         return;
      }

      if (!isLoliBladeAttack(event.getSource(), attacker)) {
         return;
      }

      LivingEntity target = event.getEntity();
      if (target == attacker || !SlashBladeTargeting.canAttack(attacker, target) || isProtectedOwner(target)) {
         return;
      }

      playLoliAttackSound(attacker, target);
   }

   private static boolean isLoliBladeAttack(DamageSource source, Player attacker) {
      if (!LoliBladeDefinitions.isLoliBlade(attacker.getMainHandItem())) {
         return false;
      }
      Entity direct = source.getDirectEntity();
      if (direct == attacker || direct == null) {
         return true;
      }
      if (direct instanceof AbstractArrow || direct instanceof ThrowableItemProjectile) {
         return false;
      }
      return direct.getType().toString().contains("slashblade")
         || direct instanceof EntityAbstractSummonedSword;
   }

   public static void handleAction(Player attacker, Action action) {
      if (attacker.level().isClientSide || !isActiveAttacker(attacker) || !(attacker.level() instanceof ServerLevel level)) {
         return;
      }

      UUID id = attacker.getUUID();
      if (!ACTIVE_ACTIONS.add(id)) {
         return;
      }

      try {
         ModConfig.LoliBlade config = ModConfig.COMMON.loliBlade;
         java.util.Map<UUID, Long> cooldowns = action == Action.FACING ? FACING_COOLDOWNS : AREA_COOLDOWNS;
         int cooldown = action == Action.FACING ? config.facingCooldownTicks.getValue() : config.areaCooldownTicks.getValue();
         if (!QWQ.QingYi.annihilationblade.common.SpecialEffectSupport.tryStartCooldown(cooldowns, attacker, level.getGameTime(), cooldown)) {
            return;
         }

         double range = action == Action.FACING ? config.attackRange.getValue() : config.slashArtRange.getValue();
         List<Entity> targets = action == Action.FACING
            ? facingTargets(level, attacker, range, config.maxTargets.getValue())
            : areaTargets(level, attacker, range, config.maxTargets.getValue());
         for (Entity target : targets) {
            executeEntity(target, attacker);
         }
      } finally {
         ACTIVE_ACTIONS.remove(id);
      }
   }

   public static void handleFacingSpecialEffect(Player attacker) {
      handleAction(attacker, Action.FACING);
   }

   public static void handleAreaSlashArt(Player attacker) {
      handleAction(attacker, Action.AREA);
   }

   public static void execute(LivingEntity target, Player attacker) {
      if (target.level().isClientSide
         || !SlashBladeTargeting.canAttack(attacker, target)
         || !isActiveAttacker(attacker)
         || isProtectedOwner(target)) {
         return;
      }

      ModConfig.LoliBlade config = ModConfig.COMMON.loliBlade;
      if (target instanceof Player player && config.clearInventory.getValue()) {
         clearPlayerInventory(player);
      }

      target.invulnerableTime = 0;
      target.setAbsorptionAmount(0.0F);
      DamageSource source = target.level().damageSources().playerAttack(attacker);
      ItemStack blade = attacker.getMainHandItem();
      int killCountBefore = getKillCount(blade);

      // 第一击：走原版伤害链，保留战利品、经验、统计和死亡事件。
      target.hurt(source, EXECUTION_DAMAGE);
      playLoliAttackSound(attacker, target);

      if (config.ultimateObliterate.getValue()) {
         //不依赖原版伤害链是否成功，直接物理抹除，不可被第三方拦截。
         obliterate(target, source);
      } else {
         // 兼容软路径：仅当开启对应开关时做 setHealth(0)+die() 兜底。
         if (target.isAlive() && config.forceDeath.getValue()) {
            target.invulnerableTime = 0;
            target.setHealth(0.0F);
            target.die(source);
         }
         if (target.isAlive() && config.removeEntity.getValue()) {
            target.remove(Entity.RemovalReason.KILLED);
         }
      }

      boolean defeated = !target.isAlive() || target.isRemoved();
      if (defeated) {
         ensureKillCountIncremented(blade, killCountBefore);
      }

      if (target instanceof ServerPlayer player && config.kickPlayer.getValue()) {
         player.connection.disconnect(Component.translatable("message.annihilationblade.loli_blade.kick"));
      }
   }

   private static void clearPlayerInventory(Player player) {
      player.getInventory().clearContent();
      player.getEnderChestInventory().clearContent();
   }

   private static void removeEntity(Entity target) {
      if (!target.isRemoved()) {
         target.remove(Entity.RemovalReason.KILLED);
      }
   }

   /**
    * 物理抹除：绕过创造免伤、无敌帧、护盾、抗性、图腾与第三方事件取消，
    * 确保目标进入死亡流程。非玩家实体追加硬移除并关闭更新，几乎不可被任何手段拦截。
    */
   private static void obliterate(LivingEntity target, DamageSource source) {
      if (!target.isAlive() && target.getHealth() <= 0.0F) {
         return; // 已被第一击当场击杀，无需重复处理
      }
      target.invulnerableTime = 0;
      target.setAbsorptionAmount(0.0F);
      // 同步数据层 / setter 双路归零（setter 会更新 entityData 同步层）
      target.setHealth(0.0F);
      target.die(source);
      target.setHealth(-1.0F);
      // 反射补刀：直接压私有 health 字段，防止某模组覆写 setHealth 绕过
      pokeHealthField(target);
      if (!(target instanceof Player)) {
         // 关闭更新并硬移除，防止被再次 tick 复活 / 移动后脱离控制
         setCanUpdateFalse(target);
         if (!target.isRemoved()) {
            target.remove(Entity.RemovalReason.KILLED);
         }
      }
   }

   /** 反射直接置零 LivingEntity 的私有 health 字段；带 try/catch，失败不影响主流程。 */
   private static void pokeHealthField(LivingEntity entity) {
      try {
         java.lang.reflect.Field field = LivingEntity.class.getDeclaredField("health");
         field.setAccessible(true);
         field.set(entity, 0.0F);
      } catch (Throwable ignored) {
         // 字段名随映射通道变化或不存在时静默忽略，主流程已通过 setHealth 兜底
      }
   }

   /** 反射关闭 Entity.canUpdate（该字段为 private，跨包不可直接访问）；失败不影响主流程。 */
   private static void setCanUpdateFalse(Entity entity) {
      try {
         java.lang.reflect.Field field = Entity.class.getDeclaredField("canUpdate");
         field.setAccessible(true);
         field.set(entity, false);
      } catch (Throwable ignored) {
         // 字段不存在或不可访问时静默忽略，硬移除已使实体脱离世界
      }
   }

   private static boolean isActiveAttacker(Player player) {
      return ModConfig.COMMON.loliBlade.attackEnabled.getValue()
         && LoliBladeDefinitions.isOwnedBy(player.getMainHandItem(), player);
   }

   private static boolean isProtectedOwner(LivingEntity target) {
      return target instanceof Player player && LoliBladeDefenseLogic.hasOwnedLoliBlade(player);
   }

   private static List<Entity> areaTargets(ServerLevel level, Player attacker, double range, int maxTargets) {
      Vec3 center = attacker.position();
      AABB area = new AABB(center, center).inflate(range);
      List<Entity> targets = level.getEntitiesOfClass(
         Entity.class,
         area,
         target -> isAreaTarget(attacker, target)
            && target.getBoundingBox().getCenter().distanceToSqr(center) <= range * range
      );
      targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)));
      return limit(targets, maxTargets);
   }

   private static List<Entity> facingTargets(ServerLevel level, Player attacker, double range, int maxTargets) {
      Vec3 start = attacker.getEyePosition();
      Vec3 direction = attacker.getLookAngle().normalize();
      Vec3 end = start.add(direction.scale(range));
      AABB area = new AABB(start, end).inflate(2.5D, 2.5D, 2.5D);
      List<Entity> targets = level.getEntitiesOfClass(
         Entity.class,
         area,
         target -> {
            if (!isAreaTarget(attacker, target)) {
               return false;
            }

            Vec3 delta = target.getBoundingBox().getCenter().subtract(start);
            double projection = delta.dot(direction);
            return projection >= 0.0D && projection <= range
               && (projection <= 1.0D || delta.normalize().dot(direction) >= CONE_HALF_ANGLE_COSINE);
         }
      );
      targets.sort(Comparator.comparingDouble(target -> target.position().subtract(attacker.position()).lengthSqr()));
      return limit(targets, maxTargets);
   }

   private static boolean isAreaTarget(Player attacker, Entity target) {
      if (target == attacker || !target.isAlive()) {
         return false;
      }

      if (target instanceof LivingEntity living) {
         return SlashBladeTargeting.canAttack(attacker, living) && !isProtectedOwner(living);
      }

      return ModConfig.COMMON.loliBlade.removeEntity.getValue();
   }

   private static List<Entity> limit(List<Entity> targets, int maxTargets) {
      int limit = Math.max(1, maxTargets);
      return targets.size() <= limit ? targets : new ArrayList<>(targets.subList(0, limit));
   }

   private static void executeEntity(Entity target, Player attacker) {
      if (target instanceof LivingEntity living) {
         execute(living, attacker);
      } else if (ModConfig.COMMON.loliBlade.removeEntity.getValue()) {
         removeEntity(target);
      }
   }

   public static void playLoliAttackSound(Player attacker, LivingEntity target) {
      if (attacker.level() instanceof ServerLevel level) {
         long gameTime = level.getGameTime();
         Long lastTick = LAST_SOUND_TICK.get(attacker.getUUID());
         if (lastTick == null || lastTick != gameTime) {
            LAST_SOUND_TICK.put(attacker.getUUID(), gameTime);
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), ModSounds.LOLI_SUCCESS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }
   }

   public enum Action {
      FACING,
      AREA
   }

   private static int getKillCount(ItemStack blade) {
      return SlashBladeStateSupport.isSlashBlade(blade) ? SlashBladeStateSupport.killCount(blade) : -1;
   }

   private static void ensureKillCountIncremented(ItemStack blade, int killCountBefore) {
      if (killCountBefore >= 0) {
         SlashBladeStateSupport.state(blade).ifPresent(state -> {
            if (state.getKillCount() == killCountBefore) {
               state.setKillCount(killCountBefore + 1);
            }
         });
      }
   }
}
