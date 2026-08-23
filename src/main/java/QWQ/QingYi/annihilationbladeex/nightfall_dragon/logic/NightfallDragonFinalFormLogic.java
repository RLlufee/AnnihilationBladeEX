package QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic;

import QWQ.QingYi.annihilationbladeex.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationbladeex.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic.EntropyDissolutionLogic;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic.GammaThunderburstLogic;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.visual.NightfallDragonFinalVisuals;
import QWQ.QingYi.annihilationbladeex.registry.ModSpecialEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mods.flammpfeil.slashblade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.Holder;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "annihilationbladeex")
public final class NightfallDragonFinalFormLogic {
   private static final int DOMAIN_INTERVAL_TICKS = 20;
   private static final double BLADE_STORM_RADIUS = 96.0;
   private static final int BLADE_STORM_TARGETS = 96;
   private static final double WORLD_CLEAVING_WIDTH = 5.0;
   private static final int WORLD_CLEAVING_TARGETS = 96;
   private static final float DOMAIN_FIXED_DAMAGE = 24.0F;
   private static final float WORLD_CLEAVING_DAMAGE = 44.0F;
   private static final int WORLD_CLEAVING_LIGHTNING_MAX = 96;
   private static final int WORLD_CLEAVING_LIGHTNING_COLOR = 0xB026FF;
   private static final double WORLD_CLEAVING_LIGHTNING_MIN_DISTANCE = 2.5;
   private static final float MAX_CREATION_SHIELD_HEALTH = 200.0F;

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

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onBearerHurt(LivingIncomingDamageEvent event) {
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

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onBearerDeath(LivingDeathEvent event) {
      if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !hasFinalFormInInventory(player)) {
         return;
      }

      event.setCanceled(true);
      restoreBearer(player);
      player.deathTime = 0;
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onFinalFormDamage(LivingDamageEvent.Post event) {
      if (event.getEntity().level().isClientSide) {
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

      absorbDamage(player, event.getNewDamage());
   }

   public static boolean isInternalFinalDamage(LivingEntity target) {
      return target != null && INTERNAL_FINAL_DAMAGE.contains(target.getUUID());
   }

   public static boolean isBladeStormSword(Entity entity, long gameTime) {
      purgeExpiredBladeStormSwords(gameTime);
      return entity instanceof EntityAbstractSummonedSword sword
         && BLADE_STORM_SWORD_IDS.getOrDefault(sword.getUUID(), Long.MIN_VALUE) > gameTime;
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      if (player.level().isClientSide) {
         return;
      }

      UUID playerId = player.getUUID();
      purgeSuppression(player.level().getGameTime());

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

      if (hasFinalEffect(stack, ModSpecialEffects.DRAGON_GOD_BODY.getId())) {
         refreshGodBody(player);
      }

      if (!isHoldingFinalForm(player)) {
         if (ownsFrozenMob(playerId)) {
            clearCombatState(player);
         }
         return;
      }

      if (hasHeldFinalEffect(player, ModSpecialEffects.ABSOLUTE_ANNIHILATION_DOMAIN.getId())
         && player.tickCount % DOMAIN_INTERVAL_TICKS == 0
         && player.level() instanceof ServerLevel level) {
         activateDomain(level, player);
      }

      if (hasHeldFinalEffect(player, ModSpecialEffects.DRAGON_GOD_BODY.getId())) {
         sliceFlightPath(player);
      }
   }

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

   private static void refreshGodBody(Player player) {
      restoreBearer(player);
      clampCreationShield(player);
      removeHarmfulEffects(player);
      if (player.level() instanceof ServerLevel level) {
         NightfallDragonFinalVisuals.spawnGodBodyAura(level, player);
      }

      if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().mayfly) {
         player.getAbilities().mayfly = true;
         PLAYERS_WITH_FLIGHT.add(player.getUUID());
         player.onUpdateAbilities();
      }

      if (player.getY() < -64.0) {
         player.teleportTo(player.getX(), 320.0, player.getZ());
         player.setDeltaMovement(0.0, 0.0, 0.0);
         if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
         }
      }
   }

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

