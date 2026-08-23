package QWQ.QingYi.annihilationblade.nightfall_dragon.logic;

import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.EntropyDissolutionLogic;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.GammaThunderburstLogic;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationblade.nightfall_dragon.visual.NightfallDragonFinalVisuals;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * <h1>永夜魔龙 - 终极形态 (Final Form State & Special Effects) 逻辑全解</h1>
 * <p>
 * 本类是模组中最复杂且强大的逻辑核心，包含以下模组开发的高阶玩法：
 * <ul>
 *   <li><b>持刀者最高优先级免死 (onBearerHurt / onBearerDeath - EventPriority.HIGHEST)</b>：只要背包中带有终极形态魔龙刀，拦截一切伤害与死亡事件（取消事件、恢复血量与饱食度，并把伤害 100% 弹射给攻击者）。</li>
 *   <li><b>龙神之躯 (Dragon God Body) 悬浮与掉虚空拯救</b>：赋予玩家创造飞行权限 (mayfly)，并在玩家落入 Y < -64 虚空时瞬间传送回 320 高空。</li>
 *   <li><b>创世护盾 (Creation Shield) 转换算法</b>：将输出伤害超出血量上限的部分按比例转化为额外黄血护盾（最高 200 HP）。</li>
 *   <li><b>绝对湮灭结界 (Absolute Annihilation Domain)</b>：剥夺敌人所有药水 Buff、压制速度、锁定 AI，并在敌生命值低于 50% 时直接调用熵灭斩杀。</li>
 *   <li><b>万龙剑阵 (Myriad Dragon Blade Storm) 与三维偏航角/俯仰角计算</b>：在玩家周围生成环绕轨迹剑阵，使用 {@code yawToFace} 和 {@code pitchToFace} 计算方向向量射向敌群。</li>
 * </ul>
 */
@EventBusSubscriber(modid = "annihilationblade")
public final class NightfallDragonFinalFormLogic {
   /** 结界 Tick 运行间隔（20 Ticks = 1 秒） */
   private static final int DOMAIN_INTERVAL_TICKS = 20;               // 绝对湮灭结界的触发循环间隔（单位：Tick，20 Ticks = 1 秒）。
   
   /** 万龙剑阵索敌半径 */
   private static final double BLADE_STORM_RADIUS = 96.0;              // 万龙剑阵挥刀召唤幻影剑时的索敌检测半径（单位：格）。
   private static final int BLADE_STORM_TARGETS = 96;                  // 万龙剑阵一次索敌能够锁定的最大目标数量。
   
   /** 斩裂世界 (World Cleaving) 范围与宽带 */
   private static final double WORLD_CLEAVING_WIDTH = 5.0;             // 斩裂世界剑气波束的横向判定宽度（单位：格）。
   private static final int WORLD_CLEAVING_TARGETS = 96;               // 斩裂世界一次贯穿能够影响的最大目标数量上限。
   private static final float DOMAIN_FIXED_DAMAGE = 24.0F;             // 绝对湮灭结界每秒对结界内目标造成的固定基础伤害。
   private static final float WORLD_CLEAVING_DAMAGE = 44.0F;           // 斩裂世界剑气直接命中目标时造成的固定基础伤害。
   private static final int WORLD_CLEAVING_LIGHTNING_MAX = 96;         // 斩裂世界挥刀时沿剑气轴线轰击的最大落雷数量。
   private static final int WORLD_CLEAVING_LIGHTNING_COLOR = 0xB026FF;  // 斩裂世界落雷的渲染颜色（RGB十六进制色值，此处为亮紫罗兰色）。
   private static final double WORLD_CLEAVING_LIGHTNING_MIN_DISTANCE = 2.5; // 落雷生成点距离持刀者的最小安全距离（防止闪电在自己身上轰击遮挡视线）。
   
   /** 创世护盾额外生命上限 (200.0F) */
   private static final float MAX_CREATION_SHIELD_HEALTH = 200.0F;     // 创世护盾（吸收生命值/黄血）能通过溢出伤害转换达到的上限值（最高 200 点 HP）。

