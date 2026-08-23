package QWQ.QingYi.annihilationbladeex.nightfall_dragon.specialeffect;

import QWQ.QingYi.annihilationbladeex.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationbladeex.annihilation_blade.visual.AnnihilationVisuals;
import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic.NightfallDragonFinalFormLogic;
import QWQ.QingYi.annihilationbladeex.registry.ModSpecialEffects;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mods.flammpfeil.slashblade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationbladeex")
public class DemonicBloodParasite extends SpecialEffect {
   private static final int EFFECT_TICKS = 120;
   private static final float PHANTOM_BURST_DAMAGE_PERCENT = 0.05F;

   private static double getExtraDamagePercent() {
      return ModConfig.COMMON.nightfallDragon.demonicBloodExtraDamagePercent.getValue();
   }

   private static int getPhantomBurstMarks() {
      return ModConfig.COMMON.nightfallDragon.demonicBloodPhantomBurstMarks.getValue();
   }

   private static final int VISUAL_COOLDOWN_TICKS = 10;
   private static final int MAX_VISUAL_BURSTS_PER_LEVEL_TICK = 3;
   private static final int MAX_VISUAL_PARTICLES_PER_LEVEL_TICK = 90;
   private static final Map<UUID, Map<UUID, Integer>> BLOOD_MARKS = new HashMap<>();
   private static final Set<UUID> INTERNAL_DAMAGE = new java.util.HashSet<>();
   private static final Map<UUID, Long> PHANTOM_BURST_SWORDS = new HashMap<>();
   private static final Map<UUID, Long> TARGET_VISUAL_COOLDOWNS = new HashMap<>();
   private static final Map<ResourceKey<Level>, VisualBudget> VISUAL_BUDGETS = new HashMap<>();

