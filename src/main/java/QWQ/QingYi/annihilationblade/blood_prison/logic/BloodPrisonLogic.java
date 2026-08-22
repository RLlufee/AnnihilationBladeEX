package QWQ.QingYi.annihilationblade.blood_prison.logic;

import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationVisuals;
import QWQ.QingYi.annihilationblade.blood_prison.BloodPrisonDefinitions;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.network.ModNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * <h1>血狱 (Blood Prison) 核心逻辑类</h1>
 * <p>
 * 本类展示了基于“献祭生命、越伤越强”理念的血祭流武器实现，包含以下核心技术：
 * <ul>
 *   <li><b>动态属性修改器 (AttributeModifier)</b>：利用 Forge/Vanilla 的 Attribute API，给持有者动态添加/移除 +40 MAX_HEALTH 临时生命上限。</li>
 *   <li><b>卖血机制 (drainBloodForSwing)</b>：玩家挥刀攻击时强行扣除自创生命值（保底留 8 HP），转化为额外攻击力。</li>
 *   <li><b>残血血盾 (updateShield)</b>：当玩家 HP 跌至 8 点以下时触发护盾，赋予黄血与抗性、再生、加速Buff。</li>
 *   <li><b>血狱结界与闪击 (Domain Attack)</b>：展开结界并同步数据包给客户端，结界内挥刀时随机瞬移至敌人身后进行破空斩击。</li>
 *   <li><b>十印幻影剑引爆 (spawnPhantomSwordBurst)</b>：攻击同一目标 10 次后，在其四周生成环形幻影剑阵并发射爆破。</li>
 * </ul>
 */
@EventBusSubscriber(modid = "annihilationblade")
public final class BloodPrisonLogic {
   public static final String BLOOD_PRISON_TRANSLATION_KEY = "item.annihilationblade.blood_prison";
   
   /** 动态给玩家追加生命上限属性的唯一 UUID 标识符 */
   private static final UUID MAX_HEALTH_MODIFIER_ID = UUID.fromString("4f4c5a3b-70f1-43ee-b65d-74f4e1c1c95d");
   
   /** 装备血狱刀获得的额外基础生命上限 (+40.0 HP = 20颗心) */
   private static final double MAX_HEALTH_BONUS = 40.0;
   
   /** 卖血机制保留的最低血量安全阀 (8.0 HP = 4颗心) */
   private static final float MIN_HEALTH = 8.0F;
   
   /** 残血护盾触发后的 Buff 持续时间 (100 Ticks = 5 秒) */
   private static final int SHIELD_BUFF_TICKS = 100;
   private static final int INVENTORY_SCAN_INTERVAL = 10;
   
   private static final Map<UUID, BloodPrisonLogic.DrainWindow> DRAIN_WINDOWS = new HashMap<>();
   private static final Map<UUID, Float> BLOOD_SHIELDS = new HashMap<>();
   private static final Map<UUID, Map<UUID, Integer>> MARKS = new HashMap<>();
   private static final Map<UUID, BloodPrisonLogic.Domain> DOMAINS = new HashMap<>();
   private static final Set<UUID> PHANTOM_BURST_TARGETS = new HashSet<>();
   private static final Map<UUID, Boolean> HAS_BLOOD_PRISON_CACHE = new HashMap<>();
   private static final Map<UUID, Integer> LAST_BLOOD_PRISON_SCAN_TICK = new HashMap<>();

   private BloodPrisonLogic() {
   }

   /**
    * 判断物品堆是否为血狱刀。
    */
   public static boolean isBloodPrison(ItemStack stack) {
      return !stack.isEmpty() && "item.annihilationblade.blood_prison".equals(stack.getDescriptionId());
   }

   /**
    * 检查玩家背包或手持中是否存在血狱刀（带 10 Tick 扫描缓存以提升性能）。
    */
   public static boolean hasBloodPrison(Player player) {
      if (!isBloodPrison(player.getMainHandItem()) && !isBloodPrison(player.getOffhandItem())) {
         UUID id = player.getUUID();
         Integer lastScan = LAST_BLOOD_PRISON_SCAN_TICK.get(id);
         // 10 Ticks 内直接取缓存结果
         if (lastScan != null && player.tickCount - lastScan < INVENTORY_SCAN_INTERVAL) {
            return HAS_BLOOD_PRISON_CACHE.getOrDefault(id, false);
         }

         // 遍历背包面板
         for (ItemStack stack : player.getInventory().items) {
            if (isBloodPrison(stack)) {
               cacheBloodPrisonState(player, true);
               return true;
            }
         }

         cacheBloodPrisonState(player, false);
         return false;
      } else {
         cacheBloodPrisonState(player, true);
         return true;
      }
   }

