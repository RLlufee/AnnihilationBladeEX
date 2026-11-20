package QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect;

import QWQ.QingYi.annihilationblade.annihilation_blade.logic.TerminusLogic;
import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class VoidDominion extends SpecialEffect {
   private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

   public VoidDominion() {
      super(0, false, false);
   }

   public static void clearPlayer(UUID playerId) {
      LAST_TRIGGER.remove(playerId);
   }

   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (event.getUser() instanceof ServerPlayer player) {
         if (!Dankong.isActive(player)) {
            ISlashBladeState state = event.getSlashBladeState();
            if (state.hasSpecialEffect(ModSpecialEffects.VOID_DOMINION.getId())) {
               ModConfig.VoidDominion config = ModConfig.COMMON.annihilationBlade.voidDominion;
               long gameTime = player.level().getGameTime();
               long last = LAST_TRIGGER.getOrDefault(player.getUUID(), -config.cooldownTicks.getValue() * 2L);
               if (gameTime - last >= config.cooldownTicks.getValue()) {
                  LAST_TRIGGER.put(player.getUUID(), gameTime);
                  ServerLevel level = player.serverLevel();
                  double range = config.range.getValue();
                  double visualScale = config.visualScale.getValue();
                  Vec3 direction = player.getLookAngle().normalize();
                  Vec3 center = player.getEyePosition().add(direction.scale(10.0));
                  List<LivingEntity> targets = new ArrayList<>();
                  for (LivingEntity target : SpecialEffectSupport.radialTargets(level, player, center, range)) {
                     if (targets.size() >= config.maxTargets.getValue()) {
                        break;
                     }
                     targets.add(target);
                  }
                  // 先发事件再处决：客户端需要在目标被移除前解析到它们
                  AnnihilationFxEvents.voidDominion(level, player, center, range, visualScale, targets);

                  for (LivingEntity target : targets) {
                     TerminusLogic.execute(target, player);
                  }
               }
            }
         }
      }
   }
}