   public DemonicBloodParasite() {
      super(0, false, false);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onHurt(LivingDamageEvent.Pre event) {
      LivingEntity target = event.getEntity();
      if (target.level().isClientSide || isInternalDamage(target) || NightfallDragonFinalFormLogic.isInternalFinalDamage(target)) {
         return;
      }

      Player player = findNightfallAttacker(event);
      if (player == null || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      if (hasMark(player, target)) {
         convertToTrueDamage(event, player, target);
      }

      int marks = addMark(player, target);
      target.addEffect(new MobEffectInstance(MobEffects.WITHER, EFFECT_TICKS, Math.min(marks, 5) - 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, false, true, true));
      if (target.level() instanceof ServerLevel level) {
         spawnAbyssalFlame(level, target, marks);
         triggerPhantomBurstIfReady(level, player, target, marks);
      }

      hurtInternally(target, player, (float)(target.getMaxHealth() * getExtraDamagePercent()));
   }

   public static void applySummonedSwordMark(Player player, LivingEntity target) {
      if (target.level().isClientSide || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      int marks = addMark(player, target);
      target.addEffect(new MobEffectInstance(MobEffects.WITHER, EFFECT_TICKS, Math.min(marks, 5) - 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, false, true, true));
      if (target.level() instanceof ServerLevel level) {
         spawnAbyssalFlame(level, target, marks);
         triggerPhantomBurstIfReady(level, player, target, marks);
      }

      hurtInternally(target, player, (float)(target.getMaxHealth() * getExtraDamagePercent()));
   }

   public static boolean isInternalDamage(LivingEntity target) {
      return target != null && INTERNAL_DAMAGE.contains(target.getUUID());
   }

   public static void clearPlayer(UUID playerId) {
      BLOOD_MARKS.remove(playerId);
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      UUID targetId = event.getEntity().getUUID();
      TARGET_VISUAL_COOLDOWNS.remove(targetId);
      for (Iterator<Map.Entry<UUID, Map<UUID, Integer>>> iterator = BLOOD_MARKS.entrySet().iterator(); iterator.hasNext();) {
         Map<UUID, Integer> marks = iterator.next().getValue();
         marks.remove(targetId);
         if (marks.isEmpty()) {
            iterator.remove();
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      clearPlayer(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      clearPlayer(event.getEntity().getUUID());
   }

   private static Player findNightfallAttacker(LivingDamageEvent.Pre event) {
      Entity source = event.getSource().getEntity();
      Entity directSource = event.getSource().getDirectEntity();
      if (!(source instanceof Player player)) {
         return null;
      }

      long gameTime = event.getEntity().level().getGameTime();
      purgeExpiredPhantomBurstSwords(gameTime);
      if (directSource instanceof EntityAbstractSummonedSword sword && PHANTOM_BURST_SWORDS.getOrDefault(sword.getUUID(), Long.MIN_VALUE) > gameTime) {
         return null;
      }
      if (OuterGodScar.isCompanionSword(directSource, gameTime)) {
         return null;
      }
      if (ReverseScaleHunt.isReverseScaleSword(directSource, gameTime)) {
         return null;
      }
      if (NightfallDragonFinalFormLogic.isBladeStormSword(directSource, gameTime)) {
         return null;
      }

      return hasDemonicBlood(player)
         && (NightfallDragonItemSupport.isDirectNightfallAttack(player, event.getSource())
         || directSource instanceof EntityAbstractSummonedSword
         || NightfallDragonItemSupport.isNightfallSlashEntityAttack(player, directSource))
            ? player
            : null;
   }

   private static boolean hasDemonicBlood(Player player) {
      ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
      return !stack.isEmpty()
         && stack.getItem() instanceof ItemSlashBlade
         && BladeStateAccess.of(stack)
            .map(state -> state.hasSpecialEffect(ModSpecialEffects.DEMONIC_BLOOD_PARASITE.getId()))
            .orElse(false);
   }

   private static boolean hasMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = BLOOD_MARKS.get(player.getUUID());
      return playerMarks != null && playerMarks.getOrDefault(target.getUUID(), 0) > 0;
   }

   private static int addMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = BLOOD_MARKS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
      int marks = playerMarks.getOrDefault(target.getUUID(), 0) + 1;
      playerMarks.put(target.getUUID(), marks);
      return marks;
   }

   private static void convertToTrueDamage(LivingDamageEvent.Pre event, Player player, LivingEntity target) {
      float amount = event.getOriginalDamage();
      event.setNewDamage(0.0F);
      hurtInternally(target, player, amount);
   }

   private static void hurtInternally(LivingEntity target, Player player, float amount) {
      if (amount <= 0.0F || target.level().isClientSide) {
         return;
      }

      INTERNAL_DAMAGE.add(target.getUUID());
      try {
         target.invulnerableTime = 0;
         target.hurt(target.level().damageSources().indirectMagic(player, player), amount);
      } finally {
         INTERNAL_DAMAGE.remove(target.getUUID());
      }
   }

   private static void triggerPhantomBurstIfReady(ServerLevel level, Player player, LivingEntity target, int marks) {
      int burstMarks = getPhantomBurstMarks();
      if (burstMarks <= 0 || marks % burstMarks != 0) {
         return;
      }

      hurtInternally(target, player, target.getMaxHealth() * PHANTOM_BURST_DAMAGE_PERCENT);
      spawnPhantomSwordBurst(level, player, target);
   }

   private static void spawnPhantomSwordBurst(ServerLevel level, Player player, LivingEntity target) {
      ModConfig.PhantomBurst config = ModConfig.COMMON.bloodPrison.phantomBurst;
      int swordCount = config.swordCount.getValue();
      double visualScale = config.visualScale.getValue();
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);

      for (int i = 0; i < swordCount; i++) {
         double angle = (Math.PI * 2.0) * i / swordCount;
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
      sword.setColor(0xDA43FF);
      sword.setDamage(0.0);
      sword.setNoClip(true);
      sword.setPierce((byte)0);
      sword.setDelay(ModConfig.COMMON.bloodPrison.phantomBurst.swordDelayTicks.getValue() + index % 4);
      sword.setRoll(index * 36.0F);
      Vec3 direction = end.subtract(start).normalize();
      sword.setPos(start.x, start.y, start.z);
      sword.moveTo(start.x, start.y, start.z, yawToFace(direction), pitchToFace(direction));
      sword.shoot(direction.x, direction.y, direction.z, 2.45F, 0.0F);
      level.addFreshEntity(sword);
      PHANTOM_BURST_SWORDS.put(sword.getUUID(), level.getGameTime() + 200L);
   }

   private static void purgeExpiredPhantomBurstSwords(long gameTime) {
      if (gameTime % 200 != 0) {
         return;
      }

      PHANTOM_BURST_SWORDS.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
   }

   private static void spawnAbyssalFlame(ServerLevel level, LivingEntity target, int marks) {
      if (!reserveVisualBurst(level, target)) {
         return;
      }

      Vec3 center = target.position();
      double radius = Math.max(0.85, target.getBbWidth() * (1.0 + marks * 0.02));
      BlockParticleOption cryingObsidian = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState());
      sendBudgetedParticles(level, cryingObsidian, center.x, center.y + 0.04, center.z, 5, radius * 0.35, 0.03, radius * 0.35, 0.02);
      sendBudgetedParticles(level, ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.12, center.z, 3, radius * 0.38, 0.06, radius * 0.38, 0.012);
      for (int i = 0; i < 4; i++) {
         double angle = (Math.PI * 2.0) * i / 4.0;
         Vec3 pos = center.add(Math.cos(angle) * radius, 0.08, Math.sin(angle) * radius);
         sendBudgetedParticles(level, cryingObsidian, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
      BlockPos below = target.blockPosition().below();
      if (level.getBlockState(below).isAir()) {
         sendBudgetedParticles(level, ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.2, center.z, 2, radius * 0.35, 0.06, radius * 0.35, 0.04);
      }
   }

   private static boolean reserveVisualBurst(ServerLevel level, LivingEntity target) {
      long gameTime = level.getGameTime();
      purgeExpiredVisualCooldowns(gameTime);
      UUID targetId = target.getUUID();
      Long blockedUntil = TARGET_VISUAL_COOLDOWNS.get(targetId);
      if (blockedUntil != null && blockedUntil > gameTime) {
         return false;
      }

      VisualBudget budget = budgetFor(level);
      if (budget.bursts >= MAX_VISUAL_BURSTS_PER_LEVEL_TICK || budget.particles >= MAX_VISUAL_PARTICLES_PER_LEVEL_TICK) {
         return false;
      }

      budget.bursts++;
      TARGET_VISUAL_COOLDOWNS.put(targetId, gameTime + VISUAL_COOLDOWN_TICKS);
      return true;
   }

   private static void purgeExpiredVisualCooldowns(long gameTime) {
      if (gameTime % 200 != 0) {
         return;
      }

      TARGET_VISUAL_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
   }

   private static void sendBudgetedParticles(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double xDist, double yDist, double zDist, double speed) {
      int allowed = reserveParticles(level, count);
      if (allowed > 0) {
         level.sendParticles(particle, x, y, z, allowed, xDist, yDist, zDist, speed);
      }
   }

   private static int reserveParticles(ServerLevel level, int requested) {
      if (requested <= 0) {
         return 0;
      }

      VisualBudget budget = budgetFor(level);
      int remaining = MAX_VISUAL_PARTICLES_PER_LEVEL_TICK - budget.particles;
      if (remaining <= 0) {
         return 0;
      }

      int allowed = Math.min(requested, remaining);
      budget.particles += allowed;
      return allowed;
   }

   private static VisualBudget budgetFor(ServerLevel level) {
      long gameTime = level.getGameTime();
      VisualBudget budget = VISUAL_BUDGETS.computeIfAbsent(level.dimension(), ignored -> new VisualBudget());
      if (budget.tick != gameTime) {
         budget.tick = gameTime;
         budget.bursts = 0;
         budget.particles = 0;
      }

      return budget;
   }

   private static float yawToFace(Vec3 direction) {
      return (float)(Mth.atan2(direction.x, direction.z) * 180.0F / (float)Math.PI);
   }

   private static float pitchToFace(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float)(-Mth.atan2(direction.y, horizontal) * 180.0F / (float)Math.PI);
   }

   private static final class VisualBudget {
      private long tick = Long.MIN_VALUE;
      private int bursts;
      private int particles;
   }
}
