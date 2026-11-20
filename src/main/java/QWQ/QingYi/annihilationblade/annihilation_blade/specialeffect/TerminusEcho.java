package QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect;

import QWQ.QingYi.annihilationblade.annihilation_blade.logic.TerminusEchoPattern;
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
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class TerminusEcho extends SpecialEffect {
   private static final Map<UUID, List<TerminusEcho.EchoSequence>> ACTIVE = new HashMap<>();
   private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

   public TerminusEcho() {
      super(0, false, false);
   }

   public static void clearPlayer(UUID playerId) {
      List<TerminusEcho.EchoSequence> sequences = ACTIVE.remove(playerId);
      if (sequences != null) {
         for (TerminusEcho.EchoSequence sequence : sequences) {
            AnnihilationFxEvents.echoEnd(sequence.fxHandle);
         }
      }
      LAST_TRIGGER.remove(playerId);
   }

   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (event.getUser() instanceof ServerPlayer player) {
         if (!Dankong.isActive(player)) {
            ISlashBladeState state = event.getSlashBladeState();
            if (state.hasSpecialEffect(ModSpecialEffects.TERMINUS_ECHO.getId())) {
               ModConfig.TerminusEcho config = ModConfig.COMMON.annihilationBlade.terminusEcho;
               List<TerminusEcho.EchoSequence> sequences = ACTIVE.get(player.getUUID());
               if (sequences == null || sequences.size() < config.maxActiveSequences.getValue()) {
                  if (SpecialEffectSupport.tryStartCooldown(LAST_TRIGGER, player, player.level().getGameTime(), config.cooldownTicks.getValue())) {
                     Vec3 start = player.getEyePosition().add(player.getLookAngle().normalize().scale(1.0));
                     Vec3 direction = player.getLookAngle().normalize();
                     Vec3 right = SpecialEffectSupport.rightOf(direction);
                     TerminusEcho.EchoSequence sequence = new TerminusEcho.EchoSequence(start, direction, right);
                     ACTIVE.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(sequence);
                     releaseEcho(player.serverLevel(), player, sequence, 0);
                     sequence.nextWave = 1;
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
            List<TerminusEcho.EchoSequence> sequences = ACTIVE.get(player.getUUID());
            if (sequences != null && !sequences.isEmpty()) {
               Iterator<TerminusEcho.EchoSequence> iterator = sequences.iterator();

               while (iterator.hasNext()) {
                  TerminusEcho.EchoSequence sequence = iterator.next();
                  sequence.age++;
                  if (sequence.age % ModConfig.COMMON.annihilationBlade.terminusEcho.echoInterval.getValue() == 0) {
                     if (sequence.nextWave >= ModConfig.COMMON.annihilationBlade.terminusEcho.echoCount.getValue()) {
                        AnnihilationFxEvents.echoEnd(sequence.fxHandle);
                        iterator.remove();
                     } else {
                        releaseEcho(player.serverLevel(), player, sequence, sequence.nextWave);
                        sequence.nextWave++;
                     }
                  }
               }

               if (sequences.isEmpty()) {
                  ACTIVE.remove(player.getUUID());
               }
            }
         }
      }
   }

   private static void releaseEcho(ServerLevel level, ServerPlayer player, TerminusEcho.EchoSequence sequence, int wave) {
      ModConfig.TerminusEcho config = ModConfig.COMMON.annihilationBlade.terminusEcho;
      double range = TerminusEchoPattern.range(config.range.getValue(), wave);
      double width = TerminusEchoPattern.width(config.width.getValue(), wave);
      Vec3 start = TerminusEchoPattern.start(sequence.start, sequence.right, wave);
      List<LivingEntity> targets = SpecialEffectSupport.beamTargets(level, player, start, sequence.direction, range, width, config.maxTargetsPerWave.getValue());
      // 先发事件再处决：客户端需要在目标被移除前解析到它们
      int echoCount = config.echoCount.getValue();
      int interval = config.echoInterval.getValue();
      if (wave == 0) {
         sequence.fxHandle = AnnihilationFxEvents.echoBegin(
            level, player, sequence.start, sequence.direction, config.range.getValue(), config.width.getValue(),
            echoCount, interval, config.visualScale.getValue(), targets, echoCount * interval + interval * 2 + 10
         );
      } else {
         AnnihilationFxEvents.echoWave(
            sequence.fxHandle, level, player, sequence.start, sequence.direction, config.range.getValue(), config.width.getValue(),
            wave, echoCount, interval, config.visualScale.getValue(), targets
         );
      }
      for (LivingEntity target : targets) {
         TerminusLogic.execute(target, player);
      }
   }

   private static class EchoSequence {
      private final Vec3 start;
      private final Vec3 direction;
      private final Vec3 right;
      private int age;
      private int nextWave;
      private int fxHandle = -1;

      private EchoSequence(Vec3 start, Vec3 direction, Vec3 right) {
         this.start = start;
         this.direction = direction;
         this.right = right;
      }
   }
}