   private static double getDomainRadius() {
      return ModConfig.COMMON.nightfallDragon.absoluteDomainRadius.getValue();
   }

   private static int getDomainMaxTargets() {
      return ModConfig.COMMON.nightfallDragon.absoluteDomainMaxTargets.getValue();
   }

   private static int getBladeStormSwords() {
      return ModConfig.COMMON.nightfallDragon.bladeStormSwords.getValue();
   }

   private static double getWorldCleavingRange() {
      return ModConfig.COMMON.nightfallDragon.worldCleavingRange.getValue();
   }
   
   private static final Set<UUID> PLAYERS_WITH_FLIGHT = new HashSet<>();
   private static final Set<UUID> INTERNAL_FINAL_DAMAGE = new HashSet<>();
   private static final Map<UUID, Long> BLADE_STORM_SWORD_IDS = new HashMap<>();
   private static final Map<UUID, FrozenMobState> FROZEN_MOBS = new HashMap<>();
   private static final Map<UUID, Long> SUPPRESSED_UNTIL = new HashMap<>();

   private NightfallDragonFinalFormLogic() {
   }

   /**
    * 持刀者受击事件（最高优先级 HIGHEST）。取消伤害、全面恢复生命与状态，并将伤害反弹给敌人。
    */
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onBearerHurt(LivingHurtEvent event) {
      if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !hasFinalFormInInventory(player)) {
         return;
      }

