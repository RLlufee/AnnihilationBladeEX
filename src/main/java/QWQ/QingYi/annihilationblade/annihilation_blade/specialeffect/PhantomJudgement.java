package QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect;

import QWQ.QingYi.annihilationblade.annihilation_blade.logic.PhantomJudgementPattern;
import QWQ.QingYi.annihilationblade.annihilation_blade.logic.TerminusLogic;
import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class PhantomJudgement extends SpecialEffect {
   private static final Map<UUID, PhantomJudgement.Sequence> ACTIVE = new HashMap<>();
   private static final Map<UUID, List<PhantomJudgement.LingeringSword>> LINGERING = new HashMap<>();
   private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

   public PhantomJudgement() {
      super(0, false, false);
   }

   public static void clearPlayer(Level level, UUID playerId) {
      ServerLevel serverLevel = level instanceof ServerLevel server ? server : null;
      PhantomJudgement.Sequence sequence = ACTIVE.remove(playerId);
      if (sequence != null) {
         AnnihilationFxEvents.phantomEnd(sequence.fxHandle);
      }
      if (serverLevel != null && sequence != null) {
         discardSearchSwords(serverLevel, sequence);
      }

      List<PhantomJudgement.LingeringSword> lingering = LINGERING.remove(playerId);
      if (serverLevel != null && lingering != null) {
         for (PhantomJudgement.LingeringSword sword : lingering) {
            discardLingering(serverLevel, sword);
         }
      }

      LAST_TRIGGER.remove(playerId);
   }

   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (event.getUser() instanceof ServerPlayer player) {
         if (!Dankong.isActive(player)) {
            ISlashBladeState state = event.getSlashBladeState();
            if (state.hasSpecialEffect(ModSpecialEffects.PHANTOM_JUDGEMENT.getId())) {
               if (!ACTIVE.containsKey(player.getUUID())) {
                  ModConfig.PhantomJudgement config = ModConfig.COMMON.annihilationBlade.phantomJudgement;
                  long gameTime = player.level().getGameTime();
                  long last = LAST_TRIGGER.getOrDefault(player.getUUID(), -config.cooldownTicks.getValue() * 2L);
                  if (gameTime - last >= config.cooldownTicks.getValue()) {
                     ServerLevel level = player.serverLevel();
                     List<PhantomJudgement.TargetLock> locks = collectTargets(level, player);
                     if (!locks.isEmpty()) {
                        LAST_TRIGGER.put(player.getUUID(), gameTime);
                        PhantomJudgement.Sequence sequence = new PhantomJudgement.Sequence(state.getColorCode(), locks);
                        ACTIVE.put(player.getUUID(), sequence);
                        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.58, 0.0);
                        sequence.fxHandle = AnnihilationFxEvents.phantomSearch(level, player, center, sequence.color, lockedEntityIds(level, locks));
                        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.8F);
                        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.1F, 0.65F);
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         if (event.player instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            tickLingeringSwords(level, player);
            PhantomJudgement.Sequence sequence = ACTIVE.get(player.getUUID());
            if (sequence != null) {
               sequence.age++;
               spawnSearchingBlades(level, player, sequence);
               if (sequence.age >= ModConfig.COMMON.annihilationBlade.phantomJudgement.searchTicks.getValue()) {
                  discardSearchSwords(level, sequence);
                  executeJudgement(level, player, sequence);
                  ACTIVE.remove(player.getUUID());
               }
            }
         }
      }
   }

   private static int[] lockedEntityIds(ServerLevel level, List<PhantomJudgement.TargetLock> locks) {
      int[] ids = new int[locks.size()];
      int count = 0;
      for (PhantomJudgement.TargetLock lock : locks) {
         LivingEntity target = findTarget(level, lock.targetId);
         if (target != null) {
            ids[count++] = target.getId();
         }
      }
      return count == ids.length ? ids : java.util.Arrays.copyOf(ids, count);
   }

   private static void spawnSearchingBlades(ServerLevel level, ServerPlayer player, PhantomJudgement.Sequence sequence) {
      ModConfig.PhantomJudgement config = ModConfig.COMMON.annihilationBlade.phantomJudgement;
      int swordCount = config.swordCount.getValue();
      double visualScale = config.visualScale.getValue();
      Vec3 center = PhantomJudgementPattern.orbitCenter(player.position(), player.getBbHeight());
      int age = sequence.age;
      double radius = PhantomJudgementPattern.orbitRadius(age, visualScale);

      for (int i = 0; i < swordCount; i++) {
         Vec3 orbit = PhantomJudgementPattern.orbitPosition(center, i, swordCount, age, radius);
         Vec3 tangent = PhantomJudgementPattern.orbitTangent(i, swordCount, age);
         EntityAbstractSummonedSword sword = getOrCreateSearchSword(level, player, sequence, i);
         placeSummonedSword(sword, orbit, tangent, sequence.color);
      }
   }

   private static void executeJudgement(ServerLevel level, ServerPlayer player, PhantomJudgement.Sequence sequence) {
      ModConfig.PhantomJudgement config = ModConfig.COMMON.annihilationBlade.phantomJudgement;
      int swordCount = config.swordCount.getValue();
      double visualScale = config.visualScale.getValue();
      Vec3 center = PhantomJudgementPattern.judgementCenter(player.position(), player.getBbHeight());

      // 先筛出本次审判的目标并发送事件，再逐个降下剑雨并处决（顺序与筛选条件与原先逐个判断完全一致）
      List<LivingEntity> targets = new ArrayList<>();
      for (PhantomJudgement.TargetLock lock : sequence.targets) {
         if (targets.size() >= config.maxTargets.getValue()) {
            break;
         }
         LivingEntity target = findTarget(level, lock.targetId);
         if (target != null && SpecialEffectSupport.canTarget(player, target)) {
            targets.add(target);
         }
      }
      AnnihilationFxEvents.phantomRain(sequence.fxHandle, level, player, center, sequence.color, targets);

      int count = 0;
      for (LivingEntity target : targets) {
         Vec3 targetCenter = SpecialEffectSupport.centerOf(target);
         Vec3 split = PhantomJudgementPattern.split(targetCenter, target.getBbHeight(), visualScale);
         spawnSwordRain(level, player, target, split, sequence.color, count);
         TerminusLogic.execute(target, player);
         count++;
      }

      level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.4F, 1.65F);
      AnnihilationFxEvents.phantomEnd(sequence.fxHandle);
   }

   private static List<PhantomJudgement.TargetLock> collectTargets(ServerLevel level, ServerPlayer player) {
      ModConfig.PhantomJudgement config = ModConfig.COMMON.annihilationBlade.phantomJudgement;
      List<LivingEntity> entities = SpecialEffectSupport.radialTargets(level, player, player.position(), config.range.getValue());
      List<PhantomJudgement.TargetLock> locks = new ArrayList<>();

      for (LivingEntity entity : entities) {
         locks.add(new PhantomJudgement.TargetLock(entity.getUUID(), SpecialEffectSupport.centerOf(entity)));
         if (locks.size() >= config.maxTargets.getValue()) {
            break;
         }
      }

      return locks;
   }

   private static EntityAbstractSummonedSword getOrCreateSearchSword(ServerLevel level, ServerPlayer player, PhantomJudgement.Sequence sequence, int index) {
      if (index < sequence.swords.size() && level.getEntity(sequence.swords.get(index)) instanceof EntityAbstractSummonedSword sword && sword.isAlive()) {
         return sword;
      }

      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(player);
      sword.setShooter(player);
      sword.setNoClip(true);
      sword.setDamage(0.0);
      sword.setPierce((byte)0);
      sword.setDelay(32);
      sword.setColor(sequence.color);
      sword.setRoll(index * 40.0F);
      level.addFreshEntity(sword);

      while (sequence.swords.size() <= index) {
         sequence.swords.add(null);
      }

      sequence.swords.set(index, sword.getUUID());
      return sword;
   }

   private static void placeSummonedSword(EntityAbstractSummonedSword sword, Vec3 pos, Vec3 direction, int color) {
      Vec3 forward = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : direction.normalize();
      sword.setColor(color);
      sword.setNoClip(true);
      sword.setDeltaMovement(Vec3.ZERO);
      sword.moveTo(pos.x, pos.y, pos.z, yawToFace(forward), pitchToFace(forward));
      sword.hasImpulse = true;
   }

   private static void discardSearchSwords(ServerLevel level, PhantomJudgement.Sequence sequence) {
      for (UUID uuid : sequence.swords) {
         if (uuid != null && level.getEntity(uuid) instanceof EntityAbstractSummonedSword sword) {
            sword.discard();
         }
      }

      sequence.swords.clear();
   }

   private static void spawnSwordRain(ServerLevel level, ServerPlayer player, LivingEntity target, Vec3 split, int color, int targetIndex) {
      ModConfig.PhantomJudgement config = ModConfig.COMMON.annihilationBlade.phantomJudgement;
      int rainSwords = config.rainSwordsPerTarget.getValue();
      double visualScale = config.visualScale.getValue();
      Vec3 center = SpecialEffectSupport.centerOf(target);
      double radius = PhantomJudgementPattern.rainRadius(target.getBbWidth(), visualScale);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 0.55F);

      for (int i = 0; i < rainSwords; i++) {
         Vec3 offset = PhantomJudgementPattern.rainOffset(i, rainSwords, targetIndex, radius);
         Vec3 start = PhantomJudgementPattern.rainStart(split, offset, i);
         Vec3 end = PhantomJudgementPattern.rainEnd(target.position(), offset);
         spawnFallingSummonedSword(level, player, start, end, color, targetIndex * rainSwords + i);
         addLingeringSword(player, end, end.subtract(start), color, targetIndex * rainSwords + i);
      }
   }

   private static void spawnFallingSummonedSword(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, int color, int index) {
      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(player);
      sword.setShooter(player);
      sword.setColor(color);
      sword.setDamage(0.0);
      sword.setNoClip(true);
      sword.setPierce((byte)0);
      sword.setDelay(ModConfig.COMMON.annihilationBlade.phantomJudgement.fallingSwordDelayTicks.getValue());
      sword.setRoll(index * 27.0F);
      Vec3 direction = end.subtract(start).normalize();
      sword.setPos(start.x, start.y, start.z);
      sword.moveTo(start.x, start.y, start.z, yawToFace(direction), pitchToFace(direction));
      sword.shoot(direction.x, direction.y, direction.z, 2.1F, 0.0F);
      level.addFreshEntity(sword);
   }

   private static LivingEntity findTarget(ServerLevel level, UUID uuid) {
      return SpecialEffectSupport.findLivingEntity(level, uuid);
   }

   private static void addLingeringSword(ServerPlayer player, Vec3 ground, Vec3 fallDirection, int color, int index) {
      Vec3 direction = fallDirection.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : fallDirection.normalize();
      List<PhantomJudgement.LingeringSword> swords = LINGERING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
      if (swords.size() < ModConfig.COMMON.annihilationBlade.phantomJudgement.maxLingeringSwords.getValue()) {
         swords.add(new PhantomJudgement.LingeringSword(ground, direction, color, index * 27.0F));
      }
   }

   private static void tickLingeringSwords(ServerLevel level, ServerPlayer player) {
      List<PhantomJudgement.LingeringSword> swords = LINGERING.get(player.getUUID());
      if (swords != null) {
         Iterator<PhantomJudgement.LingeringSword> iterator = swords.iterator();

         while (iterator.hasNext()) {
            PhantomJudgement.LingeringSword lingering = iterator.next();
            if (lingering.age++ >= ModConfig.COMMON.annihilationBlade.phantomJudgement.lingerTicks.getValue()) {
               discardLingering(level, lingering);
               iterator.remove();
            } else {
               EntityAbstractSummonedSword sword = getOrCreateLingeringSword(level, player, lingering);
               Vec3 direction = lingering.direction;
               sword.setColor(lingering.color);
               sword.setNoClip(true);
               sword.setDamage(0.0);
               sword.setPierce((byte)0);
               sword.setDelay(ModConfig.COMMON.annihilationBlade.phantomJudgement.lingerTicks.getValue());
               sword.setDeltaMovement(Vec3.ZERO);
               sword.moveTo(lingering.position.x, lingering.position.y, lingering.position.z, yawToFace(direction), pitchToFace(direction));
               sword.setRoll(lingering.roll);
               sword.hasImpulse = true;
            }
         }

         if (swords.isEmpty()) {
            LINGERING.remove(player.getUUID());
         }
      }
   }

   private static EntityAbstractSummonedSword getOrCreateLingeringSword(ServerLevel level, ServerPlayer player, PhantomJudgement.LingeringSword lingering) {
      if (lingering.entityId != null && level.getEntity(lingering.entityId) instanceof EntityAbstractSummonedSword sword && sword.isAlive()) {
         return sword;
      }

      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(player);
      sword.setShooter(player);
      sword.setColor(lingering.color);
      sword.setNoClip(true);
      sword.setDamage(0.0);
      sword.setPierce((byte)0);
      sword.setDelay(ModConfig.COMMON.annihilationBlade.phantomJudgement.lingerTicks.getValue());
      sword.setRoll(lingering.roll);
      sword.setPos(lingering.position.x, lingering.position.y, lingering.position.z);
      level.addFreshEntity(sword);
      lingering.entityId = sword.getUUID();
      return sword;
   }

   private static void discardLingering(ServerLevel level, PhantomJudgement.LingeringSword lingering) {
      if (lingering.entityId != null) {
         if (level.getEntity(lingering.entityId) instanceof EntityAbstractSummonedSword sword) {
            sword.discard();
         }
      }
   }

   private static float yawToFace(Vec3 direction) {
      return (float)(Mth.atan2(direction.x, direction.z) * 180.0F / (float)Math.PI);
   }

   private static float pitchToFace(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float)(Mth.atan2(direction.y, horizontal) * 180.0F / (float)Math.PI);
   }

   private static class LingeringSword {
      private final Vec3 position;
      private final Vec3 direction;
      private final int color;
      private final float roll;
      private UUID entityId;
      private int age;

      private LingeringSword(Vec3 position, Vec3 direction, int color, float roll) {
         this.position = position;
         this.direction = direction;
         this.color = color;
         this.roll = roll;
      }
   }

   private static class Sequence {
      private final int color;
      private final List<PhantomJudgement.TargetLock> targets;
      private final List<UUID> swords = new ArrayList<>();
      private int age;
      private int fxHandle = -1;

      private Sequence(int color, List<PhantomJudgement.TargetLock> targets) {
         this.color = color;
         this.targets = targets;
      }
   }

   private record TargetLock(UUID targetId, Vec3 center) {
   }
}