   public static boolean isPhantomBurstDamage(LivingEntity target) {
      return target != null && PHANTOM_BURST_TARGETS.contains(target.getUUID());
   }

   private static void cacheBloodPrisonState(Player player, boolean hasBloodPrison) {
      UUID id = player.getUUID();
      HAS_BLOOD_PRISON_CACHE.put(id, hasBloodPrison);
      LAST_BLOOD_PRISON_SCAN_TICK.put(id, player.tickCount);
   }

   private static void refreshBloodPrisonStats(Player player) {
      if (isBloodPrison(player.getMainHandItem())) {
         BloodPrisonDefinitions.ensureStats(player.getMainHandItem(), player.level());
      }

      if (isBloodPrison(player.getOffhandItem())) {
         BloodPrisonDefinitions.ensureStats(player.getOffhandItem(), player.level());
      }

      for (ItemStack stack : player.getInventory().items) {
         if (isBloodPrison(stack)) {
            BloodPrisonDefinitions.ensureStats(stack, player.level());
         }
      }
   }

   /**
    * 开启血狱结界：生成红色烟雾与龙息粒子、向客户端发送网络数据包并播放音效。
    */
   public static void activateDomain(Player player) {
      if (player.level() instanceof ServerLevel level && isBloodPrison(player.getMainHandItem())) {
         ModConfig.Domain config = ModConfig.COMMON.bloodPrison.domain;
         int durationTicks = config.durationTicks.getValue();
         double radius = config.radius.getValue();
         double visualScale = config.visualScale.getValue();
         
         BloodPrisonDefinitions.ensureStats(player.getMainHandItem(), level);
         DOMAINS.put(player.getUUID(),
               new BloodPrisonLogic.Domain(player.position(), level.getGameTime() + durationTicks));
         
         // 给客户端发送自定义包，同步结界圈渲染
         if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendBloodPrisonDomain(serverPlayer, durationTicks);
         }

         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 0.55F);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.55F, 1.45F);
         level.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 0.15, player.getZ(), visualCount(120, visualScale), radius * 0.3, 0.15 * visualScale, radius * 0.3, 0.03);
         level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 0.2, player.getZ(), visualCount(180, visualScale), radius * 0.8, 0.25 * visualScale, radius * 0.8, 0.08);
         AnnihilationVisuals.spawnBloodPrisonDomainPulse(level, player.position(), radius * visualScale);
      }
   }

   /**
    * 玩家 Tick 事件：负责维护最大血量上限修改器、更新残血护盾以及维持结界计时。
    */
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide) {
         Player player = event.player;
         boolean hasBloodPrison = hasBloodPrison(player);
         if (hasBloodPrison && player.tickCount % 20 == 0) {
            refreshBloodPrisonStats(player);
         }

         updateMaxHealth(player, hasBloodPrison);
         updateShield(player, hasBloodPrison);
         tickDomain(player);
      }
   }

   /**
    * 挥刀攻击时触发献祭卖血逻辑。
    */
   @SubscribeEvent
   public static void onAttack(LivingAttackEvent event) {
      if (event.getSource().getEntity() instanceof Player player && isBloodPrison(player.getMainHandItem())) {
         if (SlashBladeTargeting.canAttack(player, event.getEntity())) {
            drainBloodForSwing(player);
         }
      }
   }

   /**
    * 伤害结算：已损失血量越多，额外伤害加成越高；且攻击 10 次后在目标处引发幻影剑爆破。
    */
   @SubscribeEvent
   public static void onHurt(LivingHurtEvent event) {
      Player player = bloodPrisonDirectAttacker(event.getSource());
      if (player == null) {
         return;
      }

      LivingEntity target = event.getEntity();
      if (!SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      // 计算已损失血量，并按比例增伤
      float missing = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
      float damageBonus = (float) Math.floor(missing / 2.0F) * (player.getMaxHealth() * 0.001F);
      event.setAmount(event.getAmount() + damageBonus);

      BloodPrisonLogic.Domain domain = DOMAINS.get(player.getUUID());
      if (domain != null) {
         domain.damageDealt = domain.damageDealt + event.getAmount();
      }

      // 攻击印记累加：达到 10 印记触发幻影剑爆破 (spawnPhantomSwordBurst)
      if (!PHANTOM_BURST_TARGETS.contains(target.getUUID())) {
         Map<UUID, Integer> playerMarks = MARKS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
         int marks = playerMarks.getOrDefault(target.getUUID(), 0) + 1;
         if (marks < 10) {
            playerMarks.put(target.getUUID(), marks);
         } else {
            playerMarks.remove(target.getUUID());
            float burstDamage = target.getMaxHealth() * 0.05F;
            UUID targetId = target.getUUID();
            PHANTOM_BURST_TARGETS.add(targetId);

            try {
               target.hurt(target.level().damageSources().indirectMagic(player, player), burstDamage);
               if (target.level() instanceof ServerLevel level) {
                  spawnPhantomSwordBurst(level, player, target);
               }
            } finally {
               PHANTOM_BURST_TARGETS.remove(targetId);
            }
         }
      }
   }

   private static Player bloodPrisonDirectAttacker(DamageSource source) {
      return source.getEntity() instanceof Player player
         && source.getDirectEntity() == player
         && isBloodPrison(player.getMainHandItem())
            ? player
            : null;
   }

   /**
    * 击杀目标后吸血恢复；玩家死亡则移除结界。
    */
   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof Player player) {
         removeDomain(player);
      } else {
         if (event.getSource().getEntity() instanceof Player player && isBloodPrison(player.getMainHandItem())) {
            player.heal(event.getEntity().getMaxHealth() * 0.1F); // 击杀恢复 10% 最大生命值
         }
      }
   }

   /**
    * 结界内挥刀触发闪现连击。
    */
   @SubscribeEvent
   public static void onSlash(DoSlashEvent event) {
      if (event.getUser() instanceof Player player && DOMAINS.containsKey(player.getUUID())) {
         performDomainAttack(player);
      }
   }

   /**
    * 使用 Vanilla Attributes 给玩家增加/移除 +40.0 MAX_HEALTH 临时属性修改器。
    */
   private static void updateMaxHealth(Player player, boolean hasBloodPrison) {
      AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
      if (attribute != null) {
         AttributeModifier modifier = attribute.getModifier(MAX_HEALTH_MODIFIER_ID);
         if (hasBloodPrison) {
            if (modifier == null) {
               // 添加属性修改器 (Operation.ADDITION)
               attribute.addTransientModifier(new AttributeModifier(MAX_HEALTH_MODIFIER_ID, "blood_prison_max_health", 40.0, Operation.ADDITION));
            }
         } else if (modifier != null) {
            attribute.removeModifier(MAX_HEALTH_MODIFIER_ID); // 移除修改器
         }
      }
   }

   /**
    * 当玩家生命值 <= 8.0 HP (残血) 时，自动赋予血之护盾与四种狂暴 Buff。
    */
   private static void updateShield(Player player, boolean hasBloodPrison) {
      UUID id = player.getUUID();
      float shield = player.getMaxHealth() * 0.2F;
      if (hasBloodPrison && player.getHealth() <= 8.0F) {
         boolean newlyTriggered = !BLOOD_SHIELDS.containsKey(id);
         player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), shield));
         BLOOD_SHIELDS.put(id, shield);
         if (newlyTriggered) {
            applyShieldBuffs(player);
         }
      } else if (player.getHealth() > 20.0F && BLOOD_SHIELDS.remove(id) != null
            && player.getAbsorptionAmount() <= shield + 0.01F) {
         player.setAbsorptionAmount(0.0F);
      }
   }

   /**
    * 触发残血保护：给予抗性提升、生命恢复、移动加速与饱食度恢复 Buff。
    */
   private static void applyShieldBuffs(Player player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 1, false, true, true));
   }

   /**
    * 每次挥刀扣除自身生命值，最大扣除 4 点 HP，但绝不扣到 8.0 点以下。
    */
   private static void drainBloodForSwing(Player player) {
      long time = player.level().getGameTime();
      BloodPrisonLogic.DrainWindow window = DRAIN_WINDOWS.computeIfAbsent(player.getUUID(), ignored -> new BloodPrisonLogic.DrainWindow(time));
      if (time - window.startedAt >= 10L) {
         window.startedAt = time;
         window.drained = 0.0F;
      }

      float amount = Math.min(4.0F, 10.0F - window.drained);
      float actual = Math.min(amount, Math.max(0.0F, player.getHealth() - 8.0F));
      if (actual > 0.0F) {
         player.setHealth(player.getHealth() - actual);
         window.drained += actual;
      }
   }

   /**
    * 结界帧动画：生成红紫交错的边界粒子圈与脉冲波。
    */
   private static void tickDomain(Player player) {
      BloodPrisonLogic.Domain domain = DOMAINS.get(player.getUUID());
      if (domain != null) {
         ServerLevel level = (ServerLevel) player.level();
         if (!player.isAlive()) {
            removeDomain(player);
         } else if (level.getGameTime() >= domain.expiresAt) {
            // 结界正常结束，将结界内造成的总伤害 20% 反哺治疗给玩家
            player.heal(domain.damageDealt * 0.2F);
            removeDomain(player);
         } else {
            ModConfig.Domain config = ModConfig.COMMON.bloodPrison.domain;
            double radius = config.radius.getValue();
            double visualScale = config.visualScale.getValue();
            if (level.getGameTime() % config.borderIntervalTicks.getValue() == 0L) {
               for (double offset = -radius; offset <= radius; offset += 2.0) {
                  level.sendParticles(ParticleTypes.DRAGON_BREATH, domain.center.x + offset, domain.center.y + 0.1, domain.center.z - radius, 1, 0.0, 0.0, 0.0, 0.0);
                  level.sendParticles(ParticleTypes.DRAGON_BREATH, domain.center.x + offset, domain.center.y + 0.1, domain.center.z + radius, 1, 0.0, 0.0, 0.0, 0.0);
                  level.sendParticles(ParticleTypes.DRAGON_BREATH, domain.center.x - radius, domain.center.y + 0.1, domain.center.z + offset, 1, 0.0, 0.0, 0.0, 0.0);
                  level.sendParticles(ParticleTypes.DRAGON_BREATH, domain.center.x + radius, domain.center.y + 0.1, domain.center.z + offset, 1, 0.0, 0.0, 0.0, 0.0);
               }

               level.sendParticles(ParticleTypes.CRIMSON_SPORE, domain.center.x, domain.center.y + 0.15, domain.center.z, visualCount(18, visualScale), radius * 0.65, 0.05 * visualScale, radius * 0.65, 0.015);
            }

            if (level.getGameTime() % config.playerAuraIntervalTicks.getValue() == 0L) {
               level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0, player.getZ(), visualCount(8, visualScale), 1.2 * visualScale, 0.5 * visualScale, 1.2 * visualScale, 0.05);
            }

            if (level.getGameTime() % config.pulseIntervalTicks.getValue() == 0L) {
               AnnihilationVisuals.spawnBloodPrisonDomainPulse(level, domain.center, radius * visualScale);
            }
         }
      }
   }

   /**
    * 结界内挥刀闪击：随机选取结界内一个敌人，瞬间传送至其身后 1.5 格处并进行扫荡斩击。
    */
   private static void performDomainAttack(Player player) {
      BloodPrisonLogic.Domain domain = DOMAINS.get(player.getUUID());
      if (domain != null && player.level() instanceof ServerLevel level) {
         double radius = ModConfig.COMMON.bloodPrison.domain.radius.getValue();
         List<LivingEntity> targets = new ArrayList<>(
               level.getEntitiesOfClass(
                     LivingEntity.class, new AABB(domain.center, domain.center).inflate(radius),
                     targetx -> SlashBladeTargeting.canAttack(player, targetx)));
         if (!targets.isEmpty()) {
            LivingEntity target = targets.get(player.getRandom().nextInt(targets.size()));
            Vec3 from = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
            
            // 极坐标随机角度偏移
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            // 瞬间传送至敌人身后 1.5 格
            player.teleportTo(target.getX() + Math.cos(angle) * 1.5, target.getY(), target.getZ() + Math.sin(angle) * 1.5);
            
            target.hurt(level.damageSources().indirectMagic(player, player), 20.0F);
            domain.damageDealt += 20.0F;
            
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.75F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 3, 0.45, 0.45, 0.45, 0.0);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 24, 0.75, 0.75, 0.75, 0.12);
            Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
            AnnihilationVisuals.spawnBloodPrisonDash(level, from, hit, player.getRandom());
         }
      }
   }

   /**
    * 满 10 印记引发的环形幻影剑冲击波。
    */
   private static void spawnPhantomSwordBurst(ServerLevel level, Player player, LivingEntity target) {
      ModConfig.PhantomBurst config = ModConfig.COMMON.bloodPrison.phantomBurst;
      int swordCount = config.swordCount.getValue();
      double visualScale = config.visualScale.getValue();
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);

      // 围绕目标 360 度圆环发射剑阵
      for (int i = 0; i < swordCount; i++) {
         double angle = (Math.PI * 2) * i / swordCount;
         Vec3 start = center.add(Math.cos(angle) * 4.0 * visualScale, (3.0 + i % 3) * visualScale, Math.sin(angle) * 4.0 * visualScale);
         AnnihilationVisuals.spawnSlashBridge(level, start, center, 0.55 * visualScale, player.getRandom());
         spawnPhantomSword(level, player, start, center, i);
      }

      AnnihilationVisuals.spawnBloodPrisonBurst(level, center, Math.max(1.2, target.getBbWidth() * config.burstRadiusScale.getValue()) * visualScale, player.getRandom());
      level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.75F, 1.7F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.55F);
   }

   private static void spawnPhantomSword(ServerLevel level, Player player, Vec3 start, Vec3 end, int index) {
      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(player);
      sword.setShooter(player);
      sword.setColor(-57312);
      sword.setDamage(0.0);
      sword.setNoClip(true);
      sword.setPierce((byte) 0);
      sword.setDelay(ModConfig.COMMON.bloodPrison.phantomBurst.swordDelayTicks.getValue() + index % 4);
      sword.setRoll(index * 36.0F);
      Vec3 direction = end.subtract(start).normalize();
      sword.setPos(start.x, start.y, start.z);
      sword.moveTo(start.x, start.y, start.z, yawToFace(direction), pitchToFace(direction));
      sword.shoot(direction.x, direction.y, direction.z, 2.45F, 0.0F);
      level.addFreshEntity(sword);
   }

   private static void removeDomain(Player player) {
      if (DOMAINS.remove(player.getUUID()) != null && player instanceof ServerPlayer serverPlayer) {
         ModNetwork.sendBloodPrisonDomain(serverPlayer, 0);
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      clearPlayerState(event.getEntity());
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      clearPlayerState(event.getEntity());
   }

   private static void clearPlayerState(Player player) {
      UUID id = player.getUUID();
      HAS_BLOOD_PRISON_CACHE.remove(id);
      LAST_BLOOD_PRISON_SCAN_TICK.remove(id);
      BLOOD_SHIELDS.remove(id);
      DRAIN_WINDOWS.remove(id);
      MARKS.remove(id);
      removeDomain(player);
   }

   private static float yawToFace(Vec3 direction) {
      return (float) (Mth.atan2(direction.x, direction.z) * 180.0F / (float) Math.PI);
   }

   private static float pitchToFace(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float) (-Mth.atan2(direction.y, horizontal) * 180.0F / (float) Math.PI);
   }

   private static int visualCount(int base, double visualScale) {
      return Math.max(1, (int) Math.round(base * visualScale));
   }

   private static final class Domain {
      private final Vec3 center;
      private final long expiresAt;
      private float damageDealt;

      private Domain(Vec3 center, long expiresAt) {
         this.center = center;
         this.expiresAt = expiresAt;
      }
   }

   private static final class DrainWindow {
      private long startedAt;
      private float drained;

      private DrainWindow(long startedAt) {
         this.startedAt = startedAt;
      }
   }
}