      event.setCanceled(true); // 强行取消伤害事件
      restoreBearer(player);   // 恢复满血满饱食度
      reflectDamage(player, event.getSource(), event.getAmount()); // 反弹伤害
   }

   /**
    * 持刀者死亡事件（最高优先级 HIGHEST）。取消死亡、重置死亡倒计时并恢复满状态。
    */
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onBearerDeath(LivingDeathEvent event) {
      if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !hasFinalFormInInventory(player)) {
         return;
      }

      event.setCanceled(true); // 取消死亡事件，防止玩家死掉
      restoreBearer(player);
      player.deathTime = 0;
   }

   /**
    * 被压制实体攻击拦截与持刀者被攻击前置拦截。
    */
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onSuppressedAttack(LivingAttackEvent event) {
      if (event.getEntity().level().isClientSide) {
         return;
      }

      if (event.getEntity() instanceof Player player && hasFinalFormInInventory(player)) {
         event.setCanceled(true);
         restoreBearer(player);
         reflectDamage(player, event.getSource(), event.getAmount());
         return;
      }

      Entity attacker = event.getSource().getEntity();
      if (attacker instanceof LivingEntity living && isSuppressed(living)) {
         event.setCanceled(true);
      }
   }

   /**
    * 攻击吸血与创世护盾转换事件（最低优先级 LOWEST，在伤害最终确定后执行）。
    */
   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onFinalFormDamage(LivingHurtEvent event) {
      if (event.isCanceled() || event.getEntity().level().isClientSide) {
         return;
      }

      Entity source = event.getSource().getEntity();
      Entity directSource = event.getSource().getDirectEntity();
      if (!(source instanceof Player player) || !isFinalDamage(player, event.getSource(), directSource)) {
         return;
      }

      LivingEntity target = event.getEntity();
      if (!SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      // 将玩家造成的伤害按 1:1 转化为自身的治疗量与创世护盾
      absorbDamage(player, event.getAmount());
   }

   public static boolean isInternalFinalDamage(LivingEntity target) {
      return target != null && INTERNAL_FINAL_DAMAGE.contains(target.getUUID());
   }

   public static boolean isBladeStormSword(Entity entity, long gameTime) {
      purgeExpiredBladeStormSwords(gameTime);
      return entity instanceof EntityAbstractSummonedSword sword
         && BLADE_STORM_SWORD_IDS.getOrDefault(sword.getUUID(), Long.MIN_VALUE) > gameTime;
   }

   /**
    * 玩家 Tick 主循环。负责维持飞行、刷新防护、拉取虚空玩家以及定时触发绝对湮灭结界。
    */
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase != Phase.END || event.player.level().isClientSide) {
         return;
      }

      Player player = event.player;
      UUID playerId = player.getUUID();
      purgeSuppression(player.level().getGameTime());
      
      // 背包内如果没有终极形态武器，清理飞行和冻结数据
      if (!hasFinalFormInInventory(player)) {
         if (PLAYERS_WITH_FLIGHT.contains(playerId) || ownsFrozenMob(playerId)) {
            clearPlayer(player);
         }
         return;
      }

      ItemStack stack = NightfallDragonItemSupport.finalNightfallDragonInInventory(player);
      if (player.tickCount % DOMAIN_INTERVAL_TICKS == 0) {
         NightfallDragonDefinitions.ensureStats(stack, player.level());
      }

      // 拥有“龙神之躯”特效：持续刷新护身 Buff 与飞行、防掉虚空
      if (hasFinalEffect(stack, ModSpecialEffects.DRAGON_GOD_BODY.getId())) {
         refreshGodBody(player);
      }

      // 只有正拿着刀时才激活结界和飞行切割
      if (!isHoldingFinalForm(player)) {
         if (ownsFrozenMob(playerId)) {
            clearCombatState(player);
         }
         return;
      }

      // 触发绝对湮灭结界
      if (hasHeldFinalEffect(player, ModSpecialEffects.ABSOLUTE_ANNIHILATION_DOMAIN.getId())
         && player.tickCount % DOMAIN_INTERVAL_TICKS == 0
         && player.level() instanceof ServerLevel level) {
         activateDomain(level, player);
      }

      if (hasHeldFinalEffect(player, ModSpecialEffects.DRAGON_GOD_BODY.getId())) {
         sliceFlightPath(player);
      }
   }

   /**
    * 拔刀剑挥刀技能事件监听 (DoSlashEvent)。触发万龙剑阵或斩裂世界。
    */
   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (!(event.getUser() instanceof ServerPlayer player)) {
         return;
      }

      ISlashBladeState state = event.getSlashBladeState();
      if (state.hasSpecialEffect(ModSpecialEffects.MYRIAD_DRAGON_BLADE_STORM.getId())) {
         unleashBladeStorm(player, state);
      }

      if (state.hasSpecialEffect(ModSpecialEffects.WORLD_CLEAVING_SLASH.getId())) {
         unleashWorldCleavingSlash(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      clearPlayer(event.getEntity());
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      clearPlayer(event.getEntity());
   }

   /**
    * 龙神之躯 (Dragon God Body)：
    * 1. 消除一切负面效果（如中毒、凋零、虚弱等）。
    * 2. 赋予玩家在生存模式下的创造模式飞行能力 (mayfly)。
    * 3. 掉入虚空 (Y < -64) 时，将其瞬间传送至高空 320 格并开飞行。
    */
   private static void refreshGodBody(Player player) {
      restoreBearer(player);
      clampCreationShield(player);
      removeHarmfulEffects(player);
      if (player.level() instanceof ServerLevel level) {
         NightfallDragonFinalVisuals.spawnGodBodyAura(level, player);
      }

      // 赋予飞行权限
      if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().mayfly) {
         player.getAbilities().mayfly = true;
         PLAYERS_WITH_FLIGHT.add(player.getUUID());
         player.onUpdateAbilities();
      }

      // 虚空救起防死
      if (player.getY() < -64.0) {
         player.teleportTo(player.getX(), 320.0, player.getZ());
         player.setDeltaMovement(0.0, 0.0, 0.0);
         if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
         }
      }
   }

   /**
    * 恢复持刀者的生命值与饱食度。
    */
   private static void restoreBearer(Player player) {
      player.invulnerableTime = Math.max(player.invulnerableTime, 20);
      if (player.getHealth() < player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }

      player.getFoodData().setFoodLevel(20);
      player.getFoodData().setSaturation(20.0F);
      player.fallDistance = 0.0F;
      player.deathTime = 0;
   }

   /**
    * 清除所有负面状态（MobEffect.isBeneficial() == false）。
    */
   private static void removeHarmfulEffects(Player player) {
      List<MobEffect> harmful = new ArrayList<>();
      for (MobEffectInstance effect : player.getActiveEffects()) {
         if (!effect.getEffect().isBeneficial()) {
            harmful.add(effect.getEffect());
         }
      }

      for (MobEffect effect : harmful) {
         player.removeEffect(effect);
      }
   }

   /**
    * 伤害全额反弹：当被敌人攻击时，重置敌人无敌帧，并将相同数值的伤害直接还给攻击者。
    */
   private static void reflectDamage(Player player, DamageSource source, float amount) {
      Entity attacker = source.getEntity();
      if (!(attacker instanceof LivingEntity living) || living == player || amount <= 0.0F || !SlashBladeTargeting.canAttack(player, living)) {
         return;
      }

      suppress(living, player.level().getGameTime() + 40L);
      if (living instanceof Mob mob) {
         mob.setTarget(null);
         mob.getNavigation().stop();
      }

      living.invulnerableTime = 0;
      hurtInternally(player, living, amount); // 反弹伤害
      if (living.level() instanceof ServerLevel level) {
         NightfallDragonFinalVisuals.spawnExecutionBurst(level, living);
      }
   }

   /**
    * 吸血与创世黄血护盾算法：
    * 先补满缺失生命值，若有溢出部分，将其累加为黄血护盾（最高 200 点）。
    */
   private static void absorbDamage(Player player, float amount) {
      if (amount <= 0.0F || player.level().isClientSide) {
         return;
      }

      float missing = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
      if (amount <= missing) {
         player.heal(amount); // 补血
         return;
      }

      if (missing > 0.0F) {
         player.setHealth(player.getMaxHealth());
      }

      float overflow = amount - missing;
      if (overflow > 0.0F) {
         grantCreationShield(player, overflow); // 溢出转护盾
      }
   }

   private static void grantCreationShield(Player player, float amount) {
      if (amount <= 0.0F) {
         return;
      }

      player.setAbsorptionAmount(Math.min(MAX_CREATION_SHIELD_HEALTH, player.getAbsorptionAmount() + amount));
   }

   private static void clampCreationShield(Player player) {
      if (player.getAbsorptionAmount() > MAX_CREATION_SHIELD_HEALTH) {
         player.setAbsorptionAmount(MAX_CREATION_SHIELD_HEALTH);
      }
   }

   /**
    * 绝对湮灭结界：
    * 剥夺全场药水与黄血护盾、锁死 AI 并造成百分比生命上限伤害。对于 HP < 50% 的生物直接执行绝对抹杀！
    */
   private static void activateDomain(ServerLevel level, Player player) {
      Vec3 center = player.position();
      NightfallDragonFinalVisuals.spawnDomainFrame(level, center, player.tickCount);
      List<LivingEntity> targets = SpecialEffectSupport.limit(SpecialEffectSupport.radialTargets(level, player, center, getDomainRadius()), getDomainMaxTargets());
      Set<UUID> activeMobs = new HashSet<>();
      for (LivingEntity target : targets) {
         stripAndFreeze(level, player, target, activeMobs);
         float damage = Math.max(DOMAIN_FIXED_DAMAGE, target.getHealth() * 0.25F + DOMAIN_FIXED_DAMAGE);
         hurtAsDragon(player, target, damage);
         executeIfBelowHalf(player, target); // 低于半血直接斩杀
      }

      releaseMissingMobs(player, activeMobs);
   }

   private static void stripAndFreeze(ServerLevel level, Player player, LivingEntity target, Set<UUID> activeMobs) {
      target.invulnerableTime = 0;
      target.setAbsorptionAmount(0.0F); // 清空黄血
      target.removeAllEffects();        // 移除 Buff
      target.stopRiding();
      target.ejectPassengers();
      target.setDeltaMovement(Vec3.ZERO);
      target.fallDistance = 0.0F;
      target.hasImpulse = true;
      suppress(target, level.getGameTime() + 30L);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 255, false, false, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 255, false, false, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 255, false, false, true));
      if (target instanceof Mob mob) {
         freezeMob(mob, player);
         activeMobs.add(mob.getUUID());
      }
   }

   /**
    * 万龙剑阵：在玩家周身圆环轨道上生成幻影剑 (EntityAbstractSummonedSword)，并自动面向/射向敌人。
    */
   private static void unleashBladeStorm(ServerPlayer player, ISlashBladeState state) {
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.66, 0.0);
      Vec3 direction = safe(player.getLookAngle(), new Vec3(0.0, 0.0, 1.0));
      List<LivingEntity> targets = SpecialEffectSupport.limit(SpecialEffectSupport.radialTargets(level, player, center, BLADE_STORM_RADIUS), BLADE_STORM_TARGETS);
      
      int swordCount = getBladeStormSwords();
      for (int i = 0; i < swordCount; i++) {
         LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
         spawnDragonBlade(level, player, center, direction, target, i, state.getColorCode());
      }

      NightfallDragonFinalVisuals.spawnPiercingBladeWave(level, player, center.add(direction.scale(1.0)), center.add(direction.scale(24.0)));
      level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.4F, 0.52F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 0.5F);
   }

   /**
    * 实例化单柄幻影剑，计算其环绕轨道坐标与指向三维朝向。
    */
   private static void spawnDragonBlade(ServerLevel level, ServerPlayer player, Vec3 center, Vec3 direction, LivingEntity target, int index, int color) {
      // 三角函数计算圆环轨道坐标 (orbit)
      double angle = (Math.PI * 2.0) * index / Math.max(1, getBladeStormSwords()) + player.tickCount * 0.4;
      double radius = 2.8 + index % 4 * 0.28;
      Vec3 orbit = center.add(Math.cos(angle) * radius, 0.5 + Math.sin(angle * 2.0) * 0.85, Math.sin(angle) * radius);
      
      // 计算指向目标的三维方向向量
      Vec3 aim = target != null ? SpecialEffectSupport.centerOf(target).subtract(orbit) : direction;
      if (aim.lengthSqr() < 1.0E-6) {
         aim = direction;
      }

      Vec3 forward = aim.normalize();
      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(player);
      sword.setShooter(player);
      sword.setColor(color != 0 ? color : NightfallDragonDefinitions.FINAL_SUMMONED_SWORD_COLOR);
      sword.setDamage(0.0);
      sword.setNoClip(true);
      sword.setPierce((byte)0);
      sword.setDelay(index % 12);
      sword.setPos(orbit.x, orbit.y, orbit.z);
      
      // 使用 yawToFace / pitchToFace 将方向向量转为欧拉角 angles，传给 moveTo 设置旋转
      sword.moveTo(orbit.x, orbit.y, orbit.z, yawToFace(forward), pitchToFace(forward));
      sword.shoot(forward.x, forward.y, forward.z, 4.15F, 0.0F);
      level.addFreshEntity(sword);
      BLADE_STORM_SWORD_IDS.put(sword.getUUID(), level.getGameTime() + 200L);
      NightfallDragonFinalVisuals.spawnBladeOrbitBurst(level, index % 2 == 0 ? net.minecraft.core.particles.ParticleTypes.END_ROD : net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, orbit, 12);
   }

   /**
    * 斩裂世界 (World Cleaving Slash)：向前穿透光束，沿途将所有敌人拉扯 (pullToward) 至斩击线上并轰击紫色雷电。
    */
   private static void unleashWorldCleavingSlash(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 start = player.getEyePosition().add(0.0, -0.25, 0.0);
      Vec3 forward = safe(player.getLookAngle(), new Vec3(0.0, 0.0, 1.0));
      double worldCleavingRange = getWorldCleavingRange();
      Vec3 end = start.add(forward.scale(worldCleavingRange));
      NightfallDragonFinalVisuals.spawnPiercingBladeWave(level, player, start, end);
      
      // 射线扫描宽束内的所有目标 (beamTargets)
      List<LivingEntity> targets = SpecialEffectSupport.beamTargets(level, player, start, forward, worldCleavingRange, WORLD_CLEAVING_WIDTH, WORLD_CLEAVING_TARGETS);
      int lightningCount = 0;
      for (LivingEntity target : targets) {
         Vec3 targetCenter = SpecialEffectSupport.centerOf(target);
         // 点积投影运算：计算目标点在射线线段上的最近投影点
         double projection = Math.max(0.0, Math.min(worldCleavingRange, targetCenter.subtract(start).dot(forward)));
         Vec3 slashCenter = start.add(forward.scale(projection));
         
         // 空间吸引：强行将目标拉向斩击轴线中心
         SpecialEffectSupport.pullToward(target, slashCenter, 1.15);
         suppress(target, level.getGameTime() + 30L);
         target.invulnerableTime = 0;
         
         // 生成落雷
         if (lightningCount < WORLD_CLEAVING_LIGHTNING_MAX) {
            Vec3 boltCenter = SpecialEffectSupport.centerOf(target);
            if (boltCenter.distanceToSqr(player.position()) >= WORLD_CLEAVING_LIGHTNING_MIN_DISTANCE * WORLD_CLEAVING_LIGHTNING_MIN_DISTANCE) {
               GammaThunderburstLogic.spawnBolt(level, boltCenter, WORLD_CLEAVING_LIGHTNING_COLOR);
               lightningCount++;
            }
         }

         hurtAsDragon(player, target, WORLD_CLEAVING_DAMAGE + (float)(target.getMaxHealth() * 0.08F));
         executeIfBelowHalf(player, target);
      }
   }

   /**
    * 飞行轨迹切割：当玩家高速度飞行时，周身 5 格内的敌人会被拉扯并受到切割伤害。
    */
   private static void sliceFlightPath(Player player) {
      if (player.tickCount % 5 != 0 || player.getDeltaMovement().lengthSqr() < 0.04 || !(player.level() instanceof ServerLevel level)) {
         return;
      }

      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      List<LivingEntity> targets = SpecialEffectSupport.limit(SpecialEffectSupport.radialTargets(level, player, center, 5.0), 24);
      for (LivingEntity target : targets) {
         target.invulnerableTime = 0;
         SpecialEffectSupport.pullToward(target, center, 0.8);
         hurtAsDragon(player, target, 16.0F + (float)(target.getMaxHealth() * 0.04F));
      }
   }

   private static void hurtAsDragon(Player player, LivingEntity target, float amount) {
      if (!target.isAlive() || amount <= 0.0F || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      target.invulnerableTime = 0;
      hurtInternally(player, target, amount);
   }

   private static void hurtInternally(Player player, LivingEntity target, float amount) {
      if (amount <= 0.0F || target.level().isClientSide) {
         return;
      }

      INTERNAL_FINAL_DAMAGE.add(target.getUUID());
      try {
         target.hurt(target.level().damageSources().indirectMagic(player, player), amount);
      } finally {
         INTERNAL_FINAL_DAMAGE.remove(target.getUUID());
      }
   }

   private static void purgeExpiredBladeStormSwords(long gameTime) {
      if (gameTime % 200 != 0) {
         return;
      }

      BLADE_STORM_SWORD_IDS.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
   }

   /**
    * 生命值低于 50% 的生物触发终极斩杀（绝对抹杀）。
    */
   private static void executeIfBelowHalf(Player player, LivingEntity target) {
      if (target.isAlive() && target.getHealth() <= target.getMaxHealth() * 0.5F && SlashBladeTargeting.canAttack(player, target)) {
         if (target.level() instanceof ServerLevel level) {
            NightfallDragonFinalVisuals.spawnExecutionBurst(level, target);
         }

         EntropyDissolutionLogic.executeFinal(target, player);
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

   private static void releaseMissingMobs(Player player, Set<UUID> activeMobIds) {
      MinecraftServer server = player.getServer();
      if (server == null || FROZEN_MOBS.isEmpty()) {
         return;
      }

      UUID playerId = player.getUUID();
      Iterator<Map.Entry<UUID, FrozenMobState>> iterator = FROZEN_MOBS.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<UUID, FrozenMobState> entry = iterator.next();
         if (activeMobIds.contains(entry.getKey())) {
            continue;
         }

         FrozenMobState state = entry.getValue();
         if (!state.owners.remove(playerId)) {
            continue;
         }

         if (state.owners.isEmpty()) {
            restoreMob(server, entry.getKey(), state);
            iterator.remove();
         }
      }
   }

   private static void clearPlayer(Player player) {
      clearCombatState(player);
      clampCreationShield(player);
      if (!player.isCreative() && !player.isSpectator() && PLAYERS_WITH_FLIGHT.remove(player.getUUID())) {
         player.getAbilities().mayfly = false;
         player.getAbilities().flying = false;
         player.onUpdateAbilities();
      }
   }

   private static void clearCombatState(Player player) {
      releasePlayer(player);
   }

   private static void releasePlayer(Player player) {
      MinecraftServer server = player.getServer();
      if (server == null || FROZEN_MOBS.isEmpty()) {
         return;
      }

      UUID playerId = player.getUUID();
      Iterator<Map.Entry<UUID, FrozenMobState>> iterator = FROZEN_MOBS.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<UUID, FrozenMobState> entry = iterator.next();
         FrozenMobState state = entry.getValue();
         if (!state.owners.remove(playerId)) {
            continue;
         }

         if (state.owners.isEmpty()) {
            restoreMob(server, entry.getKey(), state);
            iterator.remove();
         }
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

   private static boolean ownsFrozenMob(UUID playerId) {
      for (FrozenMobState state : FROZEN_MOBS.values()) {
         if (state.owners.contains(playerId)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isFinalDamage(Player player, DamageSource source, Entity directSource) {
      return isHoldingFinalForm(player)
         && (NightfallDragonItemSupport.isDirectNightfallAttack(player, source)
            || directSource instanceof EntityAbstractSummonedSword
            || NightfallDragonItemSupport.isNightfallSlashEntityAttack(player, directSource));
   }

   private static boolean isHoldingFinalForm(Player player) {
      ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
      return isFinalStack(stack);
   }

   private static boolean hasFinalFormInInventory(Player player) {
      return NightfallDragonItemSupport.hasFinalNightfallDragonInInventory(player);
   }

   private static boolean isFinalStack(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof ItemSlashBlade && NightfallDragonDefinitions.isFinal(stack);
   }

   private static boolean hasHeldFinalEffect(Player player, ResourceLocation effectId) {
      ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
      return hasFinalEffect(stack, effectId);
   }

   private static boolean hasFinalEffect(ItemStack stack, ResourceLocation effectId) {
      return isFinalStack(stack)
         && stack.getCapability(ItemSlashBlade.BLADESTATE)
            .map(state -> state.hasSpecialEffect(effectId))
            .orElse(false);
   }

   private static void suppress(LivingEntity target, long until) {
      SUPPRESSED_UNTIL.put(target.getUUID(), until);
   }

   private static boolean isSuppressed(LivingEntity entity) {
      Long until = SUPPRESSED_UNTIL.get(entity.getUUID());
      return until != null && until > entity.level().getGameTime();
   }

   private static void purgeSuppression(long gameTime) {
      Iterator<Map.Entry<UUID, Long>> iterator = SUPPRESSED_UNTIL.entrySet().iterator();
      while (iterator.hasNext()) {
         if (iterator.next().getValue() <= gameTime) {
            iterator.remove();
         }
      }
   }

   private static Vec3 safe(Vec3 vector, Vec3 fallback) {
      return vector.lengthSqr() < 1.0E-6 ? fallback.normalize() : vector.normalize();
   }

   /**
    * 三维向量转 Yaw 偏航角公式：
    * Yaw = atan2(x, z) * (180 / π)
    */
   private static float yawToFace(Vec3 direction) {
      return (float)(Mth.atan2(direction.x, direction.z) * 180.0F / (float)Math.PI);
   }

   /**
    * 三维向量转 Pitch 俯仰角公式：
    * Pitch = -atan2(y, sqrt(x^2 + z^2)) * (180 / π)
    */
   private static float pitchToFace(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float)(-Mth.atan2(direction.y, horizontal) * 180.0F / (float)Math.PI);
   }

   private static final class FrozenMobState {
      private final boolean originalNoAi;
      private final Set<UUID> owners = new HashSet<>();

      private FrozenMobState(boolean originalNoAi) {
         this.originalNoAi = originalNoAi;
      }
   }
}

