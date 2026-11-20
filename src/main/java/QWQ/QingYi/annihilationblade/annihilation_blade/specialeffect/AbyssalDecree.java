package QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect;

import QWQ.QingYi.annihilationblade.annihilation_blade.logic.TerminusLogic;
import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class AbyssalDecree extends SpecialEffect {
   private static final Map<UUID, AbyssalDecree.Sequence> ACTIVE = new HashMap<>();
   private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

   public AbyssalDecree() {
      super(0, false, false);
   }

   public static void clearPlayer(UUID playerId) {
      AbyssalDecree.Sequence sequence = ACTIVE.remove(playerId);
      if (sequence != null) {
         AnnihilationFxEvents.decreeEnd(sequence.fxHandle);
      }
      LAST_TRIGGER.remove(playerId);
   }

   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (event.getUser() instanceof ServerPlayer player) {
         if (!Dankong.isActive(player)) {
            ISlashBladeState state = event.getSlashBladeState();
            if (state.hasSpecialEffect(ModSpecialEffects.ABYSSAL_DECREE.getId())) {
               if (!ACTIVE.containsKey(player.getUUID())) {
                  ModConfig.AbyssalDecree config = ModConfig.COMMON.annihilationBlade.abyssalDecree;
                  long gameTime = player.level().getGameTime();
                  long last = LAST_TRIGGER.getOrDefault(player.getUUID(), -config.cooldownTicks.getValue() * 2L);
                  if (gameTime - last >= config.cooldownTicks.getValue()) {
                     List<UUID> targets = collectTargets(player);
                     if (!targets.isEmpty()) {
                        LAST_TRIGGER.put(player.getUUID(), gameTime);
                        AbyssalDecree.Sequence sequence = new AbyssalDecree.Sequence(targets);
                        ACTIVE.put(player.getUUID(), sequence);
                        ServerLevel level = player.serverLevel();
                        int interval = config.strikeInterval.getValue();
                        sequence.fxHandle = AnnihilationFxEvents.decreeBegin(
                           level, player, crownOf(player), config.range.getValue(), config.visualScale.getValue(),
                           targets.size() * interval + interval * 2 + 20
                        );
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.55F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.1F, 1.45F);
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
            AbyssalDecree.Sequence sequence = ACTIVE.get(player.getUUID());
            if (sequence != null) {
               sequence.age++;
               ServerLevel level = player.serverLevel();
               Vec3 crown = crownOf(player);
               if (sequence.age % ModConfig.COMMON.annihilationBlade.abyssalDecree.strikeInterval.getValue() == 0) {
                  while (sequence.index < sequence.targets.size()) {
                     LivingEntity target = findTarget(level, sequence.targets.get(sequence.index++));
                     if (target != null && SpecialEffectSupport.canTarget(player, target)) {
                        strike(level, player, target, sequence);
                        return;
                     }
                  }

                  level.playSound(null, crown.x, crown.y, crown.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.1F, 0.6F);
                  AnnihilationFxEvents.decreeEnd(sequence.fxHandle);
                  ACTIVE.remove(player.getUUID());
               }
            }
         }
      }
   }

   private static Vec3 crownOf(ServerPlayer player) {
      return player.position().add(0.0, player.getBbHeight() + 3.2, 0.0);
   }

   private static List<UUID> collectTargets(ServerPlayer player) {
      ModConfig.AbyssalDecree config = ModConfig.COMMON.annihilationBlade.abyssalDecree;
      List<LivingEntity> entities = SpecialEffectSupport.radialTargets(player.serverLevel(), player, player.position(), config.range.getValue());
      entities.sort(
         Comparator.<LivingEntity>comparingDouble(entityx -> entityx.getHealth() + entityx.getArmorValue() * 2.0)
            .reversed()
            .thenComparingDouble(entityx -> entityx.distanceToSqr(player))
      );
      List<UUID> result = new ArrayList<>();

      for (LivingEntity entity : entities) {
         result.add(entity.getUUID());
         if (result.size() >= config.maxTargets.getValue()) {
            break;
         }
      }

      return result;
   }

   private static void strike(ServerLevel level, ServerPlayer player, LivingEntity target, AbyssalDecree.Sequence sequence) {
      // 先发事件再处决：客户端需要在目标被移除前解析到它
      AnnihilationFxEvents.decreeStrike(sequence.fxHandle, level, player, target, sequence.strikes++);
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.9F, 1.9F);
      TerminusLogic.execute(target, player);
   }

   private static LivingEntity findTarget(ServerLevel level, UUID uuid) {
      return SpecialEffectSupport.findLivingEntity(level, uuid);
   }

   private static class Sequence {
      private final List<UUID> targets;
      private int age;
      private int index;
      private int strikes;
      private int fxHandle = -1;

      private Sequence(List<UUID> targets) {
         this.targets = targets;
      }
   }
}