   private static void removeHarmfulEffects(Player player) {
      List<Holder<MobEffect>> harmful = new ArrayList<>();
      for (MobEffectInstance effect : player.getActiveEffects()) {
         if (!effect.getEffect().value().isBeneficial()) {
            harmful.add(effect.getEffect());
         }
      }

      for (Holder<MobEffect> effect : harmful) {
         player.removeEffect(effect);
      }
   }

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
      hurtInternally(player, living, amount);
      if (living.level() instanceof ServerLevel level) {
         NightfallDragonFinalVisuals.spawnExecutionBurst(level, living);
      }
   }

   private static void absorbDamage(Player player, float amount) {
      if (amount <= 0.0F || player.level().isClientSide) {
         return;
      }

      float missing = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
      if (amount <= missing) {
         player.heal(amount);
         return;
      }

      if (missing > 0.0F) {
         player.setHealth(player.getMaxHealth());
      }

      float overflow = amount - missing;
      if (overflow > 0.0F) {
         grantCreationShield(player, overflow);
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

   private static void activateDomain(ServerLevel level, Player player) {
      Vec3 center = player.position();
      NightfallDragonFinalVisuals.spawnDomainFrame(level, center, player.tickCount);
      List<LivingEntity> targets = SpecialEffectSupport.limit(SpecialEffectSupport.radialTargets(level, player, center, getDomainRadius()), getDomainMaxTargets());
      Set<UUID> activeMobs = new HashSet<>();
      for (LivingEntity target : targets) {
         stripAndFreeze(level, player, target, activeMobs);
         float damage = Math.max(DOMAIN_FIXED_DAMAGE, target.getHealth() * 0.25F + DOMAIN_FIXED_DAMAGE);
         hurtAsDragon(player, target, damage);
         executeIfBelowHalf(player, target);
      }

      releaseMissingMobs(player, activeMobs);
   }

   private static void stripAndFreeze(ServerLevel level, Player player, LivingEntity target, Set<UUID> activeMobs) {
      target.invulnerableTime = 0;
      target.setAbsorptionAmount(0.0F);
      target.removeAllEffects();
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

   private static void spawnDragonBlade(ServerLevel level, ServerPlayer player, Vec3 center, Vec3 direction, LivingEntity target, int index, int color) {
      double angle = (Math.PI * 2.0) * index / Math.max(1, getBladeStormSwords()) + player.tickCount * 0.4;
      double radius = 2.8 + index % 4 * 0.28;
      Vec3 orbit = center.add(Math.cos(angle) * radius, 0.5 + Math.sin(angle * 2.0) * 0.85, Math.sin(angle) * radius);

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

      sword.moveTo(orbit.x, orbit.y, orbit.z, yawToFace(forward), pitchToFace(forward));
      sword.shoot(forward.x, forward.y, forward.z, 4.15F, 0.0F);
      level.addFreshEntity(sword);
      BLADE_STORM_SWORD_IDS.put(sword.getUUID(), level.getGameTime() + 200L);
      NightfallDragonFinalVisuals.spawnBladeOrbitBurst(level, index % 2 == 0 ? net.minecraft.core.particles.ParticleTypes.END_ROD : net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, orbit, 12);
   }

   private static void unleashWorldCleavingSlash(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 start = player.getEyePosition().add(0.0, -0.25, 0.0);
      Vec3 forward = safe(player.getLookAngle(), new Vec3(0.0, 0.0, 1.0));
      double worldCleavingRange = getWorldCleavingRange();
      Vec3 end = start.add(forward.scale(worldCleavingRange));
      NightfallDragonFinalVisuals.spawnPiercingBladeWave(level, player, start, end);

      List<LivingEntity> targets = SpecialEffectSupport.beamTargets(level, player, start, forward, worldCleavingRange, WORLD_CLEAVING_WIDTH, WORLD_CLEAVING_TARGETS);
      int lightningCount = 0;
      for (LivingEntity target : targets) {
         Vec3 targetCenter = SpecialEffectSupport.centerOf(target);
         double projection = Math.max(0.0, Math.min(worldCleavingRange, targetCenter.subtract(start).dot(forward)));
         Vec3 slashCenter = start.add(forward.scale(projection));

         SpecialEffectSupport.pullToward(target, slashCenter, 1.15);
         suppress(target, level.getGameTime() + 30L);
         target.invulnerableTime = 0;

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
         && BladeStateAccess.of(stack)
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

   private static float yawToFace(Vec3 direction) {
      return (float)(Mth.atan2(direction.x, direction.z) * 180.0F / (float)Math.PI);
   }

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
